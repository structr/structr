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
