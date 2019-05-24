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
$(document).ready(function() {
	Structr.registerModule(_Dashboard);
});

var _Dashboard = {
	_moduleName: 'dashboard',
	dashboard: undefined,
	logInterval: undefined,

	init: function() {},
	unload: function() {
		window.clearInterval(_Dashboard.logInterval);
	},
	removeActiveClass: function(nodelist) {
		nodelist.forEach(function(el) {
			el.classList.remove('active');
		});
	},
	onload: function() {

		_Dashboard.init();
		Structr.updateMainHelpLink('https://support.structr.com/article/202');

		let templateConfig = {};

		fetch(rootUrl + '/_env').then(function(response) {

			return response.json();

		}).then(function(data) {

			templateConfig.envInfo = data.result;

			if (templateConfig.envInfo.startDate) {
				templateConfig.envInfo.startDate = _Dashboard.dateToIsoString(templateConfig.envInfo.startDate);
			}

			if (templateConfig.envInfo.endDate) {
				templateConfig.envInfo.endDate = _Dashboard.dateToIsoString(templateConfig.envInfo.endDate);
			}

			return fetch(rootUrl + '/me/ui');

		}).then(function(response) {

			return response.json();

		}).then(function(data) {

			templateConfig.meObj = data.result;

		}).then(function() {

			Structr.fetchHtmlTemplate('dashboard/dashboard', templateConfig, function(html) {

				main.append(html);

				document.querySelectorAll('#dashboard .tabs-menu li a').forEach(function(tabLink) {
					tabLink.addEventListener('click', function(e) {
						e.preventDefault();
						let targetId = e.target.getAttribute('href');

						_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-contents .tab-content'));
						_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-menu li'));

						e.target.closest('li').classList.add('active');
						document.querySelector(targetId).classList.add('active');
					});
				});

				document.querySelector('#clear-local-storage-on-server').addEventListener('click', function() {
					_Dashboard.clearLocalStorageOnServer(templateConfig.meObj.id);
				});

				_Dashboard.checkLicenseEnd(templateConfig.envInfo, $('#dash-about-structr .end-date'));

				$('button#do-import').on('click', function() {
					var location = $('#deployment-source-input').val();
					if (location && location.length) {
						_Dashboard.deploy('import', location);
					} else {
						// show error message
					}
				});

				$('button#do-export').on('click', function() {
					var location = $('#deployment-target-input').val();
					if (location && location.length) {
						_Dashboard.deploy('export', location);
					} else {
						// show error message
					}
				});

				_Dashboard.activateLogBox();

				_Dashboard.appendGlobalSchemaMethods($('#dash-global-schema-methods'));

				$(window).off('resize');
				$(window).on('resize', function() {
					Structr.resize();
				});

				Structr.unblockMenu(100);
			});
		});
	},
	dateToIsoString: function(dateString) {
		let date = new Date(dateString);
		return date.getFullYear() + '-' + ('' + (date.getMonth() + 1)).padStart(2, '0') + '-' + ('' + date.getDate()).padStart(2, '0');
	},
	displayVersion: function(obj) {
		return (obj.version ? ' (v' + obj.version + ')': '');
	},
	displayName: function(obj) {
		return fitStringToWidth(obj.name, 160);
	},
	clearLocalStorageOnServer: function(userId) {

		Command.setProperty(userId, 'localStorage', null, false, function() {
			blinkGreen($('#clear-local-storage-on-server'));
			LSWrapper.clear();
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
	appendGlobalSchemaMethods: function (container) {

		var maintenanceList = $('<div></div>').appendTo(container);

		$.get(rootUrl + '/SchemaMethod?schemaNode=&sort=name', function(data) {

			data.result.forEach(function(result) {

				var methodRow = $('<div class="global-method" style=""></div>');
				var methodName = $('<span>' + result.name + '</span>');

				methodRow.append('<button id="run-' + result.id + '" class="action button">Run now</button>').append(methodName);
				maintenanceList.append(methodRow);

				var cleanedComment = (result.comment && result.comment.trim() !== '') ? result.comment.replaceAll("\n", "<br>") : '';

				if (cleanedComment.trim() !== '') {
					Structr.appendInfoTextToElement({
						element: methodName,
						text: cleanedComment,
						helpElementCss: {
							"line-height": "initial"
						}
					});
				}

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

					if (cleanedComment.trim() !== '') {
						dialog.append('<div id="global-method-comment"><h3 class="heading-narrow">Comment</h3>' + cleanedComment + '</div>');
					}

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
	},
    activateLogBox: function () {

		let logBoxContentBox = $('#dash-server-log textarea');

        let scrollEnabled = true;

        let updateLog = function() {
            Command.getServerLogSnapshot(300, (a) => {
                logBoxContentBox.text(a[0].result);
                if (scrollEnabled) {
                    logBoxContentBox.scrollTop(logBoxContentBox[0].scrollHeight);
                }
            });
		};

        let registerEventHandlers = function() {
            _Dashboard.logInterval = window.setInterval(() => updateLog(), 1000);

            logBoxContentBox.bind('scroll', (event) => {
            	let textarea = event.target;

            	let maxScroll = textarea.scrollHeight - 4;
            	let currentScroll = (textarea.scrollTop+$(textarea).height());

            	if (currentScroll >= maxScroll) {
                    scrollEnabled = true;
				} else {
                    scrollEnabled = false;
				}
            });
		};

        registerEventHandlers();
        updateLog();

    },
	deploy: function(mode, location) {

		var data = {
			mode: mode
		};

		if (mode === 'import') {
			data['source'] = location;
		} else if (mode === 'export') {
			data['target'] = location;
		}

		$.ajax({
			url: rootUrl + '/maintenance/deploy',
			data: JSON.stringify(data),
			method: 'POST'
		});

	}
};