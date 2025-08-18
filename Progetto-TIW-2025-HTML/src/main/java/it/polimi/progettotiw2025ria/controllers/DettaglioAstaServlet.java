package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.beans.Offerta;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.dao.OffertaDAO;
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
import java.util.List;

@WebServlet("/DettaglioAstaServlet")
public class DettaglioAstaServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TemplateEngine templateEngine;
    private Connection connection;

    public DettaglioAstaServlet() {
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
        String path = "dettaglioAsta";

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            ctx.setVariable("errorMsg", "Utente non trovato");
            templateEngine.process("index", ctx, response.getWriter());
            return;
        }
        Integer idAsta = 0;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
            ctx.setVariable("idAsta", idAsta);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID Asta non valido");
            return;
        }

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            OffertaDAO offertaDAO = new OffertaDAO(connection);
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            List<Articolo> articoli = null;
            Asta asta = astaDAO.getAstaById(idAsta);
            if (asta != null) {
                articoli = articoloDAO.getArticoliByIdAsta(idAsta);
                asta.setArticoli(articoli);

                Offerta offertaMax = offertaDAO.getMaxOffertaByIdAsta(idAsta);
                asta.setOffertaMassima(offertaMax);
                if (offertaMax != null) {
                    asta.setPrezzoOffertaMassima(offertaMax.getPrezzo());
                }
                asta.setTempoMancante();

                if (asta.isChiusa()) {
                    if (asta.getAggiudicatario() != null) {
                        asta.setIndirizzoAggiudicatario(utenteDAO.getUtenteByUsername(asta.getAggiudicatario()).getIndirizzo());
                    }
                    ctx.setVariable("astaChiusa", asta);
                } else {
                    ctx.setVariable("astaAperta", asta);
                    if (asta.canBeClosed()) {
                        ctx.setVariable("canBeClosed", true);
                    }
                }
            }

            List<Offerta> offerte = offertaDAO.getOfferteByIdAsta(idAsta);
            if (offerte != null && !offerte.isEmpty()) {
                ctx.setVariable("offerte", offerte);
            } else {
                ctx.setVariable("offerteMsg", "Nessuna offerta trovata");
            }
            if (request.getParameter("errorMsg") != null) {
                ctx.setVariable("errorMsg", request.getParameter("errorMsg"));
            }
            if (request.getParameter("successMsg") != null) {
                ctx.setVariable("successMsg", request.getParameter("successMsg"));
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
