package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/AggiungiArticolo")
@MultipartConfig
public class AggiungiArticolo extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    @Override
    public void init() throws UnavailableException {
        try {
            connection = ConnectionHandler.getConnection();
        } catch (UnavailableException e) {
            throw new UnavailableException("Database connection unavailable");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String nome = request.getParameter("nome");
        String descrizione = request.getParameter("descrizione");
        String prezzoParam = request.getParameter("prezzo");

        // Validazione input
        if (nome == null || nome.trim().isEmpty()
                || descrizione == null || descrizione.trim().isEmpty()
                || prezzoParam == null || prezzoParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false, \"error\":\"Tutti i campi sono obbligatori\"}");
            System.err.println("[AggiungiArticolo] Errore: campi mancanti.");
            return;
        }

        double prezzo;
        try {
            prezzo = Double.parseDouble(prezzoParam.trim());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false, \"error\":\"Il prezzo deve essere un numero valido\"}");
            System.err.println("[AggiungiArticolo] Errore: prezzo non valido -> " + prezzoParam);
            return;
        }

        if (prezzo <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false, \"error\":\"Il prezzo deve essere maggiore di zero\"}");
            System.err.println("[AggiungiArticolo] Errore: prezzo <= 0");
            return;
        }

        // Recupero utente dalla sessione
        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;
        if (utente == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false, \"error\":\"Sessione utente scaduta\"}");
            System.err.println("[AggiungiArticolo] Errore: utente non trovato in sessione");
            return;
        }

        try {
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);

            // Inserimento nel DB
            articoloDAO.addArticolo(nome, descrizione, utente.getUsername(), prezzo);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false, \"error\":\"Errore DB: " + e.getMessage() + "\"}");
            System.err.println("[AggiungiArticolo] Errore SQL: " + e.getMessage());
        }
    }

    @Override
    public void destroy() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
