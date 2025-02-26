package com.example.vds_tools.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RestController
@RequestMapping("/bcc-ca")
public class PrivateKeyController {

    private final String desktopAppUrl = "http://localhost:5003/get-private-key"; // Desktop app endpoint for private key

    @GetMapping("/fetch-private-key/{alias}")
    public ResponseEntity<Map<String, Object>> fetchPrivateKeyFromDesktopApp(@PathVariable String alias) {
        try {
            // Log the request for fetching the private key
            System.out.println("Fetching private key for alias '" + alias + "' from Desktop App at: " + desktopAppUrl);

            // Append the alias to the desktop app URL
            String urlWithAlias = desktopAppUrl + "?alias=" + alias;

            // Create RestTemplate to call the desktop app
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(urlWithAlias, Map.class);

            // Log the response from the desktop app
            System.out.println("Private key for alias '" + alias + "' received from Desktop App: " + response.getBody());

            // Return the private key to the frontend
            return ResponseEntity.ok(response.getBody());
        } catch (RestClientException e) {
            // Log the error if the call fails
            System.err.println("Error while fetching private key from Desktop App: " + e.getMessage());

            // Return an error response with details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to connect to Desktop App", "details", e.getMessage()));
        }
    }
}
