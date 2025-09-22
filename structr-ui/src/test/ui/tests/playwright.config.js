/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
// @ts-check
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({

    //sloMo: 100,

    use: {
        viewport: { width: 1920, height: 1080 },
        deviceScaleFactor: 2,
        video: {
            mode: 'on',
            //size: { width: 3840, height: 2160 }
            size: { width: 1920, height: 1080 }
        },
        showUserInput: true
    },

    workers: process.env.CI ? 1 : undefined,

    // Folder for test artifacts such as screenshots, videos, traces, etc.
    //outputDir: 'test-results',

    // path to the global setup files.
    //globalSetup: require.resolve('./global-setup'),

    // path to the global teardown files.
    //globalTeardown: require.resolve('./global-teardown'),

    // Each test is given 60 seconds.
    timeout: 60_000,

    // projects: [
    //     /* Test against desktop browsers */
    //     {
    //         name: 'chromium',
    //         use: { ...devices['Desktop Chrome'] },
    //     },
    //     {
    //         name: 'firefox',
    //         use: { ...devices['Desktop Firefox'] },
    //     },
    //     {
    //         name: 'webkit',
    //         use: { ...devices['Desktop Safari'] },
    //     },
    //     /* Test against mobile viewports. */
    //     {
    //         name: 'Mobile Chrome',
    //         use: { ...devices['Pixel 5'] },
    //     },
    //     {
    //         name: 'Mobile Safari',
    //         use: { ...devices['iPhone 12'] },
    //     },
    //     /* Test against branded browsers. */
    //     {
    //         name: 'Google Chrome',
    //         use: { ...devices['Desktop Chrome'], channel: 'chrome' }, // or 'chrome-beta'
    //     },
    //     {
    //         name: 'Microsoft Edge',
    //         use: { ...devices['Desktop Edge'], channel: 'msedge' }, // or 'msedge-dev'
    //     },
    // ]
});