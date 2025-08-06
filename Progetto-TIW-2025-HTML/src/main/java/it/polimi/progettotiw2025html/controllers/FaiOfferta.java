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
import java.time.LocalDateTime;
import java.util.List;

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

        HttpSession session = request.getSession();
        Utente utente = (Utente) session.getAttribute("utente");
        Integer idAsta = (Integer) session.getAttribute("idAsta");

        String path = "offerta";
        if (idAsta == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Errore nella richiesta: idAsta non presente nella sessione.");
            return;
        }

        try {
            // variabili di contesto per ricaricare la pagina delle offerte corretta
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
            }

            // Controllo sul prezzo offerto
            double prezzo = 0.0;
            try {
                prezzo = Double.parseDouble(request.getParameter("prezzoOfferto"));
            } catch (NumberFormatException e) {
                ctx.setVariable("errorMsg", "Prezzo non valido.");
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }
            if (prezzo <= 0) {
                ctx.setVariable("errorMsg", "Il prezzo deve essere maggiore di zero.");
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }

            // Asta scaduta
            if (asta.getScadenza().isBefore(LocalDateTime.now())) {
                ctx.setVariable("errorMsg", "Asta scaduta, non è possibile fare offerte");
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }
            // Asta propria
            if (asta.getProprietario().equals(utente.getUsername())) {
                ctx.setVariable("errorMsg", "Non puoi fare offerte su una tua asta");
                templateEngine.process(path, ctx, response.getWriter());
                return;
            }
            // Offerte troppo basse
            if (offerte != null && !offerte.isEmpty()) {
                Offerta maxOfferta = offertaDAO.getMaxOffertaByIdAsta(idAsta);
                if (asta.getRialzoMinimo() > prezzo - asta.getPrezzoIniziale() ||
                        (maxOfferta != null && asta.getRialzoMinimo() > prezzo - maxOfferta.getPrezzo())) {
                    ctx.setVariable("errorMsg", "L'offerta deve rialzare il prezzo almeno quanto il rialzo minimo");
                    templateEngine.process(path, ctx, response.getWriter());
                    return;
                }
            }

            offertaDAO.addOfferta(utente.getUsername(), prezzo, idAsta);
            offerte = offertaDAO.getOfferteByIdAsta(idAsta);
            ctx.setVariable("offerte", offerte);
            templateEngine.process(path, ctx, response.getWriter());
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
