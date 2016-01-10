/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;

/**
 *
 *
 */


public class CaseHelperTest extends TestCase {

	public void testCaseConversion() {

		assertEquals("CheckIns", CaseHelper.toUpperCamelCase("check_ins"));
		assertEquals("check_ins", CaseHelper.toUnderscore("check_ins", true));
		assertEquals("check_ins", CaseHelper.toUnderscore("check_ins", false));

		assertEquals("CheckIns", CaseHelper.toUpperCamelCase("CheckIns"));
		assertEquals("check_ins", CaseHelper.toUnderscore("CheckIns", true));
		assertEquals("check_ins", CaseHelper.toUnderscore("CheckIns", false));

		assertEquals("CheckIn", CaseHelper.toUpperCamelCase("CheckIn"));
		assertEquals("check_ins", CaseHelper.toUnderscore("CheckIn", true));
		assertEquals("check_in", CaseHelper.toUnderscore("CheckIn", false));

		assertEquals("BlogEntry", CaseHelper.toUpperCamelCase("blog_entry"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("blog_entry", true));
		assertEquals("blog_entry", CaseHelper.toUnderscore("blog_entry", false));

		assertEquals("BlogEntry", CaseHelper.toUpperCamelCase("BlogEntry"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("BlogEntry", true));
		assertEquals("blog_entry", CaseHelper.toUnderscore("BlogEntry", false));

		assertEquals("BlogEntries", CaseHelper.toUpperCamelCase("blog_entries"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("blog_entries", true));
		assertEquals("blog_entries", CaseHelper.toUnderscore("blog_entries", false));

		assertEquals("BlogEntries", CaseHelper.toUpperCamelCase("BlogEntries"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("BlogEntries", true));
		assertEquals("blog_entries", CaseHelper.toUnderscore("BlogEntries", false));

		assertEquals("Blogentries", CaseHelper.toUpperCamelCase("blogentries"));
		assertEquals("blogentries", CaseHelper.toUnderscore("blogentries", true));
		assertEquals("blogentries", CaseHelper.toUnderscore("blogentries", false));

		assertEquals("Blogentries", CaseHelper.toUpperCamelCase("Blogentries"));
		assertEquals("blogentries", CaseHelper.toUnderscore("Blogentries", true));
		assertEquals("blogentries", CaseHelper.toUnderscore("Blogentries", false));

		// not working right now..
//		assertEquals("URL", CaseHelper.toUpperCamelCase("URL"));
//		assertEquals("urls", CaseHelper.toUnderscore("URL", true));
//		assertEquals("url", CaseHelper.toUnderscore("URL", false));
//
//		assertEquals("URLs", CaseHelper.toUpperCamelCase("URLs"));
//		assertEquals("urls", CaseHelper.toUnderscore("URLs", true));
//		assertEquals("urls", CaseHelper.toUnderscore("URLs", false));
//
//		assertEquals("TestRELATEDProject", CaseHelper.toUpperCamelCase("TestRELATEDProject"));
//		assertEquals("test_related_projects", CaseHelper.toUnderscore("TestRELATEDProject", true));
//		assertEquals("test_related_project", CaseHelper.toUnderscore("TestRELATEDProject", false));
//
//		assertEquals("TestRELATEDProjects", CaseHelper.toUpperCamelCase("TestRELATEDProjects"));
//		assertEquals("test_related_projects", CaseHelper.toUnderscore("TestRELATEDProjects", true));
//		assertEquals("test_related_projects", CaseHelper.toUnderscore("TestRELATEDProjects", false));
//
//		assertEquals("URLandURI", CaseHelper.toUpperCamelCase("URLandURI"));
//		assertEquals("url_and_uri", CaseHelper.toUnderscore("URLandURI", true));
//		assertEquals("url_and_uri", CaseHelper.toUnderscore("URLandURI", false));

	}
}
