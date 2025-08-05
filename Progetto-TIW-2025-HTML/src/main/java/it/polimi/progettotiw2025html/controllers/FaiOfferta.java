package it.polimi.progettotiw2025html.controllers;

import it.polimi.progettotiw2025html.beans.Asta;
import it.polimi.progettotiw2025html.beans.Offerta;
import it.polimi.progettotiw2025html.beans.Utente;
import it.polimi.progettotiw2025html.dao.AstaDAO;
import it.polimi.progettotiw2025html.dao.OffertaDAO;
import it.polimi.progettotiw2025html.utils.ConnectionHandler;
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
        String path;

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");
        Integer idAsta = (Integer) session.getAttribute("idAsta");
        if (idAsta == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Errore nella richiesta: idAsta non presente nella sessione.");
            return;
        }

        double prezzo = 0.0;
        try {
            prezzo = Double.parseDouble(request.getParameter("prezzo"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Prezzo non valido.");
            return;
        }

        if (prezzo <= 0) {
            ctx.setVariable("errorMsg", "Il prezzo deve essere maggiore di zero.");
            path = "offerta?idAsta=" + idAsta;
            templateEngine.process(path, ctx, response.getWriter());
            return;
        }
        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            Asta asta = astaDAO.getAstaById(idAsta);
            if (asta.getScadenza().isBefore(LocalDateTime.now())) {
                ctx.setVariable("errorMsg", "Asta scaduta, non è possibile fare offerte.");
                path = "offerta?idAsta=" + idAsta;
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }
            if (asta.getRialzoMinimo() > prezzo - asta.getPrezzoIniziale()) {
                ctx.setVariable("errorMsg", "L'offerta deve rialzare il prezzo almeno quanto il rialzo minimo");
                path = "offerta?idAsta=" + idAsta;
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }

            OffertaDAO offertaDAO = new OffertaDAO(connection);
            Offerta maxOfferta = offertaDAO.getMaxOffertaByIdAsta(idAsta);
            if (maxOfferta != null && maxOfferta.getPrezzo() > prezzo) {
                ctx.setVariable("errorMsg", "L'offerta deve essere superiore all'offerta massima attuale di " + maxOfferta.getPrezzo());
                path = "offerta?idAsta=" + idAsta;
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }
            offertaDAO.addOfferta(utente.getUsername(), prezzo, idAsta);
            path = request.getContextPath() + "offerte?idAsta=" + idAsta;
            response.sendRedirect(path);
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server");
        }
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
