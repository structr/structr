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
document.addEventListener('DOMContentLoaded', function() {
	Structr.registerModule(_SimplePages);
	// Structr.classes.push('');
});

let _SimplePages = {

	_moduleName: 'simple-pages',
	resizerLeftKey: 'structrSimplePagesResizerLeftKey_' + location.port,
	currentPageIdKey: 'structrSimplePagesCurrentPageIdKey_' + location.port,

	components: {},
	objectCache: {},

	onload: function() {

		main[0].innerHTML = _SimplePages.templates.main();

		_SimplePages.components.main      = document.querySelector('#simple-pages-main');
		_SimplePages.components.tree      = document.querySelector('#simple-pages-tree');
		_SimplePages.components.contents  = document.querySelector('#simple-pages-contents');
		_SimplePages.components.context   = document.querySelector('#simple-pages-context');

		_SimplePages.moveResizer();
		Structr.initVerticalSlider(document.querySelector('.column-resizer', _SimplePages.components.main), _SimplePages.resizerLeftKey, 204, _SimplePages.moveResizer);

		_SimplePages.resize();

		window.removeEventListener('resize', null, false);
		window.addEventListener('resize', function() {
			_SimplePages.resize();
		});

		Structr.unblockMenu(500);

		_SimplePages.init();
	},
	init: function() {
		_SimplePages.refreshPageList();
		_SimplePages.refreshWidgetList();
	},
	refreshPageList: function() {
		fastRemoveAllChildren(_SimplePages.components.tree);
		_SimplePages.listPages();
	},
	refreshWidgetList: function() {
		fastRemoveAllChildren(_SimplePages.components.context);
		_SimplePages.listWidgets();
	},
	showAddPageTemplateButton: function(callback) {
		let html = '<i title="Add Page Template" class="add-page-template button sprite sprite-add"></i>';
		_SimplePages.components.pageRoot.insertAdjacentHTML('beforeEnd', html);
		_SimplePages.components.addPageTemplateButton = document.querySelector('.add-page-template');
		if (callback) callback();
	},
	hideAddPageTemplateButton: function() {
		let el = querySelector('.add-page-template');
		el.parentNode.removeChild(el);
	},
	listPages: function() {

		Command.query('Page', 1000, 1, 'name', 'desc', { hidden: false }, function(pages) {

			pages.forEach(function(page) {

				let pageListNodeHtml = _SimplePages.templates.pageListNode({ page: page });
				_SimplePages.components.tree.insertAdjacentHTML('beforeEnd', pageListNodeHtml);

				live('#id_' + page.id, 'click', function() {

					LSWrapper.setItem(_SimplePages.currentPageIdKey, page.id);
					_SimplePages.components.tree.querySelectorAll('.page').forEach(function(childNode) {
						childNode.classList.remove('selected');
					});
					this.classList.add('selected');
					_SimplePages.loadPage(page.id);
				});

				let currentPageId = LSWrapper.getItem(_SimplePages.currentPageIdKey);
				if (currentPageId === page.id) {
					document.querySelector('#id_' + currentPageId).click();
				}
			});

			let createPageButton = '<i class="create-page-button icon sprite sprite-add"></i>';

			_SimplePages.components.tree.insertAdjacentHTML('beforeEnd', createPageButton);
			_SimplePages.components.tree.querySelector('.create-page-button').addEventListener('click', function() {
				Command.create({type: 'Page'}, function() {
					_SimplePages.refreshPageList();
				});
			});
		});
	},
	loadPage: function(pageId) {

		// clear object cache
		_SimplePages.objectCache = {};

		Command.get(pageId, null, function(page) {

			fastRemoveAllChildren(_SimplePages.components.contents);

			let pageRootHtml = _SimplePages.templates.pageRoot({ page: page });
			_SimplePages.components.contents.insertAdjacentHTML('beforeEnd', pageRootHtml);
			_SimplePages.components.pageRoot = document.querySelector('.page-root');

			if (page.children.length === 0) {

				// page has no child element => show add icon
				_SimplePages.showAddPageTemplateButton(function() {

					_SimplePages.components.addPageTemplateButton.addEventListener('click', function() {

						_SimplePages.addPageTemplate(page, function() {
							dialogCancelButton.click();
							_SimplePages.hideAddPageTemplateButton();
							window.setTimeout(function() {
								_SimplePages.refreshCurrentPage();
							}, 250);
						});
					});
				});
			}

			document.querySelector('#preview_' + page.id).addEventListener('load', function() {

				try {
					let doc = this.contentDocument;
					let head = doc.querySelector('head');
					if (head) {
						head.insertAdjacentHTML('beforeEnd', '<link rel="stylesheet" type="text/css" media="screen" href="' + Structr.getPrefixedRootUrl('/structr/css/sprites.css') + '">');
						head.insertAdjacentHTML('beforeEnd', '<link rel="stylesheet" type="text/css" media="screen" href="' + Structr.getPrefixedRootUrl('/structr/css/simple-pages.css') + '">');
					}

					// attach event listener
					live('.delete_icon', 'click', function() {
						let id = this.closest('[data-structr-from-widget]').getAttribute('data-structr-id');
						Command.removeChild(id, function() {
							_SimplePages.refreshCurrentPage();
						});

					}, doc);

					live('.edit_props_icon', 'click', function() {
						let id = this.closest('[data-structr-id]').getAttribute('data-structr-id');
						console.log('edit_props_icon clicked', id);
						Command.get(id, null, function(t) {
							_Entities.showProperties(t);
						});
					}, doc);

					/*
					live('.replace_icon', 'click', function() {
						let id = this.closest('[data-structr-id]').getAttribute('data-structr-id');
						Command.get(id, null, function(t) {
							_SimplePages.replaceTemplate(t);
						});
					}, doc);
					*/

					live('.add_icon', 'click', function() {
						let id = this.closest('[data-structr-insert]').getAttribute('data-structr-id');
						Command.get(id, null, function(t) {
							_SimplePages.addTemplate(t, function() {
								dialogCancelButton.click();
								_SimplePages.refreshCurrentPage();
							});
						});
					}, doc);

					let x,y;

					_SimplePages.resize();

					doc.querySelectorAll('[data-structr-insert]').forEach(function(templateElement) {

						let id = templateElement.getAttribute('data-structr-id');

						live('[data-structr-id="' + id + '"]', 'mouseenter', function(e) {
							e.preventDefault();
							e.stopPropagation();
							let templateElement = this;

							let children = doc.querySelectorAll('.edit-header');
							children.forEach(function(childNode) {
								childNode.style.zIndex = '9999999';
								//childNode.style.position = 'absolute';
							});
							templateElement.setAttribute('x-enter', x);
							templateElement.setAttribute('y-enter', y);
							templateElement.classList.add('hover');
							x = e.screenX, y = e.screenY;
						}, templateElement);

						live('[data-structr-id="' + id + '"]', 'mouseleave', function(e) {
							e.preventDefault();
							e.stopPropagation();

							let templateElement = this;
							let xEnter = parseInt(templateElement.getAttribute('x-enter'));
							let yEnter = parseInt(templateElement.getAttribute('y-enter'));
							let distance = Math.sqrt(Math.pow(e.screenX - xEnter, 2) + Math.pow(e.screenY - yEnter, 2));
							if (distance > 15) {

								templateElement.removeAttribute('x-enter');
								templateElement.removeAttribute('y-enter');

								let children = doc.querySelectorAll('.edit-header');
								children.forEach(function(childNode) {
									childNode.style.zIndex = '';
									//childNode.style.position = '';
								});

								templateElement.classList.remove('hover');
							}
						}, templateElement);

						if (!_SimplePages.objectCache.hasOwnProperty(id)) {

							Command.get(id, null, function(t) {

								_SimplePages.objectCache[id] = t;

								let html = _SimplePages.templates.templateEditElements({ template: t });
								templateElement.insertAdjacentHTML('afterBegin', html);

								//alert(html);
								//$(templateElement).append(html);

								// bind actions
								// make draggable (HTML5 drag'n drop)
								templateElement.setAttribute('draggable', 'true');
								templateElement.setAttribute('ondragover', 'return false');
								templateElement.addEventListener('dragstart', _SimplePages.dragStart, false);
								templateElement.addEventListener('dragenter', _SimplePages.dragEnter, false);
								templateElement.addEventListener('dragleave', _SimplePages.dragLeave, false);
								templateElement.addEventListener('dragover', _SimplePages.dragOver, false);
								//el.addEventListener('dragend', _SimplePages.dragEnd, false);
								templateElement.addEventListener('drop', _SimplePages.drop, false);
							});
						}
					});

					// all widgets already inserted into this template
					doc.querySelectorAll('[data-structr-from-widget]').forEach(function(templateElement) {

						let id = templateElement.getAttribute('data-structr-id');

						live('[data-structr-id="' + id + '"]', 'mouseenter', function(e) {
							e.preventDefault();
							e.stopPropagation();
							let templateElement = this;

//								let allTemplates = doc.querySelectorAll('[data-structr-id]');
//								allTemplates

							let children = doc.querySelectorAll('.edit-header');
							children.forEach(function(childNode) {
								childNode.style.zIndex = '9999999';
								//childNode.style.position = 'absolute';
							});
							templateElement.setAttribute('x-enter', x);
							templateElement.setAttribute('y-enter', y);
							templateElement.classList.add('hover');
							x = e.screenX, y = e.screenY;
						}, templateElement);

						live('[data-structr-id="' + id + '"]', 'mouseleave', function(e) {
							e.preventDefault();
							e.stopPropagation();

							let templateElement = this;
							let xEnter = parseInt(templateElement.getAttribute('x-enter'));
							let yEnter = parseInt(templateElement.getAttribute('y-enter'));
							let distance = Math.sqrt(Math.pow(e.screenX - xEnter, 2) + Math.pow(e.screenY - yEnter, 2));
							if (distance > 15) {

								templateElement.removeAttribute('x-enter');
								templateElement.removeAttribute('y-enter');

								let children = doc.querySelectorAll('.edit-header');
								children.forEach(function(childNode) {
									childNode.style.zIndex = '';
									//childNode.style.position = '';
								});

								templateElement.classList.remove('hover');
							}
						}, templateElement);

						if (!_SimplePages.objectCache.hasOwnProperty(id)) {

							Command.get(id, null, function(t) {

								_SimplePages.objectCache[id] = t;

								let html = _SimplePages.templates.widgetEditElements({ widget: t });
								templateElement.insertAdjacentHTML('afterBegin', html);
							});
						}
					});

				} catch (e) {
					console.log(e);
				};
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

		let html = _SimplePages.templates.templateSelectionNode({ template: template });
		dialogText.append(html);

		let cloneComponent = function() {
			Command.cloneComponent(template.id, parent.id, function() {
				if (callback) callback();
			});
		}

		$('.template-' + template.id).on('click', cloneComponent);

		let iframe = $('.template-' + template.id).find('iframe');
		iframe.contents().on('click', cloneComponent);
	},
	addPageTemplate: function(parent, callback) {

		Structr.dialog('Add page template to ' + parent.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});

		Structr.getShadowPage(() => {

			Command.query('TemplateCategory', 10, 1, 'name', 'desc', { name: 'Page Templates' }, function(categories) {

				Command.query('Template', 1000, 1, 'name', 'desc', { category: { id: categories[0].id }, hidden: false, pageId: _Pages.shadowPage.id }, function(templates) {

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

			Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, pageId: _Pages.shadowPage.id }, function(templates) {

				templates.forEach(function(template) {

					let html = _SimplePages.templates.templateSelectionNode({ template: template });
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
		let el = e.target.dataset.structrId ? e.target : e.target.closest('[data-structr-id]');
		el.style.border = '1px dashed rgba(0,0,0,.25)';
	},
	dragLeave: function(e) {
		e.stopPropagation();
		let el = e.target;
		e.currentTarget.style.border = '1px solid rgba(0,0,0,.25)';
		e.target.closest('[data-structr-id]').style.border = '1px dashed rgba(0,0,0,.25)';
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
		let targetId = e.target.dataset.structrId || e.target.closest('[data-structr-id]').dataset.structrId;
		//console.log('drop', sourceId, targetId);
		if (sourceId && targetId && sourceId !== targetId) {
			Command.appendChild(sourceId, targetId, null, function() {
				_SimplePages.refreshCurrentPage();
			});
		}
	},
	listWidgets: function() {

		Command.query('Widget', 1000, 1, 'name', 'desc', { hidden: false }, function(widgets) {

			widgets.forEach(function(widget) {

				let html = _SimplePages.templates.widgetListNode({ widget: widget });
				_SimplePages.components.context.insertAdjacentHTML('beforeEnd', html);
			});
		});
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/simple-pages.css">
			
			<div id="simple-pages-main" class="tree-main">
			
				<div class="column-resizer column-resizer-left"></div>
			
				<div class="tree-container" id="simple-pages-tree-container">
					<div class="tree-search-container" id="tree-search-container">
						<i id="cancel-search-button" class="cancel-search-button hidden fa fa-remove"></i>
						<button type="button" class="tree-back-button" id="tree-back-button" title="Back" disabled><i class="fa fa-caret-left"></i></button>
						<input type="text" class="tree-search-input" id="tree-search-input" placeholder="Search.." />
						<button type="button" class="tree-forward-button" id="tree-forward-button" title="Forward" disabled><i class="fa fa-caret-right"></i></button>
					</div>
					<div class="tree" id="simple-pages-tree">
					</div>
				</div>
			
				<div class="tree-contents-container" id="simple-pages-contents-container">
					<div class="tree-contents" id="simple-pages-contents">
					</div>
				</div>
			
				<div class="tree-context-container" id="simple-pages-context-container">
					<div class="tree-context" id="simple-pages-context">
					</div>
				</div>
			</div>
		`,
		functions: config => ``,
		pageListNode: config => `
			<div id="id_${config.page.id}" class="node page ui-sortable ui-droppable">
				<!--i title="Collapse ${config.page.name}" class="expand_icon sprite sprite-tree_arrow_down"></i-->
				<i class="typeIcon sprite sprite-page"></i>
				<b title="${config.page.name}" class="name_">${config.page.name}</b>
				<span class="id">${config.page.id}</span>
				<i title="Access Control and Visibility" class="key_icon button sprite sprite-key donthide" style="display: inline-block;"></i>
				<i title="Delete page '${config.page.name}'" class="delete_icon button sprite sprite-delete" style="display: none;"></i>
				<i title="Edit Properties" class="edit_props_icon button sprite sprite-application_view_detail" style="display: none;"></i>
				<i title="Clone page '${config.page.name}'" class="clone_icon button sprite sprite-page_copy" style="display: none;"></i>
			</div>
		`,
		pageRoot: config => `
			<div class="page-root">
				<iframe class="preview" id="preview_${config.page.id}" src="/structr/html/${config.page.id}?edit=5"></iframe>
			</div>
		`,
		previewUrlListNode: config => `
			<div id="id_${config.widget.id}" class="node page ui-sortable ui-droppable">
				<i class="typeIcon sprite sprite-page"></i>
				<b title="${config.widget.name}" class="name_">${config.widget.name}</b>
				<span class="id">${config.widget.id}</span>
				<i title="Access Control and Visibility" class="key_icon button sprite sprite-key donthide" style="display: inline-block;"></i>
				<i title="Delete page '${config.widget.name}'" class="delete_icon button sprite sprite-delete" style="display: none;"></i>
				<i title="Edit Properties" class="edit_props_icon button sprite sprite-application_view_detail" style="display: none;"></i>
				<i title="Clone page '${config.widget.name}'" class="clone_icon button sprite sprite-page_copy" style="display: none;"></i>
			</div>		
		`,
		templateCodePreview: config => `
			<h3 class="element-info"></h3>
			<div class="code-preview">
			</div>
		`,
		templateEditElements: config => `
			<div class="edit-template-${config.template.id} edit-header">
				<!--i title="Collapse ${config.template.name}" class="expand_icon sprite sprite-tree_arrow_down"></i-->
				<div title="${config.template.name}" class="name_">${config.template.name || ''}</div>
				<i title="Edit Properties" class="edit_props_icon button sprite sprite-application_view_detail"></i>
				<i title="Add Child Element" class="add_icon button sprite sprite-add"></i>
				<!--<i title="Remove Template" class="delete_icon button sprite sprite-page_white_delete"></i>-->
				<!--<i title="Replace Template" class="replace_icon button sprite sprite-arrow_switch"></i>-->
			</div>
		`,
		templateSelectionNode: config => `
			<div class="template-${config.template.id} template-preview">
				<div title="${config.template.name}" class="name_">${config.template.name || ''}</div>
				<iframe src="/structr/html/widget/${config.template.id}"></iframe>
			</div>
		`,
		widgetEditElements: config => `
			<div class="edit-widget-${config.widget.id} edit-header">
				<i id="remove-${config.widget.id}" title="Remove Widget" class="delete_icon button sprite sprite-page_white_delete"></i>
			</div>
		`,
		widgetListNode: config => `
			<div id="id_${config.widget.id}" class="node page ui-sortable ui-droppable">
				<i class="typeIcon sprite sprite-page"></i>
				<b title="${config.widget.name}" class="name_">${config.widget.name}</b>
				<span class="id">${config.widget.id}</span>
				<i title="Access Control and Visibility" class="key_icon button sprite sprite-key donthide" style="display: inline-block;"></i>
				<i title="Delete page '${config.widget.name}'" class="delete_icon button sprite sprite-delete" style="display: none;"></i>
				<i title="Edit Properties" class="edit_props_icon button sprite sprite-application_view_detail" style="display: none;"></i>
				<i title="Clone page '${config.widget.name}'" class="clone_icon button sprite sprite-page_copy" style="display: none;"></i>
			</div>
		`,
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

		if (el) callback.call(el, e);
	});
}