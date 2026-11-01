package org.erpklassup.erpklassup.enums;

public enum ModePaiement {
   ESPECES("Espèces"),
   CHEQUE("Chèque"),
   VIREMENT("Virement bancaire"),
   MTN_MOMO("Mobile Money"),
   CARTE_BANCAIRE("Carte bancaire"),
   TRANSFERT("Transfert"),
   AUTRE("Autre"),
   MTN_MOBILE_MONEY("MTN_MoMo"),
   ORANGE_MONEY("Orange_Money");

   private final String libelle;

   ModePaiement(String libelle) {
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
