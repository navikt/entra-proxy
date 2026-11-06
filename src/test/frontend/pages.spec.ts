import {readdirSync} from 'node:fs';
import {test, expect, json, submit, type MockResponse} from './support';

const ansatte = [
    {navIdent: 'A123456', visningNavn: 'Testansatt A'},
    {navIdent: 'B123456', visningNavn: 'Testansatt B'}
];
const nomAnsatte = ansatte.map(ansatt => ({
    navident: ansatt.navIdent,
    visningsnavn: ansatt.visningNavn,
    orgTilknytninger: [{orgEnhet: {tilgangsenhetId: '1234'}}]
}));

const basisUrl = '/api/v1/gruppe/medlemmer?gruppeNavn=Basis';
const sammenligningUrl = '/api/v1/gruppe/medlemmer?gruppeNavn=Sammenligning';
const permisjonUrl = '/api/v1/gruppe/medlemmer?gruppeNavn=Alle+disablede+brukere';
const nomBulkUrl = '/api/v1/nom/enhet/bulk';
const enhetsnavnUrl = '/api/v1/navn/1234';
const cacheUrl = '/api/v1/cache/flush';
const matriseUrl = '/api/v1/gruppe/medlemmer/sok/per-ansatt?gruppeNavn=TEMA';

for (const name of ['src/main/resources/static', 'src/main/resources/static/html']
    .flatMap(dir => readdirSync(dir)).filter(name => name.endsWith('.html'))) {
    test(`${name} loads compiled modules over HTTP and has no inline handlers`, async ({app}) => {
        await app.open(name.slice(0, -5));
        const scripts = app.page.locator('script');
        for (const script of await scripts.all()) {
            await expect(script).toHaveAttribute('type', 'module');
            await expect(script).toBeEmpty();
            const src = await script.getAttribute('src');
            if (!src) throw new Error(`Missing script source in ${name}`);
            expect(src).toMatch(/^\/.*\.js$/);
            const response = await app.page.request.get(src);
            expect(response.ok(), src).toBe(true);
            expect(response.headers()['content-type']).toContain('text/javascript');
        }
        expect(await app.page.locator('*').evaluateAll(elements =>
            elements.some(element => [...element.attributes].some(attribute => attribute.name.startsWith('on')))
        )).toBe(false);
    });
}

test('start page navigation uses links', async ({app}) => {
    await app.open('index');
    expect(await app.page.locator('nav a').evaluateAll(links => links.map(link => link.getAttribute('href'))))
        .toEqual(['/html/ansatt.html', '/html/grupper.html', '/html/gruppematrise.html']);
});

test('cache button reports success', async ({app}) => {
    await app.open('index', [json(cacheUrl, 17)]);
    const {page, calls} = app;
    await page.locator('#oppfrisk-caches').click();
    await expect(page.locator('#cache-status')).toBeVisible();
    await expect(page.locator('#cache-status')).toHaveText('Cache oppfrisket. 17 n\u00f8kler slettet.');
    await expect(page.locator('#oppfrisk-caches')).toBeEnabled();
    expect(new URL(calls[0].url()).pathname).toBe('/api/v1/cache/flush');
    expect(calls[0].method()).toBe('POST');
});

for (const [name, response, message] of [
    ['problem detail', json(cacheUrl, {detail: 'Ingen tilgang'}, 403), 'Ingen tilgang'],
    ['HTTP error', {url: cacheUrl, status: 500, body: ''}, 'Kunne ikke oppfriske cache (HTTP 500).'],
    ['redirect', {url: cacheUrl, status: 302, headers: {location: '/index.html'}, body: ''}, 'Failed to fetch'],
    ['network error', {url: cacheUrl, abort: true}, 'Failed to fetch']
] satisfies [string, MockResponse, string][]) {
    test(`cache ${name} remains visible and restores the button`, async ({app}) => {
        await app.open('index', [response]);
        const {page} = app;
        await page.locator('#oppfrisk-caches').click();
        await expect(page.locator('#cache-status')).toHaveText(`Oppfrisking feilet: ${message}`);
        await expect(page.locator('#cache-status')).toHaveClass(/error/);
        await expect(page.locator('#oppfrisk-caches')).toBeEnabled();
    });
}

