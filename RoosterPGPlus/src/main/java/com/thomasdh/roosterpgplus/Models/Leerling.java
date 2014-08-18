package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;

import lombok.Data;

@Data
public class Leerling implements Serializable {
    private static final long serialVersionUID = 102947212471347263L;

    public String llnr;
    public String naam;

    public Leerling(String llnr, String naam) {
        this.llnr = llnr;
        this.naam = naam;
    }
}
