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
$(document).ready(function() {
	Structr.registerModule(_MailTemplates);
});

let _MailTemplates = {
	_moduleName: 'mail-templates',

	mailTemplatesPager: undefined,
	mailTemplatesList: undefined,
	mailTemplateDetailContainer: undefined,
	mailTemplateDetailForm: undefined,
	previewElement: undefined,
	editor: undefined,

	mailTemplatesResizerLeftKey: 'structrMailTemplatesResizerLeftKey_' + location.port,
	mailTemplateSelectedElementKey: 'structrMailTemplatesSelectedElementKey_' + location.port,

	init: function() {},
	unload: function() {},
	onload: function() {

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('mail-templates'));

		Structr.fetchHtmlTemplate('mail-templates/main', {}, function(html) {
			main.append(html);

			Structr.fetchHtmlTemplate('mail-templates/functions', {}, function (html) {

				Structr.functionBar.innerHTML = html;

				UISettings.showSettingsForCurrentModule();

				let namePreselect = document.getElementById('mail-template-name-preselect');

				Structr.functionBar.querySelector('#create-mail-template-form').addEventListener('submit', async (e) => {
					e.preventDefault();

					_MailTemplates.showMain();
					await _MailTemplates.createObject('MailTemplate', { name: namePreselect.value }, this, (data) => {
						let id = data.result[0];
						_MailTemplates.showMailTemplateDetails(id, true);
					});
				});

				_MailTemplates.mailTemplatesList           = document.querySelector('#mail-templates-table tbody');
				_MailTemplates.mailTemplateDetailContainer = document.getElementById('mail-templates-detail-container');
				_MailTemplates.mailTemplateDetailForm      = document.getElementById('mail-template-detail-form');
				_MailTemplates.previewElement              = document.getElementById('mail-template-preview');

				_MailTemplates.listMailTemplates();

				_MailTemplates.mailTemplateDetailContainer.querySelector('button.save').addEventListener('click', function() {

					let data = _MailTemplates.getObjectDataFromElement(_MailTemplates.mailTemplateDetailForm);
					let id   = _MailTemplates.mailTemplateDetailForm.dataset['mailTemplateId'];

					_MailTemplates.updateObject('MailTemplate', id, data, this, null, function() {

						let rowInList = _MailTemplates.mailTemplatesList.querySelector('#mail-template-' + id);
						_MailTemplates.populateMailTemplatePagerRow(rowInList, data);
						_MailTemplates.updatePreview();
					});
				});

				Structr.unblockMenu(100);

				Structr.initVerticalSlider($('.column-resizer', main), _MailTemplates.mailTemplatesResizerLeftKey, 300, _MailTemplates.moveResizer);

				_MailTemplates.moveResizer(LSWrapper.getItem(_MailTemplates.mailTemplatesResizerLeftKey));
			});
		});
	},
	getContextMenuElements: function (div, entity) {

		let elements = [];

		elements.push({
			name: 'Properties',
			clickHandler: function() {
				_Entities.showProperties(entity, 'ui');
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getSvgIcon('trashcan'),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Mail Template',
			clickHandler: () => {

				_Entities.deleteNode(this, entity, false, () => {

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
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

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
		Structr.resize();
	},
	moveResizer: (left) => {

		requestAnimationFrame(() => {

			left = left || LSWrapper.getItem(_MailTemplates.mailTemplatesResizerLeftKey) || 300;
			left = Math.max(300, Math.min(left, window.innerWidth - 300));

			document.getElementById('mail-templates-list-container').style.width = 'calc(' + left + 'px - 1rem)';
			document.querySelector('.column-resizer').style.left                  = left + 'px';
			_MailTemplates.mailTemplateDetailContainer.style.width                        = 'calc(100% - ' + left + 'px - 3rem)';

			return true;
		});
	},
	updatePreview: () => {

		let value = _MailTemplates.editor ? _MailTemplates.editor.getValue() : document.getElementById('mail-template-text').value;
		_MailTemplates.previewElement.contentDocument.documentElement.innerHTML = value;
	},
	listMailTemplates: () => {

		let pagerEl = $('#mail-templates-pager');

		_Pager.initPager('mail-templates', 'MailTemplate', 1, 25, 'name', 'asc');

		_MailTemplates.mailTemplatesPager = _Pager.addPager('mail-templates', pagerEl, false, 'MailTemplate', 'ui', _MailTemplates.processPagerData);

		_MailTemplates.mailTemplatesPager.cleanupFunction = function () {
			_MailTemplates.mailTemplatesList.innerHTML = '';
		};
		_MailTemplates.mailTemplatesPager.pager.append('Filters: <input type="text" class="filter w100 mail-template-name" data-attribute="name" placeholder="Name" />');
		_MailTemplates.mailTemplatesPager.pager.append('<input type="text" class="filter w100 mail-template-locale" data-attribute="locale" placeholder="Locale" />');
		_MailTemplates.mailTemplatesPager.activateFilterElements();

		pagerEl.append('<div style="clear:both;"></div>');
	},
	processPagerData: (pagerData) => {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_MailTemplates.appendMailTemplate);
		}
	},
	appendMailTemplate: (mailTemplate) => {

		_MailTemplates.showMain();

		Structr.fetchHtmlTemplate('mail-templates/row.type', { mailTemplate: mailTemplate }, function(html) {

			let row = Structr.createSingleDOMElementFromHTML(html);

			_MailTemplates.populateMailTemplatePagerRow(row, mailTemplate);
			_MailTemplates.mailTemplatesList.appendChild(row);

			row.addEventListener('click', () => {
				_MailTemplates.selectRow(mailTemplate.id);
				_MailTemplates.showMailTemplateDetails(mailTemplate.id);
			});

			_Elements.enableContextMenuOnElement($(row), mailTemplate);
			_Entities.appendEditPropertiesIcon($(row.querySelector('.actions')), mailTemplate, true);

			let lastSelectedMailTemplateId = LSWrapper.getItem(_MailTemplates.mailTemplateSelectedElementKey);
			if (lastSelectedMailTemplateId === mailTemplate.id) {
				row.click();
			}
		});
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
	getObjectDataFromElement: function(element) {

		let data = {};

		_MailTemplates.editor.save();

		for (let el of element.querySelectorAll('.property')) {

			let propName = el.dataset.property;

			if (el.type === 'checkbox') {
				data[propName] = el.checked;
			} else {
				data[propName] = ((el.value === '') ? null : el.value);
			}
		}

		return data;
	},
	showMailTemplateDetails: function(mailTemplateId, isCreate) {

		Command.get(mailTemplateId, '', function(mt) {

			_MailTemplates.mailTemplateDetailContainer.style.display = null;

			_MailTemplates.mailTemplateDetailForm.dataset['mailTemplateId'] = mailTemplateId;

			LSWrapper.setItem(_MailTemplates.mailTemplateSelectedElementKey, mailTemplateId);

			if (_MailTemplates.editor) {
				_MailTemplates.editor.toTextArea();
			}

			for (let el of _MailTemplates.mailTemplateDetailForm.querySelectorAll('.property')) {

				let propName = el.dataset.property;
				let value    = mt[propName];

				if (el.type === 'checkbox') {
					el.checked = (value === true);
				} else {
					el.value = value;
				}
			}

			_MailTemplates.activateEditor();
			_MailTemplates.updatePreview();

			if (isCreate === true) {
				_MailTemplates.mailTemplatesPager.refresh();
			}
		});
	},
	activateEditor: () => {

		let templateContentTextarea = document.getElementById('mail-template-text');
		_MailTemplates.editor = CodeMirror.fromTextArea(templateContentTextarea, Structr.getCodeMirrorSettings({
			lineNumbers: true,
			lineWrapping: false,
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: false
		}));
	},
	createObject: async (type, data, element, successCallback) => {

		let response = await fetch(rootUrl + type, {
			method: 'POST',
			body: JSON.stringify(data)
		});

		if (response.ok) {

			let data = await response.json();

			blinkGreen($('td', element));

			if (typeof successCallback === "function") {
				successCallback(data);
			}

		} else {
			blinkRed($('td', element));
		}
	},
	updateObject: async (type, id, newData, $el, $blinkTarget, successCallback) => {

		let response = await fetch(rootUrl + type + '/' + id, {
			method: 'PUT',
			body: JSON.stringify(newData)
		});

		if (response.ok) {

			blinkGreen(($blinkTarget ? $blinkTarget : $el));

			if (typeof successCallback === "function") {
				successCallback();
			}

		} else {
			blinkRed(($blinkTarget ? $blinkTarget : $el));
		}
	}
};