package it.polimi.progettotiw2025html.beans;

public class Articolo {
    private final String codice;
    private final String nome;
    private final String descrizione;
    private String immagine;
    private final double prezzo;
    private int idAsta;
    private final String proprietario;

    public Articolo(String codice, String nome, String descrizione, String immagine, double prezzo, String proprietario) {
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.immagine = immagine;
        this.prezzo = prezzo;
        this.proprietario = proprietario;
    }

    public Articolo(String codice, String nome, String descrizione, double prezzo, String proprietario) {
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.prezzo = prezzo;
        this.proprietario = proprietario;
    }

    //----------------- Getter e Setter -----------------

    public String getCodice() {
        return codice;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getImmagine() {
        return immagine;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public int getIdAsta() {
        return idAsta;
    }

    public String getProprietario() {
        return proprietario;
    }

    public void setIdAsta(int idAsta) {
        this.idAsta = idAsta;
    }
}
