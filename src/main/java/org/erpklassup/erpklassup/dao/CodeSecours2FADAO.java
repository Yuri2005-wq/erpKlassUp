package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.service.PasswordService;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO dédié aux codes de secours 2FA.
 * Les codes sont TOUJOURS hachés en BDD (bcrypt) — jamais stockés en clair.
 */
public class CodeSecours2FADAO {

    // ==========================================
    // GÉNÉRATION + ENREGISTREMENT (haché)
    // ==========================================

    /**
     * Génère N codes de secours à 10 chiffres, les stocke HACHÉS en BDD,
     * et retourne les codes EN CLAIR (à afficher une seule fois).
     */
    public List<String> genererEtEnregistrer(String idUtilisateur, int nombre) {
        List<String> codesEnClair = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        // 1. Supprimer les anciens codes avant d'en générer de nouveaux
        supprimerTousLesCodes(idUtilisateur);

        String sql = """
            INSERT INTO CodeSecours2FA (idCodeSecours, idUtilisateur, codeHash, estUtilise)
            VALUES (?, ?, ?, 0)
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < nombre; i++) {
                // Code à 10 chiffres (ex: "0428173596")
                long valeur = random.nextLong(10_000_000_000L);
                String code = String.format("%010d", valeur);
                codesEnClair.add(code);

                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, idUtilisateur);
                stmt.setString(3, PasswordService.hacher(code));   // ✅ HACHÉ
                stmt.addBatch();
            }

            stmt.executeBatch();

        } catch (SQLException e) {
            System.err.println("❌ Erreur genererEtEnregistrer : " + e.getMessage());
            throw new RuntimeException("Impossible de générer les codes de secours", e);
        }

        return codesEnClair;
    }

    // ==========================================
    // VÉRIFICATION + CONSOMMATION (hash bcrypt)
    // ==========================================

    /**
     * Vérifie un code de secours saisi en le comparant aux hashs en BDD.
     * Si un hash correspond, marque le code comme utilisé et retourne true.
     *
     * ⚠️ Le code en clair n'est JAMAIS stocké — on ne peut pas faire de SELECT = direct.
     * Il faut parcourir tous les hashs non utilisés et comparer avec bcrypt.
     */
    public boolean verifierEtConsommer(String idUtilisateur, String codeSaisi) {
        if (idUtilisateur == null || codeSaisi == null || codeSaisi.trim().isEmpty()) {
            return false;
        }

        String codeNettoye = codeSaisi.trim();

        String sqlSelect = """
            SELECT idCodeSecours, codeHash
            FROM CodeSecours2FA
            WHERE idUtilisateur = ? AND estUtilise = 0
            """;

        String sqlUpdate = """
            UPDATE CodeSecours2FA
            SET estUtilise = 1, dateUtilisation = CURRENT_TIMESTAMP
            WHERE idCodeSecours = ?
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect)) {

            stmtSelect.setString(1, idUtilisateur);

            try (ResultSet rs = stmtSelect.executeQuery()) {
                while (rs.next()) {
                    String idCode = rs.getString("idCodeSecours");
                    String hash = rs.getString("codeHash");

                    // ✅ Comparaison bcrypt (PasswordService.verifier)
                    if (PasswordService.verifier(codeNettoye, hash)) {
                        // Marquer comme utilisé
                        try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
                            stmtUpdate.setString(1, idCode);
                            stmtUpdate.executeUpdate();
                        }
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur verifierEtConsommer : " + e.getMessage());
        }
        return false;
    }

    // ==========================================
    // UTILITAIRES
    // ==========================================

    public void supprimerTousLesCodes(String idUtilisateur) {
        String sql = "DELETE FROM CodeSecours2FA WHERE idUtilisateur = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur supprimerTousLesCodes : " + e.getMessage());
        }
    }

    /**
     * Compte les codes de secours restants (non utilisés).
     */
    public int compterCodesRestants(String idUtilisateur) {
        String sql = "SELECT COUNT(*) FROM CodeSecours2FA WHERE idUtilisateur = ? AND estUtilise = 0";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterCodesRestants : " + e.getMessage());
        }
        return 0;
    }
}