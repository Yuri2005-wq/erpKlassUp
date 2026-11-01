// util/BreadcrumbAware.java
package org.erpklassup.erpklassup.util;

public interface BreadcrumbAware {
    /**
     * Appelé quand la vue devient visible (y compris depuis le cache).
     * La vue doit y définir son breadcrumb complet.
     */
    void definirBreadcrumb();
}