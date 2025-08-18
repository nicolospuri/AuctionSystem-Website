package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.beans.Offerta;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.dao.OffertaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/ChiudiAsta")
public class ChiudiAsta extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public ChiudiAsta() {
        super();
    }

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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");
        Integer idAsta = 0;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID Asta non valido");
            return;
        }
        String path = request.getContextPath() + "/DettaglioAstaServlet?idAsta=" + idAsta;

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            Asta asta = null;
            if (utente != null) {
                asta = astaDAO.getAstaById(idAsta);
                if (asta == null) {
                    path += "&errorMds=Nessuna asta trovata";
                } else if (asta.isChiusa()) {
                    path += "&errorMsg=Asta già chiusa";
                } else if (!asta.getProprietario().equals(utente.getUsername())) {
                    path += "&errorMsg=Non sei il proprietario dell'asta";
                } else if (!asta.canBeClosed()) {
                    path += "&errorMsg=Tempo rimanente maggiore di 0, impossibile chiudere l'asta";
                } else {
                    OffertaDAO offertaDAO = new OffertaDAO(connection);
                    Offerta offertaMax = offertaDAO.getMaxOffertaByIdAsta(idAsta);
                    if (offertaMax == null) {
                        if (astaDAO.chiudiAsta(idAsta)) {
                            path += "&successMsg=Asta chiusa con successo";
                        } else {
                            path += "&errorMsg=Errore durante la chiusura dell'asta";
                        }
                    } else {
                        if (astaDAO.chiudiAsta(idAsta, offertaMax.getOfferente())) {
                            path += "&successMsg=Asta chiusa con successo";
                        } else {
                            path += "&errorMsg=Errore durante la chiusura dell'asta";
                        }
                    }
                }
            }
            response.sendRedirect(path);
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server");
        }
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch(SQLException e){
            e.printStackTrace();
        }
    }
}
