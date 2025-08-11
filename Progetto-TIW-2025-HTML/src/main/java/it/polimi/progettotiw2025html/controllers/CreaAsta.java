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

        // Recupera altri parametri dal form
        String rialzoMinimoStr = request.getParameter("rialzoMinimo");
        String dataFineStr = request.getParameter("dataFine");
        String oraFineStr = request.getParameter("oraFine");

        if (rialzoMinimoStr == null || dataFineStr == null || oraFineStr == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri mancanti per creare l'asta");
            return;
        }

        float rialzoMinimo;
        try {
            rialzoMinimo = Float.parseFloat(rialzoMinimoStr);
            if (rialzoMinimo <= 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Rialzo minimo non valido");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Rialzo minimo non è un numero valido");
            return;
        }

        Timestamp scadenza;
        try {
            LocalDate data = LocalDate.parse(dataFineStr);
            LocalTime ora = LocalTime.parse(oraFineStr);
            if (data.isBefore(LocalDate.now()) || (data.isEqual(LocalDate.now()) && ora.isBefore(LocalTime.now()))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "La scadenza deve essere nel futuro");
                return;
            }
            scadenza = Timestamp.valueOf(LocalDateTime.of(data, ora));
        } catch (DateTimeParseException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato data/ora scadenza non valido");
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
                double prezzoIniziale = articoloDAO.getSumOfPrice(conn, articoliIds);

                int idAsta = astaDAO.insertNewAsta(
                        username,
                        prezzoIniziale,
                        rialzoMinimo,
                        scadenza // unica data-ora combinata
                );

                //articoloDAO.updateIdAstaInArticles(articoliIds, idAsta);

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
