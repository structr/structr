/**************** config parameter **********************************/
var rootUrl =     '/structr-web-test/json/';
var viewRootUrl = '/structr-web-test/html/';
/********************************************************************/
var main;
var groups;
var users;
var debug = true;
var onload;

$(document).ready(function() {
    if (debug) console.log('Debug mode');
    main = $('#main');

    onload = function() {
        if (debug) console.log('onload');
        main.append('<div id="groups"></div>');
        groups = $('#groups');
        main.append('<div id="users"></div>');
        users = $('#users');
        refreshGroups();
        refreshUsers();
    }

    user = $.cookie("structrUser");

    connect();

    ws.onopen = function() {

        if (debug) console.log('logged in? ' + loggedIn);
        if (!loggedIn) {
            if (debug) console.log('no');
            $('#logoutLink').html('Login');
            login();
        } else {
            if (debug) console.log('Current user: ' + user);
            //            $.cookie("structrUser", username);
            $('#logoutLink').html(' Logout <span class="username">' + user + '</span>');
            onload();
        }
    }
});

function showUsers() {
    if (debug) console.log('showUsers()');
    return showEntities('User');
}

function showGroups() {
    if (debug) console.log('showGroups()');
    return showEntities('Group');
}

function showUsersOfGroup(groupId) {
//    var url = rootUrl + 'group/' + groupId + '/users';
//    console.log(url);
//    $.ajax({
//        url: url,
//        dataType: 'json',
//        contentType: 'application/json; charset=utf-8',
//        async: false,
//        //headers: { 'X-User' : 457 },
//        success: function(data) {
//            if (!data || data.length == 0 || !data.result) {
//                $('.' + groupId + '_ .delete_icon').on('click', function() {
//                    deleteGroup(this, groupId)
//                });
//                return;
//            }
//            disable($('.' + groupId + '_ .delete_icon'));
//            var result = data.result;
//            $(result).each(function(i,user) {
//                appendUserElement(user, groupId);
//            });
//        }
//    });
}

function addGroup(button) {
    if (isDisabled(button)) return false;
    disable(button);
    buttonClicked = button;
    var data = '{ "command" : "CREATE" , "data" : { "type" : "Group", "name" : "New group_' + Math.floor(Math.random() * (9999 - 1)) + '" } }';
    if (debug) console.log(data);
    return send(data);
}

function addUser(button, group) {
    if (debug) console.log('addUser to group + ' + group);
    if (isDisabled(button)) return false;
    disable(button);
    buttonClicked = button;
    var name = Math.floor(Math.random() * (9999 - 1));
    var data = '{ "command" : "CREATE" , "callback" : "test" , "data" : { "type" : "User", "name" : "' + name + '", "realName" : "New user_' + name + '" } }';
    if (debug) console.log(data);
    return send(data);
}

function addUserToGroup(user, group) {
    var data = '{ "command" : "ADD" , "id" : "' + group.id + '" , "data" : { "id" : "' + user.id + '" }';
    return send(data);
}

function removeUserFromGroup(userId, groupId) {
    if (debug) console.log('removeUserFromGroup: userId=' + userId + ', groupId=' + groupId);
//	$.ajax({
//		url: rootUrl + groupId + '/out',
//		success: function(data) {
//			$(data.result).each(function(i,rel) {
//				if (rel.endNodeId == userId) {
//					var url = rootUrl + groupId + '/out/' + rel.id;
//					console.log(url);
//					$.ajax({
//						url: url,
//						type: 'DELETE',
//						success: function(data) {
//							$('.' + groupId + '_ .' + userId + '_').hide('blind', {
//								direction: "vertical"
//							}, 200);
//							$('.' + groupId + '_ .' + userId + '_').remove();
//							if ($('.' + groupId + '_ .user').length == 0) {
//								enable($('.' + groupId + '_ .delete_icon'));
//							}
//						}
//					});
//				}
//			});
//		}
//	});
}

