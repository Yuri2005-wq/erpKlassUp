package org.erpklassup.erpklassup.util;

public interface StatefulController<T> {
    T sauvegarderEtat();
    void restaurerEtat(T etat);
}