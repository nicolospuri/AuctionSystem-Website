// js/vendo.js

document.addEventListener('DOMContentLoaded', () => {
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





function aggiungiArticolo(){	// e è l'evento che ha causato la chiamata della callback
    const nome = document.getElementById("nomeNewArticolo").value.trim();
    const descrizione = document.getElementById("descrizioneNewArticolo").value.trim();
    const prezzo = document.getElementById("prezzoNewArticolo").value.trim();
    const immagine = document.getElementById("immagineNewArticolo").files[0];

    if (!nome || !descrizione || !prezzo) {
        document.getElementById("newArticoloMessage").innerText = "Tutti i campi obbligatori";
        return;
    }

    let formData = new FormData();
    formData.append("nome", nome);
    formData.append("descrizione", descrizione);
    formData.append("prezzo", prezzo);
    if (immagine) {
        formData.append("immagine", immagine);
    } // se non viene selezionata nessuna immagine, non viene aggiunta

    fetch("AggiungiArticolo", {
        method: "POST",
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                emptyArticoloInputs(); // pulisce i campi del form
                document.getElementById("newArticoloMessage").innerText = "Articolo aggiunto con successo!";
            } else {
                document.getElementById("newArticoloMessage").innerText = "Errore: " + data.error;
            }
        })
        .catch(error => {
            console.error("Errore fetch:", error);
            document.getElementById("newArticoloMessage").innerText = "Errore di rete";
        });
}

function emptyArticoloInputs(){
    document.getElementById("nomeNewArticolo").value = "";
    document.getElementById("descrizioneNewArticolo").value = "";
    document.getElementById("immagineNewArticolo").value = ""; // reset input file
    document.getElementById("prezzoNewArticolo").value = "";
}
