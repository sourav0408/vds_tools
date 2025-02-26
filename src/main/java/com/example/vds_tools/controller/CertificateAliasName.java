package com.example.vds_tools.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@RestController
@RequestMapping("/bcc-ca")
public class CertificateAliasName {

    private final String desktopAppUrl = "http://localhost:5003/get-aliases"; // Desktop app endpoint

    @GetMapping("/fetch-all-aliases")
    public ResponseEntity<Map<String, Object>> fetchAllCertificatesFromDesktopApp() {
        try {
            // Log the request for fetching certificates
            System.out.println("Fetching all aliases from Desktop App at: " + desktopAppUrl);

            // Create RestTemplate to call the desktop app
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(desktopAppUrl, Map.class);

            // Log the response from the desktop app
            System.out.println("Aliases received from Desktop App: " + response.getBody());

            // Return the certificates to the frontend
            return ResponseEntity.ok(response.getBody());
        } catch (RestClientException e) {
            // Log the error if the call fails
            System.err.println("Error while fetching certificates from Desktop App: " + e.getMessage());

            // Return an error response with details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to connect to Desktop App", "details", e.getMessage()));
        }
    }
}