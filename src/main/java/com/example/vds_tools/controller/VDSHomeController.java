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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Controller
public class VDSHomeController {

    @Autowired
    private WindowsPrivateKeyService privateKeyService;

    @GetMapping("/VDS_Form") // This maps the form to the /vds URL
    public String showForm(Model model)  {
        // Return the view (Thymeleaf template)
        return "vds_home_page";  // Ensure this matches your actual Thymeleaf template name
    }


    @PostMapping("/vds-form_to_send")
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

        String base64QRCode=generateDigitalSeal(firstName,cgpa,signingCertificate,
                university,division,id,vdsType,issuingCountry,
                signerIdentifier,certificateReference,issuingDate,sigDate,storedPassword,keyStoreIdentifier);

      //  String base64QRCode = Base64.getEncoder().encodeToString(qr);
        String processedResult = base64QRCode;


        Map<String, Object> response = new HashMap<>();
        response.put("message", processedResult);
        response.put("qrCode", processedResult); // Add the QR byte array to the response

        // Return the response with the QR code and message
        return ResponseEntity.ok(response);
    }

    public String generateDigitalSeal(
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
        ResponseEntity<String> response = null;
        try {


            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:5003/receive-data";

            Map<String, Object> dataToSend = new HashMap<>();
            dataToSend.put("firstName", firstName_);
            dataToSend.put("signingCertificate", signingCertificate_);
            dataToSend.put("university", university_);
            dataToSend.put("division", division_);
            dataToSend.put("id", id_);
            dataToSend.put("vdsType", vdsType_);
            dataToSend.put("issuingCountry", issuingCountry_);
            dataToSend.put("signerIdentifier", signerIdentifier_);
            dataToSend.put("certificateReference", certificateReference_);
            dataToSend.put("issuingDate", issuingDate_);
            dataToSend.put("sigDate", sigDate_);
            dataToSend.put("storedPassword", storedPassword_);
            dataToSend.put("keyStoreIdentifier", keyStoreIdentifier_);
            dataToSend.put("cgpa", cgpa_);


            response = restTemplate.postForEntity(url, dataToSend, String.class);
            System.out.println("Response from desktop app: " + response.getBody());


            // Step 1: Create some dummy data to represent the "encoded" digital seal
            String dummySealData = "wefewfwfwfwefwef";  // Dummy data representing the digital seal

            // Step 2: Convert the dummy data into a byte array (this simulates digitalSeal.getEncoded())
            byte[] dummyEncodedBytes = dummySealData.getBytes("UTF-8");

            // Step 3: Simulate DataEncoder.encodeBase256() by encoding the bytes as Base256 (or Base64 for simplicity here)
            byte[] base256EncodedData = Base64.getEncoder().encode(dummyEncodedBytes);

            // Step 4: Create the Data Matrix barcode with the encoded data
            DataMatrixWriter dmw = new DataMatrixWriter();
            BitMatrix bitMatrix = dmw.encode(new String(base256EncodedData), BarcodeFormat.DATA_MATRIX, 450, 450);

            // Now you have a BitMatrix that you can render or process further.
            System.out.println("Data Matrix barcode generated.");


/// / can i receive it here?


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            qrBytes = outputStream.toByteArray();


        } catch (Exception e) {
            e.printStackTrace();
        }
        String responseBody = response.getBody();
        return responseBody;
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

