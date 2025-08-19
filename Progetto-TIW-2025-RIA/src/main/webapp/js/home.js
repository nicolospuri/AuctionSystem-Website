// import {renderAcquistoPage} from "./acquisto.js";
import {renderVendoPage} from "./vendo.js";


document.addEventListener('DOMContentLoaded', () => {
    const moveToVendo = document.getElementById('moveToVendo');
    const moveToAcquisto = document.getElementById('moveToAcquisto');

    moveToVendo.addEventListener('click', () => {
        showVendo();
    });
    moveToAcquisto.addEventListener('click', () => {
        showAcquisto();
    });

    renderPageByLastAction();
});

export function showVendo() {
    moveToAcquisto.removeAttribute('hidden');
    moveToVendo.setAttribute('hidden', true);  // Mostra solo il pulsante "Acquisto"
    hideAllPages();
    // renderVendoPage();
}

export function showAcquisto() {
    moveToVendo.removeAttribute('hidden');
    moveToAcquisto.setAttribute('hidden', true); // Mostra solo il pulsante "Acquisto"
    hideAllPages();
    renderAcquistoPage();
}

// Nasconde tutte le pagine
export function hideAllPages() {
    document.getElementById('vendoPage').hidden = true;
    document.getElementById('acquistoPage').hidden = true;
    document.getElementById('dettaglioAstaPage').hidden = true;
    document.getElementById('offertaPage').hidden = true;
    document.getElementById('back').hidden = true;
}

function renderPageByLastAction() {
    const request = new XMLHttpRequest();
    request.open("POST",  "/Progetto_TIW_2025_RIA_war_exploded/HomeServlet");

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