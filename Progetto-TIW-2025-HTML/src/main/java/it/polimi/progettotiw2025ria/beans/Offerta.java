package it.polimi.progettotiw2025ria.beans;

import java.time.LocalDateTime;

public class Offerta {
    private final int id;
    private final String offerente;
    private final double prezzo;
    private final LocalDateTime data;
    private final int idAsta;

    public Offerta(int id, String offerente, double prezzo, LocalDateTime data, int idAsta) {
        this.id = id;
        this.offerente = offerente;
        this.prezzo = prezzo;
        this.data = data;
        this.idAsta = idAsta;
    }

    //----------------- Getter e Setter -----------------

    public int getId() {
        return id;
    }

    public String getOfferente() {
        return offerente;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public LocalDateTime getData() {
        return data;
    }

    public int getIdAsta() {
        return idAsta;
    }
}