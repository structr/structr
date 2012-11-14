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
var restUrl =     '/structr/rest';
var viewRootUrl = '/';
/********************************************************************/

var header, main;
var debug = false;

var view = urlParam('view')?urlParam('view'):'all';
var keys = [];
var typeFromHash;

var schema = [];

var sort = urlParam('sort');
var order = urlParam('order');
var pageSize = urlParam('pageSize');
if (!pageSize) pageSize = 10;
var page = urlParam('page');
if (!page) page = 1;
var pageCount;

$(document).ready(function() {
    main = $('#main');
    var url = restUrl + '/_schema';
    var hash = document.location.hash;
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            $.each(data.result, function(i, res) {
                //console.log(res.type, res.views);
                var type = res.type;
                schema[type] = res;
                $('#resourceTabsMenu').append('<li><a href="#' + type + '"><span>' + type + '</span></a></li>');
                $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
                var typeNode = $('#' + type);
                typeNode.append('<div style="clear: both"><button id="pageLeft">&lt; Prev</button>'
                    + ' Page <input id="page" type="text" size="3" value="' + page + '"><button id="pageRight">Next &gt;</button> of <input class="readonly" readonly="readonly" id="pageCount" size="3" value="' + pageCount + '">'
                    + ' Page Size: <input id="pageSize" type="text" size="3" value="' + pageSize + '"></div>');
                typeNode.append('<table></table>');
                var table = $('table', typeNode);
                table.append('<tr></tr>');
                var tableHeader = $('tr:first-child', table);
                if (res.views[view]) {
                    var k = Object.keys(res.views[view]);
                    if (k && k.length) {
                        keys[type] = k;
                        $.each(keys[type], function(k, key) {
//                            var newSort = key;
//                            var newOrder = (order && order == 'desc' ? 'asc' : 'desc');
                            tableHeader.append('<th>' + key + '</th>');
                        });
                    }
                }
                typeNode.append('<div class="infoFooter">Query: <span id="queryTime"></span> s &nbsp; Serialization: <span id="serTime"></span> s</div>');                   
                typeNode.append('<button id="create' + type + '"><img src="icon/add.png"> Create new ' + type + '</button>');
                typeNode.append('<button id="export' + type + '"><img src="icon/database_table.png"> Export as CSV</button>');
                $('#create' + type, typeNode).on('click', function() {
                    crudCreate(type, res.url);
                });
                $('#export' + type, typeNode).on('click', function() {
                    crudExport(type);
                });
                typeFromHash = hash.substring(1);
                if (typeFromHash && typeFromHash == type) {
                    //                    console.log('activate list', typeFromHash, type);
                    activateList(type);
                    activatePagerElements(type);

                }
            });
        }
    });
    $('#resourceTabs').tabs({
        //        active: 7,
        activate: function(event, ui) {
            var type = ui.newPanel[0].id;
            activateList(type);
        }
    });
    $(document).keyup(function(e) {
        if (e.keyCode == 27) {
            $('#dialogBox .dialogCancelButton').click();
        }
    });
    
});

function replaceSortHeader(type) {
    var table = getTable(type);
    var newOrder = (order && order == 'desc' ? 'asc' : 'desc');
    $('th', table).each(function(i, t) {
        var th = $(t);
        var key = th.text();
        $('a', th).off('click');
        th.empty();
        th.append('<a href="' + sortAndPagingParameters(key, newOrder, pageSize, page) + '#' + type + '">' + key + '</a>');
        $('a', th).on('click', function(event) {
            event.preventDefault();
            sort = $(this).text();
            order = (order && order == 'desc' ? 'asc' : 'desc');
            activateList(type);
            return false;
        });
    });
}

function sortAndPagingParameters(s,o,ps,p) {
    var sortParam = (s ? 'sort=' + s : '');
    var orderParam = (o ? 'order=' + o : '');
    var pageSizeParam = (ps ? 'pageSize=' + ps : '');
    var pageParam = (p ? 'page=' + p : '');
    
    var params = (sortParam ? '?' + sortParam : '');
    params = params + (orderParam ? (params.length?'&':'?') + orderParam : '');
    params = params + (pageSizeParam ? (params.length?'&':'?') + pageSizeParam : '');
    params = params + (pageParam ? (params.length?'&':'?') + pageParam : '');
    
    return params;
}

function activateList(type) {
    //console.log('activateList', type);
    var url  = $('#' + type).attr('data-url');
    list(type, restUrl + url + '/' + view + sortAndPagingParameters(sort, order, pageSize, page));
    document.location.hash = type;
}

function clearList(type) {
    //    console.log('clearList');
    var table = getTable(type);
    var headerRow = '<tr>' + $($('tr:first-child', table)[0]).html() + '</tr>';
    //    console.log(headerRow);
    table.empty();
    table.append(headerRow);
}

