import {test as base, expect, type Page, type Request} from '@playwright/test';

export interface MockResponse {
    json?: unknown;
    body?: string;
    status?: number;
    abort?: boolean;
    headers?: Record<string, string>;
}

interface App {
    page: Page;
    calls: Request[];
    open(name: string, responses?: MockResponse[], query?: string): Promise<void>;
}

export function json(value: unknown, status = 200): MockResponse {
    return {json: value, status};
}

export const test = base.extend<{app: App}>({
    app: async ({page, baseURL}, use) => {
        const errors: string[] = [];
        const calls: Request[] = [];
        let responses: MockResponse[] = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.route('**/*', route =>
            new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort()
        );
        await page.route('**/api/**', async route => {
            calls.push(route.request());
            const response = responses.shift();
            if (!response) {
                errors.push(`Unexpected API request: ${route.request().url()}`);
                await route.abort();
            } else if (response.abort) {
                await route.abort('failed');
            } else {
                await route.fulfill({
                    status: response.status ?? 200,
                    headers: response.headers,
                    ...('json' in response
                        ? {json: response.json}
                        : {body: response.body ?? '', contentType: 'text/plain'})
                });
            }
        });
        await use({
            page,
            calls,
            async open(name, nextResponses = [], query = '') {
                expect(responses, 'Unconsumed mock responses before navigation').toHaveLength(0);
                responses = [...nextResponses];
                calls.length = 0;
                await page.goto(`/${name}.html${query}`);
            }
        });
        expect(errors, 'Browser errors and unexpected API requests').toEqual([]);
        expect(responses, 'Unconsumed mock responses').toHaveLength(0);
    }
});

export {expect};

export async function submit(page: Page, selector: string, value: string): Promise<void> {
    const form = page.locator(selector);
    await form.locator('input').fill(value);
    await form.locator('button[type="submit"]').click();
    await expect(page.locator('html')).not.toHaveClass(/soker/);
}
