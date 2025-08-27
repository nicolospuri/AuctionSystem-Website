package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/GetArticoliServlet")
public class GetArticoliServlet extends HttpServlet {
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        if (utente == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false, \"error\":\"Sessione scaduta\"}");
            return;
        }

        try {
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            List<Articolo> articoli = articoloDAO.getArticoliDisponibili(utente.getUsername());

            //array di articoli in formato JSON
            JSONArray arr = new JSONArray();
            for (Articolo a : articoli) {
                JSONObject obj = new JSONObject();
                obj.put("codice", a.getCodice());
                obj.put("nome", a.getNome());
                obj.put("descrizione", a.getDescrizione());
                obj.put("prezzo", a.getPrezzo());
                arr.put(obj);
            }

            JSONObject json = new JSONObject();
            json.put("success", true);
            json.put("articoli", arr);

            /*-response.getWriter(): ottiene un oggetto PrintWriter associato alla risposta HTTP,
                    che permette di scrivere dati nel corpo della risposta.
              -json.toString(): converte l'oggetto JSONObject json in una stringa JSON.
              -.write(...): scrive la stringa JSON nella risposta HTTP, inviandola al client che ha fatto la richiesta.*/
            response.getWriter().write(json.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false, \"error\":\"Errore DB\"}");
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