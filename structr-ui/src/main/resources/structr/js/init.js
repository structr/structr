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

/**************** config parameter **********************************/
var rootUrl = '/structr/rest/';
var viewRootUrl = '/';
var wsRoot = '/structr/ws';
/********************************************************************/

var header, main, footer;
var debug = false;
var sessionId, user;
var lastMenuEntry, activeTab;
var dmp;
var editorCursor;
var dialog, isMax = false;
var dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogCancelButton, dialogSaveButton, saveAndClose, loginButton, loginBox, dialogCloseButton;
var dialogId;
var page = {};
var pageSize = {};
var expandedIdsKey = 'structrTreeExpandedIds_' + port;
var lastMenuEntryKey = 'structrLastMenuEntry_' + port;
var pagerDataKey = 'structrPagerData_' + port + '_';
var autoRefreshDisabledKey = 'structrAutoRefreshDisabled_' + port;
var dialogDataKey = 'structrDialogData_' + port;
var dialogHtmlKey = 'structrDialogHtml_' + port;
var pushConfigKey = 'structrpushConfigKey_' + port;

$(function() {

    if (urlParam('debug')) {
        debug = true;
    }

    header = $('#header');
    main = $('#main');
    footer = $('#footer');
    loginBox = $('#login');

    if (debug) {
        footer.show();
    }

    dialogBox = $('#dialogBox');
    dialog = $('.dialogText', dialogBox);
    dialogMsg = $('.dialogMsg', dialogBox);
    dialogBtn = $('.dialogBtn', dialogBox);
    dialogTitle = $('.dialogTitle', dialogBox);
    dialogMeta = $('.dialogMeta', dialogBox);
    dialogText = $('.dialogText', dialogBox);
    dialogCancelButton = $('.closeButton');
    dialogSaveButton = $('.save', dialogBox);
    loginButton = $('#loginButton');

    $('#import_json').on('click', function(e) {
        e.stopPropagation();
        var jsonArray = $.parseJSON($('#json_input').val());
        $(jsonArray).each(function(i, json) {
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
        Structr.clearMain();
        Structr.activateMenuEntry('dashboard');
        Structr.modules['dashboard'].onload();
    });

    $('#pages_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('pages');
        Structr.modules['pages'].onload();
        _Pages.resize();
    });

    $('#widgets_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('widgets');
        Structr.modules['widgets'].onload();
        _Widgets.resize();
    });

    $('#types_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('types');
        Structr.modules['types'].onload();
    });

    $('#schema_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('schema');
        Structr.modules['schema'].onload();
    });

    $('#elements_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('elements');
        Structr.modules['elements'].onload();
    });

    $('#contents_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('contents');
        Structr.modules['contents'].onload();
    });

    $('#crud_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('crud');
        Structr.modules['crud'].onload();
    });

    $('#files_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('files');
        Structr.modules['files'].onload();
    });

    $('#images_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('images');
        Structr.modules['images'].onload();
    });

    $('#usersAndGroups_').on('click', function(e) {
        e.stopPropagation();
        Structr.clearMain();
        Structr.activateMenuEntry('usersAndGroups');
        Structr.modules['usersAndGroups'].onload();
    });

    $('#usernameField').keypress(function(e) {
        if (e.which === 13) {
            loginButton.click();
        }
    });
    $('#passwordField').keypress(function(e) {
        if (e.which === 13) {
            loginButton.click();
        }
    });

    Structr.connect();

    // This hack prevents FF from closing WS connections on ESC
    $(window).keydown(function(e) {
        if (e.keyCode === 27) {
            e.preventDefault();
        }
    });

    $(window).keyup(function(e) {
        if (e.keyCode === 27) {
            if (dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
                var saveBeforeExit = confirm('Save changes?');
                if (saveBeforeExit) {
                    dialogSaveButton.click();
                    setTimeout(function() {
                        if (dialogSaveButton && dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
                            dialogSaveButton.remove();
                        }
                        if (saveAndClose && saveAndClose.length && saveAndClose.is(':visible') && !saveAndClose.prop('disabled')) {
                            saveAndClose.remove();
                        }
                        if (dialogCancelButton && dialogCancelButton.length && dialogCancelButton.is(':visible') && !dialogCancelButton.prop('disabled')) {
                            dialogCancelButton.click();
                        }
                        return false;
                    }, 1000);
                }
            }
            if (dialogCancelButton.length && dialogCancelButton.is(':visible') && !dialogCancelButton.prop('disabled')) {
                dialogCancelButton.click();
            }
        }
        return false;
    });

    $(window).on('keydown', function(e) {
        if (e.ctrlKey && (e.which === 83)) {
            e.preventDefault();
            if (dialogSaveButton && dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
                dialogSaveButton.click();
            }
        }
    });

    $(window).on('resize', function() {
        Structr.resize();
    });
    dmp = new diff_match_patch();
});

