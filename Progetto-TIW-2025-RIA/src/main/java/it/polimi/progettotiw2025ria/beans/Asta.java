package it.polimi.progettotiw2025ria.beans;

import it.polimi.progettotiw2025ria.beans.Articolo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Asta {
    private final int id;
    private double prezzoIniziale;
    private final int rialzoMinimo;
    private final LocalDateTime scadenza;
    private final String proprietario;
    private boolean chiusa;
    private Offerta offertaMassima;
    private String aggiudicatario;
    private List<Articolo> articoli;
    private String tempoMancante;
    private double prezzoOffertaMassima;
    private String indirizzoAggiudicatario;

    // Asta aperta
    public Asta(int id, double prezzoIniziale, int rialzoMinimo, LocalDateTime scadenza, String proprietario) {
        this.id = id;
        this.prezzoIniziale = prezzoIniziale;
        this.rialzoMinimo = rialzoMinimo;
        this.scadenza = scadenza;
        this.proprietario = proprietario;
        this.chiusa = false;
    }

    // Asta chiusa
    public Asta(int id, double prezzoIniziale, int rialzoMinimo, LocalDateTime scadenza, String proprietario, boolean chiusa, String aggiudicatario) {
        this.id = id;
        this.prezzoIniziale = prezzoIniziale;
        this.rialzoMinimo = rialzoMinimo;
        this.scadenza = scadenza;
        this.proprietario = proprietario;
        this.chiusa = chiusa;
        this.aggiudicatario = aggiudicatario;
    }

    //----------------- Getter e Setter -----------------

    public int getId() {
        return id;
    }

    public double getPrezzoIniziale() {
        return prezzoIniziale;
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

    public String getAggiudicatario() {
        return aggiudicatario;
    }

    public List<Articolo> getArticoli() {
        return articoli;
    }

    public String getTempoMancante() {
        return tempoMancante;
    }

    public double getPrezzoOffertaMassima() {
        return prezzoOffertaMassima;
    }

    public String getIndirizzoAggiudicatario() {
        return indirizzoAggiudicatario;
    }

    public void setPrezzoIniziale(double prezzoIniziale) {
        this.prezzoIniziale = prezzoIniziale;
    }

    public void setOffertaMassima(Offerta offertaMassima) {
        this.offertaMassima = offertaMassima;
    }

    public void setAggiudicatario(String aggiudicatario) {
        this.aggiudicatario = aggiudicatario;
    }

    public void setArticoli(List<Articolo> articoli) {
        this.articoli = articoli;
    }

    public void setPrezzoOffertaMassima(double prezzo) {
        this.prezzoOffertaMassima = prezzo;
    }

    public void setIndirizzoAggiudicatario(String indirizzoAggiudicatario) {
        this.indirizzoAggiudicatario = indirizzoAggiudicatario;
    }

    //----------------- Stato Asta -----------------

    public boolean isChiusa() {
        return chiusa;
    }

    public void chiudiAsta() {
        this.chiusa = true;
    }

    public void setTempoMancante() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(scadenza)) {
            tempoMancante = "scaduta";
            return;
        }
        Duration dur = Duration.between(now, scadenza);
        long days = dur.toDays();
        long hours = dur.minusDays(days).toHours();
        tempoMancante = days + " giorni e " + hours + " ore";
    }

    public boolean canBeClosed() {
        return LocalDateTime.now().isAfter(scadenza) && !isChiusa();
    }
}
