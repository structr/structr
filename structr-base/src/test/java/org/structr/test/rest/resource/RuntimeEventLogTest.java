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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;



/**
 */
public class RuntimeEventLogTest extends StructrRestTestBase {

	@Test
	public void testRuntimeEventLogWithSorting() {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{}")
			.expect()
				.statusCode(200)
			.when()
				.get("/_runtimeEventLog?_order=absoluteTimestamp&_sort=description&_pageSize=100");

	}
}
