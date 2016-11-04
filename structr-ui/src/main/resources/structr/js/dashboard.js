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
var dashboard;
var aboutMe, meObj;

$(document).ready(function() {
	Structr.registerModule('dashboard', _Dashboard);
});

var _Dashboard = {
	init: function() {},
	unload: function() {},
	onload: function() {
		_Dashboard.init();
		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Dashboard');

		main.append('<div id="dashboard"></div>');
		dashboard = $('#dashboard', main);

		aboutMe = _Dashboard.appendBox('About Me', 'about-me');
		aboutMe.append('<div class="dashboard-info">You are currently logged in as <b>' + me.username + '<b>.</div>');
		aboutMe.append('<div class="dashboard-info admin red"></div>');
		aboutMe.append('<table class="props"></table>');
		$.get(rootUrl + '/me/ui', function(data) {
			meObj = data.result;
			//console.log(me);
			var t = $('table', aboutMe);
			t.append('<tr><td class="key">ID</td><td>' + meObj.id + '</td></tr>');
			t.append('<tr><td class="key">E-Mail</td><td>' + (meObj.eMail || '') + '</td></tr>');
			t.append('<tr><td class="key">Working Directory</td><td>' + (meObj.workingDirectory ? meObj.workingDirectory.name : '') + '</td></tr>');
			t.append('<tr><td class="key">Session ID(s)</td><td>' + meObj.sessionIds.join('<br>') + '</td></tr>');
			t.append('<tr><td class="key">Groups</td><td>' + meObj.groups.map(function(g) { return g.name; }).join(', ') + '</td></tr>');

		});
		_Dashboard.checkAdmin();

		aboutMe.append('<button id="clear-local-storage-on-server">Reset stored UI settings</button>');
		$('#clear-local-storage-on-server').on('click', function() {
			_Dashboard.clearLocalStorageOnServer();
		});

		var myPages = _Dashboard.appendBox('My Pages', 'my-pages');
		myPages.append('<div class="dashboard-info">You own the following <a class="internal-link" href="javascript:void(0)">pages</a>:</div>');
		Command.getByType('Page', 5, 1, 'version', 'desc', null, false, function(pages) {
			pages.forEach(function(p) {
				myPages.append('<div class="dashboard-info"><a href="/' + p.name + '" target="_blank"><img class="icon" src="' + _Icons.page_icon + '"></a> <a href="/' + p.name + '" target="_blank">' + _Dashboard.displayName(p) + '</a>' + _Dashboard.displayVersion(p) + '</div>');
			});
		});

		var myContents = _Dashboard.appendBox('My Contents', 'my-content');
		myContents.append('<div class="dashboard-info">Your most edited <a class="internal-link" href="javascript:void(0)">contents</a> are:</div>');
		Command.getByType('ContentItem', 5, 1, 'version', 'desc', null, false, function(items) {
			items.forEach(function(i) {
				myContents.append('<div class="dashboard-info"><a href="/' + i.name + '" target="_blank"><i class="fa ' + _Contents.getIcon(i) + '"></i></a> <a class="contents-link" id="open-' + i.id + '" href="javascript:void(0)">' + _Dashboard.displayName(i) + '</a>' + _Dashboard.displayVersion(i) + '</div>');
			});

			$('.contents-link', myContents).on('click', function(e) {
				e.preventDefault();
				var id = $(this).prop('id').slice(5);
				window.setTimeout(function() {
					Command.get(id, function(entity) {
						_Contents.editItem(entity);
					});
				}, 250);
				$('#contents_').click();
			});
		});

		var myFiles = _Dashboard.appendBox('My Files', 'my-files');
		myFiles.append('<div class="dashboard-info">Your most edited <a class="internal-link" href="javascript:void(0)">files</a> are:</div>');
		Command.getByType('File', 5, 1, 'version', 'desc', null, false, function(files) {
			files.forEach(function(f) {
				myFiles.append('<div class="dashboard-info"><a href="/' + f.name + '" target="_blank"><i class="fa ' + _Files.getIcon(f) + '"></i></a> <a href="/' + f.id + '" target="_blank">' + _Dashboard.displayName(f) + '</a>' + _Dashboard.displayVersion(f) + '</div>');
			});
		});

		var getImageIcon = function(file) {
			var icon = (file.contentType.startsWith('image/svg') ? file.path : (file.tnSmall ? file.tnSmall.path : _Icons.image_icon));
			return icon;
		};

		var myImages = _Dashboard.appendBox('My Images', 'my-images');
		myImages.append('<div class="dashboard-info">Your most edited <a class="internal-link" href="javascript:void(0)">images</a> are:</div>');
		Command.getByType('Image', 5, 1, 'version', 'desc', null, false, function(images) {
			images.forEach(function(i) {
				myImages.append('<div class="dashboard-info"><a href="/' + i.name + '" target="_blank"><img class="icon" src="' + getImageIcon(i) + '"></a> <a href="/' + i.id + '" target="_blank">' + _Dashboard.displayName(i) + '</a>' + _Dashboard.displayVersion(i) + '</div>');
			});
		});

		$('.dashboard-info a.internal-link').on('click', function() {
			$('#' + $(this).text() + '_').click();
		});

		$(window).off('resize');
		$(window).on('resize', function() {
			Structr.resize();
		});

		Structr.unblockMenu(100);

	},
	appendBox: function(heading, id) {
		dashboard.append('<div id="' + id + '" class="dashboard-box"><div class="dashboard-header"><h2>' + heading + '</h2></div></div>');
		return $('#' + id, main);
	},
	checkAdmin: function() {
		if (me.isAdmin && aboutMe && aboutMe.length && aboutMe.find('admin').length === 0) {
			$('.dashboard-info.admin', aboutMe).text('You have admin rights.');
		}
	},
	displayVersion: function(obj) {
		return (obj.version ? ' (v' + obj.version + ')': '');
	},
	displayName: function(obj) {
		return fitStringToWidth(obj.name, 160);
	},
	clearLocalStorageOnServer: function() {
//		console.log(meObj)
		if (!meObj) {
			Command.rest("/me/ui", function (result) {
				Command.setProperty(result[0].id, 'localStorage', null, false, function() {
					blinkGreen($('#clear-local-storage-on-server'));
					LSWrapper.clear();
				});
			});
		} else {
			Command.setProperty(meObj.id, 'localStorage', null, false, function() {
				blinkGreen($('#clear-local-storage-on-server'));
				LSWrapper.clear();
			});
		}
	}
};