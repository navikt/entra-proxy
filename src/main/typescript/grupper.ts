import type {Ansatt, MedlemRespons, NomAnsatt, NomEnhetAnsatteRespons} from './api-types.js';
import {feilmelding, hentElement} from './dom.js';

const sok = hentElement('#sok', HTMLDivElement);
const forstePanel = hentElement('.sok-panel', HTMLElement, sok);
const panelMal = forstePanel.cloneNode(true);
if (!(panelMal instanceof HTMLElement)) throw new Error('Ugyldig panelmal');
hentElement('.nom-form', HTMLFormElement, panelMal).remove();
hentElement('.hovedsok > p', HTMLParagraphElement, panelMal).remove();
hentElement('form .sok-knapper', HTMLDivElement, panelMal).insertBefore(
    hentElement('.nytt-sok', HTMLButtonElement, panelMal),
    hentElement('.fjern-gruppe', HTMLButtonElement, panelMal)
);
hentElement('.sammenligningsknapp', HTMLDivElement, panelMal).remove();
const fellesResultat = hentElement('#felles-resultat', HTMLElement);
const sammenligningDetaljer = hentElement('#sammenligning-detaljer', HTMLElement);
const sammenligningTabell = hentElement('.sammenligning-tabell', HTMLDivElement, sammenligningDetaljer);
const fellesStatus = hentElement('#felles-status', HTMLParagraphElement);
const fellesListe = hentElement('#felles-medlemmer', HTMLTableSectionElement);
const sammenligningTabellOverskrift = hentElement('#sammenligning-tabell-overskrift', HTMLTableSectionElement);
const sammenligningTabellRader = hentElement('#sammenligning-tabell-rader', HTMLTableSectionElement);
const kopierDetaljvisning = hentElement('#kopier-detaljvisning', HTMLButtonElement);
const kopierStatus = hentElement('#kopier-status', HTMLParagraphElement);
const visUtforsketMedlemmer = hentElement('#vis-utforsket-medlemmer', HTMLInputElement);
const medlemmerBryter = hentElement('.medlemmer-bryter', HTMLLabelElement);
const utforsketMedlemmerResultat = hentElement('#utforsket-medlemmer-resultat', HTMLElement);
const utforsketMedlemmerTabell = hentElement('.sammenligning-tabell', HTMLDivElement, utforsketMedlemmerResultat);
const utforsketMedlemmerStatus = hentElement('#utforsket-medlemmer-status', HTMLParagraphElement);
const utforsketMedlemmerRader = hentElement('#utforsket-medlemmer-rader', HTMLTableSectionElement);
const utforsketMedlemmerOverskrift = hentElement('#utforsket-medlemmer-overskrift-rad', HTMLTableRowElement);
const gruppemedlemmer = new WeakMap<HTMLElement, Ansatt[]>();
let antallSok = 1;
let aktiveOppslag = 0;

function oppdaterSokemarkering(endring: number): void {
    aktiveOppslag += endring;
    document.documentElement.classList.toggle('soker', aktiveOppslag > 0);
}

kopierDetaljvisning.addEventListener('click', async () => {
    kopierDetaljvisning.disabled = true;
    kopierStatus.classList.remove('error');
    kopierStatus.textContent = 'Kopierer...';

    try {
        if (!navigator.clipboard?.writeText) {
            throw new Error('Utklippstavlen er ikke tilgjengelig. Bruk HTTPS og en nettleser som st\u00f8tter kopiering.');
        }
        const rader = [
            [...sammenligningTabellOverskrift.rows[0].cells].map(celle =>
                celle.querySelector('.sorter-knapp')?.textContent.trim() ?? celle.textContent.trim()
            ),
            ...[...sammenligningTabellRader.rows].map(rad =>
                [...rad.cells].map((celle, kolonneIndeks) => {
                    const verdi = celle.textContent.trim();
                    return kolonneIndeks >= 4 ? String(verdi === '✓') : verdi;
                })
            )
        ];
        const kolonneskille = ';';
        const tekst = rader.map(rad => rad.join(kolonneskille)).join('\r\n');
        await navigator.clipboard.writeText(tekst);
        kopierStatus.textContent = 'Tabellen er kopiert til utklippstavlen.';
    } catch (error) {
        kopierStatus.textContent = `Kopiering feilet: ${feilmelding(error)}`;
        kopierStatus.classList.add('error');
    } finally {
        kopierDetaljvisning.disabled = false;
    }
});

