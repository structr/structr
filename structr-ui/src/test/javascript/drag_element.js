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

var testName = '007_add_element_to_page';
var heading = "Add Element to Page", sections = [];
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
        s.mousePointer(casper, { left: 600, top: 400 });
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

    casper.wait(100);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#pagesTab');
    });

    casper.then(function() {
        this.click('#pagesTab');
    });

    casper.wait(100);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#paletteTab');
    });

    casper.then(function() {
        this.click('#paletteTab');
    });

    casper.wait(100);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#add_page');
    });

    casper.then(function() {
        this.click('#add_page');
    });

    casper.wait(2000);

    sections.push('To add HTML elements to a page, open the "Pages Tree View" slideout on the left and the "HTML Palette" slideout on the right hand side.');

    casper.then(function() {
        s.dragDropElement(casper, '#add_content', '#pagesTree > .page > .html_element > div.node:eq(1) > div.node:eq(1)', {x: 0, y: -16});
    });

    sections.push('Then drag the desired element and drop it onto the target element in the page tree or in the page preview window.');
    
    casper.wait(2000);

    casper.then(function() {
        test.assertEvalEquals(function() {
            return $('#pagesTree > .page > .html_element > div.node:eq(1) > div.node:eq(1) > .content:eq(1) > .content_').text();
        }, '#text');
    });

    casper.then(function() {

        s.animateHtml(testName, heading, sections);

        test.done();
        this.exit();

    });

    casper.run();

});
