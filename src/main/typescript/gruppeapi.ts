import {z} from 'zod';
import {type MedlemRespons, type NomBulkAnsatt, nomBulkAnsattSchema, type NomEnhetAnsatteRespons} from './api-types.js';
import {hentJson, hentTekst, lesValidert} from './http.js';
import {type Gruppe, resultatnavn, type Sok, sokenavn, sokeUrl, tolkMedlemmer, visningsnavn} from './gruppelogikk.js';

export async function hentNomAnsatte(navIdenter: string[]): Promise<NomBulkAnsatt[]> {
    const response = await fetch('/api/v1/nom/enhet/bulk', {
        method: 'POST',
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        body: JSON.stringify(navIdenter)
    });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }
    return await lesValidert(response, z.array(nomBulkAnsattSchema), 'Uventet respons fra Nom: forventet en liste med ansatte med navident og orgTilknytninger.');
}

export async function hentTilgangsenhetsnavn(tilgangsenheter: string[]): Promise<Map<string, string>> {
    const oppslag = await Promise.all(tilgangsenheter.map(async id => {
        const navn = await hentTekst(`/api/v1/navn/${encodeURIComponent(id)}`, 'Kunne ikke hente enhetsnavn')
            .catch(() => '');
        return [id, navn || id] as const;
    }));
    return new Map(oppslag);
}

export interface HentetGruppe {
    gruppe: Gruppe;
    resultatnavn: string;
}

export async function hentGruppe(valgtSok: Sok, erGjeldende: () => boolean): Promise<HentetGruppe | undefined> {
    const resultat = await hentJson<MedlemRespons[] | NomEnhetAnsatteRespons>(
        sokeUrl(valgtSok),
        'Kunne ikke hente medlemmer'
    );
    if (!erGjeldende()) {
        return undefined;
    }
    const {medlemmer, enhetsnavn: enhetsnavnFraRespons} = tolkMedlemmer(valgtSok, resultat);
    let enhetsnavn = enhetsnavnFraRespons;
    if (valgtSok.type === 'tilgangsenhet') {
        enhetsnavn = await hentTekst(`/api/v1/navn/${valgtSok.verdi}`, 'Kunne ikke hente enhetsnavn');
        if (!enhetsnavn) {
            throw new Error('Uventet respons: mangler enhetsnavn.');
        }
        if (!erGjeldende()) {
            return undefined;
        }
    }
    return {
        gruppe: {navn: visningsnavn(valgtSok, enhetsnavn), sokenavn: sokenavn(valgtSok), medlemmer},
        resultatnavn: resultatnavn(valgtSok, enhetsnavn)
    };
}