function oppdaterUtforsketMedlemmer() {
    const forstePopulert = forstePanel.dataset.populert === 'true';
    medlemmerBryter.hidden = !forstePopulert;
    if (!forstePopulert) {
        visUtforsketMedlemmer.checked = false;
    }
    if (!visUtforsketMedlemmer.checked) {
        utforsketMedlemmerResultat.hidden = true;
        return;
    }
    utforsketMedlemmerResultat.hidden = false;
    const medlemmer = gruppemedlemmer.get(forstePanel) ?? [];
    utforsketMedlemmerRader.replaceChildren();
    for (const knapp of utforsketMedlemmerOverskrift.querySelectorAll<HTMLButtonElement>('.sorter-knapp')) {
        knapp.dataset.sortering = '';
        knapp.closest('th')?.removeAttribute('aria-sort');
    }
    utforsketMedlemmerTabell.classList.toggle('med-rullefelt', medlemmer.length > 50);
    utforsketMedlemmerTabell.scrollTop = 0;
    if (medlemmer.length === 0) {
        utforsketMedlemmerStatus.textContent = 'Ingen medlemmer i gruppa som utforskes.';
        return;
    }
    utforsketMedlemmerStatus.textContent = `${medlemmer.length} ${medlemmer.length === 1 ? 'medlem' : 'medlemmer'}`;
    for (const medlem of medlemmer) {
        const rad = document.createElement('tr');
        const navIdentCelle = document.createElement('th');
        navIdentCelle.scope = 'row';
        const navIdentLenke = document.createElement('a');
        navIdentLenke.className = 'navident-lenke';
        navIdentLenke.href = `/ansatt.html?ansattid=${encodeURIComponent(medlem.navIdent)}`;
        navIdentLenke.textContent = medlem.navIdent;
        navIdentCelle.appendChild(navIdentLenke);
        const navnCelle = document.createElement('td');
        navnCelle.textContent = medlem.visningNavn ?? '';
        rad.append(navIdentCelle, navnCelle);
        utforsketMedlemmerRader.appendChild(rad);
    }
}

visUtforsketMedlemmer.addEventListener('change', () => {
    if (visUtforsketMedlemmer.checked) {
        for (const panel of [...sok.querySelectorAll<HTMLElement>('.sok-panel')].slice(1)) {
            panel.remove();
            gruppemedlemmer.delete(panel);
        }
        hentElement('#sammenligningsoverskrift', HTMLHeadingElement, sok).hidden = true;
    }
    oppdaterFellesMedlemmer();
});

