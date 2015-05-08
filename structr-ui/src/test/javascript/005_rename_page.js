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

var s = require('../setup'),
    login = require('../templates/login'),
    createPage = require('../templates/createPage');

var testName = '005_rename_page';
var heading = "Rename Page", sections = [];
var desc = "This animation shows how a new page can be renamed."
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);
    
    casper.thenEvaluate(function() {
        window.localStorage.clear();
    }, {});
    
    sections.push('<a href="004_create_page_test.html">Login and create a page.</a>');

    login.init(test, 'admin', 'admin');
    
    createPage.init(test, 'test-page');

    sections.push('You can rename a page by simply clicking on the name on the preview tab. After entering a new name, press return or tab, or click outside the input field.');

    casper.then(function() {
        s.moveMousePointerTo(casper, '#previewTabs li:nth-child(3)');
    });

    casper.then(function() {
        this.click('#previewTabs li:nth-child(3)');
    });

    casper.wait(1000);

    casper.then(function() {
        s.animatedType(this, '#previewTabs li:nth-child(3) .new-name', false, 'renamed-page', true);
    });

    casper.wait(1000);

    casper.then(function() {
        test.assertEval(function() {
            return $('#previewTabs li:nth-child(3) .name_').text() === 'renamed-page';
        });
    });

    casper.wait(1000);

    casper.then(function() {
        s.animateHtml(testName, heading, sections);
        this.exit();
    });

    casper.run();

});
