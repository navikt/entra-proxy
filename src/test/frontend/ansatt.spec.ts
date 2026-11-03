import {test, expect, json, submit, type MockResponse} from './support';

const leder = {ressurs: {navident: 'L123456', visningsnavn: 'Testleder'}};
const enhet = {
    tilgangsenhetId: '1234',
    navn: 'Testenhet',
    id: 'ab123c',
    ledere: [leder]
};
const ansatt = {
    navident: 'A123456',
    visningsnavn: 'Testansatt',
    gjeldendeSektor: 'STAT',
    orgTilknytninger: [{orgEnhet: enhet}]
};

test('loads the compiled employee module over HTTP', async ({app}) => {
    await app.open('ansatt');
    const scripts = app.page.locator('script');
    await expect(scripts).toHaveCount(2);
    await expect(scripts.first()).toHaveAttribute('src', '/ansatt.js');
    await expect(scripts.first()).toHaveAttribute('type', 'module');
    const response = await app.page.request.get('/ansatt.js');
    expect(response.ok()).toBe(true);
    expect(response.headers()['content-type']).toContain('text/javascript');
});

test('renders employee, leader, units and group counts; filters and sorts groups', async ({app}) => {
    await app.open('ansatt', [
        json(ansatt),
        json([{rolle: 'Gruppe 10', gruppeId: 'g10'}, {rolle: 'Gruppe 2', gruppeId: 'g2'}]),
        json({g10: 12, g2: 0})
    ]);
    const {page, calls} = app;
    await submit(page, '#ansatt-form', 'a123456');
    expect(new URL(calls[0].url()).pathname).toBe('/api/v1/nom/enhet/A123456');
    expect(new URL(calls[1].url()).pathname).toBe('/api/v1/ansatt/tilganger/A123456');
    expect(new URL(calls[2].url()).pathname).toBe('/api/v1/gruppe/antall/bulk');
    expect(calls[2].method()).toBe('POST');
    expect(calls[2].postDataJSON()).toEqual(['g10', 'g2']);
    expect(calls[2].headers()['accept']).toBe('application/json');
    expect(calls[2].headers()['content-type']).toBe('application/json');
    await expect(page.locator('#status')).toHaveText('Ansatt hentet.');
    await expect(page.locator('#ansatt-resultat')).toBeVisible();
    await expect(page.locator('#enheter-resultat')).toBeVisible();
    await expect(page.locator('#grupper-resultat')).toBeVisible();
    await expect(page.locator('#ansatt-felter')).toContainText('Testansatt');
    await expect(page.locator('#ansatt-felter .navident-lenke'))
        .toHaveAttribute('href', 'https://nom.nav.no/ressurs/L123456');
    await expect(page.locator('.enhetsnummer-lenke'))
        .toHaveAttribute('href', '/html/grupper.html?tilgangsenhetId=1234');
    await expect(page.locator('#enheter-ledere-kolonne')).toBeHidden();
    const rows = page.locator('#grupper-rader tr');
    await expect(rows.nth(0).locator('td').nth(3)).toHaveText('12');
    await expect(rows.nth(1).locator('td').nth(3)).toHaveText('0');
    const sort = page.locator('[data-kolonne="0"]');
    await sort.click();
    await expect(rows.first().locator('td').first()).toHaveText('Gruppe 2');
    await expect(page.locator('th[aria-sort="ascending"]')).toContainText('Gruppenavn');
    await sort.click();
    await expect(rows.first().locator('td').first()).toHaveText('Gruppe 10');
    await page.locator('#gruppe-filter').fill(' GRUPPE 2 ');
    await expect(rows.nth(0)).toBeHidden();
    await expect(rows.nth(1)).toBeVisible();
    await expect(page.locator('#grupper-status')).toHaveText('1 av 2 grupper');
    await page.locator('[data-kolonne="3"]').click();
    await expect(rows.first().locator('td').nth(3)).toHaveText('0');
    await expect(page.locator('th[aria-sort]')).toHaveCount(1);
    await expect(page.locator('th[aria-sort]')).toContainText('Antall medlemmer');
});

