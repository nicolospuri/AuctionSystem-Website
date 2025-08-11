package it.polimi.progettotiw2025html.controllers;

import it.polimi.progettotiw2025html.beans.Articolo;
import it.polimi.progettotiw2025html.beans.Asta;
import it.polimi.progettotiw2025html.beans.Offerta;
import it.polimi.progettotiw2025html.beans.Utente;
import it.polimi.progettotiw2025html.dao.ArticoloDAO;
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
import java.util.List;

@WebServlet("/OfferteServlet")
public class OfferteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public OfferteServlet() {
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
        String path = "offerta";

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");
        Integer idAsta = 0;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
            ctx.setVariable("idAsta", idAsta);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID Asta non valido");
            return;
        }

        try {
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            List<Articolo> articoli = articoloDAO.getArticoliByIdAsta(idAsta);
            ctx.setVariable("articoli", articoli);
            AstaDAO astaDAO = new AstaDAO(connection);
            Asta asta = astaDAO.getAstaById(idAsta);
            ctx.setVariable("rialzoMinimo", asta.getRialzoMinimo());

            OffertaDAO offertaDAO = new OffertaDAO(connection);
            List<Offerta> offerte = offertaDAO.getOfferteByIdAsta(idAsta);
            if (offerte != null && !offerte.isEmpty()) {
                ctx.setVariable("offerte", offerte);
            } else {
                ctx.setVariable("offerteMsg", "Nessuna offerta trovata");
            }

            if (utente != null && !utente.getUsername().equals(asta.getProprietario())) {
                ctx.setVariable("canOffer", true);
            } else {
                ctx.setVariable("canOffer", false);
            }
            if (request.getParameter("errorMsg") != null) {
                ctx.setVariable("errorMsg", request.getParameter("errorMsg"));
            }
            templateEngine.process(path, ctx, response.getWriter());
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server");
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
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
