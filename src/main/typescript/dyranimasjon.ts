const redusertBevegelse = () => matchMedia('(prefers-reduced-motion: reduce)').matches;

const LOVE_BROEL_VARIGHET = 1600;
const GJESP_VARIGHET = 2000;
const SPYTT_VARIGHET = 1800;
const SNAPP_VARIGHET = 1500;
const SEBRA_VARIGHET = 1400;
const KOKOSNOTT_FALL = 105;

export function viftMedHalen(hale: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of hale.getAnimations()) animasjon.cancel();
    const sving = (grader: number) => `translate(-55px, 40px) rotate(${grader}deg) translate(55px, -40px)`;
    hale.animate([-22, 24, -22, 24, -22, 24, -22, 24, 0].map((grader, i, alle) => ({
        transform: sving(grader),
        offset: i / (alle.length - 1),
        easing: 'ease-in-out'
    })), {duration: LOVE_BROEL_VARIGHET});
}

export function bukk(hals: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of hals.getAnimations()) animasjon.cancel();
    const boy = (grader: number) => `translate(30px, 10px) rotate(${grader}deg) translate(-30px, -10px)`;
    hals.animate([
        {transform: boy(0), offset: 0},
        {transform: boy(70), offset: .35},
        {transform: boy(70), offset: .65},
        {transform: boy(0), offset: 1}
    ], {duration: 2200, easing: 'ease-in-out'});
}

export function gjesp(munn: SVGUseElement, kjeve: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of [...munn.getAnimations(), ...kjeve.getAnimations()]) animasjon.cancel();
    const apning = (grader: number) => `translate(45px, 38px) rotate(${grader}deg) translate(-45px, -38px)`;
    const tidsfordeling = {duration: GJESP_VARIGHET, easing: 'ease-in-out'};
    kjeve.animate([
        {transform: apning(0), offset: 0},
        {transform: apning(38), offset: .3},
        {transform: apning(38), offset: .7},
        {transform: apning(0), offset: 1}
    ], tidsfordeling);
    munn.animate([
        {opacity: 0, offset: 0},
        {opacity: 1, offset: .1},
        {opacity: 1, offset: .9},
        {opacity: 0, offset: 1}
    ], tidsfordeling);
}

export function spytt(hode: SVGUseElement, spytt: SVGCircleElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of [...hode.getAnimations(), ...spytt.getAnimations()]) animasjon.cancel();
    const vipp = (grader: number) => `translate(45px, 15px) rotate(${grader}deg) translate(-45px, -15px)`;
    hode.animate([
        {transform: vipp(0), offset: 0, easing: 'ease-out'},
        {transform: vipp(-22), offset: .3, easing: 'ease-in'},
        {transform: vipp(12), offset: .4, easing: 'ease-out'},
        {transform: vipp(12), offset: .55, easing: 'ease-in-out'},
        {transform: vipp(0), offset: 1}
    ], {duration: SPYTT_VARIGHET});
    spytt.animate([
        {transform: 'translate(0, 0) scale(.6)', opacity: 0, offset: 0},
        {transform: 'translate(0, 0) scale(.6)', opacity: 0, offset: .4},
        {transform: 'translate(0, 0) scale(1)', opacity: 1, offset: .41},
        {transform: 'translate(70px, -12px) scale(1.3)', opacity: 1, offset: .6, easing: 'ease-in'},
        {transform: 'translate(130px, 55px) scale(1)', opacity: 0, offset: .85},
        {transform: 'translate(130px, 55px) scale(1)', opacity: 0, offset: 1}
    ], {duration: SPYTT_VARIGHET, easing: 'linear'});
}

export function snapp(kjeve: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of kjeve.getAnimations()) animasjon.cancel();
    const hengsel = (grader: number) => `translate(58px, 8px) rotate(${grader}deg) translate(-58px, -8px)`;
    kjeve.animate([
        {transform: hengsel(0), offset: 0, easing: 'ease-out'},
        {transform: hengsel(-34), offset: .25},
        {transform: hengsel(-34), offset: .42, easing: 'ease-in'},
        {transform: hengsel(2), offset: .47, easing: 'ease-out'},
        {transform: hengsel(0), offset: .52},
        {transform: hengsel(0), offset: 1}
    ], {duration: SNAPP_VARIGHET});
}

