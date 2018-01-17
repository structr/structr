/*
 * Copyright (C) 2010-2018 Structr GmbH
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
;exports.init = function(test, name) {

	// TODO: make sure current active page is pages-tab
	//test.assertEval(function() { return ($('#pages').hasClass('active')); });
	sections.push('A new page has been created. The page is automatically loaded into the preview window.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#pages_", wait: 0});
	});

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#add_page", wait: 0});
	});

	casper.waitForSelector('#previewTabs li.page.active', function() {
		test.assertSelectorHasText('#previewTabs li.page.active', 'New Page');
	});

	if (name) {

		casper.then(function() {
			s.moveMousePointerAndClick(casper, {selector: "#previewTabs li.page.active", wait: 0});
		});

		casper.waitForSelector('#previewTabs li input.new-name', function() {
			s.animatedType(this, '#previewTabs li input.new-name', false, name, true);
		});

		casper.wait(1000);
	}
};