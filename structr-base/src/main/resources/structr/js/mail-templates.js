/*
 * Copyright (C) 2010-2023 Structr GmbH
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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_MailTemplates);
});

let _MailTemplates = {
	_moduleName: 'mail-templates',

	mailTemplatesPager: undefined,
	mailTemplatesList: undefined,
	mailTemplateDetailContainer: undefined,
	mailTemplateDetailForm: undefined,
	previewElement: undefined,

	mailTemplatesResizerLeftKey:     'structrMailTemplatesResizerLeftKey_' + location.port,
	mailTemplateSelectedElementKey:  'structrMailTemplatesSelectedElementKey_' + location.port,
	mailTemplatesPreselectLocaleKey: 'structrMailTemplatesPreselectLocaleKey_' + location.port,

	init: () => {},
	unload: () => {
		_Editors.disposeAllEditors();
	},
	onload: () => {

		_MailTemplates.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('mail-templates'));

		Structr.setMainContainerHTML(_MailTemplates.templates.main());
		Structr.setFunctionBarHTML(_MailTemplates.templates.functions());

		// UISettings.showSettingsForCurrentModule();

		let newMailTemplateForm = Structr.functionBar.querySelector('#create-mail-template-form');
		let namePreselect       = document.getElementById('mail-template-name-preselect');
		let localePreselect     = document.getElementById('mail-template-locale-preselect');

		localePreselect.value = LSWrapper.getItem(_MailTemplates.mailTemplatesPreselectLocaleKey) || 'en';

		newMailTemplateForm.addEventListener('submit', async (e) => {
			e.preventDefault();

			_MailTemplates.showMain();

			let preselectLocalesString = localePreselect.value.trim();

			LSWrapper.setItem(_MailTemplates.mailTemplatesPreselectLocaleKey, preselectLocalesString);

			let mtData = preselectLocalesString.split(',').map((l) => {
				return {
					name: namePreselect.value,
					locale: l.trim()
				}
			});

			let response = await _MailTemplates.createMailTemplates(mtData, newMailTemplateForm);
		});

		let createRegistrationTemplatesLink = Structr.functionBar.querySelector('#create-registration-templates');
		createRegistrationTemplatesLink.addEventListener('click', async (e) => {
			e.preventDefault();

			let defaultRegistrationTemplates = [
				{ name: 'CONFIRM_REGISTRATION_SENDER_ADDRESS',	text: 'structr-mail-daemon@localhost' },
				{ name: 'CONFIRM_REGISTRATION_SENDER_NAME',		text: 'Structr Mail Daemon' },
				{ name: 'CONFIRM_REGISTRATION_SUBJECT',			text: 'Welcome to Structr, please finalize registration' },
				{ name: 'CONFIRM_REGISTRATION_TEXT_BODY',		text: 'Go to ${link} to finalize registration.' },
				{ name: 'CONFIRM_REGISTRATION_HTML_BODY',		text: '<div>Click <a href=\'${link}\'>here</a> to finalize registration.</div>' },
				{ name: 'CONFIRM_REGISTRATION_TARGET_PAGE',		text: '/register_thanks' },
				{ name: 'CONFIRM_REGISTRATION_ERROR_PAGE',		text: '/register_error' },
			];

			let mtData = localePreselect.value.trim().split(',').map((l) => {
				return defaultRegistrationTemplates.map(mt => Object.assign({ locale: l.trim() }, mt));
			});
			mtData = [].concat(...mtData);

			let response = await _MailTemplates.createMailTemplates(mtData, createRegistrationTemplatesLink);
		});

		let createResetPasswordTemplatesLink = Structr.functionBar.querySelector('#create-reset-password-templates');
		createResetPasswordTemplatesLink.addEventListener('click', async (e) => {
			e.preventDefault();

			let defaultResetPasswordTemplates = [
				{ name: 'RESET_PASSWORD_SENDER_ADDRESS',	text: 'structr-mail-daemon@localhost' },
				{ name: 'RESET_PASSWORD_SENDER_NAME',		text: 'Structr Mail Daemon' },
				{ name: 'RESET_PASSWORD_SUBJECT',			text: 'Request to reset your Structr password' },
				{ name: 'RESET_PASSWORD_TEXT_BODY',			text: 'Go to ${link} to reset your password.' },
				{ name: 'RESET_PASSWORD_HTML_BODY',			text: '<div>Click <a href=\'${link}\'>here</a> to reset your password.</div>' },
				{ name: 'RESET_PASSWORD_TARGET_PAGE',		text: '/reset-password' }
			];

			let mtData = localePreselect.value.trim().split(',').map((l) => {
				return defaultResetPasswordTemplates.map(mt => Object.assign({ locale: l.trim() }, mt));
			});
			mtData = [].concat(...mtData);

			let response = await _MailTemplates.createMailTemplates(mtData, createResetPasswordTemplatesLink);
		});

		_MailTemplates.mailTemplatesList           = document.querySelector('#mail-templates-table tbody');
		_MailTemplates.mailTemplateDetailContainer = document.getElementById('mail-templates-detail-container');
		_MailTemplates.mailTemplateDetailForm      = document.getElementById('mail-template-detail-form');
		_MailTemplates.previewElement              = document.getElementById('mail-template-preview');

		_MailTemplates.listMailTemplates();

		_MailTemplates.mailTemplateDetailForm.addEventListener('submit', (e) => {
			e.preventDefault();
			_MailTemplates.saveMailTemplate();
		})

		Structr.mainMenu.unblock(100);

		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer'), _MailTemplates.mailTemplatesResizerLeftKey, 300, _MailTemplates.moveResizer);

		_MailTemplates.moveResizer(LSWrapper.getItem(_MailTemplates.mailTemplatesResizerLeftKey));
	},
	getContextMenuElements: (div, entity) => {

		let elements = [];

		elements.push({
			name: 'Properties',
			clickHandler: () => {
				_Entities.showProperties(entity, 'ui');
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Mail Template',
			clickHandler: () => {

				_Entities.deleteNode(entity, false, () => {

					let lastSelectedMailTemplateId = LSWrapper.getItem(_MailTemplates.mailTemplateSelectedElementKey);
					if (lastSelectedMailTemplateId === entity.id) {
						LSWrapper.removeItem(_MailTemplates.mailTemplateSelectedElementKey);
						_MailTemplates.mailTemplateDetailContainer.style.display = 'none';
					}

					_MailTemplates.mailTemplatesPager.refresh();

					let row = Structr.node(entity.id, '#mail-template-');
					if (row) {
						row.remove();
						window.setTimeout(_MailTemplates.checkMainVisibility, 200);
					}
				});
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	showMain: () => {
		document.getElementById('mail-templates-main').style.display = 'flex';
		_MailTemplates.moveResizer();
	},
	hideMain: () => {
		document.getElementById('mail-templates-main').style.display = 'none';
	},
	checkMainVisibility: () => {

		let rows = document.querySelectorAll('.mail-template-row');
		let selectedRowExists = false;
		rows.forEach((row) => {
			selectedRowExists |= row.classList.contains('selected');
		});

		if (!rows || rows.length === 0) {
			_MailTemplates.hideMain();
		} else if (!selectedRowExists) {
			_MailTemplates.mailTemplateDetailContainer.style.display = 'none';
		}
	},
	resize: () => {
		_MailTemplates.moveResizer();
	},
	prevAnimFrameReqId_moveResizer: undefined,
	moveResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_MailTemplates.prevAnimFrameReqId_moveResizer, () => {

			left = left || LSWrapper.getItem(_MailTemplates.mailTemplatesResizerLeftKey) || 300;
			left = Math.max(300, Math.min(left, window.innerWidth - 300));

			document.getElementById('mail-templates-list-container').style.width = `calc(${left}px - 1rem)`;
			document.querySelector('.column-resizer').style.left                 = `${left}px`;
			_MailTemplates.mailTemplateDetailContainer.style.width                        = `calc(100% - ${left}px - 3rem)`;

			_Editors.resizeVisibleEditors();

			return true;
		});
	},
	listMailTemplates: () => {

		let pagerEl = document.querySelector('#mail-templates-pager');

		_Pager.initPager('mail-templates', 'MailTemplate', 1, 25, 'name', 'asc');

		_MailTemplates.mailTemplatesPager = _Pager.addPager('mail-templates', pagerEl, false, 'MailTemplate', 'ui', _MailTemplates.processPagerData, undefined, undefined, true);

		_MailTemplates.mailTemplatesPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(_MailTemplates.mailTemplatesList);
		};
		_MailTemplates.mailTemplatesPager.appendFilterElements(`
			<span class="mr-1">Filters:</span>
			<input type="text" class="filter w100 mail-template-name" data-attribute="name" placeholder="Name">
			<input type="text" class="filter w100 mail-template-locale" data-attribute="locale" placeholder="Locale">
		`);
		_MailTemplates.mailTemplatesPager.activateFilterElements();
		_MailTemplates.mailTemplatesPager.setIsPaused(false);
		_MailTemplates.mailTemplatesPager.refresh();
	},
	processPagerData: (pagerData) => {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_MailTemplates.appendMailTemplateToList);
		}
	},
	appendMailTemplateToList: (mailTemplate) => {

		_MailTemplates.showMain();

		let html = _MailTemplates.templates.typeRow({ mailTemplate: mailTemplate });
		let row  = _Helpers.createSingleDOMElementFromHTML(html);

		_MailTemplates.populateMailTemplatePagerRow(row, mailTemplate);
		_MailTemplates.mailTemplatesList.appendChild(row);

		row.addEventListener('click', () => {

			// we are re-using the UI, so dispose previous editor (in this case we only have one editor, so we can simply use the kill all method)
			_Editors.disposeAllEditors();

			_MailTemplates.selectRow(mailTemplate.id);
			_MailTemplates.showMailTemplateDetails(mailTemplate.id);
		});

		_Elements.contextMenu.enableContextMenuOnElement(row, mailTemplate);
		_Entities.appendContextMenuIcon(row.querySelector('.icons-container'), mailTemplate, true);

		let lastSelectedMailTemplateId = LSWrapper.getItem(_MailTemplates.mailTemplateSelectedElementKey);
		if (lastSelectedMailTemplateId === mailTemplate.id) {
			row.click();
		}
	},
	selectRow: (id) => {

		document.querySelectorAll('.mail-template-row').forEach((row) => {
			row.classList.remove('selected');
		});

		document.getElementById('mail-template-' + id).classList.add('selected');
	},
	populateMailTemplatePagerRow: (row, mailTemplate) => {

		for (let el of row.querySelectorAll('.property')) {
			let val        = mailTemplate[el.dataset.property];
			el.textContent = ((val !== null) ? val : '');
		}
	},
	getObjectDataFromElement: (element) => {

		let data = {};

		for (let el of element.querySelectorAll('.property')) {

			let propName = el.dataset.property;

			if (el.type === 'checkbox') {
				data[propName] = el.checked;
			} else {
				data[propName] = ((el.value === '') ? null : el.value);
			}
		}

		let editor = _Editors.getEditorInstanceForIdAndProperty(data.id, 'text');
		data.text = editor.getValue();

		return data;
	},
	showMailTemplateDetails: (mailTemplateId, isCreate) => {

		Command.get(mailTemplateId, '', (mt) => {

			_MailTemplates.mailTemplateDetailContainer.style.display = null;

			_MailTemplates.mailTemplateDetailForm.dataset['mailTemplateId'] = mailTemplateId;

			LSWrapper.setItem(_MailTemplates.mailTemplateSelectedElementKey, mailTemplateId);

			for (let el of _MailTemplates.mailTemplateDetailForm.querySelectorAll('.property')) {

				let propName = el.dataset.property;
				let value    = mt[propName];

				if (el.type === 'checkbox') {
					el.checked = (value === true);
				} else {
					el.value = value;
				}
			}

			let editor = _MailTemplates.activateEditor(mt);
			_MailTemplates.updatePreview(editor.getValue());

			if (isCreate === true) {
				_MailTemplates.mailTemplatesPager.refresh();
			}
		});
	},
	activateEditor: (mt) => {

		let initialText = mt.text || '';

		let getLanguageForMailTemplateText = (text) => {
			return (text.trim().charAt(0) === '<') ? 'html' : 'text';
		}

		let mailTemplateMonacoConfig = {
			initialText: initialText,
			language: getLanguageForMailTemplateText(initialText),
			lint: true,
			autocomplete: true,
			changeFn: (editor, entity) => {
				_Editors.updateMonacoEditorLanguage(editor, getLanguageForMailTemplateText(editor.getValue()), mt);
			},
			saveFn: (editor, entity) => {
				_MailTemplates.saveMailTemplate();
			},
			wordWrap: (_Editors.getSavedEditorOptions().lineWrapping ? 'on' : 'off')
		};

		_Editors.disposeAllEditors();
		let editor = _Editors.getMonacoEditor(mt, 'text', document.getElementById('mail-template-text'), mailTemplateMonacoConfig);
		_Editors.resizeVisibleEditors();

		_Helpers.fastRemoveAllChildren(document.querySelector('#mail-template-editor-options'));
		_Editors.appendEditorOptionsElement(document.querySelector('#mail-template-editor-options'));

		return editor;
	},
	updatePreview: (text) => {
		_MailTemplates.previewElement.contentDocument.documentElement.innerHTML = text;
	},
	saveMailTemplate: async () => {

		let data = _MailTemplates.getObjectDataFromElement(_MailTemplates.mailTemplateDetailForm);
		let id   = _MailTemplates.mailTemplateDetailForm.dataset['mailTemplateId'];

		let response = await fetch(Structr.rootUrl + 'MailTemplate/' + id, {
			method: 'PUT',
			body: JSON.stringify(data)
		});

		let $blinkTarget = $(_MailTemplates.mailTemplateDetailContainer.querySelector('button[type=submit]'));

		if (response.ok) {

			_Helpers.blinkGreen($blinkTarget);

			let rowInList = _MailTemplates.mailTemplatesList.querySelector('#mail-template-' + id);
			_MailTemplates.populateMailTemplatePagerRow(rowInList, data);
			_MailTemplates.updatePreview(data.text);

		} else {
			_Helpers.blinkRed($blinkTarget);
		}
	},
	createMailTemplates: async (newTpls, blinkTarget) => {

		let response = await fetch(Structr.rootUrl + 'MailTemplate', {
			method: 'POST',
			body: JSON.stringify(newTpls)
		});

		if (response.ok) {

			_Helpers.blinkGreen($(blinkTarget));

			let data = await response.json();
			_MailTemplates.showMailTemplateDetails(data.result[0], true);

		} else {
			_Helpers.blinkRed($(blinkTarget));
		}
	},

	templates: {
		main: config => `
			<div id="mail-templates-main" style="display: none">

				<div class="column-resizer column-resizer-left"></div>

				<div id="mail-templates-list-container">
					<div id="mail-templates-list" class="resourceBox">
						<table id="mail-templates-table">
			<!--				<thead><tr>-->
			<!--					<th><a class="sort" data-sort="name">Name</a></th>-->
			<!--					<th class="narrow"><a class="sort" data-sort="sourceType">Locale</a></th>-->
			<!--					<th id="mail-templates-list-th-actions" class="narrow">Actions</th>-->
			<!--				</tr></thead>-->
							<tbody></tbody>
						</table>
					</div>
				</div>

				<div id="mail-templates-detail-container" style="display: none;">

					<form id="mail-template-detail-form">

						<input id="mail-template-id" type="hidden" class="property" data-property="id">

						<div class="form-row">
							<div class="form-col">
								<label for="mail-template-name">Name</label>
								<input id="mail-template-name" type="text" size="30" class="property" data-property="name">
							</div>

							<div class="form-col">
								<label for="mail-template-locale">Locale</label>
								<input id="mail-template-locale" type="text" size="6" class="property" data-property="locale">
							</div>
						</div>

						<div class="form-row">
							<input id="mail-template-visible-to-public-users" class="property" type="checkbox" data-property="visibleToPublicUsers">
							<label for="mail-template-visible-to-public-users">Visible to public users</label>
						</div>

						<div class="form-row">
							<input id="mail-template-visible-to-authenticated-users" class="property" type="checkbox" data-property="visibleToAuthenticatedUsers">
							<label for="mail-template-visible-to-authenticated-users">Visible to authenticated users</label>
						</div>

						<div class="form-row">
							<div class="form-col">
								<label for="mail-template-text">Content</label>
								<div id="mail-template-text" class="property" data-property="text"></div>
							</div>

							<div class="form-col">
								<label for="mail-template-preview">Preview</label>
								<iframe id="mail-template-preview"></iframe>
							</div>
						</div>

						<div class="form-row justify-between">
							<div id="mail-template-editor-options">
							</div>

							<button type="submit" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
								${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 12, 12, 'icon-green mr-2')} Save
							</button>
						</div>

					</form>
				</div>
			</div>
		`,
		functions: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/mail-templates.css">

			<form id="create-mail-template-form" autocomplete="off" class="inline-flex">
				<input title="Enter a name to create a mail template" id="mail-template-name-preselect" type="text" size="30" placeholder="Name" required class="mr-2">
				<input title="Enter a comma-separated list of locales (f.e. en,de,fr) to create mail templates for" id="mail-template-locale-preselect" type="text" size="10" placeholder="Locale(s)" class="mr-2">

				<button type="submit" form="create-mail-template-form" class="action btn inline-flex items-center" id="create-mail-template">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} New Mail Template
				</button>
			</form>

			<div class="dropdown-menu dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconMagicWand)}
				</button>
				<div class="dropdown-menu-container">

					<div class="heading-row">
						<h3>Create Mail Templates For Processes</h3>
					</div>

					<div class="row mb-2">
						The templates will be created using the locales specified in the locale field.
					</div>

					<div class="row">
						<a id="create-registration-templates" class="flex items-center py-1">
							${_Icons.getSvgIcon(_Icons.iconClipboardPencil, 16, 16, 'mr-2')} Create default "Self-Registration" mail templates
						</a>
					</div>

					<div class="row">
						<a id="create-reset-password-templates" class="flex items-center py-1">
							${_Icons.getSvgIcon(_Icons.iconPasswordReset, 16, 16, 'mr-2')} Create default "Reset Password" mail templates
						</a>
					</div>
				</div>
			</div>

			<div id="mail-templates-pager"></div>
		`,
		typeRow: config => `
			<tr class="mail-template-row" id="mail-template-${config.mailTemplate.id}">
				<td class="property allow-break" data-property="name"></td>
				<td class="property" data-property="locale"></td>
				<td class="actions">
					<div class="icons-container flex items-center justify-end"></div>
				</td>
			</tr>
		`,
	}
};