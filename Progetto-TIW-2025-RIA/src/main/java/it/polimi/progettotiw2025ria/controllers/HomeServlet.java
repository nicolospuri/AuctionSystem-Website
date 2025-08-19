package it.polimi.progettotiw2025ria.controllers;

import com.google.gson.JsonObject;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/HomeServlet")
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public HomeServlet() {
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = "home";

        if(request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }

        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        templateEngine.process(path, ctx, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        String username = utente.getUsername();

        boolean userLastActionWasAddedAsta = false;
        boolean lastActionFound = false;

        boolean primoAccesso = false;
        if (session.getAttribute("primoAccesso") != null) {
            primoAccesso = (boolean) session.getAttribute("primoAccesso");
        }

        // Se non è il primo accesso, cerco il cookie che indica se l'utente ha aggiunto un'asta
        if (!primoAccesso) {
            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (c.getName().equals("lastActionCreaAsta"+username)) {
                        userLastActionWasAddedAsta = Boolean.parseBoolean(c.getValue());
                        c.setMaxAge(60*60*24*30);
                        lastActionFound = true;
                        response.addCookie(c);
                        break;
                    }
                }
            }
        }
        // Se è il primo accesso o il cookie lastAction è scaduto, crea un nuovo cookie con valore false
        if(!lastActionFound) {
            Cookie lastAction = new Cookie("lastActionCreaAsta"+username, "false");
            lastAction.setMaxAge(60*60*24*30);
            response.addCookie(lastAction);
        }

        Cookie renderAllTablesAste = new Cookie("renderAllTablesAste"+username, "true");
        renderAllTablesAste.setMaxAge(60*60*24*30);
        response.addCookie(renderAllTablesAste);

        Cookie renderTableAsteAperte = new Cookie("renderTableAsteAperte"+username, "true");
        renderTableAsteAperte.setMaxAge(60*60*24*30);
        response.addCookie(renderTableAsteAperte);

        Cookie renderArticoli = new Cookie("renderArticoli"+username, "true");
        renderArticoli.setMaxAge(60*60*24*30);
        response.addCookie(renderArticoli);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userLastActionWasAddedAsta", userLastActionWasAddedAsta);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(jsonObject);
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
