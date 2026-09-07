package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.LogsAuditDAO;
import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.dto.FiltreAudit;

import java.util.List;
import java.util.function.Consumer;

public class AuditService extends ServiceAsyncBase {

    private final LogsAuditDAO dao;

    public AuditService() { this(new LogsAuditDAO()); }
    public AuditService(LogsAuditDAO dao) { this.dao = dao; }

    public void rechercherAsync(FiltreAudit filtre, Consumer<List<AuditLigne>> onSucces, Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> dao.rechercher(filtre),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void listerCategoriesAsync(String idEcole, Consumer<List<String>> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> dao.listerCategoriesDistinctes(idEcole), onSucces, onErreur);
    }
}