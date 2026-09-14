package org.erpklassup.erpklassup.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TotpUtil.
 * Vérifie la génération de secrets, la génération de codes TOTP,
 * et la validation de codes.
 */
class TotpUtilTest {

    private String secret;

    @BeforeEach
    void setUp() {
        // Génère un secret frais pour chaque test
        secret = TotpUtil.genererSecret();
    }

    // ==========================================
    // 1. GÉNÉRATION DE SECRET
    // ==========================================

    @Test
    @DisplayName("genererSecret() retourne une chaîne non nulle et non vide")
    void testGenererSecretNonNull() {
        String secret = TotpUtil.genererSecret();

        assertNotNull(secret, "Le secret ne doit pas être null");
        assertFalse(secret.isBlank(), "Le secret ne doit pas être vide");
    }

    @Test
    @DisplayName("genererSecret() retourne une chaîne en Base32 (32 caractères)")
    void testGenererSecretLongueur() {
        String secret = TotpUtil.genererSecret();

        // 20 octets en Base32 = 32 caractères (sans padding)
        assertEquals(32, secret.length(),
                "Un secret de 20 octets doit faire 32 caractères en Base32");
    }

    @Test
    @DisplayName("genererSecret() retourne une chaîne Base32 valide (A-Z, 2-7)")
    void testGenererSecretAlphabetBase32() {
        String secret = TotpUtil.genererSecret();

        // Base32 = A-Z + 2-7 uniquement
        assertTrue(secret.matches("[A-Z2-7]+"),
                "Le secret doit contenir uniquement des caractères Base32 (A-Z, 2-7)");
    }

    @Test
    @DisplayName("genererSecret() retourne 2 secrets différents à chaque appel")
    void testGenererSecretUnicite() {
        Set<String> secrets = new HashSet<>();

        // Génère 100 secrets → tous doivent être différents
        for (int i = 0; i < 100; i++) {
            secrets.add(TotpUtil.genererSecret());
        }

        assertEquals(100, secrets.size(),
                "Tous les secrets générés doivent être uniques");
    }

    // ==========================================
    // 2. GÉNÉRATION DE CODE TOTP
    // ==========================================

    @Test
    @DisplayName("genererCode() retourne un code à 6 chiffres")
    void testGenererCodeFormat() {
        String code = TotpUtil.genererCode(secret);

        assertNotNull(code, "Le code ne doit pas être null");
        assertEquals(6, code.length(), "Le code doit faire 6 caractères");
        assertTrue(code.matches("\\d{6}"),
                "Le code doit contenir uniquement 6 chiffres");
    }

    @Test
    @DisplayName("genererCode() retourne le même code pour le même secret à un instant proche")
    void testGenererCodeStable() {
        String code1 = TotpUtil.genererCode(secret);
        String code2 = TotpUtil.genererCode(secret);

        // En moins de 30 secondes, le code doit être identique
        assertEquals(code1, code2,
                "Le code TOTP doit être stable dans une même fenêtre de 30s");
    }

    @Test
    @DisplayName("genererCode() retourne des codes différents pour des secrets différents")
    void testGenererCodeSecretsDifferents() {
        String secret1 = TotpUtil.genererSecret();
        String secret2 = TotpUtil.genererSecret();

        String code1 = TotpUtil.genererCode(secret1);
        String code2 = TotpUtil.genererCode(secret2);

        assertNotEquals(code1, code2,
                "Deux secrets différents doivent produire des codes différents");
    }

    // ==========================================
    // 3. VÉRIFICATION DE CODE
    // ==========================================

    @Test
    @DisplayName("verifierCode() accepte un code valide généré immédiatement")
    void testVerifierCodeValide() {
        String codeValide = TotpUtil.genererCode(secret);

        assertTrue(TotpUtil.verifierCode(secret, codeValide),
                "Le code généré doit être accepté immédiatement");
    }

    @Test
    @DisplayName("verifierCode() refuse un code à 5 chiffres (mauvais format)")
    void testVerifierCodeTropCourt() {
        assertFalse(TotpUtil.verifierCode(secret, "12345"),
                "Un code à 5 chiffres doit être refusé");
    }

    @Test
    @DisplayName("verifierCode() refuse un code à 7 chiffres (mauvais format)")
    void testVerifierCodeTropLong() {
        assertFalse(TotpUtil.verifierCode(secret, "1234567"),
                "Un code à 7 chiffres doit être refusé");
    }

    @Test
    @DisplayName("verifierCode() refuse un code non numérique")
    void testVerifierCodeNonNumerique() {
        assertFalse(TotpUtil.verifierCode(secret, "ABCDEF"),
                "Un code non numérique doit être refusé");
    }

