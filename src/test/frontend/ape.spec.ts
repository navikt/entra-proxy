import {expect, test} from './support';

for (const redusertBevegelse of [false, true]) {
    test(`apekatten spiser og kaster bananen med redusert bevegelse ${redusertBevegelse}`, async ({app}) => {
        const {page} = app;
        await page.setViewportSize({width: 1440, height: 1200});
        await page.emulateMedia({reducedMotion: redusertBevegelse ? 'reduce' : 'no-preference'});
        await app.open('index');
        const banan = page.locator('use[href$="#monkey-banana"]').last();
        await expect.poll(() => banan.evaluate(element => element.getBoundingClientRect().width)).toBeGreaterThan(0);
        const ape = page.locator('svg').filter({has: page.locator('use[href$="#monkey"]')}).last();
        const boks = await ape.boundingBox();
        if (!boks) throw new Error('Apekatten er ikke synlig');
        await page.mouse.move(boks.x - 10, boks.y + 50);
        const varighet = () => banan.evaluate(element => element.getAnimations()[0]?.effect?.getTiming().duration ?? 0);
        await expect.poll(varighet).toBe(redusertBevegelse ? 0 : 1600);
        await page.mouse.move(boks.x + 50, boks.y + 50);
        if (redusertBevegelse) {
            expect(await varighet()).toBe(0);
            return;
        }
        await expect.poll(varighet).toBe(1200);
        const forflytning = await banan.evaluate(element => {
            const kast = element.getAnimations()[0];
            kast.pause();
            kast.currentTime = 0;
            const start = element.getBoundingClientRect();
            kast.currentTime = 480;
            const topp = element.getBoundingClientRect();
            return {x: topp.x - start.x, y: topp.y - start.y};
        });
        expect(forflytning.x).toBeGreaterThan(40);
        expect(forflytning.y).toBeLessThan(-60);
        await page.mouse.move(boks.x + 55, boks.y + 55);
        await expect.poll(varighet).toBe(1200);
        await banan.evaluate(element => element.getAnimations()[0].finish());
        await expect.poll(varighet).toBe(1600);
        await page.mouse.move(boks.x - 10, boks.y + 50);
        await page.mouse.move(boks.x + 50, boks.y + 50);
        await expect.poll(varighet).toBe(1200);
        await banan.evaluate(element => element.getAnimations()[0].finish());
        await expect.poll(varighet).toBe(1600);
        await page.mouse.move(boks.x - 10, boks.y + 50);
        await page.evaluate(({x, y}) => {
            const panel = document.createElement('div');
            panel.style.cssText = `position:absolute;left:${x}px;top:${y}px;width:110px;height:110px;background:white`;
            document.body.append(panel);
        }, boks);
        await page.mouse.move(boks.x + 50, boks.y + 50);
        expect(await varighet()).toBe(1600);
    });
}
