/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.auth;

import io.restassured.RestAssured;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

/**
 * Ticket 1596: POST /log?initialize=true walked &lt;files.path&gt;/s/ and called Files.delete on every
 * entry it found, and isDirectory() follows symlinks - all of it behind nothing but a POST grant on
 * "log", which an application that logs client-side events plausibly hands to anonymous users. The same
 * resource created LogEvent nodes with visibleToPublicUsers = true.
 *
 * <p>The decision on both this ticket and 55ce5dfb ("Status und Schicksal LogResource") was to remove
 * the resource rather than gate it: it is undocumented, unused, and there are better external tools for
 * what it did. This test is what keeps it removed - the grant is deliberately in place, so a 404 means
 * the resource is gone rather than merely unreachable.
 */
public class LogResourceRemovedTest extends StructrUiTest {

	@Test
	public void testLogResourceIsGone() {

		grant("log", UiAuthenticator.NON_AUTH_USER_POST | UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{}")
			.expect()
				.statusCode(404)
			.when()
				.post("/log?initialize=true");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'subjectId': 'x', 'objectId': 'y', 'action': 'z' }")
			.expect()
				.statusCode(404)
			.when()
				.post("/log");
	}
}
