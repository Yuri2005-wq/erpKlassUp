package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

import java.util.Objects;
import java.time.LocalDate;
import java.time.LocalDateTime;


public class AnneeScolaire extends BaseEntity {

        public enum Statut {
            PREPARATION("En préparation"),
            EN_COURS("En cours"),
            CLOTUREE("Clôturée"),
            ARCHIVEE("Archivée");

            private final String libelle;

            Statut(String libelle) { this.libelle = libelle; }

            public String getLibelle() { return libelle; }
        }
        private String idEcole;
        private String libelle;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private boolean estCourante = false;
        private Statut statut = Statut.PREPARATION;

        public AnneeScolaire() {
            super();
        }

        public AnneeScolaire(String idEcole, String libelle,
                             LocalDate dateDebut, LocalDate dateFin) {
            this.idEcole = Objects.requireNonNull(idEcole, "idEcole obligatoire");
            this.libelle = Objects.requireNonNull(libelle, "libelle obligatoire");
            this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut obligatoire");
            this.dateFin = Objects.requireNonNull(dateFin, "dateFin obligatoire");

            if (!dateFin.isAfter(dateDebut)) {
                throw new IllegalArgumentException("dateFin doit être postérieure à dateDebut");
            }
        }

        public boolean contient(LocalDate date) {
            if (date == null) return false;
            return !date.isBefore(dateDebut) && !date.isAfter(dateFin);
        }


        public boolean estModifiable() {
            return statut == Statut.PREPARATION || statut == Statut.EN_COURS;
        }

        public boolean estEnCours() {
            LocalDate aujourdhui = LocalDate.now();
            return statut == Statut.EN_COURS && contient(aujourdhui);
        }


        public void passerAuStatut(Statut nouveauStatut) {
            if (nouveauStatut == null) {
                throw new IllegalArgumentException("Le nouveau statut ne peut pas être null");
            }
            // Règle métier : on ne peut pas réouvrir une année clôturée
            if (this.statut == Statut.CLOTUREE && nouveauStatut != Statut.ARCHIVEE) {
                throw new IllegalStateException(
                        "Une année clôturée ne peut être que archivée");
            }
            this.statut = nouveauStatut;
            this.updatedAt = LocalDateTime.now();
        }

        public void marquerCourante() {
            this.estCourante = true;
            this.updatedAt = LocalDateTime.now();
        }

        public void retirerCourante() {
            this.estCourante = false;
            this.updatedAt = LocalDateTime.now();
        }
        public String getIdAnneeScolaire(){return getId(); }
        public String getIdEcole() { return idEcole; }
        public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public LocalDate getDateDebut() { return dateDebut; }
        public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

        public LocalDate getDateFin() { return dateFin; }
        public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

        public boolean isEstCourante() { return estCourante; }
        public void setEstCourante(boolean estCourante) { this.estCourante = estCourante; }

        public Statut getStatut() { return statut; }
        public void setStatut(Statut statut) { this.statut = statut; }

        @Override
        public String toString() {
            return "AnneeScolaire{" +
                    "idAnnee='" + getIdAnneeScolaire() + '\'' +
                    ", libelle='" + libelle + '\'' +
                    ", statut=" + statut +
                    ", estCourante=" + estCourante +
                    '}';
        }
    }