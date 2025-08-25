package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.UtenteDAO;
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

@WebServlet("/SignUp")
public class SignUp extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public SignUp() {
        super();
    }

    @Override
    public void init() throws UnavailableException {
        System.out.println("Inizializzando SignUp");
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
        if (request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        // Prendo i parametri
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String nome = request.getParameter("nome");
        String cognome = request.getParameter("cognome");
        String indirizzo = request.getParameter("indirizzo");

        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        String path = null;

        // Check sui parametri
        if (username == null || password == null || cognome == null || indirizzo == null ||
                username.isEmpty() || password.isEmpty() || nome.isEmpty() || cognome.isEmpty() || indirizzo.isEmpty()) {
            path = "index";
            ctx.setVariable("errorMsg", "Credenziali vuote o mancanti");
            templateEngine.process(path, ctx, response.getWriter());
            return;
        }
        try {
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            Utente utente = new Utente(username, password, nome, cognome, indirizzo);
            // Controllo se l'username è già presente
            boolean valido = utenteDAO.checkRegistration(utente.getUsername());
            if(!valido){
                // Se non è presente, procedo con la registrazione
                boolean isRegistered = utenteDAO.signUp(utente);
                if (isRegistered) {
                    HttpSession session = request.getSession();
                    // Imposto l'utente come attributo di sessione
                    session.setAttribute("utente", utente);
                    path = request.getContextPath() + "/HomeServlet";
                    response.sendRedirect(path);
                } else {
                    path = "index";
                    ctx.setVariable("errorMsg", "Registrazione fallita: errore durante l'inserimento");
                    templateEngine.process(path, ctx, response.getWriter());
                }
            } else {
                path = "index";
                ctx.setVariable("errorMsg", "Registrazione fallita: utente già esistente");
                templateEngine.process(path, ctx, response.getWriter());
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SQL error: impossibile registrare l'utente");
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