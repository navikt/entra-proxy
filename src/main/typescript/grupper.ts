import {feilmelding, hentElement, trygg} from './dom.js';
import {
    beregnOppsummering,
    type Gruppe,
    MAKS_RADER_UTEN_RULLEFELT,
    medlemstekst,
    type Sok,
    sokenavn,
    tolkNomSok
} from './gruppelogikk.js';
import {hentDisablede, hentGruppe, type HentetGruppe, hentNomAnsatte, hentTilgangsenhetsnavn} from './gruppeapi.js';
import {lagCelle, lagMedlemRad, lagSorterbarOverskrift} from './tabell.js';

const FASTE_KOLONNER = ['Nav-ident', 'Navn', 'Nom tilgangsenhet', 'Nom tilgangsenhet navn'];
const PERMISJON_OVERSKRIFT = 'Ansatt er i permisjon';

const sok = hentElement('#sok', HTMLDivElement);
const forstePanel = hentElement('.sok-panel', HTMLElement, sok);
const panelMal = hentElement('#sammenligningspanel-mal', HTMLTemplateElement);
const sammenligningsoverskrift = hentElement('#sammenligningsoverskrift', HTMLHeadingElement, sok);

const fellesResultat = hentElement('#felles-resultat', HTMLElement);
const fellesStatus = hentElement('#felles-status', HTMLParagraphElement);
const fellesListe = hentElement('#felles-medlemmer', HTMLTableSectionElement);

const sammenligningDetaljer = hentElement('#sammenligning-detaljer', HTMLElement);
const sammenligningTabell = hentElement('.sammenligning-tabell', HTMLDivElement, sammenligningDetaljer);
const sammenligningTabellOverskrift = hentElement('#sammenligning-tabell-overskrift', HTMLTableSectionElement);
const sammenligningTabellRader = hentElement('#sammenligning-tabell-rader', HTMLTableSectionElement);
const kopierDetaljvisning = hentElement('#kopier-detaljvisning', HTMLButtonElement);
const kopierStatus = hentElement('#kopier-status', HTMLParagraphElement);

interface TilgangsenhetCeller {
    enhet: HTMLTableCellElement;
    navn: HTMLTableCellElement;
    permisjon: HTMLTableCellElement;
}

// Paneler med et vellykket søk. Panel uten oppføring er enten tomt, under søk eller har feilet.
const grupper = new Map<HTMLElement, Gruppe>();
let antallSok = 1;
let aktiveOppslag = 0;
let sisteVisning = 0;

function allePaneler(): HTMLElement[] {
    return [...sok.querySelectorAll<HTMLElement>('.sok-panel')];
}

function nyttSokKnapp(panel: HTMLElement): HTMLButtonElement {
    return hentElement('.nytt-sok', HTMLButtonElement, panel);
}

function oppdaterSokemarkering(endring: number): void {
    aktiveOppslag += endring;
    document.documentElement.classList.toggle('soker', aktiveOppslag > 0);
}

function settStatus(element: HTMLElement, tekst: string, erFeil = false): void {
    element.textContent = tekst;
    element.classList.toggle('error', erFeil);
}

function detaljtabellSomTekst(): string {
    const overskrifter = [...sammenligningTabellOverskrift.rows[0]?.cells ?? []].map(celle =>
        celle.querySelector('.sorter-knapp')?.textContent.trim() ?? celle.textContent.trim()
    );
    const rader = [...sammenligningTabellRader.rows].map(rad =>
        [...rad.cells].map((celle, kolonneIndeks) => {
            const verdi = celle.textContent.trim();
            if (kolonneIndeks >= FASTE_KOLONNER.length && verdi !== '' && verdi !== '✓') {
                throw new Error('Medlemskap er ikke ferdig hentet eller oppslaget feilet.');
            }
            return kolonneIndeks >= FASTE_KOLONNER.length ? String(verdi === '✓') : verdi;
        })
    );
    return [overskrifter, ...rader].map(rad => rad.join(';')).join('\r\n');
}

