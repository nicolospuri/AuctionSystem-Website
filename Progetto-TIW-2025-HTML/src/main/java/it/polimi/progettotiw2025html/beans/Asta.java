package it.polimi.progettotiw2025html.beans;

import java.util.List;
import java.time.LocalDateTime;
import java.time.Duration;

public class Asta {
    private final int id;
    private final List<Articolo> articoli;
    private double prezzo;
    private final int rialzoMinimo;
    private final LocalDateTime scadenza;
    private final String proprietario;
    private boolean chiusa;
    private Offerta offertaMassima;

    public Asta(int id, List<Articolo> articoli, double prezzo, int rialzoMinimo, LocalDateTime scadenza, String proprietario) {
        this.id = id;
        this.articoli = articoli;
        this.prezzo = prezzo;
        this.rialzoMinimo = rialzoMinimo;
        this.scadenza = scadenza;
        this.proprietario = proprietario;
        this.chiusa = false;
    }

    //----------------- Getter e Setter -----------------

    public int getId() {
        return id;
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

    public LocalDateTime getScadenza() {
        return scadenza;
    }

    public String getProprietario() {
        return proprietario;
    }

    public Offerta getOffertaMassima() {
        return offertaMassima;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public void setOffertaMassima(Offerta offertaMassima) {
        this.offertaMassima = offertaMassima;
    }

    //----------------- Stato Asta -----------------

    public boolean isChiusa() {
        return chiusa;
    }

    public void chiudiAsta() {
        this.chiusa = true;
    }

    public String getTempoMancante() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(scadenza)) return "scaduta";
        Duration dur = Duration.between(now, scadenza);
        long days = dur.toDays();
        long hours = dur.minusDays(days).toHours();
        return days + " giorni e " + hours + " ore";
    }
}
