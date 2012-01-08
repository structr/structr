/* 
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

/**************** config parameter **********************************/
var rootUrl =     '/structr/json/';
var viewRootUrl = '/structr/html/';
var wsRoot = '/structr/ws';
var headers = {
	'X-User' : 0
};
/********************************************************************/

var main;
var debug = false;
//var onload = [];
var lastMenuEntry;

$(window).unload(function() {
	if (lastMenuEntry && lastMenuEntry != 'logout') {
		$.cookie('structrLastMenuEntry', lastMenuEntry);
	}
});
	
	
$(document).ready(function() {
	if (debug) console.log('Debug mode');
	
	main = $('#main');
	//main.height($(window.document).height() - $('#header').height() - 13);
	$('#import_json').on('click', function() {
		var jsonArray = $.parseJSON($('#json_input').val());
		$(jsonArray).each(function(i, json) {
			//console.log(json);
			createEntity(json);
		});
	//var json = $.parseJSON('{ "test" : "abc" }');

	});
	$('#loginButton').on('click', function() {
		var username = $('#usernameField').val();
		var password = $('#passwordField').val();
		Structr.doLogin(username, password);
	});
	$('#logout_').on('click', function() {
		Structr.doLogout();
	});


	$('#dashboard_').on('click', function() {
		main.empty();
		Structr.activateMenuEntry('dashboard');
	});

	$('#resources_').on('click', function() {
		main.empty();
		Structr.activateMenuEntry('resources');
		Structr.modules['resources'].onload();
	});

	$('#elements_').on('click', function() {
		main.empty();
		Structr.activateMenuEntry('elements');
		Structr.modules['elements'].onload();
	});

	$('#contents_').on('click', function() {
		main.empty();
		Structr.activateMenuEntry('contents');
		Structr.modules['contents'].onload();
	});

	$('#files_').on('click', function() {
		main.empty();
		Structr.activateMenuEntry('files');
		Structr.modules['files'].onload();
	});

	$('#usersAndGroups_').on('click', function() {
		main.empty();
		Structr.activateMenuEntry('usersAndGroups');
		Structr.modules['usersAndGroups'].onload();
	});

	$('#usernameField').keypress(function(e) {
		if(e.which == 13) {
			jQuery(this).blur();
			jQuery('#loginButton').focus().click();
		}
	});
	$('#passwordField').keypress(function(e) {
		if(e.which == 13) {
			jQuery(this).blur();
			jQuery('#loginButton').focus().click();
		}
	});
	
	Structr.init();
	
});


var Structr = {
	
	modules : {},
	classes : [],
	
	add_icon : 'icon/add.png',
	delete_icon : 'icon/delete.png',
	edit_icon : 'icon/pencil.png',
		
	init : function() {
        
		$.unblockUI();

		connect();
	
	
		main.empty();
		user = $.cookie('structrUser');

		//		UsersAndGroups.onload();

		ws.onopen = function() {

			if (debug) console.log('logged in? ' + loggedIn);
			if (!loggedIn) {
				if (debug) console.log('no');
				$('#logout_').html('Login');
				Structr.login();
			} else {
				if (debug) console.log('Current user: ' + user);
				//            $.cookie("structrUser", username);
				$('#logout_').html(' Logout <span class="username">' + user + '</span>');
				//				UsersAndGroups.onload();
				
				var browserUrl = window.location.href;
				
				lastMenuEntry = getAnchorFromUrl(browserUrl) || $.cookie('structrLastMenuEntry');
				if (lastMenuEntry) {
					if (debug) console.log('Last menu entry found: ' + lastMenuEntry);
					Structr.activateMenuEntry(lastMenuEntry);
					var module = Structr.modules[lastMenuEntry];
					if (module) {
						module.init();
						module.onload();
					}
				}
				
				
				
			}
		}
	

	},

	login : function(text) {
		$('#logout_').html('Login');
		if (text) $('#errorText').html(text);
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			message: $('#login'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
		});
		Structr.activateMenuEntry('logout');
	},

	doLogin : function(username, password) {
		if (debug) console.log('doLogin ' + username + ' with ' + password);
		if (send('{ "command":"LOGIN", "data" : { "username" : "' + username + '", "password" : "' + password + '" } }')) {
			user = username;
			return true;
		}
		return false;
	},

	doLogout : function(text) {
		if (debug) console.log('doLogout ' + user);
		$.cookie('structrSessionToken', '');
		$.cookie('structrUser', '');
		if (send('{ "command":"LOGOUT", "data" : { "username" : "' + user + '" } }')) {
			Structr.clearMain();
			Structr.login(text);
			return true;
		}
		return false;
	},

	clearMain : function() {
		main.empty();
	},

	confirmation : function(text, callback) {
		if (text) $('#confirmationText').html(text);
		if (callback) $('#yesButton').on('click', function() {
			callback();
		});
		$('#noButton').on('click', function() {
			$.unblockUI();
		});
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			message: $('#confirmation'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
		});
	
	},

	info : function(text, callback) {
		if (text) $('#infoText').html(text);
		if (callback) $('#okButton').on('click', function() {
			callback();
		});
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			message: $('#infoBox'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
		});

	},

	error : function(text, callback) {
		if (text) $('#infoText').html('<img src="icon/error.png"> ' + text);
		if (callback) $('#okButton').on('click', function() {
			callback();
		});
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			message: $('#infoBox'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
		});
	},

	activateMenuEntry : function(name) {
		lastMenuEntry = name;
		$('.menu a').each(function(i,v) {
			$(this).removeClass('active').addClass('inactive');
		});
		var menuEntry = $('#' + name + '_');
		menuEntry.addClass('active').removeClass('inactive');
		$('#title').text('structr ' + menuEntry.text());
	},
	
	registerModule : function(name, module) {
		Structr.modules[name] = module;
		if (debug) console.log('Module ' + name + ' registered');
	},
	
	entity : function(id, parentId) {
		var entityElement, parentElement;
		if (parentId) {
			parentElement = $('.' + parentId + '_');
			entityElement = $('.' + id + '_', parentElement);
		} else {
			entityElement = $('.' + id + '_');
		}

		var entity = {};
		
		entity.id = id;
		
		$(Structr.classes).each(function(i, cls) {
			if (entityElement.hasClass(cls)) {
				entity.type = cls;
			}
		});
		
		if (debug) console.log(entity.type);
		entity.name = $('.name', entityElement).text();
	
		return entity;
	},
	
	findParent : function(parentId, rootId, defaultElement) {
		var parent;
		if (parentId) {
			if (rootId && rootId != parentId) {
				var rootElement = $('.' + rootId + '_');
				parent = $('.' + parentId + '_', rootElement);
			} else {
				parent = $('.' + parentId + '_');
			}
		} else {
			parent = defaultElement;
		}
		
		if (debug) console.log(parent);

		return parent;
	}
};