kopierDetaljvisning.addEventListener('click', trygg(async () => {
    kopierDetaljvisning.disabled = true;
    settStatus(kopierStatus, 'Kopierer...');
    try {
        if (!navigator.clipboard?.writeText) {
            throw new Error('Utklippstavlen er ikke tilgjengelig. Bruk HTTPS og en nettleser som støtter kopiering.');
        }
        await navigator.clipboard.writeText(detaljtabellSomTekst());
        settStatus(kopierStatus, 'Tabellen er kopiert til utklippstavlen.');
    } catch (error) {
        settStatus(kopierStatus, `Kopiering feilet: ${feilmelding(error)}`, true);
    } finally {
        kopierDetaljvisning.disabled = false;
    }
}));

function visSammenligning(): void {
    const visningId = ++sisteVisning;
    const utforsket = grupper.get(forstePanel);
    const sammenligninger = allePaneler().slice(1)
        .flatMap(panel => grupper.get(panel) ?? [])
        .filter(gruppe => gruppe.medlemmer.length > 0);
    const sammenlignMedPermisjon = sammenligninger.length === 0;
    const visResultat = allePaneler().every(panel => grupper.has(panel))
        && utforsket !== undefined && utforsket.medlemmer.length > 0;

    fellesResultat.hidden = !visResultat;
    sammenligningDetaljer.hidden = !visResultat;
    fellesListe.replaceChildren();
    sammenligningTabellOverskrift.replaceChildren();
    sammenligningTabellRader.replaceChildren();
    sammenligningTabell.classList.toggle(
        'med-rullefelt',
        (utforsket?.medlemmer.length ?? 0) > MAKS_RADER_UTEN_RULLEFELT
    );
    sammenligningTabell.scrollTop = 0;
    settStatus(kopierStatus, '');
    if (!visResultat) {
        return;
    }

    let permisjonsantall: HTMLTableCellElement | undefined;
    if (sammenlignMedPermisjon) {
        settStatus(fellesStatus, 'Henter permisjonsstatus...');
    } else {
        visOppsummering(utforsket, sammenligninger);
        permisjonsantall = lagCelle('Henter...');
        const rad = document.createElement('tr');
        rad.append(lagCelle(`${utforsket.navn} og ${PERMISJON_OVERSKRIFT}`), permisjonsantall);
        fellesListe.append(rad);
    }
    const celler = visDetaljer(utforsket, sammenligninger);
    void hentPermisjonsstatus(celler, {utforsket, visningId, permisjonsantall});
    void hentNomTilgangsenheter(celler);
}

function visOppsummering(utforsket: Gruppe, sammenligninger: Gruppe[]): void {
    fellesListe.append(...beregnOppsummering(utforsket, sammenligninger).map(oppsummering => {
        const rad = document.createElement('tr');
        rad.append(lagCelle(oppsummering.grupper), lagCelle(String(oppsummering.antall)));
        return rad;
    }));
    settStatus(fellesStatus, 'Antall medlemmer basert på gruppene over.');
}

function visDetaljer(utforsket: Gruppe, sammenligninger: Gruppe[]): Map<string, TilgangsenhetCeller> {
    const overskrifter = [...FASTE_KOLONNER, ...sammenligninger.map(gruppe => gruppe.navn), PERMISJON_OVERSKRIFT];
    const overskriftRad = document.createElement('tr');
    overskriftRad.append(...overskrifter.map((tekst, indeks) =>
        lagSorterbarOverskrift(tekst, indeks, sammenligningTabellOverskrift, sammenligningTabellRader)
    ));
    sammenligningTabellOverskrift.appendChild(overskriftRad);

    const medlemssett = sammenligninger.map(gruppe => new Set(gruppe.medlemmer.map(medlem => medlem.navIdent)));
    const celler = new Map<string, TilgangsenhetCeller>();
    for (const medlem of utforsket.medlemmer) {
        const enhetsceller = {enhet: lagCelle(), navn: lagCelle(), permisjon: lagCelle()};
        celler.set(medlem.navIdent, enhetsceller);
        sammenligningTabellRader.appendChild(lagMedlemRad(
            medlem,
            enhetsceller.enhet,
            enhetsceller.navn,
            ...medlemssett.map(sett => lagCelle(sett.has(medlem.navIdent) ? '✓' : '')),
            enhetsceller.permisjon
        ));
    }
    return celler;
}

