// js/vendo.js
//todo:quando è tutto finito creare una funzione che azzera tutti i messaggi all'utente
import { mostraDettaglioAstaAperta } from './dettaglioAsta.js';
import { mostraDettaglioAstaChiusa } from './dettaglioAsta.js';

export function renderVendoPage() {
    // inizializzo la tabella articoli
    caricaListeAperte();
    caricaListeChiuse();
    aggiornaArticoliDisponibili();
    inizializzaClickDettaglioAstaAperta();
    inizializzaClickDettaglioAsteChiuse();
    resetMSG();

    // Torna alla home venditore
    document.getElementById("DettaglioAstaApertaPage").hidden = true;
    document.getElementById("vendoPage").hidden = false;
    document.getElementById("moveToVendo").hidden = true;

    //Ripulisce il contenuto della pagina dettaglio
    document.getElementById("ArticoliAstaAperta").innerHTML = "";
    document.getElementById("listaOfferte").querySelector("#bodyListaOfferte").innerHTML = "";

    const newArticoloForm = document.getElementById("submitNewArticolo");
    const newAstaForm = document.getElementById("submitNewAsta");

    // Sostituisco il bottone submitNewArticolo con un nuovo clone per poter aggiungere l'event listener
    // altrimenti aggiungerei un altro event listener oltre a quello già presente
    const newBtnArticolo = newArticoloForm.cloneNode(true);
    newArticoloForm.replaceWith(newBtnArticolo);

    newBtnArticolo.addEventListener("click", (e) => {
        e.preventDefault();
        aggiungiArticolo();
    });

    const newBtnAsta = newAstaForm.cloneNode(true);
    newAstaForm.replaceWith(newBtnAsta);

    // gestione creazione asta
    newBtnAsta.addEventListener("click", (e) => {
        e.preventDefault();
        creaAsta(e);
    });
}


// Pulisce gli input del form "nuovo articolo"
function emptyArticoloInputs() {
    const nome = document.getElementById("nomeNewArticolo");
    const descrizione = document.getElementById("descrizioneNewArticolo");
    const prezzo = document.getElementById("prezzoNewArticolo");
    const immagine = document.getElementById("immagineNewArticolo");
    if (nome) nome.value = "";
    if (descrizione) descrizione.value = "";
    if (prezzo) prezzo.value = "";
    if (immagine) immagine.value = "";
}

function aggiungiArticolo(){ // callback del click su "Inserisci articolo"
    const nome = document.getElementById("nomeNewArticolo").value.trim();
    const descrizione = document.getElementById("descrizioneNewArticolo").value.trim();
    const prezzo = document.getElementById("prezzoNewArticolo").value.trim();
    const immagine = document.getElementById("immagineNewArticolo").files[0];

    const msg = document.getElementById("newArticoloMessage");
    msg.textContent = "";            // reset messaggio
    msg.style.color = "black";       // reset colore

    if (!nome || !descrizione || !prezzo) {
        msg.style.color = "red";
        msg.style.fontWeight = "bold";
        msg.innerText = "Tutti i campi obbligatori";
        resetArticoloMSG();
        return;
    }

    const formData = new FormData();
    formData.append("nome", nome);
    formData.append("descrizione", descrizione);
    formData.append("prezzo", prezzo);
    if (immagine) formData.append("immagine", immagine); // opzionale

    fetch("AggiungiArticolo", {
        method: "POST",
        body: formData
    })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                emptyArticoloInputs(); // pulisco i campi

                // pulizia di altri messaggi eventuali
                const msgAsta   = document.getElementById("newAstaMessage");
                const msgChiudi = document.getElementById("MsgChiudiAsta");
                if (msgAsta)   msgAsta.textContent = "";
                if (msgChiudi) msgChiudi.textContent = "";

                msg.style.color = "green";
                msg.style.fontWeight = "bold";
                msg.innerText = "Articolo aggiunto!";
                resetArticoloMSG();

                aggiornaArticoliDisponibili();

            } else {
                msg.style.color = "red";
                msg.style.fontWeight = "bold";
                msg.innerText = "Errore: " + (data.error || "operazione non riuscita");
                resetArticoloMSG();
            }
        })
        .catch(err => {
            console.error("Errore fetch:", err);
            msg.style.color = "red";
            msg.style.fontWeight = "bold";
            msg.innerText = "Errore di rete";
            resetArticoloMSG();
        });
}


