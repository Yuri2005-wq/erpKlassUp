package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.dto.FiltreAudit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogsAuditDAO {

    private static final int LIMITE_RESULTATS = 1000;

    public List<AuditLigne> rechercher(FiltreAudit filtre) {
        StringBuilder sql = new StringBuilder("""
                SELECT idLogAudit, dateAction, nomAuteur, categorieAction, action, details, adresseIp, appareil
                FROM LogsAudit
                WHERE idEcole = ?
                """);
        List<Object> parametres = new ArrayList<>();
        parametres.add(filtre.idEcole());

        if (filtre.terme() != null && !filtre.terme().isBlank()) {
            sql.append(" AND (nomAuteur LIKE ? OR action LIKE ? OR details LIKE ?) ");
            String motif = "%" + filtre.terme().trim() + "%";
            parametres.add(motif);
            parametres.add(motif);
            parametres.add(motif);
        }
        if (filtre.dateDebut() != null) {
            sql.append(" AND dateAction >= ? ");
            parametres.add(Timestamp.valueOf(filtre.dateDebut().atStartOfDay()));
        }
        if (filtre.dateFin() != null) {
            sql.append(" AND dateAction < ? ");
            parametres.add(Timestamp.valueOf(filtre.dateFin().plusDays(1).atStartOfDay()));
        }
        if (filtre.categorie() != null && !filtre.categorie().isBlank()) {
            sql.append(" AND categorieAction = ? ");
            parametres.add(filtre.categorie());
        }

        sql.append(" ORDER BY dateAction DESC LIMIT ").append(LIMITE_RESULTATS);

        List<AuditLigne> lignes = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int index = 1;
            for (Object param : parametres) {
                stmt.setObject(index++, param);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lignes.add(new AuditLigne(
                            rs.getString("idLogAudit"),
                            rs.getTimestamp("dateAction").toLocalDateTime(),
                            rs.getString("nomAuteur"),
                            rs.getString("categorieAction"),
                            rs.getString("action"),
                            rs.getString("details"),
                            rs.getString("adresseIp"),
                            rs.getString("appareil")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur rechercher (LogsAudit) : " + e.getMessage());
        }
        return lignes;
    }

    public List<String> listerCategoriesDistinctes(String idEcole) {
        String sql = "SELECT DISTINCT categorieAction FROM LogsAudit WHERE idEcole = ? ORDER BY categorieAction";
        List<String> categories = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("categorieAction") != null) {
                        categories.add(rs.getString("categorieAction"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur listerCategoriesDistinctes : " + e.getMessage());
        }
        return categories;
    }

    public void inserer(String idEcole, String idUtilisateur, String nomAuteur, String categorie,
                        String action, String details, String adresseIp, String appareil) {
        String sql = "INSERT INTO LogsAudit (idLogAudit, idEcole, idUtilisateur, nomAuteur, categorieAction, " +
                "action, details, adresseIp, appareil) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, idEcole);
            stmt.setString(3, idUtilisateur);
            stmt.setString(4, nomAuteur);
            stmt.setString(5, categorie);
            stmt.setString(6, action);
            stmt.setString(7, details);
            stmt.setString(8, adresseIp);
            stmt.setString(9, appareil);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur inserer (LogsAudit) : " + e.getMessage());
        }
    }
}
//package org.erpklassup.erpklassup.dao;
//
//import org.erpklassup.erpklassup.Database;
//import org.erpklassup.erpklassup.dto.AuditLigne;
//import org.erpklassup.erpklassup.dto.FiltreAudit;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//public class LogsAuditDAO {
//
//    private static final int LIMITE_RESULTATS = 1000;
//
//    public List<AuditLigne> rechercher(FiltreAudit filtre) {
//        StringBuilder sql = new StringBuilder("""
//                SELECT idLogAudit, dateAction, nomAuteur, categorieAction, action, details, adresseIp, appareil
//                FROM LogsAudit
//                WHERE idEcole = ?
//                """);
//        List<Object> parametres = new ArrayList<>();
//        parametres.add(filtre.idEcole());
//
//        if (filtre.terme() != null && !filtre.terme().isBlank()) {
//            sql.append(" AND (nomAuteur LIKE ? OR action LIKE ? OR details LIKE ?) ");
//            String motif = "%" + filtre.terme().trim() + "%";
//            parametres.add(motif);
//            parametres.add(motif);
//            parametres.add(motif);
//        }
//        if (filtre.dateDebut() != null) {
//            sql.append(" AND dateAction >= ? ");
//            parametres.add(Timestamp.valueOf(filtre.dateDebut().atStartOfDay()));
//        }
//        if (filtre.dateFin() != null) {
//            sql.append(" AND dateAction < ? ");
//            parametres.add(Timestamp.valueOf(filtre.dateFin().plusDays(1).atStartOfDay()));
//        }
//        if (filtre.categorie() != null && !filtre.categorie().isBlank()) {
//            sql.append(" AND categorieAction = ? ");
//            parametres.add(filtre.categorie());
//        }
//
//        sql.append(" ORDER BY dateAction DESC LIMIT ").append(LIMITE_RESULTATS);
//
//        List<AuditLigne> lignes = new ArrayList<>();
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
//
//            int index = 1;
//            for (Object param : parametres) {
//                stmt.setObject(index++, param);
//            }
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    lignes.add(new AuditLigne(
//                            rs.getString("idLogAudit"),
//                            rs.getTimestamp("dateAction").toLocalDateTime(),
//                            rs.getString("nomAuteur"),
//                            rs.getString("categorieAction"),
//                            rs.getString("action"),
//                            rs.getString("details"),
//                            rs.getString("adresseIp"),
//                            rs.getString("appareil")
//                    ));
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur rechercher (LogsAudit) : " + e.getMessage());
//        }
//        return lignes;
//    }
//
//    public List<String> listerCategoriesDistinctes(String idEcole) {
//        String sql = "SELECT DISTINCT categorieAction FROM LogsAudit WHERE idEcole = ? ORDER BY categorieAction";
//        List<String> categories = new ArrayList<>();
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setString(1, idEcole);
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    if (rs.getString("categorieAction") != null) {
//                        categories.add(rs.getString("categorieAction"));
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur listerCategoriesDistinctes : " + e.getMessage());
//        }
//        return categories;
//    }
//
//    public void inserer(String idEcole, String idUtilisateur, String nomAuteur, String categorie,
//                        String action, String details, String adresseIp, String appareil) {
//        // 1. Ajout de la colonne dateAction dans le fichier SQL
//        String sql = "INSERT INTO LogsAudit (idLogAudit, idEcole, idUtilisateur, nomAuteur, categorieAction, " +
//                "action, details, adresseIp, appareil, dateAction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setString(1, UUID.randomUUID().toString());
//            stmt.setString(2, idEcole);
//            stmt.setString(3, idUtilisateur);
//            stmt.setString(4, nomAuteur);
//            stmt.setString(5, categorie);
//            stmt.setString(6, action);
//            stmt.setString(7, details);
//            stmt.setString(8, adresseIp);
//            stmt.setString(9, appareil);
//            // 2. Passage de la date/heure courante de l'application
//            stmt.setTimestamp(10, Timestamp.valueOf(java.time.LocalDateTime.now()));
//
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            System.err.println("Erreur inserer (LogsAudit) : " + e.getMessage());
//        }
//    }
//}