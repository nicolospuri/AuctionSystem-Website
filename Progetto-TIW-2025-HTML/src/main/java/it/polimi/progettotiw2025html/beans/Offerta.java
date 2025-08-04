package it.polimi.progettotiw2025html.beans;

import java.util.Date;

public class Offerta {
    private final String usernameUtente;
    private final double prezzo;
    private final Date dataOfferta;
    private final int idAsta;

    public Offerta(String usernameUtente, double prezzo, Date dataOfferta, int idAsta) {
        this.usernameUtente = usernameUtente;
        this.prezzo = prezzo;
        this.dataOfferta = dataOfferta;
        this.idAsta = idAsta;
    }

    //----------------- Getter e Setter -----------------

    public String getUsernameUtente() {
        return usernameUtente;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public Date getDataOfferta() {
        return dataOfferta;
    }

    public int getIdAsta() {
        return idAsta;
    }
}