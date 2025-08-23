import { renderOffertaPage } from "./offerta.js";

export function renderAcquistoPage(){
    document.getElementById("acquistoPage").hidden = false;
    document.getElementById("asteTrovate").hidden = true;
    document.getElementById("keyword").value = "";

    // Sostituisco il bottone faiOfferta con un nuovo clone per poter aggiungere l'event listener
    // altrimenti aggiungerei un altro event listener oltre a quello già presente
    const oldBtn = document.getElementById("cercaPerParolaChiave");
    const newBtn = oldBtn.cloneNode(true);
    oldBtn.replaceWith(newBtn);

    newBtn.addEventListener("click", (e) => {
        e.preventDefault();
        searchAstaByKeyword();
    });

    renderAsteVisionateEAggiudicate();
}

function searchAstaByKeyword(){
    document.getElementById("asteTrovateMsg").textContent = "";
    document.getElementById("asteTrovate").hidden = true;

    const keyword = document.getElementById("keyword").value;

    if(!keyword){
        document.getElementById("asteTrovateMsg").textContent = "Parola chiave mancante";
        return;
    }

    // Creazione parametri da passare con la richiesta
    const formData = new FormData();
    formData.append("keyword", keyword);

    // Creazione richiesta
    const request = new XMLHttpRequest();
    request.open("POST", "AcquistoServlet");

    request.onreadystatechange = () => {
        showAsteByKeyword(request);
    };

    request.send(formData);
}

function showAsteByKeyword(request){
    if(request.readyState === 4){
        if(request.status === 200){
            document.getElementById("asteTrovateBody").innerHTML = "";		// Svuoto la tabella delle aste con la parola chiave precedentemente ricercata
            document.getElementById("asteTrovateMsg").innerHTML = '';

            const aste = JSON.parse(request.responseText);

            if(aste.length > 0){
                document.getElementById("asteTrovate").hidden = false;
                aste.forEach((asta) => {
                    addAstaTrovataInTable(asta, "asteTrovateBody");
                });
            } else{
                document.getElementById("asteTrovateMsg").textContent = "Nessuna asta trovata";
                document.getElementById("asteTrovate").hidden = true;
            }
        } else{
            document.getElementById("asteTrovateMsg").textContent = "Errore interno al server";
            document.getElementById("asteTrovate").hidden = true;
        }
    }
}

function addAstaTrovataInTable(asta, tableBodyId){
    const tbody = document.getElementById(tableBodyId);

    const newRow = document.createElement("tr");
    newRow.style.textAlign = "center";

    const idAstaTd = document.createElement("td");
    idAstaTd.textContent = asta.id;
    idAstaTd.style.cursor = "pointer";
    idAstaTd.addEventListener("click", (e) => {
        e.preventDefault();
        renderOffertaPage(asta.id);
    });
    newRow.appendChild(idAstaTd);

    const prezzoInizialeTd = document.createElement("td");
    prezzoInizialeTd.textContent = asta.prezzoIniziale + " €";
    newRow.appendChild(prezzoInizialeTd);

    const rialzoMinimoTd = document.createElement("td");
    rialzoMinimoTd.textContent = asta.rialzoMinimo + ".00 €";
    newRow.appendChild(rialzoMinimoTd);

    const dataScadenzaTd = document.createElement("td");
    dataScadenzaTd.textContent = asta.scadenza;
    newRow.appendChild(dataScadenzaTd);

    const proprietarioTd = document.createElement("td");
    proprietarioTd.textContent = asta.proprietario;
    newRow.appendChild(proprietarioTd);

    // Creazione tabella articoli
    const articlesList = document.createElement("ul");

    asta.articoli.forEach((articolo) => {
        const articoloLi = document.createElement("li");
        articoloLi.textContent = articolo.codice + " - " + articolo.nome;
        articlesList.appendChild(articoloLi);
    });

    const articlesListTd = document.createElement("td");
    articlesListTd.appendChild(articlesList);
    newRow.appendChild(articlesListTd);

    // Inserisco la nuova riga nella tabella delle aste aperte
    tbody.appendChild(newRow);
}

function renderAsteVisionateEAggiudicate(){
    // Richiedi al server le aste visionate (la servlet analizzerà la lista di cookie e restituirà la lista delle rispettive aste)
    const request = new XMLHttpRequest();
    request.open("GET", "AcquistoServlet");

    request.onreadystatechange = () => {
        if (request.readyState === 4) {
            if (request.status === 200) {
                // Se è presente del contenuto nella risposta => sono le aste visitate da mostrare
                document.getElementById("asteVisitateBody").innerHTML = '';	// Svuoto la tabella precedente per far spazio ai dati aggiornati
                document.getElementById("asteVisitateMsg").innerHTML = '';
                document.getElementById("asteVinteBody").innerHTML = '';
                document.getElementById("asteVinteMsg").innerHTML = '';

                const jsonResponse = JSON.parse(request.responseText);

                // Mostro le aste visitate
                const asteVisitate = jsonResponse.asteVisitate;

                if (asteVisitate != null && asteVisitate.length > 0) {
                    document.getElementById("asteVisitate").hidden = false;

                    for (const asta of asteVisitate) {
                        addAstaTrovataInTable(asta, "asteVisitateBody");
                    }
                } else {
                    document.getElementById("asteVisitateMsg").textContent = "Nessuna asta visitata";
                    document.getElementById("asteVisitate").hidden = true;
                }

                const asteVinte = jsonResponse.asteVinte;
                if (asteVinte != null && asteVinte.length > 0) {
                    document.getElementById("asteVinte").hidden = false;

                    for (const asta of asteVinte) {
                        addAstaVintaInTable(asta);
                    }
                } else {
                    document.getElementById("asteVinteMsg").textContent = "Nessuna asta vinta";
                    document.getElementById("asteVinte").hidden = true;
                }
            } else {
                document.getElementById("asteVisitateMsg").textContent = "Errore interno al server";
                document.getElementById("asteVisitate").hidden = true;
                document.getElementById("asteVinteMsg").textContent = "Errore interno al server";
                document.getElementById("asteVinte").hidden = true;
            }
        }
    }

    request.send();
}

function addAstaVintaInTable(asta){
    const tbody = document.getElementById("asteVinteBody");

    const newRow = document.createElement("tr");
    newRow.style.textAlign = "center";

    const idAstaTd = document.createElement("td");
    idAstaTd.textContent = asta.id;
    newRow.appendChild(idAstaTd);

    const prezzoFinaleTd = document.createElement("td");
    prezzoFinaleTd.textContent = asta.prezzoOffertaMassima + " €";
    newRow.appendChild(prezzoFinaleTd);

    // Creazione lista articoli
    const articlesList = document.createElement("ul");

    asta.articoli.forEach((articolo) => {
        const articoloLi = document.createElement("li");
        articoloLi.textContent = articolo.codice + " - " + articolo.nome;
        articlesList.appendChild(articoloLi);
    });
    const articlesListElement = document.createElement("td");
    articlesListElement.appendChild(articlesList);
    newRow.appendChild(articlesListElement);

    tbody.appendChild(newRow);
}