package com.example.vds_tools.controller;

import com.example.vds_tools.model.Aliases;
import com.example.vds_tools.model.CertificateDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;


import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {
    @Autowired
    private HttpSession session;

    List<CertificateDTO> certificates = new ArrayList<>();
    List<CertificateDTO> actualAliases=new ArrayList<>();
    @GetMapping("/{keyStoreIdentifier}")
    public ResponseEntity<List<CertificateDTO>> getCertificates(@PathVariable String keyStoreIdentifier) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        // Replace this with your actual logic to fetch certificates


        certificates.clear();
        if ("WINDOWS".equalsIgnoreCase(keyStoreIdentifier)) {
           getWindowsKeystoreAliases();
        } else if ("DONGLE".equalsIgnoreCase(keyStoreIdentifier)) {
            getDongleKeystoreAliases();
        }

        return ResponseEntity.ok(certificates);
    }

    public void getWindowsKeystoreAliases() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("Windows-MY");

        // Load the keystore from the default Windows keystore (does not require a password)
        keyStore.load(null, null);  // No password required for "Windows-MY"

        // Get all aliases from the keystore
        Enumeration<String> aliases = keyStore.aliases();

        // Get the current date
        Date currentDate = new Date();

        // Iterate through and check validity of each alias
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            // Retrieve the certificate associated with the alias
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert != null) {
                // Get the expiration date (notAfter) and validity period (notBefore, notAfter)
                Date expirationDate = cert.getNotAfter();
                Date startDate = cert.getNotBefore();
                System.out.println("Certificate name : "+alias+ " ----start date: " +startDate+ " ----end date: "+expirationDate);
                // Check if the certificate is valid (current date is within the valid range)
                boolean isValid = currentDate.after(startDate) && currentDate.before(expirationDate);

                // If valid, add the alias to the list
                if (isValid) {
                    certificates.add(new CertificateDTO(alias,alias));
                }
            }
        }

    }

    public void getDongleKeystoreAliases() throws  IOException,  KeyStoreException, CertificateException, NoSuchAlgorithmException {
        // Load the PKCS#11 configuration file
        ClassPathResource resource = new ClassPathResource("pkcs11.cfg");
        File configFile = resource.getFile();
        String configPath = configFile.getAbsolutePath();

        Provider pkcs11Provider = Security.getProvider("SunPKCS11");
        pkcs11Provider = pkcs11Provider.configure(configPath);
        Security.addProvider(pkcs11Provider);



        String pin = (String) session.getAttribute("pin"); // Retrieve the 'pin' value
        System.out.println("Retrieved PIN: " + pin);

        // Access the key store
        KeyStore keyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
        //String pin = "Snlrmr199257#"; // Replace with your actual PIN
        keyStore.load(null, pin.toCharArray());

        // List all aliases in the key store
      //  System.out.println("Certificates in the dongle:");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Get the current date
        Date currentDate = new Date();
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            System.out.println("Alias: " + alias);

            if (keyStore.isKeyEntry(alias)) {
                Certificate cert = keyStore.getCertificate(alias);
                // System.out.println("Certificate: " + cert.toString());

                if (cert instanceof X509Certificate) {
                    X509Certificate x509Cert = (X509Certificate) cert;

                    // Extract the CN from the Subject DN
                    Date expirationDate = x509Cert.getNotAfter();
                    Date startDate = x509Cert.getNotBefore();

                    // Check if the certificate is valid (current date is within the valid range)
                    boolean isValid = currentDate.after(startDate) && currentDate.before(expirationDate);

                    // If valid, add the alias to the list
                    if (isValid) {
                        String subjectDN = x509Cert.getSubjectX500Principal().getName();
                        String cn = getCommonName(subjectDN);
                        // System.out.println("Subject CN: " + cn);
                        certificates.add(new CertificateDTO(cn,alias));
                    }
                }
            }
        }

    }

    private static String getCommonName(String subjectDN) {
        String[] dnParts = subjectDN.split(",");
        for (String part : dnParts) {
            String trimmedPart = part.trim();
            if (trimmedPart.startsWith("CN=")) {
                return trimmedPart.substring(3); // Remove "CN=" prefix
            }
        }
        return null;
    }

}
