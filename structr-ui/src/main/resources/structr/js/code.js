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
				height: windowHeight - headerOffsetHeight + 5 + 'px'
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

		var width = $(window).width() - left - 280;

		$('#code-tree').css({width: left - 14 + 'px'});
		$('#code-contents').css({left: left + 8 + 'px', width: width + 'px'});
		$('#code-context').css({left: left + width + 42 + 'px', width: '200px'});
	},
	onload: function() {

		Structr.fetchHtmlTemplate('code/main', {}, function(html) {

			main.append(html);

			_Code.init();

			codeMain     = $('#code-main');
			codeTree     = $('#code-tree');
			codeContents = $('#code-contents');
			codeContext  = $('#code-context');

			_Code.moveResizer();
			Structr.initVerticalSlider($('.column-resizer', codeMain), codeResizerLeftKey, 204, _Code.moveResizer);

			$.jstree.defaults.core.themes.dots      = false;
			$.jstree.defaults.dnd.inside_pos        = 'last';
			$.jstree.defaults.dnd.large_drop_target = true;

			codeTree.on('select_node.jstree', _Code.handleTreeClick);

			_TreeHelper.initTree(codeTree, _Code.treeInitFunction, 'structr-ui-code');

			$(window).off('resize').resize(function() {
				_Code.resize();
			});

			Structr.unblockMenu(100);

			_Code.resize();
			Structr.adaptUiToAvailableFeatures();
		});

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

			result.forEach(function(entity) {

				var hasVisibleChildren = _Code.hasVisibleChildren(id, entity);
				var icon               = _Code.getIconForPropertyType(entity);

				if (entity.type === 'SchemaNode') {

					var data     = { id: entity.id, type: entity.name };
					var children = [];

					if (entity.relatedTo.length) {
						children.push({
							id: 'outgoing-' + entity.id,
							text: 'Outgoing Relationships',
							children: _Code.convertOutgoingRelationshipNodesForTree(entity.relatedTo),
							icon: 'fa fa-chain gray',
							data: data
						});
					}

					if (entity.relatedFrom.length) {
						children.push({
							id: 'incoming-' + entity.id,
							text: 'Incoming Relationships',
							children: _Code.convertIncomingRelationshipNodesForTree(entity.relatedFrom),
							icon: 'fa fa-chain gray',
							data: data
						});
					}

					children.push({
						id: 'properties-' + entity.id,
						text: 'Properties',
						children: entity.schemaProperties.length > 0,
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
							type: entity.type
						}
					});

				} else {

					list.push({
						id: entity.id,
						text:  (entity.name ? entity.name : '[unnamed]') + (' (' + (entity.propertyType || '') + ')'),
						children: hasVisibleChildren,
						icon: 'fa ' + icon,
						data: {
							type: entity.type
						}
					});
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
						Command.query('SchemaProperty', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id }, displayFunction, true, 'ui');
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
				var lineWrapping = LSWrapper.getItem(lineWrappingKey);
				var editor = CodeMirror(contentBox.get(0), {
					value: text,
					mode: 'javascript',
					lineNumbers: true,
					lineWrapping: lineWrapping,
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
							_TreeHelper.refreshTree('#code-tree');
						});
					});
				}

				_Code.resize();

				if (entity.isTemplate) {
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

		Command.create({
			type: type,
			name: name,
			schemaNode: schemaNode,
			source: ''
		}, function() {
			_TreeHelper.refreshTree('#code-tree');
		});
	},
	getIconForPropertyType: function(entity) {

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

				// set default
				icon = 'fa-exclamation-triangle';

				switch (entity.propertyType) {

					case "Custom":
					case "IdNotion":
					case "Notion":
						icon = 'fa-magic';
						break;

					case "IntegerArray":
					case "StringArray":
						icon = 'fa-magic';
						break;

					case 'Boolean':      icon = 'fa-check'; break;
					case "Cypher":       icon = 'fa-database'; break;
					case 'Date':         icon = 'fa-calendar'; break;
					case "Double":       icon = 'fa-superscript'; break;
					case "Enum":         icon = 'fa-list'; break;
					case "Function":     icon = 'fa-code-fork'; break;
					case 'Integer':      icon = 'fa-calculator'; break;
					case "Long":         icon = 'fa-calculator'; break;
					case "Password":     icon = 'fa-key'; break;
					case 'String':       icon = 'fa-pencil-square-o'; break;
				}
				break;
		}

		return icon;
	},
	splitIdentifier: function(id) {

		var parts = id.split('-');
		if (parts.length == 2) {

			var subtype = parts[0].trim();
			var entity  = parts[1].trim();

			return { id: entity, type: subtype };
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

			// clear page
			_Code.clearMainArea();

			var identifier = _Code.splitIdentifier(data.node.id);
			switch (identifier.type) {

				// global types that are not associated with an actual entity
				case 'builtin':
				case 'core':
				case 'custom':
				case 'globals':
				case 'html':
				case 'root':
				case 'ui':
				case 'web':
					_Code.displayContent(identifier.type);
					break;

				// properties (with uuid)
				case 'properties':
					_Code.displayPropertiesContent(data.node.data);
					break;

				// methods (with uuid)
				case 'methods':
					_Code.displayMethodsContent(data.node.data);
					break;

				// outgoing relationships (with uuid)
				case 'outgoing':
					_Code.displayOutgoingRelationshipsContent(data.node.data);
					break;

				// incoming relationships (with uuid)
				case 'incoming':
					_Code.displayIncomingRelationshipsContent(data.node.data);
					break;

				// outgoing relationship (with uuid)
				case 'out':
					_Code.displayOutRelationshipContent(data.node.data);
					break;

				// incoming relationship (with uuid)
				case 'in':
					_Code.displayInRelationshipContent(node.data.node);
					break;

				// other (click on an actual object)
				default:
					_Code.handleNodeObjectClick(evt, data);
					break;
			}
		}
	},
	handleNodeObjectClick: function(evt, data) {

		if (data.node && data.node.data && data.node.data.type) {

			switch (data.node.data.type) {

				case 'SchemaProperty':
					_Code.displayPropertyDetails(data.node.id);
					break;

				case 'SchemaMethod':
					Command.get(data.node.id, null, function(result) {
						Structr.fetchHtmlTemplate('code/method', { method: result }, function(html) {
							codeContents.append(html);
							_Code.displayMethodContents(data.node.id);
						});
					});
					break;

				case 'SchemaNode':
					Command.get(data.node.id, null, function(result) {
						Structr.fetchHtmlTemplate('code/type', { type: result }, function(html) {
							codeContents.append(html);
							_Code.displayCreateButtons(true, false, false, result.id);
						});
					});
					break;
			}

		} else {

			switch (data.node.id) {

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
	displayGlobalMethodsContent: function() {
		Structr.fetchHtmlTemplate('code/globals', { }, function(html) {
			codeContents.append(html);
			Command.query('SchemaMethod', 1000, 1, 'name', 'asc', {schemaNode: null}, function(method) {

				$('#global-method-list').append('<div style="">' + method.name + '</div>');

			}, true, 'ui');
		});
	},
	displayPropertiesContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/properties', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayMethodsContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/methods', { identifier: identifier }, function(html) {
			codeContents.append(html);
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
	displayPropertyDetails: function(id) {

		Command.get(id, null, function(result) {

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
			}
		});
	},
	displayFunctionPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/function-property', { property: property }, function(html) {

			codeContents.append(html);
			_Code.editPropertyContent(undefined, property.id, 'readFunction',  $('#read-code-container'),  false);
			_Code.editPropertyContent(undefined, property.id, 'writeFunction', $('#write-code-container'), false);
			_Code.displayDefaultPropertyButtons(property);
		});
	},
	displayCypherPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/cypher-property', { property: property }, function(html) {
			codeContents.append(html);
			_Code.editPropertyContent(undefined, property.id, 'format', $('#cypher-code-container'));
			_Code.displayDefaultPropertyButtons(property);
		});
	},
	displayStringPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/string-property', { property: property }, function(html) {
			codeContents.append(html);
			_Code.displayDefaultPropertyButtons(property);
		});
	},
	displayBooleanPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/boolean-property', { property: property }, function(html) {
			codeContents.append(html);
			_Code.displayDefaultPropertyButtons(property);
		});
	},
	displayDefaultPropertyButtons: function(property) {

		// default buttons
		Structr.fetchHtmlTemplate('code/property-buttons', { property: property }, function(html) {

			var buttons = $('#property-buttons');

			buttons.append(html);

			$('.toggle-checkbox', buttons).on('click', function() {
				var targetId = $(this).data('target');
				if (targetId) {
					var box = $(targetId);
					box.prop("checked", !box.prop("checked"));
				}
			});

			$('input[type=checkbox]', buttons).on('click', function() {

			})

			// set value from property
			$('input[type=checkbox]', buttons).each(function(i) {
				var propertyName = $(this).data('property');
				if (propertyName && propertyName.length) {
					$(this).prop('checked', property[propertyName]);
				}
			});

		});
	},
	getAutocompleteHint: function(editor, callback) {

		var cursor = editor.getCursor();

		var word   = editor.findWordAt(cursor);
		var prev   = editor.findWordAt({ line: word.anchor.line, ch: word.anchor.ch - 2 });
		var range1 = editor.getRange(prev.anchor, prev.head);
		var range2 = editor.getRange(word.anchor, word.head);

		// autocomplete: function(id, type, currentToken, previousToken, thirdToken, line, cursorPosition, callback) {
		Command.autocomplete('', '', range2, range1, '', cursor.line, cursor.ch, function(result) {

			var inner  = { from: cursor, to: cursor, list: result };
			callback(inner);
		});
	}
};
