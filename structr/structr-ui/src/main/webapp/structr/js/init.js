/* 
 *  Copyright (C) 2012 Axel Morgner
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
var rootUrl =     '/structr/rest/';
var viewRootUrl = '/';
var wsRoot = '/structr/ws';
/********************************************************************/

var header, main;
var debug = false;
//var onload = [];
var lastMenuEntry, activeTab;
var dmp;
var editorCursor;


$(window).unload(function() {
    Structr.saveSession();
});
	
	
$(document).ready(function() {
    dmp = new diff_match_patch()
    if (debug) console.log('Debug mode');
    header = $('#header');
    main = $('#main');
        
    //        main.height($(window).height()-header.height());

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

    $('#components_').on('click', function() {
        main.empty();
        Structr.activateMenuEntry('components');
        Structr.modules['components'].onload();
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
        if (e.which == 13) {
            jQuery(this).blur();
            jQuery('#loginButton').focus().click();
        }
    });
    $('#passwordField').keypress(function(e) {
        if (e.which == 13) {
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
    expand_icon: 'icon/tree_arrow_right.png',
    link_icon: 'icon/link.png',
    key_icon: 'icon/key.png',
		
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
				
                Structr.loadInitialModule();
				
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
            forceInput: true,
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
        Structr.saveSession();
        $.cookie('structrSessionToken', '');
        $.cookie('structrUser', '');
        if (send('{ "command":"LOGOUT", "data" : { "username" : "' + user + '" } }')) {
            Structr.clearMain();
            Structr.login(text);
            //            var url = window.location.href;
            //            url = url.substring(0, url.lastIndexOf('#')+1);
            return true;
        }
        return false;
    },

    loadInitialModule : function() {
        var browserUrl = window.location.href;
        var anchor = getAnchorFromUrl(browserUrl);
        if (debug) console.log('anchor', anchor);

        lastMenuEntry = ((anchor && anchor != 'logout') ? anchor : $.cookie('structrLastMenuEntry'));
        if (!lastMenuEntry) lastMenuEntry = 'dashboard';
        {
            if (debug) console.log('Last menu entry found: ' + lastMenuEntry);
            Structr.activateMenuEntry(lastMenuEntry);
            if (debug) console.log(Structr.modules);
            var module = Structr.modules[lastMenuEntry];
            if (module) {
                module.init();
                module.onload();
            }
        }
    },

    saveSession : function() {
        if (lastMenuEntry && lastMenuEntry != 'logout') {
            $.cookie('structrLastMenuEntry', lastMenuEntry, {
                expires: 7,
                path: '/'
            });
            if (debug) console.log('set cookie for active tab', activeTab);
            $.cookie('structrActiveTab', activeTab, {
                expires: 7,
                path: '/'
            });
            if (resources) $.cookie('structrResourcesVisible', resources.is(':visible'), {
                expires: 7,
                path: '/'
            });
            if (components) $.cookie('structrComponentsVisible', components.is(':visible'), {
                expires: 7,
                path: '/'
            });
            if (elements) $.cookie('structrElementsVisible', elements.is(':visible'), {
                expires: 7,
                path: '/'
            });
            if (contents) $.cookie('structrContentsVisible', contents.is(':visible'), {
                expires: 7,
                path: '/'
            });
        }
    //console.log('cooke value now: ', $.cookie('structrActiveTab'));
    },

    clearMain : function() {
        main.empty();
    },

    confirmation : function(text, callback) {
        if (text) $('#confirmation .confirmationText').html(text);
        if (callback) $('#confirmation .yesButton').on('click', function() {
            callback();
        });
        $('#confirmation .noButton').on('click', function() {
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
            fadeIn: 25,
            fadeOut: 25,
            message: $('#infoBox'),
            css: {
                border: 'none',
                backgroundColor: 'transparent'
            }
        });

    },

    dialog : function(text, callbackOk, callbackCancel) {

        $('#dialogBox .dialogMsg').empty();

        if (text) $('#dialogBox .dialogTitle').html(text);
        //        if (callbackOk) $('#dialogOkButton').on('click', function() {
        //            callbackOk();
        //			$('#dialogBox .dialogText').empty();
        //			$.unblockUI();
        //        });
        if (callbackCancel) $('#dialogBox .dialogCancelButton').on('click', function() {
            callbackCancel();
            $('#dialogBox .dialogText').empty();
            $.unblockUI();
        });
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            fadeIn: 25,
            fadeOut: 25,
            message: $('#dialogBox'),
            css: {
                border: 'none',
                backgroundColor: 'transparent'
            }
        });
    //        $('.blockUI.blockMsg').center();

    },

    error : function(text, callback) {
        if (text) $('#errorBox .errorText').html('<img src="icon/error.png"> ' + text);
        console.log(callback);
        if (callback) $('#errorBox .okButton').on('click', function() {
            //callback();
            console.log(callback);
			
            $.unblockUI();
        });
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            message: $('#errorBox'),
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

    node : function(id, parentId) {
        var entityElement, parentElement;
        if (parentId) {
            parentElement = $('.' + parentId + '_');
            entityElement = $('.' + id + '_', parentElement);
        } else {
            entityElement = $('.' + id + '_');
        }
        return entityElement;
    },
    
    entity : function(id, parentId) {
        var entityElement = Structr.node(id, parentId);

        var entity = {};
		
        entity.id = id;
		
        $(Structr.classes).each(function(i, cls) {
            if (entityElement.hasClass(cls)) {
                entity.type = cls;
            }
        });
		
        if (debug) console.log(entity.type);
        entity.name = $('.name_', entityElement).text();

        if (debug) console.log(entityElement);
	
        return entity;
    },
    
    entityFromElement : function(element) {
        
        var entity = {};
        entity.id = getId($(element));

        $(Structr.classes).each(function(i, cls) {
            if (element.hasClass(cls)) {
                entity.type = cls;
            }
        });

        if (debug) console.log(entity.type);
        entity.name = $('.name_', element).text();

        return entity;
    },

    findParent : function(parentId, rootId, defaultElement) {
        var parent;
        if (parentId) {
            if (rootId && rootId != parentId) {
                var rootElement = $('.' + rootId + '_');
                parent = $('div.' + parentId + '_', rootElement);
            } else {
                parent = $('div.' + parentId + '_');
            }
        } else {
            parent = defaultElement;
        }
		
        if (debug) console.log(parent);

        return parent;
    }
};


if (typeof String.prototype.endsWith != 'function') {
    String.prototype.endsWith = function(pattern) {
        var d = this.length - pattern.length;
        return d >= 0 && this.lastIndexOf(pattern) === d;
    };
}

if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str){
        return this.indexOf(str) == 0;
    };
}

if (typeof String.prototype.capitalize != 'function') {
    String.prototype.capitalize = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    };
}

function swapFgBg(el) {
    var fg = el.css('color');
    var bg = el.css('backgroundColor');
    el.css('color', bg);
    el.css('backgroundColor', fg);

}

function isImage(contentType) {
    return (contentType.indexOf('image') > -1);
}
