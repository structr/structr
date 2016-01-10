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
    createPage = require('../templates/createPage'),
    openPagesTreeView = require('../templates/openPagesTreeView');

var testName = '007_add_element_to_page';
var heading = "Add Element to Page", sections = [];
var desc = "This animation shows how a content element is dragged into a page."
var numberOfTests = 4;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);

    login.init(test, 'admin', 'admin');
    
    createPage.init(test, 'test-page');

    openPagesTreeView.init(test);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#paletteTab');
    });

    casper.then(function() {
        this.click('#paletteTab');
    });

    sections.push('To add HTML elements to a page, open the "Pages Tree View" slideout on the left and the "HTML Palette" slideout on the right hand side.');

    casper.then(function() {
        s.dragDropElement(casper, '#add_content', '#pagesTree > .page > .html_element > div.node:eq(1) > div.node:eq(1)', {x: 0, y: -16});
    });

    sections.push('Then drag the desired element and drop it onto the target element in the page tree or in the page preview window.');
    
    casper.wait(5000, function() {
        test.assertEvalEquals(function() {
            return $('#pagesTree > .page > .html_element > div.node:eq(1) > div.node:eq(1) > .content:eq(1) > .content_').text();
        }, '#text');
    });

    casper.then(function() {
        s.animateHtml(testName, heading, sections);
        this.exit();
    });

    casper.run();

});
