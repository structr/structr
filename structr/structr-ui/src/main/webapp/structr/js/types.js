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

var types;

$(document).ready(function() {
    Structr.registerModule('types', _Types);
    Structr.classes.push('typeDefinition');
});

var _Types = {

    init : function() {
    },
	
    onload : function() {
        if (debug) console.log('onload');
        if (palette) palette.remove();

        main.append('<table><tr><td id="types"></td></tr></table>');
        types = $('#types');
        _Types.refreshTypes();
    },
    
    refreshTypes : function() {
        types.empty();
        if (Command.list('TypeDefinition')) {
            groups.append('<button class="add_type_icon button"><img title="Add Type Definition" alt="Add Type Definition" src="icon/type_add.png"> Add Type Definition</button>');
            $('.add_type_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'TypeDefinition';
                return Command.create(entity);
            });
        }
    }

};