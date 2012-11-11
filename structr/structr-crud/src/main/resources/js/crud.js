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
//var onload = [];
var lastMenuEntry, activeTab;
var dmp;
var editorCursor;
var dialog;

var view = urlParam('view')?urlParam('view'):'all';
var keys = [];
var typeFromHash;

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
                $('#resourceTabsMenu').append('<li><a href="#' + type + '"><span>' + type + '</span></a></li>');
                $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
                var typeNode = $('#' + type);
                typeNode.append('<table></table>');
                var table = $('table', typeNode);
                table.append('<tr></tr>');
                var tableHeader = $('tr:first-child', table);
                if (res.views[view]) {
                    var k = Object.keys(res.views[view]);
                    if (k && k.length) {
                        keys[type] = k;
                        $.each(keys[type], function(k, key) {
                            tableHeader.append('<th>' + key + '</th>');
                        });
                    }
                }
                typeNode.append('<button id="create' + type + '"><img src="icon/add.png"> Create new ' + type + '</button>');
                $('#create' + type, typeNode).on('click', function() {
                    crudCreate(type, res.url);
                });
                typeFromHash = hash.substring(1);
                if (typeFromHash && typeFromHash == type) {
//                    console.log('activate list', typeFromHash, type);
                    activateList(typeFromHash);
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
});

function activateList(type) {
    var url  = $('#' + type).attr('data-url');
    list(type, restUrl + url + '/' + view);
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
            //            console.log('list', data);
            $.each(data.result, function(i, item) {
                //                console.log(i, item, type);
                appendRow(type, item);
            });
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
            console.log('crudRefresh', data);
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
            console.log('crudReset', data);
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
//            console.log(data, status, xhr);
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
    c.text(oldValue);
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

function urlParam(name) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    return (results&&results.length?results[1]:'');
}