async function creaAsta(event) {
    event.preventDefault(); // evita submit standard del form

    const messageBox = document.getElementById("newAstaMessage");
    messageBox.textContent = "";
    messageBox.style.fontWeight = "bold";

    // 1. Recupero articoli selezionati
    const selectedCheckboxes = document.querySelectorAll("input[name='codiceArticolo']:checked");
    if (selectedCheckboxes.length === 0) {
        messageBox.textContent = "Seleziona almeno un articolo!";
        messageBox.style.color = "red";
        resetAstaMSG();
        return;
    }
    const articoliSelezionati = Array.from(selectedCheckboxes).map(cb => cb.value);

    // 2. Recupero rialzo minimo e scadenza
    const rialzoMinimo = document.getElementById("rialzoMinimo").value;
    const scadenza = document.getElementById("scadenza").value; // yyyy-MM-ddTHH:mm

    if (!rialzoMinimo || rialzoMinimo <= 0) {
        messageBox.textContent = "Inserisci un rialzo minimo valido!";
        messageBox.style.color = "red";
        resetAstaMSG();
        return;
    }

    if (!scadenza ) {
        messageBox.textContent = "Inserisci una data di scadenza!";
        messageBox.style.color = "red";
        resetAstaMSG();
        return;
    }

    const scadenzaDate = new Date(scadenza);
    const now = new Date();
    if (isNaN(scadenzaDate.getTime()) || scadenzaDate <= now) {
        messageBox.textContent = "Inserisci una data di scadenza valida!";
        messageBox.style.color = "red";
        resetAstaMSG();
        return;
    }

    try {
        // 3. Invio dati a /CreaAsta
        const formData = new URLSearchParams();
        articoliSelezionati.forEach(id => formData.append("articoliSelezionati", id));
        formData.append("rialzoMinimo", rialzoMinimo);
        formData.append("scadenza", scadenza);

        const response = await fetch("CreaAsta", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: formData.toString()
        });

        if (!response.ok) {
            throw new Error("Creazione dell'asta");
        }

        // 4. Aggiorno lista articoli disponibili
        await aggiornaArticoliDisponibili();
        await caricaListeAperte();

        messageBox.textContent = "Asta creata con successo!";
        messageBox.style.color = "green";
        emptyAstaInputs();
        resetAstaMSG();


    } catch (err) {
        console.error(err);
        messageBox.textContent = "Errore: " + err.message;
        messageBox.style.color = "red";
        resetAstaMSG();
    }
}

// === Funzione per aggiornare la tabella articoli disponibili ===
async function aggiornaArticoliDisponibili() {
    try {
        const response = await fetch("GetArticoliServlet", {
            method: "GET",cache: "no-store"
        });
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || "Errore nel recupero articoli");
        }

        const tbody = document.getElementById("bodyTabellaArticoliNewAsta");
        const form = document.getElementById("formNewAsta");
        const msg = document.getElementById("MSGArticoliDisponibili");

        tbody.innerHTML = ""; // svuoto tabella

        if (!data.articoli || data.articoli.length === 0) {
            // Nessun articolo disponibile
            form.hidden = true;
            msg.textContent = "Nessun articolo disponibile";
            msg.style.color = "black";
            msg.style.fontStyle = "italic";
        } else {
            // Articoli trovati → mostro tabella
            msg.textContent = "";
            form.hidden = false;
            (data.articoli || []).forEach(a => aggiungiArticoloAllaTabella(a));
        }

    } catch (err) {
        console.error("Errore aggiornamento articoli:", err);
        const msg = document.getElementById("MSGArticoliDisponibili");
        if (msg) {
            msg.textContent = "Errore nel caricamento degli articoli";
            msg.style.color = "red";
            msg.style.fontWeight = "bold";
        }
    }
}


// === Funzione per aggiungere una riga di articolo alla tabella ===
function aggiungiArticoloAllaTabella(articolo) {
    const tbody = document.getElementById("bodyTabellaArticoliNewAsta");
    const template = document.getElementById("articoliSelezionabiliRow");

    // Clono il template
    const row = template.content.cloneNode(true);

    // setto il valore della checkbox
    const checkbox = row.querySelector("input[type='checkbox']");
    checkbox.value = articolo.codice;

    // creo le celle dinamiche
    const tdCodice = document.createElement("td");
    tdCodice.textContent = articolo.codice;

    const tdNome = document.createElement("td");
    tdNome.textContent = articolo.nome;

    const tdDescrizione = document.createElement("td");
    tdDescrizione.textContent = articolo.descrizione;

    const tdPrezzo = document.createElement("td");
    tdPrezzo.textContent = articolo.prezzo.toFixed(2) + " €";

    // aggiungo le celle alla riga
    row.querySelector("tr").append(tdCodice, tdNome, tdDescrizione, tdPrezzo);

    // appendo la riga alla tabella
    tbody.appendChild(row);
}

