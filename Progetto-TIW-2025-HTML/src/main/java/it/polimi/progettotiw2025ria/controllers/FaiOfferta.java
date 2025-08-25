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
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/FaiOfferta")
public class FaiOfferta extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public FaiOfferta() {
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
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        if (request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        HttpSession session = request.getSession();
        if (session.getAttribute("utente") == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            ctx.setVariable("errorMsg", "Utente non trovato");
            templateEngine.process("index", ctx, response.getWriter());
            return;
        }
        Integer idAsta = 0;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID Asta non valido");
            return;
        }

        String path = request.getContextPath() + "/OfferteServlet?idAsta=" + idAsta;

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            Asta asta = astaDAO.getAstaById(idAsta);
            OffertaDAO offertaDAO = new OffertaDAO(connection);
            List<Offerta> offerte = offertaDAO.getOfferteByIdAsta(idAsta);
            double prezzo = 0.0;
            try {
                prezzo = Double.parseDouble(request.getParameter("prezzoOfferto"));
            } catch (NumberFormatException e) {
                path += "&errorMsg=Prezzo non valido";
                response.sendRedirect(path);
                return;
            }
            // Vari controlli sul prezzo e sull'asta
            if (prezzo <= 0) {
                path += "&errorMsg=Il prezzo deve essere maggiore di zero";
                response.sendRedirect(path);
                return;
            }

            // Asta scaduta
            if (asta.getScadenza().isBefore(LocalDateTime.now())) {
                path += "&errorMsg=Asta scaduta, non è possibile fare offerte";
                response.sendRedirect(path);
                return;
            }
            // Asta propria
            if (asta.getProprietario().equals(utente.getUsername())) {
                path += "&errorMsg=Non puoi fare offerte su una tua asta";
                response.sendRedirect(path);
                return;
            }

            // Offerta troppo bassa
            if (prezzo < asta.getPrezzoIniziale()) {
                path += "&errorMsg=L'offerta deve essere almeno pari al prezzo iniziale";
                response.sendRedirect(path);
                return;
            }

            if (offerte != null && !offerte.isEmpty()) {
                Offerta maxOfferta = offertaDAO.getMaxOffertaByIdAsta(idAsta);
                // Offerta troppo bassa
                if (maxOfferta != null && prezzo < maxOfferta.getPrezzo() + asta.getRialzoMinimo()) {
                    path += "&errorMsg=L'offerta deve rialzare il prezzo almeno quanto il rialzo minimo";
                    response.sendRedirect(path);
                    return;
                }
            }
            // Se vengono superati tutti i controlli, aggiungo l'offerta al database
            if (offertaDAO.addOfferta(utente.getUsername(), prezzo, idAsta)) {
                path += "&successMsg=Offerta effettuata con successo";
            } else {
                path += "&errorMsg=Errore durante l'inserimento dell'offerta";
            }
            // Torna alla pagina delle offerte
            response.sendRedirect(path);
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server");
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    public void destroy() {
        try{
            ConnectionHandler.closeConnection(connection);
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
