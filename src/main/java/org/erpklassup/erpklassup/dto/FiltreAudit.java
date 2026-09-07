package org.erpklassup.erpklassup.dto;

import java.time.LocalDate;

public record FiltreAudit(
        String idEcole,
        String terme,
        LocalDate dateDebut,
        LocalDate dateFin,
        String categorie
) {}