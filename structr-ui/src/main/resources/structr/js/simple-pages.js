/*
 * Copyright (C) 2010-2020 Structr GmbH
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
document.addEventListener('DOMContentLoaded', function() {
	Structr.registerModule(_SimplePages);
	Structr.classes.push('');
});
var depth = 0;
var _SimplePages = {

	_moduleName: 'simple-pages',
	resizerLeftKey: 'structrSimplePagesResizerLeftKey_' + port,
	currentPageIdKey: 'structrSimplePagesCurrentPageIdKey_' + port,

	components: {},
	objectCache: {},
	jsPlumbInstance: {},
	allConnections: [],

	onload: function() {

		Structr.fetchHtmlTemplate('simple-pages/main', {}, (html) => {

			main.append(html);

			_SimplePages.components.main       = document.querySelector('#simple-pages-main');
			_SimplePages.components.ui         = document.querySelector('#simple-pages-main #ui-container');
			_SimplePages.components.logic      = document.querySelector('#simple-pages-main #logic-container');
			_SimplePages.components.data       = document.querySelector('#simple-pages-main #data-container');
			_SimplePages.components.interfaces = document.querySelector('#simple-pages-main #interface-container');

			Structr.unblockMenu(500);

			_SimplePages.init();

			_SimplePages.jsPlumbInstance = jsPlumb.getInstance({
				//Connector: "StateMachine",
				PaintStyle: {
					lineWidth: 4,
					strokeStyle: '#bfc5d0',
					dashstyle: '3 1'
				},
				Endpoint: ['Dot', {radius: 6}],
				EndpointStyle: {
					fillStyle: '#aaa'
				},
				//Container: "simple-pages-main",
				ConnectionOverlays: [['PlainArrow', {
						location: 1,
						width: 15,
						length: 12
					}]
				]
			});

			_SimplePages.jsPlumbInstance.bind('connection', function(info, originalEvent) {
				if (!originalEvent) return;
				let sourceId = info.source.dataset['structrId'] || info.sourceId.substring(3);
				let targetId = info.targetId.substring(3);

				console.log(info, sourceId, targetId); // return;

				let targetIsFlow = info.target.classList.contains('simple-pages-flow');
				let relType = 'ATTACHED';
				if (targetIsFlow) {
					relType = 'CALLS';
				}
				console.log('creating new relationship:', sourceId, targetId, relType);
				Command.createRelationship({ sourceId: sourceId, targetId: targetId, relType: relType}, (rel) => {
					console.log('new connection created:', rel);
				});
			});



		});

	},
	init: function() {
		_SimplePages.refreshPages();
		_SimplePages.refreshFlowContainers();
		_SimplePages.refreshEventHandlers();

		addEvent(document, 'click', (e) => {
			if (
				(e.target.closest('.menu-icon') && e.target.closest('.menu-icon').classList.contains('open-context-menu'))
				|| e.target.classList.contains('open-context-menu')
				|| e.target.closest('.page-context-menu')
				|| e.target.closest('.event-handler-context-menu')
			) return false;
			_SimplePages.closeAllContextMenus();
			_SimplePages.unselectAllElementsAndCloseAllOverlays();
		});

		live('.delete-event-handler-trigger', 'click', (e) => {
			let eventHandlerId = e.target.closest('.simple-pages-event-handler').querySelector('.id').innerText;
			Command.get(eventHandlerId, null, function(eventHandler) {
				_Entities.deleteNode(e.target, eventHandler);
			});
		});

		live('.delete-page-trigger', 'click', (e) => {
			let pageId = e.target.closest('.simple-pages-page').querySelector('.id').innerText;
			Command.get(pageId, null, function(page) {
				_Entities.deleteNode(e.target, page);
			});
		});

		live('.open-page-context-menu', 'click', (e) => {
			e.preventDefault();
			e.stopPropagation();
			let pagePane = e.target.closest('.simple-pages-page');

			if (pagePane.querySelector('.page-context-menu')) {

				_SimplePages.closeAllContextMenus();
				return;
			}

			let pageId = e.target.closest('.simple-pages-page').querySelector('.id').innerText;

			Command.get(pageId, null, function(page) {
				Structr.fetchHtmlTemplate('simple-pages/page-context-menu', { page: page }, function(html) {

					let menuContainer = pagePane.querySelector('.menu-container');

					menuContainer.insertAdjacentHTML('beforeEnd', html);
					menuContainer.querySelectorAll('.menu-entry').forEach(menuEntry => {

						if (menuEntry.classList.contains('add-page-area-trigger')) {

							Structr.getShadowPage(() => {

								Command.query('PageArea', 1000, 1, 'name', 'desc', { hidden: false, pageId: shadowPage.id }, function(pageAreas) {
									pageAreas.forEach(function(pageArea) {
										Structr.fetchHtmlTemplate('simple-pages/page-area-context-menu-entry', { pageArea: pageArea }, function(html) {
											menuEntry.querySelector('.submenu').insertAdjacentHTML('beforeEnd', html);
										});
									});
								});
							});
						}

						addEvent(menuEntry, 'click', (e) => {

							let t = e.target;
							if (!t.classList.contains('delete-page-trigger')) {
								e.stopPropagation();
							}

							if (t.classList.contains('submenu-entry')) {

								if (t.classList.contains('page-area-context-menu-entry')) {
									let pageId = t.closest('.simple-pages-page').querySelector('.id').innerText;
									let pageAreaId = t.dataset['structrId'];
									//console.log('Add page area', pageId, pageAreaId);
									Command.cloneComponent(pageAreaId, pageId, function() {
										_SimplePages.refreshPageList();
									});
									return;
								}
								return;
							}

							let menuEntry = t.closest('.menu-entry');
							let subMenu      = menuEntry.querySelector('.submenu');
							let expandIcon   = menuEntry.querySelector('.menu-expand-icon');
							let collapseIcon = menuEntry.querySelector('.menu-collapse-icon');
							if (subMenu) {
								subMenu.classList.toggle('hidden');
								expandIcon.classList.toggle('hidden');
								collapseIcon.classList.toggle('hidden');
							}
						});
					});
				});
			});
		});

		live('.open-event-handler-context-menu', 'click', (e) => {
			e.preventDefault();
			e.stopPropagation();
			let eventHandlerPane = e.target.closest('.simple-pages-event-handler');

			if (eventHandlerPane.querySelector('.event-handler-context-menu')) {
				_SimplePages.closeAllContextMenus();
				return;
			}

			let eventHandlerId = e.target.closest('.simple-pages-event-handler').querySelector('.id').innerText;

			Command.get(eventHandlerId, null, function(eventHandler) {
				Structr.fetchHtmlTemplate('simple-pages/event-handler-context-menu', { eventHandler: eventHandler }, function(html) {
					// console.log(eventHandlerPane);
					let menuContainer = eventHandlerPane.querySelector('.menu-container');
					menuContainer.insertAdjacentHTML('beforeEnd', html);
				});
			});
		});

		live('.main-pane .dom-container', 'click', (e) => {
			e.preventDefault();
			e.stopPropagation();
			let clickedEl = e.path[0];
			//console.log(clickedEl);

			e.target.shadowRoot.querySelectorAll('[data-structr-id].selected').forEach((el) => {
				if (clickedEl != el) {
					el.classList.remove('selected');
				}
			});
			let structrId = clickedEl.dataset['structrId'];
			clickedEl.classList.toggle('selected');

			let overlay = clickedEl.querySelector('.element-overlay');
			// console.log(overlay);
			if (!overlay) {
				Structr.fetchHtmlTemplate('simple-pages/element-overlay', {element: clickedEl}, function (html) {
					clickedEl.insertAdjacentHTML('afterbegin', html);

					_SimplePages.jsPlumbInstance.addEndpoint(clickedEl, {
						anchor: 'Right',
						maxConnections: -1,
						isSource: true,
						deleteEndpointsOnDetach: false
					});

				});
			}
		});

		_SimplePages.displayPageElementConnections = (shadowRoot) => {

			//e.preventDefault();
			//e.stopPropagation();
			//let clickedEl = e.path[0];
			//e.target.shadowRoot.querySelectorAll('[data-structr-id].selected').forEach((el) => {
			//shadowRoot.querySelectorAll('[data-structr-id].selected').forEach((el) => {
			shadowRoot.querySelectorAll('[data-structr-id]').forEach((element) => {

				let structrId = element.dataset['structrId'];
				if (!structrId) return;

				Command.get(structrId, null, selectedEl => {
					//console.log(selectedEl);
					let targetHandlers = selectedEl.targetHandlers;
					let sourceHandlers = selectedEl.sourceHandlers;

					// console.log(sourceHandlers, targetHandlers);

					targetHandlers.forEach((targetHandler) => {
						let el = document.querySelector('#id_' + targetHandler.id);
						// el.classList.add('highlight');

						_SimplePages.allConnections.push(_SimplePages.jsPlumbInstance.connect({
							detachable: false,
							source: element,
							target: el,
							anchors: ['Right', 'Left'],
							paintStyle: {lineWidth: 4, strokeStyle: "#bfc5d0", dashstyle: '3 1'},
							endpoint: ["Dot", {radius: 6}],
							endpointStyle: {
								fillStyle: 'transparent'
							},
							// label: "test",
							overlays: [
								["PlainArrow", {
									location: 1,
									width: 15,
									length: 12
								}],
								["Label", {
									cssClass: "label element-to-event-handler",
									label: '<svg class="open-context-menu open-element-to-event-handler-context-menu menu-icon" fill="currentColor" xmlns="http://www.w3.org/2000/svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:svgjs="http://svgjs.com/svgjs" viewBox="0 0 24 24" ><g transform="matrix(0.8333333333333334,0,0,0.8333333333333334,0,0)"><path d="M8.750 3.250 A3.250 3.250 0 1 0 15.250 3.250 A3.250 3.250 0 1 0 8.750 3.250 Z" stroke="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="0"></path><path d="M8.750 12.000 A3.250 3.250 0 1 0 15.250 12.000 A3.250 3.250 0 1 0 8.750 12.000 Z" stroke="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="0"></path><path d="M8.750 20.750 A3.250 3.250 0 1 0 15.250 20.750 A3.250 3.250 0 1 0 8.750 20.750 Z" stroke="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="0"></path></g></svg>',
									location: .70,
									id: 'label'
								}
								],
							]
						}));
					});

					sourceHandlers.forEach((sourceHandler) => {
						let el = document.querySelector('#id_' + sourceHandler.id);

						_SimplePages.allConnections.push(_SimplePages.jsPlumbInstance.connect({
							detachable: false,
							source: el,
							target: element,
							anchors: ['Right', 'Left'],
							paintStyle: {lineWidth: 4, strokeStyle: "#bfc5d0", dashstyle: '3 1'},
							endpoint: ["Dot", {radius: 6}],
							endpointStyle: {
								fillStyle: 'transparent'
							},
							overlays: [
								["PlainArrow", {
									location: 1,
									width: 15,
									length: 12
								}
								]
							]
						}));

					});



				}, 'custom');
			});
		}; // displayPageElementConnections

		_SimplePages.displayFlowContainerConnections = (flowContainer) => {

			if (flowContainer.eventHandler) {
				let eventHandlerEl = document.querySelector('#id_' + flowContainer.eventHandler.id);
				let flowContainerEl = document.querySelector('#id_' + flowContainer.id);

				_SimplePages.jsPlumbInstance.connect({
					detachable: false,
					source: eventHandlerEl,
					target: flowContainerEl,
					anchors: ['Bottom', 'Top'],
					paintStyle: {lineWidth: 4, strokeStyle: "#bfc5d0", dashstyle: '3 1'},
					endpoint: ["Dot", {radius: 6}],
					endpointStyle: {
						fillStyle: 'transparent'
					},
					overlays: [
						["PlainArrow", {
							location: 1,
							width: 15,
							length: 12
						}
						]
					]
				});
			}


		};
	},
	detachAllConnections: () => {
		_SimplePages.allConnections.forEach(connection => {
			_SimplePages.jsPlumbInstance.detach(connection);
		});
		_SimplePages.allConnections = [];

		document.querySelectorAll('.simple-pages-page .dom-container').forEach(el => {
			if (el.shadowRoot) {
				_SimplePages.displayPageElementConnections(el.shadowRoot);
			}
		});
	},
	closeAllContextMenus: () => {
		document.querySelectorAll('.simple-pages-context-menu').forEach(el => {
			el.parentNode.removeChild(el);
		});
	},
	unselectAllElementsAndCloseAllOverlays: () => {
		document.querySelectorAll('.simple-pages-page .dom-container').forEach(el => {
			if (el.shadowRoot) {
				el.shadowRoot.querySelectorAll('.selected').forEach(selectedEl => {
					selectedEl.classList.remove('selected');
					let overlay = selectedEl.querySelector('.element-overlay');
					overlay.parentNode.removeChild(overlay);
				});
			}
		});
	},
	refreshPages: () => {
		fastRemoveAllChildren(_SimplePages.components.ui);
		_SimplePages.listPages();
	},
	// refreshDataSources: () => {
	// 	fastRemoveAllChildren(_SimplePages.components.data);
	// 	_SimplePages.listDataSources();
	// },
	refreshFlowContainers: () => {
		fastRemoveAllChildren(_SimplePages.components.data);
		_SimplePages.listFlowContainers();
	},
	refreshEventHandlers: () => {
		fastRemoveAllChildren(_SimplePages.components.logic);
		_SimplePages.listEventHandlers();
	},
	showAddPageTemplateButton: (callback) => {
		Structr.fetchHtmlTemplate('simple-pages/add-page-template-button', null, function(html) {
			_SimplePages.components.pageRoot.insertAdjacentHTML('beforeEnd', html);
			_SimplePages.components.addPageTemplateButton = document.querySelector('.add-page-template');
			if (callback) callback();
		});
	},
	hideAddPageTemplateButton: () => {
		let el = querySelector('.add-page-template');
		el.parentNode.removeChild(el);
	},
	listPages: () => {

		Command.query('Page', 1000, 1, 'name', 'asc', { hidden: false }, function(pages) {

			pages.forEach(function(page) {

				// TODO: Remove this filter to show all pages
				if (!page.name.startsWith('test') && !page.name.startsWith('New')) return;

				Structr.fetchHtmlTemplate('simple-pages/page-list-node', { page: page }, function(html) {

					_SimplePages.components.ui.insertAdjacentHTML('beforeEnd', html);

					live('#id_' + page.id, 'click', function() {

						LSWrapper.setItem(_SimplePages.currentPageIdKey, page.id);
						_SimplePages.components.ui.querySelectorAll('.simple-pages-page.selected').forEach(function(childNode) {
							childNode.classList.remove('selected');
						});
						this.classList.add('selected');

						// window.setTimeout(() => {
						// 	_SimplePages.jsPlumbInstance.repaintEverything();
						// }, 200);

					});

					let currentPageId = LSWrapper.getItem(_SimplePages.currentPageIdKey);
					if (currentPageId === page.id) {
						document.querySelector('#id_' + currentPageId).click();
					}

					_SimplePages.loadPage(page.id);

				});
			});

			Structr.fetchHtmlTemplate('simple-pages/create-page-button', {}, function(html) {

				_SimplePages.components.ui.insertAdjacentHTML('beforeEnd', html);
				_SimplePages.components.ui.querySelector('.create-page-button').addEventListener('click', function() {
					Command.create({type: 'Page'}, function(page) {
						LSWrapper.setItem(_SimplePages.currentPageIdKey, page.id);
						_SimplePages.refreshPages();
					});
				});
			});

		});
	},
	loadPage: (pageId) => {
		// clear object cache
		_SimplePages.objectCache = {};
		let pagePane = document.querySelector('#id_' + pageId + ' .main-pane');
		Command.get(pageId, null, page => {

			if (page.children.length === 0) {
				// page has no child element => show add icon
			} else {

				//fastRemoveAllChildren(pagePane);
				page.children.forEach(child => {
					_SimplePages.appendPageArea(child, pagePane, page, (shadowRoot) => {
						window.setTimeout(() => _SimplePages.displayPageElementConnections(shadowRoot), 250);
					});
				});
			}
		});
	},
	appendPageArea: (el, parentElement, page, callback) => {
		Command.get(el.id, null, async (element) => {
			Structr.fetchHtmlTemplate('simple-pages/page-area', { page: page, element: element }, async (html) => {
				parentElement.insertAdjacentHTML('beforeEnd', html);

				// regular DOM node the shadow DOM is attached to
				let domContainer = parentElement.querySelector('.main-pane .dom-container');
				let shadowRoot   = domContainer.attachShadow({ mode: 'open' });

				// render all children of this page area (typically the page template)
				let newHtml = await _SimplePages.replaceTemplateExpressions(element);
				//console.log('appendPageArea: newHtml', newHtml);

				// append the HTML to the shadow root element and continue
				shadowRoot.innerHTML = newHtml;

				let style = document.createElement('style');
				style.setAttribute('type', 'text/css');
				let css = '.slot:hover { outline: 2px dotted orange; outline-offset: -2px; }' +
					'[data-structr-id]:hover { outline: 2px dashed #42BEF0; outline-offset: -3px; }' +
					'[data-structr-id].selected { outline: 2px solid #84ba39; outline-offset: -2px; }' +
					'.element-overlay { border: 1px solid #d2d6e0; background-color: rgba(94,108,136, .06); margin-top: -1rem; color: #5e6c88; font-size: .75rem; }' +
					'.element-overlay .menu-icon { cursor: pointer; height: 1rem; width: 1rem; float: right; margin: .25rem 0 -.1rem 0; vertical-align: bottom; }';
				style.appendChild(document.createTextNode(css));
				shadowRoot.appendChild(style);
				if (callback) callback(shadowRoot);


			});
		});
	},
	getElement: async function(id, callback) {
		return new Promise(callback => {
			Command.get(id, null, async function(element) {
				return callback(element);
			});
		});
	},
	replaceTemplateExpressions: async (el) => {
		let element = await _SimplePages.getElement(el.id);
		let html;
		if (element.content) {
			html = element.content;
		} else {
			html = await fetch('/' + element.id + '?edit=2').then(response => response.text());
			//html = '<div class="dom-element">' + html + '</div>';
		}

		let regex = /\$\{include\('(.*)'\)\}/g;
		if (html.match(regex)) {
			matches = html.matchAll(regex);
			for (let match of matches) {
				let includeName = match[1];
				let start = html.indexOf(match[0]);
				let end   = start + match[0].length;
				html = await _SimplePages.replaceInclude(includeName, html, start, end);
			}
		}

		regex = /\$\{include_child\('(.*)'\)\}/g;
		if (html.match(regex)) {
			matches = html.matchAll(regex);
			for (let match of matches) {
				let includeName = match[1];
				let start = html.indexOf(match[0]);
				let end   = start + match[0].length;
				html = await _SimplePages.replaceInclude(includeName, html, start, end, element);
			}
		}

		regex = /\$\{render\(children\)\}/g;
		if (html.match(regex) && element.children && element.children.length) {
			matches = html.matchAll(regex);
			let childrenHtml = '';
			// loop over all child elements
			for (let i=0; i<element.children.length; i++) {
				let child = element.children[i];
				// get HTML content of each child, replace all includes
				// and append it to the HTML of all child elements
				let childHtml = await _SimplePages.replaceTemplateExpressions(child);
				childrenHtml += childHtml;
			}

			html = html.replaceAll(regex, childrenHtml);
		}
		//return html;
		return '<div class="slot">' + html + '</div>';
	},
	replaceInclude: async (includeName, html, start, end, parent) => {
		let properties = { 'name': includeName };
		if (parent) properties.parentId = parent.id;
		return new Promise(callback => {
			Command.query('Template', 1, 1, 'name', 'asc', properties, async (result) => {
				if (result && result.length) {
					let element = result[0];
					html = html.replace(html.substring(start, end), await _SimplePages.replaceTemplateExpressions(element));
				}
				//html = '<div class="slot">' + html + '</div>';
				callback(html);
			});
		});
	},
	refreshCurrentPage: function() {
		_SimplePages.refreshPage(LSWrapper.getItem(_SimplePages.currentPageIdKey));
	},
	refreshPage: function(id) {
		document.querySelector('#id_' + id).click();
	},
	appendTemplate: function(template, parent, callback) {

		Structr.fetchHtmlTemplate('simple-pages/template-selection-node', { template: template }, function(html) {
			dialogText.append(html);

			let cloneComponent = function() {
				Command.cloneComponent(template.id, parent.id, function() {
					if (callback) callback();
				});
			}

			$('.template-' + template.id).on('click', cloneComponent);

			let iframe = $('.template-' + template.id).find('iframe');
			iframe.contents().on('click', cloneComponent);
		});

	},
	addPageTemplate: function(parent, callback) {

		//console.log(parent);

		Structr.dialog('Add page template to ' + parent.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});

		Structr.getShadowPage(() => {

			Command.query('TemplateCategory', 10, 1, 'name', 'desc', { name: 'Page Templates' }, function(categories) {

				Command.query('Template', 1000, 1, 'name', 'desc', { category: { id: categories[0].id }, hidden: false, pageId: shadowPage.id }, function(templates) {

					templates.forEach(function(template) {
						_SimplePages.appendTemplate(template, parent, callback);
					});
				});
			});
		});

	},
	addTemplate: function(parent, callback) {

		var name = parent.name || parent.tag || parent.id;

		Structr.dialog('Add template to ' + name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});

		var classes = parent._html_class && parent._html_class.length ? parent._html_class.split(' ') : [];
		var htmlId  = parent._html_id;
		var tag     = parent.tag;

		Command.getSuggestions(htmlId, parent.name, tag, classes, function(result) {

			result.forEach(function(widget) {

				var id = 'create-from-' + widget.id;
				dialogText.append('<div class="app-tile"><h4>' + widget.name + '</h4><p>' + widget.description + '</p><button class="action" id="insert-' + id + '">Insert</button></div>');

				$('#insert-' + id).on('click', function() {

					_Widgets.insertWidgetIntoPage(widget, parent, parent.pageId, function() {
						_SimplePages.refreshCurrentPage();
					});
				});
			});

		});
	},
	replaceTemplate: function(t) {
		Structr.dialog('Replace template ' + t.name, function() {
			_SimplePages.refreshCurrentPage();
		});

		Structr.getShadowPage(() => {

			Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, pageId: shadowPage.id }, function(templates) {

				templates.forEach(function(template) {

					Structr.fetchHtmlTemplate('simple-pages/template-selection-node', { template: template }, function(html) {
						dialogText.append(html);

						document.querySelector('.template-' + template.id).on('click', function() {

							Command.replaceTemplate(t.id, template.id, function() {
								_SimplePages.refreshCurrentPage();
								dialogCancelButton.click();
							});
						});
					});
				});
			});
		});

	},
	removeTemplateFromParent: function(id) {
		if (id) {
			//doc.contents().find('[data-structr-id="' + id + '"]').remove();
			Command.removeChild(id, function() {
				_SimplePages.refreshCurrentPage();
				//console.log(doc, doc.contents(), doc.contents().find('[data-structr-id="' + id + '"]'));
				//doc.contents().find('[data-structr-id="' + id + '"]').remove();
			});
		}
	},
	resize: function() {

		var windowHeight = window.innerHeight;
		var headerOffsetHeight = 100;

		let iframes = document.querySelector('.page-root iframe.preview');
		if (iframes) {
			iframes.style.height = windowHeight - headerOffsetHeight - 13 + 'px';
		}

		_SimplePages.moveResizer();
		Structr.resize();

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_SimplePages.resizerLeftKey) || 300;
		_SimplePages.components.main.querySelector('.column-resizer').style.left = left + 'px';

		var contextWidth = 240;
		var width        = window.innerWidth - left - contextWidth - 81;

		_SimplePages.components.tree.style.width = left - 14 + 'px';
		_SimplePages.components.contents.style.left = left + 8 + 'px';
		_SimplePages.components.contents.style.width = width - 14 + 'px';
		_SimplePages.components.context.style.left = left + width + 18 + 'px';
		_SimplePages.components.context.style.width = contextWidth + 'px';
	},
	dragStart: function(e) {
		e.stopPropagation();
		//e.currentTarget.style.border = 'dashed';
		e.dataTransfer.setData('text/plain', e.target.dataset.structrId);
		//e.dataTransfer.dropEffect = 'move';
		//e.dataTransfer.effectAllowed = 'move';
	},
	dragEnter: function(e) {
		e.stopPropagation();
		//e.preventDefault();
		let el = e.target.dataset.structrId ? e.target : e.target.closest('[data-structr-id');
		el.style.border = '1px dashed rgba(0,0,0,.25)';
	},
	dragLeave: function(e) {
		e.stopPropagation();
		let el = e.target;
		e.currentTarget.style.border = '1px solid rgba(0,0,0,.25)';
		e.target.closest('[data-structr-id').style.border = '1px dashed rgba(0,0,0,.25)';
	},
	dragEnd: function(e) {
		//e.stopPropagation();
		//e.preventDefault();
		//e.currentTarget.style.border = '1px solid rgba(0,0,0,.25)';
	},
	drop: function(e) {
		//e.preventDefault();
		e.stopPropagation();
		e.target.style.border = '1px solid rgba(0,0,0,.25)';
		let sourceId = e.dataTransfer.getData('text/plain');
		let targetId = e.target.dataset.structrId || e.target.closest('[data-structr-id').dataset.structrId;
		//console.log('drop', sourceId, targetId);
		if (sourceId && targetId && sourceId !== targetId) {
			Command.appendChild(sourceId, targetId, null, function() {
				_SimplePages.refreshCurrentPage();
			});
		}
	},
	listDataSources: function() {
		Command.query('DataSource', 1000, 1, 'name', 'desc', { hidden: false }, function(dataSources) {
			dataSources.forEach(function(dataSource) {
				Structr.fetchHtmlTemplate('simple-pages/datasource-list-node', { dataSource: dataSource }, function(html) {
					_SimplePages.components.data.insertAdjacentHTML('beforeEnd', html);
				});
			});
		});
	},
	listFlowContainers: function() {
		Command.query('FlowContainer', 1000, 1, 'name', 'desc', { hidden: false }, function(flowContainers) {
			flowContainers.forEach(function(flowContainer) {

				// console.log(flowContainer.name, flowContainer.id);

				Structr.fetchHtmlTemplate('simple-pages/flowcontainer-list-node', { flowContainer: flowContainer }, function(html) {
					_SimplePages.components.data.insertAdjacentHTML('beforeEnd', html);

					let flowContainerElement = _SimplePages.components.data.querySelector('#id_' + flowContainer.id);

					_SimplePages.jsPlumbInstance.addEndpoint(flowContainerElement, {
						anchor: 'Top',
						maxConnections: -1,
						isTarget: true,
						deleteEndpointsOnDetach: false
					});

					window.setTimeout(() => _SimplePages.displayFlowContainerConnections(flowContainer), 250);

				});
			});
		}, true, 'public');
	},
	listEventHandlers: function() {
		Command.query('EventHandler', 1000, 1, 'name', 'desc', { hidden: false }, function(eventHandlers) {
			eventHandlers.forEach(function(eventHandler) {
				Structr.fetchHtmlTemplate('simple-pages/eventhandler-list-node', { eventHandler: eventHandler }, function(html) {
					_SimplePages.components.logic.insertAdjacentHTML('beforeEnd', html);

					let eventHandlerElement = _SimplePages.components.logic.querySelector('#id_' + eventHandler.id);

					_SimplePages.jsPlumbInstance.addEndpoint(eventHandlerElement, {
						anchor: 'Left',
						maxConnections: -1,
						isTarget: true,
						deleteEndpointsOnDetach: false
					});

					_SimplePages.jsPlumbInstance.addEndpoint(eventHandlerElement, {
						anchor: 'Bottom',
						maxConnections: -1,
						isSource: true,
						deleteEndpointsOnDetach: false
					});

					let eventSelector = eventHandlerElement.querySelector('#event');
					addEvent(eventSelector, 'change', (e) => {
	    				// console.log('change event to', e.target.value);
	    				let newValue = e.target.value;
						Command.setProperty(eventHandler.id, 'event', newValue, false, function() {
							// console.log('done');
							new MessageBuilder().success('Event changed to "' + newValue + '"').show();
						});
					});

					let actionSelector = eventHandlerElement.querySelector('#action');
					addEvent(actionSelector, 'change', (e) => {
						// console.log('change action to', e.target.value);
						let newValue = e.target.value;
						Command.setProperty(eventHandler.id, 'action', newValue, false, function() {
							// console.log('done');
							new MessageBuilder().success('Event action to "' + newValue + '"').show();
						});
					});
				});
			});

			Structr.fetchHtmlTemplate('simple-pages/create-event-handler-button', {}, function(html) {

				_SimplePages.components.logic.insertAdjacentHTML('beforeEnd', html);
				_SimplePages.components.logic.querySelector('.create-event-handler-button').addEventListener('click', function() {
					Command.create({type: 'EventHandler'}, function(eventHandler) {
						LSWrapper.setItem(_SimplePages.currentPageIdKey, eventHandler.id);
						_SimplePages.refreshEventHandlers();
					});
				});
			});
		}, true, 'custom');


	}
};

function addEvent(el, type, handler) {

	if (el.attachEvent) {

		el.attachEvent('on'+type, handler);

	} else {

		el.addEventListener(type, handler);
	}
}

function live(selector, event, callback, context) {

	addEvent(context || document, event, function(e) {

		let el = (e.target || e.srcElement).closest(selector);

		// console.log({
		// 	selector: selector,
		// 	event: event,
		// 	callback: callback,
		// 	context: context,
		// 	el: el
		// });

		if (el) callback.call(el, e);
	});
}