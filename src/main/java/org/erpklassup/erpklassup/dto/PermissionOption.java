package org.erpklassup.erpklassup.dto;

public record PermissionOption(String idPermission, String codePermission, String libelle, String categorie) {
    @Override public String toString() { return libelle; }
}