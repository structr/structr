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

$(document).ready(function() {
    Structr.registerModule('crud', _Crud);
    Structr.classes.push('crud');
});

var _Crud = {

    view : null,
    keys : [],

    schema : [],

    sort : null,
    order : null,
    page : null,
    pageSize : null,
    pageCount : null,
    type : null,

    
    init : function() {
        
        _Crud.view      = urlParam('view');
        _Crud.type      = urlParam('type');
        _Crud.sort      = urlParam('sort');
        _Crud.order     = urlParam('order');
        _Crud.pageSize  = urlParam('pageSize');
        _Crud.page      = urlParam('page');

        if (!_Crud.type)        _Crud.type = document.location.hash.substring(1);

        if (!_Crud.view)        _Crud.view = 'all';
        if (!_Crud.pageSize)    _Crud.pageSize = 10;
        if (!_Crud.page)        _Crud.page = 1;
        
        

    },
    
    onload : function() {
        
        _Crud.init();

        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');

        main = $('#main');
        main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');
        var url = rootUrl + '_schema';
        var hash = document.location.hash;
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data) {
                $.each(data.result, function(i, res) {
                    //console.log(res.type, res.views);
                    var type = res.type;
                    _Crud.schema[type] = res;
                    $('#resourceTabsMenu').append('<li><a href="#' +  type + '"><span>' + type + '</span></a></li>');
                    $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
                    var typeNode = $('#' + type);
                    typeNode.append('<div style="clear: both"><button id="pageLeft">&lt; Prev</button>'
                        + ' Page <input id="page" type="text" size="3" value="' + _Crud.page + '"><button id="pageRight">Next &gt;</button> of <input class="readonly" readonly="readonly" id="pageCount" size="3" value="' + _Crud.pageCount + '">'
                        + ' Page Size: <input id="pageSize" type="text" size="3" value="' + _Crud.pageSize + '"></div>');
                    typeNode.append('<table></table>');
                    var table = $('table', typeNode);
                    table.append('<tr></tr>');
                    var tableHeader = $('tr:first-child', table);
                    if (res.views[_Crud.view]) {
                        var k = Object.keys(res.views[_Crud.view]);
                        if (k && k.length) {
                            _Crud.keys[type] = k;
                            $.each(_Crud.keys[type], function(k, key) {
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
                        _Crud.crudCreate(type, res.url);
                    });
                    $('#export' + type, typeNode).on('click', function() {
                        _Crud.crudExport(type);
                    });
                    if (_Crud.type && _Crud.type == type) {
                        //console.log('activate list', type);
                        _Crud.activateList(type);
                        _Crud.activatePagerElements(type);

                    }
                });
            }
        });
        $('#resourceTabs').tabs({
            //        active: 7,
            activate: function(event, ui) {
                var newType = ui.newPanel[0].id;
                //console.log('deactivated', _Crud.type, 'actived', newType);
                _Crud.deActivatePagerElements(_Crud.type);
                _Crud.activateList(newType);
                _Crud.activatePagerElements(newType);
            }
        });
        $(document).keyup(function(e) {
            if (e.keyCode == 27) {
                $('#dialogBox .dialogCancelButton').click();
            }
        });
    
    },

    replaceSortHeader : function(type) {
        var table = _Crud.getTable(type);
        var newOrder = (_Crud.order && _Crud.order == 'desc' ? 'asc' : 'desc');
        $('th', table).each(function(i, t) {
            var th = $(t);
            var key = th.text();
            $('a', th).off('click');
            th.empty();
            th.append('<a href="' + _Crud.sortAndPagingParameters(key, newOrder, _Crud.pageSize, _Crud.page) + '#' + type + '">' + key + '</a>');
            $('a', th).on('click', function(event) {
                event.preventDefault();
                _Crud.sort = $(this).text();
                _Crud.order = (_Crud.order && _Crud.order == 'desc' ? 'asc' : 'desc');
                _Crud.activateList(type);
                return false;
            });
        });
    },

    sortAndPagingParameters : function(s,o,ps,p) {
        var sortParam = (s ? 'sort=' + s : '');
        var orderParam = (o ? 'order=' + o : '');
        var pageSizeParam = (ps ? 'pageSize=' + ps : '');
        var pageParam = (p ? 'page=' + p : '');
    
        var params = (sortParam ? '?' + sortParam : '');
        params = params + (orderParam ? (params.length?'&':'?') + orderParam : '');
        params = params + (pageSizeParam ? (params.length?'&':'?') + pageSizeParam : '');
        params = params + (pageParam ? (params.length?'&':'?') + pageParam : '');
    
        return params;
    },

    activateList : function(type) {
        var url  = $('#' + type).attr('data-url').substring(1);
        url = rootUrl + url + '/' + _Crud.view + _Crud.sortAndPagingParameters(_Crud.sort, _Crud.order, _Crud.pageSize, _Crud.page);
        //console.log('activateList', type, url, _Crud.view);
        _Crud.list(type, url);
        document.location.hash = type;
    },

    clearList : function(type) {
        //    console.log('clearList');
        var table = _Crud.getTable(type);
        var headerRow = '<tr>' + $($('tr:first-child', table)[0]).html() + '</tr>';
        //    console.log(headerRow);
        table.empty();
        table.append(headerRow);
    },

    list : function(type, url) {
        //    console.log('list', type, url);
        var table = _Crud.getTable(type);
        table.append('<tr></tr>');
        _Crud.clearList(type);
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data) {
                $.each(data.result, function(i, item) {
                    _Crud.appendRow(type, item);
                });
                _Crud.updatePager(type, data.query_time, data.serialization_time, data.page_size, data.page, data.page_count);
                _Crud.replaceSortHeader(type);
            }
        });
    },

    crudExport : function(type) {
        var url  = rootUrl + '/' + $('#' + type).attr('data-url') + '/' + view + _Crud.sortAndPagingParameters(_Crud.sort, _Crud.order, _Crud.pageSize, _Crud.page);
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
    
        if (_Crud.keys[type]) {
            //        console.log(type);
            $.each(_Crud.keys[type], function(k, key) {
                exportArea.append('"' + key + '"');
                if (k < _Crud.keys[type].length-1) {
                    exportArea.append(',');
                }
            });
            exportArea.append('\n');
        }
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data) {
                $.each(data.result, function(i, item) {
                    _Crud.appendRowAsCSV(type, item, exportArea);
                });
            }
        });
    },

    updatePager : function(type, qt, st, ps, p, pc) {
        var typeNode = $('#' + type);
        $('#queryTime', typeNode).text(qt);
        $('#serTime', typeNode).text(st);
        $('#pageSize', typeNode).val(ps);
    
        _Crud.page = p;
        $('#page', typeNode).val(_Crud.page);
    
        _Crud.pageCount = pc;
        $('#pageCount', typeNode).val(_Crud.pageCount);
    
        //    var documentUrl = document.location.toString().replace(/&page=\d*/, '') + '&page=' + page;
        //    documentUrl = documentUrl.replace(/&pageSize=\d*/, '') + '&pageSize=' + pageSize;
        //    documentUrl = documentUrl.replace(/&sort=\d*/, '') + '&sort=' + sort;
        //    documentUrl = documentUrl.replace(/&order=\d*/, '') + '&order=' + order;
    
        window.history.pushState('object or string', 'Title', _Crud.sortAndPagingParameters(_Crud.sort, _Crud.order, _Crud.pageSize, _Crud.page) + '&type=' + type + '#crud');
    },

    activatePagerElements : function(type) {
        console.log('activatePagerElements', type);
        var typeNode = $('#' + type);
        $('#page', typeNode).keypress(function(e) {
            if (e.keyCode == 13) {
                _Crud.page = $(this).val();
                _Crud.activateList(type);
            }
        });
                
        $('#pageSize', typeNode).keypress(function(e) {
            if (e.keyCode == 13) {
                _Crud.pageSize = $(this).val();
                _Crud.activateList(type);
            }
        });

        $('#pageLeft', typeNode).on('click', function() {
            if (_Crud.page > 1) {
                _Crud.page--;
                _Crud.activateList(type);
            }
        });
                
        $('#pageRight', typeNode).on('click', function() {
            if (_Crud.page < _Crud.pageCount) {
                _Crud.page++;
                _Crud.activateList(type);
            }
        });
    
    },
    
    deActivatePagerElements : function(type) {
        console.log('deActivatePagerElements', type);
        var typeNode = $('#' + type);
        
        $('#page', typeNode).keypress();
        $('#pageSize', typeNode).keypress();
        $('#pageLeft', typeNode).off('click');
        $('#pageRight', typeNode).off('click');
    },

    crudRead : function(type, id) {
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data) {
                _Crud.appendRow(type, data.result);
            }
        });
    },

    getTable : function(type) {
        var typeNode = $('#' + type);
        //typeNode.append('<table></table>');
        var table = $('table', typeNode);
        return table;
    },

    crudCreate : function(type, url) {
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: rootUrl + url,
            headers: headers,
            type: 'POST',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data, status, xhr) {
                var location = xhr.getResponseHeader('location');
                var id = location.substring(location.lastIndexOf('/') + 1);
                _Crud.crudRead(type, id);
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
    },

    crudRefresh : function(id, key) {
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view,
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data) {
                _Crud.refreshCell(id, key, data.result[key]);
            }
        });
    },

    crudReset : function(id, key) {
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view,
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function(data) {
                _Crud.resetCell(id, key, data.result[key]);
            }
        });
    },

    crudUpdate : function(id, key, newValue) {
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view,
            headers: headers,
            data: '{"' + key + '":"' + newValue + '"}',
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function() {
                _Crud.crudRefresh(id, key);
            },
            error: function(data, status, xhr) {
                _Crud.crudReset(id, key);
            //alert(data.responseText);
            // TODO: Overlay with error info
            }
        });
    },

    crudDelete : function(id) {
        var headers = {'X-StructrSessionToken' : token };
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view,
            headers: headers,
            type: 'DELETE',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            async: false,
            success: function() {
                alert(id + ' deleted!');
            }
        });
    },

    cell : function(id, key) {
        var row = $('#' + id);
        var cell = $('.' + key, row);
        return cell;
    },

    resetCell : function(id, key, oldValue) {
        //    console.log('resetCell', id, key, oldValue);
        var c = _Crud.cell(id, key);
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
            _Crud.activateTextInputField($('input', self), id, key);
        });
    },

    refreshCell : function(id, key, newValue) {
        //    console.log('refreshCell', id, key, newValue);
        var c = _Crud.cell(id, key);
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
            _Crud.activateTextInputField($('input', self), id, key);
        });
    },

    activateTextInputField : function(input, id, key) {
        input.focus();
        input.on('blur', function() {
            var newValue = input.val();
            _Crud.crudUpdate(id, key, newValue);
        });
        input.keypress(function(e) {
            if (e.keyCode == 13) {
                var newValue = input.val();
                _Crud.crudUpdate(id, key, newValue);
            }
        });
    },

    appendRow : function(type, item) {
        var id = item['id'];
        var table = _Crud.getTable(type);
        table.append('<tr id="' + id + '"></tr>');
        var row = $('#' + id);
    
        if (_Crud.keys[type]) {
            //        console.log(type);
            $.each(_Crud.keys[type], function(k, key) {
                    
                row.append('<td class="' + key + '">' + nvl(item[key],'') + '</td>');
                $('.' + key, row).on('mouseup', function(event) {
                    event.preventDefault();
                    var self = $(this);
                    var oldValue = self.text();
                    self.off('mouseup');
                    self.html('<input type="text" value="' + oldValue + '">');
                    _Crud.activateTextInputField($('input', self), id, key);
                });
            });
        }
    },

    appendRowAsCSV : function(type, item, textArea) {
        if (_Crud.keys[type]) {
            //        console.log(type);
            $.each(_Crud.keys[type], function(k, key) {
                textArea.append('"' + nvl(item[key],'') + '"');
                if (k < _Crud.keys[type].length-1) {
                    textArea.append(',');
                }
            });
            textArea.append('\n');
        }
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

    displayForm : function(type, el) {
        var typeDef = _Crud.schema[type];
        console.log(typeDef);
        console.log(typeDef.views[view]);
        $(el).append('<table class="props"><tr><th>Name</th><th>Value</th><th>Type</th><th>Read Only</th></table>');
        var table = $('table', $(el));
        $.each(typeDef.views[_Crud.view], function(i, property) {
            console.log(property);
            var type = property.className.substring(property.className.lastIndexOf('.') + 1);
            table.append('<tr><td><label for="' + property.name + '">' + formatKey(property.name) + '</label></td><td><input type="text" name="' + property.name + '" value=""></td><td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
        });
        $(el).append('<button id="saveProperties">Save</button>');
        $('#saveProperties', $(el)).on('click', function() {
        
            });
    },

    formatKey : function(text) {
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
}
