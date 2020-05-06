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
var main, crawlerMain, crawlerTree, crawlerList;
var currentSite, currentPage;
var sitePageSize = 10000, sitePage = 1;
var currentSiteKey = 'structrCurrentSite_' + port;
var crawlerResizerLeftKey = 'structrCrawlerResizerLeftKey_' + port;
var link, path, elid, claz, pageFrame, frameDoc;
var proxyUrl = '/structr/proxy';

$(document).ready(function() {
	Structr.registerModule(_Crawler);
});

var _Crawler = {
	_moduleName: 'crawler',
	init: function() {

		_Logger.log(_LogType.CRAWLER, '_Crawler.init');

		main = $('#main');

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

	},
	resize: function() {
		_Crawler.moveResizer();
		Structr.resize();
	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(crawlerResizerLeftKey) || 300;
		$('.column-resizer', crawlerMain).css({ left: left });

		$('#crawler-tree-container').css({width: left - 14 + 'px'});
		$('#crawler-list').css({ left: left + 8 + 'px', width: $(window).width() - left - 50 + 'px' });
	},
	onload: function() {

		$.get(rootUrl + '/me/ui', function(data) {
			_Dashboard.meObj = data.result;
			_Crawler.init();
		});

		Structr.updateMainHelpLink('https://support.structr.com/knowledge-graph');

		main.append(`<div class="tree-main" id="crawler-main"><div class="column-resizer"></div><div class="tree-container" id="crawler-tree-container"><div class="tree" id="crawler-tree"></div></div><div class="fit-to-height tree-contents-container" id="crawler-list-container"><div class="tree-contents tree-contents-with-top-buttons" id="crawler-list"></div></div>`);
		crawlerMain = $('#crawler-main');

		crawlerTree = $('#crawler-tree');
		crawlerList = $('#crawler-list');

		_Crawler.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', crawlerMain), crawlerResizerLeftKey, 204, _Crawler.moveResizer);

		$('#crawler-list-container').prepend(`<button class="add_site_icon button"><i title="Add Site" class="${_Icons.getFullSpriteClass(_Icons.add_site_icon)}" /> Add Site</button>`);

		$('.add_site_icon', main).on('click', function(e) {
			e.stopPropagation();
			Command.create({ type: 'SourceSite' }, function(site) {
				_Crawler.refreshTree();
				window.setTimeout(function() {
					$('#' + site.id + '_anchor').click();
				}, 250);
			});
		});

		$('#crawler-list-container').append(`<button class="add_page_icon button"><i title="Add Page" class="${_Icons.getFullSpriteClass(_Icons.add_page_icon)}" /> Add Page</button>`);

		$('.add_page_icon', main).on('click', function(e) {

			e.stopPropagation();
			if (currentSite) {
				Command.create({ type: 'SourcePage', site: currentSite.id }, function(site) {
					_Crawler.refreshTree();
				});
			}
		});


		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		crawlerTree.on('ready.jstree', function() {
			_TreeHelper.makeTreeElementDroppable(crawlerTree, 'root');

			_Crawler.loadAndSetWorkingDir(function() {
				if (currentSite) {
//					_Crawler.deepOpen(currentSite);
				}
//				window.setTimeout(function() {
//					crawlerTree.jstree('select_node', currentSite ? currentSite.id : 'root');
//				}, 100);
			});
		});

		crawlerTree.on('select_node.jstree', function(evt, data) {

			if (data.node.id === 'root') {
				_Crawler.deepOpen(currentSite);
			}

			if (data.node.icon === 'fa fa-sitemap') {
				_Crawler.setWorkingDirectory(data.node.id);
				_Crawler.displayPages(data.node.id, data.node.site);
			} else {
				_Crawler.setWorkingDirectory(data.node.parent);
				_Crawler.displayPatterns(data.node.id);
			}

		});

		_TreeHelper.initTree(crawlerTree, _Crawler.treeInitFunction, 'structr-ui-crawler');

		$(window).off('resize').resize(function() {
			_Crawler.resize();
		});

		_Crawler.resize();

		Structr.unblockMenu(100);

	},
	deepOpen: function(d, pages) {

		_TreeHelper.deepOpen(crawlerTree, d, pages, 'site', (currentSite ? currentSite.id : 'root'));

	},
	refreshTree: function() {

		_TreeHelper.refreshTree(crawlerTree);

	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':
				Command.list('CrawlerTreeNode', true, sitePageSize, sitePage, 'name', 'asc', null, function(sites) {

					var list = [];

					sites.forEach(function(d) {

						if (d.type === 'SourceSite') {

							list.push({
								id: d.id,
								text: d.name ? d.name : '[unnamed]',
								children: d.pages && d.pages.length > 0,
								icon: 'fa fa-sitemap'
							});

						}

					});

					callback(list);
				});
				break;

			case 'root':
				_Crawler.load(null, callback);
				break;

			default:
				_Crawler.load(obj.id, callback);
				break;
		}

	},
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#crawler-main', main));
	},
	loadAndSetWorkingDir: function(callback) {

		currentSite = LSWrapper.getItem(currentSiteKey);
		callback();

	},
	load: function(id, callback) {

		if (!id) {

			Command.list('SourceSite', true, sitePageSize, sitePage, 'name', 'asc', null, function(sites) {

				var list = [];

				sites.forEach(function(d) {
					list.push({
						id: d.id,
						text:  d.name ? d.name : '[unnamed]',
						pages: d.sourcePages,
						icon: 'fa fa-sitemap',
						path: d.path
					});
				});

				callback(list);

				_TreeHelper.makeDroppable(crawlerTree, list);

			}, true);

		} else {

			Command.query('SourcePage', sitePageSize, sitePage, 'name', 'asc', {site: id}, function(pages) {

				var list = [];

				pages.forEach(function(d) {
					list.push({
						id: d.id,
						text:  d.name ? d.name : '[unnamed]',
						patterns: d.pattern,
						icon: 'fa fa-file-code-o',
						path: d.path
					});
				});

				callback(list);

				_TreeHelper.makeDroppable(crawlerTree, list);

			}, true);
		}

	},
	setWorkingDirectory: function(id) {
		currentSite = { 'id': id };
		LSWrapper.setItem(currentSiteKey, currentSite);
	},
	displayPages: function(id, siteId) {

		fastRemoveAllChildren(crawlerList[0]);

		Command.get(id, 'id,type,name', function(site) {

			var name = (site.name || '[unnamed]');
			crawlerList.append(`
				<div class="site-header">
					<div id="id_${site.id}" class="site-name">
						<b class="name_" title="${name}">${name}</b>
					</div>
					<div class="button-area">
						<i title="Edit Properties" class="edit-properties ${_Icons.getFullSpriteClass(_Icons.view_detail_icon)}" />
						<i title="Delete Site" class="delete ${_Icons.getFullSpriteClass(_Icons.delete_icon)}" />
					</div>
				</div>`);

			var nameEl = $('.site-header .site-name', crawlerList);
			nameEl.children('b.name_').off('click').on('click', function (e) {
				_Entities.makeNameEditable(nameEl, 200, function() {
					_Crawler.refreshTree();
				});
			});

			$('.site-header .delete', crawlerList).on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, site, false, function() {
					_Crawler.refreshTree();
					window.setTimeout(function() {
						$('.jstree-wholerow').first().click();
					}, 250);
				});
			});

			var editIcon = $('.site-header .edit-properties', crawlerList);
			editIcon.on('click', function(e) {
				e.stopPropagation();
				_Entities.showProperties(site);
			});

			var handlePage = function(pages) {
				if (pages && pages.length) {
					pages.forEach(function(page) {
						_Crawler.appendPageRow(page);
					});
				}
			};

			_Pager.initPager('crawler-pages', 'SourcePage', 1, 25, 'name', 'asc');
			page['SourcePage'] = 1;
			_Pager.initFilters('crawler-pages', 'SourcePage', id === 'root' ? {} : { site: id });

			var itemsPager = _Pager.addPager('crawler-pages', $('.site-header', crawlerList), false, 'SourcePage', 'ui', handlePage);

			itemsPager.cleanupFunction = function () {
				var toRemove = $('.node.item', itemsPager.el).closest('tr');
				toRemove.each(function(i, elem) {
					fastRemoveAllChildren(elem);
				});
			};

			crawlerList.append(`<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>URL</th><th>Login Page</th></tr></thead><tbody id="files-table-body"></tbody></table>`);
		});
	},
	appendPageRow: function(sourcePage) {

		// add container/item to global model
		StructrModel.createFromData(sourcePage, null, false);

		var tableBody = $('#files-table-body');

		$('#row' + sourcePage.id, tableBody).remove();

		var rowId = 'row' + sourcePage.id;
		tableBody.append('<tr id="' + rowId + '"' + (sourcePage.isThumbnail ? ' class="thumbnail"' : '') + '></tr>');
		var row = $('#' + rowId);
		var icon = 'fa-file-code-o';

		row.append(`<td class="file-type"><a href="${sourcePage.url || ''}" target="_blank"><i class="fa ${icon}"></i></a></td>`);
		row.append(`<td><div id="id_${sourcePage.id}" data-structr_type="item" class="node item"><b title="${sourcePage.name ? sourcePage.name : '[unnamed]'}" class="name_">${sourcePage.name ? fitStringToWidth(sourcePage.name, 200) : '[unnamed]'}</b></td>`);

		row.append(`<td><div class="editable url_" title="${sourcePage.url || ''}">${sourcePage.url && sourcePage.url.length ? sourcePage.url : '<span class="placeholder">click to edit</span>'}</div></td>`);
		row.append(`<td>${sourcePage.isLoginPage ? '✓' : ''}</td>`);

		var div = Structr.node(sourcePage.id);

		if (!div || !div.length)
			return;

		row.find('.url_').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeAttributeEditable(row, sourcePage.id, '.url_', 'url', 200, function() {
				_Crawler.refreshTree();
			});
		});

		div.on('remove', function() {
			div.closest('tr').remove();
		});

		_Entities.appendAccessControlIcon(div, sourcePage);

		var delIcon = div.children('.delete_icon');

		var newDelIcon = '<i title="Delete item ' + sourcePage.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />';
		if (delIcon && delIcon.length) {
			delIcon.replaceWith(newDelIcon);
		} else {
			div.append(newDelIcon);
			delIcon = div.children('.delete_icon');
		}
		div.children('.delete_icon').on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, sourcePage, false, function() {
				_Crawler.refreshTree();
			});
		});

		div.on('click', function() {
			_Crawler.displayPatterns(sourcePage.id);
		});

		div.draggable({
			revert: 'invalid',
			containment: 'body',
			stack: '.jstree-node',
			appendTo: '#main',
			forceHelperSize: true,
			forcePlaceholderSize: true,
			distance: 5,
			cursorAt: { top: 8, left: 25 },
			zIndex: 99,
			stop: function(e, ui) {
				$(this).show();
				$(e.toElement).one('click', function(e) {
					e.stopImmediatePropagation();
				});
			},
			helper: function(event) {
				var hlp = $(this).clone();
				hlp.find('.button').remove();
				return hlp;
			}
		});

		_Entities.appendEditPropertiesIcon(div, sourcePage);
		_Entities.setMouseOver(div);
		_Entities.makeSelectable(div);

	},
	displayPatterns: function(pageId) {

		Command.get(pageId, "id,url,name,isLoginPage,site,proxyUrl,proxyUsername,proxyPassword,authUsername,authPassword,cookie", function(sourcePage) {

			fastRemoveAllChildren(crawlerList[0]);

			var name = (sourcePage.name || '[unnamed]');
			crawlerList.append(`
				<div class="page-header">
					<div id="id_${sourcePage.id}" class="page-name"><b class="name_" title="' + name + '">${name}</b></div>
					<div class="button-area">
						<i title="Edit Properties" class="edit-properties ${_Icons.getFullSpriteClass(_Icons.view_detail_icon)}" />
						<i title="Delete Page" class="delete ${_Icons.getFullSpriteClass(_Icons.delete_icon)}" />
					</div>
				</div>
			`);

			var nameEl = $('.page-header .page-name', crawlerList);
			nameEl.children('b.name_').off('click').on('click', function (e) {
				e.stopPropagation();
				_Entities.makeNameEditable(nameEl, 200, function() {
					_Crawler.refreshTree();
				});
			});

			$('.page-header .delete', crawlerList).on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, sourcePage, false, function() {
					_Crawler.refreshTree();
					window.setTimeout(function() {
						$('.jstree-wholerow').first().click();
					}, 250);
				});
			});

			var editIcon = $('.page-header .edit-properties', crawlerList);
			editIcon.on('click', function(e) {
				e.stopPropagation();
				_Entities.showProperties(sourcePage);
			});

			_Pager.initPager('crawler-patterns', 'SourcePattern', 1, 25, 'name', 'asc');
			page['SourcePattern'] = 1;
			_Pager.initFilters('crawler-patterns', 'SourcePattern', sourcePage.id === 'root' ? {} : { sourcePage: sourcePage.id });

			if (sourcePage.isLoginPage) {

				crawlerList.append(`
					<table id="files-table" class="stripe">
						<thead>
							<tr>
								<th class="icon">&nbsp;</th>
								<th>Name</th>
								<th>Selector</th>
								<th>Value</th>
							</tr>
						</thead>
						<tbody id="files-table-body">
						</tbody>
					</table>
				`);

			} else {

				crawlerList.append(`
					<table id="files-table" class="stripe">
						<thead>
							<tr>
								<th class="icon">&nbsp;</th>
								<th>Name</th>
								<th>Selector</th>
								<th>Mapped Type</th>
								<th>Mapped Attribute</th>
								<th class="transform-head">Transformation Function</th>
								<th>Actions</th>
							</tr>
						</thead>
						<tbody id="files-table-body">
						</tbody>
					</table>
				`);

				var helpText = 'Specify optional StructrScript expression here to transform the input value. The data key is &quot;input&quot; and the return value of the expression will be imported.<br><br><b>Examples</b>:<br>parse_number(input)<br>parse_date(input, \'dd.MM.yyyy\')';
				Structr.appendInfoTextToElement({
					text: helpText,
					element: $('th.transform-head', crawlerList),
					css: {
						marginLeft: "2px"
					}
				});
			}

			_Crawler.refreshPatterns(sourcePage);

			crawlerList.append(`
				<div class="crawler-inputs"><input id="element-path" type="text" placeholder="Selector">
					<input id="element-link" type="text" placeholder="Link"><br>
					<input id="element-id" type="text" placeholder="Id">
					<input id="element-class" type="text" placeholder="Class">
				</div>
			`);

			var url = proxyUrl;

			if (sourcePage.url) {

				url += '?url=' + encodeURIComponent(sourcePage.url);

				if (sourcePage.site) {

					if (sourcePage.site.proxyUrl) {
						url += '&proxyUrl=' + encodeURIComponent(sourcePage.site.proxyUrl);
					}

					if (sourcePage.site.proxyUsername) {
						url += '&proxyUsername=' + encodeURIComponent(sourcePage.site.proxyUsername);
					}

					if (sourcePage.site.proxyPassword) {
						url += '&proxyPassword=' + encodeURIComponent(sourcePage.site.proxyPassword);
					}

					if (sourcePage.site.authUsername) {
						url += '&authUsername=' + encodeURIComponent(sourcePage.site.authUsername);
					}

					if (sourcePage.site.authPassword) {
						url += '&authPassword=' + encodeURIComponent(sourcePage.site.authPassword);
					}

					if (sourcePage.site.cookie) {
						url += '&cookie=' + encodeURIComponent(sourcePage.site.cookie);
					}
				}

//				if (_Dashboard.meObj && _Dashboard.meObj.proxyUrl) {
//					proxyUrl += '&proxyUrl=' + _Dashboard.meObj.proxyUrl;
//				}

				//console.log(url);

				crawlerList.append(`<iframe id="page-frame" name="page-frame" src="${url}" data-site-id="${sourcePage.site.id}" data-page-id="${sourcePage.id}"></iframe>`);
				_Crawler.initPageFrame(sourcePage.url);
			}

			_Crawler.resize();

		});


	},
	refreshPatterns: function(sourcePage) {

		fastRemoveAllChildren($('#files-table-body')[0]);

		$('.pager.pagercrawler-patterns').remove();

		var itemsPager = _Pager.addPager('crawler-patterns', $('.page-header', crawlerList), false, 'SourcePattern', 'ui', function(patterns) {

			if (patterns && patterns.length) {
				patterns.forEach(_Crawler.appendPatternRow);

				$('.selector').each(function(i, pattern) {
					var h = $(pattern).text().split(' > ').map(function(t) { return `<span>${t}</span>`; }).join(' > ');
					$(pattern).html(h);
				});

				frameDoc = pageFrame.contents();

				var subpatterns = $('.selector span');
				subpatterns.each(function(i, subpattern) {

					var prevSiblings = $(subpattern).prevAll().toArray().reverse();
					prevSiblings.push(this);
					var selector = prevSiblings.map(function(h) { return $(h).text(); }).join(' > ');

					var sub = $(subpattern);

					sub.on('mouseover', function() {
						_Crawler.highlight($(selector, frameDoc), 'green', selector);
						sub.addClass('active-selector-element');
					}).on('mouseout', function() {
						_Crawler.unhighlight($(selector, frameDoc));
						sub.removeClass('active-selector-element');
					}).on('click', function() {

						var m = $(this);

						var fullselector = m.closest('.selector').data('raw-value');
						var parentPatternId = m.closest('tr').prop('id').substring(3);

						_Crawler.addSubpattern(selector, fullselector, parentPatternId, false, function() {
							_Crawler.refreshPatterns(sourcePage);
						});

					});
				});

				$('.sub-selector').each(function(i, pattern) {
					var h = $(pattern).text().split(' > ').map(function(t) { return `<span>${t}</span>`; }).join(' > ');
					$(pattern).html(h);
				});

				var childPatterns = $('.sub-selector span');

				childPatterns.each(function(i, subpattern) {

					var localPrevSiblings = $(subpattern).prevAll().toArray().reverse();
					localPrevSiblings.push(this);
					var localSelector = localPrevSiblings.map(function(h) { return $(h).text(); }).join(' > ');

					//console.log('local selector:', localSelector);

					//var selector = $(subpattern).text();
					var prevSiblings = $(subpattern).closest('tr').prevAll().find('.selector').toArray().reverse();
					//prevSiblings.push(localSelector);
					var selector = prevSiblings.map(function(h) { return $(h).text(); }).join(' > ') + ' > ' + localSelector;

					var sub = $(subpattern);

					sub.on('mouseover', function() {
						_Crawler.highlight($(selector, frameDoc), 'green', selector);
						sub.addClass('active-selector-element');
					}).on('mouseout', function() {
						_Crawler.unhighlight($(selector, frameDoc));
						sub.removeClass('active-selector-element');
					});

					if (selector.substring(selector.length-1) === 'a') {
						sub.on('click', function() {

							var m = $(this);
							var fullselector = m.closest('tr').prevAll().children('.selector').first().data('raw-value');
							var parentPatternId = m.closest('tr').prevAll().children('.selector').first().closest('tr').prop('id').substring(3);
							//console.log(selector, fullselector); return;
							// addSubpattern: function(selector, fullselector, parentPatternId, reusePattern, callback) {

//							console.log(fullselector);
//							console.log(fullselector + ' > ' + selector);
//							return;

							_Crawler.addSubpattern(fullselector, fullselector + ' > ' + selector, parentPatternId, true, function() {
								_Crawler.refreshPatterns(sourcePage);
							});
						});
					}
				});


				_Crawler.resize();
			}
		});

		itemsPager.cleanupFunction = function () {
			var toRemove = $('.node.item', itemsPager.el).closest('tr');
			toRemove.each(function(i, elem) {
				fastRemoveAllChildren(elem);
			});
		};

		$('.breadcrumb-entry').click(function (e) {
			e.preventDefault();

			$('#' + $(this).data('folderId') + '_anchor').click();

		});

		$('#parent-file-link').on('click', function(e) {

			if (sourcePage.id !== '#') {
				$('#' + sourcePage.id + '_anchor').click();
			}
		});

	},
	appendPatternRow: function(d) {

		//console.log(d);

		// add container/item to global model
		StructrModel.createFromData(d, null, false);

		var tableBody = $('#files-table-body');

		$('#row' + d.id, tableBody).remove();

		var selector = d.selector || '';

		var rowId = 'row' + d.id;
		tableBody.append('<tr id="' + rowId + '"></tr>');

		d.subPatterns.forEach(function(subPattern) {

			var name = subPattern.name;
			tableBody.append('<tr id="row' + subPattern.id + '">'
				+ '<td class="file-type"><a href="javascript:void(0)"><i class="fa fa-code"></i></a></td>'
				+ '<td><div id="id_' + subPattern.id + '" data-structr_type="item" class="node item"><b title="' +  (name ? name : '[unnamed]') + '" class="name_">' + (name ? fitStringToWidth(name, 200) : '[unnamed]') + '</b></td>'
				+ '<td><div data-raw-value="' + (subPattern.selector || '') + '" class="editable sub-selector">' + (subPattern.selector || '<span class="placeholder">click to edit</span>') + '</div></td>'
				+ '<td><div title="' + (subPattern.mappedType || '') + '" class="editable mappedType_"></div></td>'
				+ '<td><div title="' + (subPattern.mappedAttribute || '') + '" class="editable mappedAttribute_"></div></td>'
				+ '<td><div title="' + (subPattern.mappedAttributeFunction || '') + '" class="editable mappedAttributeFunction_">' + (subPattern.mappedAttributeFunction || '<span class="placeholder">click to edit</span>') + '</div></td>'
//				+ '<td><div title="' + (subPattern.mappedAttributeLocale || '') + '" class="editable mappedAttributeLocale_">' + (subPattern.mappedAttributeLocale || '<span class="placeholder">click to edit</span>') + '</div></td>'
//				+ '<td><div title="' + (subPattern.mappedAttributeFormat || '') + '" class="editable mappedAttributeFormat_">' + (subPattern.mappedAttributeFormat || '<span class="placeholder">click to edit</span>') + '</div></td>'
				+ '<td><div class="subPage_"></td>'
				+ '<td></td></tr>');

			var row = $('tr#row' + subPattern.id);

			if (subPattern.subPage) {
				Command.get(subPattern.subPage.id, "name", function(subPage) {
					$('tr#row' + subPattern.id + ' .subPage_').text(subPage.name);
				});
			}

			// target selection
			//row.find('.mappedType_').append('<select class="target-type-select" name="targetType"><option value="" disabled="disabled" selected="selected">Select target type..</option></select>');
			row.find('.mappedAttribute_').append(`<select class="property-select"></select>`);

			var propertySelector   = row.find('.property-select');

			var updateTypeSelector = function(type) {

				var blacklist = [
					'id', 'owner', 'ownerId', 'base', 'type', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
					'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'visibilityStartDate', 'visibilityEndDate',
					'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog', 'internalCreationTimestamp', 'internalEntityContextPath'
				];

				propertySelector.empty();

				propertySelector.append('<option value="" disabled="disabled" selected="selected">Select target attribute..</option>');

				$.get(rootUrl + '_schema/' + type + '/all', function(typeInfo) {

					if (typeInfo && typeInfo.result) {

						// sort by name
						typeInfo.result.sort(function(a, b) {
							return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
						});

						typeInfo.result.forEach(function(r) {
							if (blacklist.indexOf(r.jsonName) === -1) {
								propertySelector.append(`<option ${subPattern.mappedAttribute === r.jsonName ? 'selected ' : ''}value="${r.jsonName}">${r.jsonName}</option>`);
							}
						});
					}

				});
			}

			updateTypeSelector(d.mappedType);

			propertySelector.on('change', function() {
				var newAttr = $(this).val();
				_Entities.setProperty(subPattern.id, 'mappedAttribute', newAttr, false, function() {
					blinkGreen(row);
				});

			});

			row.find('.mappedAttributeFunction_').on('click', function(e) {
				e.stopPropagation();
				_Entities.makeAttributeEditable(row, subPattern.id, '.mappedAttributeFunction_', 'mappedAttributeFunction', 200);
			});

			var div = Structr.node(subPattern.id);
			_Entities.appendAccessControlIcon(div, d);
			_Entities.appendEditPropertiesIcon(div, subPattern);

			var delIcon = div.children('.delete_icon');

			var newDelIcon = '<i title="Delete item ' + d.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />';
			if (delIcon && delIcon.length) {
				delIcon.replaceWith(newDelIcon);
			} else {
				div.append(newDelIcon);
			}
			div.children('.delete_icon').on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, subPattern, false, function(subPattern) {
					_Crawler.refreshPatterns(subPattern.parentPattern.sourcePage);
				});
			});

			_Entities.setMouseOver(div);
			_Entities.makeSelectable(div);

		});

		var row = $('#' + rowId);

		row.append('<td class="file-type"><a href="javascript:void(0)"><i class="fa fa-code"></i></a></td>');
		row.append('<td><div id="id_' + d.id + '" data-structr_type="item" class="node item"><b title="' +  (d.name ? d.name : '[unnamed]') + '" class="name_">' + (d.name ? fitStringToWidth(d.name, 200) : '[unnamed]') + '</b></td>');
		$('.file-type', row).on('click', function() {
			_Crawler.editItem(d);
		});

		$('.item-title b', row).on('click', function() {
			_Crawler.editItem(d);
		});

		row.append('<td data-raw-value="' + (selector || '') + '" class="selector">' + (selector || '') + '</td>'

			+ (d.sourcePage.isLoginPage ?
				  '<td><div title="' + (d.inputValue || '') + '" class="editable inputValue_">' + (d.inputValue || '<span class="placeholder">click to edit</span>') + '</div></td>'
				: '<td><div title="' + (d.mappedType || '') + '" class="editable mappedType_"></div></td>')
//			+ (d.sourcePage.isLoginPage ? '' : '<td><div title="' + (d.mappedAttribute || '') + '" class="editable mappedAttribute_"></div></td>')
			+ (d.sourcePage.isLoginPage ? '' : '<td></td>')
			+ (d.sourcePage.isLoginPage ? '' : '<td><div title="' + (d.mappedAttributeFunction || '') + '" class="editable mappedAttributeFunction_">' + (d.mappedAttributeFunction || '<span class="placeholder">click to edit</span>') + '</div></td>')
//			+ (d.sourcePage.isLoginPage ? '' : '<td><div title="' + (d.mappedAttributeLocale || '') + '" class="editable mappedAttributeLocale_">' + (d.mappedAttributeLocale || '<span class="placeholder">click to edit</span>') + '</div></td>')
//			+ (d.sourcePage.isLoginPage ? '' : '<td><div title="' + (d.mappedAttributeFormat || '') + '" class="editable mappedAttributeFormat_">' + (d.mappedAttributeFormat || '<span class="placeholder">click to edit</span>') + '</div></td>')
			+ (d.sourcePage.isLoginPage ? '<td></td>' : '<td></td><td><button class="extract">Extract</button></td>'));

		// target selection
		row.find('.mappedType_').append('<select class="target-type-select" name="targetType"><option value="" disabled="disabled" selected="selected">Select target type..</option></select>');
		row.find('.mappedAttribute_').append('<select class="property-select"></select>');

		var targetTypeSelector = row.find('.target-type-select');
		var propertySelector   = row.find('.property-select');


		$.get(rootUrl + 'SchemaNode?sort=name', function(data) {

			if (data && data.result) {
				data.result.forEach(function(r) {
					targetTypeSelector.append('<option ' + (d.mappedType === r.name ? 'selected ' : '') + 'value="' + r.name + '">' + r.name + '</option>');
				});
			}
		});

		var updatePropertySelector = function(type) {

			var blacklist = [
				'id', 'owner', 'ownerId', 'base', 'type', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
				'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'visibilityStartDate', 'visibilityEndDate',
				'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
			];

			propertySelector.empty();

			propertySelector.append('<option value="" disabled="disabled" selected="selected">Select target attribute..</option>');

			$.get(rootUrl + '_schema/' + type + '/all', function(typeInfo) {

				if (typeInfo && typeInfo.result) {

					// sort by name
					typeInfo.result.sort(function(a, b) {
						return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
					});

					typeInfo.result.forEach(function(r) {
						if (blacklist.indexOf(r.jsonName) === -1) {
							propertySelector.append('<option ' + (d.mappedAttribute === r.jsonName ? 'selected ' : '') + 'value="' + r.jsonName + '">' + r.jsonName + '</option>');
						}
					});
				}

			});
		}

		updatePropertySelector(d.mappedType);

		targetTypeSelector.on('change', function() {
			var newType = $(this).val();
			_Entities.setProperty(d.id, 'mappedType', newType, false, function() {
				blinkGreen(row);
				updatePropertySelector(newType);
			});

		});

		propertySelector.on('change', function() {
			var newAttr = $(this).val();
			_Entities.setProperty(d.id, 'mappedAttribute', newAttr, false, function() {
				blinkGreen(row);
			});

		});

		row.find('.inputValue_').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeAttributeEditable(row, d.id, '.inputValue_', 'inputValue', 200);
		});

		row.find('.mappedAttributeFunction_').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeAttributeEditable(row, d.id, '.mappedAttributeFunction_', 'mappedAttributeFunction', 200);
		});

		var div = Structr.node(d.id);

		if (!div || !div.length) {
			return;
		}

		_Entities.appendAccessControlIcon(div, d);

		var delIcon = div.children('.delete_icon');

		var newDelIcon = '<i title="Delete item ' + d.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />';
		if (delIcon && delIcon.length) {
			delIcon.replaceWith(newDelIcon);
		} else {
			div.append(newDelIcon);
			delIcon = div.children('.delete_icon');
		}
		div.children('.delete_icon').on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, d, false, function() {
				_Crawler.refreshPatterns(d.sourcePage);
			});
		});


		$('.extract', row).on('click', function(e) {
			var btn = $(this);
			var text = btn.text();
			Structr.updateButtonWithAjaxLoaderAndText(btn, text);
			e.preventDefault();

			var url = '/structr/rest/SourcePattern/' + d.id + '/extract';

			$.ajax({
				url: url,
				method: 'POST',
				statusCode: {
					200: function() {
						var btn = $('.extract', row);
						btn.removeClass('disabled').attr('disabled', null);
						btn.html(text + ' <i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />');
					},
					400: function(data) {
						Structr.error('Unable to parse data with pattern ' + d.name + ': ' + JSON.stringify(data.responseJSON), true);
						//Structr.errorFromResponse(data.responseJSON, url);
						btn.html(text);
					},
					401: function(data) {
						Structr.error('Unable to parse data with pattern ' + d.name + ': ' + data.responseJSON, true);
						btn.html(text);
					},
					403: function(data) {
						Structr.error('Unable to parse data with pattern ' + d.name + ': ' + data.responseJSON, true);
						btn.html(text);
					},
					404: function(data) {
						Structr.error('Unable to parse data with pattern ' + d.name + ': ' + data.responseJSON, true);
						btn.html(text);
					},
					422: function(data) {
						Structr.error('Unable to parse data with pattern ' + d.name + ': ' + JSON.stringify(data.responseJSON), true);
						btn.html(text);
					}
				}

			}).always(function() {
				window.setTimeout(function() {
					$('i', btn).fadeOut();
					btn.removeClass('disabled').attr('disabled', null);
				}, 1000);
			});
		});

		_Entities.appendEditPropertiesIcon(div, d);
		_Entities.setMouseOver(div);
		_Entities.makeSelectable(div);

	},
	initPageFrame: function(pageUrl) {

		pageFrame = $('#page-frame');

		var url = urlParam('url');
		if (url) {
			_Crawler.showPageInFrame(decodeURIComponent(url));
		}

		$(window).on('resize', function() {
			pageFrame.css({
				height: window.innerHeight - 280 + 'px'
			});
		});

		pageFrame.css({
			height: window.innerHeight - 280 + 'px'
		});

		pageFrame.on('load', function() {

			frameSrc = pageFrame.attr('src');
			frameDoc = pageFrame.contents();
			var realUrl;

			link = $('#element-link');
			path = $('#element-path');
			elid = $('#element-id');
			claz = $('#element-class');

			if (frameSrc.substring(0, proxyUrl.length) !== proxyUrl) {
				console.log('no proxy URL');
			} else {
				realUrl = decodeURIComponent(frameSrc.substring(frameSrc.indexOf(proxyUrl + '?url=') + 11));
			}

			var elements = frameDoc.find('*:not(html body)');

			elements.on('mouseover', function(e) {

				e.preventDefault();
				var el = $(this);

				var isLink = el.closest('a').length;

				var color = 'orange';

				if (isLink) {

					color = 'blue';
					link.val(el.closest('a').prop('href'));

				} else {

					path.val(el.getSelector());
					elid.val(el.attr('id'));
					claz.val(el.attr('class'));
				}

				_Crawler.highlight(el, color);

				return false;

			});

			elements.on('mouseout', function(e) {

				e.preventDefault();
				var el = $(this);
				el.off('click');
				_Crawler.unhighlight(el);
				return false;
			});

			elements.on('click', function(e) {

				e.preventDefault();
				e.stopPropagation();

				var el = $(this);

				var link = el.closest('a');
				var isLink = link.length;
				var path = $('#element-path');
				var elid = $('#element-id');
				var claz = $('#element-class');

				if (isLink) {
					var linkSrc = link.prop('href');
					if (linkSrc) {
						// save pattern
						_Crawler.addSourcePattern(pageFrame.data('page-id'), null, path.val(), elid.val(), claz.val(), null, function(data) {
							_Crawler.refreshPatterns(pageId);
						});
					}

				} else {

					var pageId = pageFrame.data('page-id');

					// save pattern
					_Crawler.addSourcePattern(pageId, null, path.val(), elid.val(), claz.val(), null, function() {
						_Crawler.refreshPatterns(pageId);
					});
				}

				return false;
			});
		});


		$('.add-page').on('click', function() {

			if (pageFrame.length) {

				var frameSrc = pageFrame[0].src;
				var url = decodeURIComponent(frameSrc.substring(frameSrc.indexOf(proxyUrl + '?url=') + 11));

				_Crawler.addSourcePage(url, pageFrame.contents().find('title').text().trim(), pageFrame.data('site-id'));

			} else {

				_Crawler.addSourcePage(pageUrl.val(), pageName.val(), '${current.id}');
			}
		});
	},
	addSourcePage: function(url, name, siteId) {

		$.ajax({
			type: 'POST',
			url: '/structr/rest/SourcePage',
			contentType: 'application/json; charset=UTF-8',
			data: JSON.stringify({
				url: url,
				name: name,
				site: siteId
			}),
			statusCode: {
				201: function() {
					document.location.reload();
				}
			}
		});
	},

	addSourcePattern: function(sourcePageId, name, selector, elId, elClass, parentPatternId, callback) {

		if (parentPatternId) {

			var data = {};
			if (sourcePageId)    data.sourcePage      = sourcePageId;
			if (name)            data.name            = name;
			if (selector)        data.selector        = selector;
			if (elId)            data.elId            = elId;
			if (elClass)         data.elClass         = elClass;
			data.parentPattern   = parentPatternId;

			_Crawler.createSourcePattern(data, callback);

		} else {

			// try to find an existing pattern with the same selector
			$.ajax({
				type: 'GET',
				url: '/structr/rest/SourcePattern/ui?sourcePage=' + sourcePageId,
				contentType: 'application/json; charset=UTF-8',
				statusCode: {
					200: function(data) {
						var existingPattern;
						data.result.forEach(function(pattern) {
							if (selector.indexOf(pattern.selector) > -1) {
								existingPattern = pattern;
							}
						});

						if (existingPattern) {

//							console.log('existingPattern');
//							console.log(existingPattern.selector);
//							console.log(selector);
//							return;

							_Crawler.addSubpattern(existingPattern.selector, selector, existingPattern.id, true, callback);
						} else {
							// create new pattern
							var data = {};
							if (sourcePageId)    data.sourcePage      = sourcePageId;
							if (name)            data.name            = name;
							if (selector)        data.selector        = selector;
							if (elId)            data.elId            = elId;
							if (elClass)         data.elClass         = elClass;
							if (parentPatternId) data.parentPattern   = parentPatternId;

							_Crawler.createSourcePattern(data, callback);
						}
					}
				}
			});
		}
	},
	createSourcePattern: function(data, callback) {
		$.ajax({
			type: 'POST',
			url: '/structr/rest/SourcePattern',
			contentType: 'application/json; charset=UTF-8',
			data: JSON.stringify(data),
			statusCode: {
				201: function(data) {
					if (callback) callback(data);
					return;
				}
			}
		});
	},
	addSubpattern: function(selector, fullselector, parentPatternId, reusePattern, callback) {
		var childPath = fullselector.replace(selector, '').trim();

		if (reusePattern) {
			childPath = childPath.replace(/^:\S*\s*/, '').trim();
		} else {
			selector = selector.substring(0, selector.lastIndexOf(':')).trim();
		}
		childPath = childPath.trim().replace(/^>\s*/g, '');

		_Crawler.updateSelector(parentPatternId, selector, function() {
			_Crawler.addSourcePattern(null, null, childPath, null, null, parentPatternId, function(data) {
				if (callback) callback(data);
			});
		});
	},
	updateSelector: function(patternId, selector, callback) {

		$.ajax({
			type: 'PUT',
			url: '/structr/rest/SourcePattern/' + patternId,
			contentType: 'application/json; charset=UTF-8',
			data: JSON.stringify({
				selector: selector
			}),
			statusCode: {
				200: function(data) {
					if (callback) callback();
				}
			}
		});
	},
	highlight: function(el, color, selector, url) {
		el.data('border',    el.css('border'));
		el.data('boxShadow', el.css('boxShadow'));
		el.css({
			border: '3px solid ' + color,
			boxShadow: '0px 0px 8px ' + color
		});

		if (url) {
				link.val(url);
		} else {
			path.val(selector ? selector : el.getSelector());
			elid.val(el.attr('id'));
			claz.val(el.attr('class'));
		}
	},
	unhighlight: function(el) {
		el.css({
			border:    el.data('border'),
			boxShadow: el.data('boxShadow')
		});
		el.data('border', null);
		el.data('boxShadow', null);

		link.val('');
		path.val('');
		elid.val('');
		claz.val('');
	},
	showPageInFrame: function(href) {
		pageUrl.val(href);

		var pageFrame = $('#page-frame');
		var url = proxyUrl + '?url=' + encodeURIComponent(href);
		pageFrame[0].src = url;
	}
};


