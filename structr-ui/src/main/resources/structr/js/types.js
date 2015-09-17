/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

var propertyDefinitions, dynamicTypes = [];
var typesTypeKey = 'structrTypesType_' + port;
var hiddenTypesTabs = [];
var typesHiddenTabsKey = 'structrTypesHiddenTabs_' + port;

$(document).ready(function() {
	Structr.registerModule('types', _Types);
	Structr.classes.push('types');

	if (!_Types.type) {
		_Types.restoreType();
	}

	if (!_Types.type) {
		_Types.type = urlParam('type');
	}

	if (!_Types.type) {
		_Types.type = defaultType;
	}

});

var _Types = {
	type_icon: 'icon/database_table.png',
	schemaLoading: false,
	schemaLoaded: false,
	//allTypes : [],

	types: [], //'Page', 'User', 'Group', 'Folder', 'File', 'Image', 'Content' ],
	badTypes: [],
	views: ['public', 'all', 'ui'],
	schema: [],
	keys: [],
	type: null,
	pageCount: null,
	view: [],
	sort: [],
	order: [],
	page: [],
	pageSize: [],
	init: function() {

		main.append('<div id="resourceTabs">'
			+ '<div id="resourceTabsSettings"></div>'
			+ '<ul id="resourceTabsMenu"><li class="last hidden">'
			+ '<input type="checkbox" id="resourceTabsAutoHideCheckbox"> <label for="resourceTabsAutoHideCheckbox">show selected tabs only</label>'
			+ '</li></ul></div>');

		if (Structr.getAutoHideInactiveTabs()) {
			$('#resourceTabsAutoHideCheckbox').prop('checked', true);
		}

		$('#resourceTabsAutoHideCheckbox').change(function () {
			var checked = $(this).prop('checked');
			Structr.setAutoHideInactiveTabs(checked);
			Structr.setHideInactiveTabs(checked);
			//location.reload();
		});

		Structr.ensureIsAdmin($('#resourceTabs'), function() {

			_Types.schemaLoading = false;
			_Types.schemaLoaded = false;
			_Types.schema = [];
			_Types.keys = [];

			_Types.loadSchema(function() {
				_Types.initTabs();
				_Types.resize();
				Structr.unblockMenu(500);
				_Types.updateUrl(_Types.type);
			});

		});


	},
	onload: function() {

		_Types.init();

		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Types');

		$(window).off('resize');
		$(window).on('resize', function() {
			_Types.resize();
		});

	},
	initTabs: function() {

		$.each(_Types.types, function(t, type) {
			if (_Types.badTypes.indexOf(type) === -1) {
				_Types.addTab(type);
			}
		});

		$('#resourceTabsMenu input[type="checkbox"]').on('click', function(e) {
			e.stopPropagation();
			//e.preventDefault();
			
			var inp = $(this);

			var key = inp.parent().find('span').text();
			
			if (!inp.is(':checked')) {
				hiddenTypesTabs.push(key);
			} else {
				if (hiddenTypesTabs.indexOf(key) > -1) {
					hiddenTypesTabs.splice(hiddenTypesTabs.indexOf(key), 1);
				}
			}
			
			 LSWrapper.setItem(typesHiddenTabsKey, JSON.stringify(hiddenTypesTabs));

		});

		$('#resourceTabs').tabs({
			activate: function(event, ui) {
				//_Types.clearList(_Types.type);
				var newType = ui.newPanel[0].id;
				//console.log('deactivated', _Types.type, 'activated', newType);
				_Types.type = newType;
				_Types.updateUrl(newType);
			}
		});

		var t = $('a[href="#' + _Types.type + '"]');
		t.click();

		_Types.resize();


	},
	/**
	 * Read the schema from the _schema REST resource and call 'callback'
	 * after the complete schema is loaded.
	 */
	loadSchema: function(callback) {
		// Avoid duplicate loading of schema
		if (_Types.schemaLoading) {
			return;
		}
		_Types.schemaLoading = true;
		_Types.loadAccessibleResources(function() {
			_Types.badTypes = [];

			_Types.types.forEach(function(type) {
//				console.log('Loading type definition for ' + type + '...');
				if (type.startsWith('_')) {
					return;
				}
				_Types.loadTypeDefinition(type, callback);
			});
		});

	},
	isSchemaLoaded: function() {
		var all = true;
		if (!_Types.schemaLoaded) {
			//console.log('schema not loaded completely, checking all types ...');
			$.each(_Types.types, function(t, type) {
				// console.log('checking type ' + type, (_Types.schema[type] !== undefined && _Types.schema[type] !== null));
				all = all && ((_Types.badTypes.indexOf(type) !== -1) || (_Types.schema[type] !== undefined && _Types.schema[type] !== null));
			});
		}
		_Types.schemaLoaded = all;
		return _Types.schemaLoaded;
	},
	loadAccessibleResources: function(callback) {
		var url = rootUrl + 'resource_access/ui';
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				var types = [];
				_Types.types.length = 0;
				$.each(data.result, function(i, res) {
					var type = getTypeFromResourceSignature(res.signature);
					//console.log(res);
					if (type && !(type.startsWith('_')) && !isIn(type, _Types.types)) {
						_Types.types.push(type);
						types.push({'type': type, 'position': res.position});
					}
				});
				//console.log(types);
				types.sort(function(a, b) {
					return a.position - b.position;
				});

				_Types.types.length = 0; // best way to empty array

				$.each(types, function(i, typeObj) {
					_Types.types.push(typeObj.type);
				});


				_Types.types.sort();

				if (callback) {
					callback();
				}

			}
		});
	},
	loadTypeDefinition: function(type, callback) {

		//_Types.schema[type] = [];
		//console.log(type);
		var url = rootUrl + '_schema/' + type;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				200: function(data) {

					// no schema entry found?
					if (data.result_count === 0){

						// console.log("ERROR: loading Schema " + type);
						new MessageBuilder().warning("Failed loading Schema for '" + type + "' - check your resource access grants.").show();

						// NOT removing the type from the _Types.types array as that would lead to skipped types
						_Types.badTypes.push(type);

						if (_Types.isSchemaLoaded()) {
							//console.log('Schema loaded successfully');
							if (callback) {
								callback();
							}
						}
					} else {

						$.each(data.result, function(i, res) {
							//console.log(res);

							_Types.schema[type] = res;
							_Types.view[type] = 'all';
							//console.log('Type definition for ' + type + ' loaded');
							//console.log('schema loaded?', _Types.isSchemaLoaded());

							if (_Types.isSchemaLoaded()) {
								//console.log('Schema loaded successfully');
								if (callback) {
									callback();
								}
							}

						});
					}

				},
				400: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				401: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				403: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				404: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				422: function(data) {
					Structr.errorFromResponse(data.responseJSON);
				}
			},
			error:function () {
				// NOT removing the type from the _Types.types array as that would lead to skipped types
				_Types.badTypes.push(type);
			}

		});
	},
	addTab: function(type) {
		var res = _Types.schema[type];
		var hidden = hiddenTypesTabs.indexOf(type) > -1;
		$('#resourceTabsMenu li.last').before('<li' + (hidden ? ' class="hidden"' : '') + '><a href="#' + type + '"><span>' + _Crud.formatKey(type) + '</span><input type="checkbox"' + (!hidden ? ' checked="checked"' : '') + '></a></li>');
		$('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
		var typeNode = $('#' + type);
		typeNode.append('<div>Type: ' + res.type + '</div>');
		typeNode.append('<div>URL: <a target="_blank" href="' + rootUrl + res.url.substring(1) + '">' + res.url + '</a></div>');
		typeNode.append('<table><thead><tr>\n\
<th>Key</th>\n\
<th>JSON name</th>\n\
<th>DB name</th>\n\
<th>Data Type</th>\n\
<th>Related Type</th>\n\
<th>Input Converter</th>\n\
<th>Collection</th>\n\
<th>Read Only</th>\n\
<th>System Property</th>\n\
<th>Default Value</th>\n\
<th>Declaring Class</th>\n\
</tr></thead><tbody></tbody></table>');
		var tb = $('tbody', typeNode);
		var view = res.views[_Types.view[type]];
		if (view) {
			var k = Object.keys(view);
			if (k && k.length) {
				_Types.keys[type] = k;

				$.each(_Types.keys[type], function(k, key) {

					var raw = view[key].className, cls;

					cls = raw.substring(raw.lastIndexOf('.') + 1);
					cls = cls.replace('Property', '');

					tb.append('<tr class="' + key + '">\n\
<td class="key">' + key + '</td>\n\
<td>' + view[key].jsonName + '</td>\n\
<td>' + view[key].dbName + '</td>\n\
<td class="dataType" title="' + raw + '">' + cls + '</td>\n\
<td>' + nvl(view[key].relatedType, '') + '</td>\n\
<td>' + nvl(view[key].inputConverter, '') + '</td>\n\
<td>' + (view[key].isCollection ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td class="readOnly">' + (view[key].readOnly ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td class="system">' + (view[key].system ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td class="defaultValue">' + nvl(view[key].defaultValue, '') + '</td>\n\
<td>' + nvl(view[key].declaringClass, '') + '</td>\n\
</tr>');
				});
			}
		}

		_Types.resize();
		
		$('#resourceTabsMenu li.last').removeClass('hidden');

	},
	addGrants: function(type) {
		Command.create({type: 'ResourceAccess', flags: 255, signature: type, visibleToPublicUsers: true});
		Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_Ui'});
		Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_Public'});
		Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_All'});
		Command.create({type: 'ResourceAccess', flags: 255, signature: '_schema/' + type});
	},
	updateUrl: function(type) {
		if (type) {
			_Types.storeType();
			window.location.hash = '#types';

			if (Structr.getAutoHideInactiveTabs() || Structr.getHideInactiveTabs()) {
				Structr.doHideInactiveTabs();
			}
		}
		//searchField.focus();
	},
	storeType: function() {
		LSWrapper.setItem(typesTypeKey, _Types.type);
	},
	restoreType: function() {
		var val = LSWrapper.getItem(typesTypeKey);
		if (val) {
			_Types.type = val;
		}
	},
	resize: function() {

		Structr.resize();

		var w = $(window).width();
		var h = $(window).height();

		var ml = 0;
		var mt = 24;

		// Calculate dimensions of dialog
		var dw = Math.min(900, w - ml);
		var dh = Math.min(600, h - mt);
		//            var dw = (w-24) + 'px';
		//            var dh = (h-24) + 'px';

		var l = parseInt((w - dw) / 2);
		var t = parseInt((h - dh) / 2);

//        $('.blockPage').css({
//            width: dw + 'px',
//            height: dh + 'px',
//            top: t + 'px',
//            left: l + 'px'
//        });

		var bw = (dw - 28) + 'px';
		var bh = (dh - 106) + 'px';

		$('#dialogBox .dialogTextWrapper').css({
			width: bw,
			height: bh
		});

		$('#resourceTabs .resourceBox table').css({
			height: h - ($('#resourceTabsMenu').height() + 158) + 'px',
			width:  w - 59 + 'px'
		});

		$('.searchResults').css({
			height: h - 103 + 'px'
		});

	}
};