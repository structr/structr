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

/**************** config parameter **********************************/
var rootUrl =     '/structr/rest/';
var viewRootUrl = '/';
var wsRoot = '/structr/ws';
/********************************************************************/

var header, main, footer;
var debug = false;
//var onload = [];
var lastMenuEntry, activeTab;
var dmp;
var editorCursor;
var dialog;
var dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogCancelButton, dialogSaveButton, loginButton;
var dialogId;
var page = [];
var pageSize = [];
var expandedIdsCookieName = 'structrTreeExpandedIds_' + port;
var lastMenuEntryCookieName = 'structrLastMenuEntry_' + port;
var pagerDataCookieName = 'structrPagerData_' + port + '_';

$(document).ready(function() {
    
    if (urlParam('debug')) {
        debug = true;
    }
    
    header = $('#header');
    main = $('#main');
    footer = $('#footer');
    
    if (debug) {
        footer.show();
    }
    
    dialogBox = $('#dialogBox');
    dialog = $('.dialogText', dialogBox);
    dialogMsg = $('.dialogMsg', dialogBox);
    dialogBtn = $('.dialogBtn');
    dialogTitle = $('.dialogTitle', dialogBox);
    dialogMeta = $('.dialogMeta', dialogBox);
    dialogText = $('.dialogText', dialogBox);
    dialogCancelButton = $('.closeButton', dialogBtn);
    dialogSaveButton = $('.save', dialogBox);
    loginButton = $('#loginButton');

    dmp = new diff_match_patch();
        
    $('#import_json').on('click', function(e) {
        e.stopPropagation();
        var jsonArray = $.parseJSON($('#json_input').val());
        $(jsonArray).each(function(i, json) {
            //console.log(json);
            createEntity(json);
        });
    });
    
    loginButton.on('click', function(e) {
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
    
    $('#propertyDefinitions_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('propertyDefinitions');
        Structr.modules['propertyDefinitions'].onload();
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

    $('#crud_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('crud');
        Structr.modules['crud'].onload();
    });

    $('#files_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('files');
        Structr.modules['files'].onload();
    });

    $('#images_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('images');
        Structr.modules['images'].onload();
    });

    $('#usersAndGroups_').on('click', function(e) {
        e.stopPropagation();
        main.empty();
        Structr.activateMenuEntry('usersAndGroups');
        Structr.modules['usersAndGroups'].onload();
    });

    $('#usernameField').keypress(function(e) {
        e.stopPropagation();
        if (e.which === 13) {
            $(this).blur();
            loginButton.focus().click();
        }
    });
    $('#passwordField').keypress(function(e) {
        e.stopPropagation();
        if (e.which === 13) {
            $(this).blur();
            loginButton.focus().click();
        }
    });
	
    Structr.init();
    
    $(document).keyup(function(e) {
        if (e.keyCode === 27) {
            dialogCancelButton.click();
        }
    });     
    
    Structr.resize();
    
    $(window).on('resize', function() {
        Structr.resize();
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
        
        console.log('activating reconnect loop');
        reconn = window.setInterval(function() {
            connect();
        }, 1000);
        console.log('activated reconnect loop', reconn);
    },

    init : function() {
        
        token = $.cookie(tokenCookieName);
        user = $.cookie(userCookieName);
        log('token', token);
        log('user', user);

        Structr.expanded = $.parseJSON($.cookie(expandedIdsCookieName));
        log('######## Expanded IDs after reload ##########', Structr.expanded);
        log('expanded ids', $.cookie(expandedIdsCookieName));

        connect();

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
        log('doLogin ' + username + ' with ' + password);
        if (send('{ "command":"LOGIN", "data" : { "username" : "' + username + '", "password" : "' + password + '" } }')) {
            user = username;
            return true;
        }
        return false;
    },

    doLogout : function(text) {
        log('doLogout ' + user);
        //Structr.saveSession();
        $.cookie(tokenCookieName, '');
        $.cookie(userCookieName, '');
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
        log('anchor', anchor);

        lastMenuEntry = ((anchor && anchor !== 'logout') ? anchor : $.cookie(lastMenuEntryCookieName));
        if (!lastMenuEntry) {
            lastMenuEntry = 'dashboard';
        } else {
            log('Last menu entry found: ' + lastMenuEntry);
            Structr.activateMenuEntry(lastMenuEntry);
            log(Structr.modules);
            var module = Structr.modules[lastMenuEntry];
            if (module) {
                //module.init();
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
            $(this).off('click');
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

        if (browser) {

            dialogText.empty();
            dialogMsg.empty();
            dialogMeta.empty();
            //dialogBtn.empty();
            
            if (text) dialogTitle.html(text);
            if (callbackCancel) dialogCancelButton.on('click', function(e) {
                e.stopPropagation();
                callbackCancel();
                dialogText.empty();
                $.unblockUI({
                    fadeOut: 25

                });
                dialogBtn.children(':not(.closeButton)').remove();
                //dialogSaveButton.remove();
                //$('#saveProperties').remove();
                if (searchField) searchField.focus();
            });
            $.blockUI.defaults.overlayCSS.opacity = .6;
            $.blockUI.defaults.applyPlatformOpacityRules = false;
            
        
            var w = $(window).width();
            var h = $(window).height();

            var ml = 24;
            var mt = 24;

            // Calculate dimensions of dialog
            var dw = Math.min(900, w-ml);
            var dh = Math.min(600, h-mt);
            //            var dw = (w-24) + 'px';
            //            var dh = (h-24) + 'px';
            
            var l = parseInt((w-dw)/2);
            var t = parseInt((h-dh)/2);
            
            $.blockUI({
                fadeIn: 25,
                fadeOut: 25,
                message: dialogBox,
                css: {
                    cursor: 'default',
                    border: 'none',
                    backgroundColor: 'transparent',
                    width: dw + 'px',
                    height: dh + 'px',
                    top: t + 'px',
                    left: l + 'px'
                },
                themedCSS: { 
                    width: dw + 'px',
                    height: dh + 'px',
                    top: t + 'px',
                    left: l + 'px'
                },
                width: dw + 'px',
                height: dh + 'px',
                top: t + 'px',
                left: l + 'px'
            });
            
        }
    },

    resize : function() {
        var w = $(window).width();
        var h = $(window).height();

        var ml = 24;
        var mt = 24;

        // Calculate dimensions of dialog
        var dw = Math.min(900, w-ml);
        var dh = Math.min(600, h-mt);
        //            var dw = (w-24) + 'px';
        //            var dh = (h-24) + 'px';
            
        var l = parseInt((w-dw)/2);
        var t = parseInt((h-dh)/2);
        
        $('.blockPage').css({
            width: dw + 'px',
            height: dh + 'px',
            top: t + 'px',
            left: l + 'px'
        });
        
        var bw = (dw-28) + 'px';
        var bh = (dh-106) + 'px';

        $('#dialogBox .dialogTextWrapper').css({
            width: bw,
            height: bh
        });
    },

    error : function(text, callback) {
        if (text) $('#errorBox .errorText').html('<img src="icon/error.png"> ' + text);
        //console.log(callback);
        if (callback) $('#errorBox .closeButton').on('click', function(e) {
            e.stopPropagation();
            //callback();
            //console.log(callback);
			
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

    tempInfo : function(text, autoclose) {
        window.clearTimeout(dialogId);
        if (text) $('#tempInfoBox .infoHeading').html('<img src="icon/information.png"> ' + text);
        if (autoclose) {
            dialogId = window.setTimeout(function() {
                $.unblockUI({
                    fadeOut: 25
                });                
            }, 3000);
        }
        $('#tempInfoBox .closeButton').on('click', function(e) {
            e.stopPropagation();
            window.clearTimeout(dialogId);
            $.unblockUI({
                fadeOut: 25
            });
            dialogBtn.children(':not(.closeButton)').remove();
            if (searchField) searchField.focus();
        });
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            message: $('#tempInfoBox'),
            css: {
                border: 'none',
                backgroundColor: 'transparent'
            }
        });
    },

    reconnectDialog : function(text) {
        if (text) $('#tempErrorBox .errorText').html('<img src="icon/error.png"> ' + text);
        //console.log(callback);
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            message: $('#tempErrorBox'),
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
        
        if (lastMenuEntry && lastMenuEntry !== 'logout') {

            $.cookie(lastMenuEntryCookieName, lastMenuEntry, {
                expires: 7
            });
        }
    },
	
    registerModule : function(name, module) {
        Structr.modules[name] = module;
        log('Module ' + name + ' registered');
    },

    containsNodes : function(element) {
        log(element, Structr.numberOfNodes(element), Structr.numberOfNodes(element) > 0);
        return (element && Structr.numberOfNodes(element) && Structr.numberOfNodes(element) > 0);
    },

    numberOfNodes : function(element, excludeId) {
        var childNodes = $(element).children('.node');
        if (excludeId) {
            childNodes = childNodes.not('.' + excludeId + '_');
        }
        var n = childNodes.length;
        log('children', $(element).children('.node'));
        log('number of nodes in element', element, n);
        return n;
    },

    findParent : function(parentId, componentId, pageId, defaultElement) {
        var parent = Structr.node(parentId, null, componentId, pageId);
        log('findParent', parentId, componentId, pageId, defaultElement, parent);
        log('findParent: parent element from Structr.node: ', parent);
        if (!parent) parent = defaultElement;
        log('findParent: final parent element: ', parent);
        return parent;
    },
    
    parent : function(id) {
        return Structr.node(id).parent().closest('.node');
    },
    
    node : function(id) {
        var node = $($('#id_' + id)[0]);
        //console.log('Structr.node', node);
        return node.length ? node : undefined;
    },
    
    entity : function(id, parentId) {
        var entityElement = Structr.node(id, parentId);
        var entity = Structr.entityFromElement(entityElement);
        return entity;
    },
    
    getClass : function(el) {
        var c;
        log(Structr.classes);
        $(Structr.classes).each(function(i, cls) {
            log('testing class', cls);
            if (el && el.hasClass(cls)) {
                c = cls;
                log('found class', cls);
            }
        });
        return c;
    },
	
    entityFromElement : function(element) {
        log(element);
        
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
    },
    
    updatePager : function(type) {
        
        if (!type) return;
        
        var pager = $('#pager' + type);
        
        if (pager) {
            
            var pageLeft = $('.pageLeft', pager);
            var pageRight = $('.pageRight', pager);
            if (page[type] === 1) {
                pageLeft.attr('disabled', 'disabled').addClass('disabled');
            } else {
                pageLeft.removeAttr('disabled', 'disabled').removeClass('disabled');
            }

            if (page[type] === pageCount[type]) {
                pageRight.attr('disabled', 'disabled').addClass('disabled');
            } else {
                pageRight.removeAttr('disabled', 'disabled').removeClass('disabled');
            }

            //var urlData = '?pageSize=' + pageSize[type] + '&page=' + page[type] + '#' + lastMenuEntry;
            //console.log(urlData);
            //window.history.pushState('', '', urlData);
            Structr.storePagerDataInCookie(type);
        }
    },
    
    storePagerDataInCookie : function(type) {
        $.cookie(pagerDataCookieName + type, page[type] + ',' + pageSize[type]);
    },
    
    restorePagerDataFromCookie : function(type) {
        var cookie = $.cookie(pagerDataCookieName + type);
        if (cookie) {
            var pagerData = cookie.split(',');
            log('Pager Data from Cookie', pagerData);
            page[type]      = pagerData[0];
            pageSize[type]  = pagerData[1];
        }
    },
    
    /**
     * Append a pager for the given type to the given DOM element.
     * 
     * This pager calls Command#list (WebSocket call) after being loaded
     * and binds Command#list to all actions.
     */
    addPager : function(el, type) {

        // Priority of stored pager data:
        // JS variables -> URL -> Cookie
        
        var pageFromUrl     = urlParam('page');
        var pageSizeFromUrl = urlParam('pageSize');
        
        if (!page[type] && !pageSize[type]) {
            page[type]      =  pageFromUrl;
            pageSize[type]  = pageSizeFromUrl;
        }
        
        if (!page[type] && !pageSize[type]) {
            Structr.restorePagerDataFromCookie(type);
        }
        
        if (!page[type] && !pageSize[type]) {
            page[type]      = defaultPage;
            pageSize[type]  = defaultPageSize;
        }
 
        el.append('<div class="pager" id="pager' + type + '" style="clear: both"><button class="pageLeft">&lt; Prev</button>'
            + ' <input class="page" type="text" size="3" value="' + page[type] + '"><button class="pageRight">Next &gt;</button>'
            + ' of <input class="readonly pageCount" readonly="readonly" size="3">'
            + ' Items: <input class="pageSize" type="text" size="3" value="' + pageSize[type] + '"></div>');
        
        var pager = $('#pager' + type);
        
        var pageLeft = $('.pageLeft', pager);
        var pageRight = $('.pageRight', pager);
        var pageForm = $('.page', pager);
        var pageSizeForm = $('.pageSize', pager);

        pageSizeForm.on('keypress', function(e) {
            if (e.keyCode === 13) {
                pageSize[type] = $(this).val();
                pageCount[type] = Math.ceil(rawResultCount[type] / pageSize[type]);
                page[type] = 1;
                $('.page', pager).val(page[type]);
                $('.pageSize', pager).val(pageSize[type]);
                $('.pageCount', pager).val(pageCount[type]);
                $('.node', el).remove();
                Command.list(type, pageSize[type], page[type], sort, order);
            }
        });

        pageForm.on('keypress', function(e) {
            if (e.keyCode === 13) {
                page[type] = $(this).val();
                $('.page', pager).val(page[type]);
                $('.node', el).remove();
                Command.list(type, pageSize[type], page[type], sort, order);
            }
        });

        if (page[type] === 1) {
            pageLeft.attr('disabled', 'disabled').addClass('disabled');
        }

        if (page[type] === pageCount[type]) {
            pageRight.attr('disabled', 'disabled').addClass('disabled');
        }

        pageLeft.on('click', function() {
            pageRight.removeAttr('disabled').removeClass('disabled');
            page[type]--;
            if (page[type] === 1) {
                pageLeft.attr('disabled', 'disabled').addClass('disabled');
            }
            $('.page', pager).val(page[type]);
            $('.node', el).remove();
            Command.list(type, pageSize[type], page[type], sort, order);
        });
        
        pageRight.on('click', function() {
            pageLeft.removeAttr('disabled').removeClass('disabled');
            page[type]++;
            if (page[type] === pageCount[type]) {
                pageRight.attr('disabled', 'disabled').addClass('disabled');
            }
            $('.page', pager).val(page[type]);
            $('.node', el).remove();
            Command.list(type, pageSize[type], page[type], sort, order);
        });
        
        return Command.list(type, pageSize[type], page[type], sort, order);
    }
};

function getElementPath(element) {
    var i=-1;
    return $(element).parents('.node').andSelf().map(function() {
        i++;
        var self = $(this);
        //if (self.hasClass('page')) return getId(self);
        // id for top-level element
        if (i === 0) return getId(self);
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
    return (contentType && contentType.indexOf('image') > -1);
}

function plural(type) {
    if (type.substring(type.length-1, type.length) === 'y') {
        return type.substring(0, type.length-1) + 'ies';
    } else {
        return type + 's';
    }
}

function addExpandedNode(id) {
    log('addExpandedNode', id);

    if (!id) return;

    getExpanded()[id] = true;
    $.cookie(expandedIdsCookieName, $.toJSON(Structr.expanded), {
        expires: 7
    });

}

function removeExpandedNode(id) {
    log('removeExpandedNode', id);

    if (!id) return;
    
    delete getExpanded()[id];
    $.cookie(expandedIdsCookieName, $.toJSON(Structr.expanded), {
        expires: 7
    });
}

function isExpanded(id) {
    log('id, getExpanded()[id]', id, getExpanded()[id]);

    if (!id) return false;

    var isExpanded = getExpanded()[id] === true ? true : false;

    log(isExpanded);

    return isExpanded;
}

function getExpanded() {
    if (!Structr.expanded) {
        Structr.expanded = $.parseJSON($.cookie(expandedIdsCookieName));
    }

    if (!Structr.expanded) {
        Structr.expanded = {};
    }
    return Structr.expanded;
}


function formatValueInputField(key, obj) {

    if (obj === null) {
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
        if (c === c.toUpperCase()) {
            result += ' ' + c;
        } else {
            result += (i === 0 ? c.toUpperCase() : c);
        }
    }
    return result;
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

function getIdFromIdString(idString) {
    if (!idString || !idString.startsWith('id_')) return false;
    return idString.substring(3);
}

function getId(element) {
    return getIdFromIdString($(element).prop('id')) || undefined;
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
            if (!data || data.length === 0 || !data.result) return;
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