async function hentPermisjonsstatus(
    celler: Map<string, TilgangsenhetCeller>,
    oppsummering: {utforsket: Gruppe; visningId: number; permisjonsantall?: HTMLTableCellElement}
): Promise<void> {
    for (const {permisjon} of celler.values()) {
        permisjon.textContent = 'Henter...';
    }
    oppdaterSokemarkering(1);
    try {
        const disablede = await hentDisablede();
        for (const [navIdent, {permisjon}] of celler) {
            permisjon.textContent = disablede.has(navIdent) ? '✓' : '';
        }
        if (oppsummering.visningId === sisteVisning) {
            if (oppsummering.permisjonsantall) {
                oppsummering.permisjonsantall.textContent = String(
                    [...celler.keys()].filter(navIdent => disablede.has(navIdent)).length
                );
            } else {
                visOppsummering(oppsummering.utforsket, [{
                    navn: PERMISJON_OVERSKRIFT,
                    sokenavn: 'Alle disablede brukere',
                    medlemmer: oppsummering.utforsket.medlemmer.filter(medlem => disablede.has(medlem.navIdent))
                }]);
            }
        }
    } catch (error) {
        for (const {permisjon} of celler.values()) {
            permisjon.textContent = 'Oppslag feilet';
            permisjon.title = feilmelding(error);
            permisjon.classList.add('error');
        }
        if (oppsummering.visningId === sisteVisning) {
            if (oppsummering.permisjonsantall) {
                settStatus(oppsummering.permisjonsantall, 'Oppslag feilet', true);
                oppsummering.permisjonsantall.title = feilmelding(error);
            }
            settStatus(fellesStatus, `Kunne ikke hente permisjonsstatus: ${feilmelding(error)}`, true);
        }
    } finally {
        oppdaterSokemarkering(-1);
    }
}

async function hentNomTilgangsenheter(celler: Map<string, TilgangsenhetCeller>): Promise<void> {
    if (celler.size === 0) {
        return;
    }
    const alleCeller = [...celler.values()].flatMap(({enhet, navn}) => [enhet, navn]);
    for (const celle of alleCeller) {
        celle.textContent = 'Henter...';
    }
    oppdaterSokemarkering(1);
    try {
        const ressurser = await hentNomAnsatte([...celler.keys()]);
        const enheterPerNavIdent = new Map(ressurser.map(ressurs => {
            const enheter = ressurs.orgTilknytninger
                .map(tilknytning => tilknytning.orgEnhet.tilgangsenhetId)
                .filter((id): id is string => Boolean(id));
            return [ressurs.navident, [...new Set(enheter)]] as const;
        }));
        for (const [navIdent, {enhet}] of celler) {
            enhet.textContent = (enheterPerNavIdent.get(navIdent) ?? []).join(', ');
        }
        const navnPerEnhet = await hentTilgangsenhetsnavn([...new Set([...enheterPerNavIdent.values()].flat())]);
        for (const [navIdent, {navn}] of celler) {
            const ider = enheterPerNavIdent.get(navIdent) ?? [];
            navn.textContent = ider.map(id => navnPerEnhet.get(id) ?? id).join(', ');
        }
    } catch (error) {
        for (const celle of alleCeller) {
            celle.textContent = 'Oppslag feilet';
            celle.title = feilmelding(error);
            celle.classList.add('error');
        }
    } finally {
        oppdaterSokemarkering(-1);
    }
}

function oppdaterVisning(): void {
    for (const panel of allePaneler()) {
        nyttSokKnapp(panel).disabled = !grupper.has(panel);
    }
    visSammenligning();
}

function leggTilSammenligningspanel(): void {
    const nyttPanel = hentElement('.sok-panel', HTMLElement, panelMal.content).cloneNode(true) as HTMLElement;
    antallSok += 1;
    nyttPanel.setAttribute('aria-label', `Gruppesøk ${antallSok}`);
    for (const element of nyttPanel.querySelectorAll('[id]')) {
        element.id = `${element.id}-${antallSok}`;
    }
    const input = hentElement('input', HTMLInputElement, nyttPanel);
    hentElement('label', HTMLLabelElement, nyttPanel).htmlFor = input.id;
    sok.append(nyttPanel);
    sammenligningsoverskrift.hidden = false;
    klargjorSok(nyttPanel);
    oppdaterVisning();
    input.focus();
}

