-- ==========================================================
-- QUERY DI POPOLAMENTO
-- ==========================================================

TRUNCATE TABLE offerta;
TRUNCATE TABLE articolo;
TRUNCATE TABLE asta;
TRUNCATE TABLE utente;

-- =========================
-- UTENTI
-- =========================
INSERT INTO utente (Username, Password, Nome, Cognome, Indirizzo) VALUES
                                                                      ('alice',  'passAlice', 'Alice',     'Rossi',   'Via Roma 1, Milano'),
                                                                      ('bob',    'passBob',   'Bob',       'Bianchi', 'Via Garibaldi 10, Torino'),
                                                                      ('carol',  'passCarol', 'Carolina',  'Verdi',   'Via Dante 3, Firenze'),
                                                                      ('dave',   'passDave',  'Davide',    'Neri',    'Via Napoli 99, Roma'),
                                                                      ('eve',    'passEve',   'Eva',       'Gialli',  'Via Po 5, Bologna'),
                                                                      ('frank',  'passFrank', 'Francesco', 'Blu',     'Via Tevere 12, Roma'),
                                                                      ('guest',  'passGuest', 'G',         'Uest',    'Indirizzo sconosciuto'); -- utente senza aste/offerte

-- =========================
-- ASTE
-- =========================
-- Edge: asta aperta senza offerte
INSERT INTO asta (Id, Prezzo, RialzoMinimo, Scadenza,            Proprietario, Chiusa, Aggiudicatario) VALUES
    (1001,  120.00,  5, '2025-12-31 23:59:59', 'alice', 0, NULL);

-- Edge: asta aperta con offerte (vedi sezione OFFERTA) ma non chiusa, quindi senza aggiudicatario
INSERT INTO asta (Id, Prezzo, RialzoMinimo, Scadenza,            Proprietario, Chiusa, Aggiudicatario) VALUES
    (1002, 350.00, 10, '2025-11-30 20:00:00', 'alice', 0, NULL);

-- Edge: asta chiusa con offerte e aggiudicatario
INSERT INTO asta (Id, Prezzo, RialzoMinimo, Scadenza,            Proprietario, Chiusa, Aggiudicatario) VALUES
    (1003, 200.00, 10, '2025-06-01 18:00:00', 'carol', 1, 'bob');

-- Edge: asta chiusa senza offerte (aggiudicatario NULL)
INSERT INTO asta (Id, Prezzo, RialzoMinimo, Scadenza,            Proprietario, Chiusa, Aggiudicatario) VALUES
    (1004,  0.01,  1, '2025-05-01 12:00:00', 'dave',  1, NULL);

-- Edge: asta da chiudere (aggiudicatario NULL)
INSERT INTO asta (Id, Prezzo, RialzoMinimo, Scadenza,            Proprietario, Chiusa, Aggiudicatario) VALUES
    (1005,  14.50,  1, '2025-04-01 12:00:00', 'dave',  0, NULL);

-- =========================
-- ARTICOLI
-- =========================
-- Articoli collegati ad aste
INSERT INTO articolo (Codice, Nome, Descrizione, Immagine,            Prezzo,      IdAsta, Proprietario) VALUES
                                                                                                             (5003, 'Bicicletta',        'MTB usata in buono stato',  'uploads/prova.jpg',      120.00, 1001,  'alice'), -- asta 1001 (aperta, senza offerte)
                                                                                                             (5001, 'iPhone 13',         '128GB, ottime condizioni', 'uploads/prova.jpg',  350.00, 1002,  'alice'), -- asta 1002 (aperta, con offerte)
                                                                                                             (5005, 'Console X',         'Edizione limitata',        'uploads/prova.jpg',  200.00, 1003,  'carol'), -- asta 1003 (chiusa con aggiudicatario)
                                                                                                             (5006, 'Quadro moderno',    'Olio su tela',             NULL,                   0.01, 1004,  'dave'),  -- asta 1004 (chiusa senza offerte)
                                                                                                             (5007, 'Penna stilografica',    'Inchiostro nero',             NULL,                   14.50, 1005,  'dave'),  -- asta 1005 (da chiudere)
																											 (5008, 'Trattore giocattolo',    'Finto',             NULL,                   18.50, 1005,  'dave');  -- asta 1005 (da chiudere)
						
-- Articoli SENZA asta (IdAsta NULL)
INSERT INTO articolo (Codice, Nome, Descrizione, Immagine,            Prezzo,       IdAsta, Proprietario) VALUES
                                                                                                              (5002, 'Libro raro',        'Prima edizione',           NULL,               50.00,      NULL,  'bob'),   -- immagine NULL
                                                                                                              (5004, 'Collezione monete', 'Serie completa 19xx',      'uploads/prova.jpg', 99999999.99,  NULL,  'carol'); -- prezzo al limite DECIMAL(10,2)

-- =========================
-- OFFERTE
-- =========================
-- Asta 1002 (aperta): offerte presenti ma niente aggiudicatario (Chiusa=0)
INSERT INTO offerta (Id, Prezzo, Data,                 IdAsta, Offerente) VALUES
                                                                              (2001, 400.00, '2025-08-01 10:00:00', 1002, 'eve'),
                                                                              (2002, 440.00, '2025-08-02 09:30:00', 1002, 'bob'),
                                                                              (2003, 500.00, '2025-08-05 14:15:00', 1002, 'eve');

-- Asta 1003 (chiusa): offerte presenti e aggiudicatario 'bob'
INSERT INTO offerta (Id, Prezzo, Data,                 IdAsta, Offerente) VALUES
                                                                              (2004, 210.00, '2025-05-15 11:00:00', 1003, 'eve'),
                                                                              (2005, 230.00, '2025-05-20 16:45:00', 1003, 'bob'),
                                                                              (2006, 260.00, '2025-05-28 19:00:00', 1003, 'bob');  -- offerta più alta del vincitore

-- Asta 1005 (da chiudere): offerte presenti e aggiudicatario 'eve'
INSERT INTO offerta (Id, Prezzo, Data,                 IdAsta, Offerente) VALUES
    (2007, 40.00, '2025-03-15 11:00:00', 1005, 'eve');

-- Nessuna offerta per 1001 (aperta) e 1004 (chiusa) -> edge "aste senza offerte"
-- Asta 1005 da chiudere
-- Nessuna offerta per 'guest' -> edge "utente senza aste/offerte"