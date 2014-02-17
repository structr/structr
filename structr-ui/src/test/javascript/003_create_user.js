/*
 *  Copyright (C) 2010-2013 Axel Morgner
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

var s = require('../setup');

var testName = '003_create_user';
var heading = "Create User", sections = [];
var desc = "This animation shows how to create a new user."
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);
    
    casper.thenEvaluate(function() {
        window.localStorage.clear();
    }, {});
    
    sections.push('Enter username and password.');
    
    casper.then(function() {
        s.animatedType(this, '#usernameField', false, 'admin');
    });

    casper.then(function() {
        s.animatedType(this, '#passwordField', false, 'admin');
    });

    sections.push('Click on the login button.');
    
    casper.then(function() {
        s.mousePointer(casper, { left: 600, top: 400 });
        s.moveMousePointerTo(casper, '#loginButton');
    });

    casper.then(function() {
        this.click('#loginButton');
    });

    casper.waitForSelector('#errorText', function() {

        test.assertEval(function() { return !($('#errorText').text() === 'Wrong username or password!'); });

        test.assertEval(function() { return $('#pages').is(':visible'); });

    });
    
    sections.push('Click on the "Users and Groups" menu entry.');
    
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

        test.done();
        this.exit();
        
    });

    casper.run();

});
