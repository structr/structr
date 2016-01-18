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
package org.structr.rest.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 *
 *
 */
public class LogFileNodeTest {

	private static final String[] ACTIONS = { "READ", "WRITE", "VIEW", "DELETE", "TEST" };

	public static void main(final String[] args) {

		final Random random           = new Random(System.currentTimeMillis());
		final List<String> subjectIds = new ArrayList<>();
		final List<String> objectIds  = new ArrayList<>();
		final int num                 = 10;

		for (int i=0; i<num; i++) {

			final String id1 = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
			final String id2 = StringUtils.replace(UUID.randomUUID().toString(), "-", "");

			subjectIds.add(id1);
			objectIds.add(id2);

			System.out.println(id1);
			System.out.println(id2);
		}

		for (int i=0; i<num; i++) {

			for (int j=i+1; j<num; j++) {

				final String id1    = subjectIds.get(i);
				final String id2    = objectIds.get(j);


				for (int k=0; k<100; k++) {

					final String action = ACTIONS[random.nextInt(5)];

					RestAssured
						.given()
						.contentType("application/json; charset=UTF-8")
						.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
						.header("X-User", "admin")
						.header("X-Password", "admin")
						.body("{ 'subject': '" + id1 + "', 'object': '" + id2 + "', 'action': '" + action + "', 'message': '" + id1 + id2 + "' }")
						.expect()
						.statusCode(200)
						.when()
						.post("http://localhost:8082/structr/rest/log");
				}
			}
		}
	}
}
