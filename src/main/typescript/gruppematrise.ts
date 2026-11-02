import type {Ansatt, AnsattMedGrupper, MedlemRespons, NomAnsatt, NomEnhetAnsatteRespons} from './api-types.js';
import {feilmelding, hentElement} from './dom.js';

const nomForm = hentElement('#nom-form', HTMLFormElement);
const gruppeForm = hentElement('#gruppe-form', HTMLFormElement);
const basisResultat = hentElement('#basis-resultat', HTMLElement);
const basisRader = hentElement('#basis-rader', HTMLTableSectionElement);
const form = hentElement('#sok-form', HTMLFormElement);
const input = hentElement('#gruppeNavn', HTMLInputElement);
const status = hentElement('#status', HTMLParagraphElement);
const resultat = hentElement('#resultat', HTMLElement);
const tabellOverskrift = hentElement('#tabell-overskrift', HTMLTableSectionElement);
const tabellRader = hentElement('#tabell-rader', HTMLTableSectionElement);
const kopierKnapp = hentElement('#kopier-matrise', HTMLButtonElement);
const kopierStatus = hentElement('#kopier-status', HTMLParagraphElement);

let basisAnsatte: Ansatt[] | null = null;
let enhetsnummerPerAnsatt = new Map<string, string>();

function settStatus(melding: string, erFeil = false): void {
    status.textContent = melding;
    status.classList.toggle('error', erFeil);
}

async function hentEnhetsnummer(navidenter: string[]): Promise<void> {
    enhetsnummerPerAnsatt = new Map();
    if (navidenter.length === 0) {
        return;
    }
    const response = await fetch('/api/v1/nom/enhet/bulk', {
        method: 'POST',
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        body: JSON.stringify(navidenter),
    });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }
    const ressurser: NomAnsatt[] = await response.json();
    if (!Array.isArray(ressurser)) {
        throw new Error('Uventet respons fra Nom.');
    }
    for (const ressurs of ressurser) {
        const enheter = (ressurs?.orgTilknytninger ?? [])
            .map(tilknytning => tilknytning?.orgEnhet?.tilgangsenhetId)
            .filter(Boolean);
        enhetsnummerPerAnsatt.set(ressurs?.navident, [...new Set(enheter)].join(', '));
    }
}

function settBasisStatus(skjema: HTMLFormElement, melding: string, erFeil = false): void {
    const linje = hentElement('.status', HTMLParagraphElement, skjema);
    linje.textContent = melding;
    linje.classList.toggle('error', erFeil);
}

function lagOverskriftsknapp(tekst: string, kolonne: number): HTMLButtonElement {
    const knapp = document.createElement('button');
    knapp.type = 'button';
    knapp.className = 'sorter-knapp';
    knapp.dataset.kolonne = String(kolonne);
    knapp.textContent = tekst;
    return knapp;
}

function nullstillMatrise() {
    resultat.hidden = true;
    tabellOverskrift.replaceChildren();
    tabellRader.replaceChildren();
    settStatus('');
    kopierStatus.textContent = '';
    kopierStatus.classList.remove('error');
}

function lagNavIdentLenke(navIdent: string): HTMLAnchorElement {
    const lenke = document.createElement('a');
    lenke.className = 'navident-lenke';
    lenke.href = `/html/ansatt.html?ansattid=${encodeURIComponent(navIdent)}`;
    lenke.textContent = navIdent;
    return lenke;
}

function byggBasisTabell(ansatte: Ansatt[]): void {
    const rader = ansatte.map(ansatt => {
        const rad = document.createElement('tr');
        const identCelle = document.createElement('th');
        identCelle.scope = 'row';
        identCelle.textContent = ansatt.navIdent;
        const navnCelle = document.createElement('td');
        navnCelle.textContent = ansatt.visningNavn ?? '';
        rad.append(identCelle, navnCelle);
        return rad;
    });
    basisRader.replaceChildren(...rader);
}

