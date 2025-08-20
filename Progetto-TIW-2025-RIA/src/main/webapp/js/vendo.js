// js/vendo.js

document.addEventListener('DOMContentLoaded', () => {
    // inizializzo la tabella articoli
    fetch("GetArticoliServlet", { method: "GET" })
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                console.error("Errore caricamento articoli:", data.error);
                return;
            }
            (data.articoli || []).forEach(a => aggiungiArticoloAllaTabella(a));
        })
        .catch(err => console.error("Errore fetch GetArticoliServlet:", err));

    const newArticoloForm = document.getElementById("submitNewArticolo");
    const newAstaForm = document.getElementById("submitNewAsta");
    // aggiunta gestione eventi creazione articolo e asta
    newArticoloForm.addEventListener("click", (e) => {
        e.preventDefault();
        aggiungiArticolo();
    });

    //todo: levare commenti quando funziona creaAsta
    newAstaForm.addEventListener("click", (e) => {
        e.preventDefault();
        creaAsta();
    });
});


    // ========== HELPERS ==========
    function formatPrezzo(val) {
    const num = Number(val);
    if (Number.isNaN(num)) return val;
    return num.toFixed(2); // 2 decimali
}

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

    // ========== RENDER: UNA RIGA IN TABELLA ==========
    function aggiungiArticoloAllaTabella(articolo) {
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
    function getArticoliSelezionati() {
    const checked = document.querySelectorAll(
    '#bodyTabellaArticoliNewAsta input[type="checkbox"][name="codiceArticolo"]:checked'
    );
    return Array.from(checked)
    .map(cb => Number(cb.value))
    .filter(Number.isFinite);
}

function toIsoLocalDateTime(value) {
    // value es: "2025-08-20T14:30" -> "2025-08-20T14:30:00"
    if (!value) return value;
    return value.length === 16 ? value + ":00" : value;
}

function creaAsta(e) {
    if (e) e.preventDefault();
    const msg = document.getElementById("newAstaMessage");
    const btn = document.getElementById("submitNewAsta");
    const rialzoMinimo = parseInt(document.getElementById("rialzoMinimo").value, 10);
    const rawScadenza = document.getElementById("scadenza").value;

    msg.textContent= ""; // reset messaggio
    msg.style.color = "black"; // reset colore
    const articoli = Array.from(
        document.querySelectorAll('#bodyTabellaArticoliNewAsta input[type="checkbox"][name="codiceArticolo"]:checked')
    ).map(cb => Number(cb.value)).filter(Number.isFinite);

    if (!articoli.length) { msg.textContent = "Seleziona almeno un articolo."; msg.style.color = "red"; return; }
    if (!Number.isInteger(rialzoMinimo) || rialzoMinimo < 1) { msg.textContent = "Rialzo minimo non valido (>=1)."; msg.style.color = "red"; return; }
    if (!rawScadenza) { msg.textContent = "Inserisci la data di scadenza."; msg.style.color = "red"; return; }

    const payload = {
        articoli,
        rialzoMinimo,
        scadenza: toIsoLocalDateTime(rawScadenza) // <-- compatibile col tuo Adapter
    };

    if (btn) { btn.disabled = true; btn.dataset.prevText = btn.textContent; btn.textContent = "Creazione…"; }
    msg.textContent = "";

    fetch("CreaAstaServlet", {
        method: "POST",
        headers: { "Content-Type": "application/json;charset=UTF-8" },
        body: JSON.stringify(payload)
    })
        .then(async res => {
            const text = await res.text();
            if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText} – ${text || "nessun body"}`);
            try { return JSON.parse(text); } catch { throw new Error("Risposta non JSON: " + text); }
        })
        .then(data => {
            if (data.success) {
                msg.textContent = `Asta creata.`;
                msg.style.color = "green";
                document.querySelectorAll('#bodyTabellaArticoliNewAsta input[type="checkbox"][name="codiceArticolo"]:checked')
                    .forEach(cb => cb.checked = false);
            } else {
                throw new Error(data.error || "Operazione fallita");
            }
        })
        .catch(err => { console.error(err); msg.textContent = "Errore: " + err.message; msg.style.color = "red"; })
        .finally(() => { if (btn) { btn.disabled = false; btn.textContent = btn.dataset.prevText || "Crea Asta"; } });
}

