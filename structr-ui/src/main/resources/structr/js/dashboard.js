/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

var win = $(window);
var engine;
var nodeIds = {};
var relIds = {};

$(document).ready(function() {
	Structr.registerModule('dashboard', _Dashboard);
});

var _Dashboard = {
	icon: 'icon/page.png',
	add_icon: 'icon/page_add.png',
	delete_icon: 'icon/page_delete.png',
	clone_icon: 'icon/page_copy.png',
	init: function() {
	},
	onload: function() {
		_Dashboard.init();
		activeTab = $.cookie('structrActiveTab');
		main.append('<div class="searchBox"><input class="search" name="search" size="20" placeholder="Search"><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');
		main.append('<div class="canvas" id="graph"></div>');
		searchField = $('.search', main);
		searchField.focus();
		searchField.keyup(function(e) {
			var rawSearchString = $(this).val();
			var searchString = rawSearchString;

			var type;
			var posOfColon = rawSearchString.indexOf(':');
			if (posOfColon > -1) {
				type = rawSearchString.substring(0, posOfColon);
				type = type.capitalize();
				searchString = rawSearchString.substring(posOfColon + 1, rawSearchString.length);
			}
			if (searchString && searchString.length && e.which === 13) {

				$('.clearSearchIcon').show().on('click', function() {
					_Crud.clearSearch(main);
				});
				Command.search(searchString, type);

			} else if (e.which === 27 || rawSearchString === '') {
				_Dashboard.clearSearch();
			}
		});

		engine = new Engine($('#graph'));
		engine.initialize();
		engine.update();

	},
	
	appendNode: function(node, depth) {

		if (node) {

			if (nodeIds[node.id] === undefined) {

				nodeIds[node.id] = 1;

				engine.addNode(node);

				window.setTimeout(function() {

					_Dashboard.loadRelationships(node.id, depth === undefined ? 1 : depth+1);

				}, 100);
			}

			// start update loop
			engine.update();
		}
	},
		
	loadRelationships: function(nodeId, depth) {

		if (nodeId) {

			$.ajax({
				url: rootUrl + nodeId + '/out',
				dataType: "json",
				success: function(data) {

					if (!data || data.length === 0 || !data.result || !data.result.length) {
						return;
					}

					var results = data.result;
					var count = 0, i = 0;

					while (i < results.length && count < maxRels) {

						var r = results[i++];

						if (relIds[r.id] === undefined) {

							relIds[r.id] = 1;

							engine.addRelationship(r.relType, r.sourceId, r.targetId);
							_Dashboard.loadNode(r.targetId, depth === undefined ? 1 : depth+1);
						}
					}
				}
			});

			$.ajax({
				url: rootUrl + nodeId + '/in',
				dataType: "json",
				success: function(data) {

					if (!data || data.length === 0 || !data.result || !data.result.length) {
						return;
					}

					var results = data.result;
					var count = 0, i = 0;

					while (i < results.length && count < maxRels) {

						var r = results[i++];

						if (relIds[r.id] === undefined) {

							relIds[r.id] = 1;

							engine.addRelationship(r.relType, r.sourceId, r.targetId);
							_Dashboard.loadNode(r.sourceId, depth === undefined ? 1 : depth+1);
						}
					}
				}
			});
		}
	},
	loadNode: function(nodeId, depth) {

		if (nodeId) {

			$.ajax({
				url: rootUrl + nodeId + '/ui',
				dataType: "json",
				success: function(data) {

					if (!data || data.length === 0 || !data.result) {
						return;
					}

					_Dashboard.appendNode(data.result, depth);

				}
			});
		}
	},
	clearSearch: function() {
		_Dashboard.clearSearchResults();
		$('.clearSearchIcon').hide().off('click');
		$('.search').val('');
	},
	clearSearchResults: function() {
		//canvas.element.empty();
	}
};