for (const [name, parameter] of [['403', 'arsak'], ['401', 'navident']]) {
    test(`${name} displays query values as text only`, async ({app}) => {
        await app.open(name);
        await expect(app.page.locator(`#${parameter}`)).toBeHidden();
        const value = '<img src=x onerror=alert(1)>';
        await app.open(name, [], `?${new URLSearchParams({[parameter]: value})}`);
        await expect(app.page.locator(`#${parameter}`)).toBeVisible();
        await expect(app.page.locator(`#${parameter}-verdi`)).toHaveText(value);
        await expect(app.page.locator(`#${parameter}-verdi > *`)).toHaveCount(0);
    });
}

test('group explorer lists and sorts base members', async ({app}) => {
    await app.open('grupper', [json(basisUrl, ansatte)]);
    const {page, calls} = app;
    await submit(page, '#gruppe-form', 'Basis');
    await expect(page.locator('#status')).toHaveText('2 medlemmer i Basis');
    expect(calls).toHaveLength(1);

    await page.locator('.medlemmer-bryter').click();
    await expect(page.locator('#vis-utforsket-medlemmer')).toBeChecked();
    await expect(page.locator('#utforsket-medlemmer-resultat')).toBeVisible();
    const memberRows = page.locator('#utforsket-medlemmer-rader tr');
    await expect(memberRows).toHaveCount(2);
    const memberSort = page.locator('#utforsket-medlemmer-overskrift-rad .sorter-knapp').first();
    await memberSort.click();
    await memberSort.click();
    await expect(memberRows.first().locator('th')).toHaveText('B123456');
    await expect(memberRows.first().locator('a')).toHaveAttribute('href', '/html/ansatt.html?ansattid=B123456');
});

test('group explorer adds and removes comparison panels', async ({app}) => {
    await app.open('grupper', [
        json(basisUrl, ansatte),
        json(sammenligningUrl, [ansatte[0]]),
        json(permisjonUrl, [{navIdent: 'B123456'}]),
        json(nomBulkUrl, nomAnsatte),
        {url: enhetsnavnUrl, body: 'Testenhet'}
    ]);
    const {page} = app;
    await submit(page, '#gruppe-form', 'Basis');
    await page.locator('.medlemmer-bryter').click();
    await expect(page.locator('#vis-utforsket-medlemmer')).toBeChecked();
    await page.locator('.nytt-sok').click();
    await expect(page.locator('#vis-utforsket-medlemmer')).not.toBeChecked();
    const panel = page.locator('.sok-panel').nth(1);
    await expect(panel.locator('label')).toHaveAttribute('for', await panel.locator('input').getAttribute('id') ?? '');
    await submit(page, '#gruppe-form-2', 'Sammenligning');
    await expect(page.locator('#felles-resultat')).toBeVisible();
    await expect(page.locator('#sammenligning-detaljer')).toBeVisible();
    await panel.locator('.fjern-gruppe').click();
    await expect(page.locator('.sok-panel')).toHaveCount(1);
    await expect(page.locator('#felles-resultat')).toBeHidden();
});

