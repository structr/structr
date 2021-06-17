/*
 * Copyright (C) 2010-2021 Structr GmbH
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

		let callbackObject = registeredDialogs[entity.type];

		if (!callbackObject && entity.isDOMNode) {
			callbackObject = registeredDialogs['DEFAULT_DOM_NODE'];
		}

		if (callbackObject) {

			var callback     = callbackObject.callback;
			var title        = callbackObject.title;
			var id           = callbackObject.id;

			if (callbackObject.condition === undefined || (typeof callbackObject.condition === 'function' && callbackObject.condition())) {

				// call method with the same callback object for initial callback and show callback
				_Entities.appendPropTab(entity, mainTabs, contentEl, id, title, true, callback, undefined, callback);

				return true;
			}
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
	showShowHideConditionOptions: function(el, entity) {

		let showConditionsContainer = $('.show-hide-conditions-container', el);

		if (showConditionsContainer.length) {

			Structr.fetchHtmlTemplate('dialogs/visibility-partial', { entity: entity }, function (html) {

				showConditionsContainer.html(html);

				_Dialogs.popuplateInputFields(showConditionsContainer, entity);
				_Dialogs.registerSimpleInputChangeHandlers(showConditionsContainer, entity);

				let showConditionsInput  = $('input#show-conditions', showConditionsContainer);
				let showConditionsSelect = $('select#show-conditions-templates', showConditionsContainer);
				let hideConditionsInput  = $('input#hide-conditions', showConditionsContainer);
				let hideConditionsSelect = $('select#hide-conditions-templates', showConditionsContainer);

				showConditionsSelect.on('change', () => {
					showConditionsInput.val(showConditionsSelect.val());
					showConditionsInput[0].dispatchEvent(new Event('change'));
				});
				hideConditionsSelect.on('change', () => {
					hideConditionsInput.val(hideConditionsSelect.val());
					hideConditionsInput[0].dispatchEvent(new Event('change'));
				});
			});
		}
	},
	showChildContentEditor:function(el, entity) {

		if (entity && entity.children && entity.children.length === 1 && entity.children[0].type === 'Content') {

			let textContentContainer = $('.show-text-content-container', el);
			if (textContentContainer.length) {

				Structr.fetchHtmlTemplate('dialogs/content-partial', { entity: entity }, function (html) {

					textContentContainer.html(html);

					let child = entity.children[0];

					_Dialogs.popuplateInputFields(textContentContainer, child);
					_Dialogs.registerSimpleInputChangeHandlers(textContentContainer, child, true);
				});
			}
		}
	},
	showRepeaterOptions: function(el, entity) {

		let repeaterConfigContainer = $('.repeater-config-container', el);

		if (repeaterConfigContainer.length) {

			Structr.fetchHtmlTemplate('dialogs/repeater-partial', { entity: entity }, function (html) {

				repeaterConfigContainer.html(html);

				_Dialogs.popuplateInputFields(repeaterConfigContainer, entity);
				_Dialogs.registerSimpleInputChangeHandlers(repeaterConfigContainer, entity);
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

				_Dialogs.registerSimpleInputChangeHandlers(el, entity);

				// dialog logic here..
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

				_Dialogs.focusInput(el);
			});

		} else if (el) {

			// update call
			$('input#ldap-group-dn').val(el.distinguishedName);
		}
	},
	fileDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/file.options', { file: entity, title: _Dialogs.getTitle() }, function (html) {

				el.empty();
				el.append(html);

				if (Structr.isModulePresent('text-search')) {

					$('#content-extraction').removeClass('hidden');

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
				}

				_Dialogs.popuplateInputFields(el, entity);
				_Dialogs.registerSimpleInputChangeHandlers(el, entity);
				Structr.activateCommentsInElement(el);

				_Dialogs.focusInput(el);
			});
		}
	},
	folderDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/folder.options', { file: entity, title: _Dialogs.getTitle() }, function (html) {

				el.empty();
				el.append(html);

				_Dialogs.popuplateInputFields(el, entity);
				_Dialogs.registerSimpleInputChangeHandlers(el, entity);
				Structr.activateCommentsInElement(el);

			});
		}
	},
	getValueFromFormElement: (el) => {

		if (el.tagName === 'SELECT' && el.multiple === true) {
			return [].map.call(el.selectedOptions, (o) => o.value);
		} else if (el.tagName === 'INPUT' && el.type === 'date') {
			return new Date(el.value);
		} else if (el.tagName === 'INPUT' && el.type === 'checkbox') {
			return el.checked;
		} else if (el.tagName === 'INPUT' && el.type === 'radio') {
			if (el.checked === true) {
				return el.value;
			} else {
				return null;
			}
		}

		return el.value;
	},
	registerSimpleInputChangeHandlers: function(el, entity, emptyStringInsteadOfNull) {

		for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

			inputEl.addEventListener('change', () => {

				let key = inputEl.name;

				let oldVal = entity[key];
				let newVal = _Dialogs.getValueFromFormElement(inputEl);

				let isChange = (oldVal !== newVal) && !((oldVal === null || oldVal === undefined) && newVal === '');
				if (isChange) {

					let blinkElement = (inputEl.type === 'checkbox') ? $(inputEl).parent() : null;

					_Entities.setPropertyWithFeedback(entity, key, newVal || (emptyStringInsteadOfNull ? '' : null), $(inputEl), blinkElement);
				}
			});
		}

	},
	popuplateInputFields: function (el, entity) {

		for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

			let val = entity[inputEl.name];
			if (val) {
				if (inputEl.type === 'checkbox') {
					inputEl.checked = val;
				} else {
					inputEl.value = val;
				}
			}
		}

	},
	aDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(aHtmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/a.options', { entity: entity, a: aHtmlProperties, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					_Dialogs.popuplateInputFields(el, aHtmlProperties);
					_Dialogs.registerSimpleInputChangeHandlers(el, aHtmlProperties);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(el, entity);
					_Dialogs.showShowHideConditionOptions(el, entity);

					// child content
					_Dialogs.showChildContentEditor(el, entity);
				});

			}, '_html_');
		}

	},
	buttonDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(buttonHtmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/button.options', { entity: entity, button: buttonHtmlProperties, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					_Dialogs.popuplateInputFields(el, buttonHtmlProperties);
					_Dialogs.registerSimpleInputChangeHandlers(el, buttonHtmlProperties);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(el, entity);
					_Dialogs.showShowHideConditionOptions(el, entity);

					// child content
					_Dialogs.showChildContentEditor(el, entity);
				});

			}, '_html_');
		}
	},
	inputDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(inputHtmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/input.options', { entity: entity, input: inputHtmlProperties, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					_Dialogs.popuplateInputFields(el, inputHtmlProperties);
					_Dialogs.registerSimpleInputChangeHandlers(el, inputHtmlProperties);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showShowHideConditionOptions(el, entity);
				});

			}, '_html_');
		}
	},
	divDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(divHtmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/div.options', { entity: entity, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					_Dialogs.popuplateInputFields(el, divHtmlProperties);
					_Dialogs.registerSimpleInputChangeHandlers(el, divHtmlProperties);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(el, entity);
					_Dialogs.showShowHideConditionOptions(el, entity);
				});

			}, '_html_');
		}
	},
	userDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/user.options', { entity: entity, user: entity, title: _Dialogs.getTitle() }, function (html) {

				el.empty();
				el.append(html);

				_Dialogs.popuplateInputFields(el, entity);
				_Dialogs.registerSimpleInputChangeHandlers(el, entity);

				$('button#set-password-button').on('click', function(e) {
					let input = $('input#password-input');
					_Entities.setPropertyWithFeedback(entity, 'password', input.val(), input);
				});

				_Dialogs.focusInput(el);

				_Dialogs.showCustomProperties(entity);
			});
		}
	},
	pageDialog: function(el, entity) {

		if (el && entity) {

			Structr.fetchHtmlTemplate('dialogs/page.options', { entity: entity, page: entity, title: _Dialogs.getTitle() }, function (html) {

				el.empty();
				el.append(html);

				_Dialogs.popuplateInputFields(el, entity);
				_Dialogs.registerSimpleInputChangeHandlers(el, entity);

				_Dialogs.focusInput(el);

				_Dialogs.showCustomProperties(entity);
			});
		}
	},
	defaultDomDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(htmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/default_dom.options', { entity: entity, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					_Dialogs.popuplateInputFields(el, htmlProperties);
					_Dialogs.registerSimpleInputChangeHandlers(el, htmlProperties);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(el, entity);
					_Dialogs.showShowHideConditionOptions(el, entity);

					// child content (optional)
					_Dialogs.showChildContentEditor(el, entity);
				});

			}, '_html_');
		}
	},
	contentDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(htmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/content.options', { entity: entity, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					_Dialogs.popuplateInputFields(el, htmlProperties);
					_Dialogs.registerSimpleInputChangeHandlers(el, htmlProperties);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(el, entity);
					_Dialogs.showShowHideConditionOptions(el, entity);
				});

			}, '_html_');
		}
	},
	optionDialog: function(el, entity) {

		if (el && entity) {

			Command.get(entity.id, null, function(divHtmlProperties) {

				Structr.fetchHtmlTemplate('dialogs/option.options', { entity: entity, title: _Dialogs.getTitle() }, function (html) {

					el.empty();
					el.append(html);

					let data = Object.assign({}, divHtmlProperties, entity);

					_Dialogs.popuplateInputFields(el, data);
					_Dialogs.registerSimpleInputChangeHandlers(el, data);

					_Dialogs.focusInput(el);

					_Dialogs.showCustomProperties(entity);
					_Dialogs.showRepeaterOptions(el, entity);
					_Dialogs.showShowHideConditionOptions(el, entity);

					// child content
					_Dialogs.showChildContentEditor(el, entity);
				});

			}, '_html_');
		}
	},

	focusInput: function(el, selector) {

		if (selector) {
			$(selector, el).focus().select();
		} else {
			$('input:first', el).focus().select();
		}
	}
};

var registeredDialogs = {
	'DEFAULT_DOM_NODE': { id: 'general', title : 'General', callback: _Dialogs.defaultDomDialog },
	'A': { id: 'general', title : 'General', callback: _Dialogs.aDialog },
	'Button': { id: 'general', title : 'General', callback: _Dialogs.buttonDialog },
	'Content': { id: 'general', title : 'General', callback: _Dialogs.contentDialog },
	'Div': { id: 'general', title : 'General', callback: _Dialogs.divDialog },
	'File':  { id: 'general', title: 'General', callback: _Dialogs.fileDialog },
	'Folder':  { id: 'general', title: 'General', callback: _Dialogs.folderDialog },
	'Image':  { id: 'general', title: 'Advanced', callback: _Dialogs.fileDialog },
	'Input':  { id: 'general', title: 'General', callback: _Dialogs.inputDialog },
	'LDAPGroup':  { id: 'general', title: 'LDAP configuration', callback: _Dialogs.ldapGroupDialog, condition: function() { return Structr.isModulePresent('ldap-client'); } },
	'Option':  { id: 'general', title: 'General', callback: _Dialogs.optionDialog },
	'Page': { id: 'general', title : 'General', callback: _Dialogs.pageDialog },
	'Template': { id: 'general', title : 'General', callback: _Dialogs.contentDialog },
	'User': { id: 'general', title : 'General', callback: _Dialogs.userDialog }
};

function setNull(id, key, input) {
	Command.setProperty(id, key, null, false, function() {
		input.val(null);
		blinkGreen(input);
		Structr.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
	});
}