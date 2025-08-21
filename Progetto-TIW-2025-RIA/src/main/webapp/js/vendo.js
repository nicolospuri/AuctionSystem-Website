// js/vendo.js
//todo:quando è tutto finito creare una funzione che azzera tutti i messaggi all'utente
document.addEventListener('DOMContentLoaded', () => {
    // inizializzo la tabella articoli
    caricaListeAperte();
    caricaListeChiuse();
    aggiornaArticoliDisponibili();

    const newArticoloForm = document.getElementById("submitNewArticolo");
    const newAstaForm = document.getElementById("submitNewAsta");

    // gestione creazione articolo
    newArticoloForm.addEventListener("click", (e) => {
        e.preventDefault();
        aggiungiArticolo();
    });

    // gestione creazione asta
    newAstaForm.addEventListener("click", (e) => {
        e.preventDefault();
        creaAsta(e);
    });
});

    // Crea e ritorna un <td> con textContent
    function td(text) {
        const cell = document.createElement('td');
        cell.textContent = text;
        return cell;
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
        msg.textContent= ""; // reset messaggio
        msg.style.color = "black"; // reset colore
        if (!nome || !descrizione || !prezzo) {
            msg.style.color = "red";
            msg.style.fontWeight = "bold";
            msg.innerText = "Tutti i campi obbligatori";
            return;
    }

        let formData = new FormData();
        formData.append("nome", nome);
        formData.append("descrizione", descrizione);
        formData.append("prezzo", prezzo);
        if (immagine) formData.append("immagine", immagine); // se non selezionata, la servlet userà default.png

        fetch("AggiungiArticolo", {
            method: "POST",
            body: formData })
        .then(response => response.json())
        .then(data => {
        if (data.success) {
        emptyArticoloInputs(); // pulisco i campi
        msg.style.color = "green";
        msg.style.fontWeight = "bold";
        msg.innerText = "Articolo aggiunto!";

        // aggiungo la riga alla tabella con l'articolo appena inserito
        aggiungiArticoloAllaTabella({
        codice: data.codice,
        nome: data.nome,
        descrizione: data.descrizione,
        prezzo: data.prezzo
    });
    } else {
        msg.style.color = "red";
        msg.style.fontWeight = "bold";
        msg.innerText = "Errore: " + data.error;
    }
    })
        .catch(error => {
        console.error("Errore fetch:", error);
        msg.style.color = "red";
        msg.style.fontWeight = "bold";
        msg.innerText = "Errore di rete";
    });
    }

    // --- util: articoli selezionati dalla tabella ---
function toIsoLocalDateTime(value) {
    // <input type="datetime-local"> spesso fornisce "YYYY-MM-DDTHH:mm"
    // L'adapter usa ISO_LOCAL_DATE_TIME -> aggiungo ":00" se mancano i secondi
    if (!value) return value;
    return value.length === 16 ? value + ":00" : value;
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
        return;
    }
    const articoliSelezionati = Array.from(selectedCheckboxes).map(cb => cb.value);

    // 2. Recupero rialzo minimo e scadenza
    const rialzoMinimo = document.getElementById("rialzoMinimo").value;
    const scadenza = document.getElementById("scadenza").value; // yyyy-MM-ddTHH:mm

    if (!rialzoMinimo || rialzoMinimo <= 0) {
        messageBox.textContent = "Inserisci un rialzo minimo valido!";
        messageBox.style.color = "red";
        return;
    }
    if (!scadenza) {
        messageBox.textContent = "Inserisci una data di scadenza!";
        messageBox.style.color = "red";
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


    } catch (err) {
        console.error(err);
        messageBox.textContent = "Errore: " + err.message;
        messageBox.style.color = "red";
    }
}

