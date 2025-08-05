package it.polimi.progettotiw2025html.dao;

import it.polimi.progettotiw2025html.utils.ConnectionHandler;
import java.sql.*;
import it.polimi.progettotiw2025html.beans.*;

public class ArticoloDAO {
    private final Connection connection;

    public ArticoloDAO(Connection connection) {
        this.connection = connection;
    }


    public void addArticolo(Articolo articolo) throws SQLException {
        String query = "INSERT INTO Articolo (Codice, Nome, Descrizione, Immagine, Prezzo) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, articolo.getCodice());
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