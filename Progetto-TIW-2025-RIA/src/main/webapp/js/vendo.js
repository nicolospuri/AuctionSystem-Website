// js/vendo.js

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

}
