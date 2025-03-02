package com.example.vds_tools.controller;

import com.example.vds_tools.service.WindowsPrivateKeyService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.Signer;
import de.tsenger.vdstools.vds.DigitalSeal;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsMessage;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private WindowsPrivateKeyService privateKeyService;

    @GetMapping("/vdss") // This maps the form to the /vds URL
    public String showForm(Model model)  {
        // Return the view (Thymeleaf template)
        return "homePage";  // Ensure this matches your actual Thymeleaf template name
    }


    @PostMapping("/process-vds-form")
    public ResponseEntity<?> processForm(@RequestBody VdsController.FormData formData) {



        System.out.println("Form Data" + formData);

        String firstName=formData.getFirstName() ;
        String signingCertificate=formData.getSigningCertificate();
        String cgpa=formData.getCgpa() ;
        String university=formData.getUniversity() ;
        String division=formData.getDivision() ;
        String id=formData.getId() ;

        String vdsType=formData.getVdsType() ;
        String issuingCountry=formData.getIssuingCountry() ;
        String signerIdentifier=formData.getSignerIdentifier() ;
        String certificateReference=formData.getCertificateReference() ;
        String issuingDate=formData.getIssuingDate() ;
        String sigDate=formData.getSigDate() ;
        String keyStoreIdentifier=formData.getKeyStoreIdentifier() ;
        String storedPassword=formData.getStoredPassword() ;

        byte[] qr=generateDigitalSeal(firstName,cgpa,signingCertificate,
                university,division,id,vdsType,issuingCountry,
                signerIdentifier,certificateReference,issuingDate,sigDate,storedPassword,keyStoreIdentifier);

        String base64QRCode = Base64.getEncoder().encodeToString(qr);
        String processedResult = base64QRCode;


        System.out.println("QR Code Length: " + qr.length);
        System.out.println("Base64 QR Code: " + base64QRCode.substring(0, 100));


        Map<String, Object> response = new HashMap<>();
        response.put("message", processedResult);
        response.put("qrCode", base64QRCode); // Add the QR byte array to the response

        // Return the response with the QR code and message
        return ResponseEntity.ok(response);
    }

    public byte[] generateDigitalSeal(
            String firstName_,
            String cgpa_,
            String signingCertificate_,
            String university_,
            String division_,
            String id_,
            String vdsType_,
            String issuingCountry_,
            String signerIdentifier_,
            String certificateReference_,
            String issuingDate_,
            String sigDate_,
            String storedPassword_,
            String keyStoreIdentifier_) {

        byte[] qrBytes = null;
        HttpHeaders headers = null;
        try {
            String password_ = "bccca";


            PrivateKey privateKey = null;
            if (keyStoreIdentifier_.equals("WINDOWS")) {
                KeyStore keystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
                keystore.load(null, null);
                String alias = signingCertificate_;
                System.out.println("Alias: " + alias);
                X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
                System.out.println("Certificate in windows key trore: " + cert);
            } else if (keyStoreIdentifier_.equals("DONGLE")) {

                ClassPathResource resource = new ClassPathResource("pkcs11.cfg");
                File configFile = resource.getFile();
                String configPath = configFile.getAbsolutePath();

                Provider pkcs11Provider = Security.getProvider("SunPKCS11");
                pkcs11Provider = pkcs11Provider.configure(configPath);
                Security.addProvider(pkcs11Provider);

                // Access the key store
                KeyStore keyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
                //String pin = "Snlrmr199257#"; // Replace with your actual PIN

                //String pin = (String) session.getAttribute("pin"); // Retrieve the 'pin' value
                // System.out.println("Retrieved PIN: " + pin);

                String pin = storedPassword_;
                keyStore.load(null, pin.toCharArray());
                String alias = signingCertificate_;
                //System.out.println("Alias: " + alias);
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                // System.out.println("Certificate in dongle: " + cert);
                privateKey = (PrivateKey) keyStore.getKey(alias, pin.toCharArray());
            }


            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            PrivateKey key = keyPair.getPrivate();
            System.out.println("Private Key: " + key);

           /* KeyStore keystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            keystore.load(null, null);

            key= (PrivateKey) keystore.getKey("MD.SOURAV HOSSEN", null);*/


            de.tsenger.vdstools.Signer signer = new de.tsenger.vdstools.Signer(key);
            // Create VDS Header and Message
            VdsHeader header = new VdsHeader.Builder(vdsType_)
                    .setIssuingCountry(issuingCountry_)
                    .setSignerIdentifier(signerIdentifier_)
                    .setCertificateReference(certificateReference_)
                    .setIssuingDate(LocalDate.parse(issuingDate_))
                    .setSigDate(LocalDate.parse(sigDate_))
                    .build();
            String firstName = firstName_;
            String cgpa = cgpa_;
            String link = "https://tinyurl.com/23vjc5fn";
            String university = university_;
            String division = division_;
            String id = id_;
            //String department = "CSE";
            VdsMessage vdsMessage = new VdsMessage.Builder(header.getVdsType())
                    .addDocumentFeature("FIRST_NAME", firstName)
                    .addDocumentFeature("CGPA", cgpa)
                    .addDocumentFeature("LINK", link)
                    .addDocumentFeature("University", university)
                    .addDocumentFeature("Division", division)
                    .addDocumentFeature("StudentID", id)
                    .build();

            // String data =Base64.getEncoder().encodeToString(signedData);

            PrivateKey privateKeyWindows = privateKeyService.getPrivateKey(signingCertificate_);
            System.out.println("Retrieved Private Key Windows: " + privateKeyWindows);

           /* String privateKeyBase64 = "MIIEvQIBADANerhehehKBJBJBXDTXTDXGVBLNKBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCLS38rF0NtDTEgqN+rby+Qj19UlFUNMunsOfrqneVKaMrBIjVsdUvwxnWFroXRj63zpny0mYlqASyWGvJqIDXPnH974YCEm4OXnxuDSM1S+1mPJ/sVx8Vhlb90BaYbZOVX6yOySuaDTSGC09MYRQcgh4ItPrGchUB/tweagjjphmhJkhuX8LlRhWczwxhbJZbDFSZPRlpRx6GtdZ5b9r1PTYAI52PPBKD4r++Bqx9WYRSvtNb5ATChqErt30MrlrIQEOI5Ye12wZAnO0gvGggSzXnGbIDXGfPIjkIEhsgjjp49/m1Qbx91Svw900hW+5NT3SSg850+8dAu+vtJmBAhAgMBAAECggEAD5k9vWqWf2DJzpl6qq0By5NynnsZ5yd7cJuxkJJr+eAwTF5zUxjzjxv6TyUTETugxDcxodvLupY1Ev4jhWTkAcLJt+qxxKfRnaoQn6wIajGBfvmAMZk0blAhp3F2f3zV/flz/uRKWgvKI+dqrT4VpTvnhP+PTjLR3Tt224OXsfbTMGW505ZzoYzsKHCTA9n51EvrqoickKPdH0xzeTngBg9p0rEum5OAc+qKC+koDUctKWBWQTp9dzWWWAbRfVa/Oy/QTQr+d6OKvCtwfA5HLCOCrQjmaGQD8viae7Ap313Q19eE+OWrjzRzcS8NleoHDnLhN6C1lAxQzBCGmdrubQKBgQDjm4usfe4/ha3UcTfG9/wkY1FoR9RiOb24oCxtnBWH8Yc7TonKfBpngMhVkC5rYUY05GUaaQkGU6Dm6y0R7Zc6GB7ITG8dziYXY4cd3TRFgmZmyBw6aXEs39rN2BSX9JPpNOaQGeKRccwUJ7TWax/rcenwnKvyzFJRuOTaqV6VTwKBgQCcq8LBPPOtmJ2k67/VpUOMvEfgi+r4yGNJZASM4m+NwoiixDyx0c/1DMgsgJn4anm0EU1yfOXT8nYfRXRu1qs1WJFSOvgzOZyt+EPbXUcA4kg1wHt6iDmGqBR65Ux+9rmt92U0TJtmCweoKQH9PeEiIL12IrMhZ3/Tj3gobRqHjwKBgBPrwUXPn9KfeJ9naWJYwhDNQIrH/qa6Nwi5vCm7x4amdReTwCugwQ7eDqque+GaGfL3KoItP0T2fNa5LrCrAtlq0wbk6bTKHjtd0q2idri+uQe17AKQx/8NeLEbgHHsTiXTI3rpSRNByoLZFtLNfXW1+qu8irAtgeb1L9KTkFuxAoGBAIlNcind3ASoOogdX4rCAhglraxZkvyiyXi5Ic/CZldLRGm5JyQDp4evwwJVVhrCXZR0kXYjhVuhIuo2+Vpl4benvfvd2EU0WV6RtA5ciex5YyVQYia5mgir5v7pU4f1fDa9GMGj3ZCpW/WAstCYWWSKYuBUer5ssTbchkaPj297AoGAMxAbhmZJd8ykb+2PKlVIFXC7yzuK2D8+uW0vODtM96rupGvfTknAF3l+hHq61muALlp3jbQEbxvuHbm1x7veryEyr1Kx3Fj6eMzotnMrifAMOZROhaJ7xK/eH8XyzMe+1Gha3TyU9iyDkvgoQFQECzL1QeNPFwNNtUlZy0ArJyQ=";
            // Decode the Base64 string into a byte array
            byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyBase64);
            // Convert the byte array into a PrivateKey object using PKCS8EncodedKeySpec
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey2 = keyFactory.generatePrivate(keySpec);*/

            // Create Digital Seal
            DigitalSeal digitalSeal = new DigitalSeal(header, vdsMessage, new Signer(privateKeyWindows));

            byte[] encodedBytes = digitalSeal.getEncoded();

            //System.out.println("Encoded Byte: " + encodedBytes);
            // Generate the barcode (Data Matrix)
            DataMatrixWriter dmw = new DataMatrixWriter();
            BitMatrix bitMatrix = dmw.encode(DataEncoder.encodeBase256(digitalSeal.getEncoded()), BarcodeFormat.DATA_MATRIX,
                    450, 450);


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            qrBytes = outputStream.toByteArray();



        } catch (Exception e) {
            e.printStackTrace();
        }
        return qrBytes;
    }
    @Data
    static class FormData {

        private String firstName ;
        private String cgpa ;
        private String university ;
        private String division ;
        private String id ;
        private String signingCertificate;
        private String vdsType;
        private String issuingCountry;
        private String signerIdentifier;
        private String certificateReference;
        private String issuingDate;
        private String sigDate;
        private String keyStoreIdentifier;
        private String storedPassword;

    }
}
