/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.web.rest;

import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

public class CORSMixedTest extends StructrUiTest {

    private static final Logger logger = LoggerFactory.getLogger(CORSMixedTest.class.getName());

    @BeforeClass(alwaysRun = true)
    @Override
    public void setup() {

        super.setup();

        Settings.AccessControlAcceptedOrigins.setValue("*, " + RestAssured.baseURI);
        Settings.AccessControlMaxAge.setValue(1200);
        Settings.AccessControlAllowMethods.setValue("GET");
        Settings.AccessControlAllowHeaders.setValue("Authorization");
        Settings.AccessControlAllowCredentials.setValue("true");
        Settings.AccessControlExposeHeaders.setValue("Content-Type");
    }

    @Test
    public void test01NonCORSRequestWithDefaults() {

        RestAssured
            .given()
                .contentType("application/json; charset=UTF-8")

            .expect()

                .statusCode(200)

                // No Access-Control headers must exist because it's no CORS request
                .header("Access-Control-Allow-Origin",         (String) null)
                .header("Access-Control-Max-Age",              (String) null)
                .header("Access-Control-Allow-Methods",        (String) null)
                .header("Access-Control-Allow-Headers",        (String) null)
                .header("Access-Control-Allow-Credentials",    (String) null)
                .header("Access-Control-Expose-Headers",       (String) null)

            .when()
                .options("/_env");

    }

    @Test
    public void test02SimpleCORSRequestWithDefaults() {

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)

            .expect()

                .statusCode(200)

