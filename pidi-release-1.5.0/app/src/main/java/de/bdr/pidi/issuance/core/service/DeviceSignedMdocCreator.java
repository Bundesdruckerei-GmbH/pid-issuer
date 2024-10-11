/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.issuance.core.service;

import COSE.AlgorithmID;
import COSE.Attribute;
import COSE.CoseException;
import COSE.HeaderKeys;
import COSE.OneKey;
import com.nimbusds.jose.JOSEException;
import com.upokecenter.cbor.CBORObject;
import de.bdr.openid4vc.vci.credentials.mdoc.CredentialStructure;
import de.bdr.openid4vc.vci.credentials.mdoc.MDocCredentialConfiguration;
import de.bdr.openid4vc.vci.credentials.mdoc.MdocData;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.authorization.out.issuance.FaultyRequestParameterException;
import de.bdr.pidi.base.PidDataConst;
import de.bdr.pidi.base.requests.MsoMdocAuthChannelCredentialRequest;
import de.bundesdruckerei.mdoc.kotlin.core.Document;
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript;
import de.bundesdruckerei.mdoc.kotlin.core.auth.DeviceKeyInfo;
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSigned;
import de.bundesdruckerei.mdoc.kotlin.core.auth.KeyAuthorizations;
import de.bundesdruckerei.mdoc.kotlin.core.auth.ValueDigests;
import de.bundesdruckerei.mdoc.kotlin.core.auth.X5Chain;
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.ValidityRange;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceAuth;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceAuthentication;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceMac;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceNameSpaces;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceSigned;
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.DeviceSignedItems;
import de.bundesdruckerei.mdoc.kotlin.crypto.cose.COSEMac0;
import kotlin.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static de.bdr.pidi.issuance.core.service.IssuerSignedMdocCreator.getMdocDataMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

public class DeviceSignedMdocCreator {
    private final ConcurrentMap<UUID, PidCredentialData> pidCredentialDataMap = new ConcurrentHashMap<>();
    private final X5Chain x5Chain;
    @Getter
    private final MDocCredentialConfiguration configuration;

    public DeviceSignedMdocCreator(MDocCredentialConfiguration configuration, List<X509Certificate> certificates) {
        if (!PidDataConst.MDOC_TYPE.equals(configuration.getDocType()) || configuration.getCredentialStructure() != CredentialStructure.DOCUMENT) {
            throw new IllegalArgumentException("invalid config");
        }
        this.configuration = configuration;
        this.x5Chain = switch (certificates.size()) {
            case 0 -> throw new IllegalStateException("Empty certificates not allowed");
            case 1 -> X5Chain.Companion.of(certificates.get(0));
            case 2 -> X5Chain.Companion.of(certificates.get(0), certificates.get(1));
            default -> X5Chain.Companion.of(
                    certificates.get(0),
                    certificates.get(1),
                    certificates.subList(2, certificates.size()).toArray(new X509Certificate[0])
            );
        };
    }