var Structr = {
    modules: {},
    classes: [],
    expanded: {},
    add_icon: 'icon/add.png',
    delete_icon: 'icon/delete.png',
    edit_icon: 'icon/pencil.png',
    expand_icon: 'icon/tree_arrow_right.png',
    expanded_icon: 'icon/tree_arrow_down.png',
    link_icon: 'icon/link.png',
    key_icon: 'icon/key.png',
    reconnect: function() {
        ws.close();
        log('activating reconnect loop');
        reconn = window.setInterval(function() {
            wsConnect();
        }, 1000);
        log('activated reconnect loop', reconn);
    },
    init: function() {
        log('###################### Initialize UI ####################');
        $('#errorText').empty();
        log('user', user);
        Structr.ping();
        Structr.startPing();
        Structr.expanded = JSON.parse(localStorage.getItem(expandedIdsKey));
        log('######## Expanded IDs after reload ##########', Structr.expanded);
    },
    ping: function() {
        if (sessionId) {
            sendObj({command: 'PING', sessionId: sessionId});
        }
    },
    refreshUi: function() {
        localStorage.setItem(userKey, user);
        $.unblockUI({
            fadeOut: 25
        });
        Structr.clearMain();
        $('#logout_').html('Logout <span class="username">' + user + '</span>');
        Structr.loadInitialModule();
        Structr.startPing();
        var dialogData = JSON.parse(window.localStorage.getItem(dialogDataKey));
        log('Dialog data after init', dialogData);
        if (dialogData) {
            Structr.restoreDialog(dialogData);
        }
    },
    startPing: function() {
        log('Starting PING');
        if (!ping) {
            ping = window.setInterval(function() {
                sendObj({command: 'PING', sessionId: sessionId});
            }, 1000);
        }
    },
    connect: function() {
        log('connect')
        sessionId = $.cookie('JSESSIONID');
        if (!sessionId) {
            $.get('/').always(function() {
                sessionId = $.cookie('JSESSIONID');
                wsConnect();
            });
        } else {
            wsConnect();
        }
    },
    login: function(text) {

        if (loginBox.is(':visible')) {
            log('Login box is already visible, skipping...');
            return;
        }

        main.empty();

        $('#logout_').html('Login');
        if (text) {
            $('#errorText').html(text);
        }

        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;
        $.blockUI({
            fadeIn: 25,
            fadeOut: 25,
            message: loginBox,
            forceInput: true,
            css: {
                border: 'none',
                backgroundColor: 'transparent'
            }
        });
        Structr.activateMenuEntry('logout');
    },
    doLogin: function(username, password) {
        log('doLogin ' + username + ' with ' + password);
        var obj = {};
        obj.command = 'LOGIN';
        obj.sessionId = sessionId;
        var data = {};
        data.username = username;
        data.password = password;
        obj.data = data;
        if (sendObj(obj)) {
            user = username;
            return true;
        }
        return false;
    },
    doLogout: function(text) {
        log('doLogout ' + user);
        var obj = {};
        obj.command = 'LOGOUT';
        obj.sessionId = sessionId;
        var data = {};
        data.username = user;
        obj.data = data;
        if (sendObj(obj)) {
            localStorage.removeItem(userKey);
            $.cookie('JSESSIONID', null);
            sessionId.lenght = 0;
            Structr.clearMain();
            Structr.login(text);
            return true;
        }
        ws.close();
        return false;
    },
    loadInitialModule: function() {
        var browserUrl = window.location.href;
        var anchor = getAnchorFromUrl(browserUrl);
        log('anchor', anchor);

        lastMenuEntry = ((anchor && anchor !== 'logout') ? anchor : localStorage.getItem(lastMenuEntryKey));
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
                if (module.resize)
                    module.resize();
            }
        }
    },
    clearMain: function() {
        $.ui.ddmanager.droppables.default = [];
        $('iframe').unload();
        main.empty();
    },
    confirmation: function(text, callback) {
        if (text)
            $('#confirmation .confirmationText').html(text);
        if (callback)
            $('#confirmation .yesButton').on('click', function(e) {
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
    restoreDialog: function(dialogData) {
        log('restoreDialog', dialogData);
        $.blockUI.defaults.overlayCSS.opacity = .6;
        $.blockUI.defaults.applyPlatformOpacityRules = false;

        // Apply stored dimensions of dialog
        var dw = dialogData.width;
        var dh = dialogData.height;

        var l = dialogData.left;
        var t = dialogData.top;

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

        Structr.resize();

    },
    dialog: function(text, callbackOk, callbackCancel) {

        if (browser) {

            dialogText.empty();
            dialogMsg.empty();
            dialogMeta.empty();
            //dialogBtn.empty();

            if (text) {
                dialogTitle.html(text);
            }

            if (callbackCancel) {
                dialogCancelButton.on('click', function(e) {
                    e.stopPropagation();
                    callbackCancel();
                    dialogText.empty();
                    $.unblockUI({
                        fadeOut: 25

                    });
                    dialogBtn.children(':not(.closeButton)').remove();
                    //dialogSaveButton.remove();
                    //$('#saveProperties').remove();
                    if (searchField)
                        searchField.focus();

                    localStorage.removeItem(dialogDataKey);

                });
            }

            $.blockUI.defaults.overlayCSS.opacity = .6;
            $.blockUI.defaults.applyPlatformOpacityRules = false;

            var w = $(window).width();
            var h = $(window).height();

            var ml = 24;
            var mt = 24;

            // Calculate dimensions of dialog
            var dw = Math.min(900, w - ml);
            var dh = Math.min(600, h - mt);
            //            var dw = (w-24) + 'px';
            //            var dh = (h-24) + 'px';

            var l = parseInt((w - dw) / 2);
            var t = parseInt((h - dh) / 2);

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

            Structr.resize();

            log('Open dialog', dialog, text, dw, dh, t, l, callbackOk, callbackCancel);
            var dialogData = {'text': text, 'top': t, 'left': l, 'width': dw, 'height': dh};
            localStorage.setItem(dialogDataKey, JSON.stringify(dialogData));

        }
    },
    resize: function() {

        if (isMax) {
            Structr.maximize();
            return;
        }

        var w = $(window).width();
        var h = $(window).height();

        var ml = 24;
        var mt = 24;

        // Calculate dimensions of dialog
        var dw = Math.min(900, w - ml);
        var dh = Math.min(600, h - mt);

        var l = parseInt((w - dw) / 2);
        var t = parseInt((h - dh) / 2);

        $('.blockPage').css({
            width: dw + 'px',
            height: dh + 'px',
            top: t + 'px',
            left: l + 'px'
        });

        var bw = (dw - 28) + 'px';
        var bh = (dh - 118) + 'px';

        $('#dialogBox .dialogTextWrapper').css({
            width: bw,
            height: bh
        });

        var tabsHeight = $('.files-tabs ul').height();

        $('.CodeMirror').css({
            height: (dh - 118 - 14 - tabsHeight) + 'px'
        });

        $('.CodeMirror-gutters').css({
            height: (dh - 118 - 14 - tabsHeight) + 'px'
        });

        $('.CodeMirror').each(function(i, el) {
            el.CodeMirror.refresh();
        });

        $('.fit-to-height').css({
            height: h - 74 + 'px'
        });

        $('#minimizeDialog').hide();
        $('#maximizeDialog').show().on('click', function() {
            Structr.maximize();
        });

    },
    maximize: function() {

        var w = $(window).width();
        var h = $(window).height();

        var ml = 24;
        var mt = 24;

        // Calculate maximized dimensions of dialog
        var dw = w - ml;
        var dh = h - mt;

        var l = parseInt((w - dw) / 2);
        var t = parseInt((h - dh) / 2);

        $('.blockPage').css({
            width: dw + 'px',
            height: dh + 'px',
            top: t + 'px',
            left: l + 'px'
        });

        var bw = (dw - 28) + 'px';
        var bh = (dh - 118) + 'px';

        $('#dialogBox .dialogTextWrapper').css({
            width: bw,
            height: bh
        });

        var tabsHeight = $('.files-tabs ul').height();

        $('.CodeMirror').css({
            height: (dh - 118 - 14 - tabsHeight) + 'px'
        });

        $('.CodeMirror-gutters').css({
            height: (dh - 118 - 14 - tabsHeight) + 'px'
        });

        $('.CodeMirror').each(function(i, el) {
            el.CodeMirror.refresh();
        });

        $('.fit-to-height').css({
            height: h - 74 + 'px'
        });

        isMax = true;
        $('#maximizeDialog').hide();
        $('#minimizeDialog').show().on('click', function() {
            isMax = false;
            Structr.resize();
        });

    },
    error: function(text, callback) {
        if (text)
            $('#errorBox .errorText').html('<img src="icon/error.png"> ' + text);
        if (callback)
            $('#errorBox .closeButton').on('click', function(e) {
                e.stopPropagation();

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
    errorFromResponse: function(response, url) {
        var errorText = '';
        if (response.errors) {
            $.each(Object.keys(response.errors), function(i, err) {
                errorText += err + ': ';
                $.each(Object.keys(response.errors[err]), function(j, attr) {
                    errorText += attr + ' ';
                    $.each(response.errors[err][attr], function(k, cond) {
                        if (typeof cond === 'Object') {
                            $.each(Object.keys(cond), function(l, key) {
                                errorText += key + ' ' + cond[key];
                            });
                        } else {
                            errorText += ' ' + cond;
                        }
                    });
                });
                errorText += '\n';
            });
        } else {
            errorText += url + ': ' + response.code + ' ' + response.message;
        }
        Structr.error(errorText, function() {
        }, function() {
        });
    },
    tempInfo: function(text, autoclose) {
        window.clearTimeout(dialogId);
        if (text)
            $('#tempInfoBox .infoHeading').html('<img src="icon/information.png"> ' + text);
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
            if (searchField)
                searchField.focus();
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
    reconnectDialog: function(text) {
        if (text) {
            $('#tempErrorBox .errorText').html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAAK/INwWK6QAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAIsSURBVDjLpVNLSJQBEP7+h6uu62vLVAJDW1KQTMrINQ1vPQzq1GOpa9EppGOHLh0kCEKL7JBEhVCHihAsESyJiE4FWShGRmauu7KYiv6Pma+DGoFrBQ7MzGFmPr5vmDFIYj1mr1WYfrHPovA9VVOqbC7e/1rS9ZlrAVDYHig5WB0oPtBI0TNrUiC5yhP9jeF4X8NPcWfopoY48XT39PjjXeF0vWkZqOjd7LJYrmGasHPCCJbHwhS9/F8M4s8baid764Xi0Ilfp5voorpJfn2wwx/r3l77TwZUvR+qajXVn8PnvocYfXYH6k2ioOaCpaIdf11ivDcayyiMVudsOYqFb60gARJYHG9DbqQFmSVNjaO3K2NpAeK90ZCqtgcrjkP9aUCXp0moetDFEeRXnYCKXhm+uTW0CkBFu4JlxzZkFlbASz4CQGQVBFeEwZm8geyiMuRVntzsL3oXV+YMkvjRsydC1U+lhwZsWXgHb+oWVAEzIwvzyVlk5igsi7DymmHlHsFQR50rjl+981Jy1Fw6Gu0ObTtnU+cgs28AKgDiy+Awpj5OACBAhZ/qh2HOo6i+NeA73jUAML4/qWux8mt6NjW1w599CS9xb0mSEqQBEDAtwqALUmBaG5FV3oYPnTHMjAwetlWksyByaukxQg2wQ9FlccaK/OXA3/uAEUDp3rNIDQ1ctSk6kHh1/jRFoaL4M4snEMeD73gQx4M4PsT1IZ5AfYH68tZY7zv/ApRMY9mnuVMvAAAAAElFTkSuQmCC"> ' + text);
        }
        $('#tempErrorBox .closeButton').hide();
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
    activateMenuEntry: function(name) {
        lastMenuEntry = name;
        $('.menu a').each(function(i, v) {
            $(this).removeClass('active').addClass('inactive');
        });
        var menuEntry = $('#' + name + '_');
        menuEntry.addClass('active').removeClass('inactive');
        $('#title').text('Structr ' + menuEntry.text());

        if (lastMenuEntry && lastMenuEntry !== 'logout') {

            localStorage.setItem(lastMenuEntryKey, lastMenuEntry);
        }
    },
    registerModule: function(name, module) {
        Structr.modules[name] = module;
        log('Module ' + name + ' registered');
    },
    containsNodes: function(element) {
        log(element, Structr.numberOfNodes(element), Structr.numberOfNodes(element) > 0);
        return (element && Structr.numberOfNodes(element) && Structr.numberOfNodes(element) > 0);
    },
    numberOfNodes: function(element, excludeId) {
        var childNodes = $(element).children('.node');
        if (excludeId) {
            childNodes = childNodes.not('.' + excludeId + '_');
        }
        var n = childNodes.length;
        log('children', $(element).children('.node'));
        log('number of nodes in element', element, n);
        return n;
    },
    findParent: function(parentId, componentId, pageId, defaultElement) {
        var parent = Structr.node(parentId, null, componentId, pageId);
        log('findParent', parentId, componentId, pageId, defaultElement, parent);
        log('findParent: parent element from Structr.node: ', parent);
        if (!parent)
            parent = defaultElement;
        log('findParent: final parent element: ', parent);
        return parent;
    },
    parent: function(id) {
        return Structr.node(id) && Structr.node(id).parent().closest('.node');
    },
    node: function(id, prefix) {
        var p = prefix || '#id_';
        var node = $($(p + id)[0]);
        return node.length ? node : undefined;
    },
    entity: function(id, parentId) {
        var entityElement = Structr.node(id, parentId);
        var entity = Structr.entityFromElement(entityElement);
        return entity;
    },
    getClass: function(el) {
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
    entityFromElement: function(element) {
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
    initPager: function(type, p, ps) {
        var pagerData = localStorage.getItem(pagerDataKey + type);
        if (!pagerData) {
            page[type] = parseInt(p);
            pageSize[type] = parseInt(ps);
            Structr.storePagerData(type, p, ps);
        } else {
            Structr.restorePagerData(type);
        }
    },
    updatePager: function(type, el) {
        if (!type)
            return;

        var pager = (el && el.length) ? $('.pager' + type, el) : $('.pager' + type);
        if (pager.length) {

            var pageLeft = $('.pageLeft', pager);
            var pageRight = $('.pageRight', pager);
            var pageNo = $('.page', pager);

            if (page[type] === 1) {
                pageLeft.attr('disabled', 'disabled').addClass('disabled');
            } else {
                pageLeft.removeAttr('disabled', 'disabled').removeClass('disabled');
            }

            if (pageCount[type] === 1 || (page[type] === pageCount[type])) {
                pageRight.attr('disabled', 'disabled').addClass('disabled');
            } else {
                pageRight.removeAttr('disabled', 'disabled').removeClass('disabled');
            }

            if (pageCount[type] === 1) {
                pageNo.attr('disabled', 'disabled').addClass('disabled');
            } else {
                pageNo.removeAttr('disabled', 'disabled').removeClass('disabled');
            }

            Structr.storePagerData(type, page[type], pageSize[type]);
        }
    },
    storePagerData: function(type, page, pageSize) {
        if (type, page, pageSize) {
            localStorage.setItem(pagerDataKey + type, page + ',' + pageSize);
        }
    },
    restorePagerData: function(type) {
        var pagerData = localStorage.getItem(pagerDataKey + type);
        if (pagerData) {
            var pagerData = pagerData.split(',');
            page[type] = parseInt(pagerData[0]);
            pageSize[type] = parseInt(pagerData[1]);
        }
    },
    /**
     * Append a pager for the given type to the given DOM element.
     *
     * This pager calls Command#list (WebSocket call) after being loaded
     * and binds Command#list to all actions.
     *
     * If the optional callback function is given, it will be executed
     * instead of the default action.
     */
    addPager: function(el, rootOnly, type, callback) {

        if (!callback) {
            callback = function(entity) {
                StructrModel.create(entity);
            }
        }

        var isPagesEl = (el === pages);

        el.append('<div class="pager pager' + type + '" style="clear: both"><button class="pageLeft">&lt; Prev</button>'
                + ' <input class="page" type="text" size="3" value="' + page[type] + '"><button class="pageRight">Next &gt;</button>'
                + ' of <input class="readonly pageCount" readonly="readonly" size="3">'
                + ' Items: <input class="pageSize" type="text" size="3" value="' + pageSize[type] + '"></div>');

        var pager = $('.pager' + type, el);

        var pageLeft = $('.pageLeft', pager);
        var pageRight = $('.pageRight', pager);
        var pageForm = $('.page', pager);
        var pageSizeForm = $('.pageSize', pager);

        pageSizeForm.on('keypress', function(e) {
            if (e.keyCode === 13) {
                pageSize[type] = $(this).val();
                pageCount[type] = Math.max(1, Math.ceil(rawResultCount[type] / pageSize[type]));
                page[type] = 1;
                $('.page', pager).val(page[type]);
                $('.pageSize', pager).val(pageSize[type]);
                $('.pageCount', pager).val(pageCount[type]);
                $('.node', el).remove();
                if (isPagesEl)
                    _Pages.clearPreviews();
                Command.list(type, rootOnly, pageSize[type], page[type], sort, order, callback);
            }
        });

        pageForm.on('keypress', function(e) {
            if (e.keyCode === 13) {
                page[type] = $(this).val();
                $('.page', pager).val(page[type]);
                $('.node', el).remove();
                if (isPagesEl)
                    _Pages.clearPreviews();
                Command.list(type, rootOnly, pageSize[type], page[type], sort, order, callback);
            }
        });

        pageLeft.on('click', function(e) {
            e.stopPropagation();
            pageRight.removeAttr('disabled').removeClass('disabled');
            page[type]--;
            $('.page', pager).val(page[type]);
            $('.node', el).remove();
            if (isPagesEl) {
                _Pages.clearPreviews();
            }
            Command.list(type, rootOnly, pageSize[type], page[type], sort, order, callback);
        });

        pageRight.on('click', function() {
            pageLeft.removeAttr('disabled').removeClass('disabled');
            page[type]++;
            $('.page', pager).val(page[type]);
            $('.node', el).remove();
            if (isPagesEl) {
                _Pages.clearPreviews();
            }
            Command.list(type, rootOnly, pageSize[type], page[type], sort, order, callback);
        });
        return Command.list(type, rootOnly, pageSize[type], page[type], sort, order, callback);
    },
    makePagesMenuDroppable: function() {

        try {
            $('#pages_').droppable('destroy');
        } catch (err) {
            log('exception:', err.toString())
        }

        $('#pages_').droppable({
            accept: '.element, .content, .component, .file, .image, .widget',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            over: function(e, ui) {

                e.stopPropagation();
                $('#pages_').droppable('disable');
                log('over is off');

                Structr.activateMenuEntry('pages');
                window.location.href = '/structr/#pages';

                if (files && files.length)
                    files.hide();
                if (folders && folders.length)
                    folders.hide();
                if (widgets && widgets.length)
                    widgets.hide();

//                _Pages.init();
                Structr.modules['pages'].onload();
                _Pages.resize();
            }

        });
        $('#pages_').removeClass('nodeHover').droppable('enable');
    },
    openSlideOut: function(slideout, tab, activeTabKey, callback) {
        var s = $(slideout);
        var t = $(tab);
        t.addClass('active');
        s.animate({right: '+=' + rsw + 'px'}, {duration: 100}).zIndex(1);
        localStorage.setItem(activeTabKey, t.prop('id'));
        if (callback) {
            callback();
        }
        _Pages.resize(0, rsw);
    },
    closeSlideOuts: function(slideouts, activeTabKey) {
        var wasOpen = false;
        slideouts.forEach(function(w) {
            var s = $(w);
            var l = s.position().left;
            if (l !== $(window).width()) {
                wasOpen = true;
                s.animate({right: '-=' + rsw + 'px'}, {duration: 100}).zIndex(2);
                $('.compTab.active', s).removeClass('active');
            }
        });
        if (wasOpen) {
            _Pages.resize(0, -rsw);
        }

        localStorage.removeItem(activeTabKey);
    },
    openLeftSlideOut: function(slideout, tab, activeTabKey, callback) {
        var s = $(slideout);
        var t = $(tab);
        t.addClass('active');
        s.animate({left: '+=' + lsw + 'px'}, {duration: 100}).zIndex(1);
        localStorage.setItem(activeTabKey, t.prop('id'));
        if (callback) {
            callback();
        }
        _Pages.resize(lsw, 0);
    },
    closeLeftSlideOuts: function(slideouts, activeTabKey) {
        var wasOpen = false;
        slideouts.forEach(function(w) {
            var s = $(w);
            var l = s.position().left;
            if (l === 0) {
                wasOpen = true;
                s.animate({left: '-=' + lsw + 'px'}, {duration: 100}).zIndex(2);
                $('.compTab.active', s).removeClass('active');
            }
        });
        if (wasOpen) {
            _Pages.resize(-lsw, 0);
        }
        localStorage.removeItem(activeTabKey);
    },
    pushDialog: function(id, recursive) {

        var obj = StructrModel.obj(id);

        Structr.dialog('Push node to remote server', function() {
        },
                function() {
                });

        var pushConf = JSON.parse(localStorage.getItem(pushConfigKey)) || {};

        dialog.append('Do you want to transfer <b>' + (obj.name || obj.id) + '</b> to the remote server?');

        dialog.append('<table class="props push">'
                + '<tr><td>Host</td><td><input id="push-host" type="text" length="20" value="' + (pushConf.host || '') + '"></td></tr>'
                + '<tr><td>Port</td><td><input id="push-port" type="text" length="20" value="' + (pushConf.port || '') + '"></td></tr>'
                + '<tr><td>Username</td><td><input id="push-username" type="text" length="20" value="' + (pushConf.username || '') + '"></td></tr>'
                + '<tr><td>Password</td><td><input id="push-password" type="password" length="20" value="' + (pushConf.password || '') + '"></td></tr>'
                + '</table>'
                + '<button id="start-push">Start</button>');

        $('#start-push', dialog).on('click', function() {
            var host = $('#push-host', dialog).val();
            var port = parseInt($('#push-port', dialog).val());
            var username = $('#push-username', dialog).val();
            var password = $('#push-password', dialog).val();
            var key = 'key_' + obj.id;

            pushConf = {host: host, port: port, username: username, password: password};
            localStorage.setItem(pushConfigKey, JSON.stringify(pushConf));

            Command.push(obj.id, host, port, username, password, key, recursive, function() {
                dialog.empty();
                dialogCancelButton.click();
            })
        });

        return false;
    },
    pullDialog: function(type) {

        Structr.dialog('Sync ' + type.replace(/,/, '(s) or ') + '(s) from remote server', function() {
        },
                function() {
                });

        var pushConf = JSON.parse(localStorage.getItem(pushConfigKey)) || {};

        dialog.append('<table class="props push">'
                + '<tr><td>Host</td><td><input id="push-host" type="text" length="32" value="' + (pushConf.host || '') + '"></td>'
                + '<td>Port</td><td><input id="push-port" type="text" length="32" value="' + (pushConf.port || '') + '"></td></tr>'
                + '<tr><td>Username</td><td><input id="push-username" type="text" length="32" value="' + (pushConf.username || '') + '"></td>'
                + '<td>Password</td><td><input id="push-password" type="password" length="32" value="' + (pushConf.password || '') + '"></td></tr>'
                + '</table>'
                + '<button id="show-syncables">Show available entities</button>'
                + '<table id="syncables" class="props push"><tr><th>Name</th><th>Size</th><th>Last Modified</th><th>Type</th><th>Recursive</th><th>Actions</th></tr>'
                + '</table>'
        );

        $('#show-syncables', dialog).on('click', function() {

            var syncables = $("#syncables");
            var host = $('#push-host', dialog).val();
            var port = parseInt($('#push-port', dialog).val());
            var username = $('#push-username', dialog).val();
            var password = $('#push-password', dialog).val();
            var key = 'syncables';

            pushConf = {host: host, port: port, username: username, password: password};
            localStorage.setItem(pushConfigKey, JSON.stringify(pushConf));

            syncables.empty();
            syncables.append('<tr><th>Name</th><th>Size</th><th>Last Modified</th><th>Type</th><th>Recursive</th><th>Actions</th></tr>');

            Command.listSyncables(host, port, username, password, key, type, function(syncable) {

                syncables.append(
                      '<tr>'
                    + '<td>' + syncable.name + '</td>'
                    + '<td>' + (syncable.size ? syncable.size : "-") + '</td>'
                    + '<td>' + (syncable.lastModifiedDate ? syncable.lastModifiedDate : "-") + '</td>'
                    + '<td>' + syncable.type + '</td>'
                    + '<td><input type="checkbox" id="recursive-' + syncable.id + '"></td>'
                    + '<td><button id="pull-' + syncable.id + '"></td>'
                    + '</tr>'
                );

                var syncButton = $('#pull-' + syncable.id, dialog);

                if (syncable.isSynchronized) {
                    syncButton.empty();
                    syncButton.append('<img src="icon/arrow_refresh.png" title="Update" alt="Update"> Update');
                } else {
                    syncButton.empty();
                    syncButton.append('<img src="icon/page_white_put.png" title="Import" alt="Import"> Import');
                }

                syncButton.on('click', function() {

                    syncButton.empty();
                    syncButton.append('Importing..');

                    var recursive = $('#recursive-' + syncable.id, syncables).prop('checked');
                    Command.pull(syncable.id, host, port, username, password, 'key-' + syncable.id, recursive, function() {
                        // update table cell..
                        syncButton.empty();
                        syncButton.append('<img src="icon/arrow_refresh.png" title="Update" alt="Update"> Update');
                    });
                });
            });
        });

        return false;
    }
};

function getElementPath(element) {
    var i = -1;
    return $(element).parents('.node').andSelf().map(function() {
        i++;
        var self = $(this);
        // id for top-level element
        if (i === 0)
            return getId(self);
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

function addExpandedNode(id) {
    log('addExpandedNode', id);

    if (!id)
        return;

    var alreadyStored = getExpanded()[id];

    if (alreadyStored !== undefined) {
        return;
    }

    getExpanded()[id] = true;
    localStorage.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));

}

function removeExpandedNode(id) {
    log('removeExpandedNode', id);

    if (!id)
        return;

    delete getExpanded()[id];
    localStorage.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));
}

function isExpanded(id) {
    log('id, getExpanded()[id]', id, getExpanded()[id]);

    if (!id)
        return false;

    var isExpanded = getExpanded()[id] === true ? true : false;

    log(isExpanded);

    return isExpanded;
}

function getExpanded() {
    if (!Structr.expanded) {
        Structr.expanded = JSON.parse(localStorage.getItem(expandedIdsKey));
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
        return '<input name="' + key + '" type="text" value="' + escapeForHtmlAttributes(JSON.stringify(obj)) + '">';
    } else if (obj.constructor === Array) {
        var out = '';
        $(obj).each(function(i, v) {
            out += JSON.stringify(v);
        });
        return '<textarea name="' + key + '">' + out + '</textarea>';
    } else {
        return '<input name="' + key + '" type="text" value="' + escapeForHtmlAttributes(obj) + '">';
    }
}

function formatKey(text) {
    var result = '';
    for (var i = 0; i < text.length; i++) {
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
    $.ajax({
        url: nodeUrl + '/in',
        type: 'PUT',
        async: false,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        data: toPut,
        success: function(data) {
            //appendElement(parentId, elementId, data);
        }
    });
}

var keyEventBlocked = true;
var keyEventTimeout;

function getIdFromIdString(idString) {
    if (!idString || !idString.startsWith('id_'))
        return false;
    return idString.substring(3);
}

function getId(element) {
    return getIdFromIdString($(element).prop('id')) || undefined;
}

function getIdFromPrefixIdString(idString, prefix) {
    if (!idString || !idString.startsWith(prefix))
        return false;
    return idString.substring(prefix.length);
}

function getComponentId(element) {
    return getIdFromPrefixIdString($(element).prop('id'), 'componentId_') || undefined;
}

function getActiveElementId(element) {
    return getIdFromPrefixIdString($(element).prop('id'), 'active_') || undefined;
}

$(window).unload(function() {
    log('########################################### unload #####################################################');
    // Remove dialog data in case of page reload
    localStorage.removeItem(dialogDataKey);
});