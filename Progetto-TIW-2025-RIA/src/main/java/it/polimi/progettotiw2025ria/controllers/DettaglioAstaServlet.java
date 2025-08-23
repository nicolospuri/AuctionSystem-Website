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
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/DettaglioAstaServlet")
public class DettaglioAstaServlet extends HttpServlet {
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utente non autenticato");
            return;
        }

        int idAsta;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID Asta non valido");
            return;
        }

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            OffertaDAO offertaDAO = new OffertaDAO(connection);
            UtenteDAO utenteDAO = new UtenteDAO(connection);

            Asta asta = astaDAO.getAstaById(idAsta);
            if (asta == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Asta non trovata");
                return;
            }

            List<Articolo> articoli = articoloDAO.getArticoliByIdAsta(idAsta);
            asta.setArticoli(articoli);

            Offerta offertaMax = offertaDAO.getMaxOffertaByIdAsta(idAsta);
            asta.setOffertaMassima(offertaMax);
            if (offertaMax != null) {
                asta.setPrezzoOffertaMassima(offertaMax.getPrezzo());
            }

            asta.setTempoMancante();

            List<Offerta> offerte = offertaDAO.getOfferteByIdAsta(idAsta);

            // Recupero aggiudicatario e indirizzo
            String aggiudicatario = asta.getAggiudicatario();
            String indirizzoAgg = null;
            if (aggiudicatario != null) {
                Utente utenteAgg = utenteDAO.getUtenteByUsername(aggiudicatario);
                if (utenteAgg != null) {
                    indirizzoAgg = utenteAgg.getIndirizzo();
                }
            }

            // Risposta JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            PrintWriter out = response.getWriter();
            StringBuilder json = new StringBuilder("{");

            json.append("\"id\":").append(asta.getId()).append(",");
            json.append("\"prezzo\":").append(asta.getPrezzoIniziale()).append(",");
            json.append("\"rialzoMinimo\":").append(asta.getRialzoMinimo()).append(",");
            json.append("\"scadenza\":\"").append(asta.getScadenza()).append("\",");
            json.append("\"chiusa\":").append(asta.isChiusa()).append(",");

            // aggiudicatario + indirizzo
            json.append("\"aggiudicatario\":\"").append(escape(aggiudicatario)).append("\",");
            json.append("\"indirizzoAggiudicatario\":\"").append(escape(indirizzoAgg)).append("\",");

            // Articoli
            json.append("\"articoli\":[");
            for (int i = 0; i < articoli.size(); i++) {
                Articolo a = articoli.get(i);
                json.append("{")
                        .append("\"codice\":").append(a.getCodice()).append(",")
                        .append("\"nome\":\"").append(escape(a.getNome())).append("\",")
                        .append("\"prezzo\":").append(a.getPrezzo())
                        .append("}");
                if (i < articoli.size() - 1) json.append(",");
            }
            json.append("],");

            // Offerte
            json.append("\"offerte\":[");
            for (int i = 0; i < offerte.size(); i++) {
                Offerta o = offerte.get(i);
                json.append("{")
                        .append("\"offerente\":\"").append(escape(o.getOfferente())).append("\",")
                        .append("\"prezzo\":").append(o.getPrezzo()).append(",")
                        .append("\"data\":\"").append(o.getData()).append("\"")
                        .append("}");
                if (i < offerte.size() - 1) json.append(",");
            }
            json.append("]");

            json.append("}");
            out.write(json.toString());

        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore interno del server");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "").replace("\r", "");
    }
}
