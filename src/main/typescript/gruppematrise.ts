import {type Ansatt, type AnsattMedGrupper, type MedlemRespons, type NomEnhetAnsatteRespons} from './api-types.js';
import {feilmelding, hentElement, trygg} from './dom.js';
import {hentDisablede, hentNomAnsatte} from './gruppeapi.js';
import {hentResponse, lesJson} from './http.js';
import {celletekst, lagCelle} from './tabell.js';

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

const PERMISJON_OVERSKRIFT = 'Ansatt er i permisjon';

type Oppslag<T> = {data: T} | {feil: string};

let basisAnsatte: Ansatt[] | null = null;
let enhetsnummerPerAnsatt: Oppslag<Map<string, string>> = {data: new Map()};

function settStatus(melding: string, erFeil = false): void {
    status.textContent = melding;
    status.classList.toggle('error', erFeil);
}

async function hentEnhetsnummer(navidenter: string[]): Promise<Map<string, string>> {
    const enhetsnummer = new Map<string, string>();
    if (navidenter.length === 0) {
        return enhetsnummer;
    }
    const ressurser = await hentNomAnsatte(navidenter);
    for (const ressurs of ressurser) {
        const enheter = ressurs.orgTilknytninger
            .map(tilknytning => tilknytning.orgEnhet.tilgangsenhetId)
            .filter(Boolean);
        enhetsnummer.set(ressurs.navident, [...new Set(enheter)].join(', '));
    }
    return enhetsnummer;
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

function lagFeilCelle(feil: string): HTMLTableCellElement {
    const celle = lagCelle('Oppslag feilet');
    celle.title = feil;
    celle.classList.add('error');
    return celle;
}

function byggMatrise(data: AnsattMedGrupper[], substring: string, ansatte: Ansatt[], permisjonsstatus: Oppslag<Set<string>>) {
    const sok = substring.toLowerCase();
    const medlemskapPerAnsatt = new Map(
        data.map(({ansatt, grupper}) => [
            ansatt.navIdent,
            new Set(grupper.filter(gruppe => gruppe.toLowerCase().includes(sok))),
        ])
    );
    if ('data' in permisjonsstatus) {
        for (const {navIdent} of ansatte) {
            if (permisjonsstatus.data.has(navIdent)) {
                const medlemskap = medlemskapPerAnsatt.get(navIdent) ?? new Set<string>();
                medlemskap.add(PERMISJON_OVERSKRIFT);
                medlemskapPerAnsatt.set(navIdent, medlemskap);
            }
        }
    }

    const gruppeSett = new Set<string>();
    for (const ansatt of ansatte) {
        for (const gruppe of medlemskapPerAnsatt.get(ansatt.navIdent) ?? []) {
            gruppeSett.add(gruppe);
        }
    }
    if ('feil' in permisjonsstatus) {
        gruppeSett.add(PERMISJON_OVERSKRIFT);
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
        const enhetCelle = 'feil' in enhetsnummerPerAnsatt
            ? lagFeilCelle(enhetsnummerPerAnsatt.feil)
            : lagCelle(enhetsnummerPerAnsatt.data.get(ansatt.navIdent) ?? '');
        rad.append(identCelle, navnCelle, enhetCelle);
        for (const gruppe of grupper) {
            if (gruppe === PERMISJON_OVERSKRIFT && 'feil' in permisjonsstatus) {
                rad.append(lagFeilCelle(permisjonsstatus.feil));
                continue;
            }
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
        const verdiA = celletekst(radA, kolonne);
        const verdiB = celletekst(radB, kolonne);
        const sammenligning = verdiA.localeCompare(verdiB, 'nb', {numeric: true, sensitivity: 'base'});
        return retning === 'ascending' ? sammenligning : -sammenligning;
    });
    tabellRader.replaceChildren(...rader);
});

