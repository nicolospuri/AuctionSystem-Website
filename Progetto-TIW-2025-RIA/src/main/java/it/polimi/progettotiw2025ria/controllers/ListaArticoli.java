package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/ListaArticoliServlet")
public class ListaArticoliServlet extends HttpServlet {

    private ArticoloDAO articoloDAO = new ArticoloDAO(connection);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // recupero username dal parametro (oppure dalla sessione)
        String username = req.getParameter("username");

        List<Articolo> articoli;
        try {
            if (username != null && !username.isEmpty()) {
                articoli = articoloDAO.findArticlesByUser(username);
            } else {
                articoli = new ArrayList<>(); // nessun utente = nessun articolo
            }

            // Converti in JSON
            String json = new Gson().toJson(articoli);

            // Imposta tipo di risposta
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json);

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Errore caricamento articoli\"}");
        }
    }
    //todo: sistemare la classe e showup degli articoli nella tabella HTML
}