                // No Access-Control headers must exist because no "Origin" request header exists
                .header("Access-Control-Allow-Origin",         RestAssured.baseURI)
                .header("Access-Control-Max-Age",              "1200")
                .header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "Content-Type")

            .when()
                .options("/_env");

    }

    @Test
    public void test03NonCORSRequestWithSettingsObjects() {

        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"acceptedOrigins\": \"*\","
                    + "\"maxAge\": 3600,"
                    + "\"allowMethods\": \"GET, POST\","
                    + "\"allowHeaders\": \"Content-Type\","
                    + "\"allowCredentials\": \"true\","
                    + "\"exposeHeaders\": \"*, Authorization\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")

            .expect()

                .statusCode(200)

                // No Access-Control headers must exist because it's no CORS request
                .header("Access-Control-Allow-Origin",         (String) null)
                .header("Access-Control-Max-Age",              (String) null)
                .header("Access-Control-Allow-Methods",        (String) null)
                .header("Access-Control-Allow-Headers",        (String) null)
                .header("Access-Control-Allow-Credentials",    (String) null)
                .header("Access-Control-Expose-Headers",       (String) null)

            .when()
                .options("/_env");

    }

    @Test
    public void test04SimpleCORSRequestWithSettingsObjects() {

        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"acceptedOrigins\": \"*\","
                    + "\"maxAge\": 3600,"
                    + "\"allowMethods\": \"GET, POST\","
                    + "\"allowHeaders\": \"Content-Type\","
                    + "\"allowCredentials\": \"true\","
                    + "\"exposeHeaders\": \"*, Authorization\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                .header("Access-Control-Allow-Origin",         RestAssured.baseURI)
                .header("Access-Control-Max-Age",              "3600")
                // Allow-Methods and Allow-Headers are only returned for preflight requests
//                .header("Access-Control-Allow-Methods",        "GET, POST")
//                .header("Access-Control-Allow-Headers",        "Content-Type")
                .header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "*, Authorization")

            .when()
                .options("/_env");

    }

    @Test
    public void test05SimpleAnonCORSRequestWithSettingsObjects() {

        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"acceptedOrigins\": \"*\","
                    + "\"maxAge\": 3600,"
                    + "\"allowMethods\": \"GET, POST\","
                    + "\"allowHeaders\": \"Content-Type\","
                    + "\"allowCredentials\": \"false\","
                    + "\"exposeHeaders\": \"*, Authorization\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                // "Origin": "*" (wildcard) is allowed because it's an anonymous request and "allowCredentials" is false
                .header("Access-Control-Allow-Origin",         "*")
                .header("Access-Control-Max-Age",              "3600")
                // Allow-Methods and Allow-Headers are only returned for preflight requests
//                .header("Access-Control-Allow-Methods",        "GET, POST")
//                .header("Access-Control-Allow-Headers",        "Content-Type")
                //.header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "*, Authorization")

            .when()
                .options("/_env");
    }


    @Test
    public void test06PreflightCORSRequestWithSettingsObjects() {

        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"acceptedOrigins\": \"*\","
                    + "\"maxAge\": 3600,"
                    + "\"allowMethods\": \"GET, POST\","
                    + "\"allowHeaders\": \"Content-Type\","
                    + "\"allowCredentials\": \"true\","
                    + "\"exposeHeaders\": \"*, Authorization\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "*")

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                .header("Access-Control-Allow-Origin",         RestAssured.baseURI)
                .header("Access-Control-Max-Age",              "3600")
                .header("Access-Control-Allow-Methods",        "GET, POST")
                .header("Access-Control-Allow-Headers",        "Content-Type")
                .header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "*, Authorization")

            .when()
                .options("/_env");

    }

    @Test
    public void test07PreflightAnonCORSRequestWithSettingsObjects() {

        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"acceptedOrigins\": \"*\","
                    + "\"maxAge\": 3600,"
                    + "\"allowMethods\": \"GET, POST\","
                    + "\"allowHeaders\": \"Content-Type\","
                    + "\"allowCredentials\": \"false\","
                    + "\"exposeHeaders\": \"*, Authorization\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "*")

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                // "Origin": "*" (wildcard) is allowed because it's an anonymous request and "allowCredentials" is false
                .header("Access-Control-Allow-Origin",         "*")
                .header("Access-Control-Max-Age",              "3600")
                .header("Access-Control-Allow-Methods",        "GET, POST")
                .header("Access-Control-Allow-Headers",        "Content-Type")
                //.header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "*, Authorization")

            .when()
                .options("/_env");
    }


    @Test
    public void test08NonCORSRequestWithConfig() {

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")

            .expect()

                .statusCode(200)

                // No Access-Control headers must exist because it's no CORS request
                .header("Access-Control-Allow-Origin",         (String) null)
                .header("Access-Control-Max-Age",              (String) null)
                .header("Access-Control-Allow-Methods",        (String) null)
                .header("Access-Control-Allow-Headers",        (String) null)
                .header("Access-Control-Allow-Credentials",    (String) null)
                .header("Access-Control-Expose-Headers",       (String) null)

            .when()
                .options("/_env");

    }

    @Test
    public void test09SimpleCORSRequestWithConfig() {

        /*
        Settings.AccessControlAcceptedOrigins.setValue("*", RestAssured.baseURI);
        Settings.AccessControlMaxAge.setValue(1200);
        Settings.AccessControlAllowMethods.setValue("GET");
        Settings.AccessControlAllowHeaders.setValue("Authorization");
        Settings.AccessControlAllowCredentials.setValue("true");
        Settings.AccessControlExposeHeaders.setValue("Content-Type");
         */

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                .header("Access-Control-Allow-Origin",         RestAssured.baseURI)
                .header("Access-Control-Max-Age",              "1200")
                // Allow-Methods and Allow-Headers are only returned for preflight requests
//                .header("Access-Control-Allow-Methods",        "GET, POST")
//                .header("Access-Control-Allow-Headers",        "Content-Type")
                .header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "Content-Type")

            .when()
                .options("/_env");

    }

    @Test
    public void test10SimpleAnonCORSRequestWithConfig() {

        /*
        Settings.AccessControlAcceptedOrigins.setValue("*", RestAssured.baseURI);
        Settings.AccessControlMaxAge.setValue(1200);
        Settings.AccessControlAllowMethods.setValue("GET");
        Settings.AccessControlAllowHeaders.setValue("Authorization");
        Settings.AccessControlAllowCredentials.setValue("true");
        Settings.AccessControlExposeHeaders.setValue("Content-Type");
         */
        
        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"allowCredentials\": \"false\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }
        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                // "Origin": "*" (wildcard) is allowed because it's an anonymous request and "allowCredentials" is false
                .header("Access-Control-Allow-Origin",         "*")
                .header("Access-Control-Max-Age",              "1200")
                // Allow-Methods and Allow-Headers are only returned for preflight requests
//                .header("Access-Control-Allow-Methods",        "GET, POST")
//                .header("Access-Control-Allow-Headers",        "Content-Type")
                //.header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "Content-Type")

            .when()
                .options("/_env");
    }


    @Test
    public void test11PreflightCORSRequestWithConfig() {

        /*
        Settings.AccessControlAcceptedOrigins.setValue("*", RestAssured.baseURI);
        Settings.AccessControlMaxAge.setValue(1200);
        Settings.AccessControlAllowMethods.setValue("GET");
        Settings.AccessControlAllowHeaders.setValue("Authorization");
        Settings.AccessControlAllowCredentials.setValue("true");
        Settings.AccessControlExposeHeaders.setValue("Content-Type");
         */

        try {

            final String corsSettingsUuid = createEntityAsSuperUser("/CorsSetting", "{"
                    + "\"requestUri\": \"/structr/rest/_env\","
                    + "\"acceptedOrigins\": \"*\","
                    + "\"allowMethods\": \"GET,POST\","
                    + "\"allowCredentials\": \"true\""
                    + "}");

            assertNotNull(corsSettingsUuid);

        } catch (final Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception.");
        }

        RestAssured

            .given()
                .contentType("application/json; charset=UTF-8")
                .header("Origin", RestAssured.baseURI)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "*")

            .expect()

                .statusCode(200)

                // The response has to contain the configured Access-Control headers
                .header("Access-Control-Allow-Origin",         RestAssured.baseURI)
                .header("Access-Control-Max-Age",              "1200")
                .header("Access-Control-Allow-Methods",        "GET,POST")
                .header("Access-Control-Allow-Headers",        "Authorization")
                .header("Access-Control-Allow-Credentials",    "true")
                .header("Access-Control-Expose-Headers",       "Content-Type")

            .when()
                .options("/_env");

    }

}
