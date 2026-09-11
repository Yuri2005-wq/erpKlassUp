package org.erpklassup.erpklassup.dto;

import java.time.LocalDateTime;

public record SessionInfo(
        String idSession,
        String idUtilisateur,
        String idEcoleActive,
        String idGroupeActif,
        String adresseIp,
        String nomAppareil,
        LocalDateTime dateDerniereActivite,
        boolean estConnecte
) {}