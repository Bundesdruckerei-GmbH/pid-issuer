/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.base;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * access file resources
 */
@Component
public class FileResourceHelper {

    public X509Certificate readCertificate(String path) {
        try (InputStream in = new FileInputStream(Paths.get(path).toFile())) {
            var certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(in);
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Could not work with certificate from " + path, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read certificate from " + path, e);
        }
    }

    public KeyStore readKeyStore(String path, String password) {
        try (InputStream in = getFileInputStream(path)) {
            var result = KeyStore.getInstance("PKCS12");
            result.load(in, password.toCharArray());
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read keystore from " + path, e);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Could not work with the keystore from "+ path, e);
        }
    }

    public static FileInputStream getFileInputStream(String path) throws FileNotFoundException {
        return new FileInputStream(Paths.get(path).toFile());
    }
}
