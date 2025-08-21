package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Utente;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        String path = request.getContextPath() + "/VendoServlet";

        // Controllo login
        if (request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        if (request.getSession().getAttribute("utente") == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        Utente utente = (Utente) request.getSession().getAttribute("utente");
        if (utente == null) {
            ctx.setVariable("errorMsg", "Utente non trovato");
            templateEngine.process("index", ctx, response.getWriter());
            return;
        }

        String username = utente.getUsername();
        String[] articoliSelezionati = request.getParameterValues("articoliSelezionati");

        // Nessun articolo selezionato
        if (articoliSelezionati == null || articoliSelezionati.length == 0) {
            path += "?nessunArticoloMsg=Nessun articolo selezionato";
            response.sendRedirect(path);
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

        ArticoloDAO articoloDAO = new ArticoloDAO(connection);
        AstaDAO astaDAO = new AstaDAO(connection);

        try {
            // Controlla che gli articoli appartengano all'utente
            if (!articoloDAO.areAllArticlesOfUser(username, articoliIds)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Puoi selezionare solo i tuoi articoli");
                return;
            }

            // Controlla che siano liberi
            if (!articoloDAO.areAllArticlesFree(articoliIds)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Alcuni articoli sono già in un'asta");
                return;
            }

            // Prezzo iniziale = somma dei prezzi articoli
            double prezzoIniziale = articoloDAO.getSumOfPrice(articoliIds);

            // Parametri asta
            String rialzoMinimoParam = request.getParameter("rialzoMinimo");
            int rialzoMinimo;

            if (rialzoMinimoParam == null || rialzoMinimoParam.trim().isEmpty()) {
                path += "?rialzoMsg=Il rialzo minimo deve essere maggiore di 0";
                response.sendRedirect(path);
                return;
            }

            try {
                rialzoMinimo = Integer.parseInt(rialzoMinimoParam.trim());
                if (rialzoMinimo <= 0) {
                    path += "?rialzoMsg=Il rialzo minimo deve essere maggiore di 0";
                    response.sendRedirect(path);
                    return;
                }
            } catch (NumberFormatException e) {
                path += "?rialzoMsg=Rialzo minimo non valido";
                response.sendRedirect(path);
                return;
            }

            String scadenzaParam = request.getParameter("scadenza");
            LocalDateTime scadenza;

            if (scadenzaParam == null || scadenzaParam.trim().isEmpty()) {
                path += "?scadenzaMsg=La scadenza è obbligatoria";
                response.sendRedirect(path);
                return;
            }

            try {
                scadenza = LocalDateTime.parse(scadenzaParam); // richiede formato ISO: yyyy-MM-ddTHH:mm
                if (scadenza.isBefore(LocalDateTime.now())) {
                    path += "?scadenzaMsg=La scadenza deve essere nel futuro";
                    response.sendRedirect(path);
                    return;
                }
            } catch (DateTimeParseException e) {
                path += "?scadenzaMsg=Formato scadenza non valido";
                response.sendRedirect(path);
                return;
            }

            // Inserimento asta
            int idAsta = astaDAO.insertNewAsta(
                    username,
                    prezzoIniziale,
                    rialzoMinimo,
                    Timestamp.valueOf(scadenza)
            );

            // Aggiornamento articoli con id_asta
            articoloDAO.updateIdAstaInArticles(articoliIds, idAsta);

            response.sendRedirect(path);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
}
