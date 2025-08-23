import {renderAcquistoPage} from "./acquisto.js";
import {renderVendoPage} from "./vendo.js";


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

    renderPageByLastAction();
});

export function showVendo() {
    document.getElementById("moveToAcquisto").hidden = false; // Mostra solo il pulsante "Acquisto"
    document.getElementById("moveToVendo").hidden = true;
    hideAllPages();
    renderVendoPage();
}

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