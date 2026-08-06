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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.SchemaHelper;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

public class ZonedDateTimePropertyTest extends StructrRestTestBase {

    @Test
    public void testBasicRoundTrip() {

        RestAssured.given()
                .contentType("application/json; charset=UTF-8")
                .body(" { 'zonedDateTime' : '2013-04-05T10:43:40+02:00[Europe/Berlin]' } ")
                .expect()
                .statusCode(201)
                .when()
                .post("/TestThree")
                .getHeader("Location");

        RestAssured.given()
                .contentType("application/json; charset=UTF-8")
                .expect()
                .statusCode(200)
                .body("result[0].zonedDateTime", equalTo("2013-04-05T10:43:40+02:00[Europe/Berlin]"))
                .when()
                .get("/TestThree");
    }

	@Test
	public void testBasicRoundTripWithSettingOverride() {

		Settings.ZonedDateTimeFormatOverride.setValue("yyyy-MM-dd HH '['VV']'");

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'zonedDateTime' : '2013-04-05 17 [Europe/Berlin]' } ")
				.expect()
				.statusCode(201)
				.when()
				.post("/TestThree")
				.getHeader("Location");

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.expect()
				.statusCode(200)
				.body("result[0].zonedDateTime", equalTo("2013-04-05 17 [Europe/Berlin]"))
				.when()
				.get("/TestThree");

		Settings.ZonedDateTimeFormatOverride.setValue("");
	}

	@Test
	public void testBasicRoundTripWithSettingOverrideAndFallbackParsing() {

		Settings.ZonedDateTimeFormatOverride.setValue("yyyy-MM-dd HH '['VV']'");

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'zonedDateTime' : '2013-04-05T10:43:40+02:00[Europe/Berlin]' } ")		// does not parse against pattern override but parses fine against DateTimeFormatter.ISO_ZONED_DATE_TIME
				.expect()
				.statusCode(201)
				.when()
				.post("/TestThree")
				.getHeader("Location");

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.expect()
				.statusCode(200)
				.body("result[0].zonedDateTime", equalTo("2013-04-05 10 [Europe/Berlin]"))
				.when()
				.get("/TestThree");

		Settings.ZonedDateTimeFormatOverride.setValue("");
	}

	@Test
	public void testRoundTripWithCustomZonedDateTime() {

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'customZonedDateTime' : '2013-04-05 10:43:40 [Europe/Berlin]' } ")
				.expect()
				.statusCode(201)
				.when()
				.post("/TestThree")
				.getHeader("Location");

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.expect()
				.statusCode(200)
				.body("result[0].customZonedDateTime", equalTo("2013-04-05 10:43:40 [Europe/Berlin]"))
				.when()
				.get("/TestThree");

	}

	@Test
	public void testZonedDateTimeFormattingInSchemaMethodResult() {

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'zonedDateTime' : '2013-04-05T10:43:40+02:00[Europe/Berlin]', 'customZonedDateTime' : '2013-04-05 10:43:40 [Europe/Berlin]', 'dateProperty' : '2010-02-04T06:08:10+0000' } ")
				.expect()
				.statusCode(201)
				.when()
				.post("/TestThree")
				.getHeader("Location");

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "zdtTest"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), """
						{
							let testThree = $.find('TestThree')[0];
							let zdt = Temporal.ZonedDateTime.from({
								 timeZone: "Europe/Berlin",
								 year: 2026, month: 7, day: 15,
								 hour: 12, minute: 34, second: 56
							});

							return [
								testThree,
								{
									jsDate: new Date(2025, 4, 6, 12, 13, 14)
								},
								{
									jsZdt: zdt
								}
							];
						}
						""")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result[0].zonedDateTime", equalTo("2013-04-05T10:43:40+02:00[Europe/Berlin]"))
				.body("result[0].customZonedDateTime", equalTo("2013-04-05 10:43:40 [Europe/Berlin]"))
				.body("result[0].dateProperty", equalTo("2010-02-04T06:08:10+0000"))
				.body("result[1].jsDate", equalTo("2025-05-06T12:13:14+0000"))
				.body("result[2].jsZdt", equalTo("2026-07-15T12:34:56+02:00[Europe/Berlin]"))
			.when()
			 .post("/zdtTest");
	}

	@Test
	public void testZonedDateTimeFormattingInSchemaMethodResultWithSettingOverride() {

		Settings.ZonedDateTimeFormatOverride.setValue("yyyy-MM-dd HH '['VV']'");
		Settings.DefaultDateFormat.setValue("yyyy MM dd HH mm ssZ");

		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'zonedDateTime' : '2013-04-05T10:43:40+02:00[Europe/Berlin]', 'customZonedDateTime' : '2013-04-05 10:43:40 [Europe/Berlin]', 'dateProperty' : '2010-02-04T06:08:10+0000' } ")
				.expect()
				.statusCode(201)
				.when()
				.post("/TestThree")
				.getHeader("Location");

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "zdtTest"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), """
						{
							let testThree = $.find('TestThree')[0];
							let zdt = Temporal.ZonedDateTime.from({
								 timeZone: "Europe/Berlin",
								 year: 2026, month: 7, day: 15,
								 hour: 12, minute: 34, second: 56
							});

							return [
								testThree,
								{
									jsDate: new Date(2025, 4, 6, 12, 13, 14)
								},
								{
									jsZdt: zdt
								}
							];
						}
						""")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.expect()
				.statusCode(200)
				.body("result[0].zonedDateTime", equalTo("2013-04-05 10 [Europe/Berlin]"))
				.body("result[0].customZonedDateTime", equalTo("2013-04-05 10:43:40 [Europe/Berlin]"))
				.body("result[0].dateProperty", equalTo("2010 02 04 06 08 10+0000"))
				.body("result[1].jsDate", equalTo("2025 05 06 12 13 14+0000"))
				.body("result[2].jsZdt", equalTo("2026-07-15 12 [Europe/Berlin]"))
				.when()
				.post("/zdtTest");

		Settings.ZonedDateTimeFormatOverride.setValue("");
		Settings.DefaultDateFormat.setValue(Settings.DefaultDateFormat.getDefaultValue());
	}
}
