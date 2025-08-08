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
        String pattern = "%" + keyword + "%";   // Usiamo '%' per cercare qualsiasi parte della stringa,
                                                // altrimenti cercheremmo solo descrizioni o nomi che
                                                // corrispondono esattamente alla keyword
        stmt.setString(1, pattern);
        stmt.setString(2, pattern);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Asta(rs.getInt("Id"),
                    rs.getDouble("Prezzo"),
                    rs.getInt("RialzoMinimo"),
                    rs.getTimestamp("Scadenza").toLocalDateTime(),
                    rs.getString("Proprietario")));
        }
        return result;
    }

    public List<Asta> getAsteVinteByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM Asta WHERE Aggiudicatario = ? AND Chiusa = true ORDER BY Scadenza DESC";
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
                        rs.getString("Proprietario"),
                        rs.getString("Aggiudicatario")));
            }
        }
        return result;
    }

    public List<Asta> getAsteChiuseByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM Asta WHERE Proprietario = ? AND Chiusa = true ORDER BY Scadenza ASC";
        List<Asta> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, username);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Asta(rs.getInt("Id"),
                    rs.getDouble("Prezzo"),
                    rs.getInt("RialzoMinimo"),
                    rs.getTimestamp("Scadenza").toLocalDateTime(),
                    rs.getString("Proprietario"),
                    rs.getString("Aggiudicatario")));
        }
        return result;
    }

    public List<Asta> getAsteAperteByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM Asta WHERE Proprietario = ? AND Chiusa = false AND Scadenza > NOW() ORDER BY Scadenza ASC";
        List<Asta> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, username);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Asta(rs.getInt("Id"),
                    rs.getDouble("Prezzo"),
                    rs.getInt("RialzoMinimo"),
                    rs.getTimestamp("Scadenza").toLocalDateTime(),
                    rs.getString("Proprietario")));
        }
        return result;
    }

    public Asta getAstaById(int idAsta) throws SQLException {
        String sql = "SELECT * FROM Asta WHERE Id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idAsta);

        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return new Asta(rs.getInt("Id"),
                    rs.getDouble("Prezzo"),
                    rs.getInt("RialzoMinimo"),
                    rs.getTimestamp("Scadenza").toLocalDateTime(),
                    rs.getString("Proprietario"));
        }
        return null;
    }
}
