import type {Ansatt, MedlemRespons, NomEnhetAnsatteRespons} from './api-types.js';

export type Sok =
    | {type: 'gruppe'; verdi: string}
    | {type: 'tilgangsenhet'; verdi: string}
    | {type: 'nom-enhet'; verdi: string};

export interface Gruppe {
    navn: string;
    sokenavn: string;
    medlemmer: Ansatt[];
}

export interface Oppsummering {
    grupper: string;
    antall: number;
}

export const MAKS_RADER_UTEN_RULLEFELT = 50;

export function tolkNomSok(verdi: string): Sok | undefined {
    if (/^[0-9]{4}$/.test(verdi)) return {type: 'tilgangsenhet', verdi};
    if (/^[a-z]{2}[0-9]{3}[a-z]$/.test(verdi)) return {type: 'nom-enhet', verdi};
    return undefined;
}

export function sokenavn(sok: Sok): string {
    switch (sok.type) {
        case 'gruppe': return sok.verdi;
        case 'tilgangsenhet': return `Nom tilgangsenhet ${sok.verdi}`;
        case 'nom-enhet': return `Nom-enhet ${sok.verdi}`;
    }
}

export function sokeUrl(sok: Sok): string {
    switch (sok.type) {
        case 'gruppe': return `/api/v1/gruppe/medlemmer?${new URLSearchParams({gruppeNavn: sok.verdi})}`;
        case 'tilgangsenhet': return `/api/v1/nom/ansatte/${sok.verdi}`;
        case 'nom-enhet': return `/api/v1/nom/ansatte/enhet/${sok.verdi}`;
    }
}

export function visningsnavn(sok: Sok, enhetsnavn: string | undefined): string {
    return sok.type !== 'gruppe' && enhetsnavn ? `${sokenavn(sok)} (${enhetsnavn})` : sokenavn(sok);
}

export function resultatnavn(sok: Sok, enhetsnavn: string | undefined): string {
    switch (sok.type) {
        case 'gruppe': return sok.verdi;
        case 'tilgangsenhet': return enhetsnavn ?? `tilgangsenhet ${sok.verdi}`;
        case 'nom-enhet': return enhetsnavn ? `${sok.verdi} (${enhetsnavn})` : `nom-enhet ${sok.verdi}`;
    }
}

export function medlemstekst(antall: number): string {
    return `${antall} ${antall === 1 ? 'medlem' : 'medlemmer'}`;
}

export function tolkMedlemmer(
    sok: Sok,
    resultat: MedlemRespons[] | NomEnhetAnsatteRespons
): {medlemmer: Ansatt[]; enhetsnavn?: string} {
    const liste = sok.type === 'nom-enhet' ? (Array.isArray(resultat) ? undefined : resultat?.ansatte) : resultat;
    if (!Array.isArray(liste)) {
        throw new Error('Uventet respons: forventet en liste med medlemmer.');
    }
    const medlemmer = sok.type === 'gruppe'
        ? liste
        : liste.map(ressurs => ({navIdent: ressurs?.navident, visningNavn: ressurs?.visningsnavn}));
    if (!medlemmer.every((medlem): medlem is Ansatt =>
        typeof medlem?.navIdent === 'string' && typeof medlem.visningNavn === 'string')) {
        throw new Error('Uventet respons: mangler nav-ident eller navn.');
    }
    const enhetsnavn = sok.type === 'nom-enhet' && !Array.isArray(resultat)
        ? resultat?.navn?.trim() || undefined
        : undefined;
    return {medlemmer, enhetsnavn};
}

export function beregnOppsummering(utforsket: Gruppe, sammenligninger: Gruppe[]): Oppsummering[] {
    const navIdenter = new Set(utforsket.medlemmer.map(medlem => medlem.navIdent));
    const sett = sammenligninger.map(gruppe => new Set(gruppe.medlemmer.map(medlem => medlem.navIdent)));
    const kunUtforsket = [...navIdenter].filter(navIdent => sett.every(medlemmer => !medlemmer.has(navIdent)));
    return [
        {grupper: `Kun i ${utforsket.navn}`, antall: kunUtforsket.length},
        ...sammenligninger.map((gruppe, indeks) => ({
            grupper: `${utforsket.navn} og ${gruppe.navn}`,
            antall: [...navIdenter].filter(navIdent => sett[indeks]?.has(navIdent)).length
        }))
    ];
}