test.describe('group comparison details', () => {
    test.beforeEach(async ({app}) => {
        await app.open('grupper', [
            json(basisUrl, ansatte),
            json(sammenligningUrl, [ansatte[0]]),
            json(permisjonUrl, [{navIdent: 'B123456'}]),
            json(nomBulkUrl, nomAnsatte),
            {url: enhetsnavnUrl, body: 'Testenhet'}
        ]);
        await submit(app.page, '#gruppe-form', 'Basis');
        await app.page.locator('.nytt-sok').click();
        await submit(app.page, '#gruppe-form-2', 'Sammenligning');
        await expect(app.page.locator('#sammenligning-detaljer')).toBeVisible();
    });

    test('enriches units and displays membership summaries', async ({app}) => {
        const {page, calls} = app;
        expect(calls).toHaveLength(5);
        const nomCall = calls.find(call => new URL(call.url()).pathname === nomBulkUrl);
        expect(nomCall?.postDataJSON()).toEqual(['A123456', 'B123456']);
        const summary = page.locator('#felles-medlemmer tr');
        await expect(summary.nth(0).locator('td').nth(1)).toHaveText('1');
        await expect(summary.nth(1).locator('td').nth(1)).toHaveText('1');
        const rows = page.locator('#sammenligning-tabell-rader tr');
        await expect(rows.first().locator('td').nth(1)).toHaveText('1234');
        await expect(rows.first().locator('td').nth(2)).toHaveText('Testenhet');
        await expect(rows.first().locator('td').nth(3)).toHaveText('\u2713');
        await expect(rows.nth(1).locator('td').nth(3)).toBeEmpty();
        await expect(page.locator('#sammenligning-tabell-overskrift th')).toHaveText([
            'Nav-ident', 'Navn', 'Nom tilgangsenhet', 'Nom tilgangsenhet navn',
            'Sammenligning', 'Ansatt er i permisjon'
        ]);
        await expect(rows.first().locator('td').nth(4)).toBeEmpty();
        await expect(rows.nth(1).locator('td').nth(4)).toHaveText('\u2713');
    });

    for (const [name, column] of [['employee ID', 0], ['permission status', 5]] as const) {
        test(`sorts by ${name}`, async ({app}) => {
            const sort = app.page.locator('#sammenligning-tabell-overskrift .sorter-knapp').nth(column);
            await sort.click();
            await sort.click();
            await expect(app.page.locator('#sammenligning-tabell-rader tr').first().locator('th'))
                .toHaveText('B123456');
        });
    }

    test('copies unit details and memberships', async ({app}) => {
        await app.page.locator('#kopier-detaljvisning').click();
        await expect(app.page.locator('#kopier-status')).toHaveText('Tabellen er kopiert til utklippstavlen.');
        const copied = await app.page.evaluate(() => navigator.clipboard.readText());
        expect(copied).toContain('Sammenligning;Ansatt er i permisjon');
        expect(copied).toContain('A123456;Testansatt A;1234;Testenhet;true;false');
        expect(copied).toContain('B123456;Testansatt B;1234;Testenhet;false;true');
    });
});

for (const [name, response, error, reverseLookups] of [
    ['empty group', json(permisjonUrl, []), undefined],
    ['empty group with reversed mock declarations', json(permisjonUrl, []), undefined, true],
    ['HTTP error', json(permisjonUrl, {detail: 'Ingen tilgang'}, 403), 'Ingen tilgang'],
    ['network error', {url: permisjonUrl, abort: true}, 'Failed to fetch'],
    ['invalid list', json(permisjonUrl, {}), 'Uventet respons: forventet en liste med nav-identer.'],
    ['invalid member', json(permisjonUrl, [{navIdent: 42}]), 'Uventet respons: forventet en liste med nav-identer.']
] satisfies [string, MockResponse, string | undefined, boolean?][]) {
    test(`group details keep the permission column for ${name}`, async ({app}) => {
        const lookups = [
            response,
            json(nomBulkUrl, nomAnsatte)
        ];
        await app.open('grupper', [
            json(basisUrl, ansatte), json(sammenligningUrl, [ansatte[0]]),
            ...(reverseLookups ? lookups.reverse() : lookups), {url: enhetsnavnUrl, body: 'Testenhet'}
        ]);
        const {page} = app;
        await submit(page, '#gruppe-form', 'Basis');
        await page.locator('.nytt-sok').click();
        await submit(page, '#gruppe-form-2', 'Sammenligning');
        await expect(page.locator('#sammenligning-tabell-overskrift th').last())
            .toHaveText('Ansatt er i permisjon');
        const cells = page.locator('#sammenligning-tabell-rader tr td:last-child');
        await expect(cells).toHaveText(error ? ['Oppslag feilet', 'Oppslag feilet'] : ['', '']);
        if (error) {
            await expect(cells.first()).toHaveClass(/error/);
            await expect(cells.first()).toHaveAttribute('title', error);
        }
        await page.locator('#kopier-detaljvisning').click();
        await expect(page.locator('#kopier-status')).toHaveText(error
            ? 'Kopiering feilet: Medlemskap er ikke ferdig hentet eller oppslaget feilet.'
            : 'Tabellen er kopiert til utklippstavlen.');
        if (!error) {
            expect(await page.evaluate(() => navigator.clipboard.readText()))
                .toContain('A123456;Testansatt A;1234;Testenhet;true;false');
        }
    });
}

