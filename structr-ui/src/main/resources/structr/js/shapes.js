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
var pagesArea, widgetsArea, statusArea, paWidth, paHeight, currentPage, block = 0, activePreviewDoc, widgetGroups = {};
var appBuilderActiveWidgetTabRightKey = 'structrAppBuilderActiveWidgetTabRightKey' + port;

$(document).ready(function() {
	Structr.registerModule(_Shapes);
	Structr.classes.push('shapes');
});

var _Shapes = {
	_moduleName: 'shapes',
	autoRefresh: [],
	onload: function() {

		Structr.fetchHtmlTemplate('shapes/main', {}, function(html) {

			main.append(html);

			_Shapes.init();

			$(window).off('resize').resize(function() {
				_Shapes.resize();
			});

			Structr.unblockMenu(500);

		});

	},
	init: function() {

		pagesArea = $('#pages-area');
		widgetsArea = $('#widgets-area');
		statusArea = $('#status-info');

		_Shapes.refresh();

	},
	refresh: function(page) {
		_Pages.clearIframeDroppables();

		if (!page) {
			_Shapes.zoomOut();

		} else {
			var iframe = $('.page-tn').find('#app-preview_' + page.id);
//			iframe.load(function() {
//				_AppBuilder.zoomIn(page)
//			});
			iframe.attr('src', iframe.attr('src'));
		}
	},
	activateDocShadows: function(doc) {
		//var iframe = $('.page-tn').find('#app-preview_' + currentPage.id);
		//var doc = iframe.contents();
		if (!doc)
			return;
		doc.find('body').addClass('active-shadows');
	},
	zoomOut: function() {
		widgetsArea.hide();
		_Pages.clearIframeDroppables();

		$('#zoom-out').remove();
		currentPage = undefined;
		pagesArea.empty();

		var x = 0, y = 0, c = 3;
		Command.query('Page', 12, 1, 'position', 'asc', { hidden: false },
		function(pages) {
			pages.forEach(function(page) {
				if (x > c) {
					x = 0;
					y++;
				}

				pagesArea.append('<div id="page-tn-' + page.id + '" class="page-tn"><div class="page-preview">'
						+ '<iframe class="preview" id="app-preview_' + page.id + '"></iframe>'
						+ '</div><div class="page-name">' + page.name + '</div>'
						+ '<div class="icon clone-page" title="Clone Page"><i class="' + _Icons.getFullSpriteClass(_Icons.clone_icon) + '" /></div>'
						+ '<div class="icon delete-page" title="Delete Page"><i class="' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /></div>'
						+ '</div>');

				var tn = $('#page-tn-' + page.id);
				tn.find('.clone-page').on('click', function() {
					Command.clonePage(page.id, function() {
						_Shapes.refresh();
					});
				});

				tn.find('.delete-page').on('click', function() {
					Command.deleteNode(page.id, true, function() {
						_Shapes.refresh();
					});
				});

				tn.css({left: x * 300, top: y * 300});
				x++;

				_Shapes.activateAreas(page);

				$('.page-tn').not('.zoomed').find('#app-preview_' + page.id).load(function() {
					var doc = $(this).contents();
					doc.off('click');
					doc.on('click', function() {
						_Shapes.zoomIn(page);
						_Shapes.loadWidgets();
					});
					return false;
				});

				$('#app-preview_' + page.id).attr('src', '/structr/html/' + page.name + '?edit=4');

			});

			if (x > c) {
				x = 0;
				y++;
			}

			pagesArea.append('<div id="add-page-area" class="page-tn"><i class="fa fa-plus"></i></div>');
			$('#add-page-area').css({left: x * 300, top: y * 300}).on('click', function() {
				Command.create({type: 'Page'}, function() {
					_Shapes.refresh();
				});
			});
		}, false);
	},
	zoomIn: function(page) {

		currentPage = page;
		$('.page-tn').not('#page-tn-' + page.id).hide();
		$('.page-tn').off('click');
		$('.page-preview').off('click');

		//$('.page-tn').find('#app-preview_' + page.id).contents().off('click');

		var pagePreview = $('#page-tn-' + page.id);
		var iframe = $('.page-tn').find('#app-preview_' + page.id);
		var doc = iframe.contents();

		doc.off('click').off('mouseenter').off('mouseleave');

		doc.on('mouseenter', function() {
			doc.find('body').addClass('active-shadows');
		}).on('mouseleave', function() {
			doc.find('body').removeClass('active-shadows');
		});

		pagePreview.addClass('zoomed').off('click');

		var zoomOutButton = $('#zoom-out');
		if (!zoomOutButton.size()) {
			pagePreview.append('<button title="Close Preview" id="zoom-out" class="remove">×</button><div id="status-info"></div>');
			$('#zoom-out', pagePreview).on('click', function() {
				_Shapes.zoomOut();
				return false;
			});
		}

		var windowWidth = $(window).width();
		var windowHeight = $(window).height();
		var offsetWidth = 160, headerOffsetHeight = 124;

		$('#app-preview_' + page.id).css({
			width: ((windowWidth - offsetWidth) * .6) + px(pagesArea, 2),
			height: windowHeight - headerOffsetHeight + px(pagesArea, 2)
		});

		widgetsArea.show();

	},
	activateAreas: function(page) {
		$('#app-preview_' + page.id).load(function() {
			var iframe = $(this);
			var doc = $(this).contents();
			var head = doc.find('head');
			if (head) {
				head.append('<link rel="stylesheet" type="text/css" href="/structr/css/lib/font-awesome.min.css">');
				head.append('<style media="screen" type="text/css">'
						+ '* { z-index: 0}\n'
						+ 'body.active-shadows [data-structr-area] { position: relative ! important; -moz-box-shadow: 0 0 .1em #ccc ! important; -webkit-box-shadow: 0 0 .1em #ccc ! important; box-shadow: 0 0 .1em #ccc ! important; }\n'
						+ 'body.active-shadows [data-structr-area] > * { -moz-box-shadow: 0 0 .1em #ccc ! important; -webkit-box-shadow: 0 0 .1em #ccc ! important; box-shadow: 0 0 .1em #ccc ! important; }\n'
						+ 'body.active-shadows [data-structr-area]:hover, .widget-hover { min-height: 100px; opacity: .8 ! important; -moz-box-shadow: 0 0 .1em #000 ! important; -webkit-box-shadow: 0 0 .1em #000 ! important; box-shadow: 0 0 .1em #000 ! important; }\n'
						+ 'body.active-shadows .button-area {  -moz-box-shadow: none ! important; -webkit-box-shadow: none ! important; box-shadow: none ! important; z-index: 99 ! important; position: absolute ! important; color: #555 ! important; line-height: 1.85em ! important; width: 8em ! important; padding: 0 ! important; margin: 0 ! important; }\n'
						+ 'body.active-shadows .button-area button { float: right; width: 2em; border-radius: .2em; border: 1px solid #aaa; padding: 0; margin: 0; text-align: center; text-shadow: 0 1px 0 #fff; }\n'
						+ 'body.active-shadows .button-area button:hover { background-color: #eee; text-shadow: none; }\n'
						+ '.structr-editable-area { background-color: #ffe; -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px yellow; box-shadow: 0 0 5px #888; }\n'
						+ '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange ! important; color: #333; }\n'
						//+ '[data-structr-area]:hover { background-color: #ffe; border: 1px solid orange ! important; color: #333; }\n'
						/**
						 * Fix for bug in Chrome preventing the modal dialog background
						 * from being displayed if a page is shown in the preview which has the
						 * transform3d rule activated.
						 */
						+ '.navbar-fixed-top { -webkit-transform: none ! important; }\n'
						+ '</style>');
			}

			_Pages.activateComments(doc, function() {
				_Shapes.refresh(currentPage);
			});

			_Shapes.activateDocShadows(doc);

			doc.find('*').each(function(i, element) {

				$(element).children('[data-structr-area]').each(function(i, el) {
					var area = $(el);

					var children = area.children();

					if (children.length === 0) {
						area.css({minHeight: 100});
					} else {
						children.each(function(i, child) {
							var c = $(child);
							_Shapes.bindActions(c, area);
						});
					}

					area.droppable({
						iframeFix: true,
						iframe: iframe,
						accept: '.widget-preview, .element',
						greedy: true,
						hoverClass: 'widget-hover',
						drop: function(e, ui) {
							e.preventDefault();
							e.stopPropagation();

							var sourceId = $(ui.draggable).attr('id').substr('widget-preview-'.length);
							var targetId = $(this).attr('data-structr-id');

							console.log('sourceId', sourceId);
							console.log('targetId', targetId);

							var tag;
							if (!sourceId) {

								var d = $(ui.draggable);
								tag = d.text();
								if (d.attr('subkey')) {
									related = {};
									related.subKey = d.attr('subkey');
									related.isCollection = (d.attr('collection') === 'true');
								}

								Command.get(targetId, 'id,type,isContent', function(target) {
									_Dragndrop.htmlElementFromPaletteDropped(tag, target, page.id, function() {
										_Shapes.refresh(currentPage);
									});
								});

							}

							var source = StructrModel.obj(sourceId);
							var target = StructrModel.obj(targetId);

							//console.log(source, target);

							if (!target && source) {
								Command.get(targetId, 'id', function(target) {
									_Dragndrop.widgetDropped(source, target, page.id, function() {
										_Shapes.refresh(currentPage);
									});
								});
							} else if (source && target) {

								// objects are already stored in model
								_Dragndrop.widgetDropped(source, target, page.id, function() {
									_Shapes.refresh(currentPage);
								});

							} else {

								// try to get objects from server
								Command.get(sourceId, 'id,type,source,description,configuration', function(source) {
									Command.get(targetId, 'id', function(target) {
										_Dragndrop.widgetDropped(source, target, page.id, function() {
											_Shapes.refresh(currentPage);
										});
									});
								});
							}

						}
					});

				});

			});
		});
	},
	bindActions: function(c, area) {

		var body = area.closest('body');

		c.on('mouseenter', function() {
			block++;
			body.find('.button-area').remove();

			var buttonArea = $('.button-area', area);
			if (buttonArea.length === 0) {
				var isFirst = c.is(':first');
				var isLast = c.is(':last');
				var hasSiblings = c.parent().children().length > 1;

				area.append('<div class="button-area"><button class="edit-button"><i class="fa fa-cog"></i></button><button class="remove-button"><i class="fa fa-trash"></i></button></div>');
				buttonArea = $('.button-area', area);

				if (hasSiblings) {
					if (!isFirst) {
						buttonArea.prepend('<button class="up-button"><i class="fa fa-arrow-up"></i></button>');
					}

					if (!isLast) {
						buttonArea.prepend('<button class="down-button"><i class="fa fa-arrow-down"></i></button>');
					}
				}
			}

			buttonArea.css({
				left: c.position().left + c.outerWidth() - buttonArea.outerWidth(),
				top: c.position().top
			});
			buttonArea.children('button').on('mouseenter', function(e) {
				block++;
			}).on('mouseleave', function(e) {
				block--;
				window.setTimeout(function() {
					if (!block) {
						area.find('.button-area').remove();
					}
				}, 10);
			});

			var removeButton = buttonArea.children('.remove-button');
			removeButton.on('click', function(e) {
				e.stopPropagation();
				var id = c.attr('data-structr-id');
				var parentId = area.attr('data-structr-id');
				Command.removeSourceFromTarget(id, parentId, function(obj, size, command) {
					if (command === 'REMOVE_CHILD') {
						_Shapes.refresh(currentPage);
					}
				});
				return false;
			});

			var editButton = buttonArea.children('.edit-button');
			editButton.on('click', function(e) {
				e.stopPropagation();
				var id = c.attr('data-structr-id');
				_Entities.showProperties({id: id});
				return false;
			});

			var upButton = buttonArea.children('.up-button');
			upButton.on('click', function(e) {
				e.stopPropagation();
				var id = c.attr('data-structr-id');
				// TODO: Move element up
				return false;
			});

			var downButton = buttonArea.children('.down-button');
			downButton.on('click', function(e) {
				e.stopPropagation();
				var id = c.attr('data-structr-id');
				// TODO: Move element down
				return false;
			});

		}).on('mouseleave', function() {
			block = 0;
			window.setTimeout(function() {
				if (!block) {
					area.find('.button-area').remove();
				}
			}, 1);
		});
	},
	loadWidgets: function() {

		fastRemoveAllChildren(widgetsArea[0]);
		widgetsArea.append('<div class="widget-tabs"><ul></ul></div>');
		var w;
		Command.list('Widget', true, 1000, 1, 'name', 'asc', 'id,name,type,source,treePath,isWidget,previewWidth,previewHeight', function(widgets) {
			w = widgets;
			_Widgets.getRemoteWidgets(_Widgets.url, function(widget) {
				w.push(widget);
			}, function() {
				_Shapes.appendWidgets(w);
			});
		});
	},
	appendWidgets: function(widgets) {

		var tabs = $('.widget-tabs');
		var ul = tabs.children('ul').first();

		widgetGroups = {};

		widgets.forEach(function(widget) {

			if (widget.treePath) {

				var cleanedPath = clean(widget.treePath);

				if (widgetGroups[cleanedPath]) {
					widgetGroups[cleanedPath].push(widget);
					return;
				} else {
					widgetGroups[cleanedPath] = [widget];
				}

			}
		});

		var key = 'html-palette';
		ul.append('<li><a href="#widget-tab-area-' + key + '">' + key + '</a></li>');
		tabs.append('<div id="widget-tab-area-' + key + '"></div>');
		var tabArea = $('#widget-tab-area-' + key);

		{
			$(_Elements.elementGroups).each(function(i, group) {
				tabArea.append('<div class="elementGroup" id="group_' + group.name + '"><h3>' + group.name + '</h3></div>');
				$(group.elements).each(function(j, elem) {
					var div = $('#group_' + group.name);
					div.append('<div class="draggable element" id="add_' + elem + '">' + elem + '</div>');
					$('#add_' + elem, div).draggable({
						iframeFix: true,
						revert: 'invalid',
						containment: 'body',
						helper: 'clone',
						appendTo: '#main',
						//stack: '.node',
						zIndex: 99,
						activeClass: 'widget-hover'
					});
				});
			});
			_Shapes.resize();
		}

		_Shapes.appendWidgetLib(widgetGroups, function() {

			Object.keys(widgetGroups).forEach(function(key) {
				ul.append('<li><a href="#widget-tab-area-' + key + '">' + key + '</a></li>');
				tabs.append('<div id="widget-tab-area-' + key + '"></div>');
			});

			$('.widget-tabs').tabs({
				activate: function(e, ui) {
					//console.log(ui.newTab.text());
					var key = clean(ui.newTab.text());
					var tabArea = $('#widget-tab-area-' + key);

					if (widgetGroups[key] && widgetGroups[key].length) {

						fastRemoveAllChildren(tabArea[0]);

						widgetGroups[key].forEach(function(widget) {
							StructrModel.create(widget, null, false);
							_Shapes.appendWidget(widget, tabArea);
						});
						if (widgetGroups[key]) {
							_Shapes.activateWidgets(widgetGroups[key]);
						}

					}
					LSWrapper.setItem(appBuilderActiveWidgetTabRightKey, key);
				}
			});

			var tabName = LSWrapper.getItem(appBuilderActiveWidgetTabRightKey);
			if (tabName) {
				$('a[href="#widget-tab-area-' + tabName + '"]').click();
			}

		});

	},
	appendWidgetLib: function(groups, callback) {
		callback();
		return;

		var key = 'bootstrap', url = 'http://getbootstrap.com/components/', cls = '.bs-example';

		groups[key] = [];

		$('body').append('<iframe id="load-' + key + '"></iframe>');

		var libIframe = $('body').find('iframe#load-' + key);
		libIframe.css({width: 0, height: 0}).load(function() {
			var doc = $(this).contents();
			doc.find(cls).each(function(i, example) {
				var ex = $(example);

				var exampleId = ex.attr('data-example-id');

				if (!exampleId)
					return;

				var widget = {
					id: exampleId,
					name: exampleId.replaceAll('-', ' '),
					source: ex.html()
				};

				groups[key].push(widget);

			});

			callback();

		});

		libIframe.attr('src', '/proxy?url=' + url);

	},
	activateWidgets: function(widgets) {
		widgets.forEach(function(widget) {
			var previewBox = $('#widget-preview-' + widget.id);
			var iframe = $('iframe', previewBox);
			// Trigger reload
			iframe.attr('src', 'about:blank');
			console.log('activated widget', widget);
		});
	},
	appendWidget: function(widget, el) {

		el.append('<div class="widget-tn"><div id="widget-preview-' + widget.id + '" class="widget-preview"><iframe></iframe><div class="widget-name">' + widget.name + '</div></div>');
		var previewBox = $('#widget-preview-' + widget.id);

		previewBox.draggable({
			//helper: 'clone',
			revert: true,
			revertDuration: 0,
			zIndex: 10001,
			appendTo: 'body',
//			cursorAt: {
//				left: 120,
//				top: 60
//			},
			activeClass: 'widget-hover',
			containment: 'body',
			start: function(e, ui) {
				$(ui.helper).css({
					'z-index': 10000
				});
				_Shapes.activateDocShadows();
			}
		});

		$('iframe', previewBox).load(function() {
			var iframe = $(this);
			var css = {
				display: 'none'
			};

			iframe.css(css);
			var doc = $(this).contents();

			css = {
				width: '100%',
				height: '100%',
				overflow: 'hidden',
				padding: 0,
				margin: 0
			};

			doc.find('body').css(css);

			if (currentPage) {
				var linkElements = $('#app-preview_' + currentPage.id).contents().find('head').find('link[rel="stylesheet"]');
				linkElements.each(function(i, link) {
					doc.find('head').append($(link).clone());
				});
				var scriptElements = $('#app-preview_' + currentPage.id).contents().find('script');
				scriptElements.each(function(i, script) {
					doc.find('head').append($(script).clone());
				});
			}

			doc.find('body').append(widget.source);
			var firstEl = doc.find('body').children().first();
			firstEl.css({overflow: 'hidden'});

			window.setTimeout(function() {
				css = {
					width: firstEl.outerWidth(),
					height: firstEl.outerHeight(),
					overflow: 'hidden',
					cursor: 'move',
					display: 'block'
				};
				iframe.css(css);
				previewBox.show();
				_Shapes.resize();
			}, 100);
		});
	},
	resize: function() {

		Structr.resize();

		$('body').css({
			position: 'fixed'
		});

		var windowWidth = $(window).width();
		var windowHeight = $(window).height();
		var offsetWidth = 160;
		var headerOffsetHeight = 124;

		$('#app-builder').css({
			width: windowWidth,
			height: windowHeight
		});

		$('#pages-area').css({
			width: ((windowWidth - offsetWidth) * .6),
			height: windowHeight - headerOffsetHeight
		});

		if (currentPage) {
			$('#app-preview_' + currentPage.id).css({
				width: ((windowWidth - offsetWidth) * .6) + px(pagesArea, 2),
				height: windowHeight - headerOffsetHeight + px(pagesArea, 2)
			});
		}

		$('#widgets-area').css({
			width: ((windowWidth - offsetWidth) * .4),
			height: windowHeight - headerOffsetHeight
		});

		var tabsHeight = $('#widgets-area .widget-tabs ul').outerHeight();

		$('#widgets-area .ui-tabs-panel').css({
			height: windowHeight - headerOffsetHeight - tabsHeight
		});

	}
};

function px(el, em) {
	var fontSize = parseFloat(el.css("font-size"));
	return em * fontSize;
}

function clean(input) {
	return input.replaceAll('/', '').replaceAll(' ', '');
}