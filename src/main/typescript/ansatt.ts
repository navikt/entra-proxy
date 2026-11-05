import {z} from 'zod';
import {type NomLeder, nomAnsattSchema} from './api-types.js';
import {hentElement, trygg} from './dom.js';
import {hentResponse, lesValidert} from './http.js';
import {celletekst} from './tabell.js';

const gruppeSkjema = z.object({
    rolle: z.string(),
    gruppeId: z.string().nullish(),
    beskrivelse: z.string().nullish()
});

const form = hentElement('#ansatt-form', HTMLFormElement);
const input = hentElement('#ansattid', HTMLInputElement);
const knapp = hentElement('#sok-ansatt', HTMLButtonElement);
const status = hentElement('#status', HTMLParagraphElement);
const resultat = hentElement('#ansatt-resultat', HTMLElement);
const felter = hentElement('#ansatt-felter', HTMLTableSectionElement);
const enheterResultat = hentElement('#enheter-resultat', HTMLElement);
const grupperResultat = hentElement('#grupper-resultat', HTMLElement);
const grupperTabell = hentElement('#grupper-resultat .grupper-tabell', HTMLDivElement);
const enheterRader = hentElement('#enheter-rader', HTMLTableSectionElement);
const enheterLedereKolonne = hentElement('#enheter-ledere-kolonne', HTMLTableCellElement);
const grupperRader = hentElement('#grupper-rader', HTMLTableSectionElement);
const grupperStatus = hentElement('#grupper-status', HTMLParagraphElement);
const sorterKnapper = grupperResultat.querySelectorAll<HTMLButtonElement>('.sorter-knapp');
const gruppeFilter = hentElement('#gruppe-filter', HTMLInputElement);

function oppdaterGruppeFilter(): void {
    const sokeTekst = gruppeFilter.value.trim().toLocaleLowerCase('nb');
    let synlige = 0;
    for (const rad of grupperRader.rows) {
        const gruppenavn = celletekst(rad, 0).toLocaleLowerCase('nb');
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
        if (!overskrift) throw new Error('Sorteringsknapp mangler tabelloverskrift');
        const retning = overskrift.getAttribute('aria-sort') === 'ascending' ? 'descending' : 'ascending';
        for (const knapp of sorterKnapper) {
            knapp.closest('th')?.removeAttribute('aria-sort');
        }
        overskrift.setAttribute('aria-sort', retning);
        const kolonne = Number(sorterKnapp.dataset.kolonne);
        const rader = [...grupperRader.rows];
        rader.sort((radA, radB) => {
            const verdiA = celletekst(radA, kolonne);
            const verdiB = celletekst(radB, kolonne);
            const sammenligning = verdiA.localeCompare(verdiB, 'nb', {numeric: true, sensitivity: 'base'});
            return retning === 'ascending' ? sammenligning : -sammenligning;
        });
        grupperRader.replaceChildren(...rader);
    });
}

function byggLedere(celle: HTMLTableCellElement, ledere: NomLeder[] | null | undefined): void {
    (ledere ?? []).forEach((leder, indeks) => {
        if (indeks > 0) celle.append(', ');
        const navident = leder.ressurs.navident;
        celle.append(`${leder.ressurs.visningsnavn} (`);
        const lenke = document.createElement('a');
        lenke.className = 'navident-lenke';
        lenke.href = `https://nom.nav.no/ressurs/${encodeURIComponent(navident)}`;
        lenke.target = '_blank';
        lenke.rel = 'noopener noreferrer';
        lenke.textContent = navident;
        celle.append(lenke, ')');
    });
}

form.addEventListener('submit', trygg(async event => {
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
        sorterKnapp.closest('th')?.removeAttribute('aria-sort');
    }
    status.classList.remove('error');
    knapp.disabled = true;
    document.documentElement.classList.add('soker');
    status.textContent = 'Henter ansatt...';

    try {
        const nomResponse = await hentResponse(`/api/v1/nom/enhet/${encodeURIComponent(ansattid)}`, 'Kunne ikke hente nom-data');
        const nomTekst = await nomResponse.text();
        const nomData = nomTekst.trim() ? nomAnsattSchema.nullable().parse(JSON.parse(nomTekst)) : null;
        if (nomData === null) {
            status.textContent = 'Ingen ansatt funnet.';
            return;
        }
        const flereTilknytninger = nomData.orgTilknytninger.length > 1;
        const enheter = nomData.orgTilknytninger.map(tilknytning => ({
            enhetnummer: tilknytning.orgEnhet.tilgangsenhetId,
            navn: tilknytning.orgEnhet.navn,
            nomId: tilknytning.orgEnhet.id,
            ledere: tilknytning.orgEnhet.ledere ?? []
        }));
        enheterLedereKolonne.hidden = !flereTilknytninger;
        const grupperResponse = await hentResponse(`/api/v1/ansatt/tilganger/${encodeURIComponent(ansattid)}`, 'Kunne ikke hente gruppemedlemskap');
        const grupper = await lesValidert(grupperResponse, z.array(gruppeSkjema), 'Uventet respons: forventet en liste med grupper.');
        const gruppeIder = grupper.map(gruppe => gruppe.gruppeId).filter((id): id is string => Boolean(id));
        const antallMedlemmer = gruppeIder.length
            ? await lesValidert(await hentResponse('/api/v1/gruppe/antall/bulk', 'Kunne ikke hente antall medlemmer i grupper', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(gruppeIder)
            }), z.record(z.string(), z.number()), 'Uventet respons: forventet antall medlemmer per gruppe.')
            : {};
        const verdier: [string, string | null][] = [
            ['Nav-ident', nomData.navident],
            ['Navn', nomData.visningsnavn],
            ['Sektor', nomData.gjeldendeSektor]
        ];
        if (!flereTilknytninger && nomData.orgTilknytninger.length === 1) {
            verdier.push(['N\u00e6rmeste leder', null]);
        }
        for (const [felt, verdi] of verdier) {
            const rad = document.createElement('tr');
            const feltCelle = document.createElement('th');
            feltCelle.scope = 'row';
            feltCelle.textContent = felt;
            const verdiCelle = document.createElement('td');
            if (felt === 'N\u00e6rmeste leder') {
                byggLedere(verdiCelle, enheter[0]?.ledere);
            } else {
                verdiCelle.textContent = verdi ?? '';
            }
            rad.append(feltCelle, verdiCelle);
            felter.appendChild(rad);
        }
        for (const enhet of enheter) {
            const rad = document.createElement('tr');
            const nummerCelle = document.createElement('td');
            const nummerLenke = document.createElement('a');
            nummerLenke.className = 'enhetsnummer-lenke';
            nummerLenke.href = `/html/grupper.html?tilgangsenhetId=${encodeURIComponent(String(enhet.enhetnummer))}`;
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
            byggLedere(ledereCelle, enhet.ledere);
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
            antallCelle.textContent = gruppe.gruppeId ? String(antallMedlemmer[gruppe.gruppeId] ?? '') : '';
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
        status.textContent = `Henting feilet: ${error instanceof Error ? error.message : String(error)}`;
        status.classList.add('error');
    } finally {
        knapp.disabled = false;
        document.documentElement.classList.remove('soker');
    }
}));

const ansattidFraUrl = new URLSearchParams(window.location.search).get('ansattid');
if (ansattidFraUrl) {
    input.value = ansattidFraUrl;
    form.requestSubmit();
}
