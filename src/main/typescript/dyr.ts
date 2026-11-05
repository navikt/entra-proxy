import {nesteRickrollSteg, RICKROLL_KODE, erPaaGorilla, GORILLA_BREDDE, GORILLA_HOYDE, gorillaPosisjoner, erPaaLove, erPaaElefantPosisjon, elefantPosisjoner, apePosisjoner, erPaaApe, lovePosisjoner, erPaaLovePosisjon, LOVE_BOKS, sebraPosisjoner, erPaaSebra, SEBRA_BOKS, krokodillePosisjoner, erPaaKrokodille, KROKODILLE_BOKS, kamelPosisjoner, erPaaKamel, KAMEL_BOKS, flodhestPosisjoner, erPaaFlodhest, FLODHEST_BOKS, sjiraffPosisjoner, erPaaSjiraff, SJIRAFF_BOKS, palmePosisjoner, erPaaPalme, PALME_BOKS, type Posisjon} from './dyrlogikk.js';
import {bukk, fellKokosnotter, gjesp, hopp, kastBanan, kastMedHodet, loftSnabel, snapp, spisBanan, spytt, viftMedHalen} from './dyranimasjon.js';

const svgElement = <K extends keyof SVGElementTagNameMap>(tag: K) =>
    document.createElementNS('http://www.w3.org/2000/svg', tag);

const lag = document.createElement('div');
lag.setAttribute('aria-hidden', 'true');
// z-index -1 legger gorillaene over bakgrunnen, men bak sidens innhold
lag.style.cssText = 'position:absolute;top:0;left:0;pointer-events:none;overflow:hidden;z-index:-1';
document.body.append(lag);

let gorillaer: { posisjon: Posisjon; bilde: HTMLImageElement }[] = [];
let elefanter: { posisjon: Posisjon; snabel: SVGUseElement }[] = [];
let aper: { posisjon: Posisjon; banan: SVGUseElement }[] = [];
let lover: { posisjon: Posisjon; hale: SVGUseElement }[] = [];
let sjiraffer: { posisjon: Posisjon; hals: SVGUseElement }[] = [];
let flodhester: { posisjon: Posisjon; munn: SVGUseElement; kjeve: SVGUseElement }[] = [];
let kamler: { posisjon: Posisjon; hode: SVGUseElement; spytt: SVGCircleElement }[] = [];
let krokodiller: { posisjon: Posisjon; kjeve: SVGUseElement }[] = [];
let sebraer: { posisjon: Posisjon; hode: SVGUseElement }[] = [];
let palmer: { posisjon: Posisjon; kokosnotter: SVGUseElement[] }[] = [];

// Løven står i 131,87 i flisen, rotert -8 grader og skalert 0,78; halen sitter fast i kroppen i 55,40 (lokale koordinater)
const LOVE_ROT = {x: 131, y: 87};

// Sjiraffen står i 377,303 i flisen, rotert -8 grader; halsen er festet i 30,10 (lokale koordinater)
const SJIRAFF_ROT = {x: 377, y: 303};

// Flodhesten står i 126,478 i flisen, rotert -3 grader og skalert 0,82; kjeven henger i 45,38 (lokale koordinater)
const FLODHEST_ROT = {x: 126, y: 478};

// Kamelen står i 594,88 i flisen, rotert -5 grader og skalert 0,74; halsen er festet i 45,15 (lokale koordinater)
const KAMEL_ROT = {x: 594, y: 88};

// Krokodillen står i 363,505 i flisen, rotert -4 grader og skalert 0,82; overkjeven henger i 58,8 (lokale koordinater)
const KROKODILLE_ROT = {x: 363, y: 505};

// Sebraen står i 140,287 i flisen, rotert -10 grader og skalert 0,8; halsen er festet i 20,15 (lokale koordinater)
const SEBRA_ROT = {x: 140, y: 287};

const KOKOSNOTTER = ['palm-coconut-1', 'palm-coconut-2', 'palm-coconut-3'];
// Palmens rot i flisen og skalaen, brukt til å plassere kokosnøttene over bakgrunnen
const PALME_ROT = {x: 608, y: 584, skala: .9};

