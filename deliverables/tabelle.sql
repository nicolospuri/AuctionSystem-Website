-- ==========================================================
-- TABELLE
-- ==========================================================


CREATE TABLE Utente (
                        Username VARCHAR(32) PRIMARY KEY,
                        Password VARCHAR(32) NOT NULL,
                        Nome VARCHAR(32) NOT NULL,
                        Cognome VARCHAR(32) NOT NULL,
                        Indirizzo VARCHAR(255) NOT NULL
);

CREATE TABLE Articolo (
                          Codice INT AUTO_INCREMENT PRIMARY KEY,
                          Nome VARCHAR(32) NOT NULL,
                          Descrizione VARCHAR(255) NOT NULL,
                          Immagine VARCHAR(255) NOT NULL,
                          Prezzo DECIMAL(10, 2) NOT NULL CHECK ( Prezzo > 0 ),
                          IdAsta INT REFERENCES Asta(Id) ON UPDATE CASCADE ON DELETE NO ACTION,
                          Proprietario VARCHAR(32) NOT NULL REFERENCES Utente(Username) ON UPDATE CASCADE ON DELETE NO ACTION
);

CREATE TABLE Asta (
                      Id INT AUTO_INCREMENT PRIMARY KEY,
                      Prezzo DECIMAL(10, 2) NOT NULL CHECK ( Prezzo > 0 ),
                      RialzoMinimo INT NOT NULL CHECK ( RialzoMinimo > 0 ),
                      Scadenza TIMESTAMP NOT NULL,
                      Proprietario VARCHAR(32) NOT NULL REFERENCES Utente(Username) ON UPDATE CASCADE ON DELETE NO ACTION,
                      Chiusa BOOLEAN NOT NULL DEFAULT FALSE,
                      Aggiudicatario VARCHAR(32) REFERENCES Utente(Username) ON UPDATE CASCADE ON DELETE NO ACTION
);

CREATE TABLE Offerta (
                         Id INT AUTO_INCREMENT PRIMARY KEY,
                         Prezzo DECIMAL(10, 2) NOT NULL CHECK (Prezzo > 0),
                         Data TIMESTAMP NOT NULL,
                         IdAsta INT NOT NULL REFERENCES Asta(Id) ON UPDATE CASCADE ON DELETE NO ACTION,
                         Offerente VARCHAR(32) NOT NULL REFERENCES Utente(Username) ON UPDATE CASCADE ON DELETE NO ACTION
);