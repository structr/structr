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
	Structr.registerModule(_Dashboard);
});

var _Dashboard = {
	_moduleName: 'dashboard',
	dashboard: undefined,
	aboutMe: undefined,
	meObj: undefined,

	init: function() {},
	unload: function() {},
	onload: function() {
		_Dashboard.init();
		Structr.updateMainHelpLink('https://support.structr.com/article/202');

		main.append('<div id="dashboard"></div>');
		_Dashboard.dashboard = $('#dashboard', main);

		_Dashboard.aboutMe = _Dashboard.appendBox('About Me', 'about-me');
		_Dashboard.aboutMe.append('<div class="dashboard-info">You are currently logged in as <b>' + me.username + '<b>.</div>');
		_Dashboard.aboutMe.append('<div class="dashboard-info admin red"></div>');
		_Dashboard.aboutMe.append('<table class="props"></table>');

		$.get(rootUrl + '/me/ui', function(data) {
			_Dashboard.meObj = data.result;
			var t = $('table', _Dashboard.aboutMe);
			t.append('<tr><td class="key">ID</td><td>' + _Dashboard.meObj.id + '</td></tr>');
			t.append('<tr><td class="key">E-Mail</td><td>' + (_Dashboard.meObj.eMail || '') + '</td></tr>');
			t.append('<tr><td class="key">Working Directory</td><td>' + (_Dashboard.meObj.workingDirectory ? _Dashboard.meObj.workingDirectory.name : '') + '</td></tr>');
			t.append('<tr><td class="key">Session ID(s)</td><td>' + _Dashboard.meObj.sessionIds.join('<br>') + '</td></tr>');
			t.append('<tr><td class="key">Groups</td><td>' + _Dashboard.meObj.groups.map(function(g) { return g.name; }).join(', ') + '</td></tr>');
		});
		_Dashboard.checkAdmin();

		_Dashboard.aboutMe.append('<button id="clear-local-storage-on-server">Reset stored UI settings</button>');
		$('#clear-local-storage-on-server').on('click', function() {
			_Dashboard.clearLocalStorageOnServer();
		});

		_Dashboard.appendAboutBox();

		_Dashboard.appendMaintenanceBox();

		/*
		var myPages = _Dashboard.appendBox('My Pages', 'my-pages');
		myPages.append('<div class="dashboard-info">You own the following <a class="internal-link" href="javascript:void(0)">pages</a>:</div>');
		Command.getByType('Page', 5, 1, 'version', 'desc', null, false, function(pages) {
			pages.forEach(function(p) {
				myPages.append('<div class="dashboard-info"><a href="/' + p.name + '" target="_blank"><i class="icon sprite sprite-page" /></a> <a href="/' + p.name + '" target="_blank">' + _Dashboard.displayName(p) + '</a>' + _Dashboard.displayVersion(p) + '</div>');
			});
		});

		var myContents = _Dashboard.appendBox('My Contents', 'my-content');
		myContents.append('<div class="dashboard-info">Your most edited <a class="internal-link" href="javascript:void(0)">contents</a> are:</div>');
		Command.getByType('ContentItem', 5, 1, 'version', 'desc', null, false, function(items) {
			items.forEach(function(i) {
				myContents.append('<div class="dashboard-info"><a href="/' + i.name + '" target="_blank"><i class="fa ' + _Contents.getIcon(i) + '" /></a> <a class="contents-link" id="open-' + i.id + '" href="javascript:void(0)">' + _Dashboard.displayName(i) + '</a>' + _Dashboard.displayVersion(i) + '</div>');
			});

			$('.contents-link', myContents).on('click', function(e) {
				e.preventDefault();
				var id = $(this).prop('id').slice(5);
				window.setTimeout(function() {
					Command.get(id, "id,name", function(entity) {
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
				myFiles.append('<div class="dashboard-info"><a href="/' + f.name + '" target="_blank"><i class="fa ' + _Icons.getFileIconClass(f) + '" /></a> <a href="/' + f.id + '" target="_blank">' + _Dashboard.displayName(f) + '</a>' + _Dashboard.displayVersion(f) + '</div>');
			});
		});

		var myImages = _Dashboard.appendBox('My Images', 'my-images');
		myImages.append('<div class="dashboard-info">Your most edited <a class="internal-link" href="javascript:void(0)">images</a> are:</div>');
		Command.getByType('Image', 5, 1, 'version', 'desc', null, false, function(images) {
			images.forEach(function(i) {
				myImages.append('<div class="dashboard-info"><a href="/' + i.name + '" target="_blank">' + _Icons.getImageOrIcon(i) + '</a> <a href="/' + i.id + '" target="_blank">' + _Dashboard.displayName(i) + '</a>' + _Dashboard.displayVersion(i) + '</div>');
			});
		});
		*/

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
		_Dashboard.dashboard.append('<div id="' + id + '" class="dashboard-box"><div class="dashboard-header"><h2>' + heading + '</h2></div></div>');
		return $('#' + id, main);
	},
	checkAdmin: function() {
		if (me.isAdmin && _Dashboard.aboutMe && _Dashboard.aboutMe.length && _Dashboard.aboutMe.find('admin').length === 0) {
			$('.dashboard-info.admin', _Dashboard.aboutMe).text('You have admin rights.');
		}
	},
	displayVersion: function(obj) {
		return (obj.version ? ' (v' + obj.version + ')': '');
	},
	displayName: function(obj) {
		return fitStringToWidth(obj.name, 160);
	},
	clearLocalStorageOnServer: function() {

		var clear = function (userId) {
			Command.setProperty(userId, 'localStorage', null, false, function() {
				blinkGreen($('#clear-local-storage-on-server'));
				LSWrapper.clear();
			});
		};

		if (!_Dashboard.meObj) {
			Command.rest("/me/ui", function (result) {
				clear(result[0].id);
			});
		} else {
			clear(_Dashboard.meObj.id);
		}
	},
	appendAboutBox: function () {

		var aboutStructrBox = _Dashboard.appendBox('About Structr', 'about-structr');
		var aboutStructrTable = $('<table class="props"></table>').appendTo(aboutStructrBox);

		$.get(rootUrl + '/_env', function(data) {
			var envInfo = data.result;

			if (envInfo.edition) {
				var tooltipText = 'Structr ' + envInfo.edition + ' Edition';
				var versionInfo = '<i title="' + tooltipText + '" class="' + _Icons.getFullSpriteClass(_Icons.getIconForEdition(envInfo.edition)) + '"></i> (' + tooltipText + ')';

				aboutStructrTable.append('<tr><td class="key">Edition</td><td>' + versionInfo + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">Licensee</td><td>' + (envInfo.licensee || 'Unlicensed') + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">Host ID</td><td>' + (envInfo.hostId || '') + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">License Start Date</td><td>' + (envInfo.startDate || '-') + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">License End Date</td><td class="end-date">' + (envInfo.endDate || '-') + '</td></tr>');

				_Dashboard.checkLicenseEnd(envInfo, $('.end-date', aboutStructrTable));
			}
		});

	},
	checkLicenseEnd:function (envInfo, element, cfg) {

		if (envInfo && envInfo.endDate && element) {

			var showMessage = true;
			var daysLeft = Math.ceil((new Date(envInfo.endDate) - new Date()) / 86400000) + 1;

			var config = {
				element: element,
				appendToElement: element
			};
			config = $.extend(config, cfg);

			if (daysLeft <= 0) {

				config.customToggleIcon = _Icons.exclamation_icon;
				config.text = "Your Structr <b>license has expired</b>. Upon restart the Community edition will be loaded.";

			} else if (daysLeft <= 7) {

				config.customToggleIcon = _Icons.error_icon;
				config.text = "Your Structr <b>license will expire in less than a week</b>. After that the Community edition will be loaded.";

			} else {
				showMessage = false;
			}

			if (showMessage) {

				config.text += " Please get in touch via <b>licensing@structr.com</b> to renew your license.";
				Structr.appendInfoTextToElement(config);
			}

		}
	},
	appendMaintenanceBox: function () {

		var maintenanceBox  = _Dashboard.appendBox('Global schema methods', 'maintenance');
		var maintenanceList = $('<div></div>').appendTo(maintenanceBox);

		$.get(rootUrl + '/SchemaMethod?schemaNode=&sort=name', function(data) {

			data.result.forEach(function(result) {

				maintenanceList.append('<div class="dashboard-info" style="border-bottom: 1px solid #ddd; padding-bottom: 2px;"><span style="line-height: 2em;">' + result.name + '</span><button id="run-' + result.id + '" class="pull-right" style="margin-left: 1em;">Run now</button></div>');
				$('button#run-' + result.id).on('click', function() {

					Structr.dialog('Run global schema method ' + result.name, function() {}, function() {
						$('#run-method').remove();
						$('#clear-log').remove();
					});

					dialogBtn.prepend('<button id="run-method">Run</button>');
					dialogBtn.append('<button id="clear-log">Clear output</button>');

					var paramsOuterBox = $('<div id="params"><h3 class="heading-narrow">Parameters</h3></div>');
					var paramsBox = $('<div></div>');
					paramsOuterBox.append(paramsBox);
					var addParamBtn = $('<i title="Add parameter" class="button ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');
					paramsBox.append(addParamBtn);
					dialog.append(paramsOuterBox);

					Structr.appendInfoTextToElement({
						element: $('#params h3'),
						text: "Parameters can be accessed by using the <code>retrieve()</code> function.",
						css: { marginLeft: "5px" },
						helpElementCss: { fontSize: "12px" }
					});

					addParamBtn.on('click', function() {
						var newParam = $('<div class="param"><input class="param-name" placeholder="Parameter name"> : <input class="param-value" placeholder="Parameter value"></div>');
						var removeParam = $('<i class="button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" alt="Remove parameter" title="Remove parameter"/>');

						newParam.append(removeParam);
						removeParam.on('click', function() {
							newParam.remove();
						});
						paramsBox.append(newParam);
					});

					dialog.append('<h3>Method output</h3>');
					dialog.append('<pre id="log-output"></pre>');

					$('#run-method').on('click', function() {

						$('#log-output').empty();
						$('#log-output').append('Running method..\n');

						var params = {};
						$('#params .param').each(function (index, el) {
							var name = $('.param-name', el).val();
							var val = $('.param-value', el).val();
							if (name) {
								params[name] = val;
							}
						});

						$.ajax({
							url: rootUrl + '/maintenance/globalSchemaMethods/' + result.name,
							data: JSON.stringify(params),
							method: 'POST',
							complete: function(data) {
								$('#log-output').append(data.responseText);
								$('#log-output').append('Done.');
							}
						});
					});

					$('#clear-log').on('click', function() {
						$('#log-output').empty();
					});
				});
			});
		});

	}
};