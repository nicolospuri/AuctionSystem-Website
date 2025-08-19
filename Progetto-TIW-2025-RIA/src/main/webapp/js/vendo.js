// js/vendo.js

import { caricaListeVendo } from "./vendo.js";

document.addEventListener("DOMContentLoaded", () => {
    caricaListeVendo();
});


document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("submitNewArticolo"); //Senza questo, rischi di cercare gli elementi id=submitNewArticolo prima che esistano.
    if (btn) {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            aggiungiArticolo(); //altrimenti il form invia anche con il submit normale (ricaricando la pagina)
        });

    }
});

function aggiungiArticolo() {
    let nome = document.getElementById("nomeNewArticolo").value.trim();
    let descrizione = document.getElementById("descrizioneNewArticolo").value.trim();
    let prezzo = document.getElementById("prezzoNewArticolo").value.trim();
    let immagine = document.getElementById("immagineNewArticolo").files[0];

    if (!nome || !descrizione || !prezzo) {
        document.getElementById("newArticoloMessage").innerText =
            "Compila tutti i campi obbligatori";
        return;
    }

    let formData = new FormData();
    formData.append("nome", nome);
    formData.append("descrizione", descrizione);
    formData.append("prezzo", prezzo);
    if (immagine) {
        formData.append("immagine", immagine);
    }

    //Serve per inviare i dati del nuovo articolo (inclusi eventuali file immagine) al server senza ricaricare la pagina
    fetch("AggiungiArticolo", { //Serve per aggiungere un nuovo articolo tramite una chiamata AJAX senza ricaricare la pagina.
        method: "POST",
        body: formData
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                document.getElementById("newArticoloMessage").innerText =
                    "Articolo inserito con successo!";
                if (data.success) {
                    document.getElementById("newArticoloMessage").innerText =
                        "Articolo inserito con successo!";

                    // reset campi
                    document.getElementById("formNuovoArticolo").reset();

                    // 🔹 forza refresh lista articoli disponibili
                    setCookie("renderArticoli" + username, "true", 30);

                    // ricarica liste
                    caricaListeVendo();
                }

                // reset campi
                document.getElementById("formNuovoArticolo").reset();
                // ricarica lista aste/articoli
                caricaMieOfferte(); //todo: crea questa funzione per ricaricare la lista degli articoli
            } else {
                document.getElementById("newArticoloMessage").innerText =
                    "Errore: " + data.error;
            }
        })
        .catch(err => {
            document.getElementById("newArticoloMessage").innerText =
                "Errore: " + err.message;
        });

    function caricaMieOfferte() {
        // Supponiamo che l’username sia salvato in una variabile JS
        let username = "mario";  // <-- da sostituire con cookie/sessione

        fetch("ListaArticoliServlet?username=" + encodeURIComponent(username))
            .then(res => res.json())
            .then(lista => {
                let tbody = document.getElementById("tabellaArticoliBody");
                tbody.innerHTML = "";

                lista.forEach(articolo => {
                    let tr = document.createElement("tr");

                    tr.innerHTML = `
                    <td><input type="checkbox" name="articoliSelezionati" value="${articolo.codice}"></td>
                    <td>${articolo.codice}</td>
                    <td>${articolo.nome}</td>
                    <td>${articolo.descrizione}</td>
                    <td>${articolo.prezzo} €</td>
                    <td>${articolo.proprietario}</td>
                `;

                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error("Errore caricamento articoli:", err));
    }

    export async function caricaListeVendo() {
        try {
            const response = await fetch("Vendo", { method: "GET" });
            if (!response.ok) throw new Error("Errore fetch liste vendo");

            const data = await response.json();

            // === ASTE APERTE ===
            const tbodyAperte = document.getElementById("bodyTabellaAsteAperte");
            tbodyAperte.innerHTML = ""; // reset tabella
            if (data.openAste) {
                const template = document.getElementById("astaApertaRow");
                data.openAste.forEach(asta => {
                    const row = template.content.cloneNode(true);

                    const tr = row.querySelector("tr");
                    tr.innerHTML = `
          <td>${asta.id}</td>
          <td>${asta.tempoMancante}</td>
          <td>${asta.prezzoOffertaMassima || "—"}</td>
          <td>
            <table border="1">
              <tbody>
                ${asta.articoli.map(a => `
                  <tr>
                    <td>${a.codice}</td>
                    <td>${a.nome}</td>
                    <td>${a.prezzo}</td>
                  </tr>
                `).join("")}
              </tbody>
            </table>
          </td>
        `;
                    tbodyAperte.appendChild(tr);
                });
            }

            // === ASTE CHIUSE ===
            const tbodyChiuse = document.getElementById("bodyTabellaAsteChiuse");
            tbodyChiuse.innerHTML = "";
            if (data.closedAste) {
                const template = document.getElementById("astaChiusaRow");
                data.closedAste.forEach(asta => {
                    const row = template.content.cloneNode(true);

                    const tr = row.querySelector("tr");
                    tr.innerHTML = `
          <td>${asta.id}</td>
          <td>${asta.prezzoIniziale}</td>
          <td>${asta.prezzoOffertaMassima || "—"}</td>
          <td>${new Date(asta.scadenza).toLocaleString()}</td>
        `;
                    tbodyChiuse.appendChild(tr);
                });
            }

            // === ARTICOLI DISPONIBILI ===
            const tbodyArticoli = document.getElementById("bodyTabellaArticoliNewAsta");
            tbodyArticoli.innerHTML = "";
            if (data.articoli) {
                const template = document.getElementById("articoliSelezionabiliRow");
                data.articoli.forEach(art => {
                    const row = template.content.cloneNode(true);

                    const tr = row.querySelector("tr");
                    tr.innerHTML = `
          <td><input type="checkbox" name="codiceArticolo" value="${art.codice}"></td>
          <td>${art.codice}</td>
          <td>${art.nome}</td>
          <td>${art.descrizione}</td>
          <td>${art.prezzo}</td>
        `;
                    tbodyArticoli.appendChild(tr);
                });
            }

        } catch (err) {
            console.error("Errore caricamento liste vendo:", err);
        }
    }

}
