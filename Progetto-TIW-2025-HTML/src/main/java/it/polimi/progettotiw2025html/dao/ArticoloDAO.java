package it.polimi.progettotiw2025html.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import it.polimi.progettotiw2025html.beans.*;

public class ArticoloDAO {
    private final Connection connection;

    public ArticoloDAO(Connection connection) {
        this.connection = connection;
    }

    public List<Articolo> getArticoliByIdAsta(int idAsta) throws SQLException {
        String sql = "SELECT * FROM Articolo WHERE IdAsta = ?";
        List<Articolo> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idAsta);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Articolo(rs.getString("Codice"),
                    rs.getString("Nome"),
                    rs.getString("Descrizione"),
                    rs.getInt("Prezzo"),
                    rs.getString("Proprietario")));
        }
        return result;
    }
}