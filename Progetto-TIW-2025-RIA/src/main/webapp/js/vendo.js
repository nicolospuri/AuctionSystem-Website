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

