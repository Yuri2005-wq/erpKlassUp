package org.erpklassup.erpklassup.service;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordService {

    private static final int COUT_BCRYPT = 12;

    public static String hacher(String motDePasse) {
        return BCrypt.withDefaults().hashToString(COUT_BCRYPT, motDePasse.toCharArray());
    }

    public static boolean verifier(String motDePasse, String hash) {
        BCrypt.Result result = BCrypt.verifyer().verify(motDePasse.toCharArray(), hash);
        return result.verified;
    }
}