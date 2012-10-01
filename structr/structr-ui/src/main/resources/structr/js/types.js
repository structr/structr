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
    
    type_icon : 'icon/database_table.png',

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
            types.append('<button class="add_type_icon button"><img title="Add Type Definition" alt="Add Type Definition" src="' + _Types.type_icon + '"> Add Type Definition</button>');
            $('.add_type_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'TypeDefinition';
                return Command.create(entity);
            });
        }
    },
    
    appendTypeElement : function(type) {
		
        if (debug) console.log('appendTypeElement', type);
        
        
        types.append('<div id="_' + type.id + '" structr_type="folder" class="node folder ' + type.id + '_">'
            + '<img class="typeIcon" src="'+ _Types.type_icon + '">'
            + '<b class="name_">' + type.name + '</b> <span class="id">' + type.id + '</span>'
            + '</div>');
        
        var div = $('#_' + type.id);
        
        div.append('<img title="Delete Type Definition ' + type.id + '" alt="Delete Type Definition ' + type.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, type);
        });
        
        _Entities.appendAccessControlIcon(div, type);
        _Entities.appendEditPropertiesIcon(div, type);
        _Entities.setMouseOver(div);
		
        return div;
    }
};