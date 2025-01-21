package com.example.vds_tools.controller;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;


import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class PasswordController2 {

    @PostMapping("/submitPassword2")
    public Response submitPassword(@RequestBody PasswordRequest passwordRequest,HttpServletRequest request,HttpSession session ) throws IOException {
        String submittedPassword = passwordRequest.getPassword();

         session = request.getSession(false);
        if(session != null) {
          session.invalidate();
        }


        ClassPathResource resource = new ClassPathResource("pkcs11.cfg");
        File configFile = resource.getFile();
        String configPath = configFile.getAbsolutePath();

        Provider pkcs11Provider = Security.getProvider("SunPKCS11");
        pkcs11Provider = pkcs11Provider.configure(configPath);
        Security.addProvider(pkcs11Provider);

        try {
            // Step 1: Load the PKCS#11 provider using the configuration file
            //   Provider pkcs11Provider = Security.getProvider("SunPKCS11");
            pkcs11Provider = pkcs11Provider.configure(configPath);
            Security.addProvider(pkcs11Provider);

            // Step 2: Access the KeyStore (using PKCS#11)
            KeyStore keyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);

            // The PIN (password) for the dongle (this is what you're verifying)
             String pin = submittedPassword;  // Replace with the actual PIN/password
            //store pin in session



             //System.out.println("PIN:  "+pin);
            // Step 3: Attempt to load the KeyStore with the provided PIN
            pkcs11Provider = Security.getProvider("SunPKCS11").configure(configPath);
            Security.addProvider(pkcs11Provider);


            keyStore.load(null, pin.toCharArray());

            session = request.getSession(true);
            session.setAttribute("pin", pin);
            return new Response(true, "Password is correct.");

        } catch (Exception e) {
            // If an exception is thrown, the PIN is incorrect or there was another issue
            return new Response(false, "Password is incorrect.");
        }
    }

    // Response class to return the status
    public static class Response {
        private boolean success;
        private String message;

        public Response(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


    // PasswordRequest class to bind the request body
    public static class PasswordRequest {
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