/*
 *  jquery.getselector.js
 *
 *  Get the CSS Selector string for a provided jQuery object.
 *
 *  Based heavily on jquery-getpath (http://davecardwell.co.uk/javascript/jquery/plugins/jquery-getpath/),
 *  and this selector SO answer (http://stackoverflow.com/a/3454579).
 *
 *  Usage: var select = $('#foo').getSelector();
 */
(function($){
	$.fn.getSelector = function(useId) {
		element = this[0];

		// Get the selector for this element.
		// This was easier than calculating the DOM using jQuery, for now.
		var selector = '';
		for ( ; element && element.nodeType === 1; element = element.parentNode ) {
			var id = element.id;
			var c = $(element.parentNode).children(element.tagName).length;
			var i = $(element.parentNode).children().index(element) + 1;
			c > 1 ? (i = '[' + i + ']') : (i = '');

			if (useId && id) {
				selector = '/#' + id + selector;
			} else {
				selector = '/' + element.tagName.toLowerCase() + i + selector;
			}
		}

		// Return CSS selector for the calculated selector
		selector = selector.substr(1).replace(/\//g, ' > ').replace(/\[(\d+)\]/g, function($0, i) { return ':nth-child('+i+')'; });
		selector = selector.substring(selector.lastIndexOf('#'));
		return selector;
	};
})(jQuery);

