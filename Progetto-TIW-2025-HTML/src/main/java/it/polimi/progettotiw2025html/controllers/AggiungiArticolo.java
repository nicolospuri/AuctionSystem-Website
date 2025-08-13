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
import jakarta.servlet.http.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

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

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("Post di AggiungiArticolo");

        // Parametri testuali
        String nome = request.getParameter("nome");
        String descrizione = request.getParameter("descrizione");
        String prezzoParam = request.getParameter("prezzo");

        // Validazione base
        if (nome == null || nome.trim().isEmpty() ||
                descrizione == null || descrizione.trim().isEmpty() ||
                prezzoParam == null || prezzoParam.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tutti i campi sono obbligatori");
            return;
        }

        double prezzo;
        try {
            prezzo = Double.parseDouble(prezzoParam.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Il prezzo deve essere un numero valido");
            return;
        }

        // Gestione file immagine
        String immaginePath = null;
        try {
            Part filePart = request.getPart("immagine"); // nome del campo file
            if (filePart != null && filePart.getSize() > 0) {
                // crea cartella uploads se non esiste
                String uploadDir = getServletContext().getRealPath("") + File.separator + "uploads";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdir();

                // genera nome univoco per evitare conflitti
                String fileName = UUID.randomUUID() + "_" + Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

                // salva il file fisicamente
                filePart.write(uploadDir + File.separator + fileName);

                // memorizza percorso relativo
                immaginePath = "uploads/" + fileName;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel caricamento dell'immagine");
            return;
        }

        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());

        Utente utente = (Utente) request.getSession().getAttribute("utente");
        if (utente == null) {
            ctx.setVariable("errorMsg", "Utente non trovato");
            templateEngine.process("index", ctx, response.getWriter());
            return;
        }

        try {
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            if (!utenteDAO.checkRegistration(utente.getUsername())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Utente non registrato");
                return;
            }

            Articolo articolo = new Articolo(0, nome, descrizione, immaginePath, prezzo, utente.getUsername());
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            articoloDAO.addArticolo(articolo);

            response.sendRedirect(request.getContextPath() + "/VendoServlet");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante l'aggiunta dell'articolo: " + e.getMessage());
        }
    }
}