    @Test
    @DisplayName("verifierCode() refuse un code null")
    void testVerifierCodeNull() {
        assertFalse(TotpUtil.verifierCode(secret, null),
                "Un code null doit être refusé");
    }

    @Test
    @DisplayName("verifierCode() refuse un secret null")
    void testVerifierCodeSecretNull() {
        assertFalse(TotpUtil.verifierCode(null, "123456"),
                "Un secret null doit être refusé");
    }

    @Test
    @DisplayName("verifierCode() refuse un code erroné avec un fort taux de certitude")
    void testVerifierCodeErrone() {
        String codeValide = TotpUtil.genererCode(secret);

        // Génère un code "faux" qui diffère du code valide
        String codeErrone = String.format("%06d",
                (Integer.parseInt(codeValide) + 12345) % 1_000_000);

        assertNotEquals(codeValide, codeErrone, "Le code erroné doit différer");

        // ⚠️ Très rare qu'un code erroné soit accepté (fenêtre de tolérance ±30s)
        // On teste 100 codes erronés pour s'assurer qu'aucun n'est accepté
        for (int i = 0; i < 100; i++) {
            String faux = String.format("%06d", i);
            if (!faux.equals(codeValide)) {
                assertFalse(TotpUtil.verifierCode(secret, faux),
                        "Le code " + faux + " ne doit pas être accepté");
            }
        }
    }

    @Test
    @DisplayName("verifierCode() accepte les espaces autour du code")
    void testVerifierCodeAvecEspaces() {
        String codeValide = TotpUtil.genererCode(secret);

        assertTrue(TotpUtil.verifierCode(secret, "  " + codeValide + "  "),
                "Les espaces autour du code doivent être ignorés");
    }

    // ==========================================
    // 4. CODES DE SECOURS
    // ==========================================

    @Test
    @DisplayName("genererCodesSecours() retourne 8 codes")
    void testGenererCodesSecoursNombre() {
        List<String> codes = TotpUtil.genererCodesSecours();

        assertNotNull(codes, "La liste ne doit pas être null");
        assertEquals(8, codes.size(), "Il doit y avoir 8 codes de secours");
    }

    @Test
    @DisplayName("genererCodesSecours() retourne des codes à 8 chiffres")
    void testGenererCodesSecoursFormat() {
        List<String> codes = TotpUtil.genererCodesSecours();

        for (String code : codes) {
            assertNotNull(code, "Le code ne doit pas être null");
            assertEquals(8, code.length(), "Chaque code doit faire 8 caractères");
            assertTrue(code.matches("\\d{8}"),
                    "Chaque code doit contenir uniquement 8 chiffres");
        }
    }

    @Test
    @DisplayName("genererCodesSecours() retourne 8 codes uniques")
    void testGenererCodesSecoursUnicite() {
        List<String> codes = TotpUtil.genererCodesSecours();
        Set<String> uniques = new HashSet<>(codes);

        assertEquals(8, uniques.size(),
                "Tous les codes de secours doivent être uniques");
    }

    @Test
    @DisplayName("genererCodesSecours() appelé 2 fois retourne des lots différents")
    void testGenererCodesSecoursEntreAppels() {
        List<String> lot1 = TotpUtil.genererCodesSecours();
        List<String> lot2 = TotpUtil.genererCodesSecours();

        // Il est possible qu'un code coïncide par hasard, mais l'ensemble doit différer
        assertNotEquals(lot1, lot2,
                "Deux lots consécutifs doivent être différents");
    }

    // ==========================================
    // 5. TEST D'INTÉGRATION : FLUX COMPLET
    // ==========================================

    @Test
    @DisplayName("Flux complet : générer un secret → générer un code → le vérifier")
    void testFluxComplet2FA() {
        // 1. L'utilisateur s'enregistre → on génère un secret
        String secretUtilisateur = TotpUtil.genererSecret();
        assertNotNull(secretUtilisateur);

        // 2. L'utilisateur scanne le QR → son app génère un code
        String codeGenere = TotpUtil.genererCode(secretUtilisateur);

        // 3. L'utilisateur saisit le code → on le vérifie
        assertTrue(TotpUtil.verifierCode(secretUtilisateur, codeGenere),
                "Le code généré doit être accepté");

        // 4. Un attaquant essaie un autre code → refusé
        String codeAttaquant = String.format("%06d",
                (Integer.parseInt(codeGenere) + 1) % 1_000_000);
        assertFalse(TotpUtil.verifierCode(secretUtilisateur, codeAttaquant),
                "Un code voisin doit être refusé");
    }
}