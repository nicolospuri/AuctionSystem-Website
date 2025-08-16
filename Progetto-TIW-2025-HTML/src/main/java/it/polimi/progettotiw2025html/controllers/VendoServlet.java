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

@WebServlet("/VendoServlet")
public class VendoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public VendoServlet() {
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
        String path = "vendo";

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            OffertaDAO offertaDAO = new OffertaDAO(connection);
            List<Asta> asteAperte = null;
            List<Asta> asteChiuse = null;
            List<Articolo> articoli = null;
            Offerta offertaMax = null;

            if (utente != null) {
                asteAperte = astaDAO.getAsteAperteByUsername(utente.getUsername());
                asteChiuse = astaDAO.getAsteChiuseByUsername(utente.getUsername());
            }

            if (asteAperte == null || asteAperte.isEmpty()) {
                ctx.setVariable("asteAperteMsg", "Nessuna asta aperta");
            } else {
                for (Asta a : asteAperte) {
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);

                    a.setTempoMancante();

                    offertaMax = offertaDAO.getMaxOffertaByIdAsta(a.getId());
                    a.setOffertaMassima(offertaMax);
                    if (offertaMax != null) {
                        a.setPrezzoOffertaMassima(offertaMax.getPrezzo());
                    }
                }
                ctx.setVariable("asteAperte", asteAperte);
            }
            if (asteChiuse == null || asteChiuse.isEmpty()) {
                ctx.setVariable("asteChiuseMsg", "Nessuna asta chiusa");
            } else {
                for (Asta a : asteChiuse) {
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);

                    offertaMax = offertaDAO.getMaxOffertaByIdAsta(a.getId());
                    a.setOffertaMassima(offertaMax);
                    if (offertaMax != null) {
                        a.setPrezzoOffertaMassima(offertaMax.getPrezzo());
                    }
                }
                ctx.setVariable("asteChiuse", asteChiuse);
            }

            if (utente != null) {
                articoli = articoloDAO.getArticoliDisponibili(utente.getUsername());
            }
            if (articoli == null || articoli.isEmpty()) {
                ctx.setVariable("articoliMsg", "Non ci sono articoli disponibili al momento.");
            } else {
                ctx.setVariable("listaArticoli", articoli); // nuova variabile per la lista
            }

            if (request.getParameter("prezzoMsg") != null) {
                ctx.setVariable("prezzoMsg", request.getParameter("prezzoMsg"));
            }
            if (request.getParameter("rialzoMsg") != null) {
                ctx.setVariable("rialzoMsg", request.getParameter("rialzoMsg"));
            }
            if (request.getParameter("scadenzaMsg") != null) {
                ctx.setVariable("scadenzaMsg", request.getParameter("scadenzaMsg"));
            }
            if (request.getParameter("nessunArticoloMsg") != null) {
                ctx.setVariable("nessunArticoloMsg", request.getParameter("nessunArticoloMsg"));
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
        try {
            ConnectionHandler.closeConnection(connection);
        } catch(SQLException e){
            e.printStackTrace();
        }
    }
}
