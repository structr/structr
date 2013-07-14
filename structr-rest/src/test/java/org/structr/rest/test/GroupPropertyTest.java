package org.structr.rest.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.*;
import org.structr.rest.common.StructrRestTest;

/**
 *
 * @author alex
 */


public class GroupPropertyTest extends StructrRestTest{
	
	public void test01GroupProperty(){
		

		String test011 = createEntity("/test_group_prop_one","{gP1:{sP:text,iP:1337},gP2:{dblP:13.37,dP:01.01.2013}}");
		
		String test021 = createEntity("/test_group_prop_two", "{gP1:{sP:text,iP:1337,dblP:0.1337,bP:true},gP2:{ep:two}}");
		
		String test031 = createEntity("/test_group_prop_three","{gP:{sP:text,iP:1337,gpNode:",test011,"}}");
		String test032 = createEntity("/test_group_prop_three","{ggP:{igP:{gpNode:",test021,",isP:Alex}}}");
		
		// test011 check
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test011))
				.body("result.gP1.sP",equalTo("text"))
				.body("result.gP2.dblP", equalTo(13.37f))

			.when()
				.get("/test_group_prop_one/"+test011);
		
		// test021 check
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test021))
				.body("result.gP1.dblP",equalTo(0.1337f))
				.body("result.gP1.bP",equalTo(true))

			.when()
				.get("/test_group_prop_two/"+test021);
		
		// test031 check
		// Node in groupProperty
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test031))
				.body("result.gP.gpNode.id",equalTo(test011))

			.when()
				.get("/test_group_prop_three/"+test031);
		
		// test032 check
		// Node in GroupProperty in GroupProperty
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			
			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test032))
				.body("result.ggP.igP.gpNode.id",equalTo(test021))

			.when()
				.get("/test_group_prop_three/"+test032);
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
