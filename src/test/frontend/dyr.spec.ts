import {readdirSync} from 'node:fs';
import {expect, test} from './support';

test('bakgrunnsanimasjonene oppretter ikke lyd paa noen HTML-side', async ({app}) => {
    const {page} = app;
    await page.setViewportSize({width: 720, height: 600});
    await page.addInitScript(() => {
        window.AudioContext = class extends AudioContext {
            constructor(options?: AudioContextOptions) {
                super(options);
                document.documentElement.dataset.lydOpprettet = 'true';
            }
        };
    });
    for (const navn of ['src/main/resources/static', 'src/main/resources/static/html']
        .flatMap(mappe => readdirSync(mappe)).filter(navn => navn.endsWith('.html'))) {
        await app.open(navn.slice(0, -5));
        await page.locator('body > div[aria-hidden="true"] img').first().waitFor({state: 'attached'});
        await page.evaluate(() => {
            for (const element of document.body.children) {
                if (element.getAttribute('aria-hidden') !== 'true') element.setAttribute('hidden', '');
            }
        });
        await page.mouse.click(10, 10);
        await page.keyboard.press('Shift');
        await page.evaluate(() => {
            for (let y = 10; y < 600; y += 20) {
                for (let x = 10; x < 720; x += 20) {
                    document.body.dispatchEvent(new MouseEvent('mousemove', {
                        clientX: x, clientY: y, bubbles: true
                    }));
                }
            }
        });
        expect(await page.locator('html').getAttribute('data-lyd-opprettet'), navn).toBeNull();
    }
});

test('elefanten animeres uten lyd ved innpassering, men ikke over innhold', async ({app}) => {
    const {page} = app;
    await page.setViewportSize({width: 720, height: 600});
    await page.addInitScript(() => {
        const start = OscillatorNode.prototype.start;
        OscillatorNode.prototype.start = function (when) {
            document.documentElement.dataset.lydstarter =
                String(Number(document.documentElement.dataset.lydstarter ?? 0) + 1);
            start.call(this, when);
        };
    });
    await app.open('index');
    await page.locator('main').evaluate(element => element.setAttribute('hidden', ''));
    const snabel = page.locator('use[href$="#elephant-trunk"]');
    await expect.poll(() => snabel.last().evaluate(element => element.getBoundingClientRect().width)).toBeGreaterThan(0);
    await page.mouse.move(270, 90);
    const starter = () => page.evaluate(() => Number(document.documentElement.dataset.lydstarter ?? 0));
    await page.mouse.move(370, 90);
    expect(await starter()).toBe(0);
    await expect.poll(() => snabel.evaluateAll(elementer =>
        elementer.filter(element => element.getAnimations().length > 0).length
    )).toBe(1);
    const hoyder = await snabel.last().evaluate(element => {
        const animasjon = element.getAnimations()[0];
        animasjon.pause();
        animasjon.currentTime = 0;
        const nede = element.getBoundingClientRect().top;
        animasjon.currentTime = 650;
        const oppe = element.getBoundingClientRect().top;
        animasjon.finish();
        return {nede, oppe};
    });
    expect(hoyder.oppe).toBeLessThan(hoyder.nede - 20);
    await page.mouse.move(270, 90);
    await page.keyboard.press('Shift');
    await page.mouse.move(370, 90);
    expect(await starter()).toBe(0);
    await page.mouse.move(380, 100);
    expect(await starter()).toBe(0);
    await page.mouse.move(270, 90);
    await page.mouse.move(370, 90);
    expect(await starter()).toBe(0);
    await expect.poll(() => snabel.evaluateAll(elementer =>
        elementer.flatMap(element => element.getAnimations()).length
    )).toBe(1);
    await snabel.evaluateAll(elementer =>
        elementer.flatMap(element => element.getAnimations()).forEach(animasjon => animasjon.finish())
    );
    await page.mouse.move(270, 90);
    await page.evaluate(() => {
        const panel = document.createElement('div');
        panel.style.cssText = 'position:absolute;left:290px;top:40px;width:160px;height:120px;background:white';
        document.body.append(panel);
    });
    await page.mouse.move(370, 90);
    expect(await starter()).toBe(0);
    expect(await snabel.evaluateAll(elementer =>
        elementer.flatMap(element => element.getAnimations()).length
    )).toBe(0);
});

test('elefantens snabel forblir i ro ved redusert bevegelse', async ({app}) => {
    const {page} = app;
    await page.setViewportSize({width: 720, height: 600});
    await page.emulateMedia({reducedMotion: 'reduce'});
    await app.open('index');
    await page.locator('main').evaluate(element => element.setAttribute('hidden', ''));
    await page.mouse.move(370, 90);
    expect(await page.locator('use[href$="#elephant-trunk"]').evaluateAll(elementer =>
        elementer.flatMap(element => element.getAnimations()).length
    )).toBe(0);
});
