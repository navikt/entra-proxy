export const FLISE_BREDDE = 720;
export const FLISE_HOYDE = 600;
export const GORILLA_BREDDE = 129;
export const GORILLA_HOYDE = 136;

// Øvre venstre hjørne til gorillaen i flisen (sentrum 600,300 skalert 0,85 som i den opprinnelige bakgrunnen)
const GORILLA_X_I_FLISE = 600 - GORILLA_BREDDE / 2;
const GORILLA_Y_I_FLISE = 300 - GORILLA_HOYDE / 2;

export type Posisjon = { x: number; y: number };
type Boks = { x: number; y: number; bredde: number; hoyde: number };

const modulo = (verdi: number, deler: number) => ((verdi % deler) + deler) % deler;

/**
 * Bakgrunnen er sentrert og gjentas som fliser. Gir sidekoordinatene til hver gorilla
 * (øvre venstre hjørne) på en side av gitt størrelse.
 */
export const gorillaPosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(gorillaBoks, sidebredde, sidehoyde);

function boksPosisjoner(boks: Boks, sidebredde: number, sidehoyde: number): Posisjon[] {
    const startX = modulo((sidebredde - FLISE_BREDDE) / 2, FLISE_BREDDE) - FLISE_BREDDE;
    const startY = modulo((sidehoyde - FLISE_HOYDE) / 2, FLISE_HOYDE) - FLISE_HOYDE;
    const posisjoner: Posisjon[] = [];
    for (let y = startY; y < sidehoyde; y += FLISE_HOYDE) {
        for (let x = startX; x < sidebredde; x += FLISE_BREDDE) {
            posisjoner.push({x: x + boks.x, y: y + boks.y});
        }
    }
    return posisjoner;
}

const erPaaBoks = (sideX: number, sideY: number, posisjon: Posisjon, boks: Boks) =>
    sideX >= posisjon.x && sideX <= posisjon.x + boks.bredde
    && sideY >= posisjon.y && sideY <= posisjon.y + boks.hoyde;

export const erPaaGorilla = (sideX: number, sideY: number, gorilla: Posisjon) =>
    erPaaBoks(sideX, sideY, gorilla, gorillaBoks);

// Løven ligger fast i bakgrunnsflisen (sentrum omtrent 131,95); boksen dekker kropp, man og hale
export const LOVE_BOKS: Boks = {x: 51, y: 37, bredde: 160, hoyde: 116};

const ELEFANT_BOKS: Boks = {x: 290, y: 40, bredde: 160, hoyde: 120};
const APE_BOKS: Boks = {x: 230, y: 170, bredde: 110, hoyde: 110};

export const apePosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(APE_BOKS, sidebredde, sidehoyde);

export const erPaaApe = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, APE_BOKS);

// Palmekronen i flisen (palmen står i 608,584 med skala 0,9); boksen dekker kokosnøttene
export const PALME_BOKS: Boks = {x: 588, y: 468, bredde: 48, hoyde: 36};

export const palmePosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(PALME_BOKS, sidebredde, sidehoyde);

export const erPaaPalme = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, PALME_BOKS);

// Sjiraffen i flisen (står i 377,303 rotert -8 grader); boksen dekker kropp, hals og hode
export const SJIRAFF_BOKS: Boks = {x: 312, y: 198, bredde: 115, hoyde: 205};

export const sjiraffPosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(SJIRAFF_BOKS, sidebredde, sidehoyde);

export const erPaaSjiraff = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, SJIRAFF_BOKS);

// Flodhesten i flisen (står i 126,478, rotert -3 grader, skala 0,82); boksen dekker kropp og hode
export const FLODHEST_BOKS: Boks = {x: 52, y: 454, bredde: 155, hoyde: 92};

export const flodhestPosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(FLODHEST_BOKS, sidebredde, sidehoyde);

export const erPaaFlodhest = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, FLODHEST_BOKS);

// Kamelen i flisen (står i 594,88, rotert -5 grader, skala 0,74); boksen dekker kropp og hode
export const KAMEL_BOKS: Boks = {x: 538, y: 38, bredde: 125, hoyde: 120};

export const kamelPosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(KAMEL_BOKS, sidebredde, sidehoyde);

export const erPaaKamel = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, KAMEL_BOKS);

// Krokodillen i flisen (står i 363,505, rotert -4 grader, skala 0,82); boksen dekker hele dyret
export const KROKODILLE_BOKS: Boks = {x: 285, y: 488, bredde: 175, hoyde: 62};

export const krokodillePosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(KROKODILLE_BOKS, sidebredde, sidehoyde);

export const erPaaKrokodille = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, KROKODILLE_BOKS);

// Sebraen i flisen (står i 140,287, rotert -10 grader, skala 0,8); boksen dekker kropp og hode
export const SEBRA_BOKS: Boks = {x: 70, y: 215, bredde: 128, hoyde: 150};

export const sebraPosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(SEBRA_BOKS, sidebredde, sidehoyde);

export const erPaaSebra = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, SEBRA_BOKS);

export const elefantPosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(ELEFANT_BOKS, sidebredde, sidehoyde);

export const erPaaElefantPosisjon = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, ELEFANT_BOKS);

const gorillaBoks: Boks = {x: GORILLA_X_I_FLISE, y: GORILLA_Y_I_FLISE, bredde: GORILLA_BREDDE, hoyde: GORILLA_HOYDE};

export const lovePosisjoner = (sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(LOVE_BOKS, sidebredde, sidehoyde);

export const erPaaLovePosisjon = (sideX: number, sideY: number, posisjon: Posisjon) =>
    erPaaBoks(sideX, sideY, posisjon, LOVE_BOKS);

export const erPaaLove = (sideX: number, sideY: number, sidebredde: number, sidehoyde: number) =>
    boksPosisjoner(LOVE_BOKS, sidebredde, sidehoyde).some(p => erPaaBoks(sideX, sideY, p, LOVE_BOKS));

export const erPaaElefant = (sideX: number, sideY: number, sidebredde: number, sidehoyde: number) =>
    elefantPosisjoner(sidebredde, sidehoyde).some(p => erPaaElefantPosisjon(sideX, sideY, p));

// «Never gonna»: to tastetrykk utenfor skjemafelt
export const RICKROLL_KODE = ['n', 'g'];

/** Gir ny fremdrift i koden etter et tastetrykk; starter på nytt ved feil tast. */
export function nesteRickrollSteg(steg: number, tast: string): number {
    const tastNavn = tast.toLowerCase();
    if (tastNavn === RICKROLL_KODE[steg]) return steg + 1;
    return tastNavn === RICKROLL_KODE[0] ? 1 : 0;
}
