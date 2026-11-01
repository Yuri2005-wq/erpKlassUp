package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.FiliereDAO;
import org.erpklassup.erpklassup.dao.module2.GroupeMatiereDAO;
import org.erpklassup.erpklassup.dao.module2.NiveauDAO;
import org.erpklassup.erpklassup.models.module2.Filiere;
import org.erpklassup.erpklassup.models.module2.GroupeMatiere;
import org.erpklassup.erpklassup.models.module2.Niveau;
import org.erpklassup.erpklassup.service.Autorisation;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class AcademiqueService extends ServiceAsyncBase {

    private final FiliereDAO filiereDAO;
    private final NiveauDAO niveauDAO;
    private final GroupeMatiereDAO groupeMatiereDAO;

    // ==========================================
    // CONSTRUCTEURS
    // ==========================================
    public AcademiqueService() {
        this(new FiliereDAO(), new NiveauDAO(), new GroupeMatiereDAO());
    }

    public AcademiqueService(FiliereDAO filiereDAO, NiveauDAO niveauDAO, GroupeMatiereDAO groupeMatiereDAO) {
        this.filiereDAO = filiereDAO;
        this.niveauDAO = niveauDAO;
        this.groupeMatiereDAO = groupeMatiereDAO;
    }

    // ==========================================
    // SECTION : FILIERES
    // ==========================================

    // Synchrones
    public List<Filiere> listerFilieresParEcole(String idEcole) {
        return filiereDAO.findByEcole(idEcole);
    }

    public Optional<Filiere> trouverFiliereParId(String idFiliere) {
        return filiereDAO.findById(idFiliere);
    }

    // Asynchrones
    public void listerFilieresParEcoleAsync(String idEcole,
                                            Consumer<List<Filiere>> onSucces,
                                            Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> filiereDAO.findByEcole(idEcole),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                onErreur);
    }

    public void creerFiliereAsync(Filiere filiere, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("academique.filiere.creer");
            if (!filiereDAO.create(filiere)) {
                throw new IllegalStateException("Impossible de créer la filière (code existe déjà ?).");
            }
        }, onSucces, onErreur);
    }

    public void modifierFiliereAsync(Filiere filiere, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("academique.filiere.modifier");
            if (!filiereDAO.update(filiere)) {
                throw new IllegalStateException("Échec de la mise à jour de la filière.");
            }
        }, onSucces, onErreur);
    }

    public void supprimerFiliereAsync(String idFiliere, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("academique.filiere.supprimer");
            if (!filiereDAO.softDelete(idFiliere)) {
                throw new IllegalStateException("Échec de la suppression de la filière.");
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // SECTION : NIVEAUX
    // ==========================================

    // Synchrones
//    public void listerNiveauxParEcoleAsync(String idEcole,
//                                           Consumer<List<Niveau>> onSucces,
//                                           Consumer<Throwable> onErreur) {
//        long jeton = nouvelleRequete();
//        executer(() -> niveauDAO.findByEcole(idEcole),
//                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
//                onErreur);
//    }

    public void listerNiveauxParEcoleAsync(String idEcole,
                                           Consumer<List<Niveau>> onSucces,
                                           Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> {
                    try {
                        System.out.println("🔍 Execution de niveauDAO.findByEcole(" + idEcole + ")...");
                        List<Niveau> res = niveauDAO.findByEcole(idEcole);
                        System.out.println("🔍 Resultat DAO trouve : " + (res != null ? res.size() : "null"));
                        return res;
                    } catch (Throwable t) {
                        System.err.println("💥 CRASH DAO NIVEAU : " + t.getMessage());
                        t.printStackTrace(); // Affichera l'erreur SQL exacte dans la console
                        throw t;
                    }
                },
                resultat -> {
                    System.out.println("🔍 Verification jeton : estActuelle=" + estRequeteActuelle(jeton));
                    if (estRequeteActuelle(jeton)) {
                        onSucces.accept(resultat);
                    }
                },
                onErreur);
    }
    public Optional<Niveau> trouverNiveauParId(String idNiveau) {
        return niveauDAO.findById(idNiveau);
    }

    // Asynchrones
//    public void listerNiveauxParEcoleAsync(String idEcole,
//                                           Consumer<List<Niveau>> onSucces,
//                                           Consumer<Throwable> onErreur) {
//        long jeton = nouvelleRequete();
//        executer(() -> niveauDAO.findByEcole(idEcole),
//                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
//                onErreur);
//    }

    public void listerNiveauxParSystemeAsync(String idEcole, String systeme,
                                             Consumer<List<Niveau>> onSucces,
                                             Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> niveauDAO.findByEcoleAndSysteme(idEcole, systeme),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                onErreur);
    }

    public void creerNiveauAsync(Niveau niveau, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("academique.niveau.creer");
            if (!niveauDAO.create(niveau)) {
                throw new IllegalStateException("Impossible de créer le niveau.");
            }
        }, onSucces, onErreur);
    }

    public void modifierNiveauAsync(Niveau niveau, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("academique.niveau.modifier");
            if (!niveauDAO.update(niveau)) {
                throw new IllegalStateException("Échec de la mise à jour du niveau.");
            }
        }, onSucces, onErreur);
    }

    public void supprimerNiveauAsync(String idNiveau, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("academique.niveau.supprimer");
            if (!niveauDAO.softDelete(idNiveau)) {
                throw new IllegalStateException("Échec de la suppression du niveau.");
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // SECTION : GROUPES DE MATIERES
    // ==========================================

    // Synchrones
    public List<GroupeMatiere> listerGroupesMatiereParEcole(String idEcole) {
        return groupeMatiereDAO.findByEcole(idEcole);
    }

    public Optional<GroupeMatiere> trouverGroupeMatiereParId(String idGroupe) {
        return groupeMatiereDAO.findById(idGroupe);
    }

    // Asynchrones
    public void listerGroupesMatiereParEcoleAsync(String idEcole,
                                                  Consumer<List<GroupeMatiere>> onSucces,
                                                  Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> groupeMatiereDAO.findByEcole(idEcole),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                onErreur);
    }

    public void creerGroupeMatiereAsync(GroupeMatiere groupe, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("academique.groupematiere.creer");
            if (!groupeMatiereDAO.create(groupe)) {
                throw new IllegalStateException("Impossible de créer le groupe de matière.");
            }
        }, onSucces, onErreur);
    }

    public void modifierGroupeMatiereAsync(GroupeMatiere groupe, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("academique.groupematiere.modifier");
            if (!groupeMatiereDAO.update(groupe)) {
                throw new IllegalStateException("Échec de la mise à jour du groupe de matière.");
            }
        }, onSucces, onErreur);
    }

    public void supprimerGroupeMatiereAsync(String idGroupe, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("academique.groupematiere.supprimer");
            if (!groupeMatiereDAO.softDelete(idGroupe)) {
                throw new IllegalStateException("Échec de la suppression du groupe de matière.");
            }
        }, onSucces, onErreur);
    }


    /**
     * Recherche textuelle dans les filières d'une école.
     * Filtre par code OU par nom (insensible à la casse).
     */
    public void rechercherFiliereAsync(String idEcole, String terme,
                                       Consumer<List<Filiere>> onSucces,
                                       Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> {
                    List<Filiere> toutes = filiereDAO.findByEcole(idEcole);
                    if (terme == null || terme.isBlank()) return toutes;

                    String q = terme.toLowerCase().trim();
                    return toutes.stream()
                            .filter(f -> (f.getCodeFiliere() != null
                                    && f.getCodeFiliere().toLowerCase().contains(q))
                                    || (f.getNomFiliere() != null
                                    && f.getNomFiliere().toLowerCase().contains(q)))
                            .toList();
                },
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur   -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

//    public void rechercherNiveauAsync(String idEcole,
//                                Consumer<List<Niveau>> onSucces,
//                                Consumer<Throwable> onErreur) {
//        long jeton = nouvelleRequete();
//        executer(() -> niveauDAO.findByEcole(idEcole),
//                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
//                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
//    }
// Dans AcademiqueService.java
public void rechercherNiveauAsync(String idEcole, String terme,
                                  Consumer<List<Niveau>> onSucces,
                                  Consumer<Throwable> onErreur) {
    long jeton = nouvelleRequete();
    executer(() -> {
                List<Niveau> tous = niveauDAO.findByEcole(idEcole);
                if (terme == null || terme.isBlank()) return tous;
                String q = terme.toLowerCase().trim();
                return tous.stream()
                        .filter(n -> n.getNomNiveau().toLowerCase().contains(q))
                        .toList();
            },
            resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
            erreur   -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
}

    /**
     * Recherche textuelle dans les groupes de matières d'une école.
     * Filtre par code OU par nom (insensible à la casse).
     */
    public void rechercherGroupeAsync(String idEcole, String terme,
                                      Consumer<List<GroupeMatiere>> onSucces,
                                      Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> {
                    List<GroupeMatiere> tous = groupeMatiereDAO.findByEcole(idEcole);
                    if (terme == null || terme.isBlank()) return tous;

                    String q = terme.toLowerCase().trim();
                    return tous.stream()
                            .filter(g -> (g.getCodeGroupe() != null
                                    && g.getCodeGroupe().toLowerCase().contains(q))
                                    || (g.getNomGroupe() != null
                                    && g.getNomGroupe().toLowerCase().contains(q)))
                            .toList();
                },
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur   -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    /* ============================================================
   VERSIONS SANS JETON (pour appels parallèles)
   Utilisées quand plusieurs requêtes sont lancées simultanément
   sur la même instance — sans cela, elles s'invalident mutuellement.
   ============================================================ */

    public void listerNiveauxParEcoleSansJetonAsync(String idEcole,
                                                    Consumer<List<Niveau>> onSucces,
                                                    Consumer<Throwable> onErreur) {
        executer(() -> niveauDAO.findByEcole(idEcole), onSucces, onErreur);
    }

    public void listerFilieresParEcoleSansJetonAsync(String idEcole,
                                                     Consumer<List<Filiere>> onSucces,
                                                     Consumer<Throwable> onErreur) {
        executer(() -> filiereDAO.findByEcole(idEcole), onSucces, onErreur);
    }
}