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
var casper = require('casper').create(s.casperOptions);

var testName = 'drag_element';
var heading = "Drag Element"
var desc = "This animation shows how a content element is dragged into a page."
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);

    casper.then(function() {
        s.animatedType(this, '#usernameField', false, 'admin');
    });

    casper.then(function() {
        s.animatedType(this, '#passwordField', false, 'admin');
    });

    casper.then(function() {
        s.mousePointer(casper, { left: 180, top: 180 });
        s.moveMousePointerTo(casper, '#loginButton');
    });

    casper.then(function() {
        this.click('#loginButton');
    });

    casper.waitWhileVisible('#dialogBox', function() {

        test.assertEval(function() {
            return $('#errorText').text() === '';
        });

        test.assertEval(function() {
            return $('#pages').is(':visible');
        });

    });
    
    casper.then(function() {
        s.moveMousePointerTo(casper, '#add_page');
    });

    casper.then(function() {
        this.click('#add_page');
    });

    casper.wait(2000, function() {
        test.assertEval(function() {
            return $('#errorText').text() === '';
        });
    });

    casper.then(function() {
        s.dragDropElement(casper, '#add_content', '#pages');
    });

    casper.wait(5000);

    casper.then(function() {
        
        s.animateHtml(testName, heading, desc);

        test.done();
        this.exit();
        
    });

    casper.run();

});
