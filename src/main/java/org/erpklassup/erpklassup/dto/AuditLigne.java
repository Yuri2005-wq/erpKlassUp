package org.erpklassup.erpklassup.dto;

import java.time.LocalDateTime;

public record AuditLigne(
        String idLogAudit,
        LocalDateTime horodatage,
        String auteur,
        String categorie,
        String action,
        String details,
        String adresseIp,
        String appareil
) {}