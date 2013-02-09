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

var propertyDefinitions;

$(document).ready(function() {
    Structr.registerModule('propertyDefinitions', _PropertyDefinitions);
    Structr.classes.push('propertyDefinition');
});

var _PropertyDefinitions = {
    
    type_icon : 'icon/database_table.png',

    init : function() {
        pageSize['PropertyDefinition'] = 100;
        page['PropertyDefinition'] = 1;
    },
	
    onload : function() {
        _PropertyDefinitions.init();
        if (palette) palette.remove();

        main.append('<table><tr><td id="propertyDefinitions"></td></tr></table>');
        propertyDefinitions = $('#propertyDefinitions');
        _PropertyDefinitions.refreshPropertyDefinitions();
    },
    
    refreshPropertyDefinitions : function() {
        propertyDefinitions.empty();
        if (Command.list('PropertyDefinition')) {
            propertyDefinitions.append('<button class="add_type_icon button"><img title="Add PropertyDefinition" alt="Add PropertyDefinition" src="' + _PropertyDefinitions.type_icon + '"> Add PropertyDefinition</button>');
            $('.add_type_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'PropertyDefinition';
                return Command.create(entity);
            });
        }
            
        Structr.addPager(propertyDefinitions, 'PropertyDefinition');
    },
    
    appendPropertyDefinitionElement : function(propertyDefinition) {
		
        console.log('appendTypeElement', propertyDefinition);
        
        propertyDefinitions.append('<div id="_' + propertyDefinition.id + '" structr_type="propertyDefinition" class="node propertyDefinition ' + propertyDefinition.id + '_">'
            + '<img class="typeIcon" src="'+ _PropertyDefinitions.type_icon + '">'
            + '<b class="name_">' + propertyDefinition.name + '</b> <span class="id">' + propertyDefinition.id + '</span>'
            + '</div>');
        
        var div = $('#_' + propertyDefinition.id);
        
        div.append('<img title="Delete Type ' + propertyDefinition.id + '" alt="Delete Type ' + propertyDefinition.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, propertyDefinition);
        });
        
        _Entities.appendAccessControlIcon(div, propertyDefinition);
        _Entities.appendEditPropertiesIcon(div, propertyDefinition);
        _Entities.setMouseOver(div);
		
        return div;
    }
};