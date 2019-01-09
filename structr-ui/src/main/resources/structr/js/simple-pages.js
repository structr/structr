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
	
	components: {},

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
						let self = $(this);
						self.siblings().removeClass('selected');
						self.addClass('selected');
						_SimplePages.loadPage(page);
					});
				});
			});
			
			
		});
	},
	loadPage: function(page) {
		
		Structr.fetchHtmlTemplate('simple-pages/page-root', { page: page }, function(html) {
		
			fastRemoveAllChildren(_SimplePages.components.contents[0]);
			_SimplePages.components.contents.append(html);
			
			$('#preview_' + page.id).load(function() {
				try {
					var doc = $(this).contents();
					var head = $(doc).find('head');
					if (head) {
						head.append('<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/sprites.css"><style media="screen" type="text/css">'
								+ '* { z-index: 0}\n'
								+ '[data-structr-id] { position: relative; display: block; padding: 24px; margin; 24px; border: 1px solid rgba(0,0,0,.125); box-shadow: 0px 0px 36px rgba(127,127,127,.1); }\n'
								+ '[data-structr-id].hover { border: 1px solid rgba(0,0,0,.25); box-shadow: 0px 0px 36px rgba(127,127,127,.2); }\n'
								+ '[data-structr-id].hover > .edit_icon { display: block ! important }\n'
								+ '[data-structr-id].hover > .add_icon { display: block ! important }\n'
								+ '[data-structr-id].hover > .delete_icon { display: block ! important }\n'
								+ '.nodeHover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
								+ '.structr-content-container { min-height: .25em; min-width: .25em; }\n'
								+ '.structr-element-container-active:hover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
								+ '.structr-element-container-selected { -moz-box-shadow: 0 0 8px #860; -webkit-box-shadow: 0 0 8px #860; box-shadow: 0 0 8px #860; }\n'
								+ '.structr-element-container-selected:hover { -moz-box-shadow: 0 0 10px #750; -webkit-box-shadow: 0 0 10px #750; box-shadow: 0 0 10px #750; }\n'
								+ '.structr-editable-area { background-color: #ffe; -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px yellow; box-shadow: 0 0 5px #888; }\n'
								+ '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange ! important; color: #333; }\n'
								+ '.link-hover { border: 1px solid #00c; }\n'
								+ '.edit_icon, .add_icon, .delete_icon, .close_icon, .key_icon { cursor: pointer; height: 16px; width: 16px; top: 4px; position: absolute; }\n'
								+ '.edit_icon { right: 24px; position: absolute; }\n'
								+ '.add_icon { right: 4px; }\n'
								+ '.delete_icon { right: 44px; }\n'
								/**
								 * Fix for bug in Chrome preventing the modal dialog background
								 * from being displayed if a page is shown in the preview which has the
								 * transform3d rule activated.
								 */
								+ '.navbar-fixed-top { -webkit-transform: none ! important; }'
								+ '</style>');
					}
					
					_SimplePages.resize();
					
					$(doc).find('[data-structr-id]').on('mouseover', function() {
						let self = $(this);
						self.addClass('hover');
						return false;
					});
					
					$(doc).find('[data-structr-id]').on('mouseout', function() {
						let self = $(this);
						self.removeClass('hover');
					});
					
				} catch (e) { console.log(e); };

			});
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
				height: windowHeight - headerOffsetHeight - 11 + 'px'
			});
		}

		if (_SimplePages.components.context) {
			_SimplePages.components.context.css({
				height: windowHeight - headerOffsetHeight - 11 + 'px'
			});
		}
		
		var iframes = $('.page-root').find('iframe.preview');
		iframes.css({
			height: windowHeight - (headerOffsetHeight + 30) + 'px'
		});

		_SimplePages.moveResizer();
		Structr.resize();

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_SimplePages.resizerLeftKey) || 300;
		$('.column-resizer', _SimplePages.components.main).css({ left: left });

		var contextWidth = 240;
		var width        = $(window).width() - left - contextWidth - 80;

		_SimplePages.components.tree.css({width: left - 14 + 'px'});
		_SimplePages.components.contents.css({left: left + 8 + 'px', width: width + 'px'});
		_SimplePages.components.context.css({left: left + width + 41 + 'px', width: contextWidth + 'px'});
	},

};