function list(type, url) {
    //    console.log('list', type, url);
    var table = getTable(type);
    table.append('<tr></tr>');
    clearList(type);
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            $.each(data.result, function(i, item) {
                appendRow(type, item);
            });
            updatePager(type, data.query_time, data.serialization_time, data.page_size, data.page, data.page_count);
            replaceSortHeader(type);
        }
    });
}

function crudExport(type) {
    var url  = restUrl + '/' + $('#' + type).attr('data-url') + '/' + view + sortAndPagingParameters(sort, order, pageSize, page);
    var d = $('.dialogText', $('#dialogBox'));
    d.append('<textarea class="exportArea"></textarea>');
    var exportArea = $('.exportArea', d);
    var b = $('.dialogBtn', $('#dialogBox'));
    console.log(b);
    dialog('Export ' + type + ' list as CSV', function() {
    }, function() {
    });

    b.append('<button id="copyToClipboard">Copy to Clipboard</button>');
    
    $('#copyToClipboard', b).on('click', function() {
        exportArea.focus();
        exportArea.select();
    });
    
    if (keys[type]) {
        //        console.log(type);
        $.each(keys[type], function(k, key) {
            exportArea.append('"' + key + '"');
            if (k < keys[type].length-1) {
                exportArea.append(',');
            }
        });
        exportArea.append('\n');
    }
    
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            $.each(data.result, function(i, item) {
                appendRowAsCSV(type, item, exportArea);
            });
        }
    });
}

function updatePager(type, qt, st, ps, p, pc) {
    var typeNode = $('#' + type);
    $('#queryTime', typeNode).text(qt);
    $('#serTime', typeNode).text(st);
    $('#pageSize', typeNode).val(ps);
    
    page = p;
    $('#page', typeNode).val(page);
    
    pageCount = pc;
    $('#pageCount', typeNode).val(pageCount);
    
//    var documentUrl = document.location.toString().replace(/&page=\d*/, '') + '&page=' + page;
//    documentUrl = documentUrl.replace(/&pageSize=\d*/, '') + '&pageSize=' + pageSize;
//    documentUrl = documentUrl.replace(/&sort=\d*/, '') + '&sort=' + sort;
//    documentUrl = documentUrl.replace(/&order=\d*/, '') + '&order=' + order;
    
    window.history.pushState('object or string', 'Title', sortAndPagingParameters(sort, order, pageSize, page) + '#' + type);
}

function activatePagerElements(type) {
    var typeNode = $('#' + type);
    $('#page', typeNode).keypress(function(e) {
        if (e.keyCode == 13) {
            page = $(this).val();
            activateList(type);
        }
    });
                
    $('#pageSize', typeNode).keypress(function(e) {
        if (e.keyCode == 13) {
            pageSize = $(this).val();
            activateList(type);
        }
    });

    $('#pageLeft', typeNode).on('click', function() {
        if (page > 1) {
            page--;
            activateList(type);
        }
    });
                
    $('#pageRight', typeNode).on('click', function() {
        if (page < pageCount) {
            page++;
            activateList(type);
        }
    });
    
}

function crudRead(type, id) {
    $.ajax({
        url: restUrl + '/' + id + '/' + view,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            appendRow(type, data.result);
        }
    });
}

function getTable(type) {
    var typeNode = $('#' + type);
    //typeNode.append('<table></table>');
    var table = $('table', typeNode);
    return table;
}

function crudCreate(type, url) {
    $.ajax({
        url: restUrl + url,
        type: 'POST',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data, status, xhr) {
            var location = xhr.getResponseHeader('location');
            var id = location.substring(location.lastIndexOf('/') + 1);
            crudRead(type, id);
        },
        error: function(data, status, xhr) {
            console.log(data, status, xhr);
            displayForm(type, $('#dialogBox .dialogText'));
            dialog('Create new ' + type, function() {
                console.log('ok')
            }, function() {
                console.log('cancel')
            });
        }
    });
}

function crudRefresh(id, key) {
    $.ajax({
        url: restUrl + '/' + id + '/' + view,
        type: 'GET',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            refreshCell(id, key, data.result[key]);
        }
    });
}

function crudReset(id, key) {
    $.ajax({
        url: restUrl + '/' + id + '/' + view,
        type: 'GET',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            resetCell(id, key, data.result[key]);
        }
    });
}

function crudUpdate(id, key, newValue) {
    $.ajax({
        url: restUrl + '/' + id + '/' + view,
        data: '{"' + key + '":"' + newValue + '"}',
        type: 'PUT',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function() {
            crudRefresh(id, key);
        },
        error: function(data, status, xhr) {
            crudReset(id, key);
        //alert(data.responseText);
        // TODO: Overlay with error info
        }
    });
}

