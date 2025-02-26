package com.example.vds_tools.controller;

import com.example.vds_tools.model.Aliases;
import com.example.vds_tools.model.CertificateDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

@RestController
@RequestMapping("/api/aliases")
public class ClientAliasController {
    @Autowired
    private HttpSession session;
    List<Aliases> aliases = new ArrayList<>();

    @GetMapping("/{keyStoreIdentifier}")
    public ResponseEntity<List<Aliases>> getAliases(@PathVariable String keyStoreIdentifier) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        // Replace this with your actual logic to fetch certificates
System.out.println(keyStoreIdentifier);

        aliases.clear();
        System.out.println("Hello man:"+keyStoreIdentifier);
        if ("WINDOWS".equalsIgnoreCase(keyStoreIdentifier)) {
            getWindowsKeystoreAliases();
        } else if ("DONGLE".equalsIgnoreCase(keyStoreIdentifier)) {
            getDongleKeystoreAliases();
        }
//System.out.println("Certificate" + certificates);
        return ResponseEntity.ok(aliases);
    }


    public void getWindowsKeystoreAliases() {
        String AppUrl = "http://localhost:5003/get-aliases";

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(AppUrl, Map.class);

        // Assuming the response contains a key "aliases" with a List<String>
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();

            // Assuming the response contains a key "aliases" with a List<String>
            List<String> aliasNames = (List<String>) responseBody.get("aliases");

            if (aliasNames != null) {
                for (String namewithdate : aliasNames) {
                    String aliasName = namewithdate.split(" \\| Issued On: ")[0];
                    String aliasNameWithdate=namewithdate;
                    System.out.println("Actual"+aliasName);
                    System.out.println("Withdate "+aliasNameWithdate);

                    aliases.add(new Aliases(aliasNameWithdate,aliasName));
                  //  aliases.add(new Aliases(namewithdate,namewithdate));
                }
            }
        }

        // Log the response from the desktop app
        System.out.println("Aliases received from Desktop App: " + response.getBody());

    }

    public void getDongleKeystoreAliases() {
       //rivate final String desktopAppUrl = "http://localhost:5003/get-aliases";

    }
}
