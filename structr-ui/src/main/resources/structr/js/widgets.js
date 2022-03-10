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
let _Widgets = {
	defaultWidgetServerUrl: 'https://widgets.structr.org/structr/rest/widgets',
	widgetServerKey: 'structrWidgetServerKey_' + location.port,
	applicationConfigurationDataNodeKey: 'remote_widget_server',

	remoteWidgetData: [],
	remoteWidgetFilterEl: undefined,
	remoteWidgetsEl: undefined,
	localWidgetsEl: undefined,
	widgetServerSelector: undefined,

	localWidgetsCollapsedKey: 'structrWidgetLocalCollapsedKey_' + location.port,
	remoteWidgetsCollapsedKey: 'structrWidgetRemoteCollapsedKey_' + location.port,

	getContextMenuElements: function (div, entity) {

		let elements = [];

		elements.push({
			icon: _Icons.getSvgIcon('pencil_edit'),
			name: 'Edit',
			clickHandler: function () {

				Command.get(entity.id, 'id,type,name,source,configuration,description', function(entity) {
					_Widgets.editWidget(entity, true);
				});
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

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
			name: 'Delete Widget',
			clickHandler: () => {

				_Entities.deleteNode(this, entity);
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		return elements;
	},

	reloadWidgets: function() {

		_Pages.widgetsSlideout.find(':not(.slideout-activator)').remove();

		let templateConfig = {
			localCollapsed: LSWrapper.getItem(_Widgets.localWidgetsCollapsedKey, false),
			remoteCollapsed: LSWrapper.getItem(_Widgets.remoteWidgetsCollapsedKey, false)
		};

		Structr.fetchHtmlTemplate('widgets/slideout', templateConfig, function(html) {

			_Pages.widgetsSlideout.append(html);

			_Pages.widgetsSlideout[0].querySelectorAll('a.tab-group-toggle').forEach(function(toggleLink) {

				toggleLink.addEventListener('click', function(event) {
					let tabGroup = event.target.closest('.tab-group');
					tabGroup.classList.toggle('collapsed');
					LSWrapper.setItem(tabGroup.dataset.key, tabGroup.classList.contains('collapsed'));
				});
			});

			_Widgets.localWidgetsEl = $('#widgets', _Pages.widgetsSlideout);

			$('.add_widgets_icon', _Pages.widgetsSlideout).on('click', function(e) {
				e.preventDefault();
				Command.create({type: 'Widget'});
			});

			_Widgets.localWidgetsEl.droppable({
				drop: function(e, ui) {
					e.preventDefault();
					e.stopPropagation();
					_Elements.dropBlocked = true;
					var sourceId = Structr.getId($(ui.draggable));
					var sourceWidget = StructrModel.obj(sourceId);

					if (sourceWidget && sourceWidget.isWidget) {
						if (sourceWidget.treePath) {
							Command.create({ type: 'Widget', name: sourceWidget.name + ' (copied)', source: sourceWidget.source, description: sourceWidget.description, configuration: sourceWidget.configuration }, function(entity) {
								_Elements.dropBlocked = false;
							});
						}
					} else if (sourceId) {
						$.ajax({
							url: Structr.viewRootUrl + sourceId + '?edit=1',
							contentType: 'text/html',
							statusCode: {
								200: function(data) {
									Command.createLocalWidget(sourceId, 'New Widget (' + sourceId + ')', data, function(entity) {
										_Elements.dropBlocked = false;
									});
								}
							}
						});
					}
				}
			});

			_Pager.initPager('local-widgets', 'Widget', 1, 1000, 'treePath', 'asc');
			var _wPager = _Pager.addPager('local-widgets', _Widgets.localWidgetsEl, true, 'Widget', 'public', function(entities) {
				entities.forEach(function (entity) {
					StructrModel.create(entity, null, false);
					_Widgets.appendWidgetElement(entity, false, _Widgets.localWidgetsEl);
				});
			});

			_wPager.pager.append('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" /></span>');
			_wPager.activateFilterElements();

			_Widgets.remoteWidgetsEl = $('#remoteWidgets', _Pages.widgetsSlideout);

			_Widgets.remoteWidgetFilterEl = $('#remoteWidgetsFilter');
			_Widgets.remoteWidgetFilterEl.keyup(function (e) {
				if (e.keyCode === 27) {
					$(this).val('');
				}

				_Widgets.repaintRemoteWidgets();
			});

			document.querySelector('.edit-widget-servers').addEventListener('click', _Widgets.showWidgetServersDialog);

			_Widgets.updateWidgetServerSelector(function() {
				_Widgets.refreshRemoteWidgets();
			});
		});
	},
	getWidgetServerUrl: function() {

		if (_Widgets.widgetServerSelector) {
			return _Widgets.widgetServerSelector.value;
		}
	},
	getConfiguredWidgetServers: (callback) => {

		Command.getApplicationConfigurationDataNodes(_Widgets.applicationConfigurationDataNodeKey, null, (appConfigDataNodes) => {

			appConfigDataNodes.push({id: '', name: 'default', content: _Widgets.defaultWidgetServerUrl, editable: false});

			callback(appConfigDataNodes);
		});
	},
	showWidgetServersDialog: function() {

		Structr.fetchHtmlTemplate('widgets/servers-dialog', {}, function(html) {

			Structr.dialog('Widget Servers');
			dialogText.html(html);

			Structr.activateCommentsInElement(dialogText, {helpElementCss: { 'font-size': '13px'}});

			_Widgets.updateWidgetServersTable();

			dialogText[0].querySelector('button#save-widget-server').addEventListener('click', function () {
				let name = document.querySelector("#new-widget-server-name").value;
				let url = document.querySelector("#new-widget-server-url").value;

				Command.createApplicationConfigurationDataNode(_Widgets.applicationConfigurationDataNodeKey, name, url, function(e) {
					_Widgets.updateWidgetServersTable();
					_Widgets.updateWidgetServerSelector();
				});
			});
		});

	},
	updateWidgetServersTable: function() {

		_Widgets.getConfiguredWidgetServers((appConfigDataNodes) => {

			Structr.fetchHtmlTemplate('widgets/servers-table', { servers: appConfigDataNodes }, (html) => {

				let container = dialogText[0].querySelector('#widget-servers-container');

				container.innerHTML = html;

				for (let deleteIcon of container.querySelectorAll('.delete')) {

					deleteIcon.addEventListener('click', function(e) {

						let el     = e.target;
						let acdnID = el.closest('div').dataset.acdnId;

						Structr.confirmation('Really delete Widget Server URL?', () => {

							Command.deleteNode(acdnID, false, function() {

								let currentServer = LSWrapper.getItem(_Widgets.widgetServerKey);
								let needsRefresh = (_Widgets.widgetServerSelector.value === currentServer);
								if (needsRefresh) {
									LSWrapper.removeItem(_Widgets.widgetServerKey);
								}

								_Widgets.updateWidgetServerSelector(() => {
									if (needsRefresh) {
										_Widgets.refreshRemoteWidgets();
									}
								});

								$.unblockUI({
									fadeOut: 25
								});

								_Widgets.showWidgetServersDialog();
							});
						}, () => {
							_Widgets.showWidgetServersDialog();
						});
					});
				}

				for (let input of container.querySelectorAll('input')) {

					input.addEventListener('change', function(e) {
						let el     = e.target;
						let acdnID = el.closest('div').dataset.acdnId;
						let key    = el.dataset.key;

						Command.setProperty(acdnID, key, el.value, false, function(e) {

							blinkGreen($(el));

							_Widgets.updateWidgetServerSelector();
						});
					});
				}
			});
		});
	},
	updateWidgetServerSelector: function(callback) {

		_Widgets.getConfiguredWidgetServers((appConfigDataNodes) => {

			let templateConfig = {
				servers: appConfigDataNodes,
				selectedServerURL: LSWrapper.getItem(_Widgets.widgetServerKey, _Widgets.defaultWidgetServerUrl)
			};

			Structr.fetchHtmlTemplate('widgets/servers-selector', templateConfig, (html) => {

				let newElement = Structr.createSingleDOMElementFromHTML(html);

				if (_Widgets.widgetServerSelector) {

					_Widgets.widgetServerSelector.replaceWith(newElement);

				} else {

					let selectorContainer = document.querySelector('#widget-server-selector-container');
					selectorContainer.prepend(newElement);
				}

				_Widgets.widgetServerSelector = document.querySelector('#widget-server-selector');
				_Widgets.widgetServerSelector.addEventListener('change', _Widgets.refreshRemoteWidgets);

				if (typeof callback === 'function') {
					callback();
				}
			});
		});
	},
	refreshRemoteWidgets: function() {

		let url = _Widgets.getWidgetServerUrl();

		LSWrapper.setItem(_Widgets.widgetServerKey, url);

		if (!url.startsWith(document.location.origin)) {

			_Widgets.remoteWidgetsEl.empty();
			_Widgets.remoteWidgetData = [];

			_Widgets.fetchRemoteWidgets(url + '?sort=treePath', url + '?_sort=treePath').then(function(data) {

				data.forEach(function(entity) {
					var obj = StructrModel.create(entity, null, false);
					obj.srcUrl = url + '/' + entity.id;
					_Widgets.remoteWidgetData.push(obj);
				});

				_Widgets.repaintRemoteWidgets();

			}).catch(function(e) {
				_Widgets.remoteWidgetFilterEl.hide();
				_Widgets.remoteWidgetsEl.empty();
				_Widgets.remoteWidgetsEl.html('Could not fetch widget data from server (' + url + '). Make sure that the resource loads correctly and check CORS settings.<br>Also check your adblocker settings for possible conflicts.');
			});

		} else {
			new MessageBuilder().warning().text('Can not display local widgets as remote widgets. Please select another widget server!').show();
		}
	},
	repaintRemoteWidgets: function () {

		_Widgets.remoteWidgetFilterEl.show();
		let search = _Widgets.remoteWidgetFilterEl.val();
		_Widgets.remoteWidgetsEl.empty();

		if (search && search.length > 0) {

			search = search.toLowerCase();

			_Widgets.remoteWidgetData.forEach(function (obj) {
				if (obj.name.toLowerCase().indexOf(search) !== -1) {
					_Widgets.appendWidgetElement(obj, true, _Widgets.remoteWidgetsEl);
				}
			});

		} else {

			_Widgets.remoteWidgetData.forEach(function (obj) {
				_Widgets.appendWidgetElement(obj, true, _Widgets.remoteWidgetsEl);
			});
		}

		_Pages.resize();
	},
	getTreeParent: function(element, treePath, suffix) {

		var parent = element;

		if (treePath) {

			var parts = treePath.split('/');
			var num = parts.length;
			var i = 0;

			for (i = 0; i < num; i++) {

				var part = parts[i];
				if (part) {

					var lowerPart = part.toLowerCase().replace(/ /g, '');
					var idString = lowerPart + suffix;
					var newParent = $('#' + idString);

					if (newParent.length === 0) {
						_Widgets.appendFolderElement(parent, idString, _Icons.folder_icon, part);
						newParent = $('#' + idString);
					}

					parent = newParent;
				}
			}

		} else {

			var idString = 'other' + suffix;
			var newParent = $('#' + idString);

			if (newParent.length === 0) {
				_Widgets.appendFolderElement(parent, idString, _Icons.folder_icon, 'Uncategorized');
				newParent = $('#' + idString);
			}

			parent = newParent;
		}

		return parent;
	},
	appendFolderElement: function(parent, id, icon, name) {

		let expanded = Structr.isExpanded(id);

		parent.append(`
			<div id="${id}_folder" class="widget node">
				<i class="typeIcon ${_Icons.getFullSpriteClass(icon)}"></i>
				<b title="${escapeForHtmlAttributes(name)}" class="name abbr-ellipsis abbr-70pc">${name}</b>
				<div id="${id}" class="node${expanded ? ' hidden' : ''}"></div>
			</div>
			`);

		let div = $('#' + id + '_folder');

		_Widgets.appendVisualExpandIcon(div, id, name, true, false);
	},
	appendWidgetElement: function(widget, remote, el) {

		let icon   = _Icons.widget_icon;
		let parent = _Widgets.getTreeParent(el ? el : (remote ? _Widgets.remoteWidgetsEl : _Widgets.localWidgetsEl), widget.treePath, remote ? '_remote' : '_local');
		let div    = Structr.node(widget.id);

		if (!div) {

			parent.append(`
				<div id="id_${widget.id}" class="node widget">
					<i class="typeIcon ${_Icons.getFullSpriteClass(icon)}"></i>
					<b title="${escapeForHtmlAttributes(widget.name)}" class="name_ abbr-ellipsis abbr-70pc">${widget.name}</b> <span class="id">${widget.id}</span>
					<div class="icons-container"></div>
				</div>
			`);
			div = Structr.node(widget.id);
		}

		let iconsContainer = div.children('.icons-container');

		div.draggable({
			iframeFix: true,
			revert: 'invalid',
			containment: 'body',
			helper: 'clone',
			appendTo: '#main',
			stack: '.node',
			zIndex: 99
		});

		_Entities.setMouseOver(div, false);

		if (remote) {

			div.children('b.name_').off('click').css({cursor: 'move'});

			let eyeIcon = $(_Icons.getSvgIcon('eye_open', 16, 16, ['svg_eye_icon', 'icon-grey', 'cursor-pointer', 'node-action-icon']));
			iconsContainer.append(eyeIcon);

			eyeIcon.on('click', function() {
				_Widgets.editWidget(widget, false);
			});

		} else {

			_Entities.appendContextMenuIcon(iconsContainer, widget);
			_Elements.enableContextMenuOnElement(div, widget);
		}

		return div;
	},
	editWidget: function(entity, allowEdit) {

		Structr.dialog((allowEdit ? 'Edit widget "' : 'Source code of "') + entity.name + '"', () => {}, () => {}, ['popup-dialog-with-editor']);

		let id = "widget-dialog";
		dialogHead.append(`
			<div id="${id}_head">
				<div id="tabs">
					<ul id="widget-dialog-tabs">
						<li data-name="source">Source</li>
						<li data-name="config">Configuration</li>
						<li data-name="description">Description</li>
						<li data-name="selectors">Options</li>
						<li data-name="help">Help</li>
					</ul>
				</div>
			</div>
		`);
		dialogText.append(`<div id="${id}_content"></div>`);

		let mainTabs   = $('#tabs', dialogHead);
		let contentDiv = $('#' + id + '_content', dialogText);

		let ul = mainTabs.children('ul');

		let activateTab = function (tabName) {
			$('.widget-tab-content', contentDiv).hide();
			$('li', ul).removeClass('active');
			$('#tabView-' + tabName, contentDiv).show();
			$('li[data-name="' + tabName + '"]', ul).addClass('active');
			Structr.resize();

			_Editors.resizeVisibleEditors();
		};

		$('#widget-dialog-tabs > li', mainTabs).on('click', function(e) {
			activateTab($(this).data('name'));
		});

		contentDiv.append(`
			<div class="tab widget-tab-content h-full" id="tabView-source"><div class="editor h-full"></div></div>
			<div class="tab widget-tab-content h-full" id="tabView-config"><div class="editor h-full"></div></div>
			<div class="tab widget-tab-content h-full" id="tabView-description"><div class="editor h-full"></div></div>
			<div class="tab widget-tab-content" id="tabView-selectors"></div>
			<div class="tab widget-tab-content" id="tabView-help"></div>
		`);

		let changes = {};
		let widgetChanged = () => {
			let changed = false;
			for (let propertyName in changes) {
				changed = changed || changes[propertyName];
			}
			return changed;
		};

		let updateButtonStatus = () => {
			if (widgetChanged()) {
				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			} else {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			}
		};

		let editorChangeHandler = (editor, origEntity, propertyName) => {

			changes[propertyName] = ((entity[propertyName] || '') !== editor.getValue());

			if (allowEdit) {
				updateButtonStatus();
			}
		};

		let baseEditorConfig = {
			readOnly: !allowEdit,
			changeFn: editorChangeHandler
		};

		let sourceEditor      = _Editors.getMonacoEditor(entity, 'source',        $('#tabView-source .editor', contentDiv),      Object.assign({}, baseEditorConfig, { language: 'text/html', forceAllowAutoComplete: true }));
		let configEditor      = _Editors.getMonacoEditor(entity, 'configuration', $('#tabView-config .editor', contentDiv),      Object.assign({}, baseEditorConfig, { language: 'application/json' }));
		let descriptionEditor = _Editors.getMonacoEditor(entity, 'description',   $('#tabView-description .editor', contentDiv), Object.assign({}, baseEditorConfig, { language: 'text/html' }));

		// allow editing of selectors property
		_Schema.getTypeInfo(entity.type, (typeInfo) => {
			_Entities.listProperties(entity, 'editWidget', $('#tabView-selectors'), typeInfo);
		});

		_Widgets.appendWidgetHelpText($('#tabView-help', contentDiv));

		if (allowEdit) {

			dialogBtn.append(`
				<button id="editorSave" disabled="disabled" class="disabled">Save Widget</button>
				<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>
			`);

			dialogSaveButton = $('#editorSave', dialogBtn);
			saveAndClose     = $('#saveAndClose', dialogBtn);

			let saveWidgetFunction = (closeAfterSave) => {

				let widgetData = {
					source:        sourceEditor.getValue(),
					configuration: configEditor.getValue(),
					description:   descriptionEditor.getValue()
				};

				try {

					if (widgetData.configuration) {
						JSON.parse(widgetData.configuration);
					}

					Command.setProperties(entity.id, widgetData, () => {

						Structr.showAndHideInfoBoxMessage('Widget saved.', 'success', 2000, 200);

						if (closeAfterSave) {
							dialogCancelButton.click();
						} else {
							let modelObj = StructrModel.obj(entity.id);
							modelObj.source        = widgetData.source;
							modelObj.configuration = widgetData.configuration;
							modelObj.description   = widgetData.description;
							entity.source          = widgetData.source;
							entity.configuration   = widgetData.configuration;
							entity.description     = widgetData.description;

							changes = {};

							updateButtonStatus();
						}
					});

				} catch (e) {
					activateTab('config');
					alert('Configuration is not valid JSON - please review, otherwise the widget configuration dialog will not function correctly');
				}
			};

			saveAndClose.on('click', function() {
				saveWidgetFunction(true);
			});

			dialogSaveButton.on('click', function() {
				saveWidgetFunction(false);
			});
		}

		activateTab('source');
	},
	appendWidgetSelectorEditor: function (container, entity, allowEdit) {

		Structr.fetchHtmlTemplate('widgets/edit-selectors', {}, function(html) {
			container.append(html);
		});
	},
	appendWidgetHelpText: function(container) {

		Structr.fetchHtmlTemplate('widgets/help', {}, function(html) {
			container.append(html);
		});
	},
	appendVisualExpandIcon: function(el, id, name, hasChildren, expand) {

		if (hasChildren) {

			let typeIcon            = $(el.children('.typeIcon').first());
			let icon                = $(el).children('.node').hasClass('hidden') ? _Icons.collapsedClass : _Icons.expandedClass;
			let expandIconClassName = 'expand_icon_svg';

			typeIcon.css({
				paddingRight: 0 + 'px'
			}).after(`<i title="Expand ${name}" class="${expandIconClassName} ${icon}"></i>`);

			let expandIcon = el.children('.' + expandIconClassName).first();

			let expandClickHandler = function (e) {
				e.stopPropagation();
				let body = $('#' + id);
				body.toggleClass('hidden');

				let collapsed = body.hasClass('hidden');
				if (collapsed) {
					Structr.addExpandedNode(id);
					expandIcon.removeClass(_Icons.expandedClass).addClass(_Icons.collapsedClass);
				} else {
					Structr.removeExpandedNode(id);
					expandIcon.removeClass(_Icons.collapsedClass).addClass(_Icons.expandedClass);
				}
			};

			$(el).on('click', expandClickHandler);

			let button = $(el.children('.' + expandIconClassName).first());

			if (button) {

				button.on('click', expandClickHandler);

				button.on('mousedown', function(e) {
					e.stopPropagation();
				});
			}

		} else {

			el.children('.typeIcon').css({
				paddingRight: '11px'
			});
		}
	},
	insertWidgetIntoPage: function(widget, target, pageId, callback) {

		let url               = _Widgets.getWidgetServerUrl();
		let widgetSource      = widget.source;
		let widgetDescription = widget.description;
		let widgetConfig      = widget.configuration;

		if (widgetConfig) {
			try {
				widgetConfig = JSON.parse(widgetConfig);
			} catch (e) {
				new MessageBuilder().error("Cannot parse Widget configuration").show();
				return;
			}
		}

		if (widgetSource) {

			if ((widgetDescription !== null && widgetDescription !== "") || widgetConfig ) {

				Structr.dialog('Configure Widget', function() {}, function() {});

				if ((widgetDescription === null || widgetDescription === "")) {
					dialogText.append('<p>Fill out the following parameters to correctly configure the widget.</p>');
				} else {
					dialogText.append(widgetDescription);
				}

				dialogText.append('<table class="props widget-props"></table>');

				let table = $('table', dialogText);

				let getOptionsAsText = (options, defaultValue) => {

					let buffer = '';

					if (Object.prototype.toString.call(options) === '[object Array]') {
						for (let option of options) {
							buffer += `<option ${((option === defaultValue) ? 'selected' : '')}>${option}</option>`;
						}

					} else if (Object.prototype.toString.call(options) === '[object Object]') {

						for (let option in options) {
							buffer += `<option ${((option === defaultValue) ? 'selected' : '')} value="${option}">${options[option]}</option>`;
						}
					}

					return buffer;
				};

				let sortedWidgetConfig = _Widgets.sortWidgetConfigurationByPosition(widgetConfig);

				for (let configElement of sortedWidgetConfig) {

					let label = configElement[0];
					if (label === 'processDeploymentInfo') {
						return;
					}

					let cleanedLabel = label.replace(/[^\w]/g, '_');

					let fieldConfig  = configElement[1];
					let fieldType    = fieldConfig.type;
					let defaultValue = fieldConfig.default || '';
					let titleLabel   = fieldConfig.title || label;
					let placeholder  = fieldConfig.placeholder || titleLabel;

					switch (fieldType) {
						case "select":
							let options = fieldConfig.options || ["-"];

							let buffer = `<tr><td><span id="label-${cleanedLabel}">${titleLabel}</span></td><td><select id="${cleanedLabel}" class="form-field" data-key="${label}">`;
							let delayedAppendFunction;

							if (fieldConfig.dynamicOptionsFunction) {

								let dynamicOptionsFunction = new Function("callback", fieldConfig.dynamicOptionsFunction);

								let delayedAppendOptions = function (options) {
									delayedAppendFunction = new function() {
										$('select#' + cleanedLabel).append(getOptionsAsText(options, defaultValue));
									};
								};

								dynamicOptionsFunction(delayedAppendOptions);

							} else {

								buffer += getOptionsAsText(options, defaultValue);
							}

							buffer += '</select></td></tr>';

							table.append(buffer);
							if (delayedAppendFunction) {
								delayedAppendFunction();
							}
							break;

						case "textarea":
							let rows = (fieldConfig.rows ? parseInt(fieldConfig.rows) || 5 : 5);
							table.append(`<tr><td><span id="label-${cleanedLabel}">${titleLabel}</span></td><td><textarea rows=${rows} class="form-field" id="${label}" placeholder="${placeholder}" data-key="${label}">${defaultValue}</textarea></td></tr>`);
							break;

						case "input":
						default:
							table.append(`<tr><td><span id="label-${cleanedLabel}">${titleLabel}</span></td><td><input class="form-field" type="text" id="${label}" placeholder="${placeholder}" data-key="${label}" value="${defaultValue}"></td></tr>`);
					}

					if (fieldConfig.help) {
						Structr.appendInfoTextToElement({
							text: fieldConfig.help,
							element: $('#label-' + cleanedLabel)
						});
					}
				}

				dialog.append('<button id="appendWidget">Append Widget</button>');

				$('#appendWidget').on('click', function(e) {

					let attrs = {};

					for (let field of table[0].querySelectorAll('.form-field')) {
						let key = field.dataset['key'];
						if (widgetConfig[key]) {
							attrs[key] = field.value;
						}
					}

					e.stopPropagation();
					Command.appendWidget(widgetSource, target.id, pageId, url, attrs, widgetConfig.processDeploymentInfo, callback);

					dialogCancelButton.click();
					return false;
				});

			} else {

				Command.appendWidget(widgetSource, target.id, pageId, url, {}, (widgetConfig ? widgetConfig.processDeploymentInfo : false), callback);
			}
		} else {
			new MessageBuilder().warning("Ignoring empty Widget").show();
		}
	},
	sortWidgetConfigurationByPosition: function (config) {
		let flattenedConfig = [];

		for (let key in config) {
			let val = config[key];
			flattenedConfig.push([val.position, key, val]);
		}

		let sortedConfig = flattenedConfig.sort(function (a, b) {
			return (a[0] - b[0]);
		});

		return sortedConfig.map(function(el) {
			return [el[1], el[2]];
		});
	},
	fetchRemotePageTemplateWidgets: async function() {

		let url = _Widgets.getWidgetServerUrl() || _Widgets.defaultWidgetServerUrl;

		LSWrapper.setItem(_Widgets.widgetServerKey, url);

		if (!url.startsWith(document.location.origin)) {

			let widgets = await _Widgets.fetchRemoteWidgets(url + '?isPageTemplate=true&sort=name', url + '?isPageTemplate=true&_sort=name');
			return widgets;
		}

		return [];
	},
	fetchLocalPageTemplateWidgets: async function() {

		try {
			let response = await fetch(Structr.rootUrl + 'Widget?isPageTemplate=true&' + Structr.getRequestParameterName('sort') + '=name');
			if (response && response.ok) {

				let json = await response.json();
				return json.result;
			}

		} catch (e) {}

		return [];
	},
	fetchAllPageTemplateWidgets: async function(callback) {

		let widgets = [];

		let remotePageWidgets = await _Widgets.fetchRemotePageTemplateWidgets();
		let localPageWidgets  = await _Widgets.fetchLocalPageTemplateWidgets();

		callback(widgets.concat(remotePageWidgets).concat(localPageWidgets));
	},
	fetchRemoteWidgets: async (url, fallbackUrl) => {

		try {
			// stick with legacy sort parameter for widget instance - if a newer widget instance is used, retry with _sort
			let response = await fetch(url);

			if (response && response.ok) {

				let json = await response.json();
				return json.result;

			} else {

				let response = await fetch(fallbackUrl);
				if (response && response.ok) {

					let json = await response.json();
					return json.result;
				}
			}

		} catch (e) {}

		return [];
	}
};
