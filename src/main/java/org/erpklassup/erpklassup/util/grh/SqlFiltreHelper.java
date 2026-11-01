package org.erpklassup.erpklassup.util.grh;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SqlFiltreHelper {

    private final StringBuilder clauseWhere = new StringBuilder(" WHERE 1=1");
    private final List<Object> parametres = new ArrayList<>();

    public SqlFiltreHelper ajouterCondition(boolean condition, String sqlFragment, Object valeurParametre) {
        if (condition) {
            clauseWhere.append(" AND ").append(sqlFragment);
            parametres.add(valeurParametre);
        }
        return this;
    }

    public SqlFiltreHelper ajouterConditionLike(String termeRecherche, String... colonnes) {
        if (termeRecherche != null && !termeRecherche.isBlank()) {
            clauseWhere.append(" AND (");
            for (int i = 0; i < colonnes.length; i++) {
                if (i > 0) clauseWhere.append(" OR ");
                clauseWhere.append(colonnes[i]).append(" LIKE ?");
                parametres.add("%" + termeRecherche.trim() + "%");
            }
            clauseWhere.append(")");
        }
        return this;
    }

    public String getClauseWhere() {
        return clauseWhere.toString();
    }

    public void injecterParametres(PreparedStatement stmt) throws SQLException {
        for (int i = 0; i < parametres.size(); i++) {
            stmt.setObject(i + 1, parametres.get(i));
        }
    }
}