function oppdaterFellesMedlemmer() {
    oppdaterUtforsketMedlemmer();
    const allePaneler = [...sok.querySelectorAll<HTMLElement>('.sok-panel')];
    for (const panel of allePaneler) {
        hentElement('.nytt-sok', HTMLButtonElement, panel).disabled = panel.dataset.populert !== 'true';
    }
    const alleSokFullfort = allePaneler.every(panel => panel.dataset.populert === 'true');
    const forsteMedlemmer = gruppemedlemmer.get(allePaneler[0]) ?? [];
    const sammenligningsPaneler = allePaneler.slice(1).filter(panel =>
        (gruppemedlemmer.get(panel)?.length ?? 0) > 0
    );
    const visResultat = alleSokFullfort && forsteMedlemmer.length > 0 && sammenligningsPaneler.length > 0;
    fellesResultat.hidden = !visResultat;
    sammenligningDetaljer.hidden = !visResultat;
    fellesListe.replaceChildren();
    sammenligningTabellOverskrift.replaceChildren();
    sammenligningTabellRader.replaceChildren();
    sammenligningTabell.classList.toggle('med-rullefelt', forsteMedlemmer.length > 50);
    sammenligningTabell.scrollTop = 0;
    kopierStatus.textContent = '';
    kopierStatus.classList.remove('error');
    if (fellesResultat.hidden) {
        return;
    }

    const paneler = [allePaneler[0], ...sammenligningsPaneler];
    const panelMedlemmer = [forsteMedlemmer, ...sammenligningsPaneler.map(panel => gruppemedlemmer.get(panel) ?? [])];
    const medlemssett = panelMedlemmer.map(medlemmer => new Set(medlemmer.map(medlem => medlem.navIdent)));
    const forsteGruppe = medlemssett[0];
    const andreGrupper = medlemssett.slice(1);
    const kunForsteGruppe = [...forsteGruppe].filter(navIdent =>
        andreGrupper.every(medlemmer => !medlemmer.has(navIdent))
    );
    const oppsummeringer = [
        {grupper: `Kun i ${paneler[0].dataset.gruppeNavn}`, antall: kunForsteGruppe.length},
        ...andreGrupper.map((medlemmer, indeks) => {
            const fellesAntall = [...forsteGruppe].filter(navIdent => medlemmer.has(navIdent)).length;
            return {
                grupper: `${paneler[0].dataset.gruppeNavn} og ${paneler[indeks + 1].dataset.gruppeNavn}`,
                antall: fellesAntall
            };
        })
    ];
    fellesListe.replaceChildren();
    for (const oppsummering of oppsummeringer) {
        const rad = document.createElement('tr');
        const gruppeCelle = document.createElement('td');
        const antallCelle = document.createElement('td');
        gruppeCelle.textContent = oppsummering.grupper;
        antallCelle.textContent = String(oppsummering.antall);
        rad.append(gruppeCelle, antallCelle);
        fellesListe.appendChild(rad);
    }
    fellesStatus.textContent = 'Antall medlemmer basert på gruppene over.';

    const tabellOverskrift = document.createElement('tr');
    const navIdentOverskrift = document.createElement('th');
    navIdentOverskrift.scope = 'col';
    leggTilSorterbarOverskrift(navIdentOverskrift, 'Nav-ident', 0);
    const navnOverskrift = document.createElement('th');
    navnOverskrift.scope = 'col';
    leggTilSorterbarOverskrift(navnOverskrift, 'Navn', 1);
    const tilgangsenhetOverskrift = document.createElement('th');
    tilgangsenhetOverskrift.scope = 'col';
    leggTilSorterbarOverskrift(tilgangsenhetOverskrift, 'Nom tilgangsenhet', 2);
    const tilgangsenhetNavnOverskrift = document.createElement('th');
    tilgangsenhetNavnOverskrift.scope = 'col';
    leggTilSorterbarOverskrift(tilgangsenhetNavnOverskrift, 'Nom tilgangsenhet navn', 3);
    tabellOverskrift.append(navIdentOverskrift, navnOverskrift, tilgangsenhetOverskrift, tilgangsenhetNavnOverskrift);
    for (const [indeks, panel] of paneler.slice(1).entries()) {
        const gruppeOverskrift = document.createElement('th');
        gruppeOverskrift.scope = 'col';
        leggTilSorterbarOverskrift(gruppeOverskrift, panel.dataset.gruppeNavn ?? '', indeks + 4);
        tabellOverskrift.appendChild(gruppeOverskrift);
    }
    sammenligningTabellOverskrift.appendChild(tabellOverskrift);

    const tilgangsenhetCeller = new Map<string, HTMLTableCellElement>();
    const tilgangsenhetNavnCeller = new Map<string, HTMLTableCellElement>();
    for (const medlem of panelMedlemmer[0]) {
        const rad = document.createElement('tr');
        const navIdentCelle = document.createElement('th');
        navIdentCelle.scope = 'row';
        const navIdentLenke = document.createElement('a');
        navIdentLenke.className = 'navident-lenke';
        navIdentLenke.href = `/ansatt.html?ansattid=${encodeURIComponent(medlem.navIdent)}`;
        navIdentLenke.textContent = medlem.navIdent;
        navIdentCelle.appendChild(navIdentLenke);
        const navnCelle = document.createElement('td');
        navnCelle.textContent = medlem.visningNavn ?? '';
        const tilgangsenhetCelle = document.createElement('td');
        tilgangsenhetCeller.set(medlem.navIdent, tilgangsenhetCelle);
        const tilgangsenhetNavnCelle = document.createElement('td');
        tilgangsenhetNavnCeller.set(medlem.navIdent, tilgangsenhetNavnCelle);
        rad.append(navIdentCelle, navnCelle, tilgangsenhetCelle, tilgangsenhetNavnCelle);
        for (const medlemmer of andreGrupper) {
            const celle = document.createElement('td');
            celle.textContent = medlemmer.has(medlem.navIdent) ? '✓' : '';
            rad.appendChild(celle);
        }
        sammenligningTabellRader.appendChild(rad);
    }
    hentNomTilgangsenheter(tilgangsenhetCeller, tilgangsenhetNavnCeller);
}

