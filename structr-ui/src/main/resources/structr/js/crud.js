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

var defaultType, defaultView, defaultSort, defaultOrder, defaultPage, defaultPageSize;
var searchField;

var browser = (typeof document === 'object');
var headers;
var crudPagerDataCookieName = 'structrCrudPagerData_' + port + '_';
var crudTypeCookieName = 'structrCrudType_' + port;

if (browser) {
    
    $(document).ready(function() {

        defaultType     = 'Page';
        defaultView     = 'ui';
        defaultSort     = '';
        defaultOrder    = '';
                
        defaultPage     = 1;
        defaultPageSize = 10;
        
        main = $('#main');
 
        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');
        
        //        console.log(_Crud.type);
        
        if (!_Crud.type) {
            _Crud.restoreTypeFromCookie();
        //            console.log(_Crud.type);
        }

        if (!_Crud.type) {
            _Crud.type = urlParam('type');
        //            console.log(_Crud.type);
        }
        
        if (!_Crud.type) {
            _Crud.type = defaultType;
        //            console.log(_Crud.type);
        }
        
        _Crud.resize();
        $(window).on('resize', function() {
            _Crud.resize();
        });
        
    });
    
} else {
    defaultView = 'public';
}

var _Crud = {
    
    schemaLoading : false,
    schemaLoaded : false,
    
    //allTypes : [],
    
    types : [],//'Page', 'User', 'Group', 'Folder', 'File', 'Image', 'Content', 'PropertyDefinition' ],
    views : [ 'public', 'all', 'ui' ],

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

        _Crud.schemaLoading = false;
        _Crud.schemaLoaded = false;
        _Crud.schema = [];
        _Crud.keys = [];

        _Crud.loadSchema(function() {
            if (browser) _Crud.initTabs();
        });

        main.append('<div class="searchBox"><input class="search" name="search" size="20" placeholder="Search"><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');
        searchField = $('.search', main);
        searchField.focus();
        
        searchField.keyup(function(e) {
            var searchString = $(this).val();
            if (searchString && searchString.length && e.keyCode === 13) {
                
                $('.clearSearchIcon').show().on('click', function() {
                    _Crud.clearSearch(main);
                });
               
                _Crud.search(searchString, main, null, function(e, node) {
                    e.preventDefault();
                    _Crud.showDetails(node, false, node.type);
                    return false;
                });
                
                $('#resourceTabs', main).remove();
                $('#resourceBox', main).remove();
                
            } else if (e.keyCode === 27 || searchString === '') {
                
                _Crud.clearSearch(main);                
                
            }
            
            
        });
        
        main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');
//        $('#resourceTabsMenu').append('<li id="addResourceTab" class="ui-state-default ui-corner-top" role="tab"><span><img src="icon/add.png"></span></li>');
//        $('#addResourceTab', $('#resourceTabsMenu')).on('click', function() {
//           _Crud.addResource();
//        });
        
        $(document).keyup(function(e) {
            if (e.keyCode === 27) {
                dialogCancelButton.click();
            }
        });
    },
    
    onload : function() {
        
        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');

        if (token) {

            headers = {
                'X-StructrSessionToken' : token
            };
        }

        // check for single edit mode
        var id = urlParam('id');
        if (id) {
            //console.log('edit mode, editing ', id);
            _Crud.loadSchema(function() {
                _Crud.crudRead(null, id, function(node) {
                    //console.log(node, _Crud.view[node.type]);
                    _Crud.showDetails(node, false, node.type);
                });
            });
            
        } else {
            _Crud.init();
        }
    
    },

    initTabs : function() {
        
        $.each(_Crud.types, function(t, type) {
            _Crud.addTab(type);
        });

        $('#resourceTabs').tabs({
            activate: function(event, ui) {
                var newType = ui.newPanel[0].id;
                //console.log('deactivated', _Crud.type, 'activated', newType);
                var typeNode = $('#' + _Crud.type);
                var pagerNode = $('.pager', typeNode);
                _Crud.deActivatePagerElements(pagerNode);
                _Crud.activateList(newType);
                typeNode = $('#' + newType);
                pagerNode = $('.pager', typeNode);
                _Crud.activatePagerElements(newType, pagerNode);
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
                all &= (_Crud.schema[type] && _Crud.schema[type] !== null);
            });
        }
        _Crud.schemaLoaded = all;
        return _Crud.schemaLoaded;
    },

    updateUrl : function(type) {
        //console.log('updateUrl', type, _Crud.pageSize[type], _Crud.page[type]);
        
        if (type) {
            _Crud.storeTypeInCookie();
            _Crud.storePagerDataInCookie();
            //window.history.pushState('', '', _Crud.sortAndPagingParameters(_Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]) + '&view=' + _Crud.view[type] + '&type=' + type + '#crud');
            window.location.hash = '#crud';
        }
        searchField.focus();

    },

    /**
     * Read the schema from the _schema REST resource and call 'callback'
     * after the complete schema is loaded.
     */
    loadSchema : function(callback) {
        
        // Avoid duplicate loading of schema
        if (_Crud.schemaLoading) {
            return;
        }
        _Crud.schemaLoading = true;
        
        _Crud.loadAccessibleResources(function() {
            $.each(_Crud.types, function(t, type) {
                //console.log('Loading type definition for ' + type + '...');
                _Crud.loadTypeDefinition(type, callback);
            });
        });
        
    },

    loadAccessibleResources : function(callback) {
        var url = rootUrl + 'resource_access/ui';
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log(data);
                var types = [];
                $.each(data.result, function(i, res) {
                    var type = getTypeFromResourceSignature(res.signature);
                    //console.log(res);
                    if (!isIn(type, _Crud.types)) {
                        types.push({'type': type, 'position': res.position});
                    }
                });
                //console.log(types);
                types.sort(function(a, b) {
                    return a.position - b.position;
                });

                $.each(types, function(i, typeObj) {
                    _Crud.types.push(typeObj.type);
                });

                if (callback) {
                    callback();
                }
                
            }
        });
    },

    loadTypeDefinition : function(type, callback) {
        
        //_Crud.schema[type] = [];
        
        var url = rootUrl + '_schema/' + type.toUnderscore();
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                
                $.each(data.result, function(i, res) {
                    //console.log(res);
                    var type = res.type;

                    _Crud.determinePagerData(type);

                    _Crud.schema[type] = res;
                    //console.log('Type definition for ' + type + ' loaded');
                    //console.log('schema loaded?', _Crud.isSchemaLoaded());
                    
                    if (_Crud.isSchemaLoaded()) {
                        //console.log('Schema loaded successfully');
                        if (callback) {
                            callback();
                        }
                    }
                    
                });
                
            }
            
            
        });        
    },
    
    determinePagerData : function(type) {
        
        // Priority: JS vars -> Cookie -> URL -> Default

        if (!_Crud.view[type]) {
            _Crud.view[type]        = urlParam('view');
            _Crud.sort[type]        = urlParam('sort');
            _Crud.order[type]       = urlParam('order');
            _Crud.pageSize[type]    = urlParam('pageSize');
            _Crud.page[type]        = urlParam('page');
        }
        
        if (!_Crud.view[type]) {
            _Crud.restorePagerDataFromCookie();
        }

        if (!_Crud.view[type]) {
            _Crud.view[type]        = defaultView;
            _Crud.sort[type]        = defaultSort;
            _Crud.order[type]       = defaultOrder;
            _Crud.pageSize[type]    = defaultPageSize;
            _Crud.page[type]        = defaultPage;
        }
    },
    
    addTab : function(type) {
        var res = _Crud.schema[type];
        $('#resourceTabsMenu').append('<li><a href="#' +  type + '"><span>' + _Crud.formatKey(type) + '</span></a></li>');
        $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
        var typeNode = $('#' + type);
        var pagerNode = _Crud.addPager(type, typeNode);
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
                    tableHeader.append('<th class="' + key + '">' + _Crud.formatKey(key) + '</th>');
                });
                tableHeader.append('<th>Actions</th>');
            }
        }
        typeNode.append('<div class="infoFooter">Query: <span class="queryTime"></span> s &nbsp; Serialization: <span class="serTime"></span> s</div>');                   
        typeNode.append('<button id="create' + type + '"><img src="icon/add.png"> Create new ' + type + '</button>');
        typeNode.append('<button id="export' + type + '"><img src="icon/database_table.png"> Export as CSV</button>');
                    
        $('#create' + type, typeNode).on('click', function() {
            _Crud.crudCreate(type, res.url.substring(1));
        });
        $('#export' + type, typeNode).on('click', function() {
            _Crud.crudExport(type);
        });

        if (type === _Crud.type) {
            _Crud.activateList(_Crud.type);
            _Crud.activatePagerElements(_Crud.type, pagerNode);
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
        return (view && view[key] && view[key].isCollection);
    },
    
    /**
     * Return the related type of the given property key
     * of the given type
     */
    relatedType : function(key, type) {
        var typeDef = _Crud.schema[type];
        var view = typeDef.views[_Crud.view[type]];
        return (view && view[key] ? view[key].relatedType : null);
    },
    
    /**
     * Append a pager for the given type to the given DOM element.
     */
    addPager : function(type, el) {
        
        if (!_Crud.page[type]) {
            _Crud.page[type]     = urlParam('page') ? urlParam('page') : (defaultPage ? defaultPage : 1);
        }
        
        if (!_Crud.pageSize[type]) {
            _Crud.pageSize[type] = urlParam('pageSize') ? urlParam('pageSize') : (defaultPageSize ? defaultPageSize : 10);
        }
 
        el.append('<div class="pager" style="clear: both"><button class="pageLeft">&lt; Prev</button>'
            + ' Page <input class="page" type="text" size="3" value="' + _Crud.page[type] + '"><button class="pageRight">Next &gt;</button> of <input class="readonly pageCount" readonly="readonly" size="3" value="' + nvl(_Crud.pageCount, 0) + '">'
            + ' Page Size: <input class="pageSize" type="text" size="3" value="' + _Crud.pageSize[type] + '"></div>');
        
        return $('.pager', el);
        
    },
    
    storeTypeInCookie : function() {
        $.cookie(crudTypeCookieName, _Crud.type);
    },
    
    restoreTypeFromCookie : function() {
        var cookie = $.cookie(crudTypeCookieName);
        if (cookie) {
            _Crud.type = cookie;
        //console.log('Current type from Cookie', cookie);
        }
    },
    
    storePagerDataInCookie : function() {
        var type = _Crud.type;
        var pagerData = _Crud.view[type] + ',' + _Crud.sort[type] + ',' + _Crud.order[type] + ',' + _Crud.page[type] + ',' + _Crud.pageSize[type];
        //console.log('pager data to store in cookie: ', pagerData);
        $.cookie(crudPagerDataCookieName + type, pagerData);
    },
    
    restorePagerDataFromCookie : function() {
        var type = _Crud.type;
        var cookie = $.cookie(crudPagerDataCookieName + type);
        
        if (cookie) {
            var pagerData = cookie.split(',');
            //console.log('Pager Data from Cookie', pagerData);
            _Crud.view[type]      = pagerData[0];
            _Crud.sort[type]      = pagerData[1];
            _Crud.order[type]     = pagerData[2];
            _Crud.page[type]      = pagerData[3];
            _Crud.pageSize[type]  = pagerData[4];
        }
    },

    replaceSortHeader : function(type) {
        var table = _Crud.getTable(type);
        var newOrder = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
        var res = _Crud.schema[type];
        var view = res.views[_Crud.view[type]];
        $('th', table).each(function(i, t) {
            var th = $(t);
            var key = th.attr('class');
            if (key !== 'Actions') {
                $('a', th).off('click');
                th.empty();
                if (view[key]) {
                    var sortKey = view[key].jsonName;
                    th.append('<a href="' + _Crud.sortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type]) + '#' + type + '">' + _Crud.formatKey(key) + '</a>');
                    $('a', th).on('click', function(event) {
                        event.preventDefault();
                        _Crud.sort[type] = key;
                        _Crud.order[type] = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
                        _Crud.activateList(type);
                        _Crud.updateUrl(type);
                        return false;
                    });
                }
            }
        });
    },

    sortAndPagingParameters : function(t,s,o,ps,p) {
        var typeParam = (t ? 'type=' + t : '');
        var sortParam = (s ? 'sort=' + s : '');
        var orderParam = (o ? 'order=' + o : '');
        var pageSizeParam = (ps ? 'pageSize=' + ps : '');
        var pageParam = (p ? 'page=' + p : '');
    
        var params = (typeParam ? '?' + typeParam : '');
        params = params + (sortParam ? (params.length?'&':'?') + sortParam : '');
        params = params + (orderParam ? (params.length?'&':'?') + orderParam : '');
        params = params + (pageSizeParam ? (params.length?'&':'?') + pageSizeParam : '');
        params = params + (pageParam ? (params.length?'&':'?') + pageParam : '');
    
        return params;
    },

    activateList : function(type) {
        var url = rootUrl + _Crud.restType(type) + '/' + _Crud.view[type] + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
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
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log(data);
                $.each(data.result, function(i, item) {
                    if (item.type === type) {
                        _Crud.appendRow(type, item);
                    }
                });
                _Crud.updatePager(type, data.query_time, data.serialization_time, data.page_size, data.page, data.page_count);
                _Crud.replaceSortHeader(type);
            },
            error : function(a,b,c) {
                console.log(a,b,c);
            }
        });
    },

