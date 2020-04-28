/*
 * Copyright (C) 2010-2020 Structr GmbH
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

	getTitle: function() {
		return "Main Properties";
	},
	showCustomProperties: function(entity) {

		// custom properties
		let customContainer = $('div#custom-properties-container');

		_Schema.getTypeInfo(entity.type, function(typeInfo) {

			_Entities.listProperties(entity, 'custom', customContainer, typeInfo, function(properties) {

				// make container visible when custom properties exist
				if (Object.keys(properties).length > 0) {
					$('div#custom-properties-parent').removeClass("hidden");
				}

				$('input.dateField', customContainer).each(function(i, input) {
					_Entities.activateDatePicker($(input));
				});
			});
		});
	},
	showVisibilityOptions: function(entity, el) {

		let showConditionsContainer = $('.show-hide-conditions-container', el);

		if (showConditionsContainer.length) {

			Structr.fetchHtmlTemplate('dialogs/visibility-partial', { entity: entity }, function (html) {

				showConditionsContainer.html(html);

				let showConditionsInput  = $('input#show-conditions');
				let showConditionsSelect = $('select#show-conditions-templates');
				let hideConditionsInput  = $('input#hide-conditions');
				let hideConditionsSelect = $('select#hide-conditions-templates');

				showConditionsInput.on('change', () => { _Entities.setPropertyWithFeedback(entity, 'showConditions', showConditionsInput.val(), showConditionsInput); });
				hideConditionsInput.on('change', () => { _Entities.setPropertyWithFeedback(entity, 'hideConditions', hideConditionsInput.val(), hideConditionsInput); });

				showConditionsSelect.on('change', () => {
					showConditionsInput.val(showConditionsSelect.val()).trigger('change');
				});
				hideConditionsSelect.on('change', () => {
					hideConditionsInput.val(hideConditionsSelect.val()).trigger('change');
				});
			});
		}
	},
	showRepeaterOptions: function(entity, el) {

		let repeaterConfigContainer = $('.repeater-config-container', el);

		if (repeaterConfigContainer.length) {

			Structr.fetchHtmlTemplate('dialogs/repeater-partial', { entity: entity }, function (html) {

				repeaterConfigContainer.html(html);

				[ 'function-query', 'data-key' ].forEach(p => {
					_Dialogs.registerSimpleInputBlurhandler($('input#' + p + '-input'), entity, p.toCamel());
				});
			});
		}
	},

	// ----- custom dialogs -----
	ldapGroupDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/ldap.group', { group: entity }, function (html) {

				el.empty();
				el.append(html);

				var dnInput     = $('input#ldap-group-dn');
				var pathInput   = $('input#ldap-group-path');
				var filterInput = $('input#ldap-group-filter');
				var scopeInput  = $('input#ldap-group-scope');

				// dialog logic here..
				dnInput.on('blur', function() { _Entities.setPropertyWithFeedback(entity, 'distinguishedName', dnInput.val(), dnInput); });
				pathInput.on('blur', function() { _Entities.setPropertyWithFeedback(entity, 'path', pathInput.val(), pathInput); });
				filterInput.on('blur', function() { _Entities.setPropertyWithFeedback(entity, 'filter', filterInput.val(), filterInput); });
				scopeInput.on('blur', function() { _Entities.setPropertyWithFeedback(entity, 'scope', scopeInput.val(), scopeInput); });

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
	fileDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/file.options', { file: entity }, function (html) {

				el.empty();
				el.append(html);

				$('button#extract-structure-button').on('click', function() {

					Structr.showAndHideInfoBoxMessage('Extracting structure..', 'info', 2000, 200);

					$.ajax({
						url: '/structr/rest/' + entity.type + '/' + entity.id + '/extractStructure',
						method: 'post',
						statusCode: {
							200: function() {
								Structr.showAndHideInfoBoxMessage('Structure extracted, see Contents area.', 'success', 2000, 200);
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
	registerSimpleInputBlurhandler: function(input, entity, key) {

		input.on('blur', function() {

			let oldVal = entity[key];
			let newVal = input.val();

			let isChange = (oldVal !== newVal) && !((oldVal === null || oldVal === undefined) && newVal === '');

			if (isChange) {
				_Entities.setPropertyWithFeedback(entity, key, input.val() || null, input);
			}
		});

	},
	aDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(a) {

				Structr.fetchHtmlTemplate('dialogs/a.options', { entity: entity, a: a, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					[ 'id', 'class', 'href', 'style' ].forEach(p => {
						_Dialogs.registerSimpleInputBlurhandler($('input#' + p + '-input'), a, '_html_' + p);
					});

					// focus on first input field
					$('input#class-input').focus();
					$('input#class-input').select();

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(entity, el);
					_Dialogs.showVisibilityOptions(entity, el);
				});

			}, '_html_');
		}

	},
	buttonDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(button) {

				Structr.fetchHtmlTemplate('dialogs/button.options', { entity: entity, button: button, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					[ 'id', 'class', 'onclick', 'title', 'type', 'style' ].forEach(p => {
						_Dialogs.registerSimpleInputBlurhandler($('input#' + p + '-input'), button, '_html_' + p);
					});

					// focus on first input field
					$('input#class-input').focus();
					$('input#class-input').select();

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(entity, el);
					_Dialogs.showVisibilityOptions(entity, el);
				});

			}, '_html_');
		}
	},
	inputDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(input) {

				Structr.fetchHtmlTemplate('dialogs/input.options', { entity: entity, input: input, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					[ 'id', 'class', 'title', 'placeholder', 'type', 'style' ].forEach(p => {
						let input = $('input#' + p + '-input');
						_Dialogs.registerSimpleInputBlurhandler(input, entity, '_html_' + p);
					});

					// focus on first input field
					$('input#class-input').focus();
					$('input#class-input').select();

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showVisibilityOptions(entity, el);
				});

			}, '_html_');
		}
	},
	divDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(div) {

				Structr.fetchHtmlTemplate('dialogs/div.options', { entity: entity, div: div, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					[ 'id', 'class', 'style' ].forEach(p => {
						_Dialogs.registerSimpleInputBlurhandler($('input#' + p + '-input'), div, '_html_' + p);
					});

					// focus on first input field
					$('input#class-input').focus();
					$('input#class-input').select();

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(entity, el);
					_Dialogs.showVisibilityOptions(entity, el);
				});

			}, '_html_');
		}
	},
	userDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(div) {

				Structr.fetchHtmlTemplate('dialogs/user.options', { entity: entity, user: entity, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					[ 'name', 'e-mail' ].forEach(p => {
						let input = $('input#' + p + '-input');
						input.on('blur', function() { _Entities.setPropertyWithFeedback(entity, p.toCamel(), input.val() || null, input); });
					});

					[ 'is-admin', 'is-two-factor-user', 'skip-security-relationships' ].forEach(p => {
						let name  = p.toCamel();
						let input = $('input#' + p + '-checkbox');
						input.prop('checked', entity[name]);
						input.on('change', function() { _Entities.setPropertyWithFeedback(entity, name, input.is(':checked'), input); });
					});

					$('button#set-password-button').on('click', function(e) {
						let input = $('input#password-input');
						_Entities.setPropertyWithFeedback(entity, 'password', input.val(), input);
					});

					// focus on first input field
					$('input#name-input').focus();
					$('input#name-input').select();

					_Dialogs.showCustomProperties(entity);
				});

			}, '_html_');
		}
	},
	pageDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(page) {

				Structr.fetchHtmlTemplate('dialogs/page.options', { entity: entity, page: entity, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					[ 'name', 'content-type', 'category', 'position', 'show-on-error-codes' ].forEach(p => {
						let input = $('input#' + p + '-input');
						_Dialogs.registerSimpleInputBlurhandler(input, entity, p.toCamel());
					});

					[ 'dont-cache', 'page-creates-raw-data' ].forEach(p => {
						let name  = p.toCamel();
						let input = $('input#' + p + '-checkbox');
						input.prop('checked', entity[name]);
						input.on('change', function() { _Entities.setPropertyWithFeedback(entity, name, input.is(':checked'), input); });
					});

					$('button#set-password-button').on('click', function(e) {
						let input = $('input#password-input');
						_Entities.setPropertyWithFeedback(entity, 'password', input.val(), input);
					});

					// focus on first input field
					$('input#name-input').focus();
					$('input#name-input').select();

					_Dialogs.showCustomProperties(entity);
				});

			}, '_html_');
		}
	}
};

var registeredDialogs = {
	'A': { id: 'a', title : 'General', callback: _Dialogs.aDialog },
	'Button': { id: 'button', title : 'General', callback: _Dialogs.buttonDialog },
	'Div': { id: 'div', title : 'General', callback: _Dialogs.divDialog },
	'File':  { id: 'file', title: 'Advanced', callback: _Dialogs.fileDialog },
	'Image':  { id: 'file', title: 'Advanced', callback: _Dialogs.fileDialog },
	'Input':  { id: 'input', title: 'General', callback: _Dialogs.inputDialog },
	'LDAPGroup':  { id: 'ldapgroup', title: 'LDAP configuration', callback: _Dialogs.ldapGroupDialog },
	'Page': { id: 'page', title : 'General', callback: _Dialogs.pageDialog },
	'User': { id: 'user', title : 'General', callback: _Dialogs.userDialog }
};

function setNull(id, key, input) {
	Command.setProperty(id, key, null, false, function() {
		input.val(null);
		blinkGreen(input);
		Structr.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
	});
}