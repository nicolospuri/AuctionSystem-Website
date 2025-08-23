package it.polimi.progettotiw2025ria.controllers;

import com.google.gson.*;
import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.beans.Offerta;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.dao.OffertaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import it.polimi.progettotiw2025ria.utils.LocalDateTimeAdapter;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MultipartConfig
@WebServlet("/OfferteServlet")
public class OfferteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public OfferteServlet() {
        super();
    }

    @Override
    public void init() throws UnavailableException {
        try {
            connection = ConnectionHandler.getConnection();
        } catch (UnavailableException e) {
            throw new UnavailableException("Database connection unavailable");
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession() == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        HttpSession session = request.getSession();
        if (session.getAttribute("utente") == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        Utente utente = (Utente) session.getAttribute("utente");
        if (utente == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }
        String username = utente.getUsername();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        Map<String, Object> result = new HashMap<>();

        Integer idAsta = 0;
        try {
            idAsta = Integer.parseInt(request.getParameter("idAsta"));
            result.put("idAsta", idAsta);
            session.setAttribute("idAsta", idAsta);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"idAsta non valido\"}");
            return;
        }

        JsonArray asteVisitateJsonArray = null;

        Cookie[] cookies = request.getCookies();
        Cookie asteVisitateCookie = null;

        // Cerco il cookie "asteVisitate" + username
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals("asteVisitate" + username)) {
                    String decodedAsteVisitateCookie = URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
                    asteVisitateJsonArray = JsonParser.parseString(decodedAsteVisitateCookie).getAsJsonArray();
                    asteVisitateCookie = c;
                    break;
                }
            }
        }

        boolean astaAlreadyVisited = false;

        if(asteVisitateJsonArray != null) {
            // Controllo se l'asta è già stata visitata
            for(JsonElement asta : asteVisitateJsonArray) {
                if(asta.getAsInt() == idAsta)
                    astaAlreadyVisited = true;
            }
        } else { // Se il cookie non è presente, creo un JsonArray vuoto
            asteVisitateJsonArray = new JsonArray();
        }

        // Se non è già stata visitata, la aggiungo alla lista delle aste visitate
        if (!astaAlreadyVisited) {
            asteVisitateJsonArray.add(idAsta);

            String encodedAsteVisionate = URLEncoder.encode(gson.toJson(asteVisitateJsonArray), StandardCharsets.UTF_8);
            if (asteVisitateCookie != null) { // Se il cookie esiste, lo aggiorno
                asteVisitateCookie.setValue(encodedAsteVisionate);
            } else { // Altrimenti ne creo uno nuovo
                asteVisitateCookie = new Cookie("asteVisitate"+username, encodedAsteVisionate);
                asteVisitateCookie.setPath(request.getContextPath());
            }
            response.addCookie(asteVisitateCookie);
        }
        asteVisitateCookie.setMaxAge(60*60*24*30); // In qualunque caso (re)imposto la durata del cookie a un mese

        try {
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            List<Articolo> articoli = articoloDAO.getArticoliByIdAsta(idAsta);
            // Aggiungo gli articoli al result
            result.put("articoliAsta", articoli);

            AstaDAO astaDAO = new AstaDAO(connection);
            Asta asta = astaDAO.getAstaById(idAsta);
            int rialzoMinimo = asta.getRialzoMinimo();
            double prezzoIniziale = asta.getPrezzoIniziale();
            // Aggiungo il rialzo minimo e il prezzo iniziale al result
            result.put("rialzoMinimo", rialzoMinimo);
            result.put("prezzoIniziale", prezzoIniziale);

            OffertaDAO offertaDAO = new OffertaDAO(connection);
            List<Offerta> offerte = offertaDAO.getOfferteByIdAsta(idAsta);
            if (offerte != null && !offerte.isEmpty()) {
                // Aggiungo le offerte al result
                result.put("offerteAsta", offerte);
                Offerta maxOfferta = offertaDAO.getMaxOffertaByIdAsta(idAsta);
                result.put("prezzoOffertaMassima", maxOfferta.getPrezzo());
            }

            if (utente != null && !utente.getUsername().equals(asta.getProprietario())
                    && !asta.isChiusa() && asta.getScadenza().isAfter(LocalDateTime.now())) {
                // Aggiungo il booleano canOffer al result
                result.put("canOffer", true);
            }

            String jsonResponse = gson.toJson(result);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonResponse);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\":\""+e+"\"}");
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
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
