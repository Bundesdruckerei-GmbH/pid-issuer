/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.doc.in;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.doc.core.QrCodeService;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@PrimaryAdapter
@Controller
@RequestMapping("/")
public class UiController {
    private static final String SCHEME_OPENID_CREDENTIAL_OFFER = "openid-credential-offer";
    private static final String QUERY_PARAM_CREDENTIAL_OFFER = "credential_offer";

    private static final String PID_SD_JWT = "pid-sd-jwt";
    private static final String SD_JWT_URI = "sdJwtUri";
    private static final String SD_JWT_QR = "sdJwtQr";

    private static final String PID_MSO_MDOC = "pid-mso-mdoc";
    private static final String MSO_MDOC_URI = "msoMdocUri";
    private static final String MSO_MDOC_QR = "msoMdocQr";

    @Value("${pidi.base-url}")
    private String baseUrl;
    @Value("${info.app.version:unknown}")
    private String version;

    private final QrCodeService qrCodeService;

    public UiController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping()
    public String getIndexView() {
        return "index";
    }

    @GetMapping("variant-b")
    public String variantB() {
        return "variant-b";
    }

    @GetMapping("variant-b1")
    public String variantB1() {
        return "variant-b1";
    }

    @GetMapping("variant-c")
    public String variantC(Model model) throws IOException {
        URI sdJwtUri = generateUri(FlowVariant.C, PID_SD_JWT);
        URI msoMdocUri = generateUri(FlowVariant.C, PID_MSO_MDOC);
        model.addAllAttributes(Map.of(SD_JWT_URI, sdJwtUri.toString(),
                SD_JWT_QR, toPngDataUrl(qrCodeService.generateQrCode(sdJwtUri.toString(), 250)),
                MSO_MDOC_URI, msoMdocUri.toString(), MSO_MDOC_QR, toPngDataUrl(qrCodeService.generateQrCode(msoMdocUri.toString(), 250))));

        return "variant-c";
    }

    @GetMapping("variant-c1")
    public String variantC1(Model model) throws IOException {
        URI sdJwtUri = generateUri(FlowVariant.C1, PID_SD_JWT);
        URI msoMdocUri = generateUri(FlowVariant.C1, PID_MSO_MDOC);
        model.addAllAttributes(Map.of(
                SD_JWT_URI, sdJwtUri.toString(),
                SD_JWT_QR, toPngDataUrl(qrCodeService.generateQrCode(sdJwtUri.toString(), 250)),
                MSO_MDOC_URI, msoMdocUri.toString(),
                MSO_MDOC_QR, toPngDataUrl(qrCodeService.generateQrCode(msoMdocUri.toString(), 250))));

        return "variant-c1";
    }

    @GetMapping("variant-c2")
    public String variantC2(Model model) throws IOException {
        URI sdJwtUri = generateUri(FlowVariant.C2, PID_SD_JWT);
        URI msoMdocUri = generateUri(FlowVariant.C2, PID_MSO_MDOC);
        model.addAllAttributes(Map.of(SD_JWT_URI, sdJwtUri.toString(),
                SD_JWT_QR, toPngDataUrl(qrCodeService.generateQrCode(sdJwtUri.toString(), 250)),
                MSO_MDOC_URI, msoMdocUri.toString(), MSO_MDOC_QR, toPngDataUrl(qrCodeService.generateQrCode(msoMdocUri.toString(), 250))));

        return "variant-c2";
    }

    @GetMapping("sdjwt")
    public String sdJwt() {
        return "sdjwt";
    }

    @GetMapping("msomdoc")
    public String msoMdoc() {
        return "msomdoc";
    }

    @GetMapping("releases")
    public String releases() {
        return "releases";
    }

    @GetMapping("licence")
    public String licence() {
        return "licence";
    }

    @GetMapping("privacy-terms")
    public String privacy() {
        return "privacy";
    }

    @GetMapping("error")
    public String error() {
        return "error";
    }

    @ModelAttribute("version")
    public String version() {
        return version;
    }

    private URI generateUri(FlowVariant flowVariant, String credentialDataType) {
        return URI.create(SCHEME_OPENID_CREDENTIAL_OFFER + "://?" +
                QUERY_PARAM_CREDENTIAL_OFFER + "=" +
                URLEncoder.encode("{\"credential_issuer\":\"" + baseUrl + "/" + flowVariant.urlPath + "\"," +
                        "\"credential_configuration_ids\":[\"" + credentialDataType + "\"],\"grants\":{\"authorization_code\":{}}}", StandardCharsets.UTF_8)
        );
    }

    private String toPngDataUrl(BufferedImage image) throws IOException {
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes);
            var imageAsBase64 = Base64.getEncoder().encodeToString(bytes.toByteArray());
            return "data:image/png;base64," + imageAsBase64;
        }
    }
}
