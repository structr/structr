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
    login = require('../templates/login');

var testName = '009_create_and_edit_file';
var heading = "Create and Edit File", sections = [];
var desc = "This animation shows how to create and edit a new file."
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

    casper.start(s.url);

    login.init(test, 'admin', 'admin');

    sections.push('Click on the "Files" menu entry.');

    casper.then(function() {
        s.moveMousePointerTo(casper, '#filesystem_');
    });

    casper.then(function() {
        this.click('#filesystem_');
    });

    casper.wait(1000);

    sections.push('Click the "Add File" icon.');

    casper.then(function() {
        s.moveMousePointerTo(casper, '.add_file_icon');
    });

    casper.then(function() {
        this.click('.add_file_icon');
    });

    casper.wait(2000);

    sections.push('A new file with a random name has been created in the files area. You can also drag and drop a file here from your desktop or from an OS folder to upload it, using the HTML5 Drag & Drop API.');

    casper.then(function() {
        test.assertEval(function() {
            return $('#files-table .node.file').size() === 1;
        });
    });

    casper.then(function() {
        s.moveMousePointerTo(casper, '#files-table .file');
        //this.mouseEvent('mouseover', '#files-table .file');
    });

    casper.then(function() {
        s.moveMousePointerTo(casper, '#files-table .file .edit_file_icon');
    });

    casper.then(function() {
        this.click('#files-table .file .edit_file_icon');
    });

    casper.wait(1000);

    casper.then(function() {
        this.click('.CodeMirror-code div:first-child');
    });

    casper.wait(1000);

    casper.then(function() {
        s.animatedType(this, '.CodeMirror-code div:first-child', false, 'Random text', true);
        //this.sendKeys('.CodeMirror-code div:first-child', 'Random text');
    });

    casper.wait(1000);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#saveAndClose');
    });

    casper.wait(1000);

    casper.then(function() {
        this.click('#saveAndClose');
    });

    casper.wait(1000);

    casper.then(function() {
        s.moveMousePointerTo(casper, '#files-table .file .edit_file_icon');
    });

    casper.then(function() {
        this.click('#files-table .file .edit_file_icon');
    });

    casper.wait(1000);

    casper.then(function() {
        test.assertEval(function() {
            return $('.CodeMirror-code span').text() === 'Random text';
        });
    });

    casper.then(function() {
        s.animateHtml(testName, heading, sections);
        this.exit();
    });

    casper.run();

});
