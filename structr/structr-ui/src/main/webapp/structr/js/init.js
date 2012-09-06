/* 
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
var dialog;


$(document).ready(function() {
    
    dialog = $('#dialogBox .dialogText');

    dmp = new diff_match_patch()
    if (debug) console.log('Debug mode');
    header = $('#header');
    main = $('#main');
        
    $('#import_json').on('click', function(e) {
        e.stopPropagation();
        var jsonArray = $.parseJSON($('#json_input').val());
        $(jsonArray).each(function(i, json) {
            //console.log(json);
            createEntity(json);
        });
    });
    
    $('#loginButton').on('click', function(e) {
        e.stopPropagation();
        var username = $('#usernameField').val();
        var password = $('#passwordField').val();
        Structr.doLogin(username, password);
    });
    $('#logout_').on('click', function(e) {
        e.stopPropagation();
        Structr.doLogout();
    });

    $('#dashboard_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('dashboard');
        Structr.modules['dashboard'].onload();
    });

    $('#pages_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('pages');
        Structr.modules['pages'].onload();
        _Pages.resize();
    });

    _Pages.makeMenuDroppable();

    $('#components_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('components');
        Structr.modules['components'].onload();
    });
    
    $('#types_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('types');
        Structr.modules['types'].onload();
    });

    $('#elements_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('elements');
        Structr.modules['elements'].onload();
    });

    $('#contents_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('contents');
        Structr.modules['contents'].onload();
    });

    $('#files_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('files');
        Structr.modules['files'].onload();
    });

    $('#usersAndGroups_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('usersAndGroups');
        Structr.modules['usersAndGroups'].onload();
    });

    $('#usernameField').keypress(function(e) {
        e.stopPropagation();
        if (e.which == 13) {
            jQuery(this).blur();
            jQuery('#loginButton').focus().click();
        }
    });
    $('#passwordField').keypress(function(e) {
        e.stopPropagation();
        if (e.which == 13) {
            jQuery(this).blur();
            jQuery('#loginButton').focus().click();
        }
    });
	
    Structr.init();

    $(document).keyup(function(e) {
        if (e.keyCode == 27) {
            $('#dialogBox .dialogCancelButton').click();
        }
    });     
	
});


var Structr = {
	
    modules : {},
    classes : [],
    expanded: {},
	
    add_icon : 'icon/add.png',
    delete_icon : 'icon/delete.png',
    edit_icon : 'icon/pencil.png',
    expand_icon: 'icon/tree_arrow_right.png',
    expanded_icon: 'icon/tree_arrow_down.png',
    link_icon: 'icon/link.png',
    key_icon: 'icon/key.png',

    reconnect : function() {
	
        $.unblockUI({
            fadeOut: 25
        });

        Structr.init();
    //connect();
    },

    init : function() {

        token = $.cookie('structrSessionToken');
        user = $.cookie('structrUser');
        if (debug) console.log('token', token);
        if (debug) console.log('user', user);
        
        $.unblockUI({
            fadeOut: 25
        });

        connect();
	
        main.empty();

        Structr.expanded = $.parseJSON($.cookie('structrTreeExpandedIds'));
        if (debug) console.log('######## Expanded IDs after reload ##########', Structr.expanded);
        if (debug) console.log('expanded ids', $.cookie('structrTreeExpandedIds'));

        ws.onopen = function() {

            if (debug) console.log('logged in? ' + loggedIn);
            if (!loggedIn) {
                if (debug) console.log('no');
                $('#logout_').html('Login');
                Structr.login();
            } else {
                if (debug) console.log('Current user: ' + user);
                $('#logout_').html(' Logout <span class="username">' + (user ? user : '') + '</span>');
				
                Structr.loadInitialModule();
				
            }
        }

    },

    login : function(text) {

        main.empty();
	
        $('#logout_').html('Login');
        if (text) $('#errorText').html(text);
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            fadeIn: 25,
            fadeOut: 25,
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
        //Structr.saveSession();
        $.cookie('structrSessionToken', '');
        $.cookie('structrUser', '');
        if (send('{ "command":"LOGOUT", "data" : { "username" : "' + user + '" } }')) {
            Structr.clearMain();
            Structr.login(text);
            return true;
        }
        return false;
    },

    loadInitialModule : function() {
        var browserUrl = window.location.href;
        var anchor = getAnchorFromUrl(browserUrl);
        if (debug) console.log('anchor', anchor);

        lastMenuEntry = ((anchor && anchor != 'logout') ? anchor : $.cookie('structrLastMenuEntry'));
        if (!lastMenuEntry) {
            lastMenuEntry = 'dashboard';
        } else {
            if (debug) console.log('Last menu entry found: ' + lastMenuEntry);
            Structr.activateMenuEntry(lastMenuEntry);
            if (debug) console.log(Structr.modules);
            var module = Structr.modules[lastMenuEntry];
            if (module) {
                module.init();
                module.onload();
                if (module.resize) module.resize();
            }
        }
    },
   
    clearMain : function() {
        main.empty();
    },

    confirmation : function(text, callback) {
        if (text) $('#confirmation .confirmationText').html(text);
        if (callback) $('#confirmation .yesButton').on('click', function(e) {
            e.stopPropagation();
            callback();
        });
        $('#confirmation .noButton').on('click', function(e) {
            e.stopPropagation();
            $.unblockUI({
                fadeOut: 25
            });
        });
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            fadeIn: 25,
            fadeOut: 25,
            message: $('#confirmation'),
            css: {
                border: 'none',
                backgroundColor: 'transparent'
            }
        });
	
    },

    info : function(text, callback) {
        if (text) $('#infoText').html(text);
        if (callback) $('#okButton').on('click', function(e) {
            e.stopPropagation();
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
        $('#dialogBox .dialogMeta').empty();
        $('#dialogBox .dialogBtn').empty();
            
        if (text) $('#dialogBox .dialogTitle').html(text);
        if (callbackCancel) $('#dialogBox .dialogCancelButton').on('click', function(e) {
            e.stopPropagation();
            callbackCancel();
            $('#dialogBox .dialogText').empty();
            _Pages.reloadPreviews();
            $.unblockUI({
                fadeOut: 25
            });            
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
        if (callback) $('#errorBox .okButton').on('click', function(e) {
            e.stopPropagation();
            //callback();
            console.log(callback);
			
            $.unblockUI({
                fadeOut: 25
            });
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
        
        if (lastMenuEntry && lastMenuEntry != 'logout') {

            $.cookie('structrLastMenuEntry', lastMenuEntry, {
                expires: 7,
                path: '/'
            });
        }
    },
	
    registerModule : function(name, module) {
        Structr.modules[name] = module;
        if (debug) console.log('Module ' + name + ' registered');
    },

    containsNodes : function(element) {
        if (debug) console.log(element, Structr.numberOfNodes(element), Structr.numberOfNodes(element) > 0);
        return (element && Structr.numberOfNodes(element) && Structr.numberOfNodes(element) > 0);
    },

    numberOfNodes : function(element, excludeId) {
        var childNodes = $(element).children('.node');
        if (excludeId) {
            childNodes = childNodes.not('.' + excludeId + '_');
        }
        var n = childNodes.length;
        if (debug) console.log('children', $(element).children('.node'));
        if (debug) console.log('number of nodes in element', element, n);
        return n;
    },

    findParent : function(parentId, componentId, pageId, defaultElement) {
        var parent = Structr.node(parentId, null, componentId, pageId);
        if (debug) console.log('findParent', parentId, componentId, pageId, defaultElement, parent);
        if (debug) console.log('findParent: parent element from Structr.node: ', parent);
        if (!parent) parent = defaultElement;
        if (debug) console.log('findParent: final parent element: ', parent);
        return parent;
    },
    
    node : function(id, parentId, componentId, pageId, position) {
        var entityElement, parentElement, componentElement, pageElement;

        if (debug) console.log('Structr.node', id, parentId, componentId, pageId, position);

        if (id && parentId && componentId && pageId && (pageId != parentId) && (pageId != componentId)) {

            pageElement = $('.node.' + pageId + '_');
            if (debug) console.log('pageElement', pageElement);
            
            if (id == pageId) {
                
                entityElement = pageElement;
                
            } else {
                
                componentElement = $('.node.' + componentId + '_', pageElement);
                if (debug) console.log('componentElement', componentElement);
                
                if (id == componentId) {
                    entityElement = componentElement;
                    
                } else {
                    
                    if (parentId == componentId) {
                        parentElement = componentElement;
                    } else {
                        parentElement = $('.node.' + parentId + '_', componentElement);
                    }
                    if (debug) console.log('parentElement', parentElement);
                    
                    if (id == parentId) {
                        entityElement = parentElement;
                    } else {
                        entityElement = parentElement.children('.node.' + id + '_');
                        if (debug) console.log('entityElement', entityElement);
                    }
                }
            }
            

        } else if (id && parentId && pageId && (pageId != parentId)) {

            pageElement = $('.node.' + pageId + '_');
            parentElement = $('.node.' + parentId + '_', pageElement);
            entityElement = $('.node.' + id + '_', parentElement);

        } else if (id && componentId && pageId && (pageId != componentId)) {
            
            pageElement = $('.node.' + pageId + '_');
            componentElement = $('.node.' + componentId + '_', pageElement);

            if (id == componentId) {
                entityElement = componentElement;
            } else {
                entityElement = $('.node.' + id + '_', componentElement);
            }

        } else if (id && pageId) {
            
            if (id == pageId) {
                entityElement = $('.node.' + pageId + '_');
            }
            else {
                pageElement = $('.node.' + pageId + '_');
                entityElement = $('.node.' + id + '_', pageElement);
            }

        } else if (id && parentId) {

            parentElement = $('.node.' + parentId + '_');
            entityElement = parentElement.children('.node.' + id + '_');

        } else if (id) {

            entityElement = $('.node.' + id + '_');

        }

        if (entityElement && entityElement.length>1 && position) {
            return $(entityElement[position]);
        } else {
            return entityElement;
        }
    },
    
    elementFromAddress : function(treeAddress) {
        return $('#_' + treeAddress);
    },
    
    entityFromAddress : function(treeAddress) {
        var entityElement = Structr.elementFromAddress(treeAddress);
        var entity = Structr.entityFromElement(entityElement);
        return entity;
    },
    
    entity : function(id, parentId) {
        var entityElement = Structr.node(id, parentId);
        var entity = Structr.entityFromElement(entityElement);
        return entity;
    },
    
    getClass : function(el) {
        var c;
        if (debug) console.log(Structr.classes);
        $(Structr.classes).each(function(i, cls) {
            if (debug) console.log('testing class', cls);
            if (el && el.hasClass(cls)) {
                c = cls;
                if (debug) console.log('found class', cls);
            }
        });
        return c;
    },
	
    entityFromElement : function(element) {
        if (debug) console.log(element);
        
        var entity = {};
        entity.id = getId($(element));
        var cls = Structr.getClass(element);
        if (cls) {
            entity.type = cls.capitalize();
        }

        var nameEl = $(element).children('.name_');
        
        if (nameEl && nameEl.length) {
            entity.name = $(nameEl[0]).text();
        }
        
        var tagEl = $(element).children('.tag_');
        
        if (tagEl && tagEl.length) {
            entity.tag = $(tagEl[0]).text();
        }
        
        return entity;
    }
};

function getElementPath(element) {
    var i=-1;
    return $(element).parents('.node').andSelf().map(function() {
        i++;
        var self = $(this);
        //if (self.hasClass('page')) return getId(self);
        // id for top-level element
        if (i==0) return getId(self);
        return self.prevAll('.node').length;
    }).get().join('_');
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

function plural(type) {
    if (type.substring(type.length-1, type.length) == 'y') {
        return type.substring(0, type.length-1) + 'ies';
    } else {
        return type + 's';
    }
}

function addExpandedNode(treeAddress) {
    if (debug) console.log('addExpandedNode', treeAddress);

    if (!treeAddress) return;

    getExpanded()[treeAddress] = true;
    $.cookie('structrTreeExpandedIds', $.toJSON(Structr.expanded), {
        expires: 7, 
        path: '/'
    });

}

function removeExpandedNode(treeAddress) {
    if (debug) console.log('removeExpandedNode', treeAddress);

    if (!treeAddress) return;
    
    delete getExpanded()[treeAddress];
    $.cookie('structrTreeExpandedIds', $.toJSON(Structr.expanded), {
        expires: 7, 
        path: '/'
    });
}

function isExpanded(treeAddress) {
    if (debug) console.log('treeAddress, getExpanded()[treeAddress]', treeAddress, getExpanded()[treeAddress]);

    if (!treeAddress) return false;

    var isExpanded = getExpanded()[treeAddress] == true ? true : false;

    if (debug) console.log(isExpanded);

    return isExpanded;
}

function getExpanded() {
    if (!Structr.expanded) {
        Structr.expanded = $.parseJSON($.cookie('structrTreeExpandedIds'));
    }

    if (!Structr.expanded) {
        Structr.expanded = {};
    }
    return Structr.expanded;
}


function formatValue(key, obj) {

    if (obj == null) {
        return '<input name="' + key + '" type="text" value="">';
    } else if (obj.constructor === Object) {

        return '<input name="' + key + '" type="text" value="' + JSON.stringify(obj) + '">';

    } else if (obj.constructor === Array) {
        var out = '';
        $(obj).each(function(i,v) {
            //console.log(v);
            out += JSON.stringify(v);
        });

        return '<textarea name="' + key + '">' + out + '</textarea>';

    } else {
        return '<input name="' + key + '" type="text" value="' + obj + '">';

    }
}

function formatKey(text) {
    var result = '';
    for (var i=0; i<text.length; i++) {
        var c = text.charAt(i);
        if (c == c.toUpperCase()) {
            result += ' ' + c;
        } else {
            result += (i==0 ? c.toUpperCase() : c);
        }
    }
    return result;
}

function deleteAll(button, type, callback) {
    var types = plural(type);
    var element = $('#' + types);
    if (isDisabled(button)) return;
    var con = confirm('Delete all ' + types + '?');
    if (!con) return;
    var headers = {
        'X-StructrSessionToken' : token
    };
    $.ajax({
        url: rootUrl + type.toLowerCase(),
        type: "DELETE",
        headers: headers,
        success: function(data) {
            $(element).children("." + type).each(function(i, child) {
                $(child).remove();
            });
            enable(button);
            if (callback) callback();
        }
    });
}

function isDisabled(button) {
    return $(button).data('disabled');
}

function disable(button, callback) {
    var b = $(button);
    b.data('disabled', true);
    b.addClass('disabled');
    if (callback) {
        b.off('click');
        b.on('click', callback);
        b.data('disabled', false);
    //enable(button, callback);
    }
}

function enable(button, func) {
    var b = $(button);
    b.data('disabled', false);
    b.removeClass('disabled');
    if (func) {
        b.off('click');
        b.on('click', func);
    }
}

function setPosition(parentId, nodeUrl, pos) {
    var toPut = '{ "' + parentId + '" : ' + pos + ' }';
    //console.log(toPut);
    var headers = {
        'X-StructrSessionToken' : token
    };
    $.ajax({
        url: nodeUrl + '/in',
        type: 'PUT',
        async: false,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        headers: headers,
        data: toPut,
        success: function(data) {
        //appendElement(parentId, elementId, data);
        }
    });
}

function refresh(parentId, id) {
    $('.' + parentId + '_ ' + id + '_ > div.nested').remove();
    showSubEntities(parentId, id);
}

var keyEventBlocked = true;
var keyEventTimeout;

function getIdFromClassString(classString) {
    if (!classString) return false;
    var classes = classString.split(' ');
    var id;
    $(classes).each(function(i,v) {
        var len = v.length;
        if (v.substring(len-1, len) == '_') {
            id = v.substring(0,v.length-1);
        }
    });
    return id;
}

function getId(element) {
    return getIdFromClassString($(element).prop('class')) || undefined;
}

function lastPart(id, separator) {
    if (!separator) {
        separator = '_';
    }
    if (id) {
        return id.substring(id.lastIndexOf(separator)+1);
    }
    return '';
}

function sortArray(arrayIn, sortBy) {
    var arrayOut = arrayIn.sort(function(a,b) {
        return sortBy.indexOf(a.id) > sortBy.indexOf(b.id);
    });
    return arrayOut;
}

function followIds(pageId, entity) {
    var resId = pageId.toString();
    var entityId = (entity ? entity.id : pageId);
    var url = rootUrl + entityId + '/' + 'out';
    //console.log(url);
    var headers = {
        'X-StructrSessionToken' : token
    };
    var ids = [];
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        headers: headers,
        success: function(data) {
            //console.log(data);
            if (!data || data.length == 0 || !data.result) return;
            var out = data.result;
            if ($.isArray(out)) {
                //for (var i=0; i<out.length; i++) {
                $(out).each(function(i, rel) {
                    var pos = rel[resId];
                    if (pos) {
                        //console.log('pos: ' + pos);
                        ids[pos] = rel.endNodeId;
                    //console.log('ids[' + pos + ']: ' + ids[pos]);
                    }
                });
            } else {

                if (out[resId]) {
                    //console.log('out[resId]: ' + out[resId]);
                    ids[out[resId]] = out.endNodeId;
                //console.log('ids[' + out[resId] + ']: ' + out.endNodeId);
                }
            }
        }
    });
    //console.log('pageId: ' + pageId + ', nodeId: ' + pageId);
    //console.log(ids);
    return ids;
}

function isIn(id, ids) {
    return ($.inArray(id, ids) > -1);
}

function escapeTags(str) {
    if (!str) return str;
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function unescapeTags(str) {
    if (!str) return str;
    return str.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
}

$.fn.reverse = [].reverse;

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

/**
 * Clean text from contenteditable
 * 
 * This function will remove any HTML markup and convert
 * any <br> tag into a line feed ('\n').
 */
function cleanText(input) {
    if (debug)console.log(input);
    var output = '';
    $(input).each(function(i, line) {
        var cleaned = $(line).text();
        if (debug) console.log(cleaned.length);
        if (cleaned.length) {
            output += cleaned;
        } else {
            output += '\n';
        }
    });
    if (debug) console.log(output);
    return output;
}

function shorten(uuid) {
    return uuid.substring(0,8);
}