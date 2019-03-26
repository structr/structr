/*
 * Copyright (C) 2010-2019 Structr GmbH
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
var _Dialogs = {

	findAndAppendCustomTypeDialog: function(entity, mainTabs, contentEl) {

		var callbackObject = registeredDialogs[entity.type];
		if (callbackObject) {

			var callback     = callbackObject.callback;
			var title        = callbackObject.title;
			var id           = callbackObject.id;

			// call method with the same callback object for intial callback and show callback
			_Entities.appendPropTab(entity, mainTabs, contentEl, id, title, true, callback, undefined, callback);

			return true;
		}

		return false;
	},

	// ----- custom dialogs -----
	ldapGroupDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/ldap.group', { group: entity }, function (html) {

				el.append(html);

				var dnInput     = $('input#ldap-group-dn');
				var pathInput   = $('input#ldap-group-path');
				var filterInput = $('input#ldap-group-filter');
				var scopeInput  = $('input#ldap-group-scope');

				// dialog logic here..
				dnInput.on('change', function() { _Entities.setPropertyWithFeedback(entity, 'distinguishedName', dnInput.val(), dnInput); });
				pathInput.on('change', function() { _Entities.setPropertyWithFeedback(entity, 'path', pathInput.val(), pathInput); });
				filterInput.on('change', function() { _Entities.setPropertyWithFeedback(entity, 'filter', filterInput.val(), filterInput); });
				scopeInput.on('change', function() { _Entities.setPropertyWithFeedback(entity, 'scope', scopeInput.val(), scopeInput); });

				$('i#clear-ldap-group-dn').on('click', function() { setNull(entity.id, 'distinguishedName', dnInput); });
				$('i#clear-ldap-group-path').on('click', function() { setNull(entity.id, 'path', pathInput); });
				$('i#clear-ldap-group-filter').on('click', function() { setNull(entity.id, 'filter', filterInput); });
				$('i#clear-ldap-group-scope').on('click', function() { setNull(entity.id, 'scope', scopeInput); });

				$('button#ldap-sync-button').on('click', function() {

					$.ajax({
						url: '/structr/rest/' + entity.type + '/' + entity.id + '/update',
						method: 'post',
						statusCode: {
							200: function() {
								Structr.showAndHideInfoBoxMessage('Updated LDAP group successfully', 'success', 2000, 200);
							}
						}
					});
				});

			});

		} else if (el) {

			// update call
			$('input#ldap-group-dn').val(el.distinguishedName);
		}
	},
}

var registeredDialogs = {
	'LDAPGroup':  { id: 'ldapgroup', title: 'LDAP configuration', callback: _Dialogs.ldapGroupDialog }

}

function setNull(id, key, input) {
	Command.setProperty(id, key, null, false, function() {
		input.val(null);
		blinkGreen(input);
		Structr.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
	});
}