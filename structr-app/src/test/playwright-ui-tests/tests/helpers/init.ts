///
/// Copyright (C) 2010-2026 Structr GmbH
///
/// This file is part of Structr <http://structr.org>.
///
/// Structr is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as
/// published by the Free Software Foundation, either version 3 of the
/// License, or (at your option) any later version.
///
/// Structr is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with Structr.  If not, see <http://www.gnu.org/licenses/>.
///

// @ts-check
export async function initialize(playwright, createData) {

    const context = await playwright.request.newContext({
        extraHTTPHeaders: {
            'Accept': 'application/json',
            'X-User': 'superadmin',
            'X-Password': process.env.SUPERUSER_PASSWORD,
        }
    });

    // clear database
    await context.post(process.env.BASE_URL + '/structr/rest/maintenance/clearDatabase');

    // create admin user
    await context.post(process.env.BASE_URL + '/structr/rest/User', {
        data: JSON.stringify({
            name: 'admin',
            password: 'admin',
            isAdmin: true
        })
    });

    // create everything in createData
    if (createData) {

        for (let type in createData) {

            await context.post(process.env.BASE_URL + `/structr/rest/${type}`, {
                data: JSON.stringify(createData[type]),
            });
        }
    }

    return context;
}
