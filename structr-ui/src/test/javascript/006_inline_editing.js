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

var testName = '006_inline_editing';
var heading = "Inline Editing", sections = [];
var desc = "This animation shows how to edit text directly in the rendered page."
var numberOfTests = 5;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);

    casper.thenEvaluate(function() {
        window.localStorage.clear();
    }, {});

    sections.push('<a href="004_create_page_test.html">Login and create a page.</a>');

    casper.then(function() {
        s.animatedType(this, '#usernameField', false, 'admin');
    });

    casper.then(function() {
        s.animatedType(this, '#passwordField', false, 'admin');
    });

    casper.then(function() {
        s.mousePointer(casper, {left: 600, top: 400});
        s.moveMousePointerTo(casper, '#loginButton');
    });

    casper.then(function() {
        this.click('#loginButton');
    });

    casper.waitForSelector('#errorText', function() {

        test.assertEval(function() {
            return !($('#errorText').text() === 'Wrong username or password!');
        });

        test.assertEval(function() {
            return $('#pages').is(':visible');
        });

    });

    casper.wait(2000);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#add_page');
    });

    casper.then(function() {
        this.click('#add_page');
    });

    casper.wait(5000, function() {
        test.assertEval(function() {
            return $('#errorText').text() === '';
        });
    });

    casper.wait(2000);


    sections.push('To edit a text section, click on it in the preview window and directly edit the text in the page. You can add new and even empty lines by simply pressing return. When finished, hit tab, or click outside the text section.');

    casper.then(function() {
        s.moveMousePointer(casper, {left: 73, top: 78}, {left: 45, top: 185});
        //s.moveMousePointerTo(this, 'body div:nth-child(2) span');
    });


    casper.then(function() {
        test.assertExists('iframe');
    });

    casper.then(function() {
        s.clickInIframe(this, 'body div:nth-child(2) span');
    });

    casper.wait(2000);

    var text = 'Some more text';
    var result = text + 'Initial body text';

    casper.then(function() {
        s.animatedType(this, 'body div:nth-child(2) span', true, text, true);
    });

    casper.then(function() {
        s.mousePointer(casper, {left: 380, top: 140});
    });

    casper.then(function() {
        this.click('iframe');
    });

    casper.wait(2000);

    casper.then(function() {
        test.assertEval(function(r) {
            return ($('#pages .node.page .html_element > div.node:eq(1) > div.node:eq(1) > .content:eq(0) .content_').text() === r);
        }, '', result);
    });

    casper.then(function() {

        s.animateHtml(testName, heading, sections);

        test.done();
        this.exit();

    });

    casper.run();

});
