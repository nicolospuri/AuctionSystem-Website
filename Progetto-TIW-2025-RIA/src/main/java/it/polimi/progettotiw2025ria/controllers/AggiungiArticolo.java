package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

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

            //Validazione base
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

            //Recupero utente da sessione
            HttpSession session = request.getSession(false);
            Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;
            if (utente == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false, \"error\":\"Sessione scaduta\"}");
                return;
            }
            String username = utente.getUsername();

            //Gestione immagine
            String immaginePath = null;
            Part filePart = request.getPart("immagine"); // nome del campo file

            if (filePart != null && filePart.getSize() > 0) {
                String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // nome file originale

                // Per salvarlo sotto /webapp/uploads
                // Ottieni la root del progetto a partire dalla cartella di deploy
                String projectRoot = new File(getServletContext().getRealPath("")).getParentFile().getParent();
                // Costruisci il path corretto a src/main/webapp/uploads
                String uploadPath = projectRoot + File.separator + "src"
                        + File.separator + "main"
                        + File.separator + "webapp"
                        + File.separator + "uploads";

                // Per salvarlo sotto /target/Progetto.../uploads nel war esploso
                // String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";

                // Meglio usare direttamente una cartella esterna, ma essendo un progetto condiviso ci sarebbero problemi

                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) uploadDir.mkdir();

                filePart.write(uploadPath + File.separator + fileName);

                // memorizza percorso relativo
                immaginePath = "uploads/" + fileName;
            }

            //Inserimento tramite DAO
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            Articolo articolo = articoloDAO.addArticolo(nome, descrizione, immaginePath, prezzo, utente.getUsername());

            if (articolo != null) {
                JSONObject json = new JSONObject();
                json.put("success", true);
                json.put("codice", articolo.getCodice());
                json.put("nome", articolo.getNome());
                json.put("descrizione", articolo.getDescrizione());
                json.put("prezzo", articolo.getPrezzo());
                json.put("immagine", articolo.getImmagine());

                boolean lastActionFound = false;
                Cookie[] cookies = request.getCookies();

                // Cerco i cookie "lastAction"
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if (c.getName().equals("lastActionCreaAsta" + username)) {
                            c.setValue("false");
                            c.setMaxAge(60*60*24*30);
                            lastActionFound = true;
                            response.addCookie(c);
                            break;
                        }
                    }
                }

                // Se i cookie non esistono, li creo
                if(!lastActionFound) {
                    Cookie lastAction = new Cookie("lastActionCreaAsta" + username, "false");
                    lastAction.setMaxAge(60*60*24*30);
                    response.addCookie(lastAction);
                }

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(json.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"success\":false, \"error\":\"Inserimento fallito\"}");
            }

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