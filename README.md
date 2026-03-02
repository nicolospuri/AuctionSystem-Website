# Aste Online – Web Application (HTML & RIA)

Progetto di applicazione web per la gestione di aste online, sviluppato in doppia versione:
- **HTML puro (Multi-page)**
- **JavaScript RIA (Single Page Application)**

---

## 📌 Descrizione del Progetto

L’applicazione consente agli utenti registrati di:

- Vendere articoli tramite aste online
- Partecipare alle aste come acquirenti
- Effettuare offerte in tempo reale
- Gestire articoli, aste e transazioni

Il sistema è sviluppato secondo un’architettura **client-server**, con backend in Java
e database relazionale.

Sono state realizzate due versioni:

### ✅ Versione HTML Pura
- Navigazione multi-pagina
- Ricaricamento completo delle pagine
- Interazione tramite form HTML

### ✅ Versione JavaScript (RIA)
- Applicazione a pagina singola (SPA)
- Comunicazione asincrona (AJAX)
- Aggiornamento dinamico dei contenuti
- Persistenza lato client delle preferenze utente

---

## ⚙️ Tecnologie Utilizzate

### Frontend
- HTML
- JavaScript (AJAX / DOM)

### Backend
- Java (Servlet)
- JDBC

### Database
- SQL (MySQL / MariaDB o equivalente)

### Progettazione
- UML (diagrammi di classi e sequenza)

### Server
- Apache Tomcat

---

## 🛠️ Architettura

Il progetto segue il pattern **MVC (Model-View-Controller)**:

- **Model** → DAO + Database
- **View** → HTML / JavaScript
- **Controller** → Servlet Java
