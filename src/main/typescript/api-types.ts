import {z} from 'zod';

export interface Ansatt {
    navIdent: string;
    visningNavn?: string | null;
}

export interface MedlemRespons {
    navIdent?: string;
    visningNavn?: string | null;
    navident?: string;
    visningsnavn?: string | null;
}

export interface NomEnhetAnsatteRespons {
    ansatte: MedlemRespons[];
    navn?: string | null;
}

export interface AnsattMedGrupper {
    ansatt: Ansatt;
    grupper: string[];
}

const nomLederSchema = z.object({
    ressurs: z.object({
        navident: z.string(),
        visningsnavn: z.string()
    })
});

export const nomAnsattSchema = z.object({
    navident: z.string(),
    visningsnavn: z.string(),
    gjeldendeSektor: z.string(),
    orgTilknytninger: z.array(z.object({
        orgEnhet: z.object({
            tilgangsenhetId: z.string().nullable(),
            navn: z.string(),
            id: z.string(),
            ledere: z.array(nomLederSchema).nullish()
        })
    }))
});

// Bulk-oppslag brukes bare til å finne tilgangsenheter, så bare disse feltene kreves
export const nomBulkAnsattSchema = z.object({
    navident: z.string(),
    orgTilknytninger: z.array(z.object({
        orgEnhet: z.object({tilgangsenhetId: z.string().nullish()})
    }))
});

export type NomBulkAnsatt = z.infer<typeof nomBulkAnsattSchema>;
export type NomLeder = z.infer<typeof nomLederSchema>;
export type NomAnsatt = z.infer<typeof nomAnsattSchema>;
