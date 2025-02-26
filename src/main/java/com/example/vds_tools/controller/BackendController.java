package com.example.vds_tools.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestController
@RequestMapping("/bcc-ca")
@CrossOrigin(origins = "*") // Allow requests from all origins
public class BackendController {

    private final String desktopAppUrl = "http://localhost:5003/get-certificates"; // Desktop app endpoint

    @GetMapping("/fetch-certificates")
    public ResponseEntity<Map<String, Object>> fetchCertificatesFromDesktopApp() {
        try {
            // Log the request for fetching certificates
            System.out.println("Fetching certificates from Desktop App at: " + desktopAppUrl);

            // Create RestTemplate to call the desktop app
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(desktopAppUrl, Map.class);

            // Log the response from the desktop app
            System.out.println("Certificates received from Desktop App: " + response.getBody());

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

/*
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestController
@RequestMapping("/bcc-ca")
@CrossOrigin(origins = "*") // Allow requests from all origins
public class BackendController {

    private final String desktopAppUrl = "http://localhost:5003"; // Base URL of the Desktop App

    // Endpoint for fetching certificates (Base64 encoded)
    @GetMapping("/fetch-certificates")
    public ResponseEntity<Map<String, Object>> fetchCertificatesFromDesktopApp() {
        String url = desktopAppUrl + "/get-certificates";
        return fetchData(url, "fetching certificates");
    }

    // Endpoint for fetching aliases of certificates
    @GetMapping("/fetch-aliases")
    public ResponseEntity<Map<String, Object>> fetchAliasesFromDesktopApp() {
        String url = desktopAppUrl + "/get-aliases";
        return fetchData(url, "fetching aliases");
    }

    // Endpoint for fetching private keys (Base64 encoded)
    @GetMapping("/fetch-private-keys")
    public ResponseEntity<Map<String, Object>> fetchPrivateKeysFromDesktopApp() {
        String url = desktopAppUrl + "/get-private-keys";
        return fetchData(url, "fetching private keys");
    }

    // Helper method to fetch data from the desktop app
    private ResponseEntity<Map<String, Object>> fetchData(String url, String action) {
        try {
            // Log the request action
            System.out.println("Attempting to " + action + " from Desktop App at: " + url);

            // Create RestTemplate to call the desktop app
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            // Log the successful response
            System.out.println(action + " received from Desktop App: " + response.getBody());

            // Return the response body to the frontend
            return ResponseEntity.ok(response.getBody());
        } catch (RestClientException e) {
            // Log the error if the call fails
            System.err.println("Error while " + action + " from Desktop App: " + e.getMessage());

            // Return an error response with details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to connect to Desktop App", "details", e.getMessage()));
        }
    }
}*/