for (const [id, response, extra, url] of [
    ['1234', nomAnsatte, [{url: enhetsnavnUrl, body: 'Testenhet'}], '/api/v1/nom/ansatte/1234'],
    ['ab123c', {ansatte: nomAnsatte, navn: 'Testenhet'}, [], '/api/v1/nom/ansatte/enhet/ab123c']
] satisfies [string, unknown, MockResponse[], string][]) {
    test(`group explorer preserves URL lookup for ${id}`, async ({app}) => {
        await app.open('grupper', [json(url, response), ...extra], `?tilgangsenhetId=${id}`);
        const {page} = app;
        await expect(page.locator('#nom-status')).toContainText('2 medlemmer i');
        await expect(page.locator('#nom-status')).toContainText('Testenhet');
        expect(new URL(app.calls[0].url()).pathname).toBe(url);
        await expect(page.locator('.nytt-sok')).toBeEnabled();
    });
}

for (const [name, response, message] of [
    ['HTTP error', json(basisUrl, {detail: 'Ingen tilgang'}, 403), 'Ingen tilgang'],
    ['invalid list', json(basisUrl, {}), 'Uventet respons: forventet en liste med medlemmer.'],
    ['invalid member', json(basisUrl, [{navIdent: 42, visningNavn: 'Ugyldig'}]), 'Uventet respons: mangler nav-ident eller navn.']
] satisfies [string, MockResponse, string][]) {
    test(`group explorer reports ${name}`, async ({app}) => {
        await app.open('grupper', [response]);
        const {page} = app;
        await submit(page, '#gruppe-form', 'Basis');
        await expect(page.locator('#status')).toHaveText(`Henting feilet: ${message}`);
        await expect(page.locator('#status')).toHaveClass(/error/);
        await expect(page.locator('#vis-medlemmer')).toBeEnabled();
    });
}

test('matrix requires a base group before searching memberships', async ({app}) => {
    await app.open('gruppematrise');
    await submit(app.page, '#sok-form', 'TEMA');
    expect(app.calls).toHaveLength(0);
    await expect(app.page.locator('#status')).toHaveText('Velg f\u00f8rst en gruppe som skal utforskes.');
});

test.describe('membership matrix', () => {
    test.beforeEach(async ({app}) => {
        await app.open('gruppematrise', [
            json(basisUrl, ansatte),
            json(nomBulkUrl, nomAnsatte),
            json(matriseUrl, [
                {ansatt: ansatte[0], grupper: ['TEMA 10', 'TEMA 2', 'OTHER']},
                {ansatt: ansatte[1], grupper: ['TEMA 2']},
                {ansatt: {navIdent: 'C123456'}, grupper: ['TEMA 3']}
            ]),
            json(permisjonUrl, [{navIdent: 'B123456'}])
        ]);
        await submit(app.page, '#gruppe-form', 'Basis');
        await submit(app.page, '#sok-form', 'TEMA');
        await expect(app.page.locator('#status')).toHaveText('2 ansatte og 3 grupper');
    });

    test('keeps base employees and filters group names', async ({app}) => {
        const {page, calls} = app;
        expect(calls).toHaveLength(4);
        const nomCall = calls.find(call => new URL(call.url()).pathname === nomBulkUrl);
        expect(nomCall?.postDataJSON()).toEqual(['A123456', 'B123456']);
        await expect(page.locator('#basis-rader tr')).toHaveCount(2);
        await expect(page.locator('#resultat')).toBeVisible();
        await expect(page.locator('#tabell-overskrift th')).toHaveText([
            'Nav-ident', 'Navn', 'Nom-enhet', 'Ansatt er i permisjon', 'TEMA 10', 'TEMA 2'
        ]);
        const rows = page.locator('#tabell-rader tr');
        await expect(rows).toHaveCount(2);
        await expect(rows.first().locator('td').nth(1)).toHaveText('1234');
        await expect(rows.first().locator('td').nth(3)).toHaveAttribute('aria-label', 'Medlem');
        await expect(rows.nth(1).locator('td').nth(3)).toHaveClass('nei');
        await expect(rows.first().locator('a')).toHaveAttribute('href', '/html/ansatt.html?ansattid=A123456');
    });

    test('sorts employees by ID', async ({app}) => {
        const sort = app.page.locator('#tabell-overskrift th').first().locator('button');
        await sort.click();
        await sort.click();
        await expect(app.page.locator('#tabell-rader tr').first().locator('th')).toHaveText('B123456');
    });

    test('copies employee memberships', async ({app}) => {
        await app.page.locator('#kopier-matrise').click();
        await expect(app.page.locator('#kopier-status')).toHaveText('Matrisen er kopiert til utklippstavlen.');
        const copied = await app.page.evaluate(() => navigator.clipboard.readText());
        expect(copied).toContain('A123456;Testansatt A;1234;false;true;true');
        expect(copied).toContain('B123456;Testansatt B;1234;true;false;true');
    });
});

