/*
 * Copyright (C) 2010-2018 Structr GmbH
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
var main, codeMain, codeTree, codeContents;
var drop;
var selectedElements = [];
var activeMethodId, methodContents = {};
var currentWorkingDir;
var methodPageSize = 10000, methodPage = 1;
var timeout, attempts = 0, maxRetry = 10;
var displayingFavorites = false;
var codeLastOpenMethodKey = 'structrCodeLastOpenMethod_' + port;
var codeResizerLeftKey = 'structrCodeResizerLeftKey_' + port;
var activeCodeTabPrefix = 'activeCodeTabPrefix' + port;

$(document).ready(function() {
	Structr.registerModule(_Code);
});

var _Code = {
	_moduleName: 'code',
	init: function() {

		_Logger.log(_LogType.CODE, '_Code.init');

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

	},
	resize: function() {

		var windowHeight = $(window).height();
		var headerOffsetHeight = 100;

		if (codeTree) {
			codeTree.css({
				height: windowHeight - headerOffsetHeight + 'px'
			});
		}

		if (codeContents) {
			codeContents.css({
				height: windowHeight - headerOffsetHeight - 55 + 'px'
			});
		}

		_Code.moveResizer();
		Structr.resize();

		var nameColumnWidth = $('#code-table th:nth-child(2)').width();

		if (nameColumnWidth < 300) {
			$('#code-table th:nth-child(4)').css({ display: 'none' });
			$('#code-table td:nth-child(4)').css({ display: 'none' });
			$('#code-table th:nth-child(5)').css({ display: 'none' });
			$('#code-table td:nth-child(5)').css({ display: 'none' });
		}

		if (nameColumnWidth > 550) {
			$('#code-table th:nth-child(4)').css({ display: 'table-cell' });
			$('#code-table td:nth-child(4)').css({ display: 'table-cell' });
			$('#code-table th:nth-child(5)').css({ display: 'table-cell' });
			$('#code-table td:nth-child(5)').css({ display: 'table-cell' });
		}

		nameColumnWidth = $('#code-table th:nth-child(2)').width() - 96;

		$('.node.method .name_').each(function(i, el) {
			var title = $(el).attr('title');
			$(el).replaceWith('<b title="' +  title + '" class="name_">' + fitStringToWidth(title ? title : '[unnamed]', nameColumnWidth) + '</b>');
		});

		if (codeContents) {
			codeContents.find('.node').each(function() {
				_Entities.setMouseOver($(this), true);
			});
		}

		var contentBox = $('.CodeMirror');
		contentBox.height('100%');

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(codeResizerLeftKey) || 300;
		$('.column-resizer', codeMain).css({ left: left });

		$('#code-tree').css({width: left - 14 + 'px'});
		$('#code-contents').css({left: left + 8 + 'px', width: $(window).width() - left - 58 + 'px'});
	},
	onload: function() {

		_Code.init();

		main.append('<div id="code-main"><div class="column-resizer"></div><div class="fit-to-height" id="code-tree-container"><div id="code-tree"></div></div><div class="fit-to-height" id="code-contents-container"><div id="code-button-container"></div><div id="code-contents"></div></div>');
		codeMain = $('#code-main');

		codeTree = $('#code-tree');
		codeContents = $('#code-contents');

		_Code.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', codeMain), codeResizerLeftKey, 204, _Code.moveResizer);

		/*
		$('#code-contents-container').prepend(
				'<button class="add_file_icon button"><i title="Add Method" class="' + _Icons.getFullSpriteClass(_Icons.add_file_icon) + '" /> Add Method</button>'
				);

		$('.add_file_icon', main).on('click', function(e) {
			Command.create({ type: 'File', size: 0, parentId: currentWorkingDir ? currentWorkingDir.id : null });
		});

		$('.add_minified_css_file_icon', main).on('click', function(e) {
			Command.create({ type: 'MinifiedCssFile', contentType: 'text/css', size: 0, parentId: currentWorkingDir ? currentWorkingDir.id : null });
		});

		$('.add_minified_js_file_icon', main).on('click', function(e) {
			Command.create({ type: 'MinifiedJavaScriptFile', contentType: 'text/javascript', size: 0, parentId: currentWorkingDir ? currentWorkingDir.id : null });
		});

		$('.duplicate_finder', main).on('click', _DuplicateFinder.openDuplicateFinderDialog);

		$('.mount_folder', main).on('click', _Code.openMountDialog);

		$('.add_folder_icon', main).on('click', function(e) {
			Command.create({ type: 'Folder', parentId: currentWorkingDir ? currentWorkingDir.id : null });
		});
		*/

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		codeTree.on('select_node.jstree', function(evt, data) {

			if (data.node && data.node.data && data.node.data.type) {

				if (data.node.data.type === 'SchemaMethod') {

					_Code.displayMethodContents(data.node.id);

				} else {

					if (data.node.state.opened) {
						codeTree.jstree('close_node', data.node.id);
						data.node.state.openend = false;
					} else {
						codeTree.jstree('open_node', data.node.id);
						data.node.state.openend = true;
					}
				}

			} else {

				if (data.node.state.opened) {
					codeTree.jstree('close_node', data.node.id);
					data.node.state.openend = false;
				} else {
					codeTree.jstree('open_node', data.node.id);
					data.node.state.openend = true;
				}
			}
		});

		_TreeHelper.initTree(codeTree, _Code.treeInitFunction, 'structr-ui-code');

		$(window).off('resize').resize(function() {
			_Code.resize();
		});

		Structr.unblockMenu(100);

		_Code.resize();
		Structr.adaptUiToAvailableFeatures();

	},
	deepOpen: function(d, dirs) {

		_TreeHelper.deepOpen(codeTree, d, dirs, 'parent', (currentWorkingDir ? currentWorkingDir.id : 'root'));

	},
	refreshTree: function() {

		_TreeHelper.refreshTree(codeTree);

	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':

				var defaultFilesystemEntries = [
					{
						id: 'globals',
						text: 'Global Methods',
						children: true,
						icon: _Icons.star_icon
					},
					{
						id: 'root',
						text: 'All Types',
						children: true,
						icon: _Icons.structr_logo_small,
						path: '/',
						state: {
							opened: true,
							selected: true
						}
					}
				];

				callback(defaultFilesystemEntries);
				break;

			case 'root':
				_Code.load(null, callback);
				break;

			default:
				_Code.load(obj.id, callback);
				break;
		}

	},
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#code-main', main));
	},
	load: function(id, callback) {

		var displayFunction = function (result) {

			var list = [];

			result.forEach(function(d) {
				var icon = 'fa-file-code-o gray';
				switch (d.type) {
					case "SchemaMethod":
						switch (d.codeType) {
							case 'java':
								icon = 'fa-dot-circle-o red';
								break;
							default:
								icon = 'fa-circle-o blue';
								break;
						}
				}

				list.push({
					id: d.id,
					text:  d.name ? d.name : '[unnamed]',
					children: d.schemaMethods ? d.schemaMethods.length > 0 : false,
					icon: 'fa ' + icon,
					data: {
						type: d.type
					}
				});
			});

			callback(list);
		};

		if (!id) {

			Command.list('SchemaNode', false, methodPageSize, methodPage, 'name', 'asc', 'id,type,name,schemaMethods', displayFunction);

		} else {

			switch (id) {

				case 'globals':
					Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: null}, displayFunction, true);
					break;
				default:
					Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: id}, displayFunction, true);
					break;
			}
		}

	},
	displayMethodContents: function(id) {

		if (id === '#' || id === 'root' || id === 'favorites' || id === 'globals') {
			return;
		}

		fastRemoveAllChildren(codeContents[0]);

		LSWrapper.setItem(codeLastOpenMethodKey, id);

		_Code.editMethodContent(undefined, id, codeContents);
	},
	fileOrFolderCreationNotification: function (newFileOrFolder) {
		if ((currentWorkingDir === undefined || currentWorkingDir === null) && newFileOrFolder.parent === null) {
			_Code.appendFileOrFolder(newFileOrFolder);
		} else if ((currentWorkingDir !== undefined && currentWorkingDir !== null) && newFileOrFolder.parent && currentWorkingDir.id === newFileOrFolder.parent.id) {
			_Code.appendFileOrFolder(newFileOrFolder);
		}
	},
	registerParentLink: function(d, triggerEl) {

		// Change working dir by click on folder icon
		triggerEl.on('click', function(e) {

			e.preventDefault();
			e.stopPropagation();

			if (d.parentId) {

				codeTree.jstree('open_node', $('#' + d.parentId), function() {
					if (d.name === '..') {
						$('#' + d.parentId + '_anchor').click();
					} else {
						$('#' + d.id + '_anchor').click();
					}
				});

			} else {
				$('#' + d.id + '_anchor').click();
			}

			return false;
		});
	},
	updateCode: function(method, source) {
		Command.setProperty(method.id, 'source', source);
	},
	editMethodContent: function(button, id, element) {

		var url  = rootUrl + id;
		var text = '';

		$.ajax({
			url: url,
			success: function(result) {
				var method = result.result;
				_Logger.log(_LogType.CODE, method.id, methodContents);
				text = methodContents[method.id] || method.source;
				if (Structr.isButtonDisabled(button)) {
					return;
				}
				element.append('<div class="editor"></div><div id="template-preview"><textarea readonly></textarea></div>');
				var contentBox = $('.editor', element);
				var lineWrapping = LSWrapper.getItem(lineWrappingKey);
				editor = CodeMirror(contentBox.get(0), {
					value: text,
					mode: method.codeType === 'java' ? 'java' : 'javascript',
					lineNumbers: true,
					lineWrapping: lineWrapping,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true
				});

				var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + method.id));
				if (scrollInfo) {
					editor.scrollTo(scrollInfo.left, scrollInfo.top);
				}

				editor.on('scroll', function() {
					var scrollInfo = editor.getScrollInfo();
					LSWrapper.setItem(scrollInfoKey + '_' + method.id, JSON.stringify(scrollInfo));
				});

				editor.id = method.id;

				var buttonArea = $('#code-button-container');
				buttonArea.empty();
				buttonArea.append('<button id="resetMethod" disabled="disabled" class="disabled"><i title="Add Method" class="' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" /> Cancel</button>');
				buttonArea.append('<button id="saveMethod" disabled="disabled" class="disabled"><i title="Add Method" class="' + _Icons.getFullSpriteClass(_Icons.floppy_icon) + '" /> Save</button>');

				var codeResetButton = $('#resetMethod', buttonArea);
				var codeSaveButton  = $('#saveMethod', buttonArea);

				editor.on('change', function(cm, change) {

					if (text === editor.getValue()) {
						codeSaveButton.prop("disabled", true).addClass('disabled');
						codeResetButton.prop("disabled", true).addClass('disabled');
					} else {
						codeSaveButton.prop("disabled", false).removeClass('disabled');
						codeResetButton.prop("disabled", false).removeClass('disabled');
					}

				});

				codeResetButton.on('click', function(e) {

					e.preventDefault();
					e.stopPropagation();
					_Code.displayMethodContents(method.id);
					codeSaveButton.prop("disabled", true).addClass('disabled');
					codeResetButton.prop("disabled", true).addClass('disabled');

				});

				codeSaveButton.on('click', function(e) {

					e.preventDefault();
					e.stopPropagation();
					var newText = editor.getValue();
					if (text === newText) {
						return;
					}
					_Code.updateCode(method, newText);
					text = newText;
					codeSaveButton.prop("disabled", true).addClass('disabled');
					codeResetButton.prop("disabled", true).addClass('disabled');

				});

				_Code.resize();

				if (method.isTemplate) {
					_Code.updateTemplatePreview(element, url, dataType, contentType);
				}

				editor.refresh();

				$(window).on('keydown', function(e) {
					// This hack prevents FF from closing WS connections on ESC
					if (e.keyCode === 27) {
						e.preventDefault();
					}
					var k = e.which;
					if (k === 16) {
						shiftKey = true;
					}
					if (k === 18) {
						altKey = true;
					}
					if (k === 17) {
						ctrlKey = true;
					}
					if (k === 69) {
						eKey = true;
					}
					if (navigator.platform === 'MacIntel' && k === 91) {
						cmdKey = true;
					}
					if ((e.ctrlKey && (e.which === 83)) || (navigator.platform === 'MacIntel' && cmdKey && (e.which === 83))) {
						e.preventDefault();
						if (codeSaveButton && !codeSaveButton.prop('disabled')) {
							codeSaveButton.click();
						}
					}
				});

			},
			error: function(xhr, statusText, error) {
				console.log(xhr, statusText, error);
			}
		});
	}
};
