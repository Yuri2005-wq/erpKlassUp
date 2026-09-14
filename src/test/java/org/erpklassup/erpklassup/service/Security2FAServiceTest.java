package org.erpklassup.erpklassup.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Security2FAServiceTest {

    private Security2FAService service;

    @BeforeEach
    void setUp() {
        service = new Security2FAService();
    }

    @Test
    @DisplayName("genererCleSecrete() retourne une clé non nulle")
    void testGenererCleSecrete() {
        String cle = service.genererCleSecrete();

        assertNotNull(cle, "La clé ne doit pas être null");
        assertFalse(cle.isBlank(), "La clé ne doit pas être vide");
    }

    @Test
    @DisplayName("verifierCodeTOTP() accepte un code valide")
    void testVerifierCodeValide() {
        String cle = service.genererCleSecrete();
        int codeValide = new GoogleAuthenticator().getTotpPassword(cle);

        assertTrue(service.verifierCodeTOTP(cle, codeValide),
                "Le code généré doit être accepté");
    }

    @Test
    @DisplayName("verifierCodeTOTP() refuse un code invalide")
    void testVerifierCodeInvalide() {
        String cle = service.genererCleSecrete();

        assertFalse(service.verifierCodeTOTP(cle, 000000),
                "Le code 000000 doit être refusé (statistiquement)");
    }

    @Test
    @DisplayName("verifierCodeTOTP() refuse une clé null")
    void testVerifierCleNull() {
        assertFalse(service.verifierCodeTOTP(null, 123456),
                "Une clé null doit être refusée");
    }
}