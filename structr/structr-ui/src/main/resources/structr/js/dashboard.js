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

var win = $(window);

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
        activeTab = $.cookie('structrActiveTab');
        if (debug) console.log('value read from cookie', activeTab);

        if (debug) console.log('onload');
        
        main.append('<div class="callToAction button">Create a new Page <img src="img/go.gif" alt="Create a new Page"></div>');
        
        $(main.children('.callToAction')).on('click', function(e) {
            e.stopPropagation();
            main.empty();
            Structr.activateMenuEntry('pages');
            Structr.modules['pages'].onload();
            
            document.location = '/structr/#pages'
            
            Command.createSimplePage();

            _Pages.resize();

        });

    },

    refresh : function() {
        dashboard.empty();
    }

};
