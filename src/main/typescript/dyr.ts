import {erPaaGorilla, GORILLA_BREDDE, GORILLA_HOYDE, gorillaPosisjoner, erPaaLove, type Posisjon} from './dyrlogikk.js';

let lyd: AudioContext | undefined;

function bankPaaBrystet(ctx: AudioContext, start: number) {
    const oscillator = ctx.createOscillator();
    const volum = ctx.createGain();
    oscillator.frequency.setValueAtTime(110, start);
    oscillator.frequency.exponentialRampToValueAtTime(40, start + 0.12);
    volum.gain.setValueAtTime(0.9, start);
    volum.gain.exponentialRampToValueAtTime(0.001, start + 0.15);
    oscillator.connect(volum).connect(ctx.destination);
    oscillator.start(start);
    oscillator.stop(start + 0.16);
}

function grynt(ctx: AudioContext, start: number) {
    const oscillator = ctx.createOscillator();
    const filter = ctx.createBiquadFilter();
    const volum = ctx.createGain();
    oscillator.type = 'sawtooth';
    oscillator.frequency.setValueAtTime(90, start);
    oscillator.frequency.linearRampToValueAtTime(140, start + 0.15);
    oscillator.frequency.linearRampToValueAtTime(60, start + 0.5);
    filter.type = 'lowpass';
    filter.frequency.value = 500;
    volum.gain.setValueAtTime(0.0001, start);
    volum.gain.linearRampToValueAtTime(0.7, start + 0.08);
    volum.gain.exponentialRampToValueAtTime(0.001, start + 0.55);
    oscillator.connect(filter).connect(volum).connect(ctx.destination);
    oscillator.start(start);
    oscillator.stop(start + 0.6);
}

// Nettlesere tillater ikke lyd før brukeren har klikket eller trykket på siden
function laasOppLyd() {
    lyd ??= new AudioContext();
    if (lyd.state === 'suspended') void lyd.resume();
}

for (const type of ['pointerdown', 'keydown'])
    document.addEventListener(type, laasOppLyd, {once: true});

function broel(ctx: AudioContext, start: number) {
    const varighet = 1.6;
    const oscillator = ctx.createOscillator();
    const filter = ctx.createBiquadFilter();
    const volum = ctx.createGain();
    const skjelving = ctx.createOscillator();
    const skjelvingStyrke = ctx.createGain();
    oscillator.type = 'sawtooth';
    oscillator.frequency.setValueAtTime(70, start);
    oscillator.frequency.linearRampToValueAtTime(130, start + 0.35);
    oscillator.frequency.exponentialRampToValueAtTime(45, start + varighet);
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(300, start);
    filter.frequency.linearRampToValueAtTime(1100, start + 0.35);
    filter.frequency.linearRampToValueAtTime(250, start + varighet);
    // Rask amplitudemodulering gir det rullende, hese brølet
    skjelving.frequency.value = 28;
    skjelvingStyrke.gain.value = 0.35;
    volum.gain.setValueAtTime(0.0001, start);
    volum.gain.linearRampToValueAtTime(0.7, start + 0.2);
    volum.gain.exponentialRampToValueAtTime(0.001, start + varighet);
    skjelving.connect(skjelvingStyrke).connect(volum.gain);
    oscillator.connect(filter).connect(volum).connect(ctx.destination);
    for (const kilde of [oscillator, skjelving]) {
        kilde.start(start);
        kilde.stop(start + varighet + 0.05);
    }
}

function spillLovebroel() {
    if (lyd?.state !== 'running') return;
    broel(lyd, lyd.currentTime);
}

function spillGorillalyd() {
    if (lyd?.state !== 'running') return;
    const na = lyd.currentTime;
    grynt(lyd, na);
    for (let i = 0; i < 4; i++) bankPaaBrystet(lyd, na + 0.6 + i * 0.14);
}

const lag = document.createElement('div');
lag.setAttribute('aria-hidden', 'true');
// z-index -1 legger gorillaene over bakgrunnen, men bak sidens innhold
lag.style.cssText = 'position:absolute;top:0;left:0;pointer-events:none;overflow:hidden;z-index:-1';
document.body.append(lag);

let gorillaer: { posisjon: Posisjon; bilde: HTMLImageElement }[] = [];

function plasserGorillaer() {
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
}

function hopp(bilde: HTMLElement) {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    bilde.animate([
        {transform: 'translateY(0) scale(1.08, .88)', offset: 0},
        {transform: 'translateY(-110px) scale(.94, 1.1) rotate(-6deg)', offset: .4, easing: 'ease-in'},
        {transform: 'translateY(0) scale(1.08, .88)', offset: .8},
        {transform: 'translateY(0) scale(1)', offset: 1}
    ], {duration: 800, easing: 'ease-out'});
}

new ResizeObserver(plasserGorillaer).observe(document.documentElement);
plasserGorillaer();

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

document.addEventListener('mousemove', hendelse => {
    const {pageX, pageY} = hendelse;
    const rot = document.documentElement;
    const gorilla = gorillaer.find(({posisjon}) => erPaaGorilla(pageX, pageY, posisjon));
    const love = erPaaLove(pageX, pageY, rot.clientWidth, rot.getBoundingClientRect().height);
    const dekket = (gorilla || love) && erDekket(hendelse.target as Element | null);

    const gorillaTreff = !dekket ? gorilla?.bilde : undefined;
    if (gorillaTreff && gorillaTreff !== sistePaa) {
        spillGorillalyd();
        hopp(gorillaTreff);
    }
    sistePaa = gorillaTreff;

    const loveTreff = love && !dekket;
    if (loveTreff && !paaLove) spillLovebroel();
    paaLove = loveTreff;
});
