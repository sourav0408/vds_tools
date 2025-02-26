package com.example.vds_tools.model;

import lombok.Data;

@Data
public class Aliases {
    private String alias;
    private String certificateActualName;

    public Aliases(String alias, String certificateActualName) {
        this.alias = alias;
        this.certificateActualName = certificateActualName;
    }
}
