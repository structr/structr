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

var testName = 'inline_editing';
var heading = "Inline Editing"
var desc = "This animation shows how to edit text directly in the rendered page."
var numberOfTests = 4;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);
    
    casper.thenEvaluate(function() {
        window.localStorage.clear();
    }, {});
    
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
    
    casper.then(function() {
        s.moveMousePointerTo(casper, '#add_page');
    });

    casper.then(function() {
        this.click('#add_page');
    });

    casper.wait(1000, function() {
        test.assertEval(function() {
            return $('#errorText').text() === '';
        });
    });

    casper.wait(1000, function() {
    });

    casper.then(function() {
        s.mousePointer(casper, { left: 395, top: 190 });
    });

    casper.then(function() {
        s.clickInIframe(this, 'body div:nth-child(2) span');
    });

    casper.wait(1000, function() {
    });

    casper.then(function() {
        s.animatedType(this, 'body div:nth-child(2) span', true, 'New Text', true);
    });

    casper.then(function() {
        s.mousePointer(casper, { left: 380, top: 140 });
    });

    casper.wait(2000, function() {
    });
    
    casper.then(function() {
        test.assertEval(function() {
            return $('#pages .node.page .html_element > div.node:eq(1) > div.node:eq(1) > .content:eq(0) .content_').text() === 'New Text';
        });
    });
    

    casper.then(function() {
        
        s.animateHtml(testName, heading, desc);

        test.done();
        this.exit();
        
    });

    casper.run();

});
