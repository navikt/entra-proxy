import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
    testDir: './src/test/frontend',
    tsconfig: './tsconfig.test.json',
    fullyParallel: true,
    forbidOnly: Boolean(process.env.CI),
    workers: 2,
    reporter: 'list',
    outputDir: 'build/playwright-results',
    use: {
        baseURL: 'http://127.0.0.1:4173',
        trace: 'retain-on-failure',
        permissions: ['clipboard-read', 'clipboard-write']
    },
    projects: [{name: 'chromium', use: {...devices['Desktop Chrome']}}],
    webServer: {
        command: 'node src/test/frontend/server.cjs',
        url: 'http://127.0.0.1:4173',
        reuseExistingServer: false
    }
});
