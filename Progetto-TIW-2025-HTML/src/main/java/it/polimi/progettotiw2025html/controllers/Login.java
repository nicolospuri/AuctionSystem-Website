package it.polimi.progettotiw2025html.controllers;

import it.polimi.progettotiw2025html.beans.Utente;
import it.polimi.progettotiw2025html.dao.UtenteDAO;
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

@WebServlet("/Login")
public class Login extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public Login() {
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
        if (request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        String path;

        if(username == null || password == null || username.isEmpty() || password.isEmpty()){
            path = "index";
            ctx.setVariable("errorMsg", "Credenziali vuote o mancanti");
            templateEngine.process(path, ctx, response.getWriter());            // In caso di credenziali vuote o mancanti, torna al login
            return;
        }

        try {
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            // Check delle credenziali
            Utente utente = utenteDAO.login(username, password);
            if (utente != null) {
                HttpSession session = request.getSession();
                // Imposto l'utente come attributo di sessione
                session.setAttribute("utente", utente);      // Associa l'utente alla sessione
                // Reindirizzamento alla home
                path = request.getContextPath() + "/HomeServlet";
                response.sendRedirect(path);
            } else {
                path = "index";
                ctx.setVariable("errorMsg", "Username o password errati");
                templateEngine.process(path, ctx, response.getWriter());         // Torna al login in caso di errore
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno");
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
