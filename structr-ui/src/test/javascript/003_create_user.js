/*
 *  Copyright (C) 2010-2015 Structr GmbH
 *
 *  This file is part of Structr <http://structr.org>.
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

var s = require('../setup'),
    login = require('../templates/login'),
    createPage = require('../templates/createPage');
    
var testName = '003_create_user';
var heading = "Create User", sections = [];
var desc = "This animation shows how to create a new user."
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);
    
    sections.push('Click on the "Users and Groups" menu entry.');

    login.init(test, 'admin', 'admin');
    
    casper.then(function() {
        s.moveMousePointerTo(casper, '#security_');
    });

    casper.then(function() {
        this.click('#security_');
    });

    casper.then(function() {
        s.moveMousePointerTo(casper, '#usersAndGroups_');
    });

    casper.then(function() {
        this.click('#usersAndGroups_');
    });

    casper.wait(1000);
    
    sections.push('Click the "Add User" icon.');
    
    casper.then(function() {
        s.moveMousePointerTo(casper, '.add_user_icon');
    });

    casper.then(function() {
        this.click('.add_user_icon');
    });

    casper.wait(1000);

    casper.then(function() {
        test.assertEval(function() {
            return $('#users .node.user').size() === 2;
        });
    });

    sections.push('A new user with a random name has been created in the users area.');

    casper.then(function() {
        s.animateHtml(testName, heading, sections);
        this.exit();
    });

    casper.run();

});
