package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

/**
* Created by Floris on 12-7-2014.
*/
@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
@Data
public class Vak implements Serializable {
    private static final long serialVersionUID = 102947213471347263L;
    private final String naam;
    public final ArrayList<Leraar> leraren;

    public void setLeraren(Leraar ler) {
        leraren.add(ler);
    }

    public void setLeraren(ArrayList<Leraar> lers) {
        leraren.addAll(lers);
    }

    public Vak(String naam) {
        this.naam = naam;
        leraren = new ArrayList<>();
    }
}
