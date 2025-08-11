package it.polimi.progettotiw2025html.dao;

import it.polimi.progettotiw2025html.beans.Offerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OffertaDAO {
    private final Connection connection;

    public OffertaDAO(Connection connection) {
        this.connection = connection;
    }

    public List<Offerta> getOfferteByIdAsta(int idAsta) throws SQLException {
        String sql = "SELECT * FROM Offerta WHERE idAsta = ? ORDER BY Prezzo DESC";
        List<Offerta> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idAsta);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Offerta(rs.getInt("Id"),
                    rs.getString("Offerente"),
                    rs.getDouble("Prezzo"),
                    rs.getTimestamp("Data").toLocalDateTime(),
                    rs.getInt("IdAsta")));
        }
        return result;
    }

    public Offerta getMaxOffertaByIdAsta(int idAsta) throws SQLException {
        String sql = "SELECT * FROM Offerta WHERE IdAsta = ? " +
                "AND Prezzo = (SELECT MAX(Prezzo) FROM Offerta WHERE IdAsta = ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idAsta);
        stmt.setInt(2, idAsta);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new Offerta(rs.getInt("Id"),
                    rs.getString("Offerente"),
                    rs.getDouble("Prezzo"),
                    rs.getTimestamp("Data").toLocalDateTime(),
                    rs.getInt("IdAsta"));
        } else {
            return null;
        }
    }

    public boolean addOfferta(String offerente, double prezzo, int idAsta) throws SQLException {
        String sql = "INSERT INTO Offerta (Offerente, Prezzo, Data, IdAsta) VALUES (?, ?, NOW(), ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, offerente);
        stmt.setDouble(2, prezzo);
        stmt.setInt(3, idAsta);

        int rowsAffected = stmt.executeUpdate();
        return rowsAffected > 0;
    }
}
