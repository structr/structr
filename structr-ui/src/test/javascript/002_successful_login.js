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
var s = require('../setup');

var testName = '002_successful_login';
var heading = "Successful Login", sections = [];
var desc = "This animation shows a successful login.";
var numberOfTests = 2;

casper.test.begin(testName, numberOfTests, function(test) {

	s.startRecording(window, casper, testName);

	casper.start(s.url);

	sections.push('If you enter a valid combination of username and password, the system allows you to log in.');

	casper.waitForSelector('#usernameField').then(function() {
		casper.wait(500);
		s.animatedType(this, '#usernameField', false, 'admin');
	});

	casper.waitForSelector('#passwordField').then(function() {
		s.animatedType(this, '#passwordField', false, 'admin');
	});

	casper.then(function() {
		s.mousePointer(casper, { left: 600, top: 400 });
	});

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#loginButton", wait: 1000});
	});

	casper.waitForSelector('#about-me', function() {
		test.assertSelectorDoesntHaveText('#errorText', 'Wrong username or password!');

		test.assertEval(function() {
			return $('#dashboard').is(':visible');
		});

//		// this strangely does not work even though the element is visible. bug in casperjs?
//		test.assertVisible('#dashboard');
	});

	casper.then(function() {
		s.animateHtml(testName, heading, sections);
	});

	casper.run();

});
