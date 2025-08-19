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
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/AcquistoServlet")
public class AcquistoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public AcquistoServlet() {
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
        if(request.getSession() == null) {
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
        String asteVisitateJsonCookie = null;
        Cookie[] cookies = request.getCookies();

        for(Cookie cookie : cookies) {
            if(cookie.getName().equals("asteVisitate"+username)) {
                asteVisitateJsonCookie = cookie.getValue();
            }
        }

        try {
            JsonObject jsonObject = new JsonObject();
            Gson gson = new Gson();

            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            List<Articolo> articoli = null;
            List<Asta> asteVisitate = new ArrayList<>();
            Asta astaVisitata = null;

            // Se è presente il cookie, invio le aste visionate
            if(asteVisitateJsonCookie != null) {
                // Estraggo gli id delle aste visionate dal json
                String decodedAsteVisitateJsonCookie = URLDecoder.decode(asteVisitateJsonCookie, StandardCharsets.UTF_8);
                System.out.println(decodedAsteVisitateJsonCookie);
                JsonArray idAsteVisitateJson = JsonParser.parseString(decodedAsteVisitateJsonCookie).getAsJsonArray();
                int idAstaVisitata = 0;

                for (JsonElement id : idAsteVisitateJson) {
                    try {
                        idAstaVisitata = id.getAsInt();
                    }
                    catch(NumberFormatException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().print("{\"error\":\"Formato o parametri non accettati\"}");
                        return;
                    }
                    astaVisitata = astaDAO.getAstaById(idAstaVisitata);
                    if (astaVisitata != null) {
                        articoli = articoloDAO.getArticoliByIdAsta(idAstaVisitata);
                        astaVisitata.setArticoli(articoli);
                    }
                    asteVisitate.add(astaVisitata);
                }

                if (!asteVisitate.isEmpty()) {
                    rimuoviAsteChiuse(request, response, asteVisitate, username);

                    JsonArray asteVisitateJsonArray = gson.toJsonTree(asteVisitate).getAsJsonArray();
                    jsonObject.add("asteVisitate", asteVisitateJsonArray);
                }
            }

            OffertaDAO offertaDAO = new OffertaDAO(connection);
            Offerta offertaMax = null;
            List<Asta> asteVinte = null;

            asteVinte = astaDAO.getAsteVinteByUsername(utente.getUsername());
            if (asteVinte != null && !asteVinte.isEmpty()) {
                for (Asta a : asteVinte) {
                    offertaMax = offertaDAO.getMaxOffertaByIdAsta(a.getId());
                    if (offertaMax != null) {
                        a.setOffertaMassima(offertaMax);
                        a.setPrezzoOffertaMassima(offertaMax.getPrezzo());
                    }
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);
                }

                // todo: problema con LocalDateTime, non riesco a serializzarlo correttamente
                JsonArray asteVinteJsonArray = gson.toJsonTree(asteVinte).getAsJsonArray();
                jsonObject.add("asteVinte", asteVinteJsonArray);
            }

            String jsonResponse = gson.toJson(jsonObject);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(jsonResponse);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\":\""+e+"\"}");
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(request.getSession() == null) {
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

        String keyword = request.getParameter("keyword");
        if (keyword == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Parametri mancanti\"}");
            return;
        }

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);
            List<Asta> asteTrovate = null;
            List<Articolo> articoli = null;

            if (!keyword.isEmpty()) {
                asteTrovate = astaDAO.getAsteAperteByKeyword(keyword);
            }

            if (asteTrovate != null && !asteTrovate.isEmpty()) {
                for (Asta a : asteTrovate) {
                    articoli = articoloDAO.getArticoliByIdAsta(a.getId());
                    a.setArticoli(articoli);
                }
            }

            Gson gson = new Gson();
            String jsonResponse = gson.toJson(asteTrovate);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(jsonResponse);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\":\""+e+"\"}");
            e.printStackTrace(System.out);
        }
    }

    private void rimuoviAsteChiuse(HttpServletRequest request, HttpServletResponse response, List<Asta> aste, String username) {
        // Rimuovo l'asta se è chiusa
        aste.removeIf(Asta::isChiusa);

        // Aggiorno il cookie con le aste visionate e aperte
        JsonArray newAsteVisitateJsonArray = new JsonArray();
        for(Asta a: aste) {
            newAsteVisitateJsonArray.add(a.getId());
        }

        Gson gson = new Gson();

        String encodedAsteVisionate = URLEncoder.encode(gson.toJson(newAsteVisitateJsonArray), StandardCharsets.UTF_8);

        String newAsteVisitateCookieJson = gson.toJson(encodedAsteVisionate);
        Cookie updatedCookie = new Cookie("asteVisitate"+username, newAsteVisitateCookieJson);

        response.addCookie(updatedCookie);
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
