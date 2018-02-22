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

		main = $('#main');

		main.append('<div class="searchBox module-dependend" data-structr-module="text-search"><input class="search" name="search" placeholder="Search..."><i class="clearSearchIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');

		searchField = $('.search', main);

		if (searchField && searchField.length > 0) {

			searchField.focus();

			searchField.keyup(function(e) {

				var searchString = $(this).val();
				if (searchString && searchString.length && e.keyCode === 13) {

					$('.clearSearchIcon').show().on('click', function() {
						_Code.clearSearch();
					});

					_Code.fulltextSearch(searchString);

				} else if (e.keyCode === 27 || searchString === '') {
					_Code.clearSearch();
				}

			});
		}

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

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(codeResizerLeftKey) || 300;
		$('.column-resizer', codeMain).css({ left: left });

		$('#code-tree').css({width: left - 14 + 'px'});
		$('#code-contents').css({left: left + 8 + 'px', width: $(window).width() - left - 58 + 'px'});
	},
	onload: function() {

		_Code.init();

		main.append('<div id="code-main"><div class="column-resizer"></div><div class="fit-to-height" id="code-tree-container"><div id="code-tree"></div></div><div class="fit-to-height" id="code-contents-container"><div id="code-contents"></div></div>');
		codeMain = $('#code-main');

		codeTree = $('#code-tree');
		codeContents = $('#code-contents');

		_Code.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', codeMain), codeResizerLeftKey, 204, _Code.moveResizer);

		$('#code-contents-container').prepend(
				'<button class="add_file_icon button"><i title="Add Method" class="' + _Icons.getFullSpriteClass(_Icons.add_file_icon) + '" /> Add Method</button>'
				);

		/*
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

		codeTree.on('ready.jstree', function() {

			var lastOpenMethod = LSWrapper.getItem(codeLastOpenMethodKey);

			if (lastOpenMethod === 'favorites') {

				$('#favorites_anchor').click();

			} else if (currentWorkingDir) {

				_Code.deepOpen(currentWorkingDir.parent);

			} else {

				$('#root_anchor').click();
			}
		});

		codeTree.on('select_node.jstree', function(evt, data) {

			if (data.node.id === 'favorites') {

				_Code.displayMethodContents('favorites');

			} else {

				_Code.displayMethodContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);
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
						id: 'favorites',
						text: 'Favorite Methods',
						children: false,
						icon: _Icons.star_icon
					},
					{
						id: 'globals',
						text: 'Global Methods',
						children: false,
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
	fulltextSearch: function(searchString) {
		var content = $('#folder-contents');
		content.children().hide();

		var url;
		if (searchString.contains(' ')) {
			url = rootUrl + 'files/ui?loose=1';
			searchString.split(' ').forEach(function(str, i) {
				url = url + '&indexedWords=' + str;
			});
		} else {
			url = rootUrl + 'files/ui?loose=1&indexedWords=' + searchString;
		}

		_Code.displaySearchResultsForURL(url);
	},
	clearSearch: function() {
		$('.search', main).val('');
		$('#search-results').remove();
		$('#folder-contents').children().show();
	},
	load: function(id, callback) {

		var displayFunction = function (result) {

			var list = [];

			result.forEach(function(d) {

				var icon = 'fa-folder';

				console.log(d.codeType);

				switch (d.type) {

					case "SchemaMethod":

						switch (d.codeType) {

							case 'java':

								icon = 'fa-coffee';
								break;

							default:

								icon = 'fa-code';
								break;
						}
				}


				list.push({
					id: d.id,
					text:  d.name ? d.name : '[unnamed]',
					children: d.schemaMethods ? d.schemaMethods.length > 0 : false,
					icon: 'fa ' + icon
				});
			});

			callback(list);

		};

		if (!id) {
			Command.list('SchemaNode', false, methodPageSize, methodPage, 'name', 'asc', 'id,type,name,schemaMethods', displayFunction);
		} else {
			Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: id}, displayFunction, true);
		}

	},
	displayMethodContents: function(id, parentId, nodePath, parents) {

		fastRemoveAllChildren(codeContents[0]);

		LSWrapper.setItem(codeLastOpenMethodKey, id);

		displayingFavorites = (id === 'favorites');
		var isRootFolder = (id === 'root');
		var parentIsRoot = (parentId === '#');

		var handleChildren = function(children) {
			if (children && children.length) {
				children.forEach(_Code.appendFileOrFolder);
			}

			_Code.resize();
		};

		if (displayingFavorites === true) {

			$('#folder-contents-container > button').addClass('disabled').attr('disabled', 'disabled');

			codeContents.append('<h2><i class="' + _Icons.getFullSpriteClass(_Icons.star_icon) + '" /> Favorite Files</h2>');
			codeContents.append('<table id="code-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th></tr></thead><tbody id="code-table-body"></tbody></table>');

			$.ajax({
				url: rootUrl + 'me/favorites',
				statusCode: {
					200: function(data) {
						handleChildren(data.result);
					}
				}
			});

		} else {

			$('#folder-contents-container > button').removeClass('disabled').attr('disabled', null);

			if (isRootFolder) {
				Command.list('Folder', true, 1000, 1, 'name', 'asc', 'id,name,type,isFolder,folders,files,icon,path,visibleToPublicUsers,visibleToAuthenticatedUsers,owner,isMounted', handleChildren);
			} else {
				Command.query('Folder', 1000, 1, 'name', 'asc', {parentId: id}, handleChildren, true, 'public');
			}

			_Pager.initPager('filesystem-files', 'File', 1, 25, 'name', 'asc');
			page['File'] = 1;

			var filterOptions = {
				parentId: (parentIsRoot ? '' : id),
				hasParent: (!parentIsRoot)
			};

			_Pager.initFilters('filesystem-files', 'File', filterOptions);

			var filesPager = _Pager.addPager('filesystem-files', codeContents, false, 'File', 'public', handleChildren);

			filesPager.cleanupFunction = function () {
				var toRemove = $('.node.file', filesPager.el).closest('tr');
				toRemove.each(function(i, elem) {
					fastRemoveAllChildren(elem);
					elem.remove();
				});
			};

			filesPager.pager.append('Filter: <input type="text" class="filter" data-attribute="name">');
			filesPager.pager.append('<input type="text" class="filter" data-attribute="parentId" value="' + (parentIsRoot ? '' : id) + '" hidden>');
			filesPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" ' + (parentIsRoot ? '' : 'checked') + ' hidden>');
			filesPager.activateFilterElements();

			codeContents.append('<table id="code-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th></tr></thead>'
				+ '<tbody id="code-table-body">'
				+ (!isRootFolder ? '<tr id="parent-file-link"><td class="file-type"><i class="fa fa-folder"></i></td><td><a href="#">..</a></td><td></td><td></td><td></td></tr>' : '')
				+ '</tbody></table>');

			$('#parent-file-link').on('click', function(e) {

				if (!parentIsRoot) {
					$('#' + parentId + '_anchor').click();
				}
			});
		}
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
	updateTextFile: function(file, text) {
		var chunks = Math.ceil(text.length / chunkSize);
		for (var c = 0; c < chunks; c++) {
			var start = c * chunkSize;
			var end = (c + 1) * chunkSize;
			var chunk = utf8_to_b64(text.substring(start, end));
			Command.chunk(file.id, c, chunkSize, chunk, chunks);
		}
	},
	editContent: function(button, file, element) {
		var url = viewRootUrl + file.id + '?edit=1';
		_Logger.log(_LogType.CODE, 'editContent', button, file, element, url);
		var text = '';

		var contentType = file.contentType;
		var dataType = 'text';

		if (!contentType) {
			if (file.name.endsWith('.css')) {
				contentType = 'text/css';
			} else if (file.name.endsWith('.js')) {
				contentType = 'text/javascript';
			} else {
				contentType = 'text/plain';
			}
		}
		_Logger.log(_LogType.CODE, viewRootUrl, url);

		$.ajax({
			url: url,
			dataType: dataType,
			contentType: contentType,
			success: function(data) {
				_Logger.log(_LogType.CODE, file.id, methodContents);
				text = methodContents[file.id] || data;
				if (Structr.isButtonDisabled(button)) {
					return;
				}
				element.append('<div class="editor"></div><div id="template-preview"><textarea readonly></textarea></div>');
				var contentBox = $('.editor', element);
				var lineWrapping = LSWrapper.getItem(lineWrappingKey);
				editor = CodeMirror(contentBox.get(0), {
					value: text,
					mode: contentType,
					lineNumbers: true,
					lineWrapping: lineWrapping,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true
				});

				var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + file.id));
				if (scrollInfo) {
					editor.scrollTo(scrollInfo.left, scrollInfo.top);
				}

				editor.on('scroll', function() {
					var scrollInfo = editor.getScrollInfo();
					LSWrapper.setItem(scrollInfoKey + '_' + file.id, JSON.stringify(scrollInfo));
				});

				editor.id = file.id;

				dialogBtn.children('#saveFile').remove();
				dialogBtn.children('#saveAndClose').remove();

				var h = '<span class="editor-info"><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (lineWrapping ? ' checked="checked" ' : '') + '>&nbsp;&nbsp;'
				+ '<label for="isTemplate">Replace template expressions:</label> <input id="isTemplate" type="checkbox"' + (file.isTemplate ? ' checked="checked" ' : '') + '></span>';
				dialogMeta.html(h);

				Structr.appendInfoTextToElement({
					text: "Expressions like <pre>Hello ${print(me.name)} !</pre> will be evaluated. To see a preview, tick this checkbox.",
					element: dialogMeta
				});

				$('#lineWrapping').on('change', function() {
					var inp = $(this);
					if (inp.is(':checked')) {
						LSWrapper.setItem(lineWrappingKey, "1");
						editor.setOption('lineWrapping', true);
					} else {
						LSWrapper.removeItem(lineWrappingKey);
						editor.setOption('lineWrapping', false);
					}
					editor.refresh();
				});

				$('#isTemplate').on('change', function() {
					var inp = $(this);
					var active = inp.is(':checked');
					_Entities.setProperty(file.id, 'isTemplate', active, false, function() {
						if (active) {
							_Code.updateTemplatePreview(element, url, dataType, contentType);
						} else {
							var previewArea = $('#template-preview');
							previewArea.hide();
							$('textarea', previewArea).val('');
							var contentBox = $('.editor', element);
							contentBox.width('inherit');
						}
					});
				});

				dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled">Save</button>');
				dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled">Save and close</button>');

				dialogSaveButton = $('#saveFile', dialogBtn);
				saveAndClose = $('#saveAndClose', dialogBtn);

				editor.on('change', function(cm, change) {

					if (text === editor.getValue()) {
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					} else {
						dialogSaveButton.prop("disabled", false).removeClass('disabled');
						saveAndClose.prop("disabled", false).removeClass('disabled');
					}

				});

				$('button#saveFile', dialogBtn).on('click', function(e) {

					var isTemplate = $('#isTemplate').is(':checked');

					if (isTemplate) {
						$('#isTemplate').prop('checked', false);
						_Entities.setProperty(file.id, 'isTemplate', false, false, function() {

							e.preventDefault();
							e.stopPropagation();
							var newText = editor.getValue();
							if (text === newText) {
								return;
							}
							_Code.updateTextFile(file, newText);
							text = newText;
							dialogSaveButton.prop("disabled", true).addClass('disabled');
							saveAndClose.prop("disabled", true).addClass('disabled');

							$('#isTemplate').click();

						});

					} else {
						e.preventDefault();
						e.stopPropagation();
						var newText = editor.getValue();
						if (text === newText) {
							return;
						}
						_Code.updateTextFile(file, newText);
						text = newText;
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					}

				});

				saveAndClose.on('click', function(e) {
					e.stopPropagation();
					dialogSaveButton.click();
					setTimeout(function() {
						dialogSaveButton.remove();
						saveAndClose.remove();
						dialogCancelButton.click();
					}, 500);
				});

				_Code.resize();

				if (file.isTemplate) {
					_Code.updateTemplatePreview(element, url, dataType, contentType);
				}
			},
			error: function(xhr, statusText, error) {
				console.log(xhr, statusText, error);
			}
		});
	}
};
