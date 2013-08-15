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

var groups, users;

$(document).ready(function() {
    Structr.registerModule('usersAndGroups', _UsersAndGroups);
    Structr.classes.push('user');
    Structr.classes.push('group');
});

var _UsersAndGroups = {

    init : function() {
        Structr.initPager('User', 1, 25);
        Structr.initPager('Group', 1, 25);
    },

    onload : function() {
        _UsersAndGroups.init();
        //Structr.activateMenuEntry('usersAndGroups');
        log('onload');
        if (palette) palette.remove();

        main.append('<table><tr><td id="users"></td><td id="groups"></td></tr></table>');
        groups = $('#groups');
        users = $('#users');
        _UsersAndGroups.refreshGroups();
        _UsersAndGroups.refreshUsers();
    },
    
    refreshGroups : function() {
        groups.empty();
        groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="icon/group_add.png"> Add Group</button>');
        $('.add_group_icon', main).on('click', function(e) {
            e.stopPropagation();
            return Command.create({'type':'Group'});
        });
        Structr.addPager(groups, 'Group');
    },

    refreshUsers : function() {
        users.empty();
        users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="icon/user_add.png"> Add User</button>');
        $('.add_user_icon', main).on('click', function(e) {
            e.stopPropagation();
            return Command.create({'type':'User'});
        });
        Structr.addPager(users, 'User');
    },

    deleteUser : function(button, user) {
        log('deleteUser ' + user);
        _Entities.deleteNode(button, user);
    },

    deleteGroup : function(button, group) {
        log('deleteGroup ' + group);
        _Entities.deleteNode(button, group);
    },

    appendGroupElement : function(group) {
        var hasChildren = group.members && group.members.length;
        log('appendGroupElement', group, hasChildren);
        groups.append('<div id="id_' + group.id + '" class="node group">'
            + '<img class="typeIcon" src="icon/group.png">'
            + '<b title="' + group.name + '" class="name_">' + group.name + '</b> <span class="id">' + group.id + '</span>'
            + '</div>');
        var div = Structr.node(group.id);

        div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _UsersAndGroups.deleteGroup(this, group);
        });
        
        _Entities.appendExpandIcon(div, group, hasChildren);

        div.droppable({
            accept: '.user',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            drop: function(event, ui) {
                var self = $(this);
                var userId = getId(ui.draggable);
                var groupId = getId(self);
                var nodeData = {};
                nodeData.id = userId;
                //console.log('addExpandedNode(groupId)', groupId);
                addExpandedNode(groupId);
                Command.appendUser(userId, groupId);
                //Command.createAndAdd(groupId, nodeData);
                $(ui.draggable).remove();
            }
        });
        
        _Entities.appendEditPropertiesIcon(div, group);
        _Entities.setMouseOver(div);
        
        return div;
    },

    appendUserElement : function(user, group) {
        log('appendUserElement', user);

        var delIcon;
        var div = Structr.node(user.id);
        
        if (user.groups && user.groups.length) {
            
            var group = StructrModel.obj(user.groups[0]);
            
            var groupId = group.id;
            var newDelIcon = '<img title="Remove user \'' + user.name + '\' from group \'' + group.name + '\'" '
            + 'alt="Remove user ' + user.name + ' from group \'' + group.name + '\'" class="delete_icon button" src="icon/user_delete.png">'

            var parent = Structr.node(groupId);
            
            log('parent, div', parent, div);
            
            if (div && div.length) {
                parent.append(div.css({
                    top: 0,
                    left: 0
                }));
                delIcon = $('.delete_icon', div);
                delIcon.replaceWith(newDelIcon);
                
                log('################ disable delete icon');
                

            } else {
                
                log('### new user, appending to ', parent);
                
                parent.append('<div id="id_' + user.id + '" class="node user">'
                    + '<img class="typeIcon" src="icon/user.png">'
                    //				+ ' <b class="realName">' + user.realName + '</b> [<span class="id">' + user.id + '</span>]'
                    + ' <b title="' + user.name + '" class="name_">' + user.name + '</b> <span class="id">' + user.id + '</span>'
                    + '</div>');
                div = Structr.node(user.id);
                div.append(newDelIcon);
                
            }
            delIcon = $('.delete_icon', div);
            delIcon.on('click', function(e) {
                e.stopPropagation();
                Command.removeSourceFromTarget(user.id, groupId);
            });

            // disable delete icon on parent
            disable($('.delete_icon', parent)[0]);
            
            div.removeClass('ui-state-disabled').removeClass('ui-draggable-disabled').removeClass('ui-draggable');

        } else {

            users.append('<div id="id_' + user.id + '" class="node user">'
                + '<img class="typeIcon" src="icon/user.png">'
                //				+ ' <b class="realName">' + user.realName + '</b> [' + user.id + ']'
                + ' <b title="' + user.name + '" class="name_">' + user.name + '</b> <span class="id">' + user.id + '</span>'
                + '</div>');
            div = Structr.node(user.id);
            
            newDelIcon = '<img title="Delete user \'' + user.name + '\'" '
            + 'alt="Delete user \'' + user.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            delIcon = $('.delete_icon', div);
            
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            }            
            
            delIcon.on('click', function(e) {
                e.stopPropagation();
                _UsersAndGroups.deleteUser(this, user);
            });

			
            div.draggable({
                revert: 'invalid',
                containment: '#main',
                zIndex: 99
            });
        }
        _Entities.appendEditPropertiesIcon(div, user);
        _Entities.setMouseOver(div);

        return div;

    }

};