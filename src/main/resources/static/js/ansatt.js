    const form = document.getElementById('ansatt-form');
    const input = document.getElementById('ansattid');
    const knapp = document.getElementById('sok-ansatt');
    const status = document.getElementById('status');
    const resultat = document.getElementById('ansatt-resultat');
    const felter = document.getElementById('ansatt-felter');
    const enheterResultat = document.getElementById('enheter-resultat');
    const grupperResultat = document.getElementById('grupper-resultat');
    const grupperTabell = grupperResultat.querySelector('.grupper-tabell');
    const enheterRader = document.getElementById('enheter-rader');
    const enheterLedereKolonne = document.getElementById('enheter-ledere-kolonne');
    const grupperRader = document.getElementById('grupper-rader');
    const grupperStatus = document.getElementById('grupper-status');
    const sorterKnapper = grupperResultat.querySelectorAll('.sorter-knapp');
    const gruppeFilter = document.getElementById('gruppe-filter');

    function oppdaterGruppeFilter() {
        const sokeTekst = gruppeFilter.value.trim().toLocaleLowerCase('nb');
        let synlige = 0;
        for (const rad of grupperRader.rows) {
            const gruppenavn = rad.cells[0].textContent.trim().toLocaleLowerCase('nb');
            rad.hidden = !gruppenavn.includes(sokeTekst);
            if (!rad.hidden) synlige++;
        }
        const totalt = grupperRader.rows.length;
        if (totalt === 0) {
            grupperStatus.textContent = 'Ingen gruppemedlemskap';
            return;
        }
        const antall = sokeTekst ? `${synlige} av ${totalt}` : `${totalt}`;
        grupperStatus.textContent = `${antall} ${totalt === 1 ? 'gruppe' : 'grupper'}`;
    }

    gruppeFilter.addEventListener('input', oppdaterGruppeFilter);

    for (const sorterKnapp of sorterKnapper) {
        sorterKnapp.addEventListener('click', () => {
            const overskrift = sorterKnapp.closest('th');
            const retning = overskrift.getAttribute('aria-sort') === 'ascending' ? 'descending' : 'ascending';
            for (const knapp of sorterKnapper) {
                knapp.closest('th').removeAttribute('aria-sort');
            }
            overskrift.setAttribute('aria-sort', retning);
            const kolonne = Number(sorterKnapp.dataset.kolonne);
            const rader = [...grupperRader.rows];
            rader.sort((radA, radB) => {
                const verdiA = radA.cells[kolonne].textContent.trim();
                const verdiB = radB.cells[kolonne].textContent.trim();
                const sammenligning = verdiA.localeCompare(verdiB, 'nb', {numeric: true, sensitivity: 'base'});
                return retning === 'ascending' ? sammenligning : -sammenligning;
            });
            grupperRader.replaceChildren(...rader);
        });
    }

    async function hentResponse(url, beskrivelse, innstillinger = {}) {
        const response = await fetch(url, {
            ...innstillinger,
            headers: {'Accept': 'application/json', ...innstillinger.headers}
        });
        if (!response.ok) {
            let melding = `Kunne ikke hente ${beskrivelse} (HTTP ${response.status}).`;
            if (response.headers.get('content-type')?.includes('json')) {
                const problem = await response.json();
                melding = problem.detail ?? melding;
            }
            throw new Error(melding);
        }
        return response;
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const ansattid = input.value.trim().toUpperCase();
        resultat.hidden = true;
        enheterResultat.hidden = true;
        grupperResultat.hidden = true;
        felter.replaceChildren();
        enheterRader.replaceChildren();
        grupperRader.replaceChildren();
        grupperTabell.classList.remove('med-rullefelt');
        grupperTabell.scrollTop = 0;
        for (const sorterKnapp of sorterKnapper) {
            sorterKnapp.closest('th').removeAttribute('aria-sort');
        }
        status.classList.remove('error');
        knapp.disabled = true;
        document.documentElement.classList.add('soker');
        status.textContent = 'Henter ansatt...';

        try {
            const nomResponse = await hentResponse(`/api/v1/nom/enhet/${encodeURIComponent(ansattid)}`, 'nom-data');
            const nomTekst = await nomResponse.text();
            const nomData = nomTekst.trim() ? JSON.parse(nomTekst) : null;
            if (nomData === null) {
                status.textContent = 'Ingen ansatt funnet.';
                return;
            }
            const formaterLedere = ledere => (ledere ?? [])
                .map(leder => `${leder.ressurs.visningsnavn} (${leder.ressurs.navident})`)
                .join(', ');
            const flereTilknytninger = nomData.orgTilknytninger.length > 1;
            const enheter = nomData.orgTilknytninger.map(tilknytning => ({
                enhetnummer: tilknytning.orgEnhet.tilgangsenhetId,
                navn: tilknytning.orgEnhet.navn,
                nomId: tilknytning.orgEnhet.id,
                ledere: formaterLedere(tilknytning.orgEnhet.ledere)
            }));
            enheterLedereKolonne.hidden = !flereTilknytninger;
            const grupperResponse = await hentResponse(`/api/v1/ansatt/tilganger/${encodeURIComponent(ansattid)}`, 'gruppemedlemskap');
            const grupper = await grupperResponse.json();
            const gruppeIder = grupper.map(gruppe => gruppe.gruppeId).filter(Boolean);
            const antallMedlemmer = gruppeIder.length
                ? await (await hentResponse('/api/v1/gruppe/antall/bulk', 'antall medlemmer i grupper', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(gruppeIder)
                })).json()
                : {};
            const verdier = [
                ['Nav-ident', nomData.navident],
                ['Navn', nomData.visningsnavn],
                ['Sektor', nomData.gjeldendeSektor]
            ];
            if (!flereTilknytninger && nomData.orgTilknytninger.length === 1) {
                verdier.push(['N\u00e6rmeste leder', enheter[0].ledere]);
            }
            for (const [felt, verdi] of verdier) {
                const rad = document.createElement('tr');
                const feltCelle = document.createElement('th');
                feltCelle.scope = 'row';
                feltCelle.textContent = felt;
                const verdiCelle = document.createElement('td');
                verdiCelle.textContent = verdi ?? '';
                rad.append(feltCelle, verdiCelle);
                felter.appendChild(rad);
            }
            for (const enhet of enheter) {
                const rad = document.createElement('tr');
                const nummerCelle = document.createElement('td');
                const nummerLenke = document.createElement('a');
                nummerLenke.className = 'enhetsnummer-lenke';
                nummerLenke.href = `/grupper.html?tilgangsenhetId=${encodeURIComponent(enhet.enhetnummer)}`;
                nummerLenke.textContent = enhet.enhetnummer;
                nummerCelle.appendChild(nummerLenke);
                const navnCelle = document.createElement('td');
                navnCelle.textContent = enhet.navn;
                const nomIdCelle = document.createElement('td');
                if (enhet.nomId) {
                    const lenke = document.createElement('a');
                    lenke.href = `https://nom.nav.no/org/${encodeURIComponent(enhet.nomId)}`;
                    lenke.target = '_blank';
                    lenke.rel = 'noopener noreferrer';
                    lenke.textContent = enhet.nomId;
                    nomIdCelle.append(lenke);
                }
                const ledereCelle = document.createElement('td');
                ledereCelle.textContent = enhet.ledere;
                ledereCelle.hidden = !flereTilknytninger;
                rad.append(nummerCelle, navnCelle, nomIdCelle, ledereCelle);
                enheterRader.appendChild(rad);
            }
            for (const gruppe of grupper) {
                const rad = document.createElement('tr');
                const celle = document.createElement('td');
                celle.textContent = gruppe.rolle;
                const gruppeIdCelle = document.createElement('td');
                gruppeIdCelle.textContent = gruppe.gruppeId ?? '';
                gruppeIdCelle.hidden = true;
                const beskrivelseCelle = document.createElement('td');
                beskrivelseCelle.textContent = gruppe.beskrivelse ?? '';
                beskrivelseCelle.hidden = true;
                const antallCelle = document.createElement('td');
                antallCelle.textContent = antallMedlemmer[gruppe.gruppeId] ?? '';
                rad.append(celle, gruppeIdCelle, beskrivelseCelle, antallCelle);
                grupperRader.appendChild(rad);
            }
            oppdaterGruppeFilter();
            grupperTabell.classList.toggle('med-rullefelt', grupper.length > 50);
            resultat.hidden = false;
            enheterResultat.hidden = false;
            grupperResultat.hidden = false;
            status.textContent = 'Ansatt hentet.';
        } catch (error) {
            status.textContent = `Henting feilet: ${error.message}`;
            status.classList.add('error');
        } finally {
            knapp.disabled = false;
            document.documentElement.classList.remove('soker');
        }
    });

    const ansattidFraUrl = new URLSearchParams(window.location.search).get('ansattid');
    if (ansattidFraUrl) {
        input.value = ansattidFraUrl;
        form.requestSubmit();
    }
