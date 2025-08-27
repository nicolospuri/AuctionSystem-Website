package it.polimi.progettotiw2025html.beans;

public class Utente {
    private final String username;
    private final String password;
    private final String nome;
    private final String cognome;
    private final String indirizzo;

    public Utente(String username, String password, String nome, String cognome, String indirizzo) {
        this.username = username;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
        this.indirizzo = indirizzo;
    }

    //----------------- Getter e Setter -----------------

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }
}
