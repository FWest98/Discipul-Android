package com.thomasdh.roosterpgplus;

import java.util.ArrayList;

/**
 * Created by Floris on 5-12-13.
 */
public class JoinableList extends ArrayList<String> {
    public String join(final String joiner) {
        String s = "";
        for(int i = 0; i < this.size(); i++) {
            if(i == this.size()-1) { // laatste item
                s += this.get(i);
            } else {
                s += this.get(i) + joiner;
            }
        }
        return s;
    }
}
