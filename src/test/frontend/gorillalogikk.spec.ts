import {expect, test} from '@playwright/test';
import {erPaaGorilla} from '../../main/typescript/gorillalogikk';

test('gorillaen treffes i flisen og i gjentatte fliser', () => {
    // Sidebredde 720 x høyde 600 gir ingen forskyvning
    expect(erPaaGorilla(600, 300, 720, 600)).toBe(true);
    expect(erPaaGorilla(600 + 720, 300 + 600, 720, 600)).toBe(true);
    expect(erPaaGorilla(100, 100, 720, 600)).toBe(false);
});

test('forskyvning fra sentrert bakgrunn tas med', () => {
    // Bredere side flytter flisen 100 px mot høyre
    expect(erPaaGorilla(700, 300, 920, 600)).toBe(true);
    expect(erPaaGorilla(600, 300, 920, 600)).toBe(false);
});
