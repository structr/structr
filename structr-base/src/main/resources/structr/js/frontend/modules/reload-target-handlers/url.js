/*
 * Copyright (C) 2010-2025 Structr GmbH
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

	getValue(obj, path) {

		let components = path
			.replace(/\["(\w+)"]/g, '.$1')   // convert ["index"] to .index
			.replace(/\['(\w+)']/g, '.$1')   // convert ['index'] to .index
			.replace(/\[(\d+)]/g, '.$1')     // convert numeric indexes [0] to .0
			.split('.');

		return components.reduce((acc, key) => acc && acc[key], obj);
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		let data = {
			result: parameters
		};

		// Evaluate and replace each {} expression
		let value = reloadTarget.replace(/{([^}]+)}/g, (match, cg1) => this.getValue(data, cg1));

		// go to URL
		window.location.href = value;
	}
}