    public String create(MsoMdocAuthChannelCredentialRequest request, UUID issuanceId, ECPrivateKey signerPrivateKey) {
        var sessionTranscript = getSessionTranscript(request);
        ECPublicKey verifierPub = getVerifierPub(request);
        EMacKey eMacKey = buildHKDFKey(signerPrivateKey, verifierPub, sessionTranscript);
        var mdocData = create(issuanceId);

        var isBuilder = new IssuerSigned.Builder(getConfiguration().getDocType(), x5Chain);
        isBuilder.setSigningAlgorithm(AlgorithmID.ECDSA_256);
        isBuilder.setValidityRange(new ValidityRange(Objects.requireNonNull(mdocData.getValidFrom()), Objects.requireNonNull(mdocData.getValidUntil())));

        isBuilder.setDeviceKeyInfo(buildDeviceKeyInfo())
                .overrideValueDigests(buildDummyValueDigests());

        var issuerSigned = isBuilder.buildAndSign(signerPrivateKey);

        var nameSpaces = toDeviceSignedMap(mdocData);
        var deviceNameSpaces = new DeviceNameSpaces(nameSpaces);
        var deviceAuthentication = new DeviceAuthentication(sessionTranscript, getConfiguration().getDocType(), deviceNameSpaces.asTaggedCBOR());
        var mac = createMac(eMacKey.getByte(), deviceAuthentication);
        var deviceMac = new DeviceMac(mac);
        var deviceAuth = new DeviceAuth(deviceMac);
        var deviceSigned = new DeviceSigned(deviceNameSpaces, deviceAuth);

        var mdocBytes = new Document(getConfiguration().getDocType(), issuerSigned, deviceSigned).asCBOR().EncodeToBytes();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mdocBytes);
    }

    @NotNull
    private DeviceKeyInfo buildDeviceKeyInfo() {
        OneKey deviceKey;
        try {
            deviceKey = new OneKey(x5Chain.getEndEntityCert().getPublicKey(), null);
        } catch (CoseException e) {
            throw new IllegalStateException(e);
        }
        KeyAuthorizations keyAuthorizations = new KeyAuthorizations(new ArrayList<>(List.of(getConfiguration().getDocType())), null);

        return new DeviceKeyInfo(deviceKey, keyAuthorizations, null);
    }

    @NotNull
    private ValueDigests buildDummyValueDigests() {
        return new ValueDigests(Map.of(getConfiguration().getDocType(), Map.of(0L, new byte[]{0})));
    }

    private MdocData create(UUID key) {
        var data = pidCredentialDataMap.get(key);
        var validFrom = Instant.now();
        var validUntil = validFrom.plus(getConfiguration().getLifetime());
        var namespaces = Map.of(getConfiguration().getDocType(), getMdocDataMap(data, validFrom, validUntil));

        return new MdocData(validFrom, validUntil, namespaces);
    }

    private static EMacKey buildHKDFKey(ECPrivateKey signerPrivateKey, ECPublicKey verifierPub, SessionTranscript sessionTranscript) {
        try {
            return new EMacKey(signerPrivateKey, verifierPub, sessionTranscript);
        } catch (InvalidKeyException e) {
            throw new FaultyRequestParameterException("verifierPub is no valid ec key", e);
        }
    }

    private ECPublicKey getVerifierPub(MsoMdocAuthChannelCredentialRequest credentialRequest) {
        try {
            return credentialRequest.getVerifierPub().toECKey().toECPublicKey();
        } catch (JOSEException | ClassCastException e) {
            throw new FaultyRequestParameterException("verifierPub is no valid ec key", e);
        }
    }

    /**
     * SessionTranscript.Companion.fromCBOR expects not null values for deviceEngagementBytes and eReaderKeyBytes,
     * while we expect them to be null, so we have to build the SessionTranscript by ourselves
     */
    private static SessionTranscript getSessionTranscript(MsoMdocAuthChannelCredentialRequest credentialRequest) {
        var cbor = CBORObject.DecodeFromBytes(credentialRequest.getSessionTranscript());
        if (cbor.size() != 3) {
            throw new FaultyRequestParameterException("sessionTranscript has invalid size");
        }
        if (!cbor.get(0).isNull() || !cbor.get(1).isNull() || cbor.get(2).isNull()) {
            throw new FaultyRequestParameterException("sessionTranscript has invalid content");
        }
        return new SessionTranscript(null, null, cbor.get(2).EncodeToBytes());
    }

    /**
     * description of creating the COSEMac0 structure:
     * create MAC_structure = [
     * context : "MAC0",
     * protected : {"1": "5"},
     * payload : deviceAuthentication
     * ]
     * calculate tag on MAC_structure with sharedSecret
     * create COSEMac0 = [
     * context : "MAC0",
     * protected : {"1": "5"},
     * unprotected : {},
     * payload : null
     * tag: hmac from MAC_structure
     * ]
     * The COSEMac0 class seem to do this, by calculating the tag and then hide the payload in the CBOR object (detached payload)
     */
    private COSEMac0 createMac(byte[] sharedSecret, DeviceAuthentication deviceAuthentication) {
        var mac = new COSEMac0();
        try {
            mac.addAttribute(HeaderKeys.Algorithm, AlgorithmID.HMAC_SHA_256.AsCBOR(), Attribute.PROTECTED);
            mac.SetContent(deviceAuthentication.asBytes());
            mac.Create(sharedSecret);
            mac.EncodeToCBORObject();
        } catch (CoseException e) {
            throw new IllegalArgumentException(e);
        }
        return mac;
    }

    private static Map<String, DeviceSignedItems> toDeviceSignedMap(MdocData mdocData) {
        return mdocData.getNamespaces().entrySet().stream().collect(toMap(
                Map.Entry::getKey,
                a -> new DeviceSignedItems(a.getValue().entrySet().stream()
                        .map(b -> new Pair<>(b.getKey(), b.getValue()))
                        .collect(toCollection(ArrayList::new)))));
    }

    public void putPidCredentialData(UUID key, PidCredentialData value) {
        pidCredentialDataMap.put(key, value);
    }

    public void removePidCredentialData(UUID key) {
        pidCredentialDataMap.remove(key);
    }
}
