// import { renderOffertePage } from "./offerte.js";

export function renderAcquistoPage(){
    document.getElementById("acquistoPage").removeAttribute("hidden");

    document.getElementById("cercaPerParolaChiave").addEventListener("click", () => {
        searchAstaByKeyword();
    });

    renderAsteVisionateEAggiudicate();
}

function searchAstaByKeyword(){
    const keyword = document.getElementById("keyword").value;

    if(!keyword){
        document.getElementById("asteTrovateMsg").textContent = "Parola chiave mancante";
        return;
    }

    document.getElementById("asteTrovateMsg").textContent = "";
    document.getElementById("asteTrovate").hidden = true;

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

            const aste = JSON.parse(request.responseText);

            if(aste.length > 0){
                document.getElementById("asteTrovate").removeAttribute("hidden");
                aste.forEach((asta) => {
                    addAstaTrovataInTable(asta, "asteTrovateBody");
                });
            } else{
                document.getElementById("asteTrovateMsg").textContent = "Nessuna asta trovata";
            }
        } else{
            document.getElementById("asteTrovateMsg").textContent = "Errore interno al server";
        }
    }
}

function addAstaTrovataInTable(asta, tableBodyId){
    const tbody = document.getElementById(tableBodyId);

    const template = document.getElementById("asteTrovateTemplate");
    const newRow = template.content.cloneNode(true);

    let idAstaElement = document.createElement("td");
    idAstaElement.textContent = asta.id;
    /*
    idAstaElement.addEventListener("click", () => {
        renderOffertePage(asta.id);
    });
     */
    newRow.appendChild(idAstaElement);

    let prezzoInizialeElement = document.createElement("td");
    prezzoInizialeElement.textContent = asta.prezzoIniziale;
    newRow.appendChild(prezzoInizialeElement);

    let rialzoMinimoElement = document.createElement("td");
    rialzoMinimoElement.textContent = asta.rialzoMinimo;
    newRow.appendChild(rialzoMinimoElement);

    let dataScadenzaElement = document.createElement("td");
    dataScadenzaElement.textContent = asta.scadenza;
    newRow.appendChild(dataScadenzaElement);

    let proprietarioElement = document.createElement("td");
    proprietarioElement.textContent = asta.proprietario;
    newRow.appendChild(proprietarioElement);

    // Creazione tabella articoli
    let articlesTable = document.createElement("table");

    asta.articoli.forEach((articolo) => {
        let tr = document.createElement("tr");

        let codiceTd = document.createElement("td");
        codiceTd.textContent = articolo.codice;
        tr.appendChild(codiceTd);

        let nomeTd = document.createElement("td");
        nomeTd.textContent = articolo.nome;
        tr.appendChild(nomeTd);

        articlesTable.appendChild(tr);
    });

    let articlesTableTd = document.createElement("td");
    articlesTableTd.appendChild(articlesTable);
    newRow.appendChild(articlesTableTd);

    // Inserisco la nuova riga nella tabella delle aste aperte
    tbody.appendChild(newRow);
}

function renderAsteVisionateEAggiudicate(){
    // Richiedi al server le aste visionate (la servlet analizzerà la lista di cookie e restituirà la lista delle rispettive aste)
    let request = new XMLHttpRequest();
    request.open("GET", "AcquistoServlet");

    request.onreadystatechange = () => {
        if (request.readyState === 4) {
            if (request.status === 200) {
                // Se è presente del contenuto nella risposta => sono le aste visitate da mostrare
                document.getElementById("asteVisitateBody").innerHTML = '';	// Svuoto la tabella precedente per far spazio ai dati aggiornati
                document.getElementById("asteVinteBody").innerHTML = '';

                const jsonResponse = JSON.parse(request.responseText);

                // Mostro le aste visionate
                const asteVisitate = jsonResponse.asteVisitate;

                if (asteVisitate != null && asteVisitate.length > 0) {
                    document.getElementById("asteVisitate").removeAttribute("hidden");
                    document.getElementById("asteVisitateMsg").innerHTML = '';

                    for (const asta of asteVisitate) {
                        addAstaTrovataInTable(asta, "asteVisitateBody");
                    }
                } else {
                    document.getElementById("asteVisitateMsg").textContent = "Nessuna asta visitata";
                }

                const asteVinte = jsonResponse.asteVinte;
                if (asteVinte != null && asteVinte.length > 0) {
                    document.getElementById("asteVinte").removeAttribute("hidden");
                    document.getElementById("asteVinteMsg").innerHTML = '';

                    for (const astaVinta of asteVinte) {
                        addAstaVintaInTable(astaCustom);
                    }
                } else {
                    document.getElementById("asteVinteMsg").textContent = "Nessuna asta vinta";
                }
            } else {
                document.getElementById("asteVisitateMsg").textContent = "Errore interno al server";
            }
        }
    }

    request.send();
}

function addAstaVintaInTable(asta){
    const tbody = document.getElementById("asteVinteBody");

    const newRow = document.createElement("tr");

    let idAstaElement = document.createElement("td");
    idAstaElement.textContent = asta.id;
    newRow.appendChild(idAstaElement);

    let prezzoFinaleElement = document.createElement("td");
    prezzoFinaleElement.textContent = asta.prezzoOffertaMassima;
    newRow.appendChild(prezzoFinaleElement);

    // Creazione tabella articoli
    let articlesTable = document.createElement("table");

    asta.articoli.forEach((articolo) => {
        let tr = document.createElement("tr");

        let codiceTd = document.createElement("td");
        codiceTd.textContent = articolo.codice;
        tr.appendChild(codiceTd);

        let nomeTd = document.createElement("td");
        nomeTd.textContent = articolo.nome;
        tr.appendChild(nomeTd);

        articlesTable.appendChild(tr);
    });
    let articlesTableElement = document.createElement("td");
    articlesTableElement.appendChild(articlesTable);
    newRow.appendChild(articlesTableElement);

    tbody.appendChild(newRow);
}