export const FLISE_BREDDE = 720;
export const FLISE_HOYDE = 600;

// Gorillaen i wildlife.svg: sentrum (600, 300), omtrent 70 px bred og 60 px høy fra sentrum.
const GORILLA_SENTRUM_X = 600;
const GORILLA_SENTRUM_Y = 300;
const GORILLA_HALV_BREDDE = 70;
const GORILLA_HALV_HOYDE = 60;

const modulo = (verdi: number, deler: number) => ((verdi % deler) + deler) % deler;

/**
 * Bakgrunnen er sentrert og gjentas som fliser. Finner ut om en posisjon på siden
 * (sidekoordinater) ligger over en gorilla.
 */
export function erPaaGorilla(sideX: number, sideY: number, sidebredde: number, sidehoyde: number): boolean {
    const x = modulo(sideX - (sidebredde - FLISE_BREDDE) / 2, FLISE_BREDDE);
    const y = modulo(sideY - (sidehoyde - FLISE_HOYDE) / 2, FLISE_HOYDE);
    return Math.abs(x - GORILLA_SENTRUM_X) <= GORILLA_HALV_BREDDE
        && Math.abs(y - GORILLA_SENTRUM_Y) <= GORILLA_HALV_HOYDE;
}
