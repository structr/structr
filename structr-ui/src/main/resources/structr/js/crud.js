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

var defaultType, defaultView, defaultSort, defaultOrder, defaultPage, defaultPageSize;
var searchField;

var browser = (typeof document === 'object');
var crudPagerDataKey = 'structrCrudPagerData_' + port + '_';
var crudTypeKey = 'structrCrudType_' + port;
var crudHiddenColumnsKey = 'structrCrudHiddenColumns_' + port;

if (browser) {

    $(document).ready(function() {

        defaultType = 'Page';
        defaultView = 'ui';
        defaultSort = '';
        defaultOrder = '';

        defaultPage = 1;
        defaultPageSize = 10;

        main = $('#main');

        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');

        //        console.log(_Crud.type);

        if (!_Crud.type) {
            _Crud.restoreType();
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
    schemaLoading: false,
    schemaLoaded: false,
    //allTypes : [],

    types: [], //'Page', 'User', 'Group', 'Folder', 'File', 'Image', 'Content', 'PropertyDefinition' ],
    views: ['public', 'all', 'ui'],
    schema: [],
    keys: [],
    type: null,
    pageCount: null,
    view: [],
    sort: [],
    order: [],
    page: [],
    pageSize: [],
    init: function() {

        _Crud.schemaLoading = false;
        _Crud.schemaLoaded = false;
        _Crud.schema = [];
        _Crud.keys = [];

        _Crud.loadSchema(function() {
            if (browser)
                _Crud.initTabs();
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
    onload: function() {

        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Data');

        Structr.registerModule('crud', _Crud);
        Structr.classes.push('crud');

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
    initTabs: function() {

        $.each(_Crud.types, function(t, type) {
            _Crud.addTab(type);
        });

        $('#resourceTabs').tabs({
            activate: function(event, ui) {
                _Crud.clearList(_Crud.type);
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

        _Crud.resize();


    },
    isSchemaLoaded: function() {
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
    updateUrl: function(type) {
        //console.log('updateUrl', type, _Crud.pageSize[type], _Crud.page[type]);

        if (type) {
            _Crud.storeType();
            _Crud.storePagerData();
            //window.history.pushState('', '', _Crud.sortAndPagingParameters(_Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]) + '&view=' + _Crud.view[type] + '&type=' + type + '#crud');
            window.location.hash = '#crud';
        }
        searchField.focus();

    },
    /**
     * Read the schema from the _schema REST resource and call 'callback'
     * after the complete schema is loaded.
     */
    loadSchema: function(callback) {

        // Avoid duplicate loading of schema
        if (_Crud.schemaLoading) {
            return;
        }
        _Crud.schemaLoading = true;

        _Crud.loadAccessibleResources(function() {
            $.each(_Crud.types, function(t, type) {
                //console.log('Loading type definition for ' + type + '...');
                if (!type || type.startsWith('_')) {
                    return;
                }
                _Crud.loadTypeDefinition(type, callback);
            });
        });

    },
    loadAccessibleResources: function(callback) {
        //var url = rootUrl + 'resource_access/ui';
        var url = rootUrl + 'resource_access';
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;

                //console.log(data);
                var types = [];
                _Crud.types.length = 0;
                $.each(data.result, function(i, res) {
                    var type = getTypeFromResourceSignature(res.signature);
                    //console.log(res);
                    if (type && !(type.startsWith('_')) && !isIn(type, _Crud.types)) {
                        _Crud.types.push(type);
                        //console.log(type, res.position);
                        types.push({'type': type, 'position': res.position});
                    }
                });
                //console.log(types);
                types.sort(function(a, b) {
                    return a.position - b.position;
                });
                _Crud.types.length = 0;
                $.each(types, function(i, typeObj) {
                    _Crud.types.push(typeObj.type);
                });

                if (callback) {
                    callback();
                }

            }
        });
    },
    loadTypeDefinition: function(type, callback) {

        //_Crud.schema[type] = [];

        var url = rootUrl + '_schema/' + type;
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function(data) {

                    // no schema entry found?
                    if (!data || !data.result || data.result_count === 0) {

                        console.log("ERROR: loading Schema " + type);
                        //Structr.error("ERROR: loading Schema " + type, true);

                        var typeIndex = _Crud.types.indexOf(type);

                        // Delete broken type from list
                        _Crud.types.splice(typeIndex, 1);

                        if (_Crud.isSchemaLoaded()) {
                            //console.log('Schema loaded successfully');
                            if (callback) {
                                callback();
                            }
                        }
                    } else {

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
                },
                401: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON, url);
                },
                403: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON, url);
                },
                404: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON, url);
                },
                422: function(data) {
                    Structr.errorFromResponse(data.responseJSON);
                }
            }


        });
    },
    getPropertyType: function(type, key) {
        return _Crud.schema[type].views.all[key].type;
    },
    determinePagerData: function(type) {

        // Priority: JS vars -> Local Storage -> URL -> Default

        if (!_Crud.view[type]) {
            _Crud.view[type] = urlParam('view');
            _Crud.sort[type] = urlParam('sort');
            _Crud.order[type] = urlParam('order');
            _Crud.pageSize[type] = urlParam('pageSize');
            _Crud.page[type] = urlParam('page');
        }

        if (!_Crud.view[type]) {
            _Crud.restorePagerData();
        }

        if (!_Crud.view[type]) {
            _Crud.view[type] = defaultView;
            _Crud.sort[type] = defaultSort;
            _Crud.order[type] = defaultOrder;
            _Crud.pageSize[type] = defaultPageSize;
            _Crud.page[type] = defaultPage;
        }
    },
    addTab: function(type) {
        var res = _Crud.schema[type];
        $('#resourceTabsMenu').append('<li><a href="#' + type + '"><span>' + _Crud.formatKey(type) + '</span></a></li>');
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
                $.each(_Crud.filterKeys(type, _Crud.keys[type]), function(k, key) {
                    tableHeader.append('<th class="' + key + '">' + _Crud.formatKey(key) + '</th>');
                });
                tableHeader.append('<th class="_action_header">Actions</th>');
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
    restType: function(type) {
        var typeDef = _Crud.schema[type];
        return typeDef ? typeDef.url.substring(1) : type.toUnderscore();
    },
    /**
     * Return true if the combination of the given property key
     * and the given type is a collection
     */
    isCollection: function(key, type) {
        var typeDef = _Crud.schema[type];
        var view = typeDef.views[_Crud.view[type]];
        return (view && view[key] && view[key].isCollection);
    },
    /**
     * Return true if the combination of the given property key
     * and the given type is a read-only property
     */
    readOnly: function(key, type) {
        var typeDef = _Crud.schema[type];
        var view = typeDef.views[_Crud.view[type]];
        return (view && view[key] && view[key].readOnly);
    },
    /**
     * Return the related type of the given property key
     * of the given type
     */
    relatedType: function(key, type) {
        var typeDef = _Crud.schema[type];
        var view = typeDef.views[_Crud.view[type]];
        return (view && view[key] ? view[key].relatedType : null);
    },
    /**
     * Append a pager for the given type to the given DOM element.
     */
    addPager: function(type, el) {

        if (!_Crud.page[type]) {
            _Crud.page[type] = urlParam('page') ? urlParam('page') : (defaultPage ? defaultPage : 1);
        }

        if (!_Crud.pageSize[type]) {
            _Crud.pageSize[type] = urlParam('pageSize') ? urlParam('pageSize') : (defaultPageSize ? defaultPageSize : 10);
        }

        el.append('<div class="pager" style="clear: both"><button class="pageLeft">&lt; Prev</button>'
                + ' Page <input class="page" type="text" size="3" value="' + _Crud.page[type] + '"><button class="pageRight">Next &gt;</button> of <input class="readonly pageCount" readonly="readonly" size="3" value="' + nvl(_Crud.pageCount, 0) + '">'
                + ' Page Size: <input class="pageSize" type="text" size="3" value="' + _Crud.pageSize[type] + '"></div>');

        return $('.pager', el);

    },
    storeType: function() {
        localStorage.setItem(crudTypeKey, _Crud.type);
    },
    restoreType: function() {
        var val = localStorage.getItem(crudTypeKey);
        if (val) {
            _Crud.type = val;
        }
    },
    storePagerData: function() {
        var type = _Crud.type;
        var pagerData = _Crud.view[type] + ',' + _Crud.sort[type] + ',' + _Crud.order[type] + ',' + _Crud.page[type] + ',' + _Crud.pageSize[type];
        localStorage.setItem(crudPagerDataKey + type, pagerData);
    },
    restorePagerData: function() {
        var type = _Crud.type;
        var val = localStorage.getItem(crudPagerDataKey + type);

        if (val) {
            var pagerData = val.split(',');
            _Crud.view[type] = pagerData[0];
            _Crud.sort[type] = pagerData[1];
            _Crud.order[type] = pagerData[2];
            _Crud.page[type] = pagerData[3];
            _Crud.pageSize[type] = pagerData[4];
        }
    },
    replaceSortHeader: function(type) {
        var table = _Crud.getTable(type);
        var newOrder = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
        var res = _Crud.schema[type];
        var view = res.views[_Crud.view[type]];
        $('th', table).each(function(i, t) {
            var th = $(t);
            var key = th.attr('class');
            if (key === "_action_header") {

                th.empty();
                th.append('<img src="icon/application_view_detail.png" alt="Configure columns" title="Configure columns" />');
                $('img', th).on('click', function(event) {

                    _Crud.dialog('<h3>Configure columns for type ' + type + '</h3>', function() {
                    }, function() {
                    });

                    $('div.dialogText').append('<table class="props" id="configure-' + type + '-columns"></table>');

                    var table = $('#configure-' + type + '-columns');

                    // append header row
                    table.append('<tr><th>Column Key</th><th>Visible</th></tr>');
                    var filterSource = localStorage.getItem(crudHiddenColumnsKey + type);
                    var filteredKeys = {};
                    if (filterSource) {

                        filteredKeys = JSON.parse(filterSource);
                    }

                    $.each(_Crud.keys[type], function(k, key) {

                        var checkboxKey = 'column-' + type + '-' + key + '-visible';
                        var hidden = filteredKeys.hasOwnProperty(key) && filteredKeys[key] === 0;

                        table.append(
                                '<tr>'
                                + '<td><b>' + key + '</b></td>'
                                + '<td><input type="checkbox" id="' + checkboxKey + '" ' + (hidden ? '' : 'checked="checked"') + '/></td>'
                                + '</tr>'
                                );

                        $('#' + checkboxKey).on('click', function() {
                            _Crud.toggleColumn(type, key);
                        });
                    });

                    dialogCloseButton = $('.closeButton', $('#dialogBox'));
                    dialogCloseButton.on('click', function() {
                        location.reload();
                    });

                });

            } else if (key !== 'Actions') {
                $('a', th).off('click');
                th.empty();
                if (view[key]) {
                    var sortKey = view[key].jsonName;
                    th.append('<a href="' + _Crud.sortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type]) + '#' + type + '">' + _Crud.formatKey(key) + '</a>'
                            + '<img src="icon/cross_small_grey.png" alt="Hide this column" title="Hide this column" />');
                    $('a', th).on('click', function(event) {
                        event.preventDefault();
                        _Crud.sort[type] = key;
                        _Crud.order[type] = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
                        _Crud.activateList(type);
                        _Crud.updateUrl(type);
                        return false;
                    });
                    $('img', th).on('click', function(event) {

                        event.preventDefault();
                        _Crud.toggleColumn(type, key);
                        return false;
                    });
                }
            }
        });
    },
    sortAndPagingParameters: function(t, s, o, ps, p) {
        //var typeParam = (t ? 'type=' + t : '');
        var sortParam = (s ? 'sort=' + s : '');
        var orderParam = (o ? 'order=' + o : '');
        var pageSizeParam = (ps ? 'pageSize=' + ps : '');
        var pageParam = (p ? 'page=' + p : '');

        //var params = (typeParam ? '?' + typeParam : '');
        var params = '';
        params = params + (sortParam ? (params.length ? '&' : '?') + sortParam : '');
        params = params + (orderParam ? (params.length ? '&' : '?') + orderParam : '');
        params = params + (pageSizeParam ? (params.length ? '&' : '?') + pageSizeParam : '');
        params = params + (pageParam ? (params.length ? '&' : '?') + pageParam : '');

        return params;
    },
    activateList: function(type) {
        //console.log('activateList', type);
        var url = rootUrl + _Crud.restType(type) + '/' + _Crud.view[type] + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
        _Crud.list(type, url);
        document.location.hash = type;
    },
    clearList: function(type) {
        //console.log('clearList', type);
        var table = _Crud.getTable(type);
        var headerRow = '<thead><tr>' + $($('tr:first-child', table)[0]).html() + '</tr></thead>';
        //    console.log(headerRow);
        table.empty();
        table.append(headerRow);
    },
    list: function(type, url) {
        var table = _Crud.getTable(type);
        //console.log('list', type, url, table);
        table.append('<tr></tr>');
        _Crud.clearList(type);
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                //console.log(data);
                if (!data)
                    return;
                $.each(data.result, function(i, item) {
                    //console.log('calling appendRow', type, item);
                    _Crud.appendRow(type, item);
                });
                _Crud.updatePager(type, data.query_time, data.serialization_time, data.page_size, data.page, data.page_count);
                _Crud.replaceSortHeader(type);
            },
            error: function(a, b, c) {
                console.log(a, b, c);
            }
        });
    },
    crudExport: function(type) {
        var url = rootUrl + '/' + $('#' + type).attr('data-url') + '/' + _Crud.view[type] + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
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
                if (k < _Crud.keys[type].length - 1) {
                    exportArea.append(',');
                }
            });
            exportArea.append('\n');
        }
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;
                $.each(data.result, function(i, item) {
                    _Crud.appendRowAsCSV(type, item, exportArea);
                });
            }
        });
    },
    updatePager: function(type, qt, st, ps, p, pc) {
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
    activatePagerElements: function(type, pagerNode) {
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
    deActivatePagerElements: function(pagerNode) {
        //console.log('deActivatePagerElements', type);
        //        var typeNode = $('#' + type);

        $('.page', pagerNode).off('keypress');
        $('.pageSize', pagerNode).off('keypress');
        $('.pageLeft', pagerNode).off('click');
        $('.pageRight', pagerNode).off('click');
    },
    crudRead: function(type, id, callback) {
        // use 'ui' view as default to make the 'edit by id' feature work
        var view = (type && _Crud.view[type] ? _Crud.view[type] : 'ui');
        var url = rootUrl + id + '/' + view;
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;
                if (callback) {
                    callback(data.result);
                } else {
                    _Crud.appendRow(type, data.result);
                }
            }
        });
    },
    getTable: function(type) {
        var typeNode = $('#' + type);
        //typeNode.append('<table></table>');
        var table = $('table', typeNode);
        return table;
    },
    crudEdit: function(id, type) {
        var t = type || _Crud.type;
        console.log(t);
        $.ajax({
            url: rootUrl + id + (_Crud.view[t] ? '/' + _Crud.view[t] : ''),
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;
                //console.log('type', type);
                _Crud.dialog('Edit ' + t + ' ' + id, function() {
                    //console.log('ok')
                }, function() {
                    //console.log('cancel')
                });
                _Crud.showDetails(data.result, false, t);
                //_Crud.populateForm($('#entityForm'), data.result);
            }
        });
    },
    crudCreate: function(type, url, json, onSuccess, onError) {
        //console.log('crudCreate', type, url, json);
        $.ajax({
            url: rootUrl + url,
            type: 'POST',
            dataType: 'json',
            data: json,
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                201: function(xhr) {
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
                    if (dialogCloseButton) {
                        dialogCloseButton.remove();
                    }
                },
                400: function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    _Crud.showCreateError(type, data, onError);
                },
                401: function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    _Crud.showCreateError(type, data, onError);
                },
                403: function(data, status, xhr) {
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    _Crud.showCreateError(type, data, onError);
                },
                404: function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    _Crud.showCreateError(type, data, onError);
                },
                422: function(data, status, xhr) {
                    _Crud.showCreateError(type, data, onError);
                },
                500: function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                    _Crud.showCreateError(type, data, onError);
                }
            }
        });
    },
    showCreateError: function(type, data, onError) {
        if (onError) {
            onError();
        } else {
            //console.log(data, status, xhr);

            if (!dialogBox.is(':visible')) {
//                            _Crud.dialog('Create new ' + type, function() {
//                                //console.log('ok')
//                            }, function() {
//                                //console.log('cancel')
//                            });
                _Crud.showDetails(null, true, type);
            }
            var resp = JSON.parse(data.responseText);
            //console.log(resp);

            $('.props input', dialogBox).css({
                backgroundColor: '#fff',
                borderColor: '#a5a5a5'
            });

            $('.props textarea', dialogBox).css({
                backgroundColor: '#fff',
                borderColor: '#a5a5a5'
            });

            $('.props td.value', dialogBox).css({
                backgroundColor: '#fff',
                borderColor: '#b5b5b5',
            });

            $.each(Object.keys(resp.errors[type]), function(i, key) {
                var errorMsg = resp.errors[type][key][0];
                //console.log(key, errorMsg);
                var input = $('td [name="' + key + '"]', dialogText);
                if (input.length) {
                    input.prop('placeholder', errorMsg.splitAndTitleize('_')).css({
                        backgroundColor: '#fee',
                        borderColor: '#933'
                    });
                } else {
                    $('td.' + key + ' span', dialogText).remove();
                    $('td.' + key, dialogText).append('<span>' + errorMsg.splitAndTitleize('_') + '</span>').css({
                        backgroundColor: '#fee',
                        borderColor: '#933',
                        color: '#a5a5a5'
                    });
                }
            });
            //_Crud.error('Error: ' + data.responseText, true);
        }
    },
    crudRefresh: function(id, key) {
        var url = rootUrl + id + '/' + _Crud.view[_Crud.type];
        //console.log('crudRefresh', id, key, url);

        $.ajax({
            url: url,
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;
                //console.log('refresh', id, key, data.result[key], data.result.type);
                if (key) {
                    _Crud.refreshCell(id, key, data.result[key], data.result.type);
                } else {
                    _Crud.refreshRow(id, data.result, data.result.type);
                }
            }
        });
    },
    crudReset: function(id, key) {
        $.ajax({
            url: rootUrl + id + '/' + _Crud.view[_Crud.type],
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;
                //console.log('reset', id, key, data.result[key]);
                _Crud.resetCell(id, key, data.result[key]);
            }
        });
    },
    crudUpdateObj: function(id, json, onSuccess, onError) {
        //console.log('crudUpdateObj URL:', rootUrl + id);
        //console.log('crudUpdateObj JSON:', json);
        $.ajax({
            url: rootUrl + id,
            data: json,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id);
                    }
                },
                400: function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                401: function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                403: function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                404: function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                422: function(data, status, xhr) {
                    _Crud.error('Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id);
                    }
                },
                500: function(data, status, xhr) {
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
    crudUpdate: function(id, key, newValue, onSuccess, onError) {
        var url = rootUrl + id;
        var json;
        if (!newValue || newValue === '') {
            json = '{"' + key + '":null}';
        } else {
            json = '{"' + key + '":"' + newValue.escapeForJSON() + '"}';
        }
        //console.log('crudUpdate', url, json);
        $.ajax({
            url: url,
            data: json,
            type: 'PUT',
            contentType: 'application/json; charset=utf-8',
            statusCode: {
                200: function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id, key);
                    }
                },
                400: function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                401: function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                403: function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                404: function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                422: function(data, status, xhr) {
                    _Crud.error('Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                500: function(data, status, xhr) {
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
    crudRemoveProperty: function(id, key, onSuccess, onError) {
        var url = rootUrl + id;
        var json = '{"' + key + '":null}';
        $.ajax({
            url: url,
            data: json,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id, key);
                    }
                },
                204: function() {
                    if (onSuccess) {
                        onSuccess();
                    } else {
                        _Crud.crudRefresh(id, key);
                    }
                },
                400: function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                401: function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                403: function(data, status, xhr) {
                    _Crud.error('Forbidden: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                404: function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                422: function(data, status, xhr) {
                    _Crud.error('Error: ' + data.responseText, true);
                    if (onError) {
                        onError();
                    } else {
                        _Crud.crudReset(id, key);
                    }
                },
                500: function(data, status, xhr) {
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
    crudDelete: function(id) {
        $.ajax({
            url: rootUrl + '/' + id,
            type: 'DELETE',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function() {
                    var row = _Crud.row(id);
                    row.remove();
                },
                204: function() {
                    var row = _Crud.row(id);
                    row.remove();
                },
                400: function(data, status, xhr) {
                    _Crud.error('Bad request: ' + data.responseText, true);
                },
                401: function(data, status, xhr) {
                    _Crud.error('Authentication required: ' + data.responseText, true);
                },
                403: function(data, status, xhr) {
                    console.log(data, status, xhr);
                    _Crud.error('Forbidden: ' + data.responseText, true);
                },
                404: function(data, status, xhr) {
                    _Crud.error('Not found: ' + data.responseText, true);
                },
                422: function(data, status, xhr) {
                },
                500: function(data, status, xhr) {
                    _Crud.error('Internal Error: ' + data.responseText, true);
                }
            }
        });
    },
    populateForm: function(form, node) {
        //console.log(form, node);
        var fields = $('input', form);
        form.attr('data-id', node.id);
        $.each(fields, function(f, field) {
            var value = formatValue(node[field.name], field.name, node.type, node.id);
            //console.log(field, field.name, value);
            $('input[name="' + field.name + '"]').val(value);
        });
    },
    cells: function(id, key) {
        var row = _Crud.row(id);
        var cellInMainTable = $('.' + key, row);

        var cellInDetailsTable;
        var table = $('#details_' + id);
        if (table) {
            cellInDetailsTable = $('.' + key, table);
        }

        if (cellInMainTable && !cellInDetailsTable) {
            return [cellInMainTable];
        }

        if (cellInDetailsTable && !cellInMainTable) {
            return [cellInDetailsTable];
        }

        return [cellInMainTable, cellInDetailsTable];
    },
    resetCell: function(id, key, oldValue) {
        //    console.log('resetCell', id, key, oldValue);
        var cells = _Crud.cells(id, key);

        $.each(cells, function(i, cell) {
            cell.empty();
            _Crud.populateCell(id, key, _Crud.type, oldValue, cell);
            blinkRed(cell);
        });
    },
    refreshCell: function(id, key, newValue, type) {
        //    console.log('refreshCell', id, key, newValue, type);
        var cells = _Crud.cells(id, key);
        $.each(cells, function(i, cell) {
            cell.empty();
            _Crud.populateCell(id, key, type, newValue, cell);
            blinkGreen(cell);
        });
    },
    refreshRow: function(id, item, type) {
        //    console.log('refreshCell', id, key, newValue);
        var row = _Crud.row(id);
        row.empty();
        _Crud.populateRow(id, item, type);
    },
    activateTextInputField: function(el, id, key, propertyType) {
        var oldValue = el.text();
        el.off('mouseup');
        var input;
        if (propertyType === 'String') {
            el.html('<textarea name="' + key + '" class="value" cols="40" rows="4"></textare>');
            input = $('textarea', el);
        } else {
            el.html('<input name="' + key + '" class="value" type="text" size="10">');
            input = $('input', el);
        }
        input.val(oldValue);
        input.off('mouseup');
        //console.log('activateTextInputField', input, id, key);
        input.focus();
        input.on('blur', function() {
            var newValue = input.val();
            if (id) {
                _Crud.crudUpdate(id, key, newValue);
            }
        });
    },
    row: function(id) {
        return $('#_' + id);
    },
    appendRow: function(type, item) {
        //console.log('appendRow', type, item);
        var id = item['id'];
        var table = _Crud.getTable(type);
        table.append('<tr id="_' + id + '"></tr>');
        _Crud.populateRow(id, item, type);
    },
    populateRow: function(id, item, type) {
        //console.log('populateRow', id, item, type, _Crud.keys[type]);
        var row = _Crud.row(id);
        if (_Crud.keys[type]) {
            $.each(_Crud.filterKeys(type, _Crud.keys[type]), function(k, key) {
                //console.log('populateRow for key', key, row);
                row.append('<td class="' + key + '"></td>');
                var cells = _Crud.cells(id, key);
                $.each(cells, function(i, cell) {
                    _Crud.populateCell(id, key, type, item[key], cell);
                });
            });
            row.append('<td class="actions"><button class="edit"><img src="icon/pencil.png"> Edit</button><button class="delete"><img src="icon/cross.png"> Delete</button></td>');
            _Crud.resize();

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
    populateCell: function(id, key, type, value, cell) {
        var isCollection = _Crud.isCollection(key, type);
        var relatedType = _Crud.relatedType(key, type);
        var readOnly = _Crud.readOnly(key, type);
        var simpleType;

        if (readOnly) {
            cell.addClass('readonly');
        }

        if (!relatedType) {

            var propertyType = _Crud.getPropertyType(type, key);

            if (propertyType === 'Boolean') {
                cell.append('<input name="' + key + '" ' + (readOnly ? 'class="readonly" readonly disabled ' : '') + 'type="checkbox" ' + (value ? 'checked="checked"' : '') + '>');
                if (!readOnly) {
                    $('input', cell).on('change', function() {
                        //console.log('change value for ' + key + ' to ' + $(this).prop('checked'));
                        if (id) {
                            _Crud.crudUpdate(id, key, $(this).prop('checked').toString());
                        }
                    });
                }
            } else if (propertyType === 'Date') {
                cell.html(nvl(formatValue(value), '<img src="icon/calendar.png">'));
                if (!readOnly) {
                    cell.on('mouseup', function(event) {
                        event.preventDefault();
                        var self = $(this);
                        var oldValue = self.text();
                        self.html('<input name="' + key + '" class="value" type="text" size="40">');
                        var input = $('input', self);
                        input.val(oldValue);
                        input.datetimepicker({
                            // ISO8601 Format: 'yyyy-MM-dd"T"HH:mm:ssZ'
                            separator: 'T',
                            dateFormat: 'yy-mm-dd',
                            timeFormat: 'HH:mm:ssz',
                            onClose: function() {
                                var newValue = input.val();
                                if (id) {
                                    _Crud.crudUpdate(id, key, newValue);
                                }
                            }
                        });
                        input.datetimepicker('show');
                        self.off('mouseup');
                    });
                }
            } else {
                cell.text(nvl(formatValue(value), ''));
                if (!readOnly) {
                    cell.on('mouseup', function(event) {
                        event.preventDefault();
                        var self = $(this);
                        _Crud.activateTextInputField(self, id, key, propertyType);
                    });
                    if (!id) { // create
                        _Crud.activateTextInputField(cell, id, key, propertyType);
                    }
                }
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
                _Crud.dialog('Add ' + simpleType, function() {
                }, function() {
                });
                _Crud.displaySearch(type, id, key, simpleType, dialogText);
            });

        } else {

            simpleType = lastPart(relatedType, '.');

            if (value) {

                _Crud.getAndAppendNode(type, id, key, value, cell);

            } else {

                cell.append('<img class="add" src="icon/add_grey.png">');
                $('.add', cell).on('click', function() {
                    _Crud.dialog('Add ' + simpleType + ' to ' + key, function() {
                    }, function() {
                    });
                    _Crud.displaySearch(type, id, key, simpleType, dialogText);
                });
            }

        }
        //searchField.focus();
    },
    getAndAppendNode: function(parentType, parentId, key, obj, cell) {
        //console.log(parentType, parentId, key, obj, cell);
        if (!obj)
            return;
        var id;
        if ((typeof obj) === 'object') {
            id = obj.id;
        } else {
            id = obj;
        }

        $.ajax({
            url: rootUrl + id + '/' + _Crud.view[parentType],
            type: 'GET',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                if (!data)
                    return;
                var node = data.result;
                //console.log('node', node);

                var displayName = _Crud.displayName(node);

                cell.append('<div title="' + displayName + '" id="_' + node.id + '" class="node ' + (node.type ? node.type.toLowerCase() : (node.tag ? node.tag : 'element')) + ' ' + node.id + '_">' + fitStringToWidth(displayName, 80) + '<img class="remove" src="icon/cross_small_grey.png"></div>');
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
    clearSearch: function(el) {
        if (_Crud.clearSearchResults(el)) {
            $('.clearSearchIcon').hide().off('click');
            $('.search').val('');
            main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');
            _Crud.initTabs();
            _Crud.updateUrl(_Crud.type);
        }
    },
    clearSearchResults: function(el) {
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
    search: function(searchString, el, type, onClickCallback) {

        _Crud.clearSearchResults(el);

        el.append('<h2 class="searchResultsTitle">Search Results</h2>');
        el.append('<div class="searchResults"></div>');
        var searchResults = $('.searchResults', el);

        _Crud.resize();

        //$('.search').select();

        var view = _Crud.view[_Crud.type];

        //var types = type ? [ type ] : _Crud.types;
        var types;
        var posOfColon = searchString.indexOf(':');
        if (posOfColon > -1) {
            var type = searchString.substring(0, posOfColon);
            if (!type.endsWith('s')) {
                type = type + 's';
            }
            types = [type.capitalize()];
            searchString = searchString.substring(posOfColon + 1, searchString.length);
            //console.log('filter search type', types, searchString);
        } else {
            types = type ? [type] : _Crud.types;
        }

        $.each(types, function(t, type) {
            var searchPart = searchString === '*' || searchString === '' ? '' : '&name=' + encodeURIComponent(searchString) + '&loose=1';
            var url = rootUrl + _Crud.restType(type) + '/' + view + _Crud.sortAndPagingParameters(type, 'name', 'asc', 1000, 1) + searchPart;
            searchResults.append('<div id="placeholderFor' + type + '" class="searchResultGroup resourceBox"><img class="loader" src="img/ajax-loader.gif">Searching for "' + searchString + '" in ' + type + '</div>');

            //console.log('Search URL', url)

            $.ajax({
                url: url,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                statusCode: {
                    200: function(data) {
                        if (!data)
                            return;

                        $('#placeholderFor' + type + '').remove();
                        if (data.result.length) {
                            searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox"><h3>' + type.capitalize() + '</h3></div>');
                        } else {
                            searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox">No results for ' + type.capitalize() + '</div>');
                            window.setTimeout(function() {
                                $('#resultsFor' + type).fadeOut('fast')
                            }, 1000);
                        }

                        $.each(data.result, function(i, node) {

                            //console.log('node', node);
                            var displayName = _Crud.displayName(node);
                            $('#resultsFor' + type, searchResults).append('<div title="' + displayName + '" id="_' + node.id + '" class="node ' + node.type.toLowerCase() + ' ' + node.id + '_">' + fitStringToWidth(displayName, 120) + '</div>');

                            var nodeEl = $('#_' + node.id, searchResults);
                            //console.log(node);
                            if (node.type === 'Image') {
                                nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
                            }

                            nodeEl.on('click', function(e) {
                                onClickCallback(e, node);
                            });

                        });
                    },
                    400: function() {
                        $('#placeholderFor' + type + '').remove();
                    },
                    401: function() {
                        $('#placeholderFor' + type + '').remove();
                    },
                    403: function() {
                        $('#placeholderFor' + type + '').remove();
                    },
                    422: function() {
                        $('#placeholderFor' + type + '').remove();
                    },
                    500: function() {
                        $('#placeholderFor' + type + '').remove();
                    },
                    503: function() {
                        $('#placeholderFor' + type + '').remove();
                    }
                }
            });

        });


    },
    displayName: function(node) {
        var displayName;
        if (node.type === 'Content' && node.content) {
            //displayName = $(node.content).text().substring(0, 100);
            displayName = escapeTags(node.content.substring(0, 100));
        } else {
            displayName = node.name ? node.name : node.id;
        }
        return displayName;
    },
    displaySearch: function(parentType, id, key, type, el) {
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
    removeRelatedObject: function(type, id, key, relatedObj) {
        var view = _Crud.view[_Crud.type];
        var urlType = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var url = rootUrl + urlType + '/' + id + '/' + view;
        if (_Crud.isCollection(key, type)) {
            var objects = [];
            $.ajax({
                url: url,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    if (!data)
                        return;
                    //console.log(key, data.result, data.result[key]);
                    $.each(data.result[key], function(i, obj) {
                        //console.log(obj, ' equals ', relatedObj);
                        if (!_Crud.equal(obj, relatedObj)) {
                            //console.log('not equal');
                            objects.push({'id': obj.id});
                        }
                    });

                    //console.log('new id array: ', objects);
                    var json = '{"' + key + '":null}';
                    //console.log('PUT ', json, ' to url ', url);

                    $.ajax({
                        url: url,
                        type: 'PUT',
                        dataType: 'json',
                        data: json,
                        contentType: 'application/json; charset=utf-8',
                        //async: false,
                        statusCode: {
                            200: function(data) {

                                var json = '{"' + key + '":' + JSON.stringify(objects) + '}';
                                //console.log('removeRelatedObject, setting new id array', url, objects, json);

                                $.ajax({
                                    url: url,
                                    type: 'PUT',
                                    dataType: 'json',
                                    data: json,
                                    contentType: 'application/json; charset=utf-8',
                                    //async: false,
                                    statusCode: {
                                        200: function(data) {
                                            //var rowEl = $('#' + id);
                                            var nodeEl = $('.' + _Crud.id(relatedObj) + '_');
                                            //console.log(rowEl, nodeEl);
                                            nodeEl.remove();
                                        },
                                        error: function(a, b, c) {
                                            console.log(a, b, c);
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
    addRelatedObject: function(type, id, key, relatedObj, callback) {
        var view = _Crud.view[_Crud.type];
        var urlType = _Crud.restType(type); //$('#' + type).attr('data-url').substring(1);
        var url = rootUrl + urlType + '/' + id + '/' + view;
        if (_Crud.isCollection(key, type)) {
            var objects = [];
            $.ajax({
                url: url,
                type: 'GET',
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                //async: false,
                success: function(data) {
                    //console.log(data.result[key]);
                    $.each(data.result[key], function(i, node) {
                        objects.push({'id': node.id});
                    });

                    if (!isIn(relatedObj.id, objects)) {
                        objects.push({'id': relatedObj.id});
                    }
                    var json = '{"' + key + '":' + JSON.stringify(objects) + '}';
                    //console.log(url, json);
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
            _Crud.crudUpdateObj(id, '{"' + key + '":' + JSON.stringify({'id': relatedObj.id}) + '}', function() {
                _Crud.crudRefresh(id, key);
                dialogCancelButton.click();
            });
        }
    },
    appendRowAsCSV: function(type, item, textArea) {
        if (_Crud.keys[type]) {
            //        console.log(type);
            $.each(_Crud.keys[type], function(k, key) {
                textArea.append('"' + nvl(item[key], '') + '"');
                if (k < _Crud.keys[type].length - 1) {
                    textArea.append(',');
                }
            });
            textArea.append('\n');
        }
    },
    dialog: function(text, callbackOk, callbackCancel) {

        if (browser) {

            dialogText.empty();
            dialogMsg.empty();
            dialogMeta.empty();
            //dialogBtn.empty();

            if (text)
                dialogTitle.html(text);
            if (callbackCancel)
                dialogCancelButton.on('click', function(e) {
                    e.stopPropagation();
                    callbackCancel();
                    dialogText.empty();
                    $.unblockUI({
                        fadeOut: 25
                    });
                    if (dialogCloseButton) {
                        dialogCloseButton.remove();
                    }
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
            var dw = Math.min(900, w - ml);
            var dh = Math.min(600, h - mt);
            //            var dw = (w-24) + 'px';
            //            var dh = (h-24) + 'px';

            var l = parseInt((w - dw) / 2);
            var t = parseInt((h - dh) / 2);

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
    resize: function() {
        var w = $(window).width();
        var h = $(window).height();

        var ml = 0;
        var mt = 24;

        // Calculate dimensions of dialog
        var dw = Math.min(900, w - ml);
        var dh = Math.min(600, h - mt);
        //            var dw = (w-24) + 'px';
        //            var dh = (h-24) + 'px';

        var l = parseInt((w - dw) / 2);
        var t = parseInt((h - dh) / 2);

        $('.blockPage').css({
            width: dw + 'px',
            height: dh + 'px',
            top: t + 'px',
            left: l + 'px'
        });

        var bw = (dw - 28) + 'px';
        var bh = (dh - 106) + 'px';

        $('#dialogBox .dialogTextWrapper').css({
            width: bw,
            height: bh
        });

        $('#resourceTabs .resourceBox table').css({
            height: h - ($('#resourceTabsMenu').height() + 184) + 'px',
            width:  w - 59 + 'px'
        });

        $('.searchResults').css({
            height: h - 103 + 'px'
        });

    },
    error: function(text, callback) {
        if (text) {
            $('#errorBox .errorText').html('<img src="icon/error.png"> ' + text);
        }
        if (callback) {
            $('#errorBox .closeButton').on('click', function(e) {
                e.stopPropagation();
                $.unblockUI({
                    fadeOut: 25
                });
            });
        }
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
    showDetails: function(node, create, typeParam) {

        var type = typeParam || node.type;
        if (!type) {
            Structr.error('Missing type', function() {
            }, function() {
            });
            return;
        }
        var typeDef = _Crud.schema[type];

        if (!dialogBox.is(':visible')) {
            if (node) {
                //console.log('Edit node', node);
                _Crud.dialog('Details of ' + type + ' ' + (node.name ? node.name : node.id) + '<span class="id"> [' + node.id + ']</span>', function() {
                }, function() {
                });
            } else {
                //console.log('Create new node of type', typeOnCreate);
                _Crud.dialog('Create new ' + type, function() {
                }, function() {
                });
            }
        }
        //console.log('readonly', readonly);

        if (create) {
            //console.log('edit mode, appending form');
            dialogText.append('<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th>');//<th>Type</th><th>Read Only</th></table></form>');
        } else {
            dialogText.html('<table class="props" id="details_' + node.id + '"><tr><th>Name</th><th>Value</th>');//<th>Type</th><th>Read Only</th></table>');
        }

        var table = $('table', dialogText);


        var keys = _Crud.keys[type];
        if (!keys) {
            keys = Object.keys(typeDef.views[_Crud.view[type]]);
        }

        $.each(keys, function(i, key) {
            var readOnly = _Crud.readOnly(key, type);
            var isCollection = _Crud.isCollection(key, type);
            var relatedType = _Crud.relatedType(key, type);
            if (readOnly || (create && (isCollection || relatedType))) {
                return;
            }

            table.append('<tr><td class="key"><label for="' + key + '">' + _Crud.formatKey(key) + '</label></td><td class="value ' + key + '"></td>');//<td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
            var cell = $('.' + key, table);
            if (node && node.id) {
                //console.log(node.id, key, type, node[key], cell);
                _Crud.populateCell(node.id, key, node.type, node[key], cell);
            } else {
                _Crud.populateCell(null, key, type, null, cell);
            }
        });

        if (create) {
            dialogCloseButton = $('.save', $('#dialogBox'));
            if (!(dialogCloseButton.length)) {
                dialogBox.append('<button class="save" id="saveProperties">Save</button>');
                dialogCloseButton = $('.save', $('#dialogBox'));
                dialogCloseButton.on('click', function() {
                    var form = $('#entityForm');
                    var json = JSON.stringify(_Crud.serializeObject(form));
                    if (create) {
                        _Crud.crudCreate(type, typeDef.url, json);
                    } else {
                        var id = form.attr('data-id');
                        _Crud.crudUpdateObj(id, json);
                    }
                });
            }
        }

        if (node && node.type === 'Image') {
            dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.id + '"></div></div>');
        }

    },
    formatKey: function(text) {
        if (!text)
            return '';
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
    },
    filterKeys: function(type, sourceArray) {

        if (!sourceArray) {
            return;
        }

        var filterSource = localStorage.getItem(crudHiddenColumnsKey + type);
        var filteredKeys = {};
        if (filterSource) {

            filteredKeys = JSON.parse(filterSource);
        }

        return $.grep(sourceArray, function(key) {
            return filteredKeys.hasOwnProperty(key) && filteredKeys[key] === 0;
        }, true);
    },
    toggleColumn: function(type, key) {

        var table = _Crud.getTable(type);
        var filterSource = localStorage.getItem(crudHiddenColumnsKey + type);
        var filteredKeys = {};
        if (filterSource) {

            filteredKeys = JSON.parse(filterSource);
        }

        var hidden = filteredKeys.hasOwnProperty(key) && filteredKeys[key] === 0;

        if (hidden) {

            filteredKeys[key] = 1;

        } else {

            filteredKeys[key] = 0;

            // remove column(s) from table
            $('th.' + key, table).remove();
            $('td.' + key, table).each(function(i, t) {
                t.remove();
            });
        }

        localStorage.setItem(crudHiddenColumnsKey + type, JSON.stringify(filteredKeys));

    },
    serializeObject: function(obj) {
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
    displayError: function(errorText) {
        var msgEl = $('.dialogMsg', $('#dialogBox'));
        msgEl.empty();
        msgEl.text(errorText);
    },
    idArray: function(ids) {
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
    id: function(objectOrId) {
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
    equal: function(obj1, obj2) {
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
