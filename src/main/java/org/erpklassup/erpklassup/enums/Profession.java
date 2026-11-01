package org.erpklassup.erpklassup.enums;

public enum Profession {
    // ================================================================
    // SECTEUR ADMINISTRATIF ET POLITIQUE
    // ================================================================
    ADMINISTRATEUR_CIVIL("Administrateur civil"),
    AGENT_ADMINISTRATIF("Agent administratif"),
    AMBASSADEUR("Ambassadeur"),
    ATTACHE_ADMINISTRATIF("Attaché administratif"),
    CADRE_ADMINISTRATIF("Cadre administratif"),
    CHEF_DE_SERVICE("Chef de service"),
    CONSEILLER("Conseiller"),
    CONTRÔLEUR("Contrôleur"),
    DIRECTEUR_ADMINISTRATIF("Directeur administratif"),
    DIRECTEUR_GENERAL("Directeur général"),
    DIRECTEUR_REGIONAL("Directeur régional"),
    ECOLIER("Écolier"),
    GOUVERNEUR("Gouverneur"),
    INSPECTEUR("Inspecteur"),
    MINISTRE("Ministre"),
    PREFET("Préfet"),
    SOUS_PREFET("Sous-préfet"),
    SECRETAIRE_GENERAL("Secrétaire général"),
    SECRETAIRE_ADMINISTRATIF("Secrétaire administratif"),

    // ================================================================
    // SECTEUR JUDICIAIRE ET LEGAL
    // ================================================================
    AVOCAT("Avocat"),
    AVOCAT_CONSEIL("Avocat conseil"),
    BAILIFF("Huissier"),
    JUGE("Juge"),
    MAGISTRAT("Magistrat"),
    NOTAIRE("Notaire"),
    PROCUREUR("Procureur"),
    GREFFIER("Greffier"),
    CLERC_DE_NOTAIRE("Clerc de notaire"),
    JURISTE("Juriste"),
    CONSEILLER_JURIDIQUE("Conseiller juridique"),
    LEGISTE("Légiste"),

    // ================================================================
    // SECTEUR DE L'EDUCATION
    // ================================================================
    ENSEIGNANT("Enseignant"),
    PROFESSEUR("Professeur"),
    MAITRE_DE_CONFERENCE("Maître de conférence"),
    DOCTEUR_EN_SCIENCES("Docteur en sciences"),
    RECTEUR("Recteur"),
    PROVISEUR("Proviseur"),
    PRINCIPAL("Principal"),
    DIRECTEUR_D_ECOLE("Directeur d'école"),
    SURVEILLANT("Surveillant"),
    PROFESSEUR_D_ECOL("Professeur d'école"),
    MONITEUR("Moniteur"),
    INSPECTEUR_D_ACADEMIE("Inspecteur d'académie"),
    CONSEILLER_PEDAGOGIQUE("Conseiller pédagogique"),
    CHARGÉ_DE_COURS("Chargé de cours"),
    TUTEUR("Tuteur"),
    BIBLIOTHECAIRE("Bibliothécaire"),
    DOCUMENTALISTE("Documentaliste"),
    ENSEIGNANT_CHERCHEUR("Enseignant chercheur"),

    // ================================================================
    // SECTEUR DE LA SANTE
    // ================================================================
    MEDECIN("Médecin"),
    CHIRURGIEN("Chirurgien"),
    SPECIALISTE("Spécialiste"),
    GENERALISTE("Généraliste"),
    INFIRMIER("Infirmier"),
    INFIRMIERE("Infirmière"),
    SAGE_FEMME("Sage-femme"),
    PHARMACIEN("Pharmacien"),
    BIOLOGISTE("Biologiste"),
    KINESITHERAPEUTE("Kinésithérapeute"),
    ORTHOPHONISTE("Orthophoniste"),
    PSYCHOLOGUE("Psychologue"),
    PSYCHIATRE("Psychiatre"),
    DENTISTE("Dentiste"),
    OPHTALMOLOGISTE("Ophtalmologiste"),
    PEDIATRE("Pédiatre"),
    GYNECOLOGUE("Gynécologue"),
    URGENTISTE("Urgentiste"),
    RADIOLOGUE("Radiologue"),
    ANESTHESISTE("Anesthésiste"),
    LABORANTIN("Laborantin"),
    TECHNICIEN_DE_LABORATOIRE("Technicien de laboratoire"),
    AMBULANCIER("Ambulancier"),
    AIDE_SOIGNANT("Aide-soignant"),
    AUXILIAIRE_DE_SANTE("Auxiliaire de santé"),

