import {expect, test} from '@playwright/test';
import {erPaaGorilla, erPaaLove, gorillaPosisjoner} from '../../main/typescript/dyrlogikk';

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