function plasserDyr() {
    const rot = document.documentElement;
    const bredde = rot.clientWidth;
    const hoyde = rot.getBoundingClientRect().height;
    lag.style.width = `${bredde}px`;
    lag.style.height = `${Math.max(hoyde, rot.scrollHeight)}px`;
    lag.replaceChildren();
    gorillaer = gorillaPosisjoner(bredde, hoyde).map(posisjon => {
        const bilde = document.createElement('img');
        bilde.src = '/images/gorilla.svg';
        bilde.alt = '';
        bilde.width = GORILLA_BREDDE;
        bilde.height = GORILLA_HOYDE;
        bilde.style.cssText = `position:absolute;left:${posisjon.x}px;top:${posisjon.y}px;transform-origin:50% 100%`;
        lag.append(bilde);
        return {posisjon, bilde};
    });
    elefanter = elefantPosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -45 210 140');
        svg.setAttribute('width', '210');
        svg.setAttribute('height', '140');
        svg.style.cssText = `position:absolute;left:${posisjon.x - 20}px;top:${posisjon.y - 9}px;overflow:visible;transform:rotate(8deg) scale(.78);transform-origin:100px 45px`;
        const kropp = svgElement('use');
        kropp.setAttribute('href', '/images/wildlife.svg#elephant-body');
        const snabel = svgElement('use');
        snabel.setAttribute('href', '/images/wildlife.svg#elephant-trunk');
        snabel.style.cssText = 'transform-box:view-box;transform-origin:72px 8px';
        const detaljer = svgElement('use');
        detaljer.setAttribute('href', '/images/wildlife.svg#elephant-details');
        svg.append(kropp, snabel, detaljer);
        lag.append(svg);
        return {posisjon, snabel};
    });
    aper = apePosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '0 0 110 110');
        svg.setAttribute('width', '110');
        svg.setAttribute('height', '110');
        svg.style.cssText = `position:absolute;left:${posisjon.x}px;top:${posisjon.y}px;overflow:visible`;
        const kropp = svgElement('use');
        kropp.setAttribute('href', '/images/wildlife.svg#monkey');
        const banan = svgElement('use');
        banan.setAttribute('href', '/images/wildlife.svg#monkey-banana');
        banan.style.cssText = 'transform-box:view-box;transform-origin:66px 44px';
        svg.append(kropp, banan);
        lag.append(svg);
        spisBanan(banan);
        return {posisjon, banan};
    });
    lover = lovePosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -45 210 140');
        svg.setAttribute('width', '210');
        svg.setAttribute('height', '140');
        const venstre = posisjon.x - LOVE_BOKS.x + LOVE_ROT.x - 100;
        const topp = posisjon.y - LOVE_BOKS.y + LOVE_ROT.y - 45;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible;transform:rotate(-8deg) scale(.78);transform-origin:100px 45px`;
        const hale = svgElement('use');
        hale.setAttribute('href', '/images/wildlife.svg#lion-tail');
        svg.append(hale);
        lag.append(svg);
        return {posisjon, hale};
    });
    sjiraffer = sjiraffPosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -120 200 240');
        svg.setAttribute('width', '200');
        svg.setAttribute('height', '240');
        const venstre = posisjon.x - SJIRAFF_BOKS.x + SJIRAFF_ROT.x - 100;
        const topp = posisjon.y - SJIRAFF_BOKS.y + SJIRAFF_ROT.y - 120;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible;transform:rotate(-8deg);transform-origin:100px 120px`;
        const hals = svgElement('use');
        hals.setAttribute('href', '/images/wildlife.svg#giraffe-neck');
        svg.append(hals);
        lag.append(svg);
        return {posisjon, hals};
    });
    flodhester = flodhestPosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -40 210 130');
        svg.setAttribute('width', '210');
        svg.setAttribute('height', '130');
        const venstre = posisjon.x - FLODHEST_BOKS.x + FLODHEST_ROT.x - 100;
        const topp = posisjon.y - FLODHEST_BOKS.y + FLODHEST_ROT.y - 40;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible;transform:rotate(-3deg) scale(.82);transform-origin:100px 40px`;
        const munn = svgElement('use');
        munn.setAttribute('href', '/images/wildlife.svg#hippo-mouth');
        munn.style.opacity = '0';
        const kjeve = svgElement('use');
        kjeve.setAttribute('href', '/images/wildlife.svg#hippo-jaw');
        svg.append(munn, kjeve);
        lag.append(svg);
        return {posisjon, munn, kjeve};
    });
    kamler = kamelPosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -80 200 190');
        svg.setAttribute('width', '200');
        svg.setAttribute('height', '190');
        const venstre = posisjon.x - KAMEL_BOKS.x + KAMEL_ROT.x - 100;
        const topp = posisjon.y - KAMEL_BOKS.y + KAMEL_ROT.y - 80;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible;transform:rotate(-5deg) scale(.74);transform-origin:100px 80px`;
        const hode = svgElement('use');
        hode.setAttribute('href', '/images/wildlife.svg#camel-head');
        const spytt = svgElement('circle');
        spytt.setAttribute('cx', '88');
        spytt.setAttribute('cy', '-50');
        spytt.setAttribute('r', '4');
        spytt.setAttribute('fill', '#d9efe3');
        spytt.style.opacity = '0';
        svg.append(hode, spytt);
        lag.append(svg);
        return {posisjon, hode, spytt};
    });
    krokodiller = krokodillePosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -30 220 90');
        svg.setAttribute('width', '220');
        svg.setAttribute('height', '90');
        const venstre = posisjon.x - KROKODILLE_BOKS.x + KROKODILLE_ROT.x - 100;
        const topp = posisjon.y - KROKODILLE_BOKS.y + KROKODILLE_ROT.y - 30;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible;transform:rotate(-4deg) scale(.82);transform-origin:100px 30px`;
        const kjeve = svgElement('use');
        kjeve.setAttribute('href', '/images/wildlife.svg#crocodile-jaw');
        svg.append(kjeve);
        lag.append(svg);
        return {posisjon, kjeve};
    });
    sebraer = sebraPosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -100 200 200');
        svg.setAttribute('width', '200');
        svg.setAttribute('height', '200');
        const venstre = posisjon.x - SEBRA_BOKS.x + SEBRA_ROT.x - 100;
        const topp = posisjon.y - SEBRA_BOKS.y + SEBRA_ROT.y - 100;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible;transform:rotate(-10deg) scale(.8);transform-origin:100px 100px`;
        const hode = svgElement('use');
        hode.setAttribute('href', '/images/wildlife.svg#zebra-head');
        svg.append(hode);
        lag.append(svg);
        return {posisjon, hode};
    });
    palmer = palmePosisjoner(bredde, hoyde).map(posisjon => {
        const svg = svgElement('svg');
        svg.setAttribute('viewBox', '-100 -220 200 220');
        svg.setAttribute('width', `${200 * PALME_ROT.skala}`);
        svg.setAttribute('height', `${220 * PALME_ROT.skala}`);
        const venstre = posisjon.x - PALME_BOKS.x + PALME_ROT.x - 100 * PALME_ROT.skala;
        const topp = posisjon.y - PALME_BOKS.y + PALME_ROT.y - 220 * PALME_ROT.skala;
        svg.style.cssText = `position:absolute;left:${venstre}px;top:${topp}px;overflow:visible`;
        const kokosnotter = KOKOSNOTTER.map(id => {
            const kokosnott = svgElement('use');
            kokosnott.setAttribute('href', `/images/wildlife.svg#${id}`);
            return kokosnott;
        });
        svg.append(...kokosnotter);
        lag.append(svg);
        return {posisjon, kokosnotter};
    });
}

