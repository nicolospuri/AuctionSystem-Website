// js/vendo.js
//todo:quando è tutto finito creare una funzione che azzera tutti i messaggi all'utente
document.addEventListener('DOMContentLoaded', () => {
    // inizializzo la tabella articoli
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