function fjernPanel(panel: HTMLElement): void {
    panel.remove();
    grupper.delete(panel);
    sammenligningsoverskrift.hidden = allePaneler().length <= 1;
    oppdaterVisning();
}

function validerSok(panel: HTMLElement, valgtSok: Sok | undefined, erNomSok: boolean): string | undefined {
    if (!valgtSok) {
        return erNomSok
            ? 'Skriv inn en tilgangsenhetId (4 siffer) eller en nom-enhet (6 tegn).'
            : 'Skriv inn et gruppenavn.';
    }
    const navn = sokenavn(valgtSok);
    const alleredeValgt = [...grupper].some(([annetPanel, gruppe]) =>
        annetPanel !== panel && gruppe.sokenavn.toLowerCase() === navn.toLowerCase()
    );
    return alleredeValgt ? `Gruppen "${navn}" er allerede valgt i et annet søk.` : undefined;
}

function sokestatus({gruppe, resultatnavn}: HentetGruppe, erNomSok: boolean): string {
    if (gruppe.medlemmer.length) {
        return `${medlemstekst(gruppe.medlemmer.length)} i ${resultatnavn}`;
    }
    return erNomSok
        ? `Ingen medlemmer funnet i ${resultatnavn}.`
        : 'Ingen medlemmer funnet. Gruppen kan være tom eller ikke finnes.';
}

function klargjorSok(panel: HTMLElement): void {
    let sisteSok = 0;
    nyttSokKnapp(panel).addEventListener('click', leggTilSammenligningspanel);
    panel.querySelector('.fjern-gruppe')?.addEventListener('click', () => fjernPanel(panel));

    for (const form of panel.querySelectorAll('form')) {
        const input = hentElement('input', HTMLInputElement, form);
        const knapp = hentElement('button[type="submit"]', HTMLButtonElement, form);
        const status = hentElement('.status', HTMLParagraphElement, form);
        const erNomSok = form.classList.contains('nom-form');

        form.addEventListener('submit', trygg(async event => {
            event.preventDefault();
            if (knapp.disabled) {
                return;
            }
            const verdi = input.value.trim();
            const valgtSok: Sok | undefined = erNomSok
                ? tolkNomSok(verdi)
                : verdi ? {type: 'gruppe', verdi} : undefined;
            const sokId = ++sisteSok;
            const erGjeldende = () => sokId === sisteSok;

            grupper.delete(panel);
            oppdaterVisning();
            for (const sokStatus of panel.querySelectorAll<HTMLElement>('.status')) {
                settStatus(sokStatus, '');
            }
            const feil = validerSok(panel, valgtSok, erNomSok);
            if (feil || !valgtSok) {
                settStatus(status, feil ?? '', true);
                return;
            }

            for (const sokForm of panel.querySelectorAll('form')) {
                sokForm.classList.toggle('inaktivt-sok', sokForm !== form);
            }
            knapp.disabled = true;
            status.textContent = 'Henter medlemmer...';
            oppdaterSokemarkering(1);
            try {
                const hentet = await hentGruppe(valgtSok, erGjeldende);
                if (hentet) {
                    grupper.set(panel, hentet.gruppe);
                    status.textContent = sokestatus(hentet, erNomSok);
                }
            } catch (error) {
                if (erGjeldende()) {
                    settStatus(status, `Henting feilet: ${feilmelding(error)}`, true);
                }
            } finally {
                knapp.disabled = false;
                if (erGjeldende()) {
                    oppdaterVisning();
                }
                oppdaterSokemarkering(-1);
            }
        }));
    }
}

klargjorSok(forstePanel);

const tilgangsenhetIdFraUrl = new URLSearchParams(window.location.search).get('tilgangsenhetId');
if (tilgangsenhetIdFraUrl) {
    hentElement('#tilgangsenhetId', HTMLInputElement).value = tilgangsenhetIdFraUrl;
    hentElement('#nom-form', HTMLFormElement).requestSubmit();
}
