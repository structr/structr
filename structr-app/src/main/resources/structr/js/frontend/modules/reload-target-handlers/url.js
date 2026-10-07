/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		let data = {
			result: parameters
		};

        // empty value? preserve request parameters
        if (reloadTarget.length === 0) {

            if (options.updateHistory) {
                let url = new URL(window.location.href);
                for (const key in parameters) {
                    if (key === 'current') {
                        url.pathname = url.pathname.split('/').toSpliced(2, 1, parameters[key]).join('/');
                    } else {
                        url.searchParams.set(key, parameters[key]);
                    }
                }
                reloadTarget = url.toString();
            }

        } else {

            // Evaluate and replace each {} expression
            reloadTarget = reloadTarget.replace(/{([^}]+)}/g, (match, cg1) => this.frontendModule.resolvePlaceholder(data, cg1));
        }

		// go to URL
		window.location.href = reloadTarget;
	}
}