function byggMatrise(data: AnsattMedGrupper[], substring: string, ansatte: Ansatt[]) {
    const sok = substring.toLowerCase();
    const medlemskapPerAnsatt = new Map(
        data.map(({ansatt, grupper}) => [
            ansatt.navIdent,
            new Set(grupper.filter(gruppe => gruppe.toLowerCase().includes(sok))),
        ])
    );

    const gruppeSett = new Set<string>();
    for (const ansatt of ansatte) {
        for (const gruppe of medlemskapPerAnsatt.get(ansatt.navIdent) ?? []) {
            gruppeSett.add(gruppe);
        }
    }
    const grupper = [...gruppeSett].sort((a, b) => a.localeCompare(b, 'nb', {sensitivity: 'base'}));

    const overskriftsrad = document.createElement('tr');
    const identOverskrift = document.createElement('th');
    identOverskrift.scope = 'col';
    identOverskrift.append(lagOverskriftsknapp('Nav-ident', 0));
    const navnOverskrift = document.createElement('th');
    navnOverskrift.scope = 'col';
    navnOverskrift.append(lagOverskriftsknapp('Navn', 1));
    const enhetOverskrift = document.createElement('th');
    enhetOverskrift.scope = 'col';
    enhetOverskrift.append(lagOverskriftsknapp('Nom-enhet', 2));
    overskriftsrad.append(identOverskrift, navnOverskrift, enhetOverskrift);
    grupper.forEach((gruppe, indeks) => {
        const celle = document.createElement('th');
        celle.scope = 'col';
        celle.append(lagOverskriftsknapp(gruppe, indeks + 3));
        overskriftsrad.append(celle);
    });
    tabellOverskrift.replaceChildren(overskriftsrad);

    const rader = ansatte.map(ansatt => {
        const medlemskap = medlemskapPerAnsatt.get(ansatt.navIdent) ?? new Set();
        const rad = document.createElement('tr');
        const identCelle = document.createElement('th');
        identCelle.scope = 'row';
        identCelle.appendChild(lagNavIdentLenke(ansatt.navIdent));
        const navnCelle = document.createElement('td');
        navnCelle.textContent = ansatt.visningNavn ?? '';
        const enhetCelle = document.createElement('td');
        enhetCelle.textContent = enhetsnummerPerAnsatt.get(ansatt.navIdent) ?? '';
        rad.append(identCelle, navnCelle, enhetCelle);
        for (const gruppe of grupper) {
            const erMedlem = medlemskap.has(gruppe);
            const celle = document.createElement('td');
            celle.textContent = erMedlem ? '✓' : '';
            if (erMedlem) {
                celle.setAttribute('aria-label', 'Medlem');
            }
            celle.classList.add(erMedlem ? 'ja' : 'nei');
            rad.append(celle);
        }
        return rad;
    });
    tabellRader.replaceChildren(...rader);

    return {antallGrupper: grupper.length, antallAnsatte: ansatte.length};
}

tabellOverskrift.addEventListener('click', (event) => {
    const knapp = event.target instanceof Element ? event.target.closest<HTMLButtonElement>('.sorter-knapp') : null;
    if (!knapp) {
        return;
    }
    const overskrift = knapp.closest('th');
    if (!overskrift) throw new Error('Sorteringsknapp mangler tabelloverskrift');
    const retning = overskrift.getAttribute('aria-sort') === 'ascending' ? 'descending' : 'ascending';
    for (const th of tabellOverskrift.querySelectorAll('th')) {
        th.removeAttribute('aria-sort');
    }
    overskrift.setAttribute('aria-sort', retning);
    const kolonne = Number(knapp.dataset.kolonne);
    const rader = [...tabellRader.rows];
    rader.sort((radA, radB) => {
        const verdiA = radA.cells[kolonne].textContent.trim();
        const verdiB = radB.cells[kolonne].textContent.trim();
        const sammenligning = verdiA.localeCompare(verdiB, 'nb', {numeric: true, sensitivity: 'base'});
        return retning === 'ascending' ? sammenligning : -sammenligning;
    });
    tabellRader.replaceChildren(...rader);
});

kopierKnapp.addEventListener('click', async () => {
    kopierKnapp.disabled = true;
    kopierStatus.classList.remove('error');
    kopierStatus.textContent = 'Kopierer...';
    try {
        if (!navigator.clipboard?.writeText) {
            throw new Error('Utklippstavlen er ikke tilgjengelig. Bruk HTTPS og en nettleser som støtter kopiering.');
        }
        const overskrifter = [...tabellOverskrift.rows[0].cells].map(celle =>
            celle.querySelector('.sorter-knapp')?.textContent.trim() ?? celle.textContent.trim()
        );
        const rader = [...tabellRader.rows].map(rad =>
            [...rad.cells].map((celle, kolonne) =>
                kolonne >= 3 ? String(celle.textContent.trim() === '✓') : celle.textContent.trim()
            )
        );
        const tekst = [overskrifter, ...rader].map(rad => rad.join(';')).join('\r\n');
        await navigator.clipboard.writeText(tekst);
        kopierStatus.textContent = 'Matrisen er kopiert til utklippstavlen.';
    } catch (error) {
        kopierStatus.textContent = `Kopiering feilet: ${feilmelding(error)}`;
        kopierStatus.classList.add('error');
    } finally {
        kopierKnapp.disabled = false;
    }
});

