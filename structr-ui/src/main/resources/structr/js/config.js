/*
 * Copyright (C) 2010-2017 Structr GmbH
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

function createNewEntry(e) {

	var currentTab = $('div.tab-content[aria-hidden=false]');
	if (currentTab) {

		var name = window.prompt("Please enter a key for the new configuration entry.");
		if (name && name.length) {

			currentTab.append(
				'<div class="form-group">' +
				'<label>' + name + '</label>' +
				'<input type="text" name="' + name + '"/>' +
				'<input type="hidden" name="' + name + '._settings_group" value="' + $(currentTab).attr('id') + '" />' +
				'</div>'
			);
		}
	}
}

function resetToDefault(key) {
	window.location.href = '/structr/config?reset=' + key;
}