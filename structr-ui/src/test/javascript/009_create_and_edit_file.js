/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
var s = require('../setup'),
	login = require('../templates/login');

var testName = '009_create_and_edit_file';
var heading = "Create and Edit File", sections = [];
var desc = "This animation shows how to create and edit a new file.";
var numberOfTests = 4;
var testString = 'Random text';

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

	casper.start(s.url);

	login.init(test, 'admin', 'admin');

	sections.push('Click on the "Files" menu entry.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#files_", wait: 1000});
	});

	sections.push('Click the "Add File" icon.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: ".add_file_icon", wait: 1000});
	});

	sections.push('A new file with a random name has been created in the files area. You can also drag and drop a file here from your desktop or from an OS folder to upload it, using the HTML5 Drag & Drop API.');

	casper.then(function() {
		test.assertElementCount('#files-table .node.file', 1);
	});

	casper.then(function() {
		s.moveMousePointerTo(casper, '#files-table-body tr:last-child .node');
	});

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#files-table .file .edit_file_icon", wait: 1000});
	});

	sections.push('Enter the test string');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: '.CodeMirror-code', wait: 1000});
	});

	casper.then(function() {
		s.animatedType(this, '.CodeMirror-code div:first-child', false, testString, true);
	});

	casper.wait(1000);

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#saveAndClose", wait: 1000});
	});

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#files-table .file .edit_file_icon", wait: 1000});
	});

	casper.then(function() {
		test.assertSelectorHasText('.CodeMirror-code pre', testString);
	});

	casper.then(function() {
		s.animateHtml(testName, heading, sections);
	});

	casper.run();

});