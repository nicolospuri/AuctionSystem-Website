package it.polimi.progettotiw2025html.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import it.polimi.progettotiw2025html.dao.ArticoloDAO;
import it.polimi.progettotiw2025html.dao.AstaDAO;
import it.polimi.progettotiw2025html.utils.ConnectionHandler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/CreaAsta")
public class CreaAsta extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public  CreaAsta() { super(); }

    @Override
    public void init() throws UnavailableException {
        ServletContext servletContext = getServletContext();

        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);

        templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        templateResolver.setSuffix(".html");

        try {
            connection = ConnectionHandler.getConnection();
        } catch (UnavailableException e) {
            throw new UnavailableException("Database connection unavailable");
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Controllo login
        if (request.getSession(false) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String username = (String) request.getSession().getAttribute("username");
        String[] articoliSelezionati = request.getParameterValues("articoliSelezionati");

        // Nessun articolo selezionato
        if (articoliSelezionati == null || articoliSelezionati.length == 0) {
            response.sendRedirect(request.getContextPath() + "/ArticoliDisponibili?noArticoliSelezionati=true");
            return;
        }

        // Conversione a lista di interi
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

            // Controlla che gli articoli appartengano all'utente
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
                // Prezzo iniziale = somma dei prezzi articoli
                double prezzoIniziale = articoloDAO.getSumOfPrice(conn, articoliIds);

                // Parametri asta
                int rialzoMinimo = 1; // fisso, o prendere da form
                LocalDateTime scadenza = LocalDateTime.now().plusDays(7).withHour(23).withMinute(0).withSecond(0);

                // Inserimento asta
                int idAsta = astaDAO.insertNewAsta(
                        username,
                        prezzoIniziale,
                        rialzoMinimo,
                        Timestamp.valueOf(scadenza)
                );

                // Aggiornamento articoli con id_asta
                articoloDAO.updateIdAstaInArticles(conn, articoliIds, idAsta);

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
