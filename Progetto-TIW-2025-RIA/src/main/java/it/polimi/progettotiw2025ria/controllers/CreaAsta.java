package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

@WebServlet("/CreaAsta")
public class CreaAsta extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Connection connection;

    public CreaAsta() { super(); }

    @Override
    public void init() throws UnavailableException {
        try {
            connection = ConnectionHandler.getConnection();
        } catch (UnavailableException e) {
            throw new UnavailableException("Database connection unavailable");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // --- Controllo login ---
        if (request.getSession(false) == null ||
                request.getSession(false).getAttribute("utente") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Sessione scaduta o non valida").toString());
            return;
        }
        Utente utente = (Utente) request.getSession(false).getAttribute("utente");
        String username = utente.getUsername();

        // --- Parametri ---
        String[] articoliSelezionati = request.getParameterValues("articoliSelezionati");
        String rialzoMinimoParam = request.getParameter("rialzoMinimo");
        String scadenzaParam = request.getParameter("scadenza");

        // Articoli
        if (articoliSelezionati == null || articoliSelezionati.length == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Nessun articolo selezionato").toString());
            return;
        }
        ArrayList<Integer> articoliIds = new ArrayList<>();
        try {
            for (String idStr : articoliSelezionati) {
                articoliIds.add(Integer.parseInt(idStr));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Codici articoli non validi").toString());
            return;
        }

        // Rialzo minimo
        int rialzoMinimo;
        if (rialzoMinimoParam == null || rialzoMinimoParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Il rialzo minimo è obbligatorio").toString());
            return;
        }
        try {
            rialzoMinimo = Integer.parseInt(rialzoMinimoParam.trim());
            if (rialzoMinimo <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject()
                        .put("success", false)
                        .put("error", "Il rialzo minimo deve essere > 0").toString());
                return;
            }
        } catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Rialzo minimo non valido").toString());
            return;
        }

        // Scadenza (ISO_LOCAL_DATE_TIME es. 2025-08-26T16:03)
        LocalDateTime scadenza;
        if (scadenzaParam == null || scadenzaParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "La scadenza è obbligatoria").toString());
            return;
        }
        try {
            scadenza = LocalDateTime.parse(scadenzaParam);
            if (scadenza.isBefore(LocalDateTime.now())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject()
                        .put("success", false)
                        .put("error", "La scadenza deve essere nel futuro").toString());
                return;
            }
        } catch (DateTimeParseException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Formato scadenza non valido").toString());
            return;
        }

        ArticoloDAO articoloDAO = new ArticoloDAO(connection);
        AstaDAO astaDAO = new AstaDAO(connection);

        try {
            // Validazioni dominio
            if (!articoloDAO.areAllArticlesOfUser(username, articoliIds)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject()
                        .put("success", false)
                        .put("error", "Puoi selezionare solo i tuoi articoli").toString());
                return;
            }
            if (!articoloDAO.areAllArticlesFree(articoliIds)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(new JSONObject()
                        .put("success", false)
                        .put("error", "Alcuni articoli sono già in un'asta").toString());
                return;
            }

            // Prezzo iniziale = somma prezzi articoli
            double prezzoIniziale = articoloDAO.getSumOfPrice(articoliIds);

            connection.setAutoCommit(false);
            try {
                // Creazione asta
                int idAsta = astaDAO.insertNewAsta(
                        username,
                        prezzoIniziale,
                        (float) rialzoMinimo,          // firma del DAO accetta float
                        Timestamp.valueOf(scadenza)
                );

                // Aggiornamento articoli con id_asta
                articoloDAO.updateIdAstaInArticles(articoliIds, idAsta);

                connection.commit();

                boolean lastActionFound = false;
                Cookie[] cookies = request.getCookies();

                // Cerco i cookie "lastAction"
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if (c.getName().equals("lastActionCreaAsta" + username)) {
                            c.setValue("true");
                            c.setMaxAge(60*60*24*30);
                            lastActionFound = true;
                            response.addCookie(c);
                            break;
                        }
                    }
                }

                // Se i cookie non esistono, li creo
                if(!lastActionFound) {
                    Cookie lastAction = new Cookie("lastActionCreaAsta" + username, "true");
                    lastAction.setMaxAge(60*60*24*30);
                    response.addCookie(lastAction);
                }

                // Risposta JSON
                JSONObject json = new JSONObject()
                        .put("success", true)
                        .put("idAsta", idAsta)
                        .put("proprietario", username)
                        .put("prezzoIniziale", prezzoIniziale)
                        .put("rialzoMinimo", rialzoMinimo)
                        .put("scadenza", scadenza.toString())
                        .put("articoli", new JSONArray(articoliIds));

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(json.toString());
            } catch (SQLException e) {
                connection.rollback();
                throw new ServletException(e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException | ServletException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Errore DB").toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // opzionale: se vuoi supportare GET, delega al POST oppure rispondi 405
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json");
        response.getWriter().write(new JSONObject()
                .put("success", false)
                .put("error", "Metodo non consentito").toString());
    }

    @Override
    public void destroy() {
        try { if (connection != null) connection.close(); }
        catch (SQLException ignored) {}
    }
}