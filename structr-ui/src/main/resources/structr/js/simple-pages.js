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

$(document).ready(function() {
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
			
			_SimplePages.components.main     = $('#simple-pages-main');
			
			_SimplePages.components.tree     = $('#simple-pages-tree');
			_SimplePages.components.contents = $('#simple-pages-contents');
			_SimplePages.components.context  = $('#simple-pages-context');
			
			_SimplePages.moveResizer();
			Structr.initVerticalSlider($('.column-resizer', _SimplePages.components.main), _SimplePages.resizerLeftKey, 204, _SimplePages.moveResizer);
			
			_SimplePages.resize();

			$(window).off('resize').resize(function() {
				_SimplePages.resize();
			});

			Structr.unblockMenu(500);
			
			_SimplePages.init();
			
		});
		
	},
	init: function() {
		_SimplePages.listPages();
	},
	listPages: function() {
	
		Command.query('Page', 1000, 1, 'name', 'desc', { hidden: false }, function(pages) {
			
			pages.forEach(function(page) {
				
				Structr.fetchHtmlTemplate('simple-pages/page-list-node', { page: page }, function(html) {
					
					_SimplePages.components.tree.append(html);

					$('#id_' + page.id).on('click', function() {
						LSWrapper.setItem(_SimplePages.currentPageIdKey, page.id);
						let self = $(this);
						self.siblings().removeClass('selected');
						self.addClass('selected');
						_SimplePages.loadPage(page);
					});
					
					let currentPageId = LSWrapper.getItem(_SimplePages.currentPageIdKey);
					if (currentPageId) {
						$('#id_' + currentPageId).click();
					}
				});
			});
			
			
		});
	},
	loadPage: function(page) {
		
		// clear object cache
		_SimplePages.objectCache = {};
		
		Structr.fetchHtmlTemplate('simple-pages/page-root', { page: page }, function(html) {
		
			fastRemoveAllChildren(_SimplePages.components.contents[0]);
			_SimplePages.components.contents.append(html);
			
			$('#preview_' + page.id).load(function() {
				try {
					let doc = $(this).contents();
					let head = doc.find('head');
					if (head) {
						head.append('<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/sprites.css">');
						head.append('<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/simple-pages.css">');
					}
					
					_SimplePages.resize();
					
					//doc.find('[data-structr-id]').on('mouseenter', function() {
					doc.find('[data-structr-id]').off('mouseover').on('mouseover', function() {
						let self = $(this);
						let id = self.data('structr-id');

						if (!_SimplePages.objectCache.hasOwnProperty(id)) {
							
							Command.get(id, null, function(t) {
								
								_SimplePages.objectCache[id] = t;
								
								Structr.fetchHtmlTemplate('simple-pages/template-edit-elements', { template: t }, function(html) {
								
									self.prepend(html);
									
									let editElements = doc.find('.edit-template-' + id);
									
									// Bind actions
									editElements.find('.delete_icon').off('click').on('click', function() { _SimplePages.removeTemplateFromParent(id, doc); });
									editElements.find('.edit_props_icon').off('click').on('click', function() { _Entities.showProperties(t); });
									editElements.find('.replace_icon').off('click').on('click', function() { _SimplePages.replaceTemplate(t); });
									editElements.find('.add_icon').off('click').on('click', function() { _SimplePages.addTemplate(t); });
									
									// make draggable (HTML5 drag'n drop)
									self.attr('draggable', 'true').attr('ondragover', 'return false');
									self[0].addEventListener('dragstart', _SimplePages.dragStart, false);
									self[0].addEventListener('dragenter', _SimplePages.dragEnter, false);
									self[0].addEventListener('dragleave', _SimplePages.dragLeave, false);
									self[0].addEventListener('dragover', _SimplePages.dragOver, false);
									//self[0].addEventListener('dragend', _SimplePages.dragEnd, false);
									self[0].addEventListener('drop', _SimplePages.drop, false);
									
								});
							});
						}
						
						self.addClass('hover');
						return false;
					});
					
					//doc.find('[data-structr-id]').on('mouseleave', function() {
					doc.find('[data-structr-id]').off('mouseout').on('mouseout', function() {
						let self = $(this);
						self.removeClass('hover');
						//doc.find('.edit-template-' + id).remove();
						return false;
					});
					
				} catch (e) { console.log(e); };

			});       
		});
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
	refreshCurrentPage: function() {
		_SimplePages.refreshPage(LSWrapper.getItem(_SimplePages.currentPageIdKey));
	},
	refreshPage: function(id) {
		$('#id_' + id).click();
	},
	addTemplate: function(t) {
		Structr.dialog('Add template ' + t.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});
		
		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
			
			let shadowPage = entities[0];
			
			Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, pageId: shadowPage.id }, function(templates) {

				templates.forEach(function(template) {

					Structr.fetchHtmlTemplate('simple-pages/template-selection-node', { template: template }, function(html) {
						dialogText.append(html);
						
						$('.template-' + template.id).on('click', function() {
							Command.cloneComponent(template.id, t.id, function() {
								_SimplePages.refreshCurrentPage();
							});
						});
						
					});

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

						$('.template-' + template.id).on('click', function() {

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
	removeTemplateFromParent: function(id, doc) {
		Command.removeChild(id, function() {
			doc.find('[data-structr-id="' + id + '"]').remove();
		});
	},
	resize: function() {

		var windowHeight = $(window).height();
		var headerOffsetHeight = 100;

		if (_SimplePages.components.tree) {
			_SimplePages.components.tree.css({
				height: windowHeight - headerOffsetHeight - 27 + 'px'
			});
		}

		if (_SimplePages.components.contents) {
			_SimplePages.components.contents.css({
				height: windowHeight - headerOffsetHeight + 13 + 'px'
			});
		}

		if (_SimplePages.components.context) {
			_SimplePages.components.context.css({
				height: windowHeight - headerOffsetHeight - 11 + 'px'
			});
		}
		
		var iframes = $('.page-root').find('iframe.preview');
		iframes.css({
			height: windowHeight - headerOffsetHeight + 13 + 'px'
		});

		_SimplePages.moveResizer();
		Structr.resize();

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_SimplePages.resizerLeftKey) || 300;
		$('.column-resizer', _SimplePages.components.main).css({ left: left });

		var contextWidth = 240;
		var width        = $(window).width() - left - contextWidth - 81;

		_SimplePages.components.tree.css({width: left - 14 + 'px'});
		_SimplePages.components.contents.css({left: left + 8 + 'px', width: width + 'px'});
		_SimplePages.components.context.css({left: left + width + 18 + 'px', width: contextWidth + 'px'});
	},

};