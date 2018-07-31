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
var main, flowsMain, flowsTree, flowsCanvas;
var drop;
var selectedElements = [];
var activeMethodId, methodContents = {};
var currentWorkingDir;
var methodPageSize = 10000, methodPage = 1;
var timeout, attempts = 0, maxRetry = 10;
var displayingFavorites = false;
var flowsLastOpenMethodKey = 'structrFlowsLastOpenMethodKey_' + port;
var flowsResizerLeftKey = 'structrFlowsResizerLeftKey_' + port;
var activeFlowsTabPrefix = 'activeFlowsTabPrefix' + port;

$(document).ready(function() {
	Structr.registerModule(_Flows);
});

var _Flows = {
	_moduleName: 'flows',
	init: function() {

		_Logger.log(_LogType.FLOWS, '_Flows.init');

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

	},
	resize: function() {

		var windowHeight = $(window).height();
		var headerOffsetHeight = 100;

		if (flowsTree) {
			flowsTree.css({
				height: windowHeight - headerOffsetHeight + 7 + 'px'
			});
		}

		if (flowsCanvas) {
			flowsCanvas.css({
				height: windowHeight - headerOffsetHeight - 48 + 'px'
			});
		}

		_Flows.moveResizer();
		Structr.resize();

		if (flowsCanvas) {
			flowsCanvas.find('.node').each(function() {
				_Entities.setMouseOver($(this), true);
			});
		}

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(flowsResizerLeftKey) || 300;
		$('.column-resizer', flowsMain).css({ left: left });

		$('#flows-tree').css({width: left - 14 + 'px'});
		$('#flows-canvas').css({left: left + 8 + 'px', width: $(window).width() - left - 47 + 'px'});
	},
	onload: function() {

		_Flows.init();

		main.append('<div id="flows-main"><div class="column-resizer"></div><div class="fit-to-height" id="flows-tree-container"><div id="flows-tree"></div></div><div class="fit-to-height" id="flows-canvas-container"><div id="flows-button-container"></div><div id="flows-canvas"></div></div>');
		flowsMain = $('#flows-main');

		flowsTree = $('#flows-tree');
		flowsCanvas = $('#flows-canvas');

		_Flows.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', flowsMain), flowsResizerLeftKey, 204, _Flows.moveResizer);

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		flowsTree.on('select_node.jstree', function(evt, data) {

			if (data.node && data.node.data && data.node.data.type) {

				if (data.node.data.type === 'FlowContainer') {

					// display flow canvas
					console.log('load flow', data.node.id);
					
					flowsCanvas.append('<iframe width="100%" height="100%" src="js/flow-editor/src/html/pages/flow.html?id=' + data.node.id + '"></iframe>');
					
				} else {

					if (data.node.state.opened) {
						flowsTree.jstree('close_node', data.node.id);
						data.node.state.openend = false;
					} else {
						flowsTree.jstree('open_node', data.node.id);
						data.node.state.openend = true;
					}
				}

			} else {

				if (data.node.state.opened) {
					flowsTree.jstree('close_node', data.node.id);
					data.node.state.openend = false;
				} else {
					flowsTree.jstree('open_node', data.node.id);
					data.node.state.openend = true;
				}
			}
		});

		_TreeHelper.initTree(flowsTree, _Flows.treeInitFunction, 'structr-ui-flows');

		$(window).off('resize').resize(function() {
			_Flows.resize();
		});

		Structr.unblockMenu(100);

		_Flows.resize();
		Structr.adaptUiToAvailableFeatures();

	},
	refreshTree: function() {

		_TreeHelper.refreshTree(flowsTree);

	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':

				var defaultEntries = [
					{
						id: 'globals',
						text: 'Favorites',
						children: true,
						icon: _Icons.star_icon
					},
					{
						id: 'root',
						text: 'Flows',
						children: true,
//						children: [
//							{ id: 'custom', text: 'Custom', children: true, icon: _Icons.folder_icon },
//							{
//								id: 'builtin',
//								text: 'Built-In',
//								children: [
//									{ id: 'core', text: 'Core', children: true, icon: _Icons.folder_icon },
//									{ id: 'ui',  text: 'Ui',  children: [
//										{ id: 'web', text: 'Pages', children: true, icon: _Icons.folder_icon },
//										{ id: 'html', text: 'Html', children: true, icon: _Icons.folder_icon }
//									], icon: _Icons.folder_icon }
//								],
//								icon: _Icons.folder_icon
//							},
//						],
						icon: _Icons.structr_logo_small,
						path: '/',
						state: {
							opened: true,
							selected: true
						}
					}
				];

				callback(defaultEntries);
				break;

			case 'root':
				_Flows.load(null, callback);
				break;

			default:
				_Flows.load(obj.id, callback);
				break;
		}

	},
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#flows-main', main));
	},
	load: function(id, callback) {

		var displayFunction = function (result) {

			var list = [];
                        
			result.forEach(function(d) {

				var icon = 'fa-file-code-o gray';
				switch (d.type) {
					case "FlowContainer":
						switch (d.codeType) {
							case 'java':
								icon = 'fa-dot-circle-o red';
								break;
							default:
								icon = 'fa-circle-o blue';
								break;
						}
				}

				var hasVisibleChildren = false;

				if (d.flowNodes) {
					d.flowNodes.forEach(function(m) {
						hasVisibleChildren = true;
					});
				}

				list.push({
					id: d.id,
					text:  d.name ? d.name : '[unnamed]',
					children: hasVisibleChildren,
					icon: 'fa ' + icon,
					data: {
						type: d.type
					}
				});
			});

			callback(list);
		};

		if (!id) {

			Command.list('FlowContainer', false, methodPageSize, methodPage, 'name', 'asc', 'id,type,name,flowNodes', displayFunction);

		} else {

			switch (id) {

//				case 'custom':
//					Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: false}, displayFunction, true);
//					break;
//				case 'core':
//					Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true, isAbstract:false, category: 'core' }, displayFunction, false);
//					break;
//				case 'web':
//					Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true, isAbstract:false, category: 'ui' }, displayFunction, false);
//					break;
//				case 'html':
//					Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true, isAbstract:false, category: 'html' }, displayFunction, false);
//					break;
//				case 'globals':
//					Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: null}, displayFunction, true, 'ui');
//					break;
				default:
					Command.query('FlowContainer', methodPageSize, methodPage, 'name', 'asc', {flowContainer: id}, displayFunction, true, 'ui');
					break;
			}
		}

	},
	fileOrFolderCreationNotification: function (newFileOrFolder) {
		if ((currentWorkingDir === undefined || currentWorkingDir === null) && newFileOrFolder.parent === null) {
			_Flows.appendFileOrFolder(newFileOrFolder);
		} else if ((currentWorkingDir !== undefined && currentWorkingDir !== null) && newFileOrFolder.parent && currentWorkingDir.id === newFileOrFolder.parent.id) {
			_Flows.appendFileOrFolder(newFileOrFolder);
		}
	},
	registerParentLink: function(d, triggerEl) {

		// Change working dir by click on folder icon
		triggerEl.on('click', function(e) {

			e.preventDefault();
			e.stopPropagation();

			if (d.parentId) {

				flowsTree.jstree('open_node', $('#' + d.parentId), function() {
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
	}
};
