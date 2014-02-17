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

var testName = '004_create_page';
var heading = "Create Page", sections = [];
var desc = "This animation shows how an empty page is created."
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);
    
    casper.thenEvaluate(function() {
        window.localStorage.clear();
    }, {});
    
    sections.push('Login with username and password.');
    
    casper.then(function() {
        s.animatedType(this, '#usernameField', false, 'admin');
    });

    casper.then(function() {
        s.animatedType(this, '#passwordField', false, 'admin');
    });

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
    
    sections.push('If it is not already active, click on the "Pages" menu entry.');
    
    sections.push('Click on the icon with the green plus on the rightmost tab above the preview frame.');
    
    casper.then(function() {
        s.moveMousePointerTo(casper, '#add_page');
    });

    casper.then(function() {
        this.click('#add_page');
    });

    casper.wait(5000);
    
    // TODO: Add assertion to detect creation of page

    sections.push('A new page with a random name has been created. The page is automatically loaded into the preview window.');

    casper.then(function() {
        
        s.animateHtml(testName, heading, sections);

        test.done();
        this.exit();
        
    });

    casper.run();

});
