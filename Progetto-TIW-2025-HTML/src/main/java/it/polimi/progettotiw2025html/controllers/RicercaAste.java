package it.polimi.progettotiw2025html.controllers;

import it.polimi.progettotiw2025html.beans.Articolo;
import it.polimi.progettotiw2025html.beans.Asta;
import it.polimi.progettotiw2025html.beans.Utente;
import it.polimi.progettotiw2025html.dao.ArticoloDAO;
import it.polimi.progettotiw2025html.dao.AstaDAO;
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
import java.util.List;

@WebServlet("/RicercaAste")
public class RicercaAste extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public RicercaAste() {
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
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        String path;

        String keyword = request.getParameter("keyword");
        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            List<Asta> asteTrovate = null;
            List<Articolo> articoli = null;
            if (keyword != null && !keyword.isEmpty()) {
                asteTrovate = astaDAO.getAsteAperteByKeyword(keyword);
                for (Asta a : asteTrovate) {
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);
                }
            }
            List<Asta> asteVinte = null;
            if (utente != null) {
                asteVinte = astaDAO.getAsteVinteByUsername(utente.getUsername());
            }

            ctx.setVariable("keyword", keyword);

            if (asteTrovate != null && asteTrovate.isEmpty()) {
                ctx.setVariable("asteTrovateMsg", "Nessuna asta trovata");
            } else {
                for (Asta a : asteVinte) {
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);
                }
                ctx.setVariable("asteTrovate", asteTrovate);
            }
            if (asteVinte != null && asteVinte.isEmpty()) {
                ctx.setVariable("asteVinteMsg", "Nessuna asta vinta");
            } else {
                for (Asta a : asteVinte) {
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);
                }
                ctx.setVariable("asteVinte", asteVinte);
            }
            path = "acquisto";
            templateEngine.process(path, ctx, response.getWriter());
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server durante la ricerca delle aste");
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
