/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

var dashboard;

$(document).ready(function() {
    Structr.registerModule('dashboard', _Dashboard);
});

var _Dashboard = {
    icon: 'icon/page.png',
    add_icon: 'icon/page_add.png',
    delete_icon: 'icon/page_delete.png',
    clone_icon: 'icon/page_copy.png',
    init: function() {
    },
    onload: function() {
        _Dashboard.init();
        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Dashboard');
        
        main.append('<div id="dashboard"></div>');
        dashboard = $('#dashboard', main);
        
        var aboutMe = _Dashboard.appendBox('About Me', 'about-me');
        aboutMe.append('<div class="dashboard-info">You are currently logged in as <b>' + me.username + '<b>.</div>');
        if (me.isAdmin) {
            aboutMe.append('<div class="dashboard-info red">Your have admin rights.</div>');
        }
        
        var myPages = _Dashboard.appendBox('My Pages', 'my-pages');
        myPages.append('<div class="dashboard-info">You own the following <a href="/structr/#pages">pages</a>:</div>');
        Command.getByType('Page', 10, 1, 'version', 'desc', null, function(p) {
            myPages.append('<div class="dashboard-info"><a href="/' + p.name + '" target="_blank"><img class="icon" src="icon/page.png"></a> <a href="/' + p.name + '" target="_blank">' + p.name + '</a> (ver. ' + (p.version ? p.version : '') + ')</div>');
        });
        
        var myFiles = _Dashboard.appendBox('My Files', 'my-files');
        myFiles.append('<div class="dashboard-info">Your most edited files are:</div>');
        Command.getByType('File', 10, 1, 'version', 'desc', null, function(f) {
            myFiles.append('<div class="dashboard-info"><a href="/' + f.name + '" target="_blank"><img class="icon" src="' + _Files.getIcon(f) + '"></a> <a href="/' + f.name + '" target="_blank">' + f.name + '</a> (ver. ' + (f.version ? f.version : '') + ')</div>');
        });
        
        var myImages = _Dashboard.appendBox('My Images', 'my-images');
        myImages.append('<div class="dashboard-info">Your most edited images are:</div>');
        Command.getByType('Image', 10, 1, 'version', 'desc', null, function(i) {
            myImages.append('<div class="dashboard-info"><a href="/' + i.name + '" target="_blank"><img class="icon" src="' + _Images.getIcon(i) + '"></a> <a href="/' + i.name + '" target="_blank">' + i.name + '</a> (ver. ' + (i.version ? i.version : '') + ')</div>');
        });

    },
    appendBox: function(heading, id) {
        dashboard.append('<div id="' + id + '" class="dashboard-box"><div class="dashboard-header"><h2>' + heading + '</h2></div></div>');
        return $('#' + id, main);
    }
};

