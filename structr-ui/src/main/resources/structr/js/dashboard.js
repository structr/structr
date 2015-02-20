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

var win = $(window);
var engine, mode;
var nodeIds = [], relIds = [];
var activeTabRightDashboardKey = 'structrActiveTabRightDashboard_' + port;
var activeTabLeftDashboardKey = 'structrActiveTabLeftDashboard_' + port;
var activeTabLeftDashboard, activeTabRightDashboard;
var queriesSlideout, displaySlideout, filtersSlideout, nodesSlideout, relationshipsSlideout, graph;
var savedQueriesKey = 'structrSavedQueries_' + port;
var relTypes = [], nodeTypes = [];

$(document).ready(function() {
    Structr.registerModule('dashboard', _Dashboard);
});

var _Dashboard = {
    icon: 'icon/page.png',
    add_icon: 'icon/page_add.png',
    delete_icon: 'icon/page_delete.png',
    clone_icon: 'icon/page_copy.png',
    init: function() {
    },
    onload: function() {

        _Dashboard.init();

        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Dashboard');

        activeTabLeftDashboard = localStorage.getItem(activeTabRightDashboardKey);
        activeTabRightDashboard = localStorage.getItem(activeTabLeftDashboardKey);

        main.prepend(
                '<div id="queries" class="slideOut slideOutLeft"><div class="compTab" id="queriesTab">Queries</div><div><button id="clear-canvas">Clear Canvas</button></div></div>'
                + '<div id="display" class="slideOut slideOutLeft"><div class="compTab" id="displayTab">Display Options</div></div>'
                + '<div id="filters" class="slideOut slideOutLeft"><div class="compTab" id="filtersTab">Filters</div><div id="nodeFilters"><h3>Node Filters</h3></div><div id="relFilters"><h3>Relationship Filters</h3></div></div>'
                + ' <div class="canvas" id="graph"></div>'
                //+ '<div id="nodes" class="slideOut slideOutRight"><div class="compTab" id="nodesTab">Nodes</div></div>'
                //+ '<div id="relationships" class="slideOut slideOutRight"><div class="compTab" id="relationshipsTab">Relationships</div></div>'
                );

        queriesSlideout = $('#queries');
        displaySlideout = $('#display');
        filtersSlideout = $('#filters');

        graph = $('#graph');

        nodesSlideout = $('#nodes');
        relationshipsSlideout = $('#relationships');

        lsw = queriesSlideout.width() + 12;
        rsw = nodesSlideout.width() + 12;

        $('.slideOut').on('mouseover', function() {
            running = false;
            return true;
        });

        $('.slideOut').on('mouseout', function() {
            running = true;
            engine.update();
            return true;
        });

        $('#queriesTab').on('click', function() {
            if (Math.abs(queriesSlideout.position().left + lsw) <= 3) {
                Structr.closeLeftSlideOuts([displaySlideout, filtersSlideout], activeTabLeftDashboardKey);
                Structr.openLeftSlideOut(queriesSlideout, this, activeTabLeftDashboardKey);
            } else {
                Structr.closeLeftSlideOuts([queriesSlideout], activeTabLeftDashboardKey);
            }
        });

        $('#displayTab').on('click', function() {
            if (Math.abs(displaySlideout.position().left + lsw) <= 3) {
                Structr.closeLeftSlideOuts([queriesSlideout, filtersSlideout], activeTabLeftDashboardKey);
                Structr.openLeftSlideOut(displaySlideout, this, activeTabLeftDashboardKey, function() {
                    //console.log('Display options opened');
                });
            } else {
                Structr.closeLeftSlideOuts([displaySlideout], activeTabLeftDashboardKey);
            }
        });

        $('#filtersTab').on('click', function() {
            if (Math.abs(filtersSlideout.position().left + lsw) <= 3) {
                Structr.closeLeftSlideOuts([queriesSlideout, displaySlideout], activeTabLeftDashboardKey);
                Structr.openLeftSlideOut(filtersSlideout, this, activeTabLeftDashboardKey, function() {
                    //console.log('Filters opened');
                });
            } else {
                Structr.closeLeftSlideOuts([filtersSlideout], activeTabLeftDashboardKey);
            }
        });

//        $('#nodesTab').on('click', function() {
//            if (nodesSlideout.position().left === $(window).width()) {
//                Structr.closeSlideOuts([relationshipsSlideout], activeTabRightDashboardKey);
//                Structr.openSlideOut(nodesSlideout, this, activeTabRightDashboardKey, function() {
//                    console.log('Nodes opened');
//                });
//            } else {
//                Structr.closeSlideOuts([nodesSlideout], activeTabRightDashboardKey);
//            }
//        });
//
//        $('#relationshipsTab').on('click', function() {
//            if (relationshipsSlideout.position().left === $(window).width()) {
//                Structr.closeSlideOuts([nodesSlideout], activeTabRightDashboardKey);
//                Structr.openSlideOut(relationshipsSlideout, this, activeTabRightDashboardKey, function() {
//                    console.log('Rels opened');
//                });
//            } else {
//                Structr.closeSlideOuts([relationshipsSlideout], activeTabRightDashboardKey);
//            }
//        });

        if (activeTabLeftDashboard) {
            $('#' + activeTabLeftDashboard).addClass('active').click();
        }

        if (activeTabRightDashboard) {
            $('#' + activeTabRightDashboard).addClass('active').click();
        }

        queriesSlideout.append('<div class="query-box"><textarea class="search" name="rest" cols="39" rows="4" placeholder="Enter a REST query here"></textarea><img class="clearSearchIcon" id="clear-rest" src="icon/cross_small_grey.png">'
                + '<button id="exec-rest">Execute REST query</button></div>');

        queriesSlideout.append('<div class="query-box"><textarea class="search" name="cypher" cols="39" rows="4" placeholder="Enter a Cypher query here"></textarea><img class="clearSearchIcon" id="clear-cypher" src="icon/cross_small_grey.png">'
                + '<button id="exec-cypher">Execute Cypher query</button></div>');

        queriesSlideout.append('<div id="cypher-params"><h3>Cypher Parameters</h3><img id="add-cypher-parameter" src="icon/add.png">');
        _Dashboard.appendCypherParameter($('#cypher-params'));

        $('#clear-canvas').on('click', function() {
            nodeIds.length = 0;
            relIds.length = 0;
            engine.clear();
        });

        $('#exec-rest').on('click', function() {
            var query = $('.search[name=rest]').val();
            if (query && query.length) {
                _Dashboard.execQuery(query, 'rest');
            }
        });

        $('#exec-cypher').on('click', function() {
            var query = $('.search[name=cypher]').val();
            var params = {};
            var names = $.map($('[name="cyphername[]"]'), function(n) {
                return $(n).val();
            });
            var values = $.map($('[name="cyphervalue[]"]'), function(v) {
                return $(v).val();
            });

            for (var i = 0; i < names.length; i++) {
                params[names[i]] = values[i];
            }

            if (query && query.length) {
                _Dashboard.execQuery(query, 'cypher', JSON.stringify(params));
            }
        });

        $('#add-cypher-parameter').on('click', function() {
            _Dashboard.appendCypherParameter($('#cypher-params'));
        });


        _Dashboard.activateClearSearchIcon();

        queriesSlideout.append('<div><h3>Saved Queries</h3></div>');
        _Dashboard.listSavedQueries();

        //_Dashboard.restoreSavedQuery(0);


        searchField = $('.search', queriesSlideout);
        searchField.focus();
        searchField.keydown(function(e) {
            var rawSearchString = $(this).val();
            var searchString = rawSearchString;

            var self = $(this);
            var type = self.attr('name');
            if (type !== 'cypher') {

                var type;
                var posOfColon = rawSearchString.indexOf(':');
                if (posOfColon > -1) {
                    type = rawSearchString.substring(0, posOfColon);
                    type = type.capitalize();
                    searchString = rawSearchString.substring(posOfColon + 1, rawSearchString.length);
                }
            }
            if (searchString && searchString.length) {
                _Dashboard.activateClearSearchIcon(type);
            } else {
                _Dashboard.clearSearch(type);
            }
            
            
            if (searchString && searchString.length && e.which === 13) {
                //console.log('Search executed', searchString, type);
                
                if (!shiftKey) {
                    _Dashboard.execQuery(searchString, type);
                    return false;
                }
                
            } else if (e.which === 27 || rawSearchString === '') {
                _Dashboard.clearSearch(type);
            }
        });

        engine = new Engine(graph);
        engine.initialize();
        engine.update();

        win.resize(function() {
            _Dashboard.resize();
        });

    },
    execQuery: function(query, type, params) {

        //console.log('exec', type, 'query: ', query, ', with parameters.', params);

        if (query && query.length) {

            if (type === 'cypher') {
                Command.cypher(query.replace(/(\r\n|\n|\r)/gm,""), params);
                _Dashboard.saveQuery(query, 'cypher', params);
            } else {
                Command.rest(query.replace(/(\r\n|\n|\r)/gm,""));
                _Dashboard.saveQuery(query, 'rest');
            }

            _Dashboard.listSavedQueries();

        }
    },
    saveQuery: function(query, type, params) {
        var savedQueries = JSON.parse(localStorage.getItem(savedQueriesKey)) || [];
        var exists = false;
        $.each(savedQueries, function(i, q) {
            if (q.query === query && q.params === params) {
                exists = true;
            }
        });
        if (!exists) {
            savedQueries.unshift({'type': type, 'query': query, 'params': params});
            localStorage.setItem(savedQueriesKey, JSON.stringify(savedQueries));
        }
    },
    removeSavedQuery: function(i) {
        var savedQueries = JSON.parse(localStorage.getItem(savedQueriesKey)) || [];
        savedQueries.splice(i, 1);
        localStorage.setItem(savedQueriesKey, JSON.stringify(savedQueries));
        _Dashboard.listSavedQueries();
    },
    restoreSavedQuery: function(i, exec) {
        var savedQueries = JSON.parse(localStorage.getItem(savedQueriesKey)) || [];
        var query = savedQueries[i];
        $('.search[name=' +  query.type + ']').val(query.query);
        _Dashboard.activateClearSearchIcon(query.type);
        $('#cypher-params input').remove();
        $('#cypher-params br').remove();
        $('#cypher-params img.remove-cypher-parameter').remove();
        if (query.params && query.params.length) {
            var parObj = JSON.parse(query.params);
            $.each(Object.keys(parObj), function(i, key) {
                _Dashboard.appendCypherParameter($('#cypher-params'), key, parObj[key]);
            });
        } else {
            _Dashboard.appendCypherParameter($('#cypher-params'));
        }
        if (exec) {
            _Dashboard.execQuery(query.query, query.type, query.params);
        }
    },
    listSavedQueries: function() {
        $('#saved-queries').empty();
        queriesSlideout.append('<div id="saved-queries"></div>');
        var savedQueries = JSON.parse(localStorage.getItem(savedQueriesKey)) || [];
        $.each(savedQueries, function(q, query) {
            if (query.type === 'cypher') {
                $('#saved-queries').append('<div class="saved-query cypher-query"><img class="replay" alt="Cypher Query" src="icon/control_play_blue.png">' + query.query + '<img class="remove-query" src="icon/cross_small_grey.png"></div>');
            } else {
                $('#saved-queries').append('<div class="saved-query rest-query"><img class="replay" alt="REST Query" src="icon/control_play.png">' + query.query + '<img class="remove-query" src="icon/cross_small_grey.png"></div>');
            }
        });
        $('.saved-query').on('click', function() {
            _Dashboard.restoreSavedQuery($(this).index());
        });
        $('.replay').on('click', function() {
            _Dashboard.restoreSavedQuery($(this).parent().index(), true);
        });
        $('.remove-query').on('click', function() {
            _Dashboard.removeSavedQuery($(this).parent().index());
        });
    },
    activateClearSearchIcon: function(type) {
        var icon = $('#clear-' + type);
        icon.show().on('click', function() {
            $(this).hide();
            $('.search[name=' + type + ']').val('').focus();
        });
    },
    clearSearch: function(type) {
        $('#clear-' + type).hide().off('click');
        $('.search[name=' + type + ']').val('').focus();
    },
    clearGraph: function() {
        canvas.element.empty();
    },
    appendObj: function(obj) {
        _Dashboard.loadTypeDefinition(obj.type, function(typeDef) {
            if (typeDef && typeDef.isRel) {
                relTypes.push(typeDef);

                if (obj.sourceId && obj.targetId) {
                    _Dashboard.loadNode(obj.sourceId, function() {
                        _Dashboard.loadNode(obj.targetId, function() {
                            engine.addRelationship(obj.relType, obj.sourceId, obj.targetId);
                        });
                    });
                }

            } else {
                nodeTypes.push(typeDef);
                _Dashboard.appendNode(obj);
            }
        });
    },
    appendNode: function(node, depth, callback) {

        if (node) {

            if (nodeIds.indexOf(node.id) === -1) {

                nodeIds.push(node.id);

                engine.addNode(node);

                if (mode === 'auto') {
                    window.setTimeout(function() {

                        _Dashboard.loadRelationships(node.id, depth === undefined ? 1 : depth + 1);

                    }, 100);
                }

                if (callback) {
                    callback();
                }
            }

            // start update loop
            engine.update();
        }
    },
    loadRelationships: function(nodeId, depth) {

        if (nodeId) {

            $.ajax({
                url: rootUrl + nodeId + '/out',
                dataType: "json",
                success: function(data) {

                    if (!data || data.length === 0 || !data.result || !data.result.length) {
                        return;
                    }

                    var results = data.result;
                    var count = 0, i = 0;

                    while (i < results.length && count < maxRels) {

                        var r = results[i++];

                        if (relIds.indexOf(r.id) === -1) {

                            relIds.push(r.id);

                            engine.addRelationship(r.relType, r.sourceId, r.targetId);
                            _Dashboard.loadNode(r.targetId, depth === undefined ? 1 : depth + 1);
                        }
                    }
                }
            });

            $.ajax({
                url: rootUrl + nodeId + '/in',
                dataType: "json",
                success: function(data) {

                    if (!data || data.length === 0 || !data.result || !data.result.length) {
                        return;
                    }

                    var results = data.result;
                    var count = 0, i = 0;

                    while (i < results.length && count < maxRels) {

                        var r = results[i++];

                        if (relIds[r.id] === undefined) {

                            relIds[r.id] = 1;

                            engine.addRelationship(r.relType, r.sourceId, r.targetId);
                            _Dashboard.loadNode(r.sourceId, depth === undefined ? 1 : depth + 1);
                        }
                    }
                }
            });
        }
    },
    loadNode: function(nodeId, depth, callback) {
        if (nodeId) {
            Command.get(nodeId, function(n) {
                _Dashboard.appendNode(n, depth, callback);
            });
        }
    },
    loadRelationship: function(relId) {
        if (relId) {
            Command.get(relId, function(r) {
                engine.addRelationship(r.relType, r.sourceId, r.targetId);
            });
        }
    },
    resize: function() {

        var windowHeight = win.height();
        var offsetHeight = 360;

        $('#saved-queries').css({
            height: windowHeight - offsetHeight + 'px'
        });

        var ch = win.height() - 61;

        graph.css({
            height: ch,
            width: win.width(),
        });

        $('canvas', graph).css({
            height: ch,
            width: win.width(),
        });


    },
    loadTypeDefinition: function(type, callback) {
        var url = rootUrl + '_schema/' + type;
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function(data) {
                    if (callback) {
                        callback(data.result[0]);
                    }
                },
                401: function(data) {
                    console.log(data);
                },
                404: function(data) {
                    console.log(data);
                },
                422: function(data) {
                    console.log(data);
                }
            }


        }).always(function(data) {
            if (callback) {
                callback(data.result[0]);
            }
        });
    },
    appendCypherParameter: function(el, key, value) {
        el.append('<div><img class="remove-cypher-parameter" src="icon/delete.png"> <input name="cyphername[]" type="text" placeholder="name" size="10" value="' + (key || '') + '"> <input name="cyphervalue[]" type="text" placeholder="value" size="10" value="' + (value || '') + '"></div>');
        $('.remove-cypher-parameter', el).on('click', function() {
            $(this).parent().remove();
        });
    }
};

