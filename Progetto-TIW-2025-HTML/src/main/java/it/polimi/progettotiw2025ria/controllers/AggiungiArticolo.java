package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.UtenteDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@MultipartConfig
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
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ServletContext servletContext = getServletContext();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response), request.getLocale());
        String path = request.getContextPath() + "/VendoServlet";

        if (request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        if (request.getSession().getAttribute("utente") == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        Utente utente = (Utente) request.getSession().getAttribute("utente");
        if (utente == null) {
            ctx.setVariable("errorMsg", "Utente non trovato");
            templateEngine.process("index", ctx, response.getWriter());
            return;
        }

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

        double prezzo = 0.0;
        try {
            prezzo = Double.parseDouble(prezzoParam.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Il prezzo deve essere un numero valido");
            return;
        }

        // Gestione file immagine
        String immaginePath = null;
        Part filePart = request.getPart("immagine"); // nome del campo file

        if (filePart != null && filePart.getSize() > 0) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // nome file originale
            // Salvo il file nella cartella "uploads" sotto webapp
            String uploadPath = getServletContext().getRealPath("") + "uploads";

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdir();

            File file = new File(uploadDir, fileName);
            try (InputStream fileContent = filePart.getInputStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileContent.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // memorizza percorso relativo
            immaginePath = "uploads/" + fileName;
        }

        try {
            UtenteDAO utenteDAO = new UtenteDAO(connection);
            if (!utenteDAO.checkRegistration(utente.getUsername())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Utente non registrato");
                return;
            }

            if (prezzo <= 0) {
                path += "?prezzoMsg=Il prezzo deve essere maggiore di zero";
                response.sendRedirect(path);
                return;
            }

            // Aggiunta dell'articolo in base alla presenza dell'immagine
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            if (immaginePath == null) {
                articoloDAO.addArticolo(nome, descrizione, utente.getUsername(), prezzo);
            } else {
                articoloDAO.addArticolo(nome, descrizione, utente.getUsername(), prezzo, immaginePath);
            }

            response.sendRedirect(path);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante l'aggiunta dell'articolo: " + e.getMessage());
        }
    }
}
