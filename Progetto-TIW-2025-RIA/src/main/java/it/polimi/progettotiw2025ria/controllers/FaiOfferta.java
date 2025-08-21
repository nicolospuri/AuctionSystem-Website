package it.polimi.progettotiw2025ria.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.progettotiw2025ria.beans.Asta;
import it.polimi.progettotiw2025ria.beans.Offerta;
import it.polimi.progettotiw2025ria.beans.Utente;
import it.polimi.progettotiw2025ria.dao.AstaDAO;
import it.polimi.progettotiw2025ria.dao.OffertaDAO;
import it.polimi.progettotiw2025ria.utils.ConnectionHandler;
import it.polimi.progettotiw2025ria.utils.LocalDateTimeAdapter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MultipartConfig
@WebServlet("/FaiOfferta")
public class FaiOfferta extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;

    public FaiOfferta() {
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
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        if (session.getAttribute("idAsta") == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Parametro idAsta mancante in sessione\"}");
            return;
        }
        Integer idAsta = 0;
        idAsta = (Integer) (session.getAttribute("idAsta"));

        String prezzoOffertoParam = request.getParameter("prezzoOfferto");
        if (prezzoOffertoParam == null || prezzoOffertoParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Parametro prezzo mancante\"}");
            return;
        }

        double prezzoOfferto = 0.0;
        try {
            prezzoOfferto = Double.parseDouble(prezzoOffertoParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Parametro prezzo non valido\"}");
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        Map<String, Object> result = new HashMap<>();
        String jsonResponse;

        try {
            AstaDAO astaDAO = new AstaDAO(connection);
            Asta asta = astaDAO.getAstaById(idAsta);
            OffertaDAO offertaDAO = new OffertaDAO(connection);
            List<Offerta> offerte = offertaDAO.getOfferteByIdAsta(idAsta);

            // Asta scaduta
            if (asta.getScadenza().isBefore(LocalDateTime.now())) {
                result.put("offertaErrorMsg", "Asta scaduta, non è possibile fare offerte");
                jsonResponse = gson.toJson(result);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(jsonResponse);
                return;
            }
            // Asta propria
            if (asta.getProprietario().equals(utente.getUsername())) {
                result.put("offertaErrorMsg", "Non puoi fare offerte su una tua asta");
                jsonResponse = gson.toJson(result);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(jsonResponse);
                return;
            }

            // Offerta troppo bassa
            if (prezzoOfferto < asta.getPrezzoIniziale()) {
                result.put("offertaErrorMsg", "L'offerta deve essere almeno pari al prezzo iniziale");
                jsonResponse = gson.toJson(result);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(jsonResponse);
                return;
            }

            if (offerte != null && !offerte.isEmpty()) {
                Offerta maxOfferta = offertaDAO.getMaxOffertaByIdAsta(idAsta);
                // Offerta troppo bassa
                if (maxOfferta != null && prezzoOfferto < maxOfferta.getPrezzo() + asta.getRialzoMinimo()) {
                    result.put("offertaErrorMsg", "L'offerta deve rialzare il prezzo almeno quanto il rialzo minimo");
                    jsonResponse = gson.toJson(result);

                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(jsonResponse);
                    return;
                }
            }

            if (!offertaDAO.addOfferta(utente.getUsername(), prezzoOfferto, idAsta)) {
                result.put("offertaErrorMsg", "Errore durante l'inserimento dell'offerta");
                jsonResponse = gson.toJson(result);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(jsonResponse);
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

            result.put("offertaSuccessMsg", "Offerta effettuata con successo");
            result.put("offerta", offertaDAO.getMaxOffertaByIdAsta(idAsta));

            jsonResponse = gson.toJson(result);

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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
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
