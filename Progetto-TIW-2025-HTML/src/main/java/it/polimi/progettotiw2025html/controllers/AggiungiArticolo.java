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

@WebServlet("/AggiungiArticolo")
public class AggiungiArticolo extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public AggiungiArticolo() {
        super();
    }

    @Override
    public void init() throws UnavailableException {
        System.out.println("Inizializzando AggiungiArticolo");
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

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("Post di AggiungiArticolo");
        String nome = request.getParameter("nome");
        String descrizione = request.getParameter("descrizione");
        String immagine = request.getParameter("immagine");
        double prezzo = Double.parseDouble(request.getParameter("prezzo"));
        String proprietario = request.getParameter("proprietario");

        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        String path = null;

        Utente utente = (Utente) request.getSession().getAttribute("utente");
        if (utente == null) {
            path = "index";
            ctx.setVariable("errorMsg", "Utente non trovato");
            templateEngine.process(path, ctx, response.getWriter());
            return;
        }

        try {
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            if (!utenteDAO.checkRegistration(proprietario)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Utente non registrato");
                return;
            }

            Articolo articolo = new Articolo(0, nome, descrizione, immagine, prezzo, proprietario);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            articoloDAO.addArticolo(articolo);

            response.sendRedirect(request.getContextPath() + "/VendoServlet");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante l'aggiunta dell'articolo: " + e.getMessage());
        }
    }
}
