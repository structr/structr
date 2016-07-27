/**
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
package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.*;
import org.structr.web.common.StructrUiTest;

/**
 *
 *
 */
public class PathPropertyTest extends StructrUiTest {

	public void test01PathProperty() {
		
		// create a folder and a subfolder
		
		String folder01 = createEntityAsSuperUser("/folder", "{ name: 'folder 01', visibleToPublicUsers: true }");
		String folder02 = createEntityAsSuperUser("/folder", "{ name: 'folder 02', visibleToPublicUsers: true, parent: '" + folder01 + "'}");
		
		grant("Folder", 4095, true);
		
		// find folder by name
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			
			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder01))

			.when()
				.get("/folder?name=folder 01");

		
		// find subfolder by name
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			
			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder02))

			.when()
				.get("/folder?name=folder 02");
		
		// find folder by path
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			
			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder01))

			.when()
				.get("/folder?path=/folder 01");

		// find subfolder by path
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			
			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder02))

			.when()
				.get("/folder?path=/folder 01/folder 02");

		
//		// test update via put
//		RestAssured
//		    
//			.given()
//				.contentType("application/json; charset=UTF-8")
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
//				.body(" { testSixIds: [" + test03 + "," + test04 + "] } ")
//			
//			.expect()
//				.statusCode(200)
//
//			.when()
//				.put(concat("/test_sevens/", test09));
//
//		RestAssured
//		    
//			.given()
//				.contentType("application/json; charset=UTF-8")
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
//			
//			.expect()
//				.statusCode(200)
//
//				.body("result.testSixIds", containsInAnyOrder(test03, test04))
//
//			.when()
//				.get(concat("/test_sevens/", test09));
	}
	
}
