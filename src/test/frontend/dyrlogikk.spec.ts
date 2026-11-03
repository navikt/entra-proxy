import {expect, test} from '@playwright/test';
import {nesteRickrollSteg, RICKROLL_KODE, erPaaGorilla, erPaaLove, erPaaElefant, gorillaPosisjoner, apePosisjoner, erPaaApe} from '../../main/typescript/dyrlogikk';

test('en side på størrelse med flisen har én gorilla på riktig sted', () => {
    const gorillaer = gorillaPosisjoner(720, 600).filter(g => g.x >= 0 && g.y >= 0);
    expect(gorillaer).toHaveLength(1);
    expect(erPaaGorilla(600, 300, gorillaer[0])).toBe(true);
    expect(erPaaGorilla(100, 100, gorillaer[0])).toBe(false);
});

test('gorillaene gjentas for hver flis', () => {
    const gorillaer = gorillaPosisjoner(1440, 1200).filter(g => g.x >= 0 && g.y >= 0);
    expect(gorillaer).toHaveLength(6);
    expect(new Set(gorillaer.map(g => g.x)).size).toBe(3);
});

test('forskyvning fra sentrert bakgrunn tas med', () => {
    // Bredere side flytter flisen 100 px mot høyre
    const gorilla = gorillaPosisjoner(920, 600).find(g => g.x > 600 && g.y >= 0)!;
    expect(erPaaGorilla(700, 300, gorilla)).toBe(true);
    expect(erPaaGorilla(600, 300, gorilla)).toBe(false);
});

test('løven treffes i alle fliser', () => {
    // Side på størrelse med flisen: løven ligger rundt 131,95
    expect(erPaaLove(131, 95, 720, 600)).toBe(true);
    // Bredere og høyere side forskyver og gjentar flisen
    expect(erPaaLove(491, 395, 1440, 1200)).toBe(true);
    expect(erPaaLove(400, 300, 720, 600)).toBe(false);
});

test('elefanten treffes i sentrerte og gjentatte fliser', () => {
    expect(erPaaElefant(370, 90, 720, 600)).toBe(true);
    expect(erPaaElefant(730, 390, 1440, 1200)).toBe(true);
    expect(erPaaElefant(10, 390, 1440, 1200)).toBe(true);
    expect(erPaaElefant(470, 90, 920, 600)).toBe(true);
    expect(erPaaElefant(131, 95, 720, 600)).toBe(false);
    expect(erPaaElefant(451, 90, 720, 600)).toBe(false);
});

test('apekatten gjentas og treffes i sentrerte fliser', () => {
    const ape = apePosisjoner(720, 600).find(p => p.x === 230 && p.y === 170);
    expect(ape).toBeDefined();
    if (!ape) throw new Error('Fant ikke apekatten i flisen');
    expect(erPaaApe(280, 220, ape)).toBe(true);
    expect(erPaaApe(229, 220, ape)).toBe(false);
    expect(erPaaApe(280, 281, ape)).toBe(false);
    expect(apePosisjoner(1440, 1200)).toContainEqual({x: 590, y: 470});
    expect(apePosisjoner(920, 600)).toContainEqual({x: 330, y: 170});
});

test('rickroll-koden gjenkjennes og nullstilles ved feil tast', () => {
    const kjor = (taster: string[]) => taster.reduce(nesteRickrollSteg, 0);
    expect(kjor(['n', 'g'])).toBe(RICKROLL_KODE.length);
    expect(kjor(['N', 'G'])).toBe(RICKROLL_KODE.length);
    expect(kjor(['n', 'x'])).toBe(0);
    expect(kjor(['n', 'n'])).toBe(1);
});
