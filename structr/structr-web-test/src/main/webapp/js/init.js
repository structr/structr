/* 
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

/**************** config parameter **********************************/
var rootUrl =     '/structr/json/';
var viewRootUrl = '/structr/html/';
var wsRoot = '/structr/ws';
var headers = {
    'X-User' : 0
};
/********************************************************************/

var main;
var debug = false;
var onload;

$(document).ready(function() {
    if (debug) console.log('Debug mode');

    main = $('#main');
    //refreshEntities('group');
    //refreshEntities('user');
    $('#import_json').on('click', function() {
        var jsonArray = $.parseJSON($('#json_input').val());
        $(jsonArray).each(function(i, json) {
            //console.log(json);
            createEntity(json);
        });
    //var json = $.parseJSON('{ "test" : "abc" }');

    });
    $('#loginButton').on('click', function() {
        var username = $('#usernameField').val();
        var password = $('#passwordField').val();
        doLogin(username, password);
    });
    $('#logoutLink').on('click', function() {
        doLogout();
    });


    $('#dashboardLink').on('click', function() {
        main.empty();
        activateMenuEntry('dashboardLink');
    });

    $('#resourcesLink').on('click', function() {
        main.empty();
        activateMenuEntry('resourcesLink');
    });

    $('#usersAndGroupsLink').on('click', function() {
        $(this).addClass('active');
        activateMenuEntry('usersAndGroupsLink');

        main.empty();

        onload = function() {
            if (debug) console.log('onload');
            main.append('<table><tr><td id="groups"></td><td id="users"></td></tr></table>');
            //main.append('<div id="groups"></div>');
            groups = $('#groups');
            //main.append('<div id="users"></div>');
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

    $('#usernameField').keypress(function(e) {
        if(e.which == 13) {
            jQuery(this).blur();
            jQuery('#loginButton').focus().click();
        }
    });
    $('#passwordField').keypress(function(e) {
        if(e.which == 13) {
            jQuery(this).blur();
            jQuery('#loginButton').focus().click();
        }
    });
});

function activateMenuEntry(name) {

    $('.menu a').each(function(i,v) {
        $(this).removeClass('active');
    });
    $('#' + name).addClass('active');

}