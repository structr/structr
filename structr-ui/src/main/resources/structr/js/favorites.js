/*
 * Copyright (C) 2010-2016 Structr GmbH
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
	Structr.registerModule('favorites', _Favorites);
});

var _Favorites = {
	init: function() {
		/*
		Pager.initPager('users',           'User', 1, 25, 'name', 'asc');
		_Pager.initPager('groups',          'Group', 1, 25, 'name', 'asc');
		_Pager.initPager('resource-access', 'ResourceAccess', 1, 25, 'signature', 'asc');
		*/
	},
	onload: function() {
		_Favorites.init();

		Structr.updateMainHelpLink('http://docs.structr.org/frontend-user-guide#Users and Groups');
		_Logger.log(_LogType.FAVORITES, 'onload');

		main.append('<div id="favoriteTabs"><ul id="favoriteTabsMenu"></ul></div>');

		var container = $('#favoriteTabs');
		var menu      = $('#favoriteTabsMenu');

		Command.query('Favoritable', 10, 1, 'name', 'asc', {}, function(favorites) {

			favorites.forEach(function(favorite) {

				var id   = favorite.id;
				var name = favorite.name || favorite.type + ' ' + favorite.id;

				menu.append('<li><a id="tab-' + id + '_" href="#content-' + id + '"><span>' + name + '</span></a></li>');
				container.append('<div id="content-' + id + '"></div>');

				editor = CodeMirror($('#content-' + id).get(0), {
					value: favorite.content || favorite.source || 'Error, no content found',
					mode: 'text/plain',
					lineNumbers: true,
					lineWrapping: false,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true
				});
			});

			$('#favoriteTabs').tabs({
				active: (activeTab === 'usersAndGroups' ? 0 : 1),
				activate: function(event, ui) {
					_Favorites.selectTab(ui.newPanel[0].id);
				}
			});

			Structr.unblockMenu(100);

		});
	},
	selectTab: function (tab) {
	}
};