export function kastMedHodet(hode: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of hode.getAnimations()) animasjon.cancel();
    const vipp = (grader: number) => `translate(20px, 15px) rotate(${grader}deg) translate(-20px, -15px)`;
    hode.animate([0, -26, 4, -22, 2, -14, 0].map((grader, i, alle) => ({
        transform: vipp(grader),
        offset: i / (alle.length - 1),
        easing: 'ease-in-out'
    })), {duration: SEBRA_VARIGHET});
}

export function fellKokosnotter(kokosnotter: SVGUseElement[]) {
    if (redusertBevegelse()) return;
    kokosnotter.forEach((kokosnott, i) => {
        if (kokosnott.getAnimations().length > 0) return;
        const fall = KOKOSNOTT_FALL + i * 3;
        kokosnott.animate([
            {transform: 'translate(0, 0) rotate(0deg)', opacity: 1, offset: 0, easing: 'ease-in'},
            {transform: `translate(${(i - 1) * 6}px, ${fall}px) rotate(${180 + i * 90}deg)`, opacity: 1, offset: .35, easing: 'ease-out'},
            {transform: `translate(${(i - 1) * 9}px, ${fall - 14}px) rotate(${240 + i * 90}deg)`, opacity: 1, offset: .45, easing: 'ease-in'},
            {transform: `translate(${(i - 1) * 11}px, ${fall}px) rotate(${300 + i * 90}deg)`, opacity: 1, offset: .55},
            {transform: `translate(${(i - 1) * 11}px, ${fall}px) rotate(${300 + i * 90}deg)`, opacity: 1, offset: .8},
            {transform: `translate(${(i - 1) * 11}px, ${fall}px) rotate(${300 + i * 90}deg)`, opacity: 0, offset: .81},
            {transform: 'translate(0, 0) rotate(0deg)', opacity: 0, offset: .9},
            {transform: 'translate(0, 0) rotate(0deg)', opacity: 1, offset: 1}
        ], {duration: 2600, delay: i * 120, fill: 'backwards'});
    });
}

export function spisBanan(banan: SVGUseElement) {
    if (redusertBevegelse()) return;
    banan.animate([
        {transform: 'translate(0, 0) rotate(0deg)'},
        {transform: 'translate(-5px, -4px) rotate(-12deg)'},
        {transform: 'translate(0, 0) rotate(0deg)'}
    ], {duration: 1600, iterations: Infinity, easing: 'ease-in-out'});
}

export function kastBanan(banan: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of banan.getAnimations()) animasjon.cancel();
    const kast = banan.animate([
        {transform: 'translate(0, 0) rotate(0deg)', opacity: 1, offset: 0},
        {transform: 'translate(65px, -95px) rotate(150deg)', opacity: 1, offset: .4},
        {transform: 'translate(160px, 30px) rotate(330deg)', opacity: 0, offset: .85},
        {transform: 'translate(0, 0) rotate(0deg)', opacity: 0, offset: .86},
        {transform: 'translate(0, 0) rotate(0deg)', opacity: 1, offset: 1}
    ], {duration: 1200, easing: 'linear'});
    kast.onfinish = () => {
        if (banan.isConnected) spisBanan(banan);
    };
}

export function loftSnabel(snabel: SVGUseElement) {
    if (redusertBevegelse()) return;
    for (const animasjon of snabel.getAnimations()) animasjon.cancel();
    snabel.animate([
        {transform: 'rotate(0deg)', offset: 0},
        {transform: 'rotate(-130deg)', offset: .3},
        {transform: 'rotate(-130deg)', offset: .65},
        {transform: 'rotate(0deg)', offset: 1}
    ], {duration: 1300, easing: 'ease-in-out'});
}

export function hopp(bilde: HTMLElement) {
    if (redusertBevegelse()) return;
    bilde.animate([
        {transform: 'translateY(0) scale(1.08, .88)', offset: 0},
        {transform: 'translateY(-110px) scale(.94, 1.1) rotate(-6deg)', offset: .4, easing: 'ease-in'},
        {transform: 'translateY(0) scale(1.08, .88)', offset: .8},
        {transform: 'translateY(0) scale(1)', offset: 1}
    ], {duration: 800, easing: 'ease-out'});
}
