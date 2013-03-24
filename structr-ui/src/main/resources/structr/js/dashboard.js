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

var win = $(window);
var canvas;

$(document).ready(function() {
    Structr.registerModule('dashboard', _Dashboard);


//    win.resize(function() {
//        _Dashboard.resize();
//    });

});

var _Dashboard = {

    icon : 'icon/page.png',
    add_icon : 'icon/page_add.png',
    delete_icon : 'icon/page_delete.png',
    clone_icon : 'icon/page_copy.png',

    init : function() {
    },

    onload : function() {
        
        _Dashboard.init();
        
        activeTab = $.cookie('structrActiveTab');
        log('value read from cookie', activeTab);

        log('onload');
        
        main.append('<div class="searchBox"><input class="search" name="search" size="20" placeholder="Search"><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');
        //main.append('<div class="canvas"></div>');
        //canvas = $('.canvas', main);
        
        main.append('<div class="canvas" id="graph"></div>');
        
        
        searchField = $('.search', main);
        searchField.focus();
        
        searchField.keyup(function(e) {
            var searchString = $(this).val();
            if (searchString && searchString.length && e.keyCode === 13) {
                
                $('.clearSearchIcon').show().on('click', function() {
                    _Crud.clearSearch(main);
                });
               
               Command.search(searchString);
               
                
            } else if (e.keyCode === 27 || searchString === '') {
                
                _Dashboard.clearSearch();
                
            }
            
            
        });
        
        drawGraph();

    },

    appendNode : function(node) {
        
        graph.render(node.id, 0, [canvas.w / 2, canvas.h / 2]);
//        var hasChildren = node.children && node.children.length;
//        console.log('appendNode', node, hasChildren);
//        canvas.append('<div id="id_' + node.id + '" class="node">'
//            + '<b title="' + node.name + '" class="name_">' + fitStringToSize(node.name, 200) + '</b> <span class="id">' + node.id + '</span>'
//            + '</div>');
//        var div = Structr.node(node.id);
//
//        div.append('<img title="Delete node ' + node.id + '" alt="Delete node ' + node.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
//        $('.delete_icon', div).on('click', function(e) {
//            e.stopPropagation();
//            _Entities.deleteNode(this, node)
//        });
//
//        div.draggable();
//        
//        _Entities.appendEditPropertiesIcon(div, node);
//        _Entities.setMouseOver(div);
//        
//        return div;
    },

    clearSearch : function() {
        _Dashboard.clearSearchResults();
         $('.clearSearchIcon').hide().off('click');
         $('.search').val('');
    },
    
    clearSearchResults : function() {
        canvas.empty();
    },
   
};
