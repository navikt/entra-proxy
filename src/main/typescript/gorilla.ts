import {erPaaGorilla} from './gorillalogikk.js';

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

function spillGorillalyd() {
    lyd ??= new AudioContext();
    // Nettlesere krever en brukerhandling før lyd kan spilles
    if (lyd.state === 'suspended') void lyd.resume();
    const na = lyd.currentTime;
    grynt(lyd, na);
    for (let i = 0; i < 4; i++) bankPaaBrystet(lyd, na + 0.6 + i * 0.14);
}

let paaGorilla = false;

document.addEventListener('mousemove', hendelse => {
    const rot = document.documentElement;
    const traff = erPaaGorilla(hendelse.pageX, hendelse.pageY, rot.clientWidth, rot.getBoundingClientRect().height);
    if (traff && !paaGorilla) spillGorillalyd();
    paaGorilla = traff;
});