//    addResource : function() {
//        _Crud.dialog('Add another Type', function() {
//            }, function() {
//            });
//        dialogText.append('Select Types');
//        $.each(_Crud.allTypes, function(i, type) {
//            dialogText.append('<div id="add-resource-' + type + '" class="button">' + type + '</div>');
//            $('#add-resource-' + type, dialogText).on('click', function() {
//                $.ajax({
//                    url: rootUrl + 'resource_access?signature=' + type,
//                    headers: headers,
//                    data : '{visibleToAuthenticatedUsers:true}',
//                    dataType: 'json',
//                    contentType: 'application/json; charset=utf-8',
//                    method: 'PUT',
//                    success: function(data) {
//                        console.log('success');
//                    },
//                    error : function(a,b,c) {
//                        console.log(a,b,c);
//                    }
//                });                
//            });
//        });
//    },

    crudExport : function(type) {
        var url  = rootUrl + '/' + $('#' + type).attr('data-url') + '/' + _Crud.view[type] + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
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
        $('.queryTime', typeNode).text(qt);
        $('.serTime', typeNode).text(st);
        $('.pageSize', typeNode).val(ps);
    
        _Crud.page[type] = p;
        $('.page', typeNode).val(_Crud.page[type]);
    
        _Crud.pageCount = pc;
        $('.pageCount', typeNode).val(_Crud.pageCount);

        var pageLeft = $('.pageLeft', typeNode);
        var pageRight = $('.pageRight', typeNode);

        if (_Crud.page[type] < 2) {
            pageLeft.attr('disabled', 'disabled').addClass('disabled');
        } else {
            pageLeft.removeAttr('disabled').removeClass('disabled');
        }
        
        if (!_Crud.pageCount || _Crud.pageCount === 0 || (_Crud.page[type] === _Crud.pageCount)) {
            pageRight.attr('disabled', 'disabled').addClass('disabled');
        } else {
            pageRight.removeAttr('disabled').removeClass('disabled');
        }

        _Crud.updateUrl(type);
    },

    activatePagerElements : function(type, pagerNode) {
        //console.log('activatePagerElements', type);
        //        var typeNode = $('#' + type);
        $('.page', pagerNode).on('keypress', function(e) {
            if (e.keyCode === 13) {
                _Crud.page[type] = $(this).val();
                _Crud.activateList(type);
            }
        });
                
        $('.pageSize', pagerNode).on('keypress', function(e) {
            if (e.keyCode === 13) {
                _Crud.pageSize[type] = $(this).val();
                // set page no to 1
                _Crud.page[type] = 1;
                _Crud.activateList(type);
            }
        });

        var pageLeft = $('.pageLeft', pagerNode);
        var pageRight = $('.pageRight', pagerNode);

        pageLeft.on('click', function() {
            pageRight.removeAttr('disabled').removeClass('disabled');
            if (_Crud.page[type] > 1) {
                _Crud.page[type]--;
                _Crud.activateList(type);
            }
        });
                
        pageRight.on('click', function() {
            pageLeft.removeAttr('disabled').removeClass('disabled');
            if (_Crud.page[type] < _Crud.pageCount) {
                _Crud.page[type]++;
                _Crud.activateList(type);
            }
        });
    
    },
    
    deActivatePagerElements : function(pagerNode) {
        //console.log('deActivatePagerElements', type);
        //        var typeNode = $('#' + type);
        
        $('.page', pagerNode).off('keypress');
        $('.pageSize', pagerNode).off('keypress');
        $('.pageLeft', pagerNode).off('click');
        $('.pageRight', pagerNode).off('click');
    },

    crudRead : function(type, id, callback) {
        // use 'ui' view as default to make the 'edit by id' feature work
        var view = (type && _Crud.view[type] ? _Crud.view[type] : 'ui');
        var url = rootUrl + id + '/' + view;
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (callback) {
                    callback(data.result);
                } else {
                    _Crud.appendRow(type, data.result);
                }
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
        var type = _Crud.type;
        $.ajax({
            url: rootUrl + id + '/' + _Crud.view[type],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log('type', type);
                _Crud.dialog('Edit ' + type + ' ' + id, function() {
                    //console.log('ok')
                    }, function() {
                    //console.log('cancel')
                    });                
                _Crud.showDetails(data.result, false, type);
            //_Crud.populateForm($('#entityForm'), data.result);
            }
        });
    },

    crudCreate : function(type, url, json, onSuccess, onError) {
        //console.log('crudCreate', type, url, json);
        $.ajax({
            url: rootUrl + url,
            headers: headers,
            type: 'POST',
            dataType: 'json',
            data: json,
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode : {
                201 : function(xhr) {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        //                        var location = xhr.getResponseHeader('location');
                        //                        var id = location.substring(location.lastIndexOf('/') + 1);
                        //                        _Crud.crudRead(type, id);
                        $.unblockUI({
                            fadeOut: 25
                        });
                        _Crud.activateList(type);
                    //document.location.reload();
                    }
                },
                400 : function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        //console.log(data, status, xhr);
                        _Crud.dialog('Create new ' + type, function() {
                            //console.log('ok')
                            }, function() {
                            //console.log('cancel')
                            });
                        _Crud.showDetails(null, true, type);
                    }
                },
                401 : function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        //console.log(data, status, xhr);
                        _Crud.dialog('Create new ' + type, function() {
                            //console.log('ok')
                            }, function() {
                            //console.log('cancel')
                            });
                        _Crud.showDetails(null, true, type);
                    }
                },
                403 : function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        //console.log(data, status, xhr);
                        _Crud.dialog('Create new ' + type, function() {
                            //console.log('ok')
                            }, function() {
                            //console.log('cancel')
                            });
                        _Crud.showDetails(null, true, type);
                    }
                },
                404 : function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        //console.log(data, status, xhr);
                        _Crud.dialog('Create new ' + type, function() {
                            //console.log('ok')
                            }, function() {
                            //console.log('cancel')
                            });
                        _Crud.showDetails(null, true, type);
                    }
                },
                422 : function(data, status, xhr) {
                    if (onError) {
                        onError();
                    } else {
                        //console.log(data, status, xhr);
                        _Crud.dialog('Create new ' + type, function() {
                            //console.log('ok')
                            }, function() {
                            //console.log('cancel')
                            });
                        _Crud.showDetails(null, true, type);
                        var resp = JSON.parse(data.responseText);
                        console.log(resp);
                        $.each(Object.keys(resp.errors[type]), function(i, key) {
                            var errorMsg = resp.errors[type][key][0];
                           console.log(key, errorMsg); 
                           $('td.' + key + ' input', dialogText).prop('placeholder', errorMsg).css({
                               backgroundColor: '#fee',
                               borderColor: '#933'
                           });
                        });
                        //_Crud.error('Error: ' + data.responseText, true);
                    }
                },
                500 : function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        //console.log(data, status, xhr);
                        _Crud.dialog('Create new ' + type, function() {
                            //console.log('ok')
                            }, function() {
                            //console.log('cancel')
                            });
                        _Crud.showDetails(null, true, type);
                    }
                }
            }
        });
    },

    crudRefresh : function(id, key) {
        var url = rootUrl + id + '/' + _Crud.view[_Crud.type];
        //console.log('crudRefresh', id, key, headers, url);
        
        $.ajax({
            url: url,
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log('refresh', id, key, data.result[key]);
                if (key) {
                    _Crud.refreshCell(id, key, data.result[key]);
                } else {
                    _Crud.refreshRow(id, data.result);
                }
            }
        });
    },

    crudReset : function(id, key) {
        $.ajax({
            url: rootUrl + id + '/' + _Crud.view[_Crud.type],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                console.log('reset', id, key, data.result[key]);
                _Crud.resetCell(id, key, data.result[key]);
            }
        });
    },

    crudUpdateObj : function(id, json, onSuccess, onError) {
        //console.log('crudUpdateObj JSON:', json);
        $.ajax({
            url: rootUrl + id,
            headers: headers,
            data: json,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode : {
                200 : function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id);
                    }
                },
                400 : function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                401 : function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                403 : function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                404 : function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                422 : function(data, status, xhr) {
                    _Crud.error('Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                500 : function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                }
            }
        });
    },

    crudUpdate : function(id, key, newValue, onSuccess, onError) {
        var url = rootUrl + id;
        var json = '{"' + key + '":"' + newValue + '"}';
        //console.log('crudUpdate', headers, url, json);
        $.ajax({
            url: url,
            headers: headers,
            data: json,
            type: 'PUT',
            //dataType: 'json',
            //contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode : {
                200 : function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id, key);
                    }
                },
                400 : function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                401 : function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                403 : function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                404 : function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                422 : function(data, status, xhr) {
                    _Crud.error('Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                500 : function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                }
            }
        //            success: function() {
        //                console.log('crudUpdate success', url, headers, json);
        //                if (onSuccess) {
        //                    onSuccess();
        //                } else {
        //                    _Crud.crudRefresh(id, key);
        //                }
        //            },
        //            error: function(data, status, xhr) {
        //                console.log('crudUpdate error', url, headers, json, data, status, xhr);
        //                // since jQuery 1.9, an empty response body is regarded as an error
        //                // we have to react on this here
        //                if (data.status == 200) {
        //                } else {
        //                    if (onError) {
        //                        onError();
        //                    } else {
        //                        _Crud.crudReset(id, key);
        //                    }
        //                }
        //            //alert(data.responseText);
        //            // TODO: Overlay with error info
        //            }
        });
    },
    
    crudRemoveProperty : function(id, key, onSuccess, onError) {
        
        var url = rootUrl + id;
        var json = '{"' + key + '":null}';
        
        //console.log('crudRemoveProperty', headers, url, json);
        
        $.ajax({
            url: url,
            headers: headers,
            data: json,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode : {
                200 : function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id, key);
                    }
                },
                204 : function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id, key);
                    }
                },
                400 : function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                401 : function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                403 : function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                404 : function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                422 : function(data, status, xhr) {
                    _Crud.error('Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                500 : function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                }
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
            statusCode : {
                200 : function() {
                    var row = $('#' + id);
                    row.remove();
                },
                204 : function() {
                    var row = $('#' + id);
                    row.remove();
                },
                400 : function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                },
                401 : function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                },
                403 : function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                },
                404 : function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                },
                422 : function(data, status, xhr) {
                },
                500 : function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                }
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

    cells : function(id, key) {
        var row = $('#' + id);
        var cellInMainTable = $('.' + key, row);
        
        var cellInDetailsTable;
        var table = $('#details_' + id);
        if (table) {
            cellInDetailsTable = $('.' + key, table);
        }
        
        if (cellInMainTable && !cellInDetailsTable) {
            return [ cellInMainTable ];
        }
        
        if (cellInDetailsTable && !cellInMainTable) {
            return [ cellInDetailsTable ];
        }
        
        return [ cellInMainTable, cellInDetailsTable ];
    },

    resetCell : function(id, key, oldValue) {
        //    console.log('resetCell', id, key, oldValue);
        var cells = _Crud.cells(id, key);
        
        $.each(cells, function(i, cell) {
            cell.empty();
            _Crud.populateCell(id, key, _Crud.type, oldValue, cell);
            //        c.text(nvl(formatValue(oldValue), ''));
            var oldFg = cell.css('color');
            var oldBg = cell.css('backgroundColor');
            //console.log('old colors' , oldFg, oldBg);
            cell.animate({
                color: '#f00',
                backgroundColor: '#fbb'
            }, 250, function() {
                $(this).animate({
                    color: oldFg,
                    backgroundColor: oldBg
                }, 500);
            });
            
        });
        
    },

    refreshCell : function(id, key, newValue) {
        //    console.log('refreshCell', id, key, newValue);
        var cells = _Crud.cells(id, key);
        $.each(cells, function(i, cell) {
            cell.empty();
            _Crud.populateCell(id, key, _Crud.type, newValue, cell);
        
            var oldFg = cell.css('color');
            var oldBg = cell.css('backgroundColor');
            //console.log('old colors' , oldFg, oldBg);
            cell.animate({
                color: '#81ce25',
                backgroundColor: '#efe'
            }, 100, function() {
                $(this).animate({
                    color: oldFg,
                    backgroundColor: oldBg
                }, 500);
            });
        });
    },
    
    refreshRow : function(id, item) {
        //    console.log('refreshCell', id, key, newValue);
        var row = $('#' + id);
        row.empty();
        _Crud.populateRow(id, item);
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
            if (e.keyCode === 13) {
                var newValue = input.val();
                _Crud.crudUpdate(id, key, newValue);
            }
        });
    },
    
    appendRow : function(type, item) {
        var id = item['id'];
        var table = _Crud.getTable(type);
        table.append('<tr id="' + id + '"></tr>');
        _Crud.populateRow(id, item);
    },

    populateRow : function(id, item) {
        var type = item.type;
        var row = $('#' + id);
        if (_Crud.keys[type]) {
            $.each(_Crud.keys[type], function(k, key) {
                row.append('<td class="' + key + '"></td>');
                var cells = _Crud.cells(id, key);
                $.each(cells, function(i, cell) {
                    _Crud.populateCell(id, key, type, item[key], cell);
                });
            });
            row.append('<td class="actions"><button class="edit"><img src="icon/pencil.png"> Edit</button><button class="delete"><img src="icon/cross.png"> Delete</button></td>');
            $('.actions .edit', row).on('mouseup', function(event) {
                event.preventDefault();
                _Crud.crudEdit(id);
                return false;
            });
            $('.actions .delete', row).on('mouseup', function(event) {
                event.preventDefault();
                var c = confirm('Are you sure to delete ' + type + ' ' + id + ' ?');
                if (c === true) {
                    _Crud.crudDelete(id);
                }
            });
        }
    },

    populateCell : function(id, key, type, value, cell) {
        var isCollection = _Crud.isCollection(key, type);
        var relatedType  = _Crud.relatedType(key, type);
        var simpleType;
        
        if (!relatedType) {
            
            //console.log(key, value, typeof value);
            
            if (typeof value === 'boolean') {
                cell.append('<input type="checkbox" ' + (value?'checked="checked"':'') + '>');
                $('input', cell).on('change', function() {
                   //console.log('change value for ' + key + ' to ' + $(this).prop('checked'));
                   _Crud.crudUpdate(id, key, $(this).prop('checked').toString());
                });
                
        
            } else {
                cell.text(nvl(formatValue(value),'')).on('mouseup', function(event) {
                    event.preventDefault();
                    var self = $(this);
                    var oldValue = self.text();
                    self.off('mouseup');
                    self.html('<input class="value" type="text" size="40" value="' + oldValue + '">');
                    _Crud.activateTextInputField($('input', self), id, key);
                });
            }

        } else if (isCollection) {
                    
            simpleType = lastPart(relatedType, '.');
            
            if (value && value.length) {
                    
                $.each(value, function(v, relatedId) {
                    _Crud.getAndAppendNode(type, id, key, relatedId, cell);
                });
                //cells.append('<div style="clear:both"><img class="add" src="icon/add_grey.png"></div>');
            }

            cell.append('<img class="add" src="icon/add_grey.png">');
            $('.add', cell).on('click', function() {
                _Crud.dialog('Add ' + simpleType, function() {}, function() {});
                _Crud.displaySearch(type, id, key, simpleType, dialogText);
            });

        } else {
                    
            simpleType = lastPart(relatedType, '.');
            
            if (value) {
                    
                _Crud.getAndAppendNode(type, id, key, value, cell);
                        
            } else {
                
                cell.append('<img class="add" src="icon/add_grey.png">');
                $('.add', cell).on('click', function() {
                    _Crud.dialog('Add ' + simpleType + ' to ' + key, function() {}, function() {});
                    _Crud.displaySearch(type, id, key, simpleType, dialogText);
                });
            }

        }
    //searchField.focus();
    },

    getAndAppendNode : function(parentType, parentId, key, obj, cell) {
        //console.log('headers', headers);
        if (!obj) return;
        var id;
        if ((typeof obj) === 'object') {
            id = obj.id;
        } else {
            id = obj;
        }
        
        $.ajax({
            url: rootUrl + id + '/' + _Crud.view[parentType],
            headers: headers,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                var node = data.result;
                //console.log('node', node);
                
                cell.append('<div title="' + node.name + '" id="_' + node.id + '" class="node ' + (node.type ? node.type.toLowerCase() : (node.tag ? node.tag : 'element')) + ' ' + node.id + '_">' + fitStringToSize(node.name, 80) + '<img class="remove" src="icon/cross_small_grey.png"></div>');
                var nodeEl = $('#_' + node.id, cell);
                //console.log(node);
                if (node.type === 'Image') {
                    
                    if (node.isThumbnail) {
                        nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
                    } else if (node.tnSmall) {
                        nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.tnSmall.id + '"></div>');
                        
                    }
                        
                    $('.thumbnail', nodeEl).on('click', function(e) {
                        e.stopPropagation();
                        e.preventDefault();
                        _Crud.showDetails(node, false, node.type);
                        return false;
                    });

                    if (node.tnMid) {
                        $('.thumbnail', nodeEl).on('mouseenter', function(e) {
                            e.stopPropagation();
                            $('.thumbnailZoom').remove();
                            nodeEl.parent().append('<img class="thumbnailZoom" src="/' + node.tnMid.id + '">');
                            var tnZoom = $($('.thumbnailZoom', nodeEl.parent())[0]);
                            tnZoom.css({
                                top: (nodeEl.position().top) + 'px',
                                left: (nodeEl.position().left - 42) + 'px'
                            });
                            tnZoom.on('mouseleave', function(e) {
                                e.stopPropagation();
                                $('.thumbnailZoom').remove();
                            });
                        });
                    }
                }
                $('.remove', nodeEl).on('click', function(e) {
                    e.preventDefault();
                    _Crud.removeRelatedObject(parentType, parentId, key, obj);
                    return false;
                });
                nodeEl.on('click', function(e) {
                    e.preventDefault();
                    _Crud.showDetails(node, false, node.type);
                    return false;
                });                    

            }
        });
  
    },
    
    //    getAndAppendNodes : function(parentType, id, key, type, el, pageSize) {
    //        var urlType  = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
    //        var view = 'public'; // _Crud.view[_Crud.type]
    //        var url = rootUrl + urlType + '/' + view + _Crud.sortAndPagingParameters('name', 'asc', pageSize, 1);
    //        $.ajax({
    //            url: url,
    //            headers: headers,
    //            type: 'GET',
    //            dataType: 'json',
    //            contentType: 'application/json; charset=utf-8',
    //            //async: false,
    //            success: function(data) {
    //                
    //                $.each(data.result, function(i, node) {
    //                
    //                    //console.log('node', node);
    //                    el.append('<div title="' + node.name + '" id="_' + node.id + '" class="node ' + node.type.toLowerCase() + ' ' + node.id + '_">' + fitStringToSize(node.name, 120) + '</div>');
    //                
    //                    var nodeEl = $('#_' + node.id, el);
    //                    //console.log(node);
    //                    if (node.type == 'Image') {
    //                        nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
    //                    }
    //                    
    //                    nodeEl.on('click', function(e) {
    //                        e.preventDefault();
    //                        _Crud.addRelatedObject(parentType, id, key, node, function() {
    //                            document.location.reload();
    //                        });
    //                        return false;
    //                    });
    //                
    //                });
    //            }
    //        });
    //  
    //    },
    
    clearSearch : function(el) {
        if (_Crud.clearSearchResults(el)) {
            $('.clearSearchIcon').hide().off('click');
            $('.search').val('');
            main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');
            _Crud.initTabs();
            _Crud.updateUrl(_Crud.type);
        }
    },
    
    clearSearchResults : function(el) {
        var searchResults = $('.searchResults', el);
        if (searchResults.length) {
            searchResults.remove();
            $('.searchResultsTitle').remove();
            return true;
        }
        return false;
    },
    
    /**
     * Conduct a search and append search results to 'el'.
     * 
     * If an optional type is given, restrict search to this type.
     */
    search : function(searchString, el, type, onClickCallback) {

        _Crud.clearSearchResults(el);

        el.append('<h2 class="searchResultsTitle">Search Results</h2>');
        el.append('<div class="searchResults"></div>');
        var searchResults = $('.searchResults', el);
        
        //$('.search').select();
        
        var view = _Crud.view[_Crud.type];
        
        var types = type ? [ type ] : _Crud.types;
        
        $.each(types, function(t, type) {
            
            var url = rootUrl + _Crud.restType(type) + '/' + view + _Crud.sortAndPagingParameters(type, 'name', 'asc', pageSize, 1) + '&name=' + searchString + '&loose=1';
            
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
                        
                        if (node.type === type) {
                            
                            //console.log('node', node);
                            $('#resultsFor' + type, searchResults).append('<div title="' + node.name + '" id="_' + node.id + '" class="node ' + node.type.toLowerCase() + ' ' + node.id + '_">' + fitStringToSize(node.name, 120) + '</div>');
                
                            var nodeEl = $('#_' + node.id, searchResults);
                            //console.log(node);
                            if (node.type === 'Image') {
                                nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
                            }
                    
                            nodeEl.on('click', function(e) {
                                onClickCallback(e, node);
                            });
                        
                        }
                
                    });
                }
            });
        
        });

        
    },

    displaySearch : function(parentType, id, key, type, el) {
        el.append('<div class="searchBox searchBoxDialog"><input class="search" name="search" size="20" placeholder="Search"><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');
        var searchBox = $('.searchBoxDialog', el);
        var search = $('.search', searchBox);
        window.setTimeout(function() {
            search.focus();
        }, 250);
        
        //        //_Crud.addPager(type, el, 50, 1);
        //        el.append('<div id="relatedNodesList"></div>');
        //        var relatedNodesList = $('#relatedNodesList', el);
        //        var pagerNode = _Crud.addPager(type, relatedNodesList);
        //        _Crud.getAndAppendNodes(parentType, id, key, type, relatedNodesList, 100, false);
        //        _Crud.activatePagerElements(type, pagerNode);

        search.keyup(function(e) {
            e.preventDefault();
                
            var searchString = $(this).val();
            if (searchString && searchString.length && e.keyCode === 13) {
                
                $('.clearSearchIcon', searchBox).show().on('click', function() {
                    if (_Crud.clearSearchResults(el)) {
                        $('.clearSearchIcon').hide().off('click');
                        search.val('');
                        search.focus();
                    }
                });

                _Crud.search(searchString, el, type, function(e, node) {
                    e.preventDefault();
                    _Crud.addRelatedObject(parentType, id, key, node, function() {
                        //document.location.reload();
                        });
                    return false;
                });
                
            } else if (e.keyCode === 27 || searchString === '') {

                if (!searchString || searchString === '') {
                    dialogCancelButton.click();
                }
                
                if (_Crud.clearSearchResults(el)) {
                    $('.clearSearchIcon').hide().off('click');
                    search.val('');
                    search.focus();
                }
                 
            }
            
            return false;
            
        });
    },

    removeRelatedObject : function(type, id, key, relatedObj) {
        var view = _Crud.view[_Crud.type];
        var urlType  = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var url = rootUrl + urlType + '/' + id + '/' + view;
        console.log(url);
        if (_Crud.isCollection(key, type)) {
            var objects = [];
            $.ajax({
                url: url,
                headers: headers,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    //console.log(key, data.result, data.result[key]);
                    $.each(data.result[key], function(i, obj) {
                        //console.log(obj, ' equals ', relatedObj);
                        if (!_Crud.equal(obj, relatedObj)) {
                            //console.log('not equal');
                            objects.push(obj);
                        }
                    });
                    
                    //console.log('new id array: ', objects);
                    var json = '{"' + key + '":null}';
                    //console.log('PUT ', json, ' to url ', url);
                    
                    $.ajax({
                        url: url,
                        headers: headers,
                        type: 'PUT',
                        dataType: 'json',
                        data: json,
                        contentType: 'application/json; charset=utf-8',
                        //async: false,
                        statusCode : {
                            200 : function(data) {
                            
                                var json = '{"' + key + '":' + JSON.stringify(objects) + '}';
                                //console.log('removeRelatedObject, setting new id array', headers, url, objects, json);
                            
                                $.ajax({
                                    url: url,
                                    headers: headers,
                                    type: 'PUT',
                                    dataType: 'json',
                                    data: json,
                                    contentType: 'application/json; charset=utf-8',
                                    //async: false,
                                    statusCode : {
                                        200 : function(data) {
                                            var rowEl = $('#' + id);
                                            var nodeEl = $('.' + _Crud.id(relatedObj) + '_', rowEl);
                                            console.log(rowEl, nodeEl);
                                            nodeEl.remove();
                                        },
                                        error: function(a,b,c) {
                                            console.log(a,b,c);
                                        }
                                        
                                    }
                                });
                                
                            }
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
    
    addRelatedObject : function(type, id, key, relatedObj, callback) {
        var view = _Crud.view[_Crud.type];
        var urlType  = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var url = rootUrl + urlType + '/' + id + '/' + view;
        //console.log('headers', headers);
        if (_Crud.isCollection(key, type)) {
            var objects = [];
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
                        objects.push(node); 
                    });

                    if (!isIn(relatedObj, objects)) {
                        objects.push(relatedObj);
                    }
                    var json = '{"' + key + '":' + JSON.stringify(objects) + '}';
                    //console.log(headers, url, json);
                    _Crud.crudUpdateObj(id, json, function() {
                        _Crud.crudRefresh(id, key);
                        //dialogCancelButton.click();
                        if (callback) {
                            callback();
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
                    var json = '{"' + key + '":'  + JSON.stringify(relatedObj) + '}';
                    //console.log(data.result);
                    //var value = data.result[key];
                    //_Crud.crudUpdate(id, key, _Crud.idArray([ relatedId ]), reload ? document.location.reload() : function() {});
                    //console.log('update single related object', id, relatedObj, json);
                    _Crud.crudUpdateObj(id, json, function() {
                        _Crud.crudRefresh(id, key);
                        dialogCancelButton.click();
                    });
                },
                error: function(a,b,c) {
                    console.log(a,b,c);
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
            $.blockUI.defaults.css.cursor = 'default';
            
        
            var w = $(window).width();
            var h = $(window).height();

            var ml = 0;
            var mt = 24;

            // Calculate dimensions of dialog
            var dw = Math.min(900, w-ml);
            var dh = Math.min(600, h-mt);
            //            var dw = (w-24) + 'px';
            //            var dh = (h-24) + 'px';
            
            var l = parseInt((w-dw)/2);
            var t = parseInt((h-dh)/2);
            
            //console.log(w, h, dw, dh, l, t);
            
            $.blockUI({
                fadeIn: 25,
                fadeOut: 25,
                message: dialogBox,
                css: {
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

        var ml = 0;
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

    showDetails : function(node, create, typeParam) {

        var type = typeParam || node.type;
        if (!type) {
            Structr.error('Missing type', function() {}, function() {});
            return;
        }
        var typeDef = _Crud.schema[type];
        
        if (node) {
            //console.log('Edit node', node);
            _Crud.dialog('Details of ' + type + ' ' + (node.name ? node.name : node.id) + '<span class="id"> [' + node.id + ']</span>', function() {}, function() {});
        } else {
            //console.log('Create new node of type', typeOnCreate);
            _Crud.dialog('Create new ' + type, function() {}, function() {});
        }
        
        //console.log('readonly', readonly);
        
        if (create) {
            //console.log('edit mode, appending form');
            dialogText.append('<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th>');//<th>Type</th><th>Read Only</th></table></form>');
        } else {
            dialogText.append('<table class="props" id="details_' + node.id + '"><tr><th>Name</th><th>Value</th>');//<th>Type</th><th>Read Only</th></table>');
        }
        
        var table = $('table', dialogText);
        
        
        var keys = _Crud.keys[type];
        if (!keys) {
            keys = Object.keys(typeDef.views[_Crud.view[type]]);
        }
        
        $.each(keys, function(i, key) {
        //$.each(typeDef.views[_Crud.view[type]], function(i, property) {
            //console.log(property);
            //var type = property.className.substring(property.className.lastIndexOf('.') + 1);
            //var key = property.jsonName;
            table.append('<tr><td class="key"><label for="' + key + '">' + _Crud.formatKey(key) + '</label></td><td class="value ' + key + '"></td>');//<td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
            var cell = $('.' + key, table);
            if (node && node.id) {
                //console.log(node.id, key, type, node[key], cell);
                _Crud.populateCell(node.id, key, node.type, node[key], cell);
            } else {
                //console.log(key,node[key]);
                cell.append(formatValueInputField(key, ''));
            }
        });
        dialogSaveButton.remove();
        if (create) {
            dialogBox.append('<button class="save" id="saveProperties">Save</button>');
            $('.save', $('#dialogBox')).on('click', function() {
                $(this).remove();
                var form = $('#entityForm');
                var json = JSON.stringify(_Crud.serializeObject(form));
                //console.log(json);
                //dialogText.empty();
                if (create) {
                    _Crud.crudCreate(type, typeDef.url, json);
                } else {
                    var id = form.attr('data-id');
                    //console.log('updating', id);
                    _Crud.crudUpdateObj(id, json);
                }
            });
        }
        
        if (node && node.type === 'Image') {
            dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.id + '"></div></div>');
        }
        
    },

    formatKey : function(text) {
        if (!text) return '';
        var result = '';
        for (var i=0; i<text.length; i++) {
            var c = text.charAt(i);
            if (c === c.toUpperCase()) {
                result += ' ' + c;
            } else {
                result += (i===0 ? c.toUpperCase() : c);
            }
        }
        return result;
    },

    serializeObject : function(obj) {
        var o = {};
        var a = obj.serializeArray();
        $.each(a, function() {
            if (this.value && this.value !== '') {
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
    },
    
    idArray : function(ids) {
        var arr = [];
        $.each(ids, function(i, id) {
            var obj = {};
            obj.id = id;
            arr.push(obj);
        });
        return arr;
    },
    
    /**
     * Return the id of the given object.
     */
    id : function(objectOrId) {
        if (typeof objectOrId === 'object') {
            return objectOrId.id;
        } else {
            return objectOrId;
        }
    },
    
    /**
     * Compare to values and return true if they can be regarded equal.
     * 
     * If both values are of type 'object', compare their id property,
     * in any other case, compare values directly.
     */
    equal : function(obj1, obj2) {
        if (typeof obj1 === 'object' && typeof obj2 === 'object') {
            var id1 = obj1.id;
            var id2 = obj2.id;
            if (id1 && id2) {
                return id1 === id2;
            }
        } else {
            return obj1 === obj2;
        }
        
        // default
        return false;
    }
    
}

// Hook for nodejs
if (typeof module === 'object') {
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