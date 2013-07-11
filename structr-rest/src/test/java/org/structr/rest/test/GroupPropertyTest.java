package org.structr.rest.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.structr.rest.common.StructrRestTest;

/**
 *
 * @author alex
 */


public class GroupPropertyTest extends StructrRestTest{
	
	public void test01GroupProperty(){
		
		/*
			{
			 "name": null,
			 "gP1": {
				"sP": "string",
				"iP": 1337
			 },
			 "gP2": null,
			 "id": "d96113452c1b4034b6b1a81f616313af",
			 "type": "TestGroupPropOne"
			}
		 */
		String test011 = createEntity("/test_group_prop_one","{gP1:{sP:text,iP:1337},gP2:{dblP:13.37,dP:01.01.2013}}");
		
		/*
			{
			 "name": null,
			 "gP1": {
				"sP": "string",
				"iP": 1337,
				"lP": null,
				"dblP": 0.1337,
				"bP": true
			 },
			 "gP2": {
				"eP": null
			 },
			 "id": "43c0c0873b7143bdb245afe8ec523bdf",
			 "type": "TestGroupPropTwo"
			}
		 */
		String test021 = createEntity("/test_group_prop_two", "{gP1:{sP:text,iP:1337,dblP:0.1337,bP:true},gP2:{ep:two}}");
		
		String test031 = createEntity("/test_group_prop_three","{gP:{sP:text,iP:1337,gpNode:",test011,"}}");
		String test032 = createEntity("/test_group_prop_three","{ggP:{igP:{gpNode:",test021,",isP:Alex}}}");
		
		// test011 check
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test011))
				.body("result[0].gP1.sP",equalTo("text"))
				.body("result[0].gP2.dblP",equalTo("13.37"))

			.when()
				.get(concat("/TestGroupPropOne/"+test011));
		
		// test021 check
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test021))
				.body("result[0].gP1.dblP",equalTo("0.1337"))
				.body("result[0].gP1.bP",equalTo("true"))

			.when()
				.get(concat("/TestGroupPropOne/"+test021));
		
		// test031 check
		// Node in groupProperty
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test031))
				.body("result[0].gP.gpNode.id",equalTo(test011))

			.when()
				.get(concat("/TestGroupPropOne/"+test031));
		
		// test032 check
		// Node in GroupProperty in GroupProperty
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test032))
				.body("result[0].ggP.igp.gpNode.id",equalTo(test021))

			.when()
				.get(concat("/TestGroupPropOne/"+test032));
	}
	
	private String concat(String... parts) {

		StringBuilder buf = new StringBuilder();
		
		for (String part : parts) {
			buf.append(part);
		}
		
		return buf.toString();
	}
	
	private String createEntity(String resource, String... body) {
		
		StringBuilder buf = new StringBuilder();
		
		for (String part : body) {
			buf.append(part);
		}
		
		return getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(buf.toString())
			.expect().statusCode(201).when().post(resource).getHeader("Location"));
	}
}