    // ================================================================
    // SECTEUR DE L'INGENIERIE ET TECHNIQUE
    // ================================================================
    INGENIEUR("Ingénieur"),
    INGENIEUR_CIVIL("Ingénieur civil"),
    INGENIEUR_INFORMATIQUE("Ingénieur informatique"),
    INGENIEUR_AGRO("Ingénieur agro"),
    INGENIEUR_ENVIRONNEMENT("Ingénieur environnement"),
    INGENIEUR_ELECTRIQUE("Ingénieur électrique"),
    INGENIEUR_MECANIQUE("Ingénieur mécanique"),
    INGENIEUR_CHIMISTE("Ingénieur chimiste"),
    INGENIEUR_PETROLIER("Ingénieur pétrolier"),
    ARCHITECTE("Architecte"),
    URBANISTE("Urbaniste"),
    TOPOGRAPHE("Topographe"),
    GEOMETRE("Géomètre"),
    GEOLOGUE("Géologue"),
    TECHNICIEN_SUPERIEUR("Technicien supérieur"),
    TECHNICIEN("Technicien"),
    TECHNICIEN_INFORMATIQUE("Technicien informatique"),
    TECHNICIEN_MAINTENANCE("Technicien de maintenance"),
    TECHNICIEN_ELECTRONIQUE("Technicien électronique"),
    TECHNICIEN_AUDIOVISUEL("Technicien audiovisuel"),
    DESSINATEUR("Dessinateur"),
    DESSINATEUR_INDUSTRIEL("Dessinateur industriel"),
    MODÉLISTE("Modéliste"),

    // ================================================================
    // SECTEUR DE L'INFORMATIQUE ET DES TELECOMS
    // ================================================================
    INFORMATICIEN("Informaticien"),
    ANALYSTE_INFORMATIQUE("Analyste informaticien"),
    DEVELOPPEUR("Développeur"),
    PROGRAMMEUR("Programmeur"),
    ARCHITECTE_LOGICIEL("Architecte logiciel"),
    ADMINISTRATEUR_RESEAU("Administrateur réseau"),
    ADMINISTRATEUR_SYSTEME("Administrateur système"),
    ADMINISTRATEUR_BASE_DONNEES("Administrateur base de données"),
    SECURITE_INFORMATIQUE("Sécurité informatique"),
    CONSULTANT_INFORMATIQUE("Consultant informatique"),
    CHEF_DE_PROJET_INFORMATIQUE("Chef de projet informatique"),
    RESPONSABLE_INFORMATIQUE("Responsable informatique"),
    WEBMASTER("Webmaster"),
    INTEGRATEUR_WEB("Intégrateur web"),
    GRAPHISTE("Graphiste"),
    INFOGRAFFE("Infograffe"),
    TECHNICIEN_TELECOMS("Technicien télécoms"),
    INGENIEUR_TELECOMS("Ingénieur télécoms"),

    // ================================================================
    // SECTEUR COMMERCIAL ET FINANCIER
    // ================================================================
    COMMERCANT("Commerçant"),
    COMMERCANTE("Commerçante"),
    PETIT_COMMERCANT("Petit commerçant"),
    GROSSISTE("Grossiste"),
    DETAILLANT("Détaillant"),
    REVENDEUR("Revendeur"),
    MARCHAND("Marchand"),
    MARCHANDE("Marchande"),
    ENTREPRENEUR("Entrepreneur"),
    CHEF_D_ENTREPRISE("Chef d'entreprise"),
    DIRECTEUR_COMMERCIAL("Directeur commercial"),
    RESPONSABLE_COMMERCIAL("Responsable commercial"),
    COMMERCIAL("Commercial"),
    CONSEILLER_COMMERCIAL("Conseiller commercial"),
    AGENT_COMMERCIAL("Agent commercial"),
    CHEF_DE_RECETTES("Chef de recettes"),
    CAISSIER("Caissier"),
    CAISSIERE("Caissière"),
    COMPTABLE("Comptable"),
    COMPTABLE_AGREE("Comptable agréé"),
    EXPERT_COMPTABLE("Expert comptable"),
    COMMISSAIRE_AUX_COMPTES("Commissaire aux comptes"),
    FINANCIER("Financier"),
    ANALYSTE_FINANCIER("Analyste financier"),
    AUDITEUR("Auditeur"),
    BANQUIER("Banquier"),
    AGENT_BANCAIRE("Agent bancaire"),
    CONSEILLER_BANCAIRE("Conseiller bancaire"),
    DIRECTEUR_BANCAIRE("Directeur bancaire"),
    ASSUREUR("Assureur"),
    AGENT_D_ASSURANCE("Agent d'assurance"),
    COURTAGE("Courtage"),
    GERANT_DE_MAGASIN("Gérant de magasin"),
    GERANTE_DE_MAGASIN("Gérante de magasin"),
    VENDEUR("Vendeur"),
    VENDEUSE("Vendeuse"),
    TELEVENDEUR("Télévendeur"),
    RESPONSABLE_DE_RAYON("Responsable de rayon"),
    ACHETEUR("Acheteur"),

