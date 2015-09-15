package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;

import lombok.Data;

@Data
public class Lokaal implements Serializable {
    private static final long serialVersionUID = 102947212471432253L;
    private final String code;
    public final String naam;

    public Lokaal(String code, String naam) {
        this.code = code;
        this.naam = naam;
    }
}