// === Funzione per aggiornare la tabella articoli disponibili ===
async function aggiornaArticoliDisponibili() {
    try {
        const response = await fetch("GetArticoliServlet", { method: "GET" });
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || "Errore nel recupero articoli");
        }

        const tbody = document.getElementById("bodyTabellaArticoliNewAsta");
        tbody.innerHTML = ""; // svuoto tabella

        (data.articoli || []).forEach(a => aggiungiArticoloAllaTabella(a));

    } catch (err) {
        console.error("Errore aggiornamento articoli:", err);
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

// todo:nel caso in cui serva in futuro
/*function aggiungiArticoloAllaTabella(articolo) {
    // articolo deve avere: codice, nome, descrizione, prezzo (e opzionale immagine)
    const tbody = document.getElementById("bodyTabellaArticoliNewAsta");
    const template = document.getElementById("articoliSelezionabiliRow");

    // clono il template
    const fragment = template.content.cloneNode(true);
    const tr = fragment.querySelector("tr");

    // 1) checkbox nella prima colonna
    const checkbox = tr.querySelector('input[type="checkbox"][name="codiceArticolo"]');
    checkbox.value = articolo.codice;   // VERY IMPORTANT: value = id articolo

    // 2) resto delle colonne: id | nome | descrizione | prezzo
    tr.appendChild(td(articolo.codice));
    tr.appendChild(td(articolo.nome));
    tr.appendChild(td(articolo.descrizione));
    tr.appendChild(td(formatPrezzo(articolo.prezzo)));

    // append in tabella
    tbody.appendChild(tr);
}*/

// ===== POPOLA "Le tue aste aperte" =====
async function caricaListeAperte() {
    const tbody = document.getElementById("bodyTabellaAsteAperte");
    const tpl   = document.getElementById("astaApertaRow");
    if (!tbody) { console.error("bodyTabellaAsteAperte non trovato"); return; }

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
            const tr       = tpl ? tpl.content.firstElementChild.cloneNode(true) : document.createElement("tr");
            const tdId     = document.createElement("td");
            const tdTempo  = document.createElement("td");
            const tdOffMax = document.createElement("td");
            const tdArt    = document.createElement("td");

            tdId.textContent = a.id ?? "";

            tdTempo.textContent = a.tempoMancante
                ? a.tempoMancante
                : calcolaTempoMancante(a.scadenza);

            const offMax = (a.prezzoOffertaMassima != null)
                ? a.prezzoOffertaMassima
                : (a.offertaMassima && a.offertaMassima.prezzo) ? a.offertaMassima.prezzo : 0;
            tdOffMax.textContent = formatEuro(offMax);

            // elenco puntato: Nome — Prezzo
            const ul = buildArticoliList(a.articoli || []);
            tdArt.appendChild(ul);

            tr.append(tdId, tdTempo, tdOffMax, tdArt);
            frag.appendChild(tr);
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

async function caricaListeChiuse() {
    const tbody = document.getElementById("bodyTabellaAsteChiuse");
    const tpl   = document.getElementById("astaChiusaRow");
    if (!tbody) { console.error("bodyTabellaAsteChiuse non trovato"); return; }

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
            const tr= tpl ? tpl.content.firstElementChild.cloneNode(true) : document.createElement("tr");
            const tdId     = document.createElement("td");
            const tdPrezzo = document.createElement("td");
            const tdAgg    = document.createElement("td");
            const tdArt    = document.createElement("td");

            // ID
            tdId.textContent = a.id ?? "";

            // Prezzo finale: offerta vincente se presente, altrimenti prezzo iniziale
            const prezzoFinale = (a.prezzoOffertaMassima != null)
                ? a.prezzoOffertaMassima
                : (a.offertaMassima && a.offertaMassima.prezzo)
                    ? a.offertaMassima.prezzo
                    : (a.prezzoIniziale ?? 0);
            tdPrezzo.textContent = formatEuro(prezzoFinale);

            // Aggiudicatario
            tdAgg.textContent = a.aggiudicatario ? a.aggiudicatario : "—";

            // Articoli → elenco puntato (Nome — Prezzo)
            const ul = buildArticoliList(a.articoli || []);
            tdArt.appendChild(ul);

            tr.append(tdId, tdPrezzo, tdAgg, tdArt);
            frag.appendChild(tr);
        });

        tbody.appendChild(frag);

    } catch (err) {
        console.error("caricaListeChiuse error:", err);
        tbody.innerHTML = `<tr><td colspan="4" style="color:#b00">Errore nel caricamento</td></tr>`;
    }
}