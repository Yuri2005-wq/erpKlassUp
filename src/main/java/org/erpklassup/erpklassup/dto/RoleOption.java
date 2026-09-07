package org.erpklassup.erpklassup.dto;

public record RoleOption(String idRole, String nomRole, String description) {
    @Override public String toString() { return nomRole; }
}