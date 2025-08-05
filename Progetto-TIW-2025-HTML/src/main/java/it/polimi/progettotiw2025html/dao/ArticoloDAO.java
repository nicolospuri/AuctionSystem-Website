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


    public void addArticolo(Articolo articolo) throws SQLException {
        String query = "INSERT INTO Articolo (Codice, Nome, Descrizione, Immagine, Prezzo) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, articolo.getCodice());
            stmt.setString(2, articolo.getNome());
            stmt.setString(3, articolo.getDescrizione());
            stmt.setString(4, articolo.getImmagine());
            stmt.setDouble(5, articolo.getPrezzo());

            stmt.executeUpdate(); // Esegue l'operazione SQL che modifica i dati


        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Errore durante l'aggiunta dell'articolo: " + e.getMessage());
        }
    }
}