kopierKnapp.addEventListener('click', trygg(async () => {
    kopierKnapp.disabled = true;
    kopierStatus.classList.remove('error');
    kopierStatus.textContent = 'Kopierer...';
    try {
        if (!navigator.clipboard?.writeText) {
            throw new Error('Utklippstavlen er ikke tilgjengelig. Bruk HTTPS og en nettleser som støtter kopiering.');
        }
        const overskrifter = [...tabellOverskrift.rows[0]?.cells ?? []].map(celle =>
            celle.querySelector('.sorter-knapp')?.textContent.trim() ?? celle.textContent.trim()
        );
        const rader = [...tabellRader.rows].map(rad =>
            [...rad.cells].map((celle, kolonne) => {
                if (celle.classList.contains('error')) {
                    throw new Error('Kan ikke kopiere matrisen fordi ett eller flere oppslag feilet.');
                }
                return kolonne >= 3 ? String(celle.textContent.trim() === '✓') : celle.textContent.trim();
            })
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
}));

for (const skjema of [nomForm, gruppeForm]) {
    skjema.addEventListener('submit', trygg(async (event) => {
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
        enhetsnummerPerAnsatt = {data: new Map()};
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
            const response = await hentResponse(url, 'Kunne ikke hente medlemmer');

            const data = await lesJson<MedlemRespons[] | NomEnhetAnsatteRespons>(response);
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
                enhetsnummerPerAnsatt = {data: await hentEnhetsnummer(ansatte.map(ansatt => ansatt.navIdent))};
            } catch (error) {
                enhetsnummerPerAnsatt = {feil: feilmelding(error)};
            }
            const enhetsnavn = (erNomSok && erNomEnhet && !Array.isArray(data)) ? data?.navn?.trim() : undefined;
            const kilde = enhetsnavn ? `${verdi} (${enhetsnavn})` : verdi;
            const melding = ansatte.length
                ? `${ansatte.length} ansatt(e) valgt fra ${kilde}. Bygg matrisen under.`
                : `Ingen medlemmer funnet i ${kilde}.`;
            settBasisStatus(skjema, 'feil' in enhetsnummerPerAnsatt
                ? `${melding} Kunne ikke hente Nom-enheter: ${enhetsnummerPerAnsatt.feil}`
                : melding, 'feil' in enhetsnummerPerAnsatt);
        } catch (error) {
            settBasisStatus(skjema, `Henting feilet: ${feilmelding(error)}`, true);
        } finally {
            document.documentElement.classList.remove('soker');
        }
    }));
}

form.addEventListener('submit', trygg(async (event) => {
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
        const response = await hentResponse(url, 'Søket feilet');

        const data = await lesJson<AnsattMedGrupper[]>(response);
        let permisjonsstatus: Oppslag<Set<string>>;
        try {
            permisjonsstatus = {data: await hentDisablede()};
        } catch (error) {
            permisjonsstatus = {feil: feilmelding(error)};
        }
        const {antallGrupper, antallAnsatte} = byggMatrise(Array.isArray(data) ? data : [], gruppeNavn, basisAnsatte, permisjonsstatus);
        const mangler = [
            ...('feil' in enhetsnummerPerAnsatt ? ['Nom-enheter'] : []),
            ...('feil' in permisjonsstatus ? ['permisjonsstatus'] : [])
        ];
        const advarsel = mangler.length ? ` Kunne ikke hente ${mangler.join(' og ')}.` : '';
        if (antallGrupper === 0) {
            settStatus(`Ingen av de ${antallAnsatte} ansatte er medlem av grupper som inneholder «${gruppeNavn}».${advarsel}`, mangler.length > 0);
            return;
        }
        resultat.hidden = false;
        settStatus(`${antallAnsatte} ansatte og ${antallGrupper} grupper${advarsel}`, mangler.length > 0);
    } catch (error) {
        settStatus(feilmelding(error), true);
    } finally {
        document.documentElement.classList.remove('soker');
    }
}));
