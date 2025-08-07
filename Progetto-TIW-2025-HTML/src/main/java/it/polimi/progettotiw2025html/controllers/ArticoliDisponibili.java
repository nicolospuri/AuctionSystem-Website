package it.polimi.progettotiw2025html.controllers;

import it.polimi.progettotiw2025html.beans.Articolo;
import it.polimi.progettotiw2025html.beans.Utente;
import it.polimi.progettotiw2025html.dao.ArticoloDAO;
import it.polimi.progettotiw2025html.dao.UtenteDAO;
import it.polimi.progettotiw2025html.beans.Articolo;
import it.polimi.progettotiw2025html.dao.ArticoloDAO;
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

@WebServlet("/ArticoliDisponibili")
public class ArticoliDisponibili extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public  ArticoliDisponibili() { super(); }

    @Override
    public void init() throws UnavailableException {
        System.out.println("Inizializzando ArticoliDisponibili");
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

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");

        try{
            ArticoloDAO articoliDAO = new ArticoloDAO(connection);
            List<Articolo> articoli = null;
            if (utente != null) {
                articoli = articoliDAO.getArticoliDisponibili(utente.getUsername());
            }

            if (articoli == null || articoli.isEmpty()) {
                ctx.setVariable("articoliMsg", "Non ci sono articoli disponibili al momento.");
            } else {
                ctx.setVariable("listaArticoli", articoli); // nuova variabile per la lista
            }

            templateEngine.process("articoliDisponibili", ctx, response.getWriter());
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
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
