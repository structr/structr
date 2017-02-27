/*
 * Copyright (C) 2010-2017 Structr GmbH
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
	login = require('../templates/login'),
	createPage = require('../templates/createPage'),
	openPagesTreeView = require('../templates/openPagesTreeView');

var testName = '010_page_visibility';
var heading = "Page Visibility", sections = [];
var desc = "This animation shows how to change the visibility for a page.";
var numberOfTests = 4;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

	casper.start(s.url);

	login.init(test, 'admin', 'admin');

	createPage.init(test, 'visible');

	openPagesTreeView.init(test);

	sections.push('Open the Access Control tab.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#pagesTree .page .key_icon", wait: 2000});
	});

	sections.push('Apply visibility switches recursively.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#recursive", wait: 2000});
	});

	sections.push('Change the visibility for public/auth users.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#dialogBox .visibleToPublicUsers_", wait: 0});
	});

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#dialogBox .visibleToAuthenticatedUsers_", wait: 2000});
	});

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#dialogBox .dialogBtn .closeButton", wait: 2000});
	});

	sections.push ('Logout and open the created page.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#logout_", wait: 2000});
	});

	casper.thenOpen(s.baseUrl + 'visible');

	casper.waitForSelector('body h1', function() {
		test.assertSelectorHasText('body h1', 'Visible');
	});

	casper.wait(2000);

	casper.then(function() {
		s.animateHtml(testName, heading, sections);
	});

	casper.run();

});