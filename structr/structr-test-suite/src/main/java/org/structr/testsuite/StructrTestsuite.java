package org.structr.testsuite;

import com.jayway.restassured.RestAssured;
import static org.hamcrest.Matchers.*;

/**
 * Structr REST test suite.
 * 
 * @author Christian Morgner
 */
public class StructrTestsuite extends com.jayway.restassured.RestAssured {

	public static void main(String[] args) {

		StructrTestsuite suite = new StructrTestsuite();
		suite.test01();
	}

	public void test01() {

		RestAssured.baseURI = "http://server0.morgner.de";
		RestAssured.port = 8888;
		RestAssured.basePath = "/splink_sgdb/api";

		String res = "/version";

		try {

			System.out.println("Testing " + res + "...");

			expect().body(containsString("0.2.8")).when().get(res);

		} catch(Throwable t) {

			t.printStackTrace();
		}

	}
}

