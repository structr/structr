/**************** config parameter **********************************/
var rootUrl =     '/splink_sgdb/api/';
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
  groups.append('<div class="group ' + group.id + '_">'
                                       + '<img class="typeIcon" src="icon/group.png">'
                                       + '<b>' + group.name + '</b> [' + group.id + ']'
                                       + '</div>');
  var div = $('.' + group.id + '_');
  div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="icon/group_delete.png">');
  div.append('<img title="Add User to Group ' + group.id + '" alt="Add User to Group ' + group.id + '" class="add_icon button" src="icon/user_add.png">');
  $('.add_icon', div).on('click', function() { addUser(this, group.id) });
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
        $('.' + groupId + '_ .delete_icon').on('click', function() { deleteUser(this, groupId) });
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
  var url = rootUrl + 'group';
  var data = '{ "type" : "group", "name" : "New group_' + Math.floor(Math.random() * (9999 - 1)) + '" }';
  var resp = $.ajax({
    url: url,
    //async: false,
    type: 'POST',
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    data: data,
    success: function(data) {
      var getUrl = resp.getResponseHeader('Location');
      $.ajax({
        url: getUrl + '/all',
        success: function(data) {
          var group = data.result;
          appendGroupElement(group);
          enable(button);
        }
      });
    }
  });
}

function addUser(button, groupId) {
  if (isDisabled(button)) return;
  disable(button);
//  var url = rootUrl + 'user';
  var data = '{ "type" : "user", "realName" : "New user_' + Math.floor(Math.random() * (9999 - 1)) + '" ' + (groupId ? ', "groupId" : ' + groupId : '') + ' }';
  var resp = $.ajax({
    url: url,
    //async: false,
    type: 'POST',
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    data: data,
    success: function(data) {
      var getUrl = resp.getResponseHeader('Location');
      $.ajax({
        url: getUrl + '/all',
        success: function(data) {
          var user = data.result;
          if (groupId) appendUserElement(user, groupId);
          appendUserElement(user);
          disable($('.' + groupId + '_ .delete_icon')[0]);
          enable(button);
        }
      });
    }
  });
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
              $('.' + groupId + '_ .' + userId + '_').hide('blind', { direction: "vertical" }, 200);
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

function deleteUser(button, userId, groupId) {
  var parent;
  if (!groupId) {
    parent = $('.' + userId + '_').parent('.group');
  } else {
    parent = $('.' + groupId + '_');
  }
  
  deleteNode(button, userId, function() {
    console.log($('.user', parent).length);
    if ($('.user', parent).length == 0) {
      enable($('.delete_icon', parent));
    }
  });

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
  $('.add_group_icon', main).on('click', function() { addGroup(this); });
}

function refreshUsers() {
  //users.empty();
  showUsers();
  //users.append('<div style="clear: both"></div>');
  users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="icon/user_add.png"> Add User</button>');
  $('.add_user_icon', main).on('click', function() { addUser(this); });
}

function appendUserElement(user, groupId) {
  var div;
  if (groupId) {
    $('.' + groupId + '_').append('<div class="nested user ' + user.id + '_">'
                   + '<img class="typeIcon" src="icon/user.png">'
                   + ' <b class="title realName">' + user.realName + '</b> '
                   //+ '[' + user.uuid + '] ' + (groupId ? '(group: ' + groupId + ')' : '')
                   //+ '<b>' + name + '</b>'
                   + '</div>');
    div = $('.' + groupId + '_ .' + user.id + '_')
    div.append('<img title="Remove user ' + user.id + ' from group ' + groupId + '" '
             + 'alt="Remove user ' + user.id + ' from group ' + groupId + '" class="delete_icon button" src="icon/delete.png">');
    $('.delete_icon', div).on('click', function() { removeUserFromGroup(user.id, groupId) });
    div.append('<img title="Edit" alt="Edit" class="edit_icon button" src="icon/pencil.png">');
    $('.edit_icon', div).on('click', function() {editUserProperties(this, user.id, groupId); });
  } else {
    users.append('<div class="nested user ' + user.id + '_">'
                   + '<img class="typeIcon" src="icon/user.png">'
                   + ' <b>' + user.realName + '</b> '
                   //+ '[' + user.uuid + '] ' + (groupId ? '(group: ' + groupId + ')' : '')
                   //+ '<b>' + name + '</b>'
                   + '</div>');
    div = $('#users .' + user.id + '_');
    div.append('<img title="Delete user ' + user.id + '" '
             + 'alt="Delete user ' + user.id + '" class="delete_icon button" src="icon/user_delete.png">');
    $('.delete_icon', div).on('click', function() { deleteUser(this, user.id) });
    div.append('<img title="Edit" alt="Edit" class="edit_icon button" src="icon/pencil.png">');
    enable($('.edit_icon', div), function() { showProperties(this, user.id, 'all', $('.' + user.id + '_', users)); });
  }
}

function editUserProperties(button, userId, groupId) {
  var element = $('.' + groupId + '_ .' + userId + '_');
  showProperties(button, userId, 'all', element);
}