async function hentNomTilgangsenheter(
    celler: Map<string, HTMLTableCellElement>,
    navnCeller: Map<string, HTMLTableCellElement>
): Promise<void> {
    if (celler.size === 0) {
        return;
    }
    for (const celle of [...celler.values(), ...navnCeller.values()]) {
        celle.textContent = 'Henter...';
    }
    oppdaterSokemarkering(1);
    try {
        const response = await fetch('/api/v1/nom/enhet/bulk', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
            body: JSON.stringify([...celler.keys()])
        });
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const ressurser: NomAnsatt[] = await response.json();
        if (!Array.isArray(ressurser)) {
            throw new Error('Uventet respons fra Nom: forventet en liste med ansatte.');
        }
        const tilgangsenheterPerNavIdent = new Map(ressurser.map(ressurs => {
            if (typeof ressurs?.navident !== 'string' || !Array.isArray(ressurs.orgTilknytninger)) {
                throw new Error('Uventet respons fra Nom: mangler navident eller orgTilknytninger.');
            }
            const tilgangsenheter = ressurs.orgTilknytninger
                .map(tilknytning => tilknytning.orgEnhet.tilgangsenhetId);
            return [ressurs.navident, [...new Set(tilgangsenheter)]] as const;
        }));
        for (const [navIdent, celle] of celler) {
            celle.textContent = (tilgangsenheterPerNavIdent.get(navIdent) ?? []).join(', ');
        }
        const alleTilgangsenheter = [...new Set([...tilgangsenheterPerNavIdent.values()].flat())];
        const navnPerTilgangsenhet = await hentTilgangsenhetsnavn(alleTilgangsenheter);
        for (const [navIdent, celle] of navnCeller) {
            const ider = tilgangsenheterPerNavIdent.get(navIdent) ?? [];
            celle.textContent = ider.map(id => navnPerTilgangsenhet.get(id) ?? id).join(', ');
        }
    } catch (error) {
        for (const celle of [...celler.values(), ...navnCeller.values()]) {
            celle.textContent = 'Oppslag feilet';
            celle.title = feilmelding(error);
            celle.classList.add('error');
        }
    } finally {
        oppdaterSokemarkering(-1);
    }
}

async function hentTilgangsenhetsnavn(tilgangsenheter: (string | null)[]): Promise<Map<string | null, string | null>> {
    const oppslag = await Promise.all(tilgangsenheter.map(async id => {
        try {
            const response = await fetch(`/api/v1/navn/${encodeURIComponent(String(id))}`, {
                headers: {'Accept': 'text/plain'}
            });
            if (!response.ok) {
                return [id, id] as const;
            }
            const navn = (await response.text()).trim();
            return [id, navn || id] as const;
        } catch {
            return [id, id] as const;
        }
    }));
    return new Map(oppslag);
}

function leggTilSorterbarOverskrift(
    celle: HTMLTableCellElement,
    tekst: string,
    kolonneIndeks: number,
    overskriftContainer: HTMLTableSectionElement | HTMLTableRowElement = sammenligningTabellOverskrift,
    raderContainer: HTMLTableSectionElement = sammenligningTabellRader
): void {
    const knapp = document.createElement('button');
    knapp.className = 'sorter-knapp';
    knapp.type = 'button';
    knapp.textContent = tekst;
    knapp.addEventListener('click', () => {
        const retning = knapp.dataset.sortering === 'ascending' ? 'descending' : 'ascending';
        const andreKnapper = overskriftContainer.querySelectorAll<HTMLButtonElement>('.sorter-knapp');
        for (const annenKnapp of andreKnapper) {
            annenKnapp.dataset.sortering = '';
            annenKnapp.closest('th')?.removeAttribute('aria-sort');
        }
        knapp.dataset.sortering = retning;
        celle.setAttribute('aria-sort', retning);
        const rader = [...raderContainer.rows];
        rader.sort((radA, radB) => {
            const verdiA = radA.cells[kolonneIndeks].textContent.trim();
            const verdiB = radB.cells[kolonneIndeks].textContent.trim();
            const sammenligning = verdiA.localeCompare(verdiB, undefined, {numeric: true, sensitivity: 'base'});
            return retning === 'ascending' ? sammenligning : -sammenligning;
        });
        raderContainer.append(...rader);
    });
    celle.appendChild(knapp);
}

