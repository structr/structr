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
	_Favorites.resize();
});

var _Favorites = {
	container: undefined,
	menu: undefined,
	favoritesTabKey: 'structrFavoritesTab_' + port,
	init: function() {
	},
	onload: function() {
		_Favorites.init();

		Structr.updateMainHelpLink('http://docs.structr.org/frontend-user-guide#Users and Groups');
		_Logger.log(_LogType.FAVORITES, 'onload');

		main.append('<div id="favoriteTabs"><ul id="favoriteTabsMenu"></ul><div id="favoriteContainer">Loading favorites..</div></div>');

		_Favorites.container = $('#favoriteContainer');
		_Favorites.menu      = $('#favoriteTabsMenu');

		$.ajax({
			url: rootUrl + 'me/favorites/fav',
			statusCode: {
				200: function(data) {

					_Favorites.container.empty();

					if (data && data.result && data.result.length) {

						var favorites = data.result;

						favorites.forEach(function(favorite) {

							var id   = favorite.id;

							_Favorites.menu.append(
								'<li id="tab-' + id + '"><a id="tab-' + id + '_" href="#content-' + id + '"><span>' + favorite.favoriteContext + '</span>' +
								'<i title="Close" id="button-close-' + id + '" class="' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" >' +
								'</a></li>'
							);

							_Favorites.container.append(
								'<div class="favoriteContent" id="content-' + id + '" style="padding:0px;">' +
									//'<div class="favoriteMenu" id="menu-' + id + '"><b class="favoriteContext">' + favorite.favoriteContext + '</b></div>' +
									'<div class="favoriteEditor" id="editor-' + id + '"></div>' +
									'<div class="favoriteButtons" id="buttons-' + id + '"></div>' +
								'</div>'
							);

							var editor = CodeMirror($('#editor-' + id).get(0), {
								value: favorite.favoriteContent || '',
								mode: favorite.favoriteContentType || 'text/plain',
								autoFocus: true,
								lineNumbers: true,
								lineWrapping: false,
								indentUnit: 4,
								tabSize:4,
								indentWithTabs: true,
								extraKeys: {
									"Ctrl-S": function() {
										$('#button-save-' + id).click();
									},
									"Ctrl-W": function() {
										$('#button-close-' + id).click();
									}
								}
							});

							$('#buttons-' + id).append('<button class="pull-right" id="button-save-' + id +'">Save</button>');
							$('#buttons-' + id).append('<div id="info-' + id +'"></div>');
							$('#buttons-' + id).append('<div id="info-' + id +'" style="clear:both;"></div>');

							// save button
							$('#button-save-' + id).on('click', function() {
								Command.setProperty(id, 'favoriteContent', editor.getValue(), false, function() {
									$('#info-' + id).empty();
									$('#info-' + id).append('<div class="success" id="info">Saved</div>');
									$('#info').delay(1000).fadeOut(1000)
								});
							});

							// prevent DELETE ajax call without relationship ID
							if (favorite.relationshipId && favorite.relationshipId.length === 32) {

								// close button
								$('#button-close-' + id).on('click', function() {
									$.ajax({
										url: rootUrl + '/' + favorite.relationshipId,
										type: 'DELETE',
										statusCode: {
											200: function() {
												$('#tab-' + id).prev().find('a').click();
												$('#tab-' + id).next().find('a').click();
												$('#tab-' + id).remove();
												$('#content-' + id).remove();
											}
										}
									});
									return false;
								});
							}
						});

					} else {

						_Favorites.container.append(' No favorites found');
					}

					var activeTab = LSWrapper.getItem(_Favorites.favoritesTabKey);
					_Favorites.selectTab(activeTab);

					$('#favoriteTabs').tabs({
						activate: function(event, ui) {
							_Favorites.selectTab(ui.newPanel[0].id);
						}
					});

					$('a[href=#' + activeTab + ']').click();

					Structr.unblockMenu(100);

				}
			}
		});
	},
	resize: function() {
		Structr.resize();
	},
	selectTab: function (tab) {
		if (tab && tab.length) {
			LSWrapper.setItem(_Favorites.favoritesTabKey, tab);
			var id = tab.substring(8);
			var el = $('#editor-' + id).find('.CodeMirror');
			if (el) {
				var e = el.get(0);
				if (e && e.CodeMirror) {
					e.CodeMirror.focus();
				}
			}
		}
	}
};