new ResizeObserver(plasserDyr).observe(document.documentElement);
plasserDyr();

// Gorillaen er bak innholdet og skal bare reagere når pekeren er over selve bakgrunnen
function erDekket(element: Element | null) {
    for (let e = element; e && e !== document.body && e !== document.documentElement; e = e.parentElement) {
        const bakgrunn = getComputedStyle(e);
        if (bakgrunn.backgroundColor !== 'rgba(0, 0, 0, 0)' || bakgrunn.backgroundImage !== 'none') return true;
    }
    return false;
}

let sistePaa: HTMLImageElement | undefined;
let paaLove = false;
let paaElefant = false;
let sisteApe: SVGUseElement | undefined;
let sisteSjiraff: typeof sjiraffer[number] | undefined;
let sisteFlodhest: typeof flodhester[number] | undefined;
let sisteKamel: typeof kamler[number] | undefined;
let sisteKrokodille: typeof krokodiller[number] | undefined;
let sisteSebra: typeof sebraer[number] | undefined;
let sistePalme: { posisjon: Posisjon } | undefined;

document.addEventListener('mousemove', hendelse => {
    const {pageX, pageY} = hendelse;
    const rot = document.documentElement;
    const gorilla = gorillaer.find(({posisjon}) => erPaaGorilla(pageX, pageY, posisjon));
    const love = erPaaLove(pageX, pageY, rot.clientWidth, rot.getBoundingClientRect().height);
    const elefant = elefanter.find(({posisjon}) => erPaaElefantPosisjon(pageX, pageY, posisjon));
    const ape = aper.find(({posisjon}) => erPaaApe(pageX, pageY, posisjon));
    const palme = palmer.find(({posisjon}) => erPaaPalme(pageX, pageY, posisjon));
    const lovePosisjon = lover.find(({posisjon}) => erPaaLovePosisjon(pageX, pageY, posisjon));
    const sjiraff = sjiraffer.find(({posisjon}) => erPaaSjiraff(pageX, pageY, posisjon));
    const flodhest = flodhester.find(({posisjon}) => erPaaFlodhest(pageX, pageY, posisjon));
    const kamel = kamler.find(({posisjon}) => erPaaKamel(pageX, pageY, posisjon));
    const krokodille = krokodiller.find(({posisjon}) => erPaaKrokodille(pageX, pageY, posisjon));
    const sebra = sebraer.find(({posisjon}) => erPaaSebra(pageX, pageY, posisjon));
    const dekket = (gorilla || love || elefant || ape || palme || sjiraff || flodhest || kamel || krokodille || sebra) && erDekket(hendelse.target as Element | null);

    const gorillaTreff = !dekket ? gorilla?.bilde : undefined;
    if (gorillaTreff && gorillaTreff !== sistePaa) {
        hopp(gorillaTreff);
    }
    sistePaa = gorillaTreff;

    const loveTreff = love && !dekket;
    if (loveTreff && !paaLove) {
        if (lovePosisjon) viftMedHalen(lovePosisjon.hale);
    }
    paaLove = loveTreff;

    const elefantTreff = Boolean(elefant && !dekket);
    if (elefantTreff && !paaElefant) {
        if (elefant) loftSnabel(elefant.snabel);
    }
    paaElefant = elefantTreff;

    const apeTreff = !dekket ? ape?.banan : undefined;
    if (apeTreff && apeTreff !== sisteApe) kastBanan(apeTreff);
    sisteApe = apeTreff;

    const sjiraffTreff = !dekket ? sjiraff : undefined;
    if (sjiraffTreff && sjiraffTreff !== sisteSjiraff) bukk(sjiraffTreff.hals);
    sisteSjiraff = sjiraffTreff;

    const flodhestTreff = !dekket ? flodhest : undefined;
    if (flodhestTreff && flodhestTreff !== sisteFlodhest) {
        gjesp(flodhestTreff.munn, flodhestTreff.kjeve);
    }
    sisteFlodhest = flodhestTreff;

    const kamelTreff = !dekket ? kamel : undefined;
    if (kamelTreff && kamelTreff !== sisteKamel) {
        spytt(kamelTreff.hode, kamelTreff.spytt);
    }
    sisteKamel = kamelTreff;

    const krokodilleTreff = !dekket ? krokodille : undefined;
    if (krokodilleTreff && krokodilleTreff !== sisteKrokodille) {
        snapp(krokodilleTreff.kjeve);
    }
    sisteKrokodille = krokodilleTreff;

    const sebraTreff = !dekket ? sebra : undefined;
    if (sebraTreff && sebraTreff !== sisteSebra) {
        kastMedHodet(sebraTreff.hode);
    }
    sisteSebra = sebraTreff;

    const palmeTreff = !dekket ? palme : undefined;
    if (palmeTreff && palmeTreff !== sistePalme) fellKokosnotter(palmeTreff.kokosnotter);
    sistePalme = palmeTreff;
});

const erSkjemafelt = (element: EventTarget | null) =>
    element instanceof HTMLElement && (element.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(element.tagName));

let rickrollSteg = 0;
document.addEventListener('keydown', hendelse => {
    if (hendelse.ctrlKey || hendelse.metaKey || hendelse.altKey || erSkjemafelt(hendelse.target)) return;
    rickrollSteg = nesteRickrollSteg(rickrollSteg, hendelse.key);
    if (rickrollSteg === RICKROLL_KODE.length) {
        rickrollSteg = 0;
        window.open('https://www.youtube.com/watch?v=dQw4w9WgXcQ', '_blank', 'noopener,noreferrer');
    }
});
