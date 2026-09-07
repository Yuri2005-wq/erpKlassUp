package org.erpklassup.erpklassup.dto;

import java.util.List;

public record ResultatPagine<T>(List<T> elements, long total, int page, int taillePage) {
    public int nombreDePages() { return total == 0 ? 1 : (int) Math.ceil((double) total / taillePage); }
}