package com.example.vds_tools.model;

import lombok.Data;

@Data
public class CertificateDTO {
    private String alias;
    private String certificateActualName;

    public CertificateDTO(String alias, String certificateActualName) {
        this.alias = alias;
        this.certificateActualName = certificateActualName;
    }



}
