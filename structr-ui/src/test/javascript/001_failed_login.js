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

var s = require('../setup');

var testName = '001_failed_login';
var heading = "Failed Login", sections = [];
var desc = "This animation shows what happens if an incorrect username/password combination was entered."
var numberOfTests = 2;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);
    
    sections.push('If you enter a wrong combination of username and password, the system does not allow you to log in.');

    casper.waitForSelector('#usernameField').then(function() {
        s.animatedType(this, '#usernameField', false, 'wrong');
    });

    casper.waitForSelector('#passwordField').then(function() {
        s.animatedType(this, '#passwordField', false, 'wrong');
    });

    casper.then(function() {
        s.mousePointer(casper, { left: 600, top: 400 });
        s.moveMousePointerTo(casper, '#loginButton');
    });

    casper.then(function() {
        this.click('#loginButton');
    });

    casper.waitForSelector('#errorText', function() {

        test.assertEval(function() {
            return $('#errorText').text() === 'Wrong username or password!';
        });
        
        test.assertEval(function() {
            return !$('#dashboard').is(':visible');
        });

    });
    
    casper.then(function() {
        s.animateHtml(testName, heading, sections);
        this.exit();
    });

    casper.run();

});