    // ================================================================
    // SECTEUR AGRICOLE ET ELEVAGE
    // ================================================================
    AGRICULTEUR("Agriculteur"),
    AGRICULTRICE("Agricultrice"),
    PAYSAN("Paysan"),
    PAYSANNE("Paysanne"),
    ELEVEUR("Éleveur"),
    ELEVEUSE("Éleveuse"),
    AGRONOME("Agronome"),
    TECHNICIEN_AGRICOLE("Technicien agricole"),
    INGENIEUR_AGRONOME("Ingénieur agronome"),
    PISCICULTEUR("Pisciculteur"),
    AVICULTEUR("Aviculteur"),
    APICULTEUR("Apiculteur"),
    VETERINAIRE("Vétérinaire"),
    PATOULIER("Patoulier"),
    JARDINIER("Jardinier"),
    MARAICHER("Maraîcher"),
    SERRURICULTEUR("Serriculteur"),
    FORESTIER("Forestier"),

    // ================================================================
    // SECTEUR ARTISANAL ET ARTISTIQUE
    // ================================================================
    ARTISAN("Artisan"),
    ARTISANE("Artisane"),
    MENUISIER("Menuisier"),
    SCULPTEUR("Sculpteur"),
    POTIER("Potier"),
    VANNERIE("Vannerie"),
    TISSERAND("Tisserand"),
    BRODEUR("Brodeur"),
    COUTURIER("Couturier"),
    COUTURIERE("Couturière"),
    MODISTE("Modiste"),
    TAILLEUR("Tailleur"),
    TAILLEUSE("Tailleuse"),
    CORDEONNIER("Cordonnier"),
    SERRURIER("Serrurier"),
    FORGERON("Forgeron"),
    SOUDEUR("Soudeur"),
    PLOMBIER("Plombier"),
    ELECTRICIEN("Électricien"),
    ELECTRONICIEN("Électronicien"),
    MECANICIEN("Mécanicien"),
    CARROSSIER("Carrossier"),
    PEINTRE("Peintre"),
    PEINTRE_EN_BATIMENT("Peintre en bâtiment"),
    MAÇON("Maçon"),
    MENAGERE("Ménagère"), // ✅ AJOUTÉ
    FEMME_AU_FOYER("Femme au foyer"),
    HOMME_AU_FOYER("Homme au foyer"),
    ARTISTE_PEINTRE("Artiste peintre"),
    MUSICIEN("Musicien"),
    CHANTEUR("Chanteur"),
    COMEDIEN("Comédien"),
    DANCER("Danseur"),
    DANSEUSE("Danseuse"),
    PHOTOGRAPHE("Photographe"),
    CINEMATOGRAPHE("Cinématographe"),
    REALISATEUR("Réalisateur"),
    SCENARISTE("Scénariste"),

    // ================================================================
    // SECTEUR DES TRANSPORTS ET LOGISTIQUE
    // ================================================================
    CHAUFFEUR("Chauffeur"),
    CONDUCTEUR("Conducteur"),
    CONDUCTEUR_DE_BUS("Conducteur de bus"),
    CONDUCTEUR_DE_CAMION("Conducteur de camion"),
    CONDUCTEUR_DE_MOTO("Conducteur de moto"),
    CHAUFFEUR_PARTICULIER("Chauffeur particulier"),
    TAXIMAN("Taximan"),
    CHAUFFEUR_DE_TAXI("Chauffeur de taxi"),
    CHAUFFEUR_DE_CAR("Chauffeur de car"),
    PILOTE_DE_BATEAU("Pilote de bateau"),
    PILOTE_DAVION("Pilote d'avion"),
    MECANICIEN_AUTO("Mécanicien auto"),
    REPARATEUR_AUTO("Réparateur auto"),
    VENDEUR_AUTO("Vendeur auto"),
    LOGISTICIEN("Logisticien"),
    MANUTENTIONNAIRE("Manutentionnaire"),
    GERANT_DE_DEPOT("Gérant de dépôt"),
    MAGASINIER("Magasinier"),
    EMPLOYE_DE_MAGASIN("Employé de magasin"),
    LIVREUR("Livreur"),

