import { renderAcquistoPage } from './acquisto.js';
import { hideAllPages, showAcquisto } from './home.js';

export function renderOffertaPage(idAsta) {
    hideAllPages();
    document.getElementById("moveToAcquisto").hidden = false; // Mostra anche il pulsante "Acquisto"

    const request = new XMLHttpRequest();
    request.open('GET', `OfferteServlet?idAsta=${encodeURIComponent(idAsta)}`);

    request.onreadystatechange = () => {
        if (request.readyState === 4) {
            if (request.status === 200) {
                document.getElementById('articoliAstaBody').innerHTML = '';
                document.getElementById('offerteAstaBody').innerHTML = '';
                document.getElementById('offerteMsg').innerHTML = '';

                const jsonResponse = JSON.parse(request.responseText);

                const articoliAsta = jsonResponse.articoliAsta;
                const offerteAsta = jsonResponse.offerteAsta;
                const rialzoMinimo = jsonResponse.rialzoMinimo;
                const prezzoOffertaMassima = jsonResponse.prezzoOffertaMassima;
                const canOffer = jsonResponse.canOffer;

                if (articoliAsta != null && articoliAsta.length > 0) {
                    document.getElementById('articoliAsta').hidden = false;
                    for (const articolo of articoliAsta) {
                        addArticoloInTable(articolo);
                    }
                }

                if (offerteAsta != null && offerteAsta.length > 0) {
                    document.getElementById('offerteAsta').hidden = false;
                    for (const offerta of offerteAsta) {
                        addOffertaInTable(offerta);
                    }
                } else {
                    document.getElementById('offerteMsg').textContent = 'Nessuna offerta presente per questa asta';
                    document.getElementById('offerteAsta').hidden = true;
                }

                if (canOffer) {
                    document.getElementById('canOffer').hidden = false;
                    document.getElementById("showRialzoMinimo").textContent = rialzoMinimo + ".00 €";

                    document.getElementById("faiOfferta").addEventListener("click", (e) => {
                        e.preventDefault();
                        faiOfferta(prezzoOffertaMassima, rialzoMinimo);
                    })
                } else {
                    document.getElementById('canOffer').hidden = true;
                }

            } else {
                document.getElementById("offerteMsg").textContent = "Errore interno al server";
                document.getElementById('articoliAsta').hidden = true;
                document.getElementById('offerteAsta').hidden = true;
                document.getElementById('canOffer').hidden = true;
            }
        }
    }

    document.getElementById('offertaPage').hidden = false;

    request.send();
}

function addArticoloInTable(articolo) {
    const tbody = document.getElementById("articoliAstaBody");

    const newRow = document.createElement("tr");
    newRow.style.textAlign = "center";

    const codiceTd = document.createElement("td");
    codiceTd.textContent = articolo.codice;
    newRow.appendChild(codiceTd);

    const nomeTd = document.createElement("td");
    nomeTd.textContent = articolo.nome;
    newRow.appendChild(nomeTd);

    const descrizioneTd = document.createElement("td");
    descrizioneTd.textContent = articolo.descrizione;
    newRow.appendChild(descrizioneTd);

    const prezzoTd = document.createElement("td");
    prezzoTd.textContent = articolo.prezzo + " €";
    newRow.appendChild(prezzoTd);

    tbody.appendChild(newRow);
}

function addOffertaInTable(offerta) {
    const tbody = document.getElementById("offerteAstaBody");

    const newRow = document.createElement("tr");
    newRow.style.textAlign = "center";

    const offertenteTd = document.createElement("td");
    offertenteTd.textContent = offerta.offerente;
    newRow.appendChild(offertenteTd);

    const prezzoTd = document.createElement("td");
    prezzoTd.textContent = offerta.prezzo + " €";
    newRow.appendChild(prezzoTd);

    const dataTd = document.createElement("td");
    dataTd.textContent = offerta.data;
    newRow.appendChild(dataTd);

    tbody.appendChild(newRow);
}

function faiOfferta(prezzoOffertaMassima, rialzoMinimo) {
    document.getElementById("offertaSuccessMsg").innerHTML = "";
    document.getElementById("offertaErrorMsg").innerHTML = "";

    const input = document.getElementById("prezzoOfferto");
    if (input == null) {
        document.getElementById("offertaErrorMsg").textContent = "Il prezzo deve essere maggiore di zero";
        return;
    }
    const prezzoOfferto = parseFloat(input.value);
    if (isNaN(prezzoOfferto)) {
        document.getElementById("offertaErrorMsg").textContent = "Prezzo non valido";
        return;
    }
    if (prezzoOfferto < 0) {
        document.getElementById("offertaErrorMsg").textContent = "Il prezzo deve essere maggiore di zero";
        return;
    }
    if (prezzoOfferto < prezzoOffertaMassima + rialzoMinimo) {
        document.getElementById("offertaErrorMsg").textContent = "L'offerta deve rialzare il prezzo almeno quanto il rialzo minimo";
        return;
    }

    const formData = new FormData();
    formData.append("prezzoOfferto", prezzoOfferto);

    const request = new XMLHttpRequest();
    request.open("POST", "FaiOfferta");

    request.onreadystatechange = () => {
        if (request.readyState === 4) {
            if (request.status === 200) {
                const response = JSON.parse(request.responseText);

                // In caso di errore, mostro il messaggio di errore
                if (response.offertaErrorMsg) {
                    document.getElementById("offertaErrorMsg").textContent = response.offertaErrorMsg;
                    return
                }

                // Manda il messaggio di successo e inserisci l'offerta nella tabella
                document.getElementById("offertaSuccessMsg").textContent = response.offertaSuccessMsg;
                addOffertaInTable(response.offerta);

                // Ripulisco input
                document.getElementById("prezzoOfferto").value = '';
            } else {
                document.getElementById("offertaErrorMsg").textContent = "Errore interno al server";
            }
        }
    }
    request.send(formData);
}