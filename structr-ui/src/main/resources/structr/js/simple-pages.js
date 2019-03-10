/*
 * Copyright (C) 2010-2019 Structr GmbH
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

var _SimplePages = {
	
	_moduleName: 'simple-pages',
	resizerLeftKey: 'structrSimplePagesResizerLeftKey_' + port,
	currentPageIdKey: 'structrSimplePagesCurrentPageIdKey_' + port,
	
	components: {},
	objectCache: {},
	

	onload: function() {

		Structr.fetchHtmlTemplate('simple-pages/main', {}, function(html) {

			main.append(html);
			
			_SimplePages.components.main                  = document.querySelector('#simple-pages-main');
			             
			_SimplePages.components.tree                  = document.querySelector('#simple-pages-tree');
			_SimplePages.components.contents              = document.querySelector('#simple-pages-contents');
			_SimplePages.components.context               = document.querySelector('#simple-pages-context');
			
			_SimplePages.moveResizer();
			Structr.initVerticalSlider(document.querySelector('.column-resizer', _SimplePages.components.main), _SimplePages.resizerLeftKey, 204, _SimplePages.moveResizer);
			
			_SimplePages.resize();

			window.removeEventListener('resize', null, false);
			window.addEventListener('resize', function() {
				_SimplePages.resize();
			});

			Structr.unblockMenu(500);
			
			_SimplePages.init();
			
		});
		
	},
	init: function() {
		_SimplePages.refreshPageList();
	},
	refreshPageList: function() {
		fastRemoveAllChildren(_SimplePages.components.tree);
		_SimplePages.listPages();
	},
	showAddPageTemplateButton: function(callback) {
		Structr.fetchHtmlTemplate('simple-pages/add-page-template-button', null, function(html) {
			_SimplePages.components.pageRoot.insertAdjacentHTML('beforeEnd', html);
			_SimplePages.components.addPageTemplateButton = document.querySelector('.add-page-template');
			if (callback) callback();
		});
	},
	hideAddPageTemplateButton: function() {
		let el = querySelector('.add-page-template');
		el.parentNode.removeChild(el);
	},
	listPages: function() {
	
		Command.query('Page', 1000, 1, 'name', 'desc', { hidden: false }, function(pages) {
			
			pages.forEach(function(page) {

				Structr.fetchHtmlTemplate('simple-pages/page-list-node', { page: page }, function(html) {
					
					_SimplePages.components.tree.insertAdjacentHTML('beforeEnd', html);

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
			});
			
			Structr.fetchHtmlTemplate('simple-pages/create-page-button', {}, function(html) {

				_SimplePages.components.tree.insertAdjacentHTML('beforeEnd', html);
				_SimplePages.components.tree.querySelector('.create-page-button').addEventListener('click', function() {
					Command.create({type: 'Page'}, function() {
						_SimplePages.refreshPageList();
					});
				});
			});
			
		});
	},
	loadPage: function(pageId) {
		
		// clear object cache
		_SimplePages.objectCache = {};

		Command.get(pageId, null, function(page) {

			Structr.fetchHtmlTemplate('simple-pages/page-root', { page: page }, function(html) {

				fastRemoveAllChildren(_SimplePages.components.contents);
				_SimplePages.components.contents.insertAdjacentHTML('beforeEnd', html);
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
							head.insertAdjacentHTML('beforeEnd', '<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/sprites.css">');
							head.insertAdjacentHTML('beforeEnd', '<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/simple-pages.css">');
						}

						// attach event listener
						live('.delete_icon', 'click', function() {
							let id = this.getAttribute('data-structr-id');
							Command.get(id, null, function(t) {
								_SimplePages.removeTemplateFromParent(t);
							}, doc);

						});

						live('.edit_props_icon', 'click', function() {
							let id = this.closest('[data-structr-id]').getAttribute('data-structr-id');
							console.log('edit_props_icon clicked', id);
							Command.get(id, null, function(t) {
								_Entities.showProperties(t);
							});
						}, doc);

						live('.replace_icon', 'click', function() {
							let id = this.closest('[data-structr-id]').getAttribute('data-structr-id');
							Command.get(id, null, function(t) {
								_SimplePages.replaceTemplate(t);
							});
						}, doc);

						live('.add_icon', 'click', function() {
							let id = this.closest('[data-structr-id]').getAttribute('data-structr-id');
							Command.get(id, null, function(t) {
								_SimplePages.addTemplate(t, function() {
									dialogCancelButton.click();
									_SimplePages.refreshCurrentPage();
								});
							});
						}, doc);

						let x,y;
//						doc.addEventListener('mousemove', function(e) {
//							x = e.clientX, y = e.clientY;
//							//console.log(x,y);
//						});
//						live('[data-structr-id]', 'mouseenter', function(e) {
//							e.preventDefault();
//							e.stopPropagation();
//							let templateElement = this;
//							templateElement.classList.remove('hover');
//						});

						_SimplePages.resize();

						let templates = doc.querySelectorAll('[data-structr-id]');
						templates.forEach(function(templateElement) {

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
								console.log(distance);
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
							
//							doc.removeEventListener('mouseleave', null, false);
//							doc.addEventListener('mouseleave', function() {
//								let children = this.querySelectorAll(':not([data-structr-id]):not(.edit-header)');
//								children.forEach(function(childNode) {
//									childNode.style.display = '';
//								});
//							});

							//doc.find('[data-structr-id]').on('mouseenter', function() {
//							t.removeEventListener('mouseenter', null, false);
//							t.removeEventListener('mouseleave', null, false);
							
//							templateElement.addEventListener('mouseleave', function(e) {
//								e.preventDefault();
//								e.stopPropagation();
//								templateElement.classList.remove('hover');
//							});
							
//							templateElement.addEventListener('mouseenter', function(e) {
//								e.preventDefault();
//								e.stopPropagation();
//								let templateElement = this;
////								
////								let children = this.querySelectorAll(':not([data-structr-id]):not(.edit-header)');
////								children.forEach(function(childNode) {
////									childNode.style.display = 'none';
////								});
//								templateElement.classList.add('hover');
//							});
							
							if (!_SimplePages.objectCache.hasOwnProperty(id)) {

								Command.get(id, null, function(t) {

									_SimplePages.objectCache[id] = t;

									Structr.fetchHtmlTemplate('simple-pages/template-edit-elements', { template: t }, function(html) {
										
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
								});

							}
						});

					} catch (e) { console.log(e); };

				});       
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
			iframe.load(function() {
				iframe.contents().on('click', cloneComponent);
			});

		});
		
	},
	addPageTemplate: function(parent, callback) {
		Structr.dialog('Add page template to ' + parent.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});
		
		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
			
			let shadowPage = entities[0];
			
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
		Structr.dialog('Add template to ' + parent.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});
		
		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
			
			let shadowPage = entities[0];
			
			Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, pageId: shadowPage.id }, function(templates) {

				templates.forEach(function(template) {
					_SimplePages.appendTemplate(template, parent, callback);
				});
			});
		});
		
	},
	replaceTemplate: function(t) {
		Structr.dialog('Replace template ' + t.name, function() {
			_SimplePages.refreshCurrentPage();
		});
		
		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
			
			let shadowPage = entities[0];
			
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

		if (_SimplePages.components.tree) {
			_SimplePages.components.tree.style.height = windowHeight - headerOffsetHeight - 27 + 'px';
		}

		if (_SimplePages.components.contents) {
			_SimplePages.components.contents.style.height = windowHeight - headerOffsetHeight - 11 + 'px';
		}

		if (_SimplePages.components.context) {
			_SimplePages.components.context.style.height = windowHeight - headerOffsetHeight - 11 + 'px';
		}
		
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
		_SimplePages.components.contents.style.width = width - 24 + 'px';
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
	}
};

function addEvent(el, type, handler) {
    if (el.attachEvent) el.attachEvent('on'+type, handler); else el.addEventListener(type, handler);
}

function live(selector, event, callback, context) {
    addEvent(context || document, event, function(e) {
        let el = (e.target || e.srcElement).closest(selector);
        if (el) callback.call(el, e);
    });
}