    // ================================================================
    // SECTEUR DE LA COMMUNICATION ET DES MEDIAS
    // ================================================================
    JOURNALISTE("Journaliste"),
    REDACTEUR("Rédacteur"),
    COMMUNICANT("Communicant"),
    ATTACHE_DE_PRESSE("Attaché de presse"),
    RESPONSABLE_COMMUNICATION("Responsable communication"),
    CHARGE_DE_COMMUNICATION("Chargé de communication"),
    AGENT_DE_COMMUNICATION("Agent de communication"),
    PRESENTATEUR_TELE("Présentateur télé"),
    ANIMATEUR_RADIO("Animateur radio"),
    PRODUCTEUR("Producteur"),
    CADREUR("Cadreur"),
    TECHNICIEN_SON("Technicien son"),
    MONTEUR("Monteur"),
    PUBLICITAIRE("Publicitaire"),
    DESIGNER("Designer"),

    // ================================================================
    // SECTEUR DES SERVICES
    // ================================================================
    AGENT_DE_SECURITE("Agent de sécurité"),
    GARDIEN("Gardien"),
    VIGILE("Vigile"),
    AGENT_DE_MANTENANCE("Agent de maintenance"),
    CONCIERGE("Concierge"),
    FEMME_DE_MENAGE("Femme de ménage"),
    REPASSEUSE("Repasseuse"),
    BLANCHISSEUSE("Blanchisseuse"),
    COIFFEUR("Coiffeur"),
    COIFFEUSE("Coiffeuse"),
    ESTHETICIENNE("Esthéticienne"),
    MANUCURE("Manucure"),
    PEDICURE("Pédicure"),
    HOTESSE("Hôtesse"),
    RECEPTIONNISTE("Réceptionniste"),
    GUIDE_TOURISTIQUE("Guide touristique"),
    GARDE_DENFANTS("Garde d'enfants"),
    AIDE_A_DOMICILE("Aide à domicile"),
    EMPLOYE_DOMAINE("Employé de domaine"),
    SERVANT("Servant"),
    BOUTIQUIER("Boutiquier"),
    PATISSIER("Pâtissier"),
    BOULANGER("Boulanger"),
    PIZZAIOLO("Pizzaiolo"),
    CUISINIER("Cuisinier"),
    CUISINIERE("Cuisinière"),
    RESTAURATEUR("Restaurateur"),
    GERANT_DE_RESTAURANT("Gérant de restaurant"),
    BARMAN("Barman"),
    SERVEUR("Serveur"),
    SOMMELIER("Sommelier"),

    // ================================================================
    // SECTEUR RELIGIEUX ET CULTUREL
    // ================================================================
    PASTEUR("Pasteur"),
    PASTEURE("Pasteure"),
    PRETRE("Prêtre"),
    RELIGIEUX("Religieux"),
    RELIGIEUSE("Religieuse"),
    RABBIN("Rabbin"),
    IMAN("Imam"),
    CHEF_RELIGIEUX("Chef religieux"),
    DESSERVANT("Desservant"),
    CATECHISTE("Catéchiste"),
    EVANGELISTE("Évangéliste"),
    MISSIONNAIRE("Missionnaire"),
    DOYEN("Doyen"),
    GRAND_MAITRE("Grand maître"),

    // ================================================================
    // AUTRES METIERS
    // ================================================================
    AGENT_DE_VOYAGE("Agent de voyage"),
    CONSEILLER_ORIENTATION("Conseiller orientation"),
    DELEGUE("Délégué"),
    DELEGUE_MEDICAL("Délégué médical"),
    REPRESENTANT("Représentant"),
    SYNDICALISTE("Syndicaliste"),
    MILITANT("Militant"),
    ACTIVISTE("Activiste"),
    BUREAUCRATE("Bureaucrate"),
    FONCTIONNAIRE("Fonctionnaire"),
    RETRAITE("Retraité"),
    RETRAITEE("Retraitée"),
    SANS_EMPLOI("Sans emploi"),
    ETUDIANT("Étudiant"),
    STAGIAIRE("Stagiaire"),
    APPRENTI("Apprenti"),
    AUTRE("Autre");

    // ================================================================
    // ATTRIBUTS ET CONSTRUCTEUR
    // ================================================================
    private final String libelle;

    Profession(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}