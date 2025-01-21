package com.example.vds_tools.controller;
import com.example.vds_tools.model.Aliases;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.Signer;
import de.tsenger.vdstools.vds.DigitalSeal;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsMessage;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.*;

@Controller
public class VdsController {


    private HttpSession session;

    @GetMapping("/vds") // This maps the form to the /vds URL
    public String showForm(Model model)  {
        // Return the view (Thymeleaf template)
        return "vdsForm";  // Ensure this matches your actual Thymeleaf template name
    }


    @PostMapping("/process-form")
    public ResponseEntity<?> processForm(@RequestBody FormData formData) {



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


            if(keyStoreIdentifier_.equals("WINDOWS"))
            {
                KeyStore keystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
                keystore.load(null, null);
                String alias =signingCertificate_;
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

                String pin=storedPassword_;
                keyStore.load(null, pin.toCharArray());
                String alias =signingCertificate_;
                //System.out.println("Alias: " + alias);
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
               // System.out.println("Certificate in dongle: " + cert);
            }


            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            PrivateKey key = keyPair.getPrivate();
            System.out.println("Private Key: " + key);

                Signer signer = new Signer(key);
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
            String link="https://tinyurl.com/23vjc5fn";
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

                //System.out.println("Encoded Byte: " + encodedBytes);
                // Generate the barcode (Data Matrix)
                DataMatrixWriter dmw = new DataMatrixWriter();
                BitMatrix bitMatrix = dmw.encode(DataEncoder.encodeBase256(digitalSeal.getEncoded()), BarcodeFormat.DATA_MATRIX,
                        450, 450);


                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
                qrBytes = outputStream.toByteArray();


                //headers = new HttpHeaders();
                // headers.set("Content-Type", "image/png");


               // Path path = Path.of("D:\\D\\all_project\\VDS\\Certificate\\test.png");
                //MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);


          //  }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return qrBytes;
    }


   /* public static InputStream  getCertificateInputStream(String alias) throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        // Load the Windows KeyStore (Windows-MY)
        KeyStore windowsKeystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
        windowsKeystore.load(null, null);  // Load the keystore

        // Print out all aliases in the keystore
        Enumeration<String> aliases = windowsKeystore.aliases();
        while (aliases.hasMoreElements()) {
            String aliasInKeystore = aliases.nextElement();
            System.out.println("Alias in Keystore: " + aliasInKeystore);
        }
        char[] password = "bcca".toCharArray();  // Replace with the actual password
        PrivateKey privateKey = (PrivateKey) windowsKeystore.getKey(alias, password);
        // Check for the alias and get the certificate
        InputStream certStream = null;
        if (windowsKeystore.containsAlias(alias)) {
            Certificate certificate = windowsKeystore.getCertificate(alias);
            if (certificate != null) {
                System.out.println("Certificate found for alias: " + alias);
                byte[] certificateBytes = certificate.getEncoded();
                certStream = new ByteArrayInputStream(certificateBytes);
                System.out.println("Certificate InputStream is available.");
            } else {
                System.out.println("No certificate found for alias: " + alias);
            }
        } else {
            System.out.println("No alias found: " + alias);
        }
        return certStream;
    }*/

  public void generateCertificate( String alias1)
  {
      try {
          // Add BouncyCastle provider for PKCS12
          Security.addProvider(new BouncyCastleProvider());

          // Load the Windows KeyStore
          KeyStore windowsKeystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
          windowsKeystore.load(null, null);

          // Enumerate through the aliases in the Windows KeyStore
          Enumeration<String> aliases = windowsKeystore.aliases();
          while (aliases.hasMoreElements()) {
              String alias = aliases.nextElement();
              System.out.println("Alias in Keystore: " + alias);


              String aliaS = alias1;
              String outputFilePath = "src/main/resources/certificates/ahad_cert.p12";
              char[] pfxPassword = "bccca".toCharArray();


              if (aliaS.equals(alias)) {
                  System.out.println("Exporting certificate with alias: " + alias);

                  if (windowsKeystore.isKeyEntry(alias)) {
                      // Retrieve the private key and certificate chain
                      PrivateKey privateKey = (PrivateKey) windowsKeystore.getKey(alias, null);
                      Certificate[] certChain = windowsKeystore.getCertificateChain(alias);
                      System.out.println("Certificate chain length: " + certChain.length);

                      if (privateKey == null || certChain == null) {
                          System.out.println("No private key or certificate chain found for alias: " + alias);
                          return;
                      }

                      KeyStore pfxKeystore = KeyStore.getInstance("PKCS12", "BC");

                      pfxKeystore.load(null, null);

                      try  (FileOutputStream fos = new FileOutputStream(outputFilePath))
                      {
                          pfxKeystore.store(fos, pfxPassword);
                      }
                      catch (Exception e)
                      {
                          e.printStackTrace();
                      }

                      break;
                  }
              }
          }
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

    // Helper class for form data
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


//tiny url
/*    public static String createTinyURL(String originalUrl) throws IOException {
        String tinyURLAPI = "http://tinyurl.com/api-create.php?url=";
        // Encode the original URL to handle special characters
        String encodedUrl = URLEncoder.encode(originalUrl, "UTF-8");

        // Combine the TinyURL API with the encoded original URL
        URL url = new URL(tinyURLAPI + encodedUrl);

        // Open a connection to the TinyURL API
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get the Tiny URL from the response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String tinyUrl = reader.readLine(); // Read the response

        reader.close();
        return tinyUrl; // Return the shortened Tiny URL
    }*/
}
