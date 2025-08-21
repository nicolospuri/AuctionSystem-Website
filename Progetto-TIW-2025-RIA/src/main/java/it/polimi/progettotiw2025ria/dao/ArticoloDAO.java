package it.polimi.progettotiw2025ria.dao;

import it.polimi.progettotiw2025ria.beans.Articolo;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArticoloDAO {
    private final Connection connection;

    public ArticoloDAO(Connection connection) {
        this.connection = connection;
    }


    public void addArticolo(String Nome, String Descrizione, String Proprietario, Double Prezzo) throws SQLException {
        String query = "INSERT INTO articolo (Nome, Descrizione, Proprietario, Prezzo) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, Nome);
            stmt.setString(2, Descrizione);
            stmt.setString(3, Proprietario);
            stmt.setDouble(4, Prezzo);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Errore durante l'aggiunta dell'articolo: " + e.getMessage());
        }
    }

    public Articolo addArticolo(String nome, String descrizione, String immagine, double prezzo, String proprietario) throws SQLException {
        String sql = "INSERT INTO articolo (Nome, Descrizione, immagine, Prezzo, Proprietario) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.setString(2, descrizione);
            ps.setString(3, immagine);
            ps.setDouble(4, prezzo);
            ps.setString(5, proprietario);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int codice = rs.getInt(1);
                    return new Articolo(codice, nome, descrizione, immagine, prezzo, proprietario);
                }
            }
        }
        return null;
    }


    public List<Articolo> getArticoliDisponibili(String proprietario) throws SQLException {
        String sql = "SELECT * FROM articolo WHERE idAsta IS NULL AND Proprietario = ?";
        List<Articolo> articoli = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, proprietario);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            articoli.add(new Articolo(rs.getInt("Codice"),
                    rs.getString("Nome"),
                    rs.getString("Descrizione"),
                    rs.getString("Immagine"),
                    rs.getDouble("Prezzo"),
                    rs.getString("Proprietario")));
        }
        return articoli;
    }

    public List<Articolo> getArticoliByIdAsta(int idAsta) throws SQLException {
        String sql = "SELECT * FROM articolo WHERE IdAsta = ?";
        List<Articolo> result = new ArrayList<>();

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, idAsta);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            result.add(new Articolo(rs.getInt("Codice"),
                    rs.getString("Nome"),
                    rs.getString("Descrizione"),
                    rs.getString("Immagine"),
                    rs.getDouble("Prezzo"),
                    rs.getString("Proprietario")));
        }
        return result;
    }

    public boolean areAllArticlesOfUser(String usernameProprietario, ArrayList<Integer> idArticoli) throws SQLException {
        String query = "SELECT codice FROM articolo WHERE proprietario = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, usernameProprietario);

            try (ResultSet resultSet = ps.executeQuery()) {
                // trasformo il result set in un array di integer (lista di codici dell'utente)
                Set<Integer> userArticles = new HashSet<>();
                while (resultSet.next()) {
                    userArticles.add(resultSet.getInt("codice"));
                }

                // appena uno degli articoli da inserire non è tra quelli dell'utente, restituisce false
                for (int idArticolo : idArticoli) {
                    if (!userArticles.contains(idArticolo)) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public boolean areAllArticlesFree(ArrayList<Integer> idArticoliToInsertInAsta) throws SQLException {
        String query = "SELECT count(*) AS notFreeArticles FROM articolo WHERE IdAsta IS NOT NULL AND Codice IN (";
        for (int i = 0; i < idArticoliToInsertInAsta.size(); i++) {        // i dati presenti in idArticoliToInsertInAsta sono sanificati e non si rischia SQL injection
            query += idArticoliToInsertInAsta.get(i);
            if (i < idArticoliToInsertInAsta.size() - 1) {
                query += ", ";
            }
        }
        query += ")";

        try (
                PreparedStatement ps = connection.prepareStatement(query);
                ResultSet resultSet = ps.executeQuery()
        ) {
            if (resultSet.next() && resultSet.getInt("notFreeArticles") > 0) {    // false se almeno un articolo non è libero
                return false;
            }
            return true;
        }
    }

    public int getSumOfPrice(ArrayList<Integer> articoliIds) throws SQLException {
        if (articoliIds == null || articoliIds.isEmpty()) {
            return 0; // nessun elemento
        }

        StringBuilder query = new StringBuilder("SELECT SUM(prezzo) FROM articolo WHERE codice IN (");
        for (int i = 0; i < articoliIds.size(); i++) {
            query.append(articoliIds.get(i));
            if (i < articoliIds.size() - 1) {
                query.append(", ");
            }
        }
        query.append(")");

        try (
                PreparedStatement stmt = connection.prepareStatement(query.toString());
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public void updateIdAstaInArticles(ArrayList<Integer> articles, int idAsta) throws SQLException {
        if (articles == null || articles.isEmpty()) return;

        StringBuilder query = new StringBuilder("UPDATE Articolo SET IdAsta = ? WHERE codice IN (");
        for (int i = 0; i < articles.size(); i++) {
            query.append("?");
            if (i < articles.size() - 1) {
                query.append(", ");
            }
        }
        query.append(")");

        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            stmt.setInt(1, idAsta);
            for (int i = 0; i < articles.size(); i++) {
                stmt.setInt(i + 2, articles.get(i)); // +2 perché il primo parametro è idAsta
            }
            stmt.executeUpdate();
        }
    }

    public List<Articolo> findArticlesByUser(String username) throws SQLException {
        List<Articolo> articoli = new ArrayList<>();

        String query = "SELECT codice, nome, descrizione, immagine, prezzo, IdAsta, proprietario " +
                "FROM articolo WHERE proprietario = ?";

        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, username);

            try (ResultSet result = pstatement.executeQuery()) {
                while (result.next()) {
                    String immagine = result.getString("immagine");
                    if (immagine == null || immagine.trim().isEmpty()) {
                        immagine = "img/default.png"; // default se nullo
                    }

                    Articolo articolo = new Articolo(
                            result.getInt("codice"),
                            result.getString("nome"),
                            result.getString("descrizione"),
                            immagine,
                            result.getDouble("prezzo"),
                            result.getString("proprietario")
                    );
                    articolo.setIdAsta(result.getInt("IdAsta"));


                    articoli.add(articolo);
                }
            }
        }

        return articoli;
    }
}