function crudDelete(id) {
    $.ajax({
        url: restUrl + '/' + id + '/' + view,
        type: 'DELETE',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function() {
            alert(id + ' deleted!');
        }
    });
}

function nvl(value, defaultValue) {
    var returnValue;
    if (value === undefined) {
        returnValue = defaultValue;
    } else if (value === false) {
        returnValue = 'false';
    } else if (!value) {
        returnValue = '';
    } else {
        returnValue = value;
    }
    return returnValue;
}

function cell(id, key) {
    var row = $('#' + id);
    var cell = $('.' + key, row);
    return cell;
}

function resetCell(id, key, oldValue) {
    //    console.log('resetCell', id, key, oldValue);
    var c = cell(id, key);
    c.empty();
    c.text(nvl(oldValue, ''));
    var oldFg = c.css('color');
    var oldBg = c.css('backgroundColor');
    //console.log('old colors' , oldFg, oldBg);
    c.animate({
        color: '#f00',
        backgroundColor: '#fbb'
    }, 250, function() {
        $(this).animate({
            color: oldFg,
            backgroundColor: oldBg
        }, 500);
    });
    c.on('mouseup', function(event) {
        event.preventDefault();
        var self = $(this);
        var oldValue = self.text();
        self.off('mouseup');
        self.html('<input type="text" value="' + oldValue + '">');
        activateTextInputField($('input', self), id, key);
    });
}


function refreshCell(id, key, newValue) {
    //    console.log('refreshCell', id, key, newValue);
    var c = cell(id, key);
    c.empty();
    c.text(newValue);
    var oldFg = c.css('color');
    var oldBg = c.css('backgroundColor');
    //console.log('old colors' , oldFg, oldBg);
    c.animate({
        color: '#81ce25',
        backgroundColor: '#efe'
    }, 100, function() {
        $(this).animate({
            color: oldFg,
            backgroundColor: oldBg
        }, 500);
    });
    c.on('mouseup', function(event) {
        event.preventDefault();
        var self = $(this);
        var oldValue = self.text();
        self.off('mouseup');
        self.html('<input type="text" value="' + oldValue + '">');
        activateTextInputField($('input', self), id, key);
    });
}

function activateTextInputField(input, id, key) {
    input.focus();
    input.on('blur', function() {
        var newValue = input.val();
        crudUpdate(id, key, newValue);
    });
    input.keypress(function(e) {
        if (e.keyCode == 13) {
            var newValue = input.val();
            crudUpdate(id, key, newValue);
        }
    });
}

function appendRow(type, item) {
    var id = item['id'];
    var table = getTable(type);
    table.append('<tr id="' + id + '"></tr>');
    var row = $('#' + id);
    
    if (keys[type]) {
        //        console.log(type);
        $.each(keys[type], function(k, key) {
                    
            row.append('<td class="' + key + '">' + nvl(item[key],'') + '</td>');
            $('.' + key, row).on('mouseup', function(event) {
                event.preventDefault();
                var self = $(this);
                var oldValue = self.text();
                self.off('mouseup');
                self.html('<input type="text" value="' + oldValue + '">');
                activateTextInputField($('input', self), id, key);
            });
        });
    }
}

function appendRowAsCSV(type, item, textArea) {
    if (keys[type]) {
        //        console.log(type);
        $.each(keys[type], function(k, key) {
            textArea.append('"' + nvl(item[key],'') + '"');
            if (k < keys[type].length-1) {
                textArea.append(',');
            }
        });
        textArea.append('\n');
    }
}

function urlParam(name) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    return (results&&results.length?results[1]:'');
}

function dialog(text, callbackOk, callbackCancel) {

    $('#dialogBox .dialogMsg').empty();
    $('#dialogBox .dialogMeta').empty();
    $('#dialogBox .dialogBtn').empty();
            
    if (text) $('#dialogBox .dialogTitle').html(text);
    if (callbackCancel) $('#dialogBox .dialogCancelButton').on('click', function(e) {
        e.stopPropagation();
        callbackCancel();
        $('#dialogBox .dialogText').empty();
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
}

function error(text, callback) {
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
}

function displayForm(type, el) {
    var typeDef = schema[type];
    console.log(typeDef);
    console.log(typeDef.views[view]);
    $(el).append('<table class="props"><tr><th>Name</th><th>Value</th><th>Type</th><th>Read Only</th></table>');
    var table = $('table', $(el));
    $.each(typeDef.views[view], function(i, property) {
        console.log(property);
        var type = property.className.substring(property.className.lastIndexOf('.') + 1);
        table.append('<tr><td><label for="' + property.name + '">' + formatKey(property.name) + '</label></td><td><input type="text" name="' + property.name + '" value=""></td><td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
    });
    $(el).append('<button id="saveProperties">Save</button>');
    $('#saveProperties', $(el)).on('click', function() {
        
        });
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
