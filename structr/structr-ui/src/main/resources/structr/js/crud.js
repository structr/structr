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

var defaultView, defaultSort, defaultOrder, defaultPage, defaultPageSize;
var searchField;

var browser = (typeof document == 'object');
var headers;

if (browser) {
    
    $(document).ready(function() {

        defaultView     = urlParam('view') ? urlParam('view') : 'public';
        defaultSort     = urlParam('sort') ? urlParam('sort') : '';
        defaultOrder    = urlParam('order') ? urlParam('order') : '';
        defaultPage     = urlParam('page') ? urlParam('page') : 1;
        defaultPageSize = urlParam('pageSize') ? urlParam('pageSize') : 10;
 
        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');

    });
} else {
    defaultView = 'public';
    // default header fields
    headers = {
        'X-User' : 'admin',
        'X-Password' : 'admin'
    };   
}

var _Crud = {
    
    schemaLoading : false,
    schemaLoaded : false,
    
    types : [  "Conference", "Person", "Session", "Track" ],
    views : [ "public", "all"],

    schema : [],
    keys : [],

    type : null,
    pageCount : null,

    view : [],
    sort : [],
    order : [],
    page : [],
    pageSize : [],
    
    init : function() {

        headers = {
            'X-StructrSessionToken' : token
        };

        _Crud.schemaLoading = false;
        _Crud.schemaLoaded = false;
        _Crud.schema = [];
        _Crud.keys = [];

        _Crud.type             = urlParam('type');
        if (!_Crud.type) _Crud.type = 'Conference';

        _Crud.loadSchema();


        main = $('#main');
        //showAjaxLoader(main);
        
        
        main.append('<div id="searchBox"><input id="search" name="search" size="20" placeholder="Search"><img id="clearSearchIcon" src="icon/cross_small_grey.png"></div>');
        searchField = $('#search', main);
        searchField.focus();
        
        searchField.keyup(function(e) {
            var searchString = $(this).val();
            if (searchString && searchString.length && e.keyCode == 13) {
                
                $('#clearSearchIcon').show().on('click', function() {
                    _Crud.clearSearch();
                });
               
                _Crud.search(searchString);
                
                $('#resourceTabs', main).remove();
                $('#resourceBox', main).remove();
                
            } else if (e.keyCode == 27 || searchString == '') {
                
                _Crud.clearSearch();
            }
            
            
        });
        
        main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');
        
        $(document).keyup(function(e) {
            if (e.keyCode == 27) {
                dialogCancelButton.click();
            }
        });
    },
    
    onload : function() {
        
        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');
        
        _Crud.init();

    
    },

    initTabs : function() {
        
        $.each(_Crud.types, function(t, type) {
            _Crud.addTab(type);
        });

        $('#resourceTabs').tabs({
            activate: function(event, ui) {
                var newType = ui.newPanel[0].id;
                //console.log('deactivated', _Crud.type, 'activated', newType);
                _Crud.deActivatePagerElements(_Crud.type);
                _Crud.activateList(newType);
                _Crud.activatePagerElements(newType);
                _Crud.type = newType;
                _Crud.updateUrl(newType);
            }
        });
    },

    isSchemaLoaded : function() {
        var all = true;
        if (!_Crud.schemaLoaded) {
            //console.log('schema not loaded completely, checking all types ...');
            $.each(_Crud.types, function(t, type) {
                //console.log('checking type ' + type, (_Crud.schema[type] && _Crud.schema[type] != null));
                all &= (_Crud.schema[type] && _Crud.schema[type] != null);
            });
        }
        _Crud.schemaLoaded = all;
        return _Crud.schemaLoaded;
    },

    updateUrl : function(type) {
        //console.log('updateUrl', type);
        if (type) {
            window.history.pushState('object or string', 'Title', _Crud.sortAndPagingParameters(_Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]) + '&view=' + _Crud.view[type] + '&type=' + type + '#crud');
        }
        searchField.focus();

    },

    loadSchema : function() {
        // Avoid duplicate loading of schema
        if (_Crud.schemaLoading) {
            return;
        }
        _Crud.schemaLoading = true;
        $.each(_Crud.types, function(t, type) {
            //console.log('Loading type definition for ' + type + '...');
            _Crud.loadTypeDefinition(type);
        });
    },

    loadTypeDefinition : function(type) {
        
        //_Crud.schema[type] = [];
        
        var url = rootUrl + '_schema/' + type.toLowerCase();
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                
                $.each(data.result, function(i, res) {
                    //console.log(res.type, res.views);
                    var type = res.type;

                    _Crud.view[type]        = defaultView;
                    _Crud.sort[type]        = defaultSort;
                    _Crud.order[type]       = defaultOrder;
                    _Crud.pageSize[type]    = defaultPageSize;
                    _Crud.page[type]        = defaultPage;

                    _Crud.schema[type] = res;
                    //console.log('Type definition for ' + type + ' loaded');
                    //console.log('schema loaded?', _Crud.isSchemaLoaded());
                    
                    if (_Crud.isSchemaLoaded()) {
                        
                        console.log('Schema loaded successfully');
        
                        if (browser) _Crud.initTabs();
                    
                    }
                    
                });
                
            }
            
            
        });        
    },
    
    addTab : function(type) {
        var res = _Crud.schema[type];
        $('#resourceTabsMenu').append('<li><a href="#' +  type + '"><span>' + _Crud.formatKey(type) + '</span></a></li>');
        $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
        var typeNode = $('#' + type);
        _Crud.addPager(type, typeNode, _Crud.pageSize[type], _Crud.page[type]);
        typeNode.append('<table></table>');
        var table = $('table', typeNode);
        table.append('<tr></tr>');
        var tableHeader = $('tr:first-child', table);
        var view = res.views[_Crud.view[type]];
                    
        if (view) {
            var k = Object.keys(view);
            if (k && k.length) {
                _Crud.keys[type] = k;
                $.each(_Crud.keys[type], function(k, key) {
                    tableHeader.append('<th class="' + view[key].jsonName + '">' + _Crud.formatKey(view[key].jsonName) + '</th>');
                //tableHeader.append('<th>' + key + '</th>');
                });
                tableHeader.append('<th>Actions</th>');
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

        if (type == _Crud.type) {
            _Crud.activateList(_Crud.type);
            _Crud.activatePagerElements(_Crud.type);
        }

    },
    
    /**
     * Return the REST endpoint for the given type
     */
    restType : function(type) {
        var typeDef = _Crud.schema[type];
        return typeDef.url.substring(1);
    },
    
    /**
     * Return true if the combination of the given property key
     * and the given type is a collection
     */
    isCollection : function(key, type) {
        var typeDef = _Crud.schema[type];
        var view = typeDef.views[_Crud.view[type]];
        return view[key].isCollection;
    },
    
    /**
     * Return the related type of the given property key
     * of the given type
     */
    relatedType : function(key, type) {
        var typeDef = _Crud.schema[type];
        var view = typeDef.views[_Crud.view[type]];
        return view[key].relatedType;
    },

    addPager : function(type, el, pageSize, page) {
        el.append('<div style="clear: both"><button id="pageLeft">&lt; Prev</button>'
            + ' Page <input id="page" type="text" size="3" value="' + page + '"><button id="pageRight">Next &gt;</button> of <input class="readonly" readonly="readonly" id="pageCount" size="3" value="' + nvl(_Crud.pageCount, 0) + '">'
            + ' Page Size: <input id="pageSize" type="text" size="3" value="' + pageSize + '"></div>');
        
    },

    replaceSortHeader : function(type) {
        var table = _Crud.getTable(type);
        var newOrder = (_Crud.order[type] && _Crud.order[type] == 'desc' ? 'asc' : 'desc');
        $('th', table).each(function(i, t) {
            var th = $(t);
            var key = th.attr('class');
            if (key != 'Actions') {
                $('a', th).off('click');
                th.empty();
                th.append('<a href="' + _Crud.sortAndPagingParameters(key, newOrder, _Crud.pageSize[type], _Crud.page[type]) + '#' + type + '">' + _Crud.formatKey(key) + '</a>');
                $('a', th).on('click', function(event) {
                    event.preventDefault();
                    _Crud.sort[type] = key;
                    _Crud.order[type] = (_Crud.order[type] && _Crud.order[type] == 'desc' ? 'asc' : 'desc');
                    _Crud.activateList(type);
                    _Crud.updateUrl(type);
                    return false;
                });
            }
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
        //        console.log('activateList', type, _Crud.view[type], _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
        //        console.log($('#' + type), $('#' + type).attr('data-url'));
        //var url  = $('#' + type).attr('data-url').substring(1);
        var url = _Crud.restType(type);
        
        url = rootUrl + url + '/' + _Crud.view[type] + _Crud.sortAndPagingParameters(_Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
        //console.log('activateList', url);
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
        var table = _Crud.getTable(type);
        //console.log('list', type, url, table);
        table.append('<tr></tr>');
        _Crud.clearList(type);
        var headers = {
            'X-StructrSessionToken' : token
        };
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
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
        var url  = rootUrl + '/' + $('#' + type).attr('data-url') + '/' + _Crud.view[type] + _Crud.sortAndPagingParameters(_Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
        //console.log(b);
        _Crud.dialog('Export ' + type + ' list as CSV', function() {
            }, function() {
            });
        dialogText.append('<textarea class="exportArea"></textarea>');
        var exportArea = $('.exportArea', dialogText);

        dialogBtn.append('<button id="copyToClipboard">Copy to Clipboard</button>');
    
        $('#copyToClipboard', dialogBtn).on('click', function() {
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
        var headers = {
            'X-StructrSessionToken' : token
        };
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
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
    
        _Crud.page[type] = p;
        $('#page', typeNode).val(_Crud.page[type]);
    
        _Crud.pageCount = pc;
        $('#pageCount', typeNode).val(_Crud.pageCount);

        _Crud.updateUrl(type);
    },

    activatePagerElements : function(type) {
        //console.log('activatePagerElements', type);
        var typeNode = $('#' + type);
        $('#page', typeNode).on('keypress', function(e) {
            if (e.keyCode == 13) {
                _Crud.page[type] = $(this).val();
                _Crud.activateList(type);
            }
        });
                
        $('#pageSize', typeNode).on('keypress', function(e) {
            if (e.keyCode == 13) {
                _Crud.pageSize[type] = $(this).val();
                _Crud.activateList(type);
            }
        });

        $('#pageLeft', typeNode).on('click', function() {
            if (_Crud.page[type] > 1) {
                _Crud.page[type]--;
                _Crud.activateList(type);
            }
        });
                
        $('#pageRight', typeNode).on('click', function() {
            if (_Crud.page[type] < _Crud.pageCount) {
                _Crud.page[type]++;
                _Crud.activateList(type);
            }
        });
    
    },
    
    deActivatePagerElements : function(type) {
        //console.log('deActivatePagerElements', type);
        var typeNode = $('#' + type);
        
        $('#page', typeNode).off('keypress');
        $('#pageSize', typeNode).off('keypress');
        $('#pageLeft', typeNode).off('click');
        $('#pageRight', typeNode).off('click');
    },

    crudRead : function(type, id) {
        console.log('headers', headers);
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view[type],
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
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

    crudEdit : function(id) {
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view[_Crud.type],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                var type = data.result.type;
                //console.log('type', type);
                _Crud.dialog('Edit ' + type + ' ' + id, function() {
                    //console.log('ok')
                    }, function() {
                    //console.log('cancel')
                    });                
                _Crud.showDetails(data.result, dialogText, false, false);
            //_Crud.populateForm($('#entityForm'), data.result);
            }
        });
    },

    crudCreate : function(type, url, json, onSuccess, onError) {
        $.ajax({
            url: rootUrl + url,
            headers: headers,
            type: 'POST',
            dataType: 'json',
            data: json,
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data, status, xhr) {
                if (onSuccess) {
                    onSuccess();
                } else {
                    var location = xhr.getResponseHeader('location');
                    var id = location.substring(location.lastIndexOf('/') + 1);
                    _Crud.crudRead(type, id);
                    $.unblockUI({
                        fadeOut: 25
                    });
                }
            },
            error: function(data, status, xhr) {
                if (onError) {
                    onError();
                } else {
                    //console.log(data, status, xhr);
                    _Crud.dialog('Create new ' + type, function() {
                        //console.log('ok')
                        }, function() {
                        //console.log('cancel')
                        });
                    _Crud.showDetails(null, dialogText, false, true, type);
                }
            }
        });
    },

    crudRefresh : function(id, key) {
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view[_Crud.type],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log('refresh', id, key, data.result[key]);
                _Crud.refreshCell(id, key, data.result[key]);
            }
        });
    },

    crudReset : function(id, key) {
        $.ajax({
            url: rootUrl + '/' + id + '/' + _Crud.view[_Crud.type],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log('reset', id, key, data.result[key]);
                _Crud.resetCell(id, key, data.result[key]);
            }
        });
    },

    crudUpdateObj : function(id, json) {
        $.ajax({
            url: rootUrl + '/' + id,
            headers: headers,
            data: json,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function() {
                document.reload();
            },
            error: function(data, status, xhr) {
            //_Crud.crudReset(id);
            //alert(data.responseText);
            // TODO: Overlay with error info
            }
        });
    },

    crudUpdate : function(id, key, newValue, onSuccess, onError) {
        $.ajax({
            url: rootUrl + '/' + id,
            headers: headers,
            data: '{"' + key + '":"' + newValue + '"}',
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function() {
                if (onSuccess) {
                    onSuccess();
                } else {
                    _Crud.crudRefresh(id, key);
                }
            },
            error: function(data, status, xhr) {
                if (onError) {
                    onError();
                } else {
                    _Crud.crudReset(id, key);
                }
            //alert(data.responseText);
            // TODO: Overlay with error info
            }
        });
    },
    
    crudRemoveProperty : function(id, key, onSuccess, onError) {
        $.ajax({
            url: rootUrl + '/' + id,
            headers: headers,
            data: '{"' + key + '":null}',
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function() {
                if (onSuccess) {
                    onSuccess();
                } else {
                    _Crud.crudRefresh(id, key);
                }
            },
            error: function(data, status, xhr) {
                if (onError) {
                    onError();
                } else {
                    _Crud.crudReset(id, key);
                }
            //alert(data.responseText);
            // TODO: Overlay with error info
            }
        });
    },

    crudDelete : function(id) {
        $.ajax({
            url: rootUrl + '/' + id,
            headers: headers,
            type: 'DELETE',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function() {
                //alert(id + ' deleted!');
                var row = $('#' + id);
                row.remove();
            }
        });
    },

    populateForm : function(form, node) {
        //console.log(form, node);
        var fields = $('input', form);
        form.attr('data-id', node.id);
        $.each(fields, function(f, field) {
            var value = formatValue(node[field.name], field.name, node.type, node.id);
            //console.log(field, field.name, value);
            $('input[name="' + field.name + '"]').val(value);
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
        c.text(nvl(formatValue(oldValue), ''));
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
            //console.log('oldValue', oldValue);
            self.off('mouseup');
            self.html(formatValueInputField(key, oldValue));
            _Crud.activateTextInputField($('input', self), id, key);
        });
    },

    refreshCell : function(id, key, newValue) {
        //    console.log('refreshCell', id, key, newValue);
        var c = _Crud.cell(id, key);
        //c.empty();
        c.text(newValue);
        //console.log('newValue', newValue);
        _Crud.populateCell(id, key, _Crud.type, newValue, c);
        
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
            var oldValue = newValue;
            //console.log('oldValue', oldValue);
            self.off('mouseup');
            self.html('<input class="value" type="text" size="40" value="' + oldValue + '">');
            _Crud.activateTextInputField($('input', self), id, key);
        });
    },

    activateTextInputField : function(input, id, key) {
        input.off('mouseup');
        //console.log('activateTextInputField', input, id, key);
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
            //console.log(type);
            $.each(_Crud.keys[type], function(k, key) {
                row.append('<td class="' + key + '"></td>');
                var el = _Crud.cell(id, key);
                _Crud.populateCell(id, key, type, item[key], el);
            });
            row.append('<td class="actions"><span class="edit"><button><img src="icon/pencil.png"> Edit</span></button><button><span class="delete"><img src="icon/cross.png"> Delete</span></button></td>');
            $('.actions .edit', row).on('mouseup', function(event) {
                event.preventDefault();
                _Crud.crudEdit(id);
                return false;
            });
            $('.actions .delete', row).on('mouseup', function(event) {
                event.preventDefault();
                var c = confirm('Are you sure to delete ' + type + ' ' + id + ' ?');
                if (c == true) {
                    _Crud.crudDelete(id);
                }
            });
        }
    },

    populateCell : function(id, key, type, value, el) {

        var isCollection = _Crud.isCollection(key, type);
        var relatedType  = _Crud.relatedType(key, type);
        var simpleType;

        if (!relatedType) {

            //console.log('single value, no related type');
            el.text(nvl(formatValue(value),'')).on('mouseup', function(event) {
                event.preventDefault();
                var self = $(this);
                var oldValue = self.text();
                self.off('mouseup');
                self.html('<input class="value" type="text" size="40" value="' + oldValue + '">');
                _Crud.activateTextInputField($('input', self), id, key);
            });

        } else if (isCollection) {
                    
            simpleType = lastPart(relatedType, '.');
                    
            $.each(value, function(v, relatedId) {
                _Crud.getAndAppendNode(type, id, key, relatedId, el);
            });
            //cell.append('<div style="clear:both"><img class="add" src="icon/add_grey.png"></div>');
            el.append('<img class="add" src="icon/add_grey.png">');
            $('.add', el).on('click', function() {
                _Crud.dialog('Add ' + simpleType, function() {}, function() {});
                _Crud.displaySearch(type, id, key, simpleType, dialogText);
            });

        } else {
                    
            simpleType = lastPart(relatedType, '.');
                    
            if (value) {
                    
                _Crud.getAndAppendNode(type, id, key, value, el);
                        
            } else {
                //cell.append('<div style="clear:both"><img class="add" src="icon/add_grey.png"></div>');
                el.append('<img class="add" src="icon/add_grey.png">');
                $('.add', el).on('click', function() {
                    _Crud.dialog('Add ' + simpleType + ' to ' + key, function() {}, function() {});
                    _Crud.displaySearch(type, id, key, simpleType, dialogText);
                });
            }

        }
        searchField.focus();
    },

    getAndAppendNode : function(parentType, parentId, key, id, cell) {
        console.log('headers', headers);
        if (!id) return;
        $.ajax({
            url: rootUrl + id + '/' + _Crud.view[_Crud.type],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                var node = data.result;
                //console.log('node', node);
                cell.append('<div id="_' + node.id + '" class="node ' + node.type.toLowerCase() + ' ' + node.id + '_">' + fitStringToSize(node.name, 120) + '<img class="remove" src="icon/cross_small_grey.png"></div>');
                var nodeEl = $('#_' + node.id, cell);
                //console.log(node);
                if (node.type == 'Image') {
                    nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.name + '"></div>');
                }
                $('.remove', nodeEl).on('click', function(e) {
                    e.preventDefault();
                    _Crud.removeRelatedObject(parentType, parentId, key, id);
                    return false;
                });
                nodeEl.on('click', function(e) {
                    e.preventDefault();
                    _Crud.showDetails(node, dialogText, true, false); // readonly form
                    return false;
                });
            }
        });
  
    },
    
    getAndAppendNodes : function(parentType, id, key, type, el, pageSize) {
        var urlType  = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var view = 'public'; // _Crud.view[_Crud.type]
        var url = rootUrl + urlType + '/' + view + _Crud.sortAndPagingParameters('name', 'asc', pageSize, 1);
        $.ajax({
            url: url,
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                
                $.each(data.result, function(i, node) {
                
                    //console.log('node', node);
                    el.append('<div id="_' + node.id + '" class="node ' + node.type.toLowerCase() + ' ' + node.id + '_">' + fitStringToSize(node.name, 120) + '</div>');
                
                    var nodeEl = $('#_' + node.id, el);
                    //console.log(node);
                    if (node.type == 'Image') {
                        nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.name + '"></div>');
                    }
                    
                    nodeEl.on('click', function(e) {
                        e.preventDefault();
                        _Crud.addRelatedObject(parentType, id, key, node.id, true); // with reload
                        return false;
                    });
                
                });
            }
        });
  
    },
    
    clearSearch : function() {
        if (_Crud.clearSearchResults()) {
            $('#clearSearchIcon').hide().off('click');
            $('#search').val('');
            main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');
            _Crud.initTabs();
            _Crud.updateUrl(_Crud.type);
        }
    },
    
    clearSearchResults : function() {
        var searchResults = $('#searchResults', main);
        if (searchResults.length) {
            searchResults.remove();
            $('#searchResultsTitle').remove();
            return true;
        }
        return false;
    },
    
    search : function(searchString) {

        _Crud.clearSearchResults();

        main.append('<h2 id="searchResultsTitle">Search Results</h2>');
        main.append('<div id="searchResults"></div>');
        searchResults = $('#searchResults', main);
        
        $('input', searchBox).select();
        
        var view = 'public'; // _Crud.view[_Crud.type]
        
        $.each(_Crud.types, function(t, type) {
            
            var url = rootUrl + _Crud.restType(type) + '/' + view + _Crud.sortAndPagingParameters('name', 'asc', pageSize, 1) + '&name=' + searchString + '&loose=1';
            
            //console.log(url);
            
            $.ajax({
                url: url,
                headers: headers,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {

                    if (data.result.length) {
                        searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox"><h3>' + type.capitalize() + '</h3></div>');
                    }
                
                    $.each(data.result, function(i, node) {
                
                        //console.log('node', node);
                        $('#resultsFor' + type, searchResults).append('<div id="_' + node.id + '" class="node ' + node.type.toLowerCase() + ' ' + node.id + '_">' + fitStringToSize(node.name, 120) + '</div>');
                
                        var nodeEl = $('#_' + node.id, searchResults);
                        //console.log(node);
                        if (node.type == 'Image') {
                            nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.name + '"></div>');
                        }
                    
                        nodeEl.on('click', function(e) {
                            e.preventDefault();
                            _Crud.showDetails(node, dialogText, true, false); // readonly form
                            return false;
                        });
                
                    });
                }
            });
        
        });

        
    },

    displaySearch : function(parentType, id, key, type, el) {
        //        el.append('<form id="searchForm">Search: <input class="search" type="search" value=""></form>');
        //        var form = $('#searchForm', el);
        //_Crud.addPager(type, el, 50, 1);
        el.append('<div id="relatedNodesList"></div>');
        var relatedNodesList = $('#relatedNodesList', el);
        _Crud.getAndAppendNodes(parentType, id, key, type, relatedNodesList, 100, false);
    //_Crud.activatePagerElements(type);
    //        $('.search', form).on('submit', function() {
    //            var searchTerm = $(this).val();
    //            //console.log(searchTerm);
    //        });
    },

    removeRelatedObject : function(type, id, key, relatedId) {
        var urlType  = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var url = rootUrl + urlType + '/' + id;
        if (_Crud.isCollection(key, type)) {
            var ids = [];
            $.ajax({
                url: url,
                headers: headers,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    //console.log(data.result[key]);
                    $.each(data.result[key], function(i, id) {
                        if (id != relatedId) {
                            ids.push(id);
                        }
                    });
                    $.ajax({
                        url: url,
                        headers: headers,
                        type: 'PUT',
                        dataType: 'json',
                        data: '{"' + key + '":null}',
                        contentType: 'application/json; charset=utf-8',
                        //async: false,
                        success: function(data) {
                            $.ajax({
                                url: url,
                                headers: headers,
                                type: 'PUT',
                                dataType: 'json',
                                data: '{"' + key + '":' + JSON.stringify(ids) + '}',
                                contentType: 'application/json; charset=utf-8',
                                //async: false,
                                success: function(data) {
                                    $('#_' + relatedId).remove();
                                }
                            });
                        }
                    });
                }
            });
        } else {
            //console.log(url);
        
            $.ajax({
                url: url,
                headers: headers,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    _Crud.crudRemoveProperty(id, key);
                }
            });
            
        }
    },
    
    addRelatedObject : function(type, id, key, relatedId, reload) {
        var urlType  = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var url = rootUrl + urlType + '/' + id;
        if (_Crud.isCollection(key, type)) {
            var ids = [];
            $.ajax({
                url: url,
                headers: headers,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    //console.log(data.result[key]);
                    $.each(data.result[key], function(i, node) {
                        ids.push(node.id); 
                    });

                    if (!isIn(relatedId, ids)) {
                        ids.push(relatedId);
                    }
                    $.ajax({
                        url: url,
                        headers: headers,
                        type: 'PUT',
                        dataType: 'json',
                        data: '{"' + key + '":' + JSON.stringify(ids) + '}',
                        contentType: 'application/json; charset=utf-8',
                        //async: false,
                        success: function(data) {
                            if (reload) document.location.reload();
                        }
                    });

                }
            });
        } else {
            $.ajax({
                url: url,
                headers: headers,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    //console.log(data.result);
                    //var value = data.result[key];
                    _Crud.crudUpdate(id, key, relatedId, reload ? document.location.reload() : function() {});

                }
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
                dialogSaveButton.remove();
                $('#saveProperties').remove();
                searchField.focus();
            });
            $.blockUI.defaults.overlayCSS.opacity = .6;
            $.blockUI.defaults.applyPlatformOpacityRules = false;
            $.blockUI({
                fadeIn: 25,
                fadeOut: 25,
                message: dialogBox,
                css: {
                    border: 'none',
                    backgroundColor: 'transparent'
                }
            });
        }
    },

    error : function(text, callback) {
        if (text) $('#errorBox .errorText').html('<img src="icon/error.png"> ' + text);
        //console.log(callback);
        if (callback) $('#errorBox .okButton').on('click', function(e) {
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

    showDetails : function(node, el, readonly, create, typeOnCreate) {

        var type = (node ? node.type : typeOnCreate);
        //        console.log(t);
        //        var type  = $('#' + t).attr('data-url').substring(1);
        //        console.log(type);
        ////        var typeDef = _Crud.schema[node.type];
        //        console.log(typeDef);
        var typeDef = _Crud.schema[type];
        
        if (node) {
            //console.log('Edit node', node);
            _Crud.dialog('Details of ' + node.type + ' ' + node.name + '<span class="id"> [' + node.id + ']</span>', function() {}, function() {});
        } else {
            //console.log('Create new node of type', typeOnCreate);
            _Crud.dialog('Create new ' + typeOnCreate, function() {}, function() {});
        }
        
        //console.log('readonly', readonly);
        
        if (!readonly) {
            //console.log('edit mode, appending form');
            $(el).append('<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th><th>Type</th><th>Read Only</th></table></form>');
        } else {
            $(el).append('<table class="props"><tr><th>Name</th><th>Value</th><th>Type</th><th>Read Only</th></table>');
        }
        
        var table = $('table', $(el));
        
        $.each(typeDef.views[_Crud.view[type]], function(i, property) {
            //console.log(property);
            var type = property.className.substring(property.className.lastIndexOf('.') + 1);
            var key = property.jsonName;
            table.append('<tr class="' + key + '"><td class="key"><label for="' + key + '">' + _Crud.formatKey(key) + '</label></td><td class="value"></td><td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
            var cell = $('.value', $('.' + key, table));
            if (node && node.id) {
                //console.log(node.id, key, type, node[key], cell);
                _Crud.populateCell(node.id, key, node.type, node[key], cell);
            } else {
                //console.log(key,node[key]);
                cell.append(formatValueInputField(key, ''));
            }
        });
        
        dialogSaveButton.remove();
        if (!readonly) {
            dialogBox.append('<button class="save" id="saveProperties">Save</button>');
            $('.save', $('#dialogBox')).on('click', function() {
                var form = $('#entityForm');
                var json = JSON.stringify(serializeObject(form));
                //console.log(json);
                dialogText.empty();
                if (create) {
                    _Crud.crudCreate(typeOnCreate, typeDef.url, json);
                } else {
                    var id = form.attr('data-id');
                    //console.log('updating', id);
                    _Crud.crudUpdateObj(id, json);
                }
            });
        }
        
        if (node && node.type == 'Image') {
            dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.name + '"></div></div>');
        }
        
    },

    formatKey : function(text) {
        if (!text) return '';
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
    },

    serializeObject : function(obj) {
        var o = {};
        var a = obj.serializeArray();
        $.each(a, function() {
            if (this.value && this.value != '') {
                if (o[this.name]) {
                    if (!o[this.name].push) {
                        o[this.name] = [o[this.name]];
                    }
                    o[this.name].push(this.value || '');
                } else {
                    o[this.name] = this.value || '';
                }
            }
        });
        return o;
    },
    
    displayError : function(errorText) {
        var msgEl = $('.dialogMsg', $('#dialogBox'));
        msgEl.empty();
        msgEl.text(errorText);
    }
    
}

// Hook for nodejs
if (typeof module == 'object') {
    var $ = require('jquery');
    var token = '';
    var rootUrl = 'http://localhost:8180/structr/rest/';
    module.exports = _Crud;
    
    function nvl(value, defaultValue) {
        var returnValue;
        if (value === undefined) {
            returnValue = defaultValue;
        } else if (value === false) {
            returnValue = 'false';
        } else if (!value) {
            returnValue = '';
        }
        else {
            returnValue = value;
        }
        return returnValue;
    }
    
    function isIn(id, ids) {
        return ($.inArray(id, ids) > -1);
    }

    
}