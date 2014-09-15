package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;

import lombok.Data;

/**
* Created by Floris on 12-7-2014.
*/
@Data
public class Week implements Serializable {
    private static final long serialVersionUID = 102947213471347253L;
    public final int week;
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean vakantieweek;

    public Week(int week, boolean vakantieweek) {
        this.week = week;
        this.vakantieweek = vakantieweek;
    }
}
