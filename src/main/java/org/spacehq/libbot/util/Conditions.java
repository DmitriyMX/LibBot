package org.spacehq.libbot.util;

import org.spacehq.libbot.module.ModuleException;

public class Conditions {
    public static void notNull(Object o, String name) {
        if(o == null) {
            throw new ModuleException(name + " cannot be null.");
        }
    }

    public static void notNullOrEmpty(String s, String name) {
        if(s == null || s.isEmpty()) {
            throw new ModuleException(name + " cannot be null or empty.");
        }
    }
}
