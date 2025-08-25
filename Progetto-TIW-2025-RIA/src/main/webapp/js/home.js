import {renderAcquistoPage} from "./acquisto.js";
import {renderVendoPage} from "./vendo.js";

// Associa gli eventi ai pulsanti per spostarsi tra le pagine una volta che il DOM è stato caricato
document.addEventListener("DOMContentLoaded", () => {
    // Associa gli eventi ai pulsanti per spostarsi tra le pagine
    document.getElementById("moveToVendo").addEventListener('click', (e) => {
        e.preventDefault();
        showVendo();
    });
    document.getElementById("moveToAcquisto").addEventListener('click', (e) => {
        e.preventDefault();
        showAcquisto();
    });

    // Mostra la pagina in base all'ultima azione dell'utente
    renderPageByLastAction();
});

// Mostra la pagina Vendo e nasconde le altre
export function showVendo() {
    document.getElementById("moveToAcquisto").hidden = false; // Mostra solo il pulsante "Acquisto"
    document.getElementById("moveToVendo").hidden = true;
    hideAllPages();
    renderVendoPage();
}

// Mostra la pagina Acquisto e nasconde le altre
export function showAcquisto() {
    document.getElementById("moveToAcquisto").hidden = true;
    document.getElementById("moveToVendo").hidden = false; // Mostra solo il pulsante "Vendo"
    hideAllPages();
    renderAcquistoPage();
}

// Nasconde tutte le pagine
export function hideAllPages() {
    document.getElementById("vendoPage").hidden = true;
    document.getElementById("acquistoPage").hidden = true;
    document.getElementById("DettaglioAstaApertaPage").hidden = true;
    document.getElementById("listaOfferte").hidden = false;
    document.getElementById("DettaglioAstaChiusaPage").hidden = true;
    document.getElementById("offertaPage").hidden = true;
}

// Mostra la pagina in base all'ultima azione dell'utente (creazione asta o ricerca asta)
function renderPageByLastAction() {
    const request = new XMLHttpRequest();
    request.open("POST",  "HomeServlet");

    request.onreadystatechange = () => {
        if(request.readyState === 4){
            if(request.status === 200){
                const userLastActionWasAddedAsta = JSON.parse(request.responseText).userLastActionWasAddedAsta;

                if (userLastActionWasAddedAsta) {		// entra nel then SSE l'ultima azione è la creazione dell'asta
                    showVendo();
                } else {
                    showAcquisto();
                }
            }
            else{
                alert("Problema con il caricamento dei dati dal server");
            }
        }
    }
    request.send();
}