function deleteUser(button, user, groupId) {
    if (debug) console.log('deleteUser ' + user);
    var parent;
    if (!groupId) {
        parent = $('.' + user.id + '_').parent('.group');
    } else {
        parent = $('.' + groupId + '_');
    }
  
    //	deleteNode(button, user, "function() { console.log($('.user', parent).length); if ($('.user', parent).length == 0) { enable($('.delete_icon', parent)); } }");
    deleteNode(button, user);

}

function deleteGroup(button, group) {
    //	buttonClicked = button;
    var data = '{ "type" : "Group" , "name" : "' + group.name + '" , "id" : "' + group.id + '" }';
    deleteNode(button, $.parseJSON(data));
}
      
function refreshGroup(groupId) {
    //console.log('#resource_' + resourceId + '_element_' + id);
    $('#group_' + groupId + ' > div.nested').remove();
    showUsersOfGroup(groupId);
}

function refreshGroups() {
    groups.empty();
    if (showGroups()) {
        //groups.append('<div style="clear: both"></div>');
        groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="icon/group_add.png"> Add Group</button>');
        $('.add_group_icon', main).on('click', function() {
            addGroup(this);
        });
    }
}

function refreshUsers() {
    users.empty();
    if (showUsers()) {
        //users.append('<div style="clear: both"></div>');
        users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="icon/user_add.png"> Add User</button>');
        $('.add_user_icon', main).on('click', function() {
            addUser(this);
        });
    }
}

function appendGroupElement(group) {
    groups.append('<div class="nested top group ' + group.id + '_">'
        + '<img class="typeIcon" src="icon/group.png">'
        + '<b class="name">' + group.name + '</b> [' + group.id + ']'
        + '</div>');
    var div = $('.' + group.id + '_');
    div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="icon/group_delete.png">');
    div.append('<img title="Add User to Group ' + group.id + '" alt="Add User to Group ' + group.id + '" class="add_icon button" src="icon/user_add.png">');
    $('.add_icon', div).on('click', function() {
        addUser(this, group)
    });
    $('.delete_icon', div).on('click', function() {
        deleteGroup(this, group)
    });
    $('b', div).on('click', function() {
        showProperties(this, group, 'all', $('.' + group.id + '_', groups));
    });
    showUsersOfGroup(group.id);
}

function appendUserElement(user, groupId) {
    var div;
    if (groupId) {
        $('.' + groupId + '_').append('<div class="nested user ' + user.id + '_">'
            + '<img class="typeIcon" src="icon/user.png">'
            + ' <b class="realName">' + user.realName + '</b> '
            //+ '[' + user.id + '] ' + (groupId ? '(group: ' + groupId + ')' : '')
            //+ '<b>' + name + '</b>'
            + '</div>');
        div = $('.' + groupId + '_ .' + user.id + '_')
        div.append('<img title="Remove user ' + user.id + ' from group ' + groupId + '" '
            + 'alt="Remove user ' + user.id + ' from group ' + groupId + '" class="delete_icon button" src="icon/delete.png">');
        $('.delete_icon', div).on('click', function() {
            removeUserFromGroup(user, groupId)
        });
        $('b', div).on('click', function() {
            editUserProperties(this, user, groupId);
        });
    } else {
        users.append('<div class="nested user ' + user.id + '_">'
            + '<img class="typeIcon" src="icon/user.png">'
            + ' <b class="realName">' + user.realName + '</b> '
            //+ '[' + user.id + '] ' + (groupId ? '(group: ' + groupId + ')' : '')
            //+ '<b>' + name + '</b>'
            + '</div>');
        div = $('#users .' + user.id + '_');
        div.append('<img title="Delete user ' + user.id + '" '
            + 'alt="Delete user ' + user.id + '" class="delete_icon button" src="icon/user_delete.png">');
        $('.delete_icon', div).on('click', function() {
            deleteUser(this, user)
        });
        $('b', div).on('click', function() {
            editUserProperties(this, user, groupId);
        });
    }
}

function editUserProperties(button, user, groupId) {
    showProperties(button, user, 'all', $('.' + user.id + '_'));
}
