package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;

import lombok.Data;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
@Data
public class Leerling implements Serializable {
    private static final long serialVersionUID = 102947212471347263L;

    private String llnr;
    private String naam;

    public Leerling(String llnr, String naam) {
        this.llnr = llnr;
        this.naam = naam;
    }
}
