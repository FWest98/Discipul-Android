package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;

import lombok.Data;


@Data
public class Leraar implements Serializable {
    private static final long serialVersionUID = 102947212471347253L;
    private final String code;
    public final String naam;

    public Leraar(String code, String naam) {
        this.code = code;
        this.naam = naam;
    }
}
