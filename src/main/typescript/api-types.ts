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

export interface NomLeder {
    ressurs: {
        navident: string;
        visningsnavn: string;
    };
}

export interface NomAnsatt {
    navident: string;
    visningsnavn: string;
    gjeldendeSektor: string;
    orgTilknytninger: {
        orgEnhet: {
            tilgangsenhetId: string | null;
            navn: string;
            id: string;
            ledere?: NomLeder[] | null;
        };
    }[];
}
