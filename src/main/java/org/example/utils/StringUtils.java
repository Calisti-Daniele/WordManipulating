package org.example.utils;

public class StringUtils {

    // Metodo per rendere la prima lettera maiuscola
    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str; // Se la stringa è null o vuota, restituisci la stringa originale
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}

