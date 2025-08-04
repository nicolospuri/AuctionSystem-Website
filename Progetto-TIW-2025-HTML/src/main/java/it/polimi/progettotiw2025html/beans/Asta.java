package it.polimi.progettotiw2025html.beans;

import java.util.Date;
import java.util.List;

public class Asta {
    private final int idAsta;
    private final List<Articolo> articoli;
    private double prezzo;
    private final int rialzoMinimo;
    private final Date scadenza;
    private final String proprietario;
    private boolean chiusa;
    private String vincitore;

    public Asta(int idAsta, List<Articolo> articoli, double prezzo, int rialzoMinimo, Date scadenza, String proprietario) {
        this.idAsta = idAsta;
        this.articoli = articoli;
        this.prezzo = prezzo;
        this.rialzoMinimo = rialzoMinimo;
        this.scadenza = scadenza;
        this.proprietario = proprietario;
        this.chiusa = false;
    }

    //----------------- Getter e Setter -----------------

    public int getIdAsta() {
        return idAsta;
    }

    public List<Articolo> getArticoli() {
        return articoli;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public int getRialzoMinimo() {
        return rialzoMinimo;
    }

    public Date getScadenza() {
        return scadenza;
    }

    public String getProprietario() {
        return proprietario;
    }

    public String getVincitore() {
        return vincitore;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public void setVincitore(String vincitore) {
        this.vincitore = vincitore;
    }

    //----------------- Stato Asta -----------------

    public boolean isChiusa() {
        return chiusa;
    }

    public void chiudiAsta() {
        this.chiusa = true;
    }
}
