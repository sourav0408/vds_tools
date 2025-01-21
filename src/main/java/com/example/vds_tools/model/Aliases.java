package com.example.vds_tools.model;

import lombok.Data;

@Data
public class Aliases {
    private String alias;
    public Aliases(String alias) {
        this.alias = alias;
    }
}
