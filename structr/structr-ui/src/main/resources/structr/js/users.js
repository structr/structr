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

var groups, users;

$(document).ready(function() {
    Structr.registerModule('usersAndGroups', _UsersAndGroups);
    Structr.classes.push('user');
    Structr.classes.push('group');
});

var _UsersAndGroups = {

    init : function() {
    //Structr.classes.push('user');
    //Structr.classes.push('group');
    },
	
    onload : function() {
        //Structr.activateMenuEntry('usersAndGroups');
        if (debug) console.log('onload');
        if (palette) palette.remove();

        main.append('<table><tr><td id="users"></td><td id="groups"></td></tr></table>');
        groups = $('#groups');
        users = $('#users');
        _UsersAndGroups.refreshGroups();
        _UsersAndGroups.refreshUsers();
    },
    
    refreshGroups : function() {
        groups.empty();
        if (Command.list('Group')) {
            groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="icon/group_add.png"> Add Group</button>');
            $('.add_group_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'Group';
                return Command.create(entity);
            });
        }
    },

    refreshUsers : function() {
        users.empty();
        if (Command.list('User')) {
            users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="icon/user_add.png"> Add User</button>');
            $('.add_user_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'User';
                return Command.create(entity);
            });
        }
    },

    removeUserFromGroup : function(userId, groupId) {

        var group = Structr.node(groupId);

        var user = Structr.node(userId, groupId);

        if ($('.' + userId + '_', users).length) {
            user.remove();
        } else {
            users.append(user);//.animate();
        }

        _Entities.resetMouseOverState(user);
        
        if (debug) console.log('removeUserFromGroup, containesNodes?', group, Structr.containsNodes(group));

        if (!Structr.containsNodes(group)) {
            _Entities.removeExpandIcon(group);
        }

        $('.delete_icon', user).replaceWith('<img title="Delete user ' + user.name + '" '
            + 'alt="Delete user ' + user.name + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', user).on('click', function(e) {
            e.stopPropagation();
            _UsersAndGroups.deleteUser(this, Structr.entity(userId));
        });
        
        user.draggable({
            revert: 'invalid',
            containment: '#main',
            zIndex: 1
        });

        var numberOfUsers = $('.user', group).size();
        if (debug) console.log(numberOfUsers);
        if (numberOfUsers == 0) {
            enable($('.delete_icon', group)[0]);
        }

        if (debug) console.log('removeUserFromGroup: userId=' + userId + ', groupId=' + groupId);
        
    },

    deleteUser : function(button, user) {
        if (debug) console.log('deleteUser ' + user);
        _Entities.deleteNode(button, user);
    },

    deleteGroup : function(button, group) {
        if (debug) console.log('deleteGroup ' + group);
        _Entities.deleteNode(button, group);
    },

    appendGroupElement : function(group, hasChildren) {
        if (debug) console.log('appendGroupElement', group, hasChildren);
        groups.append('<div id="_' + group.id + '" class="node group ' + group.id + '_">'
            + '<img class="typeIcon" src="icon/group.png">'
            + '<b class="name_">' + group.name + '</b> <span class="id">' + group.id + '</span>'
            + '</div>');
        var div = Structr.node(group.id);

        div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _UsersAndGroups.deleteGroup(this, group)
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
                Command.createAndAdd(groupId, nodeData);
            }
        });
        
        _Entities.appendEditPropertiesIcon(div, group);
        _Entities.setMouseOver(div);
        
        return div;
    },

    appendUserElement : function(user, groupId, removeExisting) {
        if (debug) console.log('appendUserElement', user, groupId, removeExisting);

        if (!groupId && user.group.length) return false;

        removeExisting = true;

        var div;
        var newDelIcon = '<img title="Remove user \'' + user.name + '\' from group ' + groupId + '" '
        + 'alt="Remove user ' + user.name + ' from group ' + groupId + '" class="delete_icon button" src="icon/user_delete.png">'
        var delIcon;
        div = $('.' + user.id + '_', users);
        
        if (groupId) {
            
            //div = Structr.node(user.id, groupId);

            var parent = Structr.node(groupId);
            
            if (debug) console.log('parent, div', parent, div);
            
            if (removeExisting && div && div.length) {
                parent.append(div.css({
                    top: 0,
                    left: 0
                }));
                delIcon = $('.delete_icon', div);
                delIcon.replaceWith(newDelIcon);
                
                if (debug) console.log('################ disable delete icon');
                

            } else {
                
                if (debug) console.log('### new user, appending to ', parent);
                
                
                parent.append('<div class="node user ' + user.id + '_">'
                    + '<img class="typeIcon" src="icon/user.png">'
                    //				+ ' <b class="realName">' + user.realName + '</b> [<span class="id">' + user.id + '</span>]'
                    + ' <b class="name_">' + user.name + '</b> <span class="id">' + user.id + '</span>'
                    + '</div>');
                div = Structr.node(user.id, groupId);
                div.append(newDelIcon);
                
            }
            delIcon = $('.delete_icon', div);
            delIcon.on('click', function(e) {
                e.stopPropagation();
                Command.removeSourceFromTarget(user.id, groupId);
            });

            // disable delete icon on parent
            disable($('.delete_icon', parent)[0]);
            div.draggable('disable').removeClass('ui-state-disabled').removeClass('ui-draggable-disabled').removeClass('ui-draggable');

        } else {

            if (Structr.node(user.id).length) return false;

            users.append('<div class="node user ' + user.id + '_">'
                + '<img class="typeIcon" src="icon/user.png">'
                //				+ ' <b class="realName">' + user.realName + '</b> [' + user.id + ']'
                + ' <b class="name_">' + user.name + '</b> <span class="id">' + user.id + '</span>'
                + '</div>');
            div = $('.' + user.id + '_', users);
            
            newDelIcon = '<img title="Delete user \'' + user.name + '\'" '
            + 'alt="Delete user \'' + user.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            delIcon = $('.delete_icon', div);
            
            if (removeExisting && delIcon && delIcon.length) {
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
                //                helper: 'clone',
                revert: 'invalid',
                containment: '#main',
                zIndex: 1
            });
        }
        _Entities.appendEditPropertiesIcon(div, user);
        _Entities.setMouseOver(div);

        return div;

    }

};