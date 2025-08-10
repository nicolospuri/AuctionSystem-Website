package it.polimi.progettotiw2025html.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import it.polimi.progettotiw2025html.dao.ArticoloDAO;
import it.polimi.progettotiw2025html.dao.AstaDAO;
import it.polimi.progettotiw2025html.utils.ConnectionHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/CreaAsta")
public class CreaAsta extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getSession(false) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String username = (String) request.getSession().getAttribute("username");
        String[] articoliSelezionati = request.getParameterValues("articoliSelezionati");

        if (articoliSelezionati == null || articoliSelezionati.length == 0) {
            response.sendRedirect(request.getContextPath() + "/ArticoliDisponibili?noArticoliSelezionati=true");
            return;
        }

        ArrayList<Integer> articoliIds = new ArrayList<>();
        try {
            for (String idStr : articoliSelezionati) {
                articoliIds.add(Integer.parseInt(idStr));
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Codici articoli non validi");
            return;
        }

        try (Connection conn = ConnectionHandler.getConnection()) {
            ArticoloDAO articoloDAO = new ArticoloDAO(conn);
            AstaDAO astaDAO = new AstaDAO(conn);

            // Controlla che appartengano all'utente
            if (!articoloDAO.areAllArticlesOfUser(conn, username, articoliIds)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Puoi selezionare solo i tuoi articoli");
                return;
            }

            // Controlla che siano liberi
            if (!articoloDAO.areAllArticlesFree(conn, articoliIds)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Alcuni articoli sono già in un'asta");
                return;
            }

            conn.setAutoCommit(false);
            try {
                int prezzoIniziale = articoloDAO.getSumOfPrice(conn,articoliIds);

                // Parametri fissi (puoi sostituirli con valori dal form)
                float rialzoMinimo = 1.0f;
                Date dataFine = Date.valueOf(LocalDate.now().plusDays(7));
                Time oraFine = Time.valueOf(LocalTime.now().plusHours(1));

                int idAsta = astaDAO.insertNewAsta(
                        username,
                        prezzoIniziale,
                        rialzoMinimo,
                        dataFine,
                        oraFine
                );

                articoloDAO.updateIdAstaInArticles(articoliIds, idAsta);

                conn.commit();
                response.sendRedirect(request.getContextPath() + "/aste?creazioneOk=true");
            } catch (SQLException e) {
                conn.rollback();
                throw new ServletException("Errore durante la creazione dell'asta", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
