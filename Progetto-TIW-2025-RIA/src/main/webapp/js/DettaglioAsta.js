import { caricaListeAperte, caricaListeChiuse } from "./vendo.js";

document.addEventListener('DOMContentLoaded', () => {
    // ...già presente il resto...

    // Handler CHIUDI ASTA
    const chiudiBtn = document.getElementById("chiudiAsta");
    if (chiudiBtn) {
        chiudiBtn.addEventListener("click", async () => {
            const idAsta = chiudiBtn.dataset.idAsta;
            if (!idAsta) return;

            chiudiBtn.disabled = true;

            try {
                const resp = await fetch("ChiudiAsta", {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: new URLSearchParams({ idAsta }).toString()
                });


                const data = await resp.json();
                if (!resp.ok || !data.success) {
                    alert("Errore nella chiusura: " + (data.error || `HTTP ${resp.status}`));
                    chiudiBtn.disabled = false;
                    return;
                }

                const msg = document.getElementById("MsgChiudiAsta");
                msg.textContent="Asta chiusa con successo";
                msg.style.color="green";

                // Pulizia dettaglio & ritorno a Vendo
                document.getElementById("ArticoliAstaAperta").innerHTML = "";
                document.getElementById("bodyListaOfferte").innerHTML = "";
                document.getElementById("listaOfferte").hidden = true;
                document.getElementById("DettaglioAstaApertaPage").hidden = true;
                document.getElementById("vendoPage").hidden = false;
                document.getElementById("moveToVendo").hidden = true;
                document.getElementById("moveToAcquisto").hidden = false;

                // Refresh liste
                await caricaListeAperte();
                await caricaListeChiuse();

            } catch (err) {
                console.error("Errore chiusura asta:", err);
                alert("Errore di rete durante la chiusura dell'asta");
                chiudiBtn.disabled = false;
            }
        });
    }
});

function formatEuro(n) {
    const num = Number(n);
    return Number.isFinite(num)
        ? num.toLocaleString(undefined, { style: "currency", currency: "EUR", minimumFractionDigits: 2 })
        : `${n} €`;
}

export function mostraDettaglioAstaAperta(data) {
    // Mostra/nasconde le sezioni
    document.getElementById("DettaglioAstaChiusaPage").hidden = true;
    document.getElementById("vendoPage").hidden = true;
    document.getElementById("DettaglioAstaApertaPage").hidden = false;
    document.getElementById("moveToVendo").hidden = false;
    const msg = document.getElementById("MsgChiudiAsta");
    msg.textContent = "";

    // Lista dettagli + articoli
    const ul = document.getElementById("ArticoliAstaAperta");
    ul.innerHTML = "";

    // Dettagli principali (come nello screenshot)
    const dettagli = [
        `ID Asta: ${data.id}`,
        `Prezzo Iniziale: ${data.prezzo}`,                     // come nel tuo esempio "81.0"
        `Rialzo Minimo: ${Number(data.rialzoMinimo).toFixed(2)} €`,
        `Scadenza: ${data.scadenza}`
    ];
    dettagli.forEach(txt => {
        const li = document.createElement("li");
        li.textContent = txt;
        ul.appendChild(li);
    });

    // Etichetta "Articoli:"
    const liArtTitle = document.createElement("li");
    liArtTitle.textContent = "Articoli:";
    ul.appendChild(liArtTitle);

    // Lista articoli annidata
    const innerUl = document.createElement("ul");
    (data.articoli || []).forEach(a => {
        const li = document.createElement("li");
        li.textContent = `${a.nome} — €${Number(a.prezzo).toFixed(2)}`;
        innerUl.appendChild(li);
    });
    ul.appendChild(innerUl);

    // Tabella offerte
    const tbody = document.getElementById("bodyListaOfferte");
    tbody.innerHTML = "";

    if (!data.offerte || data.offerte.length === 0) {
        const row = document.createElement("tr");
        const td = document.createElement("td");
        td.colSpan = 3;
        td.textContent = "Nessuna offerta";
        td.style.textAlign = "center";
        td.style.fontStyle = "italic";
        row.appendChild(td);
        tbody.appendChild(row);
    } else {
        (data.offerte || []).forEach(o => {
            const row = document.createElement("tr");
            row.innerHTML = `
      <td>${o.offerente}</td>
      <td>€${Number(o.prezzo).toFixed(2)}</td>
      <td>${o.data}</td>
    `;
            tbody.appendChild(row);
        });
    }


    // Bottone "Chiudi Asta" solo se scaduta
    const chiudiBtn = document.getElementById("chiudiAsta");
    const scadenza = new Date(data.scadenza);
    chiudiBtn.hidden = (new Date() < scadenza);
    chiudiBtn.dataset.idAsta = data.id; // <-- necessario per il click handler

}


export function mostraDettaglioAstaChiusa(data) {
    // Nasconde le altre sezioni
    document.getElementById("DettaglioAstaApertaPage").hidden = true;
    document.getElementById("DettaglioAstaChiusaPage").hidden = false;
    document.getElementById("vendoPage").hidden = true;
    document.getElementById("moveToVendo").hidden = false;

    const page = document.getElementById("DettaglioAstaChiusaPage");
    const ul = document.getElementById("ArticoliAstaChiusa");

    page.hidden = false;
    ul.innerHTML = ""; // Pulisce contenuto precedente

    // Dettagli principali
    const dettagli = [
        `ID Asta: ${data.id}`,
        `Prezzo Iniziale: ${formatEuro(data.prezzo)}`,
        `Rialzo Minimo: ${formatEuro(data.rialzoMinimo)}`,
        `Scadenza: ${data.scadenza}`,
        `Aggiudicatario: ${data.aggiudicatario || "—"}`,
        `Prezzo Finale: ${formatEuro(data.prezzoFinale || data.prezzoOffertaMassima || data.prezzo)}`,
        `Indirizzo Aggiudicatario: ${data.indirizzoAggiudicatario || "—"}`,
        `Articoli:`
    ];

    dettagli.forEach(txt => {
        const li = document.createElement("li");
        li.textContent = txt;
        ul.appendChild(li);
    });

    // Articoli → lista interna
    if (Array.isArray(data.articoli)) {
        const innerUl = document.createElement("ul");
        data.articoli.forEach(a => {
            const li = document.createElement("li");
            li.textContent = `${a.nome} — ${formatEuro(a.prezzo)}`;
            innerUl.appendChild(li);
        });
        ul.appendChild(innerUl);
    }
}