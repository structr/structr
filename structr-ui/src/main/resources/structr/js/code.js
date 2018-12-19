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
var main, codeMain, codeTree, codeContents, codeContext;
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
				height: windowHeight - headerOffsetHeight - 22 + 'px'
			});
		}

		if (codeContents) {
			codeContents.css({
				height: windowHeight - headerOffsetHeight - 11 + 'px'
			});
		}

		if (codeContext) {
			codeContext.css({
				height: windowHeight - headerOffsetHeight - 11 + 'px'
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

		var contextWidth = 240;
		var width        = $(window).width() - left - contextWidth - 80;

		$('#code-tree').css({width: left - 14 + 'px'});
		$('#code-contents').css({left: left + 8 + 'px', width: width + 'px'});
		$('#code-context').css({left: left + width + 42 + 'px', width: contextWidth + 'px'});
	},
	onload: function() {

		Structr.fetchHtmlTemplate('code/main', {}, function(html) {

			main.append(html);

			_Code.init();

			codeMain     = $('#code-main');
			codeTree     = $('#tree');
			codeContents = $('#code-contents');
			codeContext  = $('#code-context');

			_Code.moveResizer();
			Structr.initVerticalSlider($('.column-resizer', codeMain), codeResizerLeftKey, 204, _Code.moveResizer);

			$.jstree.defaults.core.themes.dots      = false;
			$.jstree.defaults.dnd.inside_pos        = 'last';
			$.jstree.defaults.dnd.large_drop_target = true;

			codeTree.on('select_node.jstree', _Code.handleTreeClick);

			_Code.loadFavorites(function() {
				_TreeHelper.initTree(codeTree, _Code.treeInitFunction, 'structr-ui-code');
			});

			$(window).off('resize').resize(function() {
				_Code.resize();
			});

			Structr.unblockMenu(100);

			_Code.resize();
			Structr.adaptUiToAvailableFeatures();

			$('#tree-search-input').on('keyup', function(e) {
				var text = $(this).val();
				// implement search
			});
		});

	},
	loadFavorites: function(doneCallback) {

		// load stored favorites
		var favorites = [];
		var count     = 0;

		Command.appData('list', 'code-favorites', '', '', function(result) {

			if (result && result.length && result[0].names && result[0].names.length) {

				result[0].names.forEach(function(name) {

					Command.appData('get', 'code-favorites', name, '', function(value) {

						Command.query('AbstractNode', 1, 1, 'name', true, { id: name }, function(existing) {

							if (existing.length) {

								// make existing entry available to display later
								favorites.push(JSON.parse(value.value));

							} else {

								// delete nonexisting favorite entry from disk
								_Code.deleteRecentlyUsedElement(name);
							}

							// iteration finished?
							if (++count === result[0].names.length) {

								// sort by position
								favorites.sort(function(a, b) {
									if (a.position > b.position) { return 1; }
									if (a.position < b.position) { return -1; }
									return 0;
								});

								for (var f of favorites) {
									_Code.addRecentlyUsedElement(f.id, f.name, f.icon, f.path, true);
								};

								if (typeof doneCallback === 'function') {
									doneCallback();
								}
							}
						}, true);
					});
				});

			} else {

				if (typeof doneCallback === 'function') {
					doneCallback();
				}
			}
		});
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
						text: 'Types',
						children: [
							{ id: 'custom', text: 'Custom', children: true, icon: _Icons.folder_icon },
							{
								id: 'builtin',
								text: 'Built-In',
								children: [
									{ id: 'core', text: 'Core', children: true, icon: _Icons.folder_icon },
									{ id: 'ui',  text: 'Ui',  children: [
										{ id: 'web', text: 'Pages', children: true, icon: _Icons.folder_icon },
										{ id: 'html', text: 'Html', children: true, icon: _Icons.folder_icon }
									], icon: _Icons.folder_icon }
								],
								icon: _Icons.folder_icon
							},
						],
						icon: _Icons.structr_logo_small,
						path: '/',
						state: {
							opened: true
						}
					}
				];

				callback(defaultFilesystemEntries);
				break;

			case 'root':
				_Code.load(null, callback);
				break;

			default:
				_Code.load(obj, callback);
				break;
		}

	},
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#code-main', main));
	},
	load: function(obj, callback) {

		var id = obj.id;

		var displayFunction = function (result) {

			var list = [];

			result.forEach(function(entity) {

				var hasVisibleChildren = _Code.hasVisibleChildren(id, entity);
				var icon               = _Code.getIconForNodeType(entity);

				if (entity.type === 'SchemaNode') {

					var data     = { id: entity.id, type: entity.name, name: entity.name };
					var children = [];

					children.push({
						id: 'properties-' + entity.id,
						text: 'Properties',
						children: true, //entity.schemaProperties.length > 0,
						icon: 'fa fa-sliders gray',
						data: data
					});

					children.push({
						id: 'methods-' + entity.id,
						text: 'Methods',
						children: entity.schemaMethods.length > 0,
						icon: 'fa fa-code gray',
						data: data
					});

					list.push({
						id: entity.id,
						text:  entity.name ? entity.name : '[unnamed]',
						children: children,
						icon: 'fa ' + icon,
						data: {
							type: entity.type,
							name: entity.name
						}
					});

				} else {

					if (entity.inherited) {

						list.push({
							id: entity.id,
							text:  (entity.name ? entity.name : '[unnamed]') + (' (' + (entity.propertyType || '') + ')'),
							children: false,
							icon: 'fa ' + icon,
							li_attr: {
								style: 'color: #aaa;'
							},
							data: {
								type: entity.type,
								name: entity.name
							}
						});

					} else {

						list.push({
							id: entity.id,
							text:  (entity.name ? entity.name : '[unnamed]') + (' (' + (entity.propertyType || '') + ')'),
							children: hasVisibleChildren,
							icon: 'fa ' + icon,
							data: {
								type: entity.type,
								name: entity.name
							}
						});

					}
				}
			});

			callback(list);
		};

		switch (id) {

			case 'custom':
				Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: false}, displayFunction, true);
				break;
			case 'core':
				Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true, isAbstract:false, category: 'core' }, displayFunction, false);
				break;
			case 'web':
				Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true, isAbstract:false, category: 'ui' }, displayFunction, false);
				break;
			case 'html':
				Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true, isAbstract:false, category: 'html' }, displayFunction, false);
				break;
			case 'globals':
				Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: null}, displayFunction, true, 'ui');
				break;
			default:
				var identifier = _Code.splitIdentifier(id);
				switch (identifier.type) {

					case 'outgoing':
						Command.query('SchemaRelationshipNode', methodPageSize, methodPage, 'name', 'asc', {relatedFrom: [identifier.id ]}, displayFunction, true, 'ui');
						break;
					case 'incoming':
						Command.query('SchemaRelationshipNode', methodPageSize, methodPage, 'name', 'asc', {relatedTo: [identifier.id ]}, displayFunction, true, 'ui');
						break;
					case 'properties':
						Command.listSchemaProperties(identifier.id, 'custom', function(result) {
							var filtered = result.filter(function(p) {
								return p.declaringUuid && p.declaringClass !== 'GraphObject' && p.declaringClass !== 'NodeInterface' && p.declaringClass !== 'AbstractNode';
							});
							displayFunction(filtered.map(function(s) {
								if (s.declaringClass === obj.data.type) {
									return {
										id: s.declaringUuid,
										type: 'SchemaProperty',
										name: s.name,
										propertyType: s.declaringPropertyType,
										inherited: false
									};

								} else {
									return {
										id: 'inherited-' + s.declaringUuid + '-' + s.declaringClass + '-' + obj.data.type,
										type: 'SchemaProperty',
										name: s.declaringClass + '.' + s.name,
										propertyType: s.declaringPropertyType,
										inherited: true
									};
								}
							}));
						});
						break;
					case 'methods':
						Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id }, displayFunction, true, 'ui');
						break;
					default:
						Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id}, displayFunction, true, 'ui');
						break;
				}
		}

	},
	clearMainArea: function() {
		fastRemoveAllChildren(codeContents[0]);
		$('#code-button-container').empty();
	},
	displayMethodContents: function(id) {

		if (id === '#' || id === 'root' || id === 'favorites' || id === 'globals') {
			return;
		}

		LSWrapper.setItem(codeLastOpenMethodKey, id);

		_Code.editPropertyContent(undefined, id, 'source', codeContents);
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
	editPropertyContent: function(button, id, key, element, canDelete) {

		var url  = rootUrl + id;
		var text = '';

		$.ajax({
			url: url,
			success: function(result) {
				var entity = result.result;
				_Logger.log(_LogType.CODE, entity.id, methodContents);
				text = entity[key] || '';
				if (Structr.isButtonDisabled(button)) {
					return;
				}
				var contentBox = $('.editor', element);
				var editor = CodeMirror(contentBox.get(0), {
					value: text,
					mode: 'text/javascript',
					lineNumbers: true,
					lineWrapping: false,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true,
					extraKeys: {
						"Ctrl-Space": "autocomplete"
					}
				});

				CodeMirror.registerHelper('hint', 'ajax', _Code.getAutocompleteHint);
				CodeMirror.hint.ajax.async = true;
				CodeMirror.commands.autocomplete = function(mirror) {
					mirror.showHint({ hint: CodeMirror.hint.ajax });
				};

				var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + entity.id));
				if (scrollInfo) {
					editor.scrollTo(scrollInfo.left, scrollInfo.top);
				}

				editor.on('scroll', function() {
					var scrollInfo = editor.getScrollInfo();
					LSWrapper.setItem(scrollInfoKey + '_' + entity.id, JSON.stringify(scrollInfo));
				});

				editor.on('change', function() {
					var type = _Code.getEditorModeForContent(editor.getValue());
					var prev = editor.getOption('mode');
					if (prev !== type) {
						editor.setOption('mode', type);
					}
				});

				editor.id = entity.id;

				var buttonArea = $('.code-button-container', element);
				buttonArea.empty();
				buttonArea.append('<button id="resetMethod' + id + '" disabled="disabled" class="disabled"><i title="Cancel" class="' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" /> Cancel</button>');
				buttonArea.append('<button id="saveMethod' + id + '" disabled="disabled" class="disabled"><i title="Save" class="' + _Icons.getFullSpriteClass(_Icons.floppy_icon) + '" /> Save</button>');

				var codeResetButton  = $('#resetMethod' + id, buttonArea);
				var codeSaveButton   = $('#saveMethod' + id, buttonArea);

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
					editor.setValue(text);
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
					Command.setProperty(entity.id, key, newText);
					text = newText;
					codeSaveButton.prop("disabled", true).addClass('disabled');
					codeResetButton.prop("disabled", true).addClass('disabled');

				});

				if (canDelete) {

					buttonArea.append('<button id="deleteMethod' + id + '"><i title="Delete" class="' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /> Delete</button>');
					var codeDeleteButton = $('#deleteMethod' + id, buttonArea);
					codeDeleteButton.on('click', function(e) {

						e.preventDefault();
						e.stopPropagation();
						_Entities.deleteNode(codeDeleteButton, entity, false, function() {
							_TreeHelper.refreshTree('#tree');
						});
					});
				}

				_Code.resize();

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

				_Code.displayDefaultMethodOptions(entity);

			},
			error: function(xhr, statusText, error) {
				console.log(xhr, statusText, error);
			}
		});
	},
	displayCreateButtons: function(showCreateMethodsButton, showCreateGlobalButton, showCreateTypeButton, schemaNodeId) {

		if (showCreateMethodsButton) {

			Structr.fetchHtmlTemplate('code/method-button', {}, function(html) {

				$('#code-button-container').append(html);

				$('#add-method-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', $('#schema-method-name').val() || 'unnamed', schemaNodeId);
				});

				$('#add-onCreate-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', 'onCreate', schemaNodeId);
				});

				$('#add-onSave-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', 'onSave', schemaNodeId);
				});
			});

		}

		if (showCreateGlobalButton) {

			Structr.fetchHtmlTemplate('code/global-button', {}, function(html) {

				$('#code-button-container').append(html);

				$('#create-global-method-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', $('#schema-method-name').val() || 'unnamed');
				});
			});

		}

		if (showCreateTypeButton) {

			Structr.fetchHtmlTemplate('code/type-button', {}, function(html) {

				$('#code-button-container').append(html);

				$('#add-type-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaNode', $('#schema-type-name').val());
				});
			});

		}
	},
	createMethodAndRefreshTree: function(type, name, schemaNode) {

		_Code.showSchemaRecompileMessage();
		Command.create({
			type: type,
			name: name,
			schemaNode: schemaNode,
			source: ''
		}, function() {
			_TreeHelper.refreshTree('#tree');
			_Code.hideSchemaRecompileMessage();
		});
	},
	getIconForNodeType: function(entity) {

		// set global default
		var icon = 'fa-file-code-o gray';

		switch (entity.type) {

			case 'SchemaMethod':

				switch (entity.codeType) {
					case 'java':
						icon = 'fa-dot-circle-o red';
						break;
					default:
						icon = 'fa-circle-o blue';
						break;
				}
				break;

			case 'SchemaProperty':
				return 'fa fa-' + _Code.getIconForPropertyType(entity.propertyType);
		}

		return icon;
	},
	getIconForPropertyType: function(propertyType) {

		var icon = 'exclamation-triangle';

		switch (propertyType) {

			case "Custom":
			case "IdNotion":
			case "Notion":
				icon = 'magic';
				break;

			case "IntegerArray":
			case "StringArray":
				icon = 'magic';
				break;

			case 'Boolean':      icon = 'check'; break;
			case "Cypher":       icon = 'database'; break;
			case 'Date':         icon = 'calendar'; break;
			case "Double":       icon = 'superscript'; break;
			case "Enum":         icon = 'list'; break;
			case "Function":     icon = 'code-fork'; break;
			case 'Integer':      icon = 'calculator'; break;
			case "Long":         icon = 'calculator'; break;
			case 'String':       icon = 'pencil-square-o'; break;
			default:             icon = 'sliders'; break;
		}

		return icon;
	},
	splitIdentifier: function(id) {

		var parts = id.split('-');
		if (parts.length >= 2) {

			switch (parts.length) {
				case 4:
				case 3:
					var extra   = parts[2].trim();
				case 2:
					var entity  = parts[1].trim();
					var subtype = parts[0].trim();
			}

			return { id: entity, type: subtype, extra: extra };
		}

		return { id : id, type: id };
	},
	convertOutgoingRelationshipNodesForTree: function(related) {

		var list = [];

		related.forEach(function(rel) {

			list.push({
				id: 'out-' + rel.id,
				text: rel.targetJsonName + ': ' + rel.targetMultiplicity,
				children: false,
				icon: 'fa fa-arrow-right gray'
			});
		});

		return list;
	},
	convertIncomingRelationshipNodesForTree: function(related) {

		var list = [];

		related.forEach(function(rel) {

			list.push({
				id: 'in-' + rel.id,
				text: rel.sourceJsonName + ': ' + rel.sourceMultiplicity,
				children: false,
				icon: 'fa fa-arrow-left gray'
			});
		});

		return list;
	},
	hasVisibleChildren: function(id, entity) {

		var hasVisibleChildren = false;

		if (entity.schemaMethods) {

			entity.schemaMethods.forEach(function(m) {

				if (id === 'custom' || !m.isPartOfBuiltInSchema) {

					hasVisibleChildren = true;
				}
			});
		}

		return hasVisibleChildren;
	},
	handleTreeClick: function(evt, data) {

		if (data && data.node && data.node.id) {

			var selection = {
				id: data.node.id
			};

			if (data.node.data) {
				selection.type = data.node.data.type;
			}

			_Code.handleSelection(selection);
		}
	},
	handleSelection: function(data) {

		// clear page
		_Code.clearMainArea();

		var identifier = _Code.splitIdentifier(data.id);
		switch (identifier.type) {

			// global types that are not associated with an actual entity
			case 'builtin':
			case 'core':
			case 'html':
			case 'root':
			case 'ui':
			case 'web':
				_Code.displayContent(identifier.type);
				break;

			case 'globals':
				_Code.displayGlobalMethodsContent();
				break;

			case 'custom':
				_Code.displayCustomTypesContent(identifier.type);
				break;

			// properties (with uuid)
			case 'properties':
				_Code.displayPropertiesContent(identifier);
				break;

			// methods (with uuid)
			case 'methods':
				_Code.displayMethodsContent(identifier);
				break;

			// outgoing relationships (with uuid)
			case 'outgoing':
				_Code.displayOutgoingRelationshipsContent(identifier);
				break;

			// incoming relationships (with uuid)
			case 'incoming':
				_Code.displayIncomingRelationshipsContent(identifier);
				break;

			// outgoing relationship (with uuid)
			case 'out':
				_Code.displayOutRelationshipContent(identifier);
				break;

			// incoming relationship (with uuid)
			case 'in':
				_Code.displayInRelationshipContent(identifier);
				break;

			case 'inherited':
				console.log(identifier);
				_Code.findAndOpenNode('Types/Custom/' + identifier.extra + '/Properties/' + identifier.id);
				break;

			// other (click on an actual object)
			default:
				_Code.handleNodeObjectClick(data);
				break;
		}
	},
	handleNodeObjectClick: function(data) {

		if (data.type) {

			switch (data.type) {

				case 'SchemaProperty':
					_Code.displayPropertyDetails(data);
					break;

				case 'SchemaMethod':
					Command.get(data.id, null, function(result) {
						_Code.updateRecentlyUsed(data, result);
						Structr.fetchHtmlTemplate('code/method', { method: result }, function(html) {
							codeContents.empty();
							codeContents.append(html);
							_Code.displayMethodContents(data.id);
						});
					});
					break;

				case 'SchemaNode':
					Command.get(data.id, null, function(result) {
						Structr.fetchHtmlTemplate('code/type', { type: result }, function(html) {

							codeContents.empty();
							codeContents.append(html);
							_Code.displayCreateButtons(true, false, false, result.id);

							$.ajax({
								url: '/structr/rest/SchemaNode/' + result.id + '/getGeneratedSourceCode',
								method: 'post',
								statusCode: {
									200: function(result) {

										var container = $('.editor');
										var editor    = CodeMirror(container[0], {
											value: result.result,
											mode: 'text/x-java',
											lineNumbers: true
										});

										$('.CodeMirror').height('100%');
										editor.refresh();

									}
								}
							});
						});
					});
					break;
			}

		} else {

			switch (data.id) {

				case 'globals':
					_Code.displayCreateButtons(false, true, false, '');
					break;

				case 'custom':
					_Code.displayCreateButtons(false, false, true, '');
					break;
			}
		}
	},
	displayContent: function(templateName) {
		Structr.fetchHtmlTemplate('code/' + templateName, { }, function(html) {
			codeContents.append(html);
		});
	},
	displayCustomTypesContent: function() {
		Structr.fetchHtmlTemplate('code/custom', { }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			_Code.displayCreateButton('#create-type-container', 'magic', 'new', 'type', '', { type: 'SchemaNode' }, _Code.displayCustomTypesContent);
		});
	},
	displayGlobalMethodsContent: function() {
		Structr.fetchHtmlTemplate('code/globals', { }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			_Code.displayCreateButton('#create-method-container', 'magic', 'new', 'global schema method', '', { type: 'SchemaMethod' }, _Code.displayGlobalMethodsContent);
		});
	},
	displayPropertiesContent: function(selection) {
		Structr.fetchHtmlTemplate('code/properties', { identifier: selection }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			var callback = function() { _Code.displayPropertiesContent(selection); };
			var data     = { type: 'SchemaProperty', schemaNode: selection.id };
			var id       = '#create-property-container';

			_Code.displayCreatePropertyButton(id, 'String',   data, callback);
			_Code.displayCreatePropertyButton(id, 'Boolean',  data, callback);
			_Code.displayCreatePropertyButton(id, 'Integer',  data, callback);
			_Code.displayCreatePropertyButton(id, 'Long',     data, callback);
			_Code.displayCreatePropertyButton(id, 'Double',   data, callback);
			_Code.displayCreatePropertyButton(id, 'Date',     data, callback);
			_Code.displayCreatePropertyButton(id, 'Function', data, callback);
			_Code.displayCreatePropertyButton(id, 'Cypher',   data, callback);
		});
	},
	displayMethodsContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/methods', { identifier: identifier }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			var callback = function() { _Code.displayMethodsContent(identifier); };
			var data     = { type: 'SchemaMethod', schemaNode: identifier.id };
			_Code.displayCreateButton('#create-method-container', 'magic', 'on-create', '<b>onCreate</b> method', 'onCreate', data, callback);
			_Code.displayCreateButton('#create-method-container', 'magic', 'on-save',   '<b>onSave</b> method',     'onSave', data, callback);
			_Code.displayCreateButton('#create-method-container', 'magic', 'new',       'schema method',                  '', data, callback);
		});
	},
	displayOutgoingRelationshipsContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/outgoing-relationships', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayIncomingRelationshipsContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/incoming-relationships', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayOutRelationshipContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/outgoing-relationship', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayInRelationshipContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/incoming-relationship', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayPropertyDetails: function(selection) {

		Command.get(selection.id, null, function(result) {

			_Code.updateRecentlyUsed(selection, result);

			switch (result.propertyType) {
				case 'Cypher':
					_Code.displayCypherPropertyDetails(result);
					break;
				case 'Function':
					_Code.displayFunctionPropertyDetails(result);
					break;
				case 'String':
					_Code.displayStringPropertyDetails(result);
					break;
				case 'Boolean':
					_Code.displayBooleanPropertyDetails(result);
					break;
				default:
					_Code.displayDefaultPropertyDetails(result);
					break;
			}
		});
	},
	displayFunctionPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/function-property', { property: property }, function(html) {

			codeContents.empty();
			codeContents.append(html);
			_Code.editPropertyContent(undefined, property.id, 'readFunction',  $('#read-code-container'),  false);
			_Code.editPropertyContent(undefined, property.id, 'writeFunction', $('#write-code-container'), false);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayCypherPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/cypher-property', { property: property }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			_Code.editPropertyContent(undefined, property.id, 'format', $('#cypher-code-container'));
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayStringPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/string-property', { property: property }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayBooleanPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/boolean-property', { property: property }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayDefaultPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/default-property', { property: property }, function(html) {
			codeContents.empty();
			codeContents.append(html);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayDefaultPropertyOptions: function(property, callback) {

		// default buttons
		Structr.fetchHtmlTemplate('code/property-options', { property: property }, function(html) {

			var buttons = $('#property-buttons');
			buttons.prepend(html);

			$('.toggle-checkbox', buttons).on('click', function() {
				var targetId = $(this).data('target');
				if (targetId) {
					var box = $(targetId);
					box.click();
				}
			});

			_Code.activatePropertyValueInput('property-name-input',         property.id, 'name');
			_Code.activatePropertyValueInput('property-content-type-input', property.id, 'contentType');
			_Code.activatePropertyValueInput('property-dbname-input',       property.id, 'dbname');
			_Code.activatePropertyValueInput('property-format-input',       property.id, 'format');
			_Code.activatePropertyValueInput('property-default-input',      property.id, 'defaultValue');

			if (property.propertyType === 'Function') {
				_Code.activatePropertyValueInput('property-type-hint-input',    property.id, 'typeHint');
			} else {
				$('#property-type-hint-input').parent().remove();
			}

			$('input[type=checkbox]', buttons).on('click', function() {
				var elem         = $(this);
				var propertyName = elem.data('property');
				var data         = {};
				data[propertyName] = elem.prop('checked');
				_Code.lockPropertyOptions();
				_Code.showSchemaRecompileMessage();
				Command.setProperties(property.id, data, function() {
					_Code.unlockPropertyOptions();
					blinkGreen(elem.parent());
					_Code.refreshTree();
					_Code.hideSchemaRecompileMessage();
				});
			})

			// set value from property
			$('input[type=checkbox]', buttons).each(function(i) {
				var propertyName = $(this).data('property');
				if (propertyName && propertyName.length) {
					$(this).prop('checked', property[propertyName]);
				}
			});

			if (typeof callback === 'function') {
				callback();
			}
		});
	},
	displayDefaultMethodOptions: function(method, callback) {

		// default buttons
		Structr.fetchHtmlTemplate('code/method-options', { method: method }, function(html) {

			var buttons = $('#method-buttons');
			buttons.prepend(html);

			$('.toggle-checkbox', buttons).on('click', function() {
				var targetId = $(this).data('target');
				if (targetId) {
					var box = $(targetId);
					box.click();
				}
			});

			_Code.activatePropertyValueInput('method-name-input', method.id, 'name');

			$('input[type=checkbox]', buttons).on('click', function() {
				var elem         = $(this);
				var propertyName = elem.data('property');
				var data         = {};
				data[propertyName] = elem.prop('checked');
				_Code.lockPropertyOptions();
				_Code.showSchemaRecompileMessage();
				Command.setProperties(method.id, data, function() {
					_Code.unlockPropertyOptions();
					blinkGreen(elem.parent());
					_Code.refreshTree();
					_Code.hideSchemaRecompileMessage();
				});
			})

			// set value from property
			$('input[type=checkbox]', buttons).each(function(i) {
				var propertyName = $(this).data('property');
				if (propertyName && propertyName.length) {
					$(this).prop('checked', method[propertyName]);
				}
			});

			if (typeof callback === 'function') {
				callback();
			}
		});
	},
	lockPropertyOptions: function() {
		$('#property-options').find('input').attr('disabled', true);
	},
	unlockPropertyOptions: function() {
		$('#property-options').find('input').attr('disabled', false);
	},
	activatePropertyValueInput: function(inputId, id, name) {
		$('input#' + inputId).on('blur', function() {
			var elem     = $(this);
			var previous = elem.attr('value');
			if (previous !== elem.val()) {
				var data   = {};
				data[name] = elem.val();
				_Code.lockPropertyOptions();
				_Code.showSchemaRecompileMessage();
				Command.setProperties(id, data, function() {
					_Code.unlockPropertyOptions();
					blinkGreen(elem);
					_Code.refreshTree();
					_Code.hideSchemaRecompileMessage();
				});
			}
		})
	},
	getAutocompleteHint: function(editor, callback) {

		var cursor = editor.getCursor();
		var word   = editor.findWordAt(cursor);
		var prev   = editor.findWordAt({ line: word.anchor.line, ch: word.anchor.ch - 2 });
		var range1 = editor.getRange(prev.anchor, prev.head);
		var range2 = editor.getRange(word.anchor, word.head);
		var type   = _Code.getEditorModeForContent(editor.getValue());
		Command.autocomplete('', '', range2, range1, '', cursor.line, cursor.ch, type, function(result) {
			var inner  = { from: cursor, to: cursor, list: result };
			callback(inner);
		});
	},
	activateCreateDialog: function(suffix, presetValue, nodeData, cancelCallback) {
		var button = $('div#create-object-button-' + suffix);
		button.on('click.create-object-' + suffix, function() {

			// hover / overlay effect
			button.css({
				'margin':    '17px 17px -54px 17px',
				'z-index':   1000,
				'border':    '4px solid #81ce25',
				'box-shadow': '0px 0px 36px rgba(127,127,127,.2)'
			});

			Structr.fetchHtmlTemplate('code/create-object-form', { value: presetValue, suffix: suffix }, function(html) {
				button.append(html);
				$('#new-object-name-' + suffix).focus();
				$('#new-object-name-' + suffix).on('keyup', function(e) {
					if (e.keyCode === 27) { cancelCallback(); }
					if (e.keyCode === 13) { $('#create-button-' + suffix).click(); }
				});
				button.off('click.create-object-' + suffix);
				$('#cancel-button-' + suffix).on('click', function() {
					cancelCallback();
				});
				$('#create-button-' + suffix).on('click', function() {
					var data = Object.assign({}, nodeData);
					data['name'] = $('#new-object-name-' + suffix).val();
					_Code.showSchemaRecompileMessage();
					Command.create(data, function() {
						_Code.refreshTree();
						_Code.displayCustomTypesContent();
						_Code.hideSchemaRecompileMessage();
					});
				});
			});
		});
	},
	displayCreateButton: function(targetId, icon, suffix, name, presetValue, createData, callback) {
		Structr.fetchHtmlTemplate('code/create-button', { icon: icon, suffix: suffix, name: name }, function(html) {
			$(targetId).append(html);
			_Code.activateCreateDialog(suffix, presetValue, createData , callback);
		});
	},
	displayCreatePropertyButton: function(targetId, type, nodeData, callback) {
		var data = Object.assign({}, nodeData);
		data['propertyType'] = type;
		_Code.displayCreateButton(targetId, _Code.getIconForPropertyType(type), type.toLowerCase(), '<b>' + type + '</b> property', '', data, callback);
	},
	getEditorModeForContent: function(content) {
		if (content.indexOf('{') === 0) {
			return 'text/javascript';
		}
		return 'text';
	},
	updateRecentlyUsed: function(selection, entity) {

		var name     = entity.name;
		var id       = entity.id;

		switch (entity.type) {
			case 'SchemaNode':
				name = 'Class ' + entity.name;
				break;
			case 'SchemaMethod':
				if (entity.schemaNode && entity.schemaNode.name) {
					name = entity.schemaNode.name + '.' + entity.name + '()';
				} else {
					name = 'Global method ' + entity.name + '()';
				}
				break;
			case 'SchemaProperty':
				if (entity.schemaNode && entity.schemaNode.name) {
					name = entity.propertyType + 'Property ' + entity.schemaNode.name + '.' + entity.name;
				}
				break;
		}
		_Code.addRecentlyUsedElement(id, name, _Code.getIconForNodeType(entity), _Code.getTreePath(selection.id, entity.name));
	},
	addRecentlyUsedElement: function(id, name, icon, path, fromStorage) {
		Structr.fetchHtmlTemplate('code/recently-used-button', { id: id, name: name, icon: icon }, function(html) {
			var ctx  = $('#code-context');
			var elem = $('#recently-used-' + id, ctx);
			if (!elem.length) {
				if (!fromStorage) {
					Command.appData('add', 'code-favorites', id, JSON.stringify({id: id, name: name, icon: icon, path: path, position: $('.code-favorite').length }));
				}
				elem.off('click.recently-used');
				elem.remove();
				ctx.append(html);
				$('#recently-used-' + id).on('click.recently-used', function() {
					_Code.findAndOpenNode(path);
				});
				$('#remove-recently-used-' + id).on('click.recently-used', function(e) {
					e.stopPropagation();
					_Code.deleteRecentlyUsedElement(id);
				});
			} else {

				Command.appData('replace', 'code-favorites', id, JSON.stringify({
					id: id,
					name: name,
					icon: icon,
					path: path,
					position: $('.code-favorite').length
				}));
			}
		});
	},
	deleteRecentlyUsedElement: function(id) {
		Command.appData('delete', 'code-favorites', id, '', function() {
			$('#recently-used-' + id).remove();
		});
	},
	getTreePath: function(id, name) {
		var path    = [ name ];
		var tree    = $('#tree').jstree(true);
		var current = id;

		while (current) {

			var node = tree.get_node(current);
			if (node) {
				current = tree.get_parent(node);
				if (current && current !== '#') {
					var parent = tree.get_node(current);
					if (parent) {
						path.unshift(parent.text);
					} else {
						path.unshift(current);
					}
				}
			}
		}

		return path.join('/');
	},
	findAndOpenNode: function(path) {
		console.log(path);
		var tree = $('#tree').jstree(true);
		_Code.findAndOpenNodeRecursive(tree, path, 0);
	},
	findAndOpenNodeRecursive: function(tree, path, depth, node) {
		var parts = path.split('/');
		if (path.length === 0) { return; }
		if (parts.length < 1) {	return; }
		if (depth > 15) { return; }

		var pos  = path.indexOf('/', 1);
		var tail = pos >= 0 ? path.substring(pos + 1) : '';
		var id   = parts[0];

		if (id.length === 0) {
			id  = parts[1];
		}

		var data     = tree.get_json(node);
		var searchId = _Code.findNodeIdByText(data, id);

		if (tail.length === 0) {

			// node found, activate
			tree.activate_node(searchId);

			// also scroll into view if node is in tree
			var domNode = document.getElementById( tree.get_parent(tree.get_parent(searchId)) );
			if (domNode) {
				domNode.scrollIntoView();
			}

		} else {

			tree.open_node(searchId, function(n) {
				_Code.findAndOpenNodeRecursive(tree, tail, depth + 1, n);
			});
		}
	},
	findNodeIdByText: function(data, text) {

		if (data && data.length) {

			for (var i=0; i<data.length; i++) {

				var result = _Code.findNodeIdByTextRecursive(data[i], text);
				if (result) {

					return result;
				}
			}

		} else {

			return _Code.findNodeIdByTextRecursive(data, text);
		}
	},
	findNodeIdByTextRecursive: function(data, text) {

		if (data.data && data.data.name && data.data.name === text) {
			return data.id;
		}

		if (data.id && data.id === text) {
			return data.id;
		}

		//if (data.text && data.text.indexOf(text) >= 0) {
		if (data.text && data.text === text) {

			return data.id;

		} else if (data.children) {

			var children = data.children;
			for (var i=0; i<children.length; i++) {

				var result = _Code.findNodeIdByTextRecursive(children[i], text);
				if (result) {

					return result;
				}
			}
		}
	},
	showSchemaRecompileMessage: function() {
		Structr.showLoadingMessage('Schema is compiling', 'Please wait', 200);
	},
	hideSchemaRecompileMessage:  function() {
		Structr.hideLoadingMessage();
	}
};
