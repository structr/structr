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

var trees;
var win = $(window);

$(document).ready(function() {
    Structr.registerModule('trees', _Trees);
    //    Structr.classes.push('image');
    _Trees.resize();
    win.resize(function() {
        _Trees.resize();
    });
});

var _Trees = {

    icon : 'icon/page_white.png',
    add_data_node_icon : 'icon/database_add.png',
    data_node_icon : 'icon/folder_database.png',
    delete_folder_icon : 'icon/folder_delete.png',
	
    init : function() {
    },

    resize : function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 82;

        if (trees) {
            trees.css({
                width: Math.max(180, Math.min(windowWidth/3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }


    },

    onload : function() {
        
        _Trees.init();
        
        log('onload');
        if (palette) palette.remove();

        main.append('<table><tr><td id="trees"></td></tr></table>');
        trees = $('#trees');
        //        images = $('#images');
        
        _Trees.refreshFolders();
    },

    unload : function() {
        $(main.children('table')).remove();
    },

	
    refreshFolders : function() {
        trees.empty();
        trees.append('<button class="add_data_node_icon button"><img title="Add Data Node" alt="Add Data Node" src="' + _Trees.add_data_node_icon + '"> Add Data Node</button>');
        
        //Command.list(type, pageSize[type], page[type], sort, order);
        Command.list('DataNode', 1000, 1, 'name', 'asc');
        
        //if (Structr.addPager(folders, 'Folder')) {

        $('.add_data_node_icon', main).on('click', function(e) {
            e.stopPropagation();
            var entity = {};
            entity.type = 'DataNode';
            Command.create(entity);
        });
        //}
    },

    getIcon : function(file) {
        var icon = _Trees.icon; // default
        if (file && file.contentType) {

            if (file.contentType.indexOf('pdf') > -1) {
                icon = 'icon/page_white_acrobat.png';
            } else if (file.contentType.indexOf('text') > -1) {
                icon = 'icon/page_white_text.png';
            } else if (file.contentType.indexOf('xml') > -1) {
                icon = 'icon/page_white_code.png';
            }
        }
        
        return icon;
    },

    appendDataNode : function(dataNode) {
        
        log('appendDataNode', dataNode, dataNode.parent);

        var hasParent = dataNode.parent && dataNode.parent.id;
        
        log(dataNode.name, 'has parent?', hasParent);

        var parentId, parentNode;
        if (dataNode.parent && dataNode.parent.id) {
            parentId = dataNode.parent.id;
            parentNode = Structr.node(parentId);
        }

        var parent = parentNode ? parentNode : trees;

        parent.append('<div id="id_' + dataNode.id + '" structr_type="folder" class="node folder">'
            + '<img class="typeIcon" src="'+ _Trees.data_node_icon + '">'
            + '<b title="' + dataNode.name + '" class="name_">' + fitStringToSize(dataNode.name, 200) + '</b> <span class="id">' + dataNode.id + '</span>'
            + '</div>');
        
        var div = Structr.node(dataNode.id);
        
        var delIcon = div.children('.delete_icon');
        
        if (parent != trees) {
            var newDelIcon = '<img title="Remove folder ' + dataNode.name + '\' from folder ' + dataNode.parent.id + '" alt="Remove folder ' + dataNode.name + '\' from folder ' + dataNode.parent.id + '" class="delete_icon button" src="' + _Files.delete_folder_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                //delIcon = $('.delete_icon', div);
            }
            div.children('.delete_icon').on('click', function(e) {
                e.stopPropagation();
                Command.removeChild(dataNode.id);
            });
            //disable($('.delete_icon', parent)[0]);
			
        } else {
            newDelIcon = '<img title="Delete folder \'' + dataNode.name + '\'" alt="Delete folder \'' + dataNode.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                //delIcon = $('.delete_icon', div);
            } 
            div.children('.delete_icon').on('click', function(e) {
                e.stopPropagation();
                _Entities.deleteNode(this, dataNode);
            });
        }
        
        var hasChildren = (dataNode.folders && dataNode.folders.length) || (dataNode.files && dataNode.files.length);
        
        log(dataNode.name, 'has children?', hasChildren, 'is expanded?', isExpanded(dataNode.id));
        
        _Entities.appendExpandIcon(div, dataNode, hasChildren);
        
        div.draggable({
            revert: 'invalid',
            //helper: 'clone',
            containment: '#main',
            stack: 'div'
        });
        
        div.droppable({
            accept: '.folder, .file, .image',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            drop: function(event, ui) {
                var self = $(this);
                var nodeId = getId(ui.draggable);
                var parentNodeId = getId(self);
                log('nodeId, parentNodeId', nodeId, parentNodeId);
                if (!(nodeId == parentNodeId)) {
                    var nodeData = {};
                    nodeData.id = nodeId;
                    addExpandedNode(parentNodeId);
                    //log('addExpandedNode(folderId)', addExpandedNode(folderId));
                    Command.appendData(nodeId, parentNodeId, null); // TODO: add key
                    $(ui.draggable).remove();
                //Command.createAndAdd(folderId, nodeData);
                }
            }
        });

        _Entities.appendAccessControlIcon(div, dataNode);
        _Entities.appendEditPropertiesIcon(div, dataNode);
        _Entities.setMouseOver(div);
		
        return div;
    }
    
};
