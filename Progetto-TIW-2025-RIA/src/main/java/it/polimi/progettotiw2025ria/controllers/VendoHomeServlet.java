package it.polimi.progettotiw2025ria.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.polimi.progettotiw2025ria.beans.Articolo;
import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.dao.ArticoloDAO;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
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

@WebServlet("/VendoHome")
public class VendoHomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private Gson gson = new Gson();

    @Override
    public void init() throws UnavailableException {
        try {
            connection = ConnectionHandler.getConnection();
        } catch (UnavailableException e) {
            throw new UnavailableException("Database connection unavailable");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("username") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\":\"Utente non loggato\"}");
            return;
        }

        String username = (String) session.getAttribute("username");

        // Recupero dai cookie
        String renderAllAste = getCookieValue(req, "renderAllTablesAste" + username);
        String renderClosedAste = getCookieValue(req, "renderTableAsteChiuse" + username);
        String renderArticoli = getCookieValue(req, "renderArticoli" + username);

        JsonObject finalObject = new JsonObject();

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            ArticoloDAO articoloDAO = new ArticoloDAO(connection);

            // 🔹 Aste aperte + chiuse
            if (renderAllAste == null || renderAllAste.equals("true")) {
                List<Asta> openAste = astaDAO.getAsteAperteByUsername(username);
                for (Asta asta : openAste) {
                    asta.setTempoMancante(); // usa metodo del bean
                }
                finalObject.add("openAste", gson.toJsonTree(openAste).getAsJsonArray());

                List<Asta> closedAste = astaDAO.getAsteChiuseByUsername(username);
                finalObject.add("closedAste", gson.toJsonTree(closedAste).getAsJsonArray());

                setCookie(resp, "renderAllTablesAste" + username, "false", 30);
            }

            // 🔹 Solo aste chiuse
            if (renderClosedAste == null || renderClosedAste.equals("true")) {
                List<Asta> closedAste = astaDAO.getAsteChiuseByUsername(username);
                finalObject.add("closedAste", gson.toJsonTree(closedAste).getAsJsonArray());
                setCookie(resp, "renderTableAsteChiuse" + username, "false", 30);
            }

            // 🔹 Articoli del proprietario
            if (renderArticoli == null || renderArticoli.equals("true")) {
                List<Articolo> articoli = articoloDAO.findArticlesByUser(username);
                finalObject.add("articoli", gson.toJsonTree(articoli).getAsJsonArray());
                setCookie(resp, "renderArticoli" + username, "false", 30);
            }

            // 🔹 Output JSON finale
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            PrintWriter out = resp.getWriter();
            out.print(gson.toJson(finalObject));

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Errore caricamento dati\"}");
        }
    }

    // --- Utilità gestione cookie ---
    private void setCookie(HttpServletResponse response, String name, String value, int days) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, value);
        cookie.setMaxAge(days * 60 * 60 * 24);
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