function emptyAstaInputs() {
    const rialzo=document.getElementById("rialzoMinimo");
    const scadenza=document.getElementById("scadenza");
    if(rialzo) rialzo.value="";
    if(scadenza) scadenza.value="";
}

// ===== POPOLA "Le tue aste aperte" =====
export async function caricaListeAperte() {
    const tbody = document.getElementById("bodyTabellaAsteAperte");
    const tpl   = document.getElementById("astaApertaRow");
    if (!tbody || !tpl) {
        console.error("bodyTabellaAsteAperte o template non trovato");
        return;
    }

    tbody.innerHTML = "";

    try {
        const resp = await fetch("CaricaListeAperte", {
            method: "GET",
            headers: { "Accept": "application/json" },
            credentials: "same-origin",
            cache: "no-store"
        });

        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);

        const data = await resp.json();
        if (!data.success) {
            tbody.innerHTML = `<tr><td colspan="4" style="color:#b00">Errore: ${data.error || 'richiesta fallita'}</td></tr>`;
            return;
        }

        const liste = Array.isArray(data.asteAperte) ? data.asteAperte : [];

        if (liste.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4">Nessuna asta aperta</td></tr>`;
            return;
        }

        const frag = document.createDocumentFragment();

        liste.forEach(a => {
            const row = tpl.content.firstElementChild.cloneNode(true);

            // Popola il link nell'ID Asta
            const link = row.querySelector(".id-asta-link");
            if (link) {
                link.textContent = a.id;
                link.dataset.id = a.id;
            }

            // Tempo mancante
            const tempoCell = row.querySelector(".tempo-mancante");
            if (tempoCell) {
                tempoCell.textContent = a.tempoMancante
                    ? a.tempoMancante
                    : calcolaTempoMancante(a.scadenza);
            }

            // Offerta massima
            const offertaCell = row.querySelector(".offerta-massima");
            if (offertaCell) {
                const offMax = (a.prezzoOffertaMassima != null)
                    ? a.prezzoOffertaMassima
                    : (a.offertaMassima && a.offertaMassima.prezzo)
                        ? a.offertaMassima.prezzo
                        : 0;
                offertaCell.textContent = formatEuro(offMax);
            }

            // Articoli
            const articoliCell = row.querySelector(".articoli");
            if (articoliCell) {
                articoliCell.innerHTML = "";
                const ul = buildArticoliList(a.articoli || []);
                articoliCell.appendChild(ul);
            }

            frag.appendChild(row);
        });

        tbody.appendChild(frag);

    } catch (err) {
        console.error("caricaListeAperte error:", err);
        tbody.innerHTML = `<tr><td colspan="4" style="color:#b00">Errore nel caricamento</td></tr>`;
    }
}


function formatEuro(n) {
    const num = Number(n);
    if (Number.isFinite(num)) return num.toLocaleString(undefined, { style: "currency", currency: "EUR", minimumFractionDigits: 2 });
    return `${n} €`;
}
function safe(v){ return v ?? ""; }
function escapeHTML(v){
    return String(v).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")
        .replaceAll('"',"&quot;").replaceAll("'","&#039;");
}
function calcolaTempoMancante(iso){
    if (!iso) return "";
    const end = new Date(iso), now = new Date();
    if (isNaN(end)) return "";
    const diff = end - now;
    if (diff <= 0) return "scaduta";
    const giorni = Math.floor(diff / (1000*60*60*24));
    const ore    = Math.floor((diff % (1000*60*60*24)) / (1000*60*60));
    return `${giorni} giorni e ${ore} ore`;
}

function buildArticoliList(articoli) {
    const ul = document.createElement("ul");
    ul.className = "articoli-list";
    if (!Array.isArray(articoli) || articoli.length === 0) {
        const li = document.createElement("li");
        li.textContent = "Nessun articolo";
        ul.appendChild(li);
        return ul;
    }
    articoli.forEach(ar => {
        const li = document.createElement("li");
        // Richiesto: nome + prezzo (senza tabella, senza codice)
        li.innerHTML = `${escapeHTML(safe(ar.nome))} — <strong>${formatEuro(ar.prezzo)}</strong>`;
        ul.appendChild(li);
    });
    return ul;
}

