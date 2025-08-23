package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.beans.Offerta;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.dao.OffertaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/ChiudiAsta")
public class ChiudiAstaServlet extends HttpServlet {
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
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utente non autenticato");
            return;
        }
        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false, \"error\":\"Sessione scaduta\"}");
            return;
        }
        String username = utente.getUsername();

        int idAsta;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametro idAsta mancante o invalido");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {
            AstaDAO astaDAO = new AstaDAO(connection);
            OffertaDAO offertaDAO = new OffertaDAO(connection);

            Asta asta = astaDAO.getAstaById(idAsta);
            if (asta == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"success\":false,\"error\":\"Asta non trovata\"}");
                return;
            }

            // (Consigliato) consentire la chiusura solo al proprietario
            if (asta.getProprietario() != null && !asta.getProprietario().equals(utente.getUsername())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"success\":false,\"error\":\"Non sei il proprietario dell'asta\"}");
                return;
            }

            // Verifica che sia scaduta e non già chiusa
            boolean scaduta = java.time.LocalDateTime.now().isAfter(asta.getScadenza());
            if (!scaduta) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"success\":false,\"error\":\"Asta non ancora scaduta\"}");
                return;
            }
            if (asta.isChiusa()) {
                out.write("{\"success\":true,\"alreadyClosed\":true}");
                return;
            }

            // Trova offerta massima (se esiste)
            Offerta max = offertaDAO.getMaxOffertaByIdAsta(idAsta);

            boolean ok;
            String aggiudicatario = null;
            if (max != null) {
                aggiudicatario = max.getOfferente();
                ok = astaDAO.chiudiAsta(idAsta, aggiudicatario); // chiusura con aggiudicatario
            } else {
                ok = astaDAO.chiudiAsta(idAsta); // chiusura senza aggiudicatario
            }

            if (!ok) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"success\":false,\"error\":\"Chiusura non riuscita\"}");
                return;
            }

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

            // OK
            out.write("{\"success\":true,"
                    + "\"idAsta\":" + idAsta + ","
                    + "\"chiusa\":true,"
                    + "\"aggiudicatario\":" + (aggiudicatario == null ? "null" : "\"" + escape(aggiudicatario) + "\"")
                    + "}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"error\":\"Errore DB\"}");
        }
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String escape(String s){
        return s == null ? "" : s.replace("\"","\\\"");
    }
}