for (const [id, response, url] of [
    ['1234', nomAnsatte, '/api/v1/nom/ansatte/1234'],
    ['ab123c', {ansatte: nomAnsatte, navn: 'Testenhet'}, '/api/v1/nom/ansatte/enhet/ab123c']
] satisfies [string, unknown, string][]) {
    test(`matrix supports Nom lookup for ${id} and empty results`, async ({app}) => {
        await app.open('gruppematrise', [
            json(url, response), json(nomBulkUrl, nomAnsatte),
            json('/api/v1/gruppe/medlemmer/sok/per-ansatt?gruppeNavn=MISSING', []), json(permisjonUrl, [])
        ]);
        const {page} = app;
        await submit(page, '#nom-form', id);
        expect(new URL(app.calls[0].url()).pathname).toBe(url);
        await expect(page.locator('#nom-status')).toContainText('2 ansatt');
        await submit(page, '#sok-form', 'MISSING');
        await expect(page.locator('#resultat')).toBeHidden();
        await expect(page.locator('#status')).toContainText('Ingen av de 2 ansatte');
    });
}

test('matrix displays lookup errors', async ({app}) => {
    await app.open('gruppematrise', [
        json(basisUrl, {detail: 'Ingen tilgang'}, 403),
        json(basisUrl, ansatte), json(nomBulkUrl, nomAnsatte),
        json(matriseUrl, {detail: 'S\u00f8k feilet'}, 500)
    ]);
    const {page} = app;
    await submit(page, '#gruppe-form', 'Basis');
    await expect(page.locator('#basis-status')).toHaveText('Henting feilet: Ingen tilgang');
    await submit(page, '#gruppe-form', 'Basis');
    await submit(page, '#sok-form', 'TEMA');
    await expect(page.locator('#status')).toHaveText('S\u00f8k feilet');
    await expect(page.locator('#status')).toHaveClass(/error/);
});

test('matrix displays clipboard errors through the visible copy button', async ({app}) => {
    await app.page.addInitScript(() => Object.defineProperty(navigator, 'clipboard', {value: undefined}));
    await app.open('gruppematrise', [
        json(basisUrl, ansatte), json(nomBulkUrl, nomAnsatte),
        json(matriseUrl, [{ansatt: ansatte[0], grupper: ['TEMA']}]),
        json(permisjonUrl, [])
    ]);
    const {page} = app;
    await submit(page, '#gruppe-form', 'Basis');
    await submit(page, '#sok-form', 'TEMA');
    await page.locator('#kopier-matrise').click();
    await expect(page.locator('#kopier-status')).toContainText('Utklippstavlen er ikke tilgjengelig');
    await expect(page.locator('#kopier-status')).toHaveClass(/error/);
    await expect(page.locator('#kopier-matrise')).toBeEnabled();
});

for (const name of ['grupper', 'gruppematrise']) {
    test(`${name} requires a Nom unit object containing an employee list`, async ({app}) => {
        await app.open(name, [json('/api/v1/nom/ansatte/enhet/ab123c', nomAnsatte)]);
        await submit(app.page, '#nom-form', 'ab123c');
        await expect(app.page.locator('#nom-status'))
            .toHaveText('Henting feilet: Uventet respons: forventet en liste med medlemmer.');
        await expect(app.page.locator('#nom-status')).toHaveClass(/error/);
    });
}
