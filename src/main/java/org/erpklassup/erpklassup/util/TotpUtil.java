package org.erpklassup.erpklassup.util;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class TotpUtil {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final String ALGORITHM = "HmacSHA1";

    /**
     * Génère une clé secrète aléatoire de 160 bits encodée en Base32.
     */
    public static String genererSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        Base32 base32 = new Base32();
        return base32.encodeToString(buffer).replace("=", "");
    }

    /**
     * Génère le code à 6 chiffres pour une clé secrète à l'instant T.
     */
    public static String genererCode(String secretBase32) {
        long timeWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        return genererCodePourFenetre(secretBase32, timeWindow);
    }

    /**
     * Vérifie si le code saisi par l'utilisateur est valide (avec une marge d'erreur de ±1 intervalle de 30s).
     */
    public static boolean verifierCode(String secretBase32, String codeSaisi) {
        if (secretBase32 == null || codeSaisi == null || codeSaisi.trim().length() != DIGITS) {
            return false;
        }

        long timeWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

        // Tolérance de ±1 intervalle (30s avant / 30s après) pour compenser les décalages d'horloge
        for (int i = -1; i <= 1; i++) {
            String codeCalcule = genererCodePourFenetre(secretBase32, timeWindow + i);
            if (codeCalcule.equals(codeSaisi.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Génère une liste de codes de secours à usage unique (ex: 8 codes de 8 caractères).
     */
    public static List<String> genererCodesSecours() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 8; i++) {
            int code = 10000000 + random.nextInt(90000000);
            codes.add(String.valueOf(code));
        }
        return codes;
    }

    // ========== ALGORITHME INTERNE TOTP (RFC 6238 / RFC 4226) ==========
    private static String genererCodePourFenetre(String secretBase32, long timeWindow) {
        try {
            Base32 base32 = new Base32();
            byte[] key = base32.decode(secretBase32);

            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (timeWindow & 0xFF);
                timeWindow >>= 8;
            }

            SecretKeySpec signKey = new SecretKeySpec(key, ALGORITHM);
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= Math.pow(10, DIGITS);

            return String.format("%0" + DIGITS + "d", truncatedHash);
        } catch (Exception e) {
            System.err.println("❌ Erreur génération TOTP : " + e.getMessage());
            return "";
        }
    }
}
