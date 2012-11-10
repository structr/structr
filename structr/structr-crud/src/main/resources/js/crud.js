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
        
                    console.log('activate list', typeFromHash, type);
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
    console.log('clearList');
    var table = getTable(type);
    var headerRow = '<tr>' + $($('tr:first-child', table)[0]).html() + '</tr>';
    console.log(headerRow);
    table.empty();
    table.append(headerRow);
}

function list(type, url) {
    console.log('list', type, url);
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
        url: restUrl + '/' + id,
        type: 'GET',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function(data) {
            refreshCell(data.result, key);
        }
    });
}

function crudUpdate(id, key, newValue) {
    $.ajax({
        url: restUrl + '/' + id,
        data: '{"' + key + '":"' + newValue + '"}',
        type: 'PUT',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        success: function() {
            
            cell(id, key).animate({
                color: '#81ce25'
            }, 100, function() {
                $(this).animate({
                    color: '#333333'
                }, 200);
            });
            crudRefresh(id, key);
        }
    });
}

function crudDelete(id) {
    $.ajax({
        url: restUrl + '/' + id,
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
    return value?value:defaultValue;
}

function cell(id, key) {
    var row = $('#' + id);
    var cell = $('.' + key, row);
    return cell;
}

function refreshCell(item, key) {
    cell(item.id, key).text(item[key]);
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
            $('.' + key, row).on('mouseup', function() {
                var self = $(this);
                var oldValue = self.text();
                self.empty();
                self.off('click');
                self.append('<input type="text" value="' + oldValue + '">');
                $('input', self).focus();
                $('input', self).on('blur', function() {
                    var input = $(this);
                    var newValue = input.val();
                    //console.log((input.val()));
                    crudUpdate(id, key, newValue);
                    self.empty();
                    self.text(newValue);
                });
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
