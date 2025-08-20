package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/AggiungiArticolo")
@MultipartConfig
public class AggiungiArticolo extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    @Override
    public void init() throws UnavailableException {
        try {
            connection = ConnectionHandler.getConnection();
        } catch (UnavailableException e) {
            throw new UnavailableException("Database connection unavailable");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String nome = request.getParameter("nome");
            String descrizione = request.getParameter("descrizione");
            String prezzoParam = request.getParameter("prezzo");

            if (nome == null || nome.trim().isEmpty()
                    || descrizione == null || descrizione.trim().isEmpty()
                    || prezzoParam == null || prezzoParam.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false, \"error\":\"Tutti i campi sono obbligatori\"}");
                return;
            }

            double prezzo;
            try {
                prezzo = Double.parseDouble(prezzoParam.trim());
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false, \"error\":\"Prezzo non valido\"}");
                return;
            }

            if (prezzo <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false, \"error\":\"Il prezzo deve essere maggiore di zero\"}");
                return;
            }

            HttpSession session = request.getSession(false);
            Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;
            if (utente == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false, \"error\":\"Sessione scaduta\"}");
                return;
            }

            // Gestione immagine
            Part filePart = request.getPart("immagine");
            String fileName = "default.png"; // default

            if (filePart != null && filePart.getSize() > 0) {
                // Qui salvi il file sul filesystem o in DB
                // Ad esempio in una cartella "uploads"
                String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) uploadDir.mkdir();

                fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                filePart.write(uploadPath + File.separator + fileName);
            }

            // Inserimento nel DB (salvando il nome file immagine o path relativo)
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            articoloDAO.addArticolo(nome, descrizione, utente.getUsername(), prezzo, fileName);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false, \"error\":\"Errore interno: " + e.getMessage() + "\"}");
        }
    }

    @Override
    public void destroy() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
