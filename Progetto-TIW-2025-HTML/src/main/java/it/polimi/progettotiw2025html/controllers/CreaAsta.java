package it.polimi.tiw.Servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.ConnectionManager;
import it.polimi.tiw.dao.ArticoliDAO;
import it.polimi.tiw.dao.ArticoliDAOImpl;
import it.polimi.tiw.dao.AsteDAO;
import it.polimi.tiw.dao.AsteDAOImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/CreaAsta")
public class CreaAstaServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private AsteDAO asteDAO;
    private ArticoliDAO articoliDAO;

    @Override
    public void init() throws ServletException {
        asteDAO = new AsteDAOImpl();
        articoliDAO = new ArticoliDAOImpl();
    }

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

        List<Integer> articoliIds = new ArrayList<>();
        try {
            for (String idStr : articoliSelezionati) {
                articoliIds.add(Integer.parseInt(idStr));
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Codici articoli non validi");
            return;
        }

        try (Connection conn = ConnectionManager.getConnection()) {
            // Verifica che gli articoli appartengano all’utente
            if (!articoliDAO.areAllArticlesOfUser(conn, username, articoliIds)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Puoi selezionare solo i tuoi articoli");
                return;
            }

            // Verifica che non siano già in un'asta
            if (!articoliDAO.areAllArticlesFree(conn, articoliIds)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Alcuni articoli sono già in un'asta");
                return;
            }

            conn.setAutoCommit(false);
            try {
                int prezzoIniziale = articoliDAO.getSumOfPrice(conn, articoliIds);

                // Rialzo minimo e scadenza puoi aggiungerli come parametri del form se servono
                float rialzoMinimo = 1.0f; // valore di default
                java.sql.Date dataFine = java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(7));
                java.sql.Time oraFine = java.sql.Time.valueOf(java.time.LocalTime.now().plusHours(1));

                int idAsta = asteDAO.insertNewAsta(
                        conn,
                        username,
                        prezzoIniziale,
                        rialzoMinimo,
                        dataFine,
                        oraFine
                );

                articoliDAO.updateIdAstaInArticles(conn, articoliIds, idAsta);

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
