package org.erpklassup.erpklassup.dto;

import org.erpklassup.erpklassup.enums.SousSysteme;
import org.erpklassup.erpklassup.enums.StatutDepartement;
import org.erpklassup.erpklassup.enums.TypeDepartement;

import java.math.BigDecimal;

public record DepartementLigne(
        String idDepartement,
        String code,
        String libelle,
        TypeDepartement type,
        SousSysteme sousSysteme,
        Boolean estConseilEnseignement,
        String parentLibelle,
        String responsableNomComplet,
        String responsableAdjointNomComplet,
        int effectifActuel,
        Integer effectifMax,
        StatutDepartement statut,
        BigDecimal budgetAnnuel,
        String codeMineduc
) {}