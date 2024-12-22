package com.example.vds_tools.controller;

import com.google.zxing.BarcodeFormat;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.example.vds_tools.model.VdsFormModel;
import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.Signer;

import de.tsenger.vdstools.vds.DigitalSeal;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsMessage;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.bouncycastle.cms.RecipientId.password;

@Controller
public class VdsController {

    @GetMapping("/vds")// This maps the form to the /show-form URL
    public String showForm(Model model) {
        model.addAttribute("VdsFormModel", new VdsFormModel()); // Add an empty form model
        return "vdsForm";
    }

    @PostMapping("/process-form")
    public ResponseEntity<?> processForm(@RequestBody FormData formData) {



        System.out.println("hello" + formData);

        String firstName=formData.getFirstName() ;
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

        byte[] qr=generateDigitalSeal(firstName,cgpa,
                university,division,id,vdsType,issuingCountry,
                signerIdentifier,certificateReference,issuingDate,sigDate);

        // System.out.println("SOURAV"+qr);


        // Simulate processing the data
        //"Processed: " + formData.getField1() + " & " + formData.getField2();
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

    public static byte[] convertBitMatrixToByteArray(BitMatrix bitMatrix, String format) throws Exception {
        // Validate format
        if (!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG")) {
            throw new IllegalArgumentException("Unsupported image format: " + format);
        }

        // Convert BitMatrix to BufferedImage
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF); // Black or white
            }
        }

        // Write BufferedImage to ByteArrayOutputStream
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean success = ImageIO.write(image, format, baos);
            if (!success) {
                throw new RuntimeException("Failed to write image in format: " + format);
            }
            return baos.toByteArray();
        }
    }

    public byte[] generateDigitalSeal(
            String firstName_,
            String cgpa_,
            String university_,
            String division_,
            String id_,
            String vdsType_,
            String issuingCountry_,
            String signerIdentifier_,
            String certificateReference_,
            String issuingDate_,
            String sigDate_ ) {

        byte[] qrBytes = null;
        HttpHeaders headers = null;
        try {
            String password_ = "bccca";
            FileInputStream fis = new FileInputStream("D:\\D\\all_project\\VDS\\Certificate\\ahad_cert.p12");



            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, password_.toCharArray());
            Enumeration<String> aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                PrivateKey key = (PrivateKey) keyStore.getKey(alias, password_.toCharArray());

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
                String link = "https://tinyurl.com/f475a4x4";
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

                // Create Digital Seal
                DigitalSeal digitalSeal = new DigitalSeal(header, vdsMessage, new Signer(key));
                byte[] encodedBytes = digitalSeal.getEncoded();

                System.out.println("Encoded Byte: " + encodedBytes);
                // Generate the barcode (Data Matrix)
                DataMatrixWriter dmw = new DataMatrixWriter();
                BitMatrix bitMatrix = dmw.encode(DataEncoder.encodeBase256(digitalSeal.getEncoded()), BarcodeFormat.DATA_MATRIX,
                        450, 450);


                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
                qrBytes = outputStream.toByteArray();

                //headers = new HttpHeaders();
                // headers.set("Content-Type", "image/png");


                Path path = Path.of("D:\\D\\all_project\\VDS\\Certificate\\test.png");
                MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return qrBytes;
    }


    // Helper class for form data
    @Data
    static class FormData {
        //private String field1;
        //private String field2;


        private String firstName ;
        private String cgpa ;
        private String university ;
        private String division ;
        private String id ;

        private String vdsType;
        private String issuingCountry;
        private String signerIdentifier;
        private String certificateReference;
        private String issuingDate;
        private String sigDate;

   /*     // Getters and setters
        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setField2(String field2) {
            this.field2 = field2;
        }*/
    }
}
