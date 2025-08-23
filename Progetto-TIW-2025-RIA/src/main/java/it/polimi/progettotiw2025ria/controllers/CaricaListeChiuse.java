package it.polimi.progettotiw2025ria.controllers;

import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.beans.Offerta;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.dao.OffertaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/CaricaListeChiuse")
public class CaricaListeChiuse extends HttpServlet {
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
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        Utente utente = (session != null) ? (Utente) session.getAttribute("utente") : null;

        if (utente == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Sessione scaduta").toString());
            return;
        }

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            OffertaDAO offertaDAO = new OffertaDAO(connection);

            List<Asta> asteChiuse = astaDAO.getAsteChiuseByUsername(utente.getUsername());

            JSONArray arrAste = new JSONArray();
            if (asteChiuse != null) {
                for (Asta a : asteChiuse) {
                    // articoli collegati
                    List<Articolo> articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);

                    // offerta massima (se esiste)
                    Offerta offertaMax = offertaDAO.getMaxOffertaByIdAsta(a.getId());
                    a.setOffertaMassima(offertaMax);
                    if (offertaMax != null) {
                        a.setPrezzoOffertaMassima(offertaMax.getPrezzo());
                    }

                    JSONObject jAsta = new JSONObject()
                            .put("id", a.getId())
                            .put("prezzoIniziale", a.getPrezzoIniziale())
                            .put("proprietario", a.getProprietario())
                            .put("aggiudicatario", a.getAggiudicatario() == null ? JSONObject.NULL : a.getAggiudicatario());

                    if (offertaMax != null) {
                        jAsta.put("prezzoOffertaMassima", offertaMax.getPrezzo());
                        jAsta.put("offertaMassima", new JSONObject()
                                .put("id", offertaMax.getId())
                                .put("offerente", offertaMax.getOfferente())
                                .put("prezzo", offertaMax.getPrezzo())
                                .put("data", offertaMax.getData().toString())
                                .put("idAsta", offertaMax.getIdAsta()));
                    } else {
                        jAsta.put("prezzoOffertaMassima", JSONObject.NULL);
                    }

                    JSONArray jArtList = new JSONArray();
                    for (Articolo art : articoli) {
                        jArtList.put(new JSONObject()
                                .put("codice", art.getCodice())
                                .put("nome", art.getNome())
                                .put("descrizione", art.getDescrizione())
                                .put("prezzo", art.getPrezzo()));
                    }
                    jAsta.put("articoli", jArtList);

                    arrAste.put(jAsta);
                }
            }

            JSONObject out = new JSONObject()
                    .put("success", true)
                    .put("asteChiuse", arrAste);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(out.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject()
                    .put("success", false)
                    .put("error", "Errore DB").toString());
        }
    }

    @Override
    public void destroy() {
        try { if (connection != null) connection.close(); }
        catch (SQLException ignored) {}
    }
}