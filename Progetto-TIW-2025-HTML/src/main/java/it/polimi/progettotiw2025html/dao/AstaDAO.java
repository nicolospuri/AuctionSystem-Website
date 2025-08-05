package it.polimi.progettotiw2025html.dao;

import it.polimi.progettotiw2025html.beans.Asta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AstaDAO {
    private final Connection connection;

    public AstaDAO(Connection connection) {
        this.connection = connection;
    }

    public List<Asta> getAsteAperteByKeyword(String keyword) throws SQLException {
        String sql = "SELECT a.* " +
                "FROM Asta a JOIN Articolo art ON art.idAsta = a.id " +
                "WHERE a.chiusa = FALSE AND a.scadenza > NOW() AND (art.nome LIKE ? OR art.descrizione LIKE ?) " +
                "ORDER BY a.scadenza DESC";
        List<Asta> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        String pattern = "%" + keyword + "%";
        stmt.setString(1, pattern);
        stmt.setString(2, pattern);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Asta(rs.getInt("id"),
                    rs.getDouble("Prezzo"),
                    rs.getInt("RialzoMinimo"),
                    rs.getTimestamp("Scadenza").toLocalDateTime(),
                    rs.getString("Proprietario")));
        }
        return result;
    }

    public List<Asta> getAsteVinteByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM Asta WHERE aggiudicatario = ? AND chiusa = true ORDER BY scadenza DESC";
        List<Asta> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, username);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            while (rs.next()) {
                result.add(new Asta(rs.getInt("id"),
                        rs.getDouble("Prezzo"),
                        rs.getInt("RialzoMinimo"),
                        rs.getTimestamp("Scadenza").toLocalDateTime(),
                        rs.getString("Proprietario")));
            }
        }
        return result;
    }
}
