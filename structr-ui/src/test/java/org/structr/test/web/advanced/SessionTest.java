/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.emptyString;
import static org.testng.AssertJUnit.fail;

public class SessionTest extends StructrUiTest {

    @Test
    public void testSessionScriptingAccess() {

        try (final Tx tx = app.tx()) {

            createAdminUser();

            tx.success();

        } catch (FrameworkException fex) {

            fex.printStackTrace();
            fail("Unexpected exception");
        }

        try {

            try (final Tx tx = app.tx()) {
                app.create(StructrTraits.SCHEMA_METHOD,
                        new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "doSessionTest"),
                        new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),                     "{const params = $.methodParameters; $.session[params.key] = params.value; return `${$.session[params.key]}->${$.session.id}`}")
                );

                tx.success();
            }

            try (final Tx tx = app.tx()) {

                RestAssured

                        .given()
                        .contentType("application/json; charset=UTF-8")
                        .filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
                        .filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
                        .filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
                        .filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
                        .filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
                        .filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
                        .headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
                        .body("{'key':'testKey', 'value':'testValue'}")

                        .expect()
                        .statusCode(200)

                        .body("result", Matchers.not(emptyString()))
                        .body("result", Matchers.startsWith("testValue->"))

                        .when()
                        .post("/doSessionTest");

                tx.success();
            }

        } catch (final FrameworkException fex) {

            fex.printStackTrace();
            fail("Unexpected exception.");
        }
    }

}
