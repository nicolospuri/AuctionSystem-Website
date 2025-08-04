package it.polimi.progettotiw2025html.beans;

import java.util.Date;

public class Offerta {
    private final String offerente;
    private final double prezzo;
    private final Date data;
    private final int idAsta;

    public Offerta(String offerente, double prezzo, Date data, int idAsta) {
        this.offerente = offerente;
        this.prezzo = prezzo;
        this.data = data;
        this.idAsta = idAsta;
    }

    //----------------- Getter e Setter -----------------

    public String getOfferente() {
        return offerente;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public Date getData() {
        return data;
    }

    public int getIdAsta() {
        return idAsta;
    }
}