for (const skjema of [nomForm, gruppeForm]) {
    skjema.addEventListener('submit', async (event) => {
        event.preventDefault();
        const erNomSok = skjema.classList.contains('nom-form');
        const verdi = hentElement('input', HTMLInputElement, skjema).value.trim();
        const erTilgangsenhet = /^[0-9]{4}$/.test(verdi);
        const erNomEnhet = /^[a-z]{2}[0-9]{3}[a-z]$/.test(verdi);

        if (erNomSok ? !(erTilgangsenhet || erNomEnhet) : !verdi) {
            settBasisStatus(skjema, erNomSok
                ? 'Skriv inn en tilgangsenhetId (4 siffer) eller en nom-enhet (6 tegn).'
                : 'Skriv inn et gruppenavn.', true);
            return;
        }

        basisAnsatte = null;
        enhetsnummerPerAnsatt = new Map();
        basisResultat.hidden = true;
        basisRader.replaceChildren();
        nullstillMatrise();
        settBasisStatus(nomForm, '');
        settBasisStatus(gruppeForm, '');
        settBasisStatus(skjema, 'Henter medlemmer...');
        document.documentElement.classList.add('soker');

        try {
            const url = erNomSok
                ? (erNomEnhet ? `/api/v1/nom/ansatte/enhet/${verdi}` : `/api/v1/nom/ansatte/${verdi}`)
                : `/api/v1/gruppe/medlemmer?${new URLSearchParams({gruppeNavn: verdi})}`;
            const response = await fetch(url, {headers: {'Accept': 'application/json'}});
            if (!response.ok) {
                let melding = `Kunne ikke hente medlemmer (HTTP ${response.status}).`;
                if (response.headers.get('content-type')?.includes('json')) {
                    const problem: {detail?: string | null} = await response.json();
                    melding = problem.detail ?? melding;
                }
                throw new Error(melding);
            }

            const data: MedlemRespons[] | NomEnhetAnsatteRespons = await response.json();
            const ansatteListe = (erNomSok && erNomEnhet)
                ? (Array.isArray(data) ? undefined : data?.ansatte)
                : data;
            if (!Array.isArray(ansatteListe)) {
                throw new Error('Uventet respons: forventet en liste med medlemmer.');
            }
            const ansatte: MedlemRespons[] = erNomSok
                ? ansatteListe.map(ressurs => ({navIdent: ressurs?.navident, visningNavn: ressurs?.visningsnavn}))
                : ansatteListe.map(ressurs => ({navIdent: ressurs?.navIdent, visningNavn: ressurs?.visningNavn}));
            if (!ansatte.every((ansatt): ansatt is Ansatt => typeof ansatt.navIdent === 'string')) {
                throw new Error('Uventet respons: mangler nav-ident.');
            }

            basisAnsatte = ansatte;
            byggBasisTabell(ansatte);
            try {
                await hentEnhetsnummer(ansatte.map(ansatt => ansatt.navIdent));
            } catch {
                enhetsnummerPerAnsatt = new Map();
            }
            const enhetsnavn = (erNomSok && erNomEnhet && !Array.isArray(data)) ? data?.navn?.trim() : undefined;
            const kilde = enhetsnavn ? `${verdi} (${enhetsnavn})` : verdi;
            settBasisStatus(skjema, ansatte.length
                ? `${ansatte.length} ansatt(e) valgt fra ${kilde}. Bygg matrisen under.`
                : `Ingen medlemmer funnet i ${kilde}.`);
        } catch (error) {
            settBasisStatus(skjema, `Henting feilet: ${feilmelding(error)}`, true);
        } finally {
            document.documentElement.classList.remove('soker');
        }
    });
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const gruppeNavn = input.value.trim();
    if (!gruppeNavn) {
        return;
    }
    if (!basisAnsatte || basisAnsatte.length === 0) {
        settStatus('Velg først en gruppe som skal utforskes.', true);
        return;
    }

    document.documentElement.classList.add('soker');
    settStatus('Søker...');
    nullstillMatrise();

    try {
        const url = `/api/v1/gruppe/medlemmer/sok/per-ansatt?${new URLSearchParams({gruppeNavn})}`;
        const response = await fetch(url, {headers: {'Accept': 'application/json'}});
        if (!response.ok) {
            let melding = `Søket feilet (HTTP ${response.status}).`;
            if (response.headers.get('content-type')?.includes('json')) {
                const problem: {detail?: string | null} = await response.json();
                melding = problem.detail ?? melding;
            }
            throw new Error(melding);
        }

        const data: AnsattMedGrupper[] = await response.json();
        const {antallGrupper, antallAnsatte} = byggMatrise(Array.isArray(data) ? data : [], gruppeNavn, basisAnsatte);
        if (antallGrupper === 0) {
            settStatus(`Ingen av de ${antallAnsatte} ansatte er medlem av grupper som inneholder «${gruppeNavn}».`);
            return;
        }
        resultat.hidden = false;
        settStatus(`${antallAnsatte} ansatte og ${antallGrupper} grupper`);
    } catch (error) {
        settStatus(feilmelding(error), true);
    } finally {
        document.documentElement.classList.remove('soker');
    }
});
