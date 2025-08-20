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
    // aggiunta gestione eventi creazione articolo e asta
    newArticoloForm.addEventListener("click", (e) => {
        e.preventDefault();
        aggiungiArticolo();
    });

    //todo: levare commenti quando funziona creaAsta
    /*document.querySelector("#submitNewAsta").addEventListener(
        "click",
        creaAsta
    );*/
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

    // ========== TUO CODICE: AGGIUNGI ARTICOLO ==========
    function aggiungiArticolo(){ // callback del click su "Inserisci articolo"
        const nome = document.getElementById("nomeNewArticolo").value.trim();
        const descrizione = document.getElementById("descrizioneNewArticolo").value.trim();
        const prezzo = document.getElementById("prezzoNewArticolo").value.trim();
        const immagine = document.getElementById("immagineNewArticolo").files[0];

        if (!nome || !descrizione || !prezzo) {
            const msg = document.getElementById("newArticoloMessage");
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
        const msg = document.getElementById("newArticoloMessage");
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
        const msg = document.getElementById("newArticoloMessage");
        msg.style.color = "red";
        msg.style.fontWeight = "bold";
        msg.innerText = "Errore di rete";
    });
    }
