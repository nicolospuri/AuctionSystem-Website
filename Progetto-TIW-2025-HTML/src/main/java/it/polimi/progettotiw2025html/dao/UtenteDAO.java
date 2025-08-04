package it.polimi.progettotiw2025html.dao;

import it.polimi.progettotiw2025html.utils.ConnectionHandler;
import java.sql.*;
import it.polimi.progettotiw2025html.beans.*;

public class UtenteDAO {
    private final Connection connection;

    public UtenteDAO(Connection connection){
        this.connection = connection;
    }

    public Utente login(String username, String password) throws SQLException {
        String query = "SELECT * FROM utente WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {     //se l'utente esiste il ResultSet avrà una riga e quindi rs.next()=true
                    return new Utente(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("nome"),
                            rs.getString("cognome"),
                            rs.getString("indirizzo")
                    );
                }
            }
        }
        return null;
    }

    public boolean checkRegistration(String username) throws SQLException {
        String query = "SELECT * FROM utenti WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        ResultSet result = statement.executeQuery(); //Contiene tutte le righe trovate dal DB

        return result.isBeforeFirst(); //Ritorna false se è vuoto, non esiste nessun utente con quel username
    }

    public boolean signUp(Utente utente) throws SQLException {
        String query = "INSERT INTO utente (username, password, nome, cognome, indirizzo) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, utente.getUsername());
            stmt.setString(2, utente.getPassword());
            stmt.setString(3, utente.getNome());
            stmt.setString(4, utente.getCognome());
            stmt.setString(5, utente.getIndirizzo());

            stmt.executeUpdate(); //serve per eseguire operazioni SQL che modificano i dati
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false; // Username già esistente
        }
    }

}