function byggUtforsketMedlemmerOverskrift() {
    const navIdentOverskrift = document.createElement('th');
    navIdentOverskrift.scope = 'col';
    leggTilSorterbarOverskrift(navIdentOverskrift, 'Nav-ident', 0, utforsketMedlemmerOverskrift, utforsketMedlemmerRader);
    const navnOverskrift = document.createElement('th');
    navnOverskrift.scope = 'col';
    leggTilSorterbarOverskrift(navnOverskrift, 'Navn', 1, utforsketMedlemmerOverskrift, utforsketMedlemmerRader);
    utforsketMedlemmerOverskrift.append(navIdentOverskrift, navnOverskrift);
}

byggUtforsketMedlemmerOverskrift();

function klargjorSok(panel: HTMLElement): void {
    let sisteSok = 0;
    hentElement('.nytt-sok', HTMLButtonElement, panel).addEventListener('click', () => {
        if (visUtforsketMedlemmer.checked) {
            visUtforsketMedlemmer.checked = false;
            oppdaterUtforsketMedlemmer();
        }
        const nyttPanel = panelMal.cloneNode(true);
        if (!(nyttPanel instanceof HTMLElement)) throw new Error('Ugyldig s\u00f8kepanel');
        nyttPanel.querySelector('h2')?.remove();
        antallSok += 1;
        nyttPanel.setAttribute('aria-label', `Gruppes\u00f8k ${antallSok}`);
        for (const element of nyttPanel.querySelectorAll('[id]')) {
            element.id = `${element.id}-${antallSok}`;
        }
        hentElement('label', HTMLLabelElement, nyttPanel).htmlFor = hentElement('input', HTMLInputElement, nyttPanel).id;
        hentElement('.fjern-gruppe', HTMLButtonElement, nyttPanel).hidden = false;
        sok.append(nyttPanel);
        hentElement('#sammenligningsoverskrift', HTMLHeadingElement, sok).hidden = false;
        klargjorSok(nyttPanel);
        oppdaterFellesMedlemmer();
        hentElement('input', HTMLInputElement, nyttPanel).focus();
    });

    hentElement('.fjern-gruppe', HTMLButtonElement, panel).addEventListener('click', () => {
        panel.remove();
        gruppemedlemmer.delete(panel);
        const gjenstaendePaneler = [...sok.querySelectorAll('.sok-panel')];
        hentElement('#sammenligningsoverskrift', HTMLHeadingElement, sok).hidden = gjenstaendePaneler.length <= 1;
        oppdaterFellesMedlemmer();
    });

    for (const form of panel.querySelectorAll('form')) {
        const input = hentElement('input', HTMLInputElement, form);
        const button = hentElement('button[type="submit"]', HTMLButtonElement, form);
        const status = hentElement('.status', HTMLParagraphElement, form);
        const erNomSok = form.classList.contains('nom-form');
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            if (button.disabled) {
                return;
            }
            const verdi = input.value.trim();
            const erTilgangsenhet = /^[0-9]{4}$/.test(verdi);
            const erNomEnhet = /^[a-z]{2}[0-9]{3}[a-z]$/.test(verdi);
            const sokId = ++sisteSok;
            const gruppeNavn = erNomSok ? (erNomEnhet ? `Nom-enhet ${verdi}` : `Nom tilgangsenhet ${verdi}`) : verdi;
            panel.dataset.populert = 'false';
            gruppemedlemmer.delete(panel);
            oppdaterFellesMedlemmer();
            for (const sokStatus of panel.querySelectorAll('.status')) {
                sokStatus.textContent = '';
                sokStatus.classList.remove('error');
            }

            if (erNomSok ? !(erTilgangsenhet || erNomEnhet) : !verdi) {
                status.textContent = erNomSok ? 'Skriv inn en tilgangsenhetId (4 siffer) eller en nom-enhet (6 tegn).' : 'Skriv inn et gruppenavn.';
                status.classList.add('error');
                return;
            }

            const alleredeValgt = [...sok.querySelectorAll<HTMLElement>('.sok-panel')]
                .some(annetPanel => annetPanel !== panel
                    && annetPanel.dataset.gruppeNavn
                    && annetPanel.dataset.gruppeNavn.toLowerCase() === gruppeNavn.toLowerCase());
            if (alleredeValgt) {
                status.textContent = `Gruppen "${gruppeNavn}" er allerede valgt i et annet s\u00f8k.`;
                status.classList.add('error');
                return;
            }

            for (const sokForm of panel.querySelectorAll('form')) {
                sokForm.classList.toggle('inaktivt-sok', sokForm !== form);
            }
            button.disabled = true;
            status.textContent = 'Henter medlemmer...';
            oppdaterSokemarkering(1);

            try {
                const url = erNomSok
                    ? (erNomEnhet ? `/api/v1/nom/ansatte/enhet/${verdi}` : `/api/v1/nom/ansatte/${verdi}`)
                    : `/api/v1/gruppe/medlemmer?${new URLSearchParams({gruppeNavn})}`;
                const response = await fetch(url, {
                    headers: {'Accept': 'application/json'}
                });
                if (!response.ok) {
                    let melding = `Kunne ikke hente medlemmer (HTTP ${response.status}).`;
                    if (response.headers.get('content-type')?.includes('json')) {
                        const problem: {detail?: string | null} = await response.json();
                        melding = problem.detail ?? melding;
                    }
                    throw new Error(melding);
                }

                const resultat: MedlemRespons[] | NomEnhetAnsatteRespons = await response.json();
                if (sokId !== sisteSok) {
                    return;
                }
                const ansatteListe = (erNomSok && erNomEnhet)
                    ? (Array.isArray(resultat) ? undefined : resultat?.ansatte)
                    : resultat;
                if (!Array.isArray(ansatteListe)) {
                    throw new Error('Uventet respons: forventet en liste med medlemmer.');
                }
                const medlemmer = erNomSok
                    ? ansatteListe.map(ressurs => ({navIdent: ressurs?.navident, visningNavn: ressurs?.visningsnavn}))
                    : ansatteListe;
                if (!medlemmer.every((medlem): medlem is Ansatt =>
                    typeof medlem?.navIdent === 'string' && typeof medlem.visningNavn === 'string')) {
                    throw new Error('Uventet respons: mangler nav-ident eller navn.');
                }
                let enhetsnavn;
                if (erNomSok && erTilgangsenhet) {
                    const navnResponse = await fetch(`/api/v1/navn/${verdi}`, {
                        headers: {'Accept': 'text/plain'}
                    });
                    if (!navnResponse.ok) {
                        throw new Error(`Kunne ikke hente enhetsnavn (HTTP ${navnResponse.status}).`);
                    }
                    enhetsnavn = (await navnResponse.text()).trim();
                    if (!enhetsnavn) {
                        throw new Error('Uventet respons: mangler enhetsnavn.');
                    }
                    if (sokId !== sisteSok) {
                        return;
                    }
                } else if (erNomSok && erNomEnhet) {
                    enhetsnavn = !Array.isArray(resultat) ? resultat?.navn?.trim() || undefined : undefined;
                }
                const resultatnavn = erNomSok
                    ? (erNomEnhet
                        ? (enhetsnavn ? `${verdi} (${enhetsnavn})` : `nom-enhet ${verdi}`)
                        : (enhetsnavn ?? `tilgangsenhet ${verdi}`))
                    : gruppeNavn;
                const visningsnavn = (erNomSok && enhetsnavn)
                    ? (erNomEnhet ? `Nom-enhet ${verdi} (${enhetsnavn})` : `Nom tilgangsenhet ${verdi} (${enhetsnavn})`)
                    : gruppeNavn;
                gruppemedlemmer.set(panel, medlemmer);
                panel.dataset.gruppeNavn = visningsnavn;
                panel.dataset.populert = 'true';
                status.textContent = medlemmer.length
                    ? `${medlemmer.length} ${medlemmer.length === 1 ? 'medlem' : 'medlemmer'} i ${resultatnavn}`
                    : erNomSok
                        ? `Ingen medlemmer funnet i ${resultatnavn}.`
                        : 'Ingen medlemmer funnet. Gruppen kan v\u00e6re tom eller ikke finnes.';
            } catch (error) {
                if (sokId === sisteSok) {
                    status.textContent = `Henting feilet: ${feilmelding(error)}`;
                    status.classList.add('error');
                }
            } finally {
                button.disabled = false;
                if (sokId === sisteSok) {
                    oppdaterFellesMedlemmer();
                }
                oppdaterSokemarkering(-1);
            }
        });
    }
}

klargjorSok(forstePanel);

const tilgangsenhetIdFraUrl = new URLSearchParams(window.location.search).get('tilgangsenhetId');
if (tilgangsenhetIdFraUrl) {
    hentElement('#tilgangsenhetId', HTMLInputElement).value = tilgangsenhetIdFraUrl;
    hentElement('#nom-form', HTMLFormElement).requestSubmit();
}
