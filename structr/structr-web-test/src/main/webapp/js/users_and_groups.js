/**************** config parameter **********************************/
var rootUrl =     '/structr-web-test/json/';
var viewRootUrl = '/structr-web-test/html/';
/********************************************************************/
var main;
var groups;
var users;
$(document).ready(function() {
    main = $('#main');
    groups = $('#groups');
    users = $('#users');
    refreshGroups();
    refreshUsers();

    connect();

});

function showUsers() {
    $.getJSON(rootUrl + 'users/all',function(data) {
        if (!data || data.length == 0 || !data.result) return;
        $(data.result).each(function(i,user) {
            appendUserElement(user);
        });
    });
}

function showGroups() {
    $.getJSON(rootUrl + 'groups/all', function(data) {
        if (!data || data.length == 0 || !data.result) return;
        $(data.result).each(function(i,group) {
            appendGroupElement(group);
        });
    });
}

function appendGroupElement(group) {
    groups.append('<div class="nested top group ' + group.id + '_">'
        + '<img class="typeIcon" src="icon/group.png">'
        + '<b>' + group.name + '</b> [' + group.id + ']'
        + '</div>');
    var div = $('.' + group.id + '_');
    div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="icon/group_delete.png">');
    div.append('<img title="Add User to Group ' + group.id + '" alt="Add User to Group ' + group.id + '" class="add_icon button" src="icon/user_add.png">');
    $('.add_icon', div).on('click', function() {addUser(this, group.id)});
    showUsersOfGroup(group.id);
}

function showUsersOfGroup(groupId) {
    var url = rootUrl + 'group/' + groupId + '/users';
    console.log(url);
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        //headers: { 'X-User' : 457 },
        success: function(data) {
            if (!data || data.length == 0 || !data.result) {
                $('.' + groupId + '_ .delete_icon').on('click', function() {deleteGroup(this, groupId)});
                return;
            }
            disable($('.' + groupId + '_ .delete_icon'));
            var result = data.result;
            $(result).each(function(i,user) {
                appendUserElement(user, groupId);
            });
        }
    });
}

function addGroup(button) {
    if (isDisabled(button)) return;
    disable(button);
    buttonClicked = button;
    var url = rootUrl + 'group';
    var data = '{ "command" : "CREATE" , "type" : "Group", "name" : "New group_' + Math.floor(Math.random() * (9999 - 1)) + '" }';
    console.log(data);
    ws.send(data);
//    var resp = $.ajax({
//        url: url,
//        //async: false,
//        type: 'POST',
//        dataType: 'json',
//        contentType: 'application/json; charset=utf-8',
//        data: data,
//        success: function(data) {
//            var getUrl = resp.getResponseHeader('Location');
//            $.ajax({
//                url: getUrl + '/all',
//                success: function(data) {
//                    var group = data.result;
//                    appendGroupElement(group);
//                    enable(button);
//                }
//            });
//        }
//    });
}

function addUser(button, groupId) {
    if (isDisabled(button)) return;
    disable(button);
    buttonClicked = button;
//    var url = rootUrl + 'user';
    var name = Math.floor(Math.random() * (9999 - 1));
    var data = '{ "command" : "CREATE" , "type" : "User", "name" : "' + name + '", "realName" : "New user_' + name + '" ' + (groupId ? ', "groupId" : ' + groupId : '') + ' }';
    console.log(data);
    ws.send(data);

//    var resp = $.ajax({
//        url: url,
//        //async: false,
//        type: 'POST',
//        dataType: 'json',
//        contentType: 'application/json; charset=utf-8',
//        data: data,
//        success: function(data) {
//            var getUrl = resp.getResponseHeader('Location');
//            $.ajax({
//                url: getUrl + '/all',
//                success: function(data) {
//                    var user = data.result;
//                    if (groupId) appendUserElement(user, groupId);
//                    appendUserElement(user);
//                    disable($('.' + groupId + '_ .delete_icon')[0]);
//                    enable(button);
//                }
//            });
//        }
//    });
}