export async function caricaListeChiuse() {
    const tbody = document.getElementById("bodyTabellaAsteChiuse");
    const tpl   = document.getElementById("astaChiusaRow");
    if (!tbody || !tpl) {
        console.error("bodyTabellaAsteChiuse o template non trovato");
        return;
    }

    tbody.innerHTML = "";

    try {
        const resp = await fetch("CaricaListeChiuse", {
            method: "GET",
            headers: { "Accept": "application/json" },
            credentials: "same-origin",
            cache: "no-store"
        });

        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        const data = await resp.json();

        if (!data.success) {
            tbody.innerHTML = `<tr><td colspan="4" style="color:#b00">Errore: ${data.error || 'richiesta fallita'}</td></tr>`;
            return;
        }

        const liste = Array.isArray(data.asteChiuse) ? data.asteChiuse : [];
        if (liste.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4">Nessuna asta chiusa</td></tr>`;
            return;
        }

        const frag = document.createDocumentFragment();

        liste.forEach(a => {
            const row = tpl.content.firstElementChild.cloneNode(true);

            // ID come link cliccabile
            const link = row.querySelector(".id-asta-chiusa-link");
            if (link) {
                link.textContent = a.id;
                link.dataset.id = a.id;
            }

            // Prezzo finale
            const tdPrezzo = row.querySelector(".prezzo-finale");
            if (tdPrezzo) {
                const prezzoFinale = (a.prezzoOffertaMassima != null)
                    ? a.prezzoOffertaMassima
                    : (a.prezzoIniziale ?? 0);
                tdPrezzo.textContent = formatEuro(prezzoFinale);
            }

            // Aggiudicatario
            const tdAgg = row.querySelector(".aggiudicatario");
            if (tdAgg) tdAgg.textContent = a.aggiudicatario || "—";

            // Articoli
            const tdArt = row.querySelector(".articoli");
            if (tdArt) {
                tdArt.innerHTML = "";
                const ul = buildArticoliList(a.articoli || []);
                tdArt.appendChild(ul);
            }

            frag.appendChild(row);
        });

        tbody.appendChild(frag);

    } catch (err) {
        console.error("caricaListeChiuse error:", err);
        tbody.innerHTML = `<tr><td colspan="4" style="color:#b00">Errore nel caricamento</td></tr>`;
    }
}


function inizializzaClickDettaglioAstaAperta() {
    const tbody = document.getElementById("bodyTabellaAsteAperte");

    tbody.addEventListener("click", async (e) => {
        const td = e.target.closest("td");
        if (!td || td.cellIndex !== 0) return; // Solo prima colonna (ID Asta)

        const idAsta = td.textContent.trim();
        if (!idAsta) return;

        try {
            const resp = await fetch(`DettaglioAstaServlet?idAsta=${idAsta}`, {
                headers: { "X-Requested-With": "XMLHttpRequest" }
            });

            if (!resp.ok) throw new Error(`Errore HTTP ${resp.status}`);
            const data = await resp.json();

            mostraDettaglioAstaAperta(data);

        } catch (err) {
            console.error("Errore caricamento dettaglio asta:", err);
            alert("Errore nel caricamento del dettaglio asta.");
        }
    });
}

function inizializzaClickDettaglioAsteChiuse() {
    const tbody = document.getElementById("bodyTabellaAsteChiuse");
    if (!tbody) return;

    tbody.addEventListener("click", async (e) => {
        const link = e.target.closest(".id-asta-chiusa-link");
        if (!link) return;

        e.preventDefault();
        const idAsta = link.dataset.id;
        if (!idAsta) return;

        try {
            const resp = await fetch(`DettaglioAstaServlet?idAsta=${idAsta}`, {
                headers: { "X-Requested-With": "XMLHttpRequest" }
            });
            if (!resp.ok) throw new Error(`Errore HTTP ${resp.status}`);

            const data = await resp.json();
            mostraDettaglioAstaChiusa(data); // <-- importata da ./DettaglioAsta.js

        } catch (err) {
            console.error("Errore caricamento dettaglio asta chiusa:", err);
            alert("Errore nel caricamento del dettaglio dell'asta chiusa.");
        }
    });
}

export function resetMSG(){
    const msgArt=document.getElementById("newArticoloMessage");
    const msgAsta=document.getElementById("newAstaMessage");
    const msgChiudi=document.getElementById("MsgChiudiAsta");
    if(msgArt) msgArt.textContent="";
    if(msgAsta) msgAsta.textContent="";
    if(msgChiudi) msgChiudi.textContent="";
}

function resetAstaMSG(){
    const msgArt=document.getElementById("newArticoloMessage");
    const msgChiudi=document.getElementById("MsgChiudiAsta");
    if(msgArt) msgArt.textContent="";
    if(msgChiudi) msgChiudi.textContent="";
}

function resetArticoloMSG(){
    const msgAsta=document.getElementById("newAstaMessage");
    const msgChiudi=document.getElementById("MsgChiudiAsta");
    if(msgAsta) msgAsta.textContent="";
    if(msgChiudi) msgChiudi.textContent="";
}