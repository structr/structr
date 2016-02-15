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

var testName = '011_html_entity_encoding';
var heading = "HTML entity encoding", sections = [];
var desc = "This animation shows that html entities stay intact when saving a file.";
var numberOfTests = 3;
var entityTestString = 'javascript:escape("&quot;");';

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

	casper.start(s.url);

	login.init(test, 'admin', 'admin');

	sections.push('Access the filesystem tab');

	casper.then(function() {
		s.moveMousePointerTo(casper, '#filesystem_');
	});
	casper.then(function() {
		this.click('#filesystem_');
	});
	casper.wait(1000);



	sections.push('Create a new file');

	casper.then(function() {
		s.moveMousePointerTo(casper, '#folder-contents-container button.add_file_icon');
	});
	casper.then(function() {
		this.click('#folder-contents-container button.add_file_icon');
	});
	casper.wait(1000);


	sections.push('Edit that file');

	casper.then(function() {
		s.moveMousePointerTo(casper, '#files-table-body tr:last-child img.edit_file_icon');
	});
	casper.then(function() {
		this.click('#files-table-body tr:last-child img.edit_file_icon');
	});
	casper.wait(1000);



	sections.push('Enter the test string');

	casper.then(function() {
		s.moveMousePointerTo(casper, '.CodeMirror-code');
	});
	casper.then(function() {
		this.click('.CodeMirror-code pre');
	});
	casper.wait(1000);

    casper.then(function() {
        s.animatedType(this, '.CodeMirror-code div:first-child', false, entityTestString, true);
    });

	casper.wait(1000);


	sections.push('Save the file');

	casper.then(function() {
		s.moveMousePointerTo(casper, '#dialogBox #saveAndClose');
	});
	casper.then(function() {
		this.click('#dialogBox #saveAndClose');
	});
	casper.wait(1000);



	sections.push('Re-open the editor');

	casper.then(function() {
		s.moveMousePointerTo(casper, '#files-table-body tr:last-child img.edit_file_icon');
	});
	casper.then(function() {
		this.click('#files-table-body tr:last-child img.edit_file_icon');
	});

	casper.wait(1000);


//	casper.then(function() {
//        test.assertEvalEquals(function() {
//            return $('.CodeMirror-code pre').text();
//        }, entityTestString);
//    });
//

	casper.then(function() {
        test.assertEval(function (r) {
			return ($('.CodeMirror-code pre').text() === r);
		}, '', entityTestString);
    });

	casper.wait(1000);


    casper.then(function() {
        s.animateHtml(testName, heading, sections);
        this.exit();
    });

	casper.run();

});