function removeUserFromGroup(userId, groupId) {
    console.log('removeUserFromGroup: userId=' + userId + ', groupId=' + groupId);
    $.ajax({
        url: rootUrl + groupId + '/out',
        success: function(data) {
            $(data.result).each(function(i,rel) {
                if (rel.endNodeId == userId) {
                    var url = rootUrl + groupId + '/out/' + rel.id;
                    console.log(url);
                    $.ajax({
                        url: url,
                        type: 'DELETE',
                        success: function(data) {
                            $('.' + groupId + '_ .' + userId + '_').hide('blind', {direction: "vertical"}, 200);
                            $('.' + groupId + '_ .' + userId + '_').remove();
                            if ($('.' + groupId + '_ .user').length == 0) {
                                enable($('.' + groupId + '_ .delete_icon'));
                            }
                        }
                    });
                }
            });
        }
    });
}

function deleteUser(button, user, groupId) {
    console.log('deleteUser ' + user);
    var parent;
    if (!groupId) {
        parent = $('.' + user.id + '_').parent('.group');
    } else {
        parent = $('.' + groupId + '_');
    }
  
    deleteNode(button, user, "function() { console.log($('.user', parent).length); if ($('.user', parent).length == 0) { enable($('.delete_icon', parent)); } }");

}

function deleteGroup(button, groupId) {
    buttonClicked = button;
    var data = '{ "type" : "Group" , "id" : "' + groupId + '" , "uuid" : "' + groupId + '" }';
    deleteNode(button, $.parseJSON(data));
}
      
function refreshGroup(groupId) {
    //console.log('#resource_' + resourceId + '_element_' + id);
    $('#group_' + groupId + ' > div.nested').remove();
    showUsersOfGroup(groupId);
}

function refreshGroups() {
    groups.empty();
    showGroups();
    //groups.append('<div style="clear: both"></div>');
    groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="icon/group_add.png"> Add Group</button>');
    $('.add_group_icon', main).on('click', function() {addGroup(this);});
}

function refreshUsers() {
    //users.empty();
    showUsers();
    //users.append('<div style="clear: both"></div>');
    users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="icon/user_add.png"> Add User</button>');
    $('.add_user_icon', main).on('click', function() {addUser(this);});
}

function appendUserElement(user, groupId) {
    var div;
    if (groupId) {
        $('.' + groupId + '_').append('<div class="nested user ' + user.id + '_">'
            + '<img class="typeIcon" src="icon/user.png">'
            + ' <b>' + user.realName + '</b> '
        //+ '[' + user.id + '] ' + (groupId ? '(group: ' + groupId + ')' : '')
        //+ '<b>' + name + '</b>'
            + '</div>');
        div = $('.' + groupId + '_ .' + user.id + '_')
        div.append('<img title="Remove user ' + user.id + ' from group ' + groupId + '" '
            + 'alt="Remove user ' + user.id + ' from group ' + groupId + '" class="delete_icon button" src="icon/delete.png">');
        $('.delete_icon', div).on('click', function() {removeUserFromGroup(user, groupId)});
        $('b', div).on('click', function() {editUserProperties(this, user, groupId);});
    } else {
        users.append('<div class="nested user ' + user.id + '_">'
            + '<img class="typeIcon" src="icon/user.png">'
            + ' <b>' + user.realName + '</b> '
        //+ '[' + user.id + '] ' + (groupId ? '(group: ' + groupId + ')' : '')
        //+ '<b>' + name + '</b>'
            + '</div>');
        div = $('#users .' + user.id + '_');
        div.append('<img title="Delete user ' + user.id + '" '
            + 'alt="Delete user ' + user.id + '" class="delete_icon button" src="icon/user_delete.png">');
        $('.delete_icon', div).on('click', function() {deleteUser(this, user)});
        $('b', div).on('click', function() {editUserProperties(this, user, groupId);});
    }
}

function editUserProperties(button, user, groupId) {
    console.log(button);
//    if (isDisabled(button)) return;
//    disable(button);
    showProperties(button, user, 'all', $('.' + user.id + '_'));
}
