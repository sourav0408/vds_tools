package com.example.vds_tools.service;

import com.example.vds_tools.controller.PrivateKeyController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Base64;
import java.util.Map;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

@Service
public class WindowsPrivateKeyService {

    private final PrivateKeyController privateKeyController;

    public WindowsPrivateKeyService(PrivateKeyController privateKeyController) {
        this.privateKeyController = privateKeyController;
    }

    public PrivateKey getPrivateKey(String alias) throws Exception {
        ResponseEntity<Map<String, Object>> response = privateKeyController.fetchPrivateKeyFromDesktopApp(alias);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String privateKeyBase64 = (String) response.getBody().get("privateKey");  // Adjust key name based on response

            byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } else {
            throw new RuntimeException("Failed to retrieve private key.");
        }
    }
}