test('URL lookup displays leaders per unit for multiple affiliations', async ({app}) => {
    await app.open('ansatt', [
        json({...ansatt, orgTilknytninger: [{orgEnhet: enhet}, {orgEnhet: {...enhet, ledere: null}}]}),
        json([])
    ], '?ansattid=A123456');
    const {page} = app;
    await expect(page.locator('#status')).toHaveText('Ansatt hentet.');
    expect(app.calls).toHaveLength(2);
    await expect(page.locator('#enheter-ledere-kolonne')).toBeVisible();
    await expect(page.locator('#ansatt-felter tr')).toHaveCount(3);
    await expect(page.locator('#enheter-rader tr').first().locator('td').nth(3)).toBeVisible();
    await expect(page.locator('#enheter-rader')).toContainText('Testleder');
    await expect(page.locator('#grupper-status')).toHaveText('Ingen gruppemedlemskap');
});

for (const [name, response] of [
    ['empty', {body: ''}],
    ['null', json(null)]
] satisfies [string, MockResponse][]) {
    test(`${name} employee responses leave results hidden and restore controls`, async ({app}) => {
        await app.open('ansatt', [response]);
        const {page} = app;
        await submit(page, '#ansatt-form', 'a123456');
        expect(app.calls).toHaveLength(1);
        await expect(page.locator('#status')).toHaveText('Ingen ansatt funnet.');
        await expect(page.locator('#ansatt-resultat')).toBeHidden();
        await expect(page.locator('#sok-ansatt')).toBeEnabled();
        await expect(page.locator('html')).not.toHaveClass(/soker/);
    });
}

for (const [name, response, message] of [
    ['problem detail', json({detail: 'Ingen tilgang'}, 403), 'Henting feilet: Ingen tilgang'],
    ['HTTP error', {status: 500, body: ''}, 'Henting feilet: Kunne ikke hente nom-data (HTTP 500).'],
    ['network error', {abort: true}, 'Henting feilet: Failed to fetch']
] satisfies [string, MockResponse, string][]) {
    test(`employee ${name} is visible and restores controls`, async ({app}) => {
        await app.open('ansatt', [response]);
        const {page} = app;
        await submit(page, '#ansatt-form', 'a123456');
        await expect(page.locator('#status')).toHaveText(message);
        await expect(page.locator('#status')).toHaveClass(/error/);
        await expect(page.locator('#sok-ansatt')).toBeEnabled();
        await expect(page.locator('html')).not.toHaveClass(/soker/);
    });
}

test('scrolling starts above 50 groups and a new lookup clears prior results', async ({app}) => {
    await app.open('ansatt', [
        json(ansatt), json(Array.from({length: 50}, (_, i) => ({rolle: `Gruppe ${i}`}))),
        json(ansatt), json(Array.from({length: 51}, (_, i) => ({rolle: `Gruppe ${i}`}))),
        json({...ansatt, orgTilknytninger: []}), json([])
    ]);
    const {page} = app;
    const table = page.locator('.grupper-tabell');
    await submit(page, '#ansatt-form', 'a123456');
    await expect(page.locator('#grupper-rader tr')).toHaveCount(50);
    await expect(table).not.toHaveClass(/med-rullefelt/);
    await submit(page, '#ansatt-form', 'a123456');
    await expect(page.locator('#grupper-rader tr')).toHaveCount(51);
    await expect(table).toHaveClass(/med-rullefelt/);
    expect(await table.evaluate(element => element.scrollHeight > element.clientHeight)).toBe(true);
    await page.locator('[data-kolonne="0"]').click();
    await submit(page, '#ansatt-form', 'a123456');
    await expect(table).not.toHaveClass(/med-rullefelt/);
    await expect(page.locator('#grupper-rader tr')).toHaveCount(0);
    await expect(page.locator('#enheter-rader tr')).toHaveCount(0);
    await expect(page.locator('#ansatt-felter tr')).toHaveCount(3);
    await expect(page.locator('[aria-sort]')).toHaveCount(0);
});

test('browser validation blocks missing and malformed employee IDs', async ({app}) => {
    await app.open('ansatt');
    const {page} = app;
    await page.locator('#sok-ansatt').click();
    await expect(page.locator('#ansattid:invalid')).toBeFocused();
    await page.locator('#ansattid').fill('1234567');
    await page.locator('#sok-ansatt').click();
    await expect(page.locator('#ansattid:invalid')).toBeFocused();
    expect(app.calls).toHaveLength(0);
    await expect(page.locator('#status')).toBeEmpty();
});
