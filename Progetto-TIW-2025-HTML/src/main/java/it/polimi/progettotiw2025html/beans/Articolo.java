package it.polimi.progettotiw2025html.beans;

public class Articolo {
    private final int codice;
    private final String nome;
    private final String descrizione;
    private String immagine;
    private final double prezzo;
    private int idAsta;
    private String proprietario;


    public Articolo(int codice, String nome, String descrizione, String immagine, double prezzo, String proprietario) {
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.immagine = immagine;
        this.prezzo = prezzo;
        this.proprietario = null; // Proprietario non specificato
    }

    //----------------- Getter e Setter -----------------

    public int getCodice() {
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
