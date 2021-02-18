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

var _MailTemplates = {
	_moduleName: 'mail-templates',

	mailTemplatesPager: undefined,
	mailTemplatesList: undefined,
	mailTemplateDetail: undefined,
	mailTemplateDetailTable: undefined,
	previewElement: undefined,

	mailTemplatesResizerLeftKey: 'structrMailTemplatesResizerLeftKey_' + port,

	modes: {
		edit: 'edit',
		create: 'create',
		preview: 'preview'
	},

	init: function() {},
	unload: function() {},
	onload: async function() {
		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('mail-templates'));

		let html = await Structr.fetchHtmlTemplate('mail-templates/main', {});
		main.append(html);

		$('#create-mail-template').on('click', function() {
			_MailTemplates.clearMailTemplateDetails();

			_MailTemplates.switchMode(_MailTemplates.modes.create);
			_MailTemplates.mailTemplateDetail.show();
		});

		_MailTemplates.mailTemplatesList = $('#mail-templates-table tbody');
		_MailTemplates.listMailTemplates();

		_MailTemplates.mailTemplateDetail = $('#mail-template-detail').hide();
		_MailTemplates.mailTemplateDetailTable = $('#mail-template-detail-table');
		_MailTemplates.previewElement = document.getElementById('mail-template-preview');

		_MailTemplates.registerChangeListeners();

		$('button.save', _MailTemplates.mailTemplateDetail).on('click', function() {

			var data = _MailTemplates.getObjectDataFromElement(_MailTemplates.mailTemplateDetailTable);

			_MailTemplates.saveObject('MailTemplate', data, _MailTemplates.mailTemplateDetailTable, function (data) {
				var createdId = data.result[0];
				_MailTemplates.mailTemplateDetailTable.data('mail-template-id', createdId);

				_MailTemplates.switchMode(_MailTemplates.modes.edit);
				_MailTemplates.mailTemplatesPager.refresh();
			});
		});

		$('button.cancel', _MailTemplates.mailTemplateDetail).on('click', function () {
			_MailTemplates.clearMailTemplateDetails();
			_MailTemplates.mailTemplateDetail.hide();
		});

		$('button.preview', _MailTemplates.mailTemplateDetail).on('click', function () {

			_MailTemplates.previewElement.contentDocument.open();
			_MailTemplates.previewElement.contentDocument.write($('textarea', _MailTemplates.mailTemplateDetail).val());
			_MailTemplates.previewElement.contentDocument.close();
			_MailTemplates.switchMode(_MailTemplates.modes.preview);
		});

		$('button.exit-preview', _MailTemplates.mailTemplateDetail).on('click', function () {
			_MailTemplates.switchMode(_MailTemplates.modes.edit);
		});

		Structr.unblockMenu(100);

		_MailTemplates.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', main), _MailTemplates.mailTemplatesResizerLeftKey, 300, _MailTemplates.moveResizer);

		_MailTemplates.resize();

	},
	resize: function() {
		_MailTemplates.moveResizer();
		Structr.resize();
	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_MailTemplates.mailTemplatesResizerLeftKey) || 300;
		$('.column-resizer', main).css({ left: left });

		$('#mail-templates-list').css({width: left - 25 + 'px'});
	},
	switchMode: function(mode) {

		$('.show-in-modes', _MailTemplates.mailTemplateDetail).hide();

		if (mode) {
			$('.show-in-modes', _MailTemplates.mailTemplateDetail).each(function(i, el) {
				var self = $(this);
				if (self.data('modes').split('|').indexOf(mode) !== -1) {
					self.show();
					if (self[0].tagName === 'TABLE') {
						self.css('display', 'table');
					}
				}
			});
		}
	},
	registerChangeListeners: function() {

		$(_MailTemplates.mailTemplateDetailTable).on('change', '.property', function() {
			var el = $(this);
			var table = el.closest('table');
			var objId = table.data('mail-template-id');
			if (objId) {
				var data = _MailTemplates.getObjectDataFromElement(table);
				_MailTemplates.updateObject('MailTemplate', objId, data, el, el.closest('td'), function() {
					var rowInList = $('#mail-template-' + objId, _MailTemplates.mailTemplatesList);
					_MailTemplates.populateMailTemplatePagerRow(rowInList, data);
				});
			}
		});
	},
	listMailTemplates: function () {

		let pagerEl = $('#mail-templates-pager');

		_Pager.initPager('mail-templates', 'MailTemplate', 1, 25, 'name', 'asc');

		_MailTemplates.mailTemplatesPager = _Pager.addPager('mail-templates', pagerEl, false, 'MailTemplate', 'ui', _MailTemplates.processPagerData);

		_MailTemplates.mailTemplatesPager.cleanupFunction = function () {
			fastRemoveAllChildren(_MailTemplates.mailTemplatesList[0]);
		};
		_MailTemplates.mailTemplatesPager.pager.append('<br>Filters: <input type="text" class="filter w100 mail-template-name" data-attribute="name" placeholder="Name" />');
		_MailTemplates.mailTemplatesPager.pager.append('<input type="text" class="filter w100 mail-template-locale" data-attribute="locale" placeholder="Locale" />');
		_MailTemplates.mailTemplatesPager.activateFilterElements();

		pagerEl.append('<div style="clear:both;"></div>');

		$('#mail-templates-table .sort').on('click', function () {
			_MailTemplates.mailTemplatesPager.setSortKey($(this).data('sort'));
		});

	},
	processPagerData: function (pagerData) {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_MailTemplates.appendMailTemplate);
		}
	},
	appendMailTemplate: async function (mailTemplate) {

		let html = await Structr.fetchHtmlTemplate('mail-templates/row.type', {mailTemplate: mailTemplate});

		var row = $(html);

		_MailTemplates.populateMailTemplatePagerRow(row, mailTemplate);
		_MailTemplates.mailTemplatesList.append(row);

		var actionsCol = $('.actions', row);

		$('<a title="Edit Properties" class="properties"><i class=" button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" /></a>').on('click', function () {
			_MailTemplates.showMailTemplateDetails(mailTemplate.id);
		}).appendTo(actionsCol);

		_MailTemplates.appendDeleteIcon(actionsCol, 'Do you really want to delete the mail template "' + mailTemplate.name + '"?', function() {

			Command.deleteNode(mailTemplate.id);

			if (mailTemplate.id === _MailTemplates.mailTemplateDetailTable.data('mail-template-id')) {
				_MailTemplates.clearMailTemplateDetails();
				_MailTemplates.mailTemplateDetail.hide();
			}

			row.remove();
		});

	},
	populateMailTemplatePagerRow:function(row, mailTemplate) {
		$('.property', row).each(function(i, el) {
			var self = $(this);
			var val = mailTemplate[self.attr('data-property')];
			self.text(val !== null ? val : "");
		});
	},
	deleteVirtualType: function(id) {

		console.log('DELETE id: ' + id + ' + all properties!');
		console.log('If this ID is currently active, remove it!');

	},
	getObjectDataFromElement: function(element) {
		var data = {};

		$('.property', element).each(function(idx, el) {
			var el = $(el);

			if (el.attr('type') === 'checkbox') {
				data[el.data('property')] = el.prop('checked');
			} else {
				var val = el.val();
				if (val === "") {
					val = null;
				}
				data[el.data('property')] = val;
			}
		});

		return data;
	},
	showMailTemplateDetails:function(mailTemplateId) {

		Command.get(mailTemplateId, '', function(mt) {

			_MailTemplates.mailTemplateDetailTable.data('mail-template-id', mailTemplateId);

			$('.property', _MailTemplates.mailTemplateDetailTable).each(function(idx, el) {
				var el = $(el);
				var val = mt[el.data('property')];

				if (el.attr('type') === 'checkbox') {
					el.prop('checked', (val === true));
				} else {
					el.val(val);
				}
			});

			_MailTemplates.switchMode(_MailTemplates.modes.edit);
			_MailTemplates.mailTemplateDetail.show();
		});
	},
	clearMailTemplateDetails: function() {

		_MailTemplates.mailTemplateDetailTable.removeData('mail-template-id');

		$('.property', _MailTemplates.mailTemplateDetailTable).each(function(idx, el) {
			var el = $(el);

			if (el.attr('type') === 'checkbox') {
				el.prop('checked', true);
			} else {
				el.val("");
			}
		});
	},
	appendDeleteIcon: function(insertPoint, confirmText, deletionActionCallback) {

		$('<a title="Delete" class="delete"><i class=" button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /></a>').on('click', function() {

			if (true === confirm(confirmText)) {
				deletionActionCallback();
			}

		}).appendTo(insertPoint);
	},
	saveObject:function (type, data, element, successCallback) {

		$.ajax({
			url: rootUrl + type,
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(data),
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				blinkGreen($('td', element));

				if (typeof(successCallback) === "function") {
					successCallback(data);
				}
			},
			error: function () {
				blinkRed($('td', element));
			}
		});

	},
	updateObject:function (type, id, newData, $el, $blinkTarget, successCallback) {

		$.ajax({
			url: rootUrl + type + '/' + id,
			type: 'PUT',
			dataType: 'json',
			data: JSON.stringify(newData),
			contentType: 'application/json; charset=utf-8',
			success: function() {
				blinkGreen(($blinkTarget ? $blinkTarget : $el));
				if (typeof(successCallback) === "function") {
					successCallback();
				}
			},
			error: function () {
				blinkRed(($blinkTarget ? $blinkTarget : $el));
			}
		});
	}
};