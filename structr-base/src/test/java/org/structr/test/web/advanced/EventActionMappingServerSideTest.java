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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ParameterMappingTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Server side of the Event Action Mapping: how the event endpoint runs an action with the parameters the browser
 * sends, which attributes the page renders for frontend.js, and how the migration treats stored action names.
 *
 * Extends DeploymentTestBase for the migration tests, which drive the migration through a deployment import, the
 * only place it runs under test: Services skips the startup migration while testing.
 */
public class EventActionMappingServerSideTest extends DeploymentTestBase {

	// ----- create with a type chosen at runtime -----

	@Test
	public void testCreateWithAbstractNodeCreatesTheTypeFromTheTypeParameter() {

		final String buttonUuid = setupCreateButton();

		final String createdType = postEvent(buttonUuid, Map.of("type", "Item", "name", "Dynamic item"), 200)
			.jsonPath().getString("result.type");

		assertEquals("AbstractNode as data type means that the type parameter decides the type", "Item", createdType);

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> items = app.nodeQuery("Item").name("Dynamic item").getAsList();

			assertEquals("Expected exactly one Item created by the action", 1, items.size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testCreateWithAbstractNodeRejectsAMissingOrUnusableType() {

		final String buttonUuid = setupCreateButton();

		// no type parameter at all, a blank one, a type that does not exist, and a relationship type
		postEvent(buttonUuid, Map.of("name", "Missing type"), 422);
		postEvent(buttonUuid, Map.of("type", "", "name", "Blank type"), 422);
		postEvent(buttonUuid, Map.of("type", "NoSuchType", "name", "Unknown type"), 422);
		postEvent(buttonUuid, Map.of("type", StructrTraits.PRINCIPAL_OWNS_NODE, "name", "Relationship type"), 422);

		// a relationship type configured directly as data type is rejected the same way
		String relationshipButtonUuid = null;

		try (final Tx tx = app.tx()) {

			final Page page      = app.nodeQuery(StructrTraits.PAGE).name("page1").getFirst().as(Page.class);
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = createElement(page, div, "button", "create-relationship");

			relationshipButtonUuid = btn.getUuid();

			createMapping(btn, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,    "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY, StructrTraits.PRINCIPAL_OWNS_NODE
			));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		postEvent(relationshipButtonUuid, Map.of("name", "Relationship data type"), 422);

		try (final Tx tx = app.tx()) {

			assertTrue("A rejected create action must not create anything", app.nodeQuery("Item").getAsList().isEmpty());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- static methods -----

	@Test
	public void testStaticMethodReceivesOnlyTheMappedParameters() {

		String buttonUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Item");

			type.addMethod("summarize", "{ return $.arguments; }").setIsStatic(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = createElement(page, div, "button", "call-static");

			buttonUuid = btn.getUuid();

			createMapping(btn, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,        "method",
				ActionMappingTraitDefinition.METHOD_PROPERTY,        "summarize",
				ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY, "Item"
			));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// the payload frontend.js sends: the internal keys of the trigger (structrId, structrAction, structrMethod,
		// structrIdExpression, structrTarget, ...) plus the one mapped parameter
		final Map<String, Object> arguments = postEvent(buttonUuid, Map.of("note", "from the page"), 200)
			.jsonPath().getMap("result");

		assertEquals("A static method must see only the mapped parameters in $.arguments, like an instance method", Map.of("note", "from the page"), arguments);
	}

	// ----- sign-out -----

	@Test
	public void testSignOutWithANonStringParameterEndsTheSession() {

		String buttonUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = createElement(page, div, "button", "sign-out");

			buttonUuid = btn.getUuid();

			createMapping(btn, Map.of(ActionMappingTraitDefinition.ACTION_PROPERTY, "sign-out"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// what the browser sends for the button; a json(true) constant arrives as a boolean, not as a string
		final Map<String, Object> payload = BrowserEventPayload.of(fetchPageHtml("/html/page1"), buttonUuid);

		payload.put("htmlEvent", "click");
		payload.put("everywhere", true);

		RestAssured.basePath = "/structr/rest";

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		final String sessionId = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ \"name\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\" }")
			.expect()
				.statusCode(200)
			.when()
				.post("/login")
			.cookie("JSESSIONID");

		assertNotNull("The login did not create a session", sessionId);

		// precondition: the session is signed in
		RestAssured
			.given()
				.cookie("JSESSIONID", sessionId)
			.expect()
				.statusCode(200)
				.body("result.name", equalTo(ADMIN_USERNAME))
			.when()
				.get("/me");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.cookie("JSESSIONID", sessionId)
				.body(BrowserEventPayload.toJson(payload))
			.expect()
				.statusCode(200)
			.when()
				.post("/DOMElement/" + buttonUuid + "/event");

		RestAssured
			.given()
				.cookie("JSESSIONID", sessionId)
			.expect()
				.statusCode(401)
			.when()
				.get("/me");
	}

	// ----- values the server takes from the action mapping, not from the request -----

	@Test
	public void testMethodNameInTheRequestIsIgnored() {

		final Map<String, String> ids = setupFunctionButtons();

		// the request names the private function, the mapping configures the public one: the configured one runs
		final Map<String, Object> payload = browserPayload(ids.get("configured"));

		payload.put(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD, "privateFunction");

		final String result = postPayload(ids.get("configured"), payload, 200).jsonPath().getString("result");

		assertEquals("The method configured on the action mapping must run, not the one named in the request", "configured", result);
		assertEquals("The function named in the request must not run", 0, countItems("private ran"));
	}

	@Test
	public void testDataTypeInTheRequestIsIgnored() {

		String buttonUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("Item");

			StructrSchema.extendDatabaseSchema(app, schema);

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = createElement(page, div, "button", "create-item");

			buttonUuid = btn.getUuid();

			createMapping(btn, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,    "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY, "Item"
			));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// the request names User as data type, the mapping configures Item: an Item is created
		final Map<String, Object> payload = browserPayload(buttonUuid);

		payload.put(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE, StructrTraits.USER);
		payload.put("name", "Forged type");

		final String createdType = postPayload(buttonUuid, payload, 200).jsonPath().getString("result.type");

		assertEquals("The data type configured on the action mapping must be created, not the one named in the request", "Item", createdType);
		assertEquals("Expected the configured type to be created", 1, countItems("Forged type"));

		try (final Tx tx = app.tx()) {

			assertTrue("No User must be created from a data type named in the request", app.nodeQuery(StructrTraits.USER).name("Forged type").getAsList().isEmpty());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMethodNameIsNeverEvaluatedAsAnExpression() {

		final Map<String, String> ids = setupFunctionButtons();

		// the method is a static name: a template expression in it is not evaluated, so it names no function at all,
		// neither a private nor a public one
		postEvent(ids.get("expressionPrivate"), Map.of(), 422);

		assertEquals("A method name must not be evaluated as an expression", 0, countItems("private ran"));

		postEvent(ids.get("expressionPublic"), Map.of(), 422);
	}

	@Test
	public void testTargetInTheRequestCanBeAnotherObjectTheUserMayWrite() {

		final Map<String, String> ids = setupUpdateButtons();

		// the target is evaluated when the page renders (${current.id}, a repeater row), so it comes from the request;
		// the permissions of the object are the boundary, and the admin may write Item B
		final Map<String, Object> payload = browserPayload(ids.get("buttonA"));

		payload.put(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION, ids.get("itemB"));
		payload.put("name", "Redirected");

		postPayload(ids.get("buttonA"), payload, 200);

		assertItemName(ids.get("itemA"), "Item A");
		assertItemName(ids.get("itemB"), "Redirected");
	}

	@Test
	public void testTypeChosenAtRuntimeNeedsACreateGrant() {

		final String buttonUuid = setupCreateButton();

		try (final Tx tx = app.tx()) {

			// the anonymous visitor must see the trigger to call its event method
			app.getNodeById(StructrTraits.DOM_ELEMENT, buttonUuid).setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Map<String, Object> payload = browserPayload(buttonUuid);

		payload.put("type", "Item");
		payload.put("name", "Anonymous item");

		RestAssured.basePath = "/structr/rest";

		// the event itself is allowed for anonymous visitors, creating an Item over REST is not
		grant("DOMElement/_id/event", UiAuthenticator.NON_AUTH_USER_POST, true);

		postAnonymously(buttonUuid, payload, 401);

		assertEquals("Without a create grant for the type, nothing must be created", 0, countItems("Anonymous item"));

		// with the same grant POST /Item needs, the type chosen at runtime is created
		RestAssured.basePath = "/structr/rest";

		grant("Item", UiAuthenticator.NON_AUTH_USER_POST, false);

		postAnonymously(buttonUuid, payload, 200);

		assertEquals("With a create grant for the type, the Item must be created", 1, countItems("Anonymous item"));
	}

	@Test
	public void testAnonymousVisitorCannotTouchAnObjectItCannotSee() {

		final Map<String, String> ids = new LinkedHashMap<>();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("Item").addMethod("describe", "{ return 'Item ' + $.this.name; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Page page       = Page.createSimplePage(securityContext, "page1");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final NodeInterface item = app.create("Item", "Invisible item");
			final PropertyKey<Boolean> visibleToPublicUsers = Traits.of(StructrTraits.DOM_ELEMENT).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);

			for (final String action : List.of("update", "delete", "method")) {

				final DOMElement btn = createElement(page, div, "button", action);

				createMapping(btn, Map.of(
					ActionMappingTraitDefinition.ACTION_PROPERTY,        action,
					ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY, item.getUuid(),
					ActionMappingTraitDefinition.METHOD_PROPERTY,        "describe"
				));

				// the anonymous visitor must see the trigger to call its event method
				btn.setProperty(visibleToPublicUsers, true);

				ids.put(action, btn.getUuid());
			}

			ids.put("item", item.getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/rest";

		grant("DOMElement/_id/event", UiAuthenticator.NON_AUTH_USER_POST, true);

		// the Item is not visible to the visitor, so there is no target: 200 as for a uuid without an object
		for (final String action : List.of("update", "delete", "method")) {

			final Map<String, Object> payload = new LinkedHashMap<>();

			payload.put("htmlEvent", "click");
			payload.put(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION, ids.get("item"));
			payload.put("name", "Changed by visitor");

			postAnonymously(ids.get(action), payload, 200);
		}

		assertItemName(ids.get("item"), "Invisible item");
	}

	// ----- rendering -----

	@Test
	public void testElementsThatAreOnlyHiddenByAFollowUpRenderTheirId() {

		String successHideUuid = null;
		String failureHideUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page                = Page.createSimplePage(securityContext, "page1");
			final DOMNode div              = page.getElementsByTagName("div").get(0);
			final DOMElement btn           = createElement(page, div, "button", "button");
			final DOMElement successHide   = createElement(page, div, "div", "success-hide");
			final DOMElement failureHide   = createElement(page, div, "div", "failure-hide");

			createElement(page, div, "div", "unrelated");

			successHideUuid = successHide.getUuid();
			failureHideUuid = failureHide.getUuid();

			final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
			final NodeInterface eam = createMapping(btn, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,            "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY,         "Item",
				ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY, "show-hide-section-linked",
				ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY, "show-hide-section-linked"
			));

			// only hide targets, no element to show and nothing to reload
			eam.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_HIDE_TARGETS_PROPERTY), List.of(successHide));
			eam.setProperty(traits.key(ActionMappingTraitDefinition.FAILURE_HIDE_TARGETS_PROPERTY), List.of(failureHide));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Document doc = Jsoup.parse(fetchPageHtml("/html/page1"));

		assertEquals("The trigger addresses the success hide target by its data-structr-id", "show-hide-section:hide=[data-structr-id='" + successHideUuid + "']",
			doc.getElementById("button").attr("data-structr-success-target"));
		assertEquals("The trigger addresses the failure hide target by its data-structr-id", "show-hide-section:hide=[data-structr-id='" + failureHideUuid + "']",
			doc.getElementById("button").attr("data-structr-failure-target"));

		// without the id, the selector the trigger carries matches nothing in the browser
		assertEquals("An element that is only a success hide target must render data-structr-id", successHideUuid,
			doc.getElementById("success-hide").attr(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY));
		assertEquals("An element that is only a failure hide target must render data-structr-id", failureHideUuid,
			doc.getElementById("failure-hide").attr(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY));

		assertFalse("An element without any action mapping relation renders no data-structr-id",
			doc.getElementById("unrelated").hasAttr(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY));
	}

	@Test
	public void testRequestParametersOfAReloadTargetAreListedInRequestKeys() {

		setupSelfReloadingButton();

		final Document doc              = Jsoup.parse(fetchPageHtml("/html/page1?mode=compact&sortKey=name"));
		final Map<String, String> attrs = getAttributes(doc.getElementById("save"));

		assertEquals("Wrong request attribute for mode", "compact", attrs.get("data-request-mode"));
		assertEquals("Wrong request attribute for sortKey", "name", attrs.get("data-request-sort-key"));
		assertEquals("data-structr-request-keys must list exactly the rendered data-request-* attributes", Set.of("mode", "sort-key"), requestKeys(attrs));

		// the mapped parameter keeps its own attribute, and it is no request state
		assertEquals("The mapped parameter requestNote renders as data-request-note", "kept", attrs.get("data-request-note"));

		// an element that is no reload target carries no request state at all
		final Map<String, String> other = getAttributes(doc.getElementById("other"));

		assertNull("A trigger that is no reload target has no request attributes", other.get("data-request-mode"));
		assertNull("A trigger that is no reload target has no request key list", other.get("data-structr-request-keys"));
	}

	@Test
	public void testMappedParameterKeepsTheAttributeARequestParameterWouldRender() {

		setupSelfReloadingButton();

		final String html               = fetchPageHtml("/html/page1?mode=compact&note=from-url");
		final Map<String, String> attrs = getAttributes(Jsoup.parse(html).getElementById("save"));

		// the parameter requestNote and the request parameter note both map to data-request-note
		assertEquals("The parameter keeps data-request-note with its own value", "kept", attrs.get("data-request-note"));
		assertEquals("The colliding request parameter must not be rendered a second time", 1, html.split("data-request-note=", -1).length - 1);
		assertEquals("The colliding request parameter is not request state of this element", Set.of("mode"), requestKeys(attrs));
	}

	@Test
	public void testFreeTextTargetAndDialogAttributesAreEscaped() {

		final String selector = "#list[title=\"a & b\"] > .row:not(<none>)";

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = createElement(page, div, "button", "button");

			createMapping(btn, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,                        "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY,                     "Item",
				ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY,             "partial-refresh",
				ActionMappingTraitDefinition.FAILURE_PARTIAL_PROPERTY,               selector,
				ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY,         "custom-dialog",
				ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY, selector,
				ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY,         "custom-dialog",
				ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY, selector,
				ActionMappingTraitDefinition.DIALOG_TYPE_PROPERTY,                   selector
			));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String html               = fetchPageHtml("/html/page1");
		final Element button            = Jsoup.parse(html).getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		for (final String name : List.of("data-structr-failure-target", "data-structr-success-notifications-partial", "data-structr-failure-notifications-partial", "data-structr-dialog-type")) {

			assertEquals("The value of " + name + " must arrive unchanged in the browser", selector, attrs.get(name));
		}

		// an unescaped quote would end the attribute early and turn the rest of the selector into attributes of its own
		assertFalse("The selector must not appear unescaped in the HTML", html.contains("[title=\"a & b\"]"));
		assertFalse("The selector must not break out into an attribute of its own", attrs.containsKey("b\"]"));
	}

	// ----- migration -----

	@Test
	public void testImportRestoresBuiltInActionsThatWereMigratedToMethodCalls() {

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			// damaged by an earlier migration run: the built-in action became a call of a method with its name
			createNamedMapping(page, div, "damaged-none", "method", "none");
			createNamedMapping(page, div, "damaged-prev", "method", "prev-page");
			createNamedMapping(page, div, "damaged-next", "method", "next-page");

			// intact built-in actions, which the migration must leave alone
			createNamedMapping(page, div, "intact-none", "none", null);
			createNamedMapping(page, div, "intact-prev", "prev-page", null);
			createNamedMapping(page, div, "intact-next", "next-page", null);

			// a real method call of another name is no damaged built-in action
			createNamedMapping(page, div, "custom-method", "method", "recalculate");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// the import runs the migration
		doImportExportRoundtrip(true);

		try (final Tx tx = app.tx()) {

			assertActionAndMethod("damaged-none", "none", null);
			assertActionAndMethod("damaged-prev", "prev-page", null);
			assertActionAndMethod("damaged-next", "next-page", null);

			assertActionAndMethod("intact-none", "none", null);
			assertActionAndMethod("intact-prev", "prev-page", null);
			assertActionAndMethod("intact-next", "next-page", null);

			assertActionAndMethod("custom-method", "method", "recalculate");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testImportKeepsACallOfARealMethodNamedLikeABuiltInAction() {

		try (final Tx tx = app.tx()) {

			// a user-defined function that really is called "none"
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "none"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'called'; }")
			);

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			createNamedMapping(page, div, "calls-none", "method", "none");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		doImportExportRoundtrip(true);

		try (final Tx tx = app.tx()) {

			assertActionAndMethod("calls-none", "method", "none");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----

	/**
	 * A page with an Item type and a button whose create action takes its data type from the payload.
	 */
	private String setupCreateButton() {

		String buttonUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("Item");

			StructrSchema.extendDatabaseSchema(app, schema);

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = createElement(page, div, "button", "create");

			buttonUuid = btn.getUuid();

			createMapping(btn, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,    "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY, "AbstractNode"
			));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return buttonUuid;
	}

	/**
	 * A save button that is its own reload target and maps a constant parameter named requestNote, which renders as
	 * data-request-note, plus a second button that is no reload target.
	 */
	private void setupSelfReloadingButton() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page        = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page.getElementsByTagName("div").get(0);
			final DOMElement save  = createElement(page, div, "button", "save");
			final DOMElement other = createElement(page, div, "button", "other");

			final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
			final NodeInterface eam = createMapping(save, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,            "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY,         "Item",
				ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY, "partial-refresh-linked"
			));

			eam.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(save));

			final Traits parameterTraits = Traits.of(StructrTraits.PARAMETER_MAPPING);

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam),
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "constant-value"),
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "requestNote"),
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY), "kept")
			);

			createMapping(other, Map.of(
				ActionMappingTraitDefinition.ACTION_PROPERTY,    "create",
				ActionMappingTraitDefinition.DATA_TYPE_PROPERTY, "Item"
			));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private DOMElement createElement(final Page page, final DOMNode parent, final String tag, final String htmlId) throws FrameworkException {

		final DOMElement element = page.createElement(tag);

		parent.appendChild(element);
		element.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY), htmlId);

		return element;
	}

	private NodeInterface createMapping(final DOMElement trigger, final Map<String, Object> values) throws FrameworkException {

		final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
		final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

		eam.setProperty(traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(trigger));
		eam.setProperty(traits.key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");

		for (final Map.Entry<String, Object> entry : values.entrySet()) {

			eam.setProperty(traits.key(entry.getKey()), entry.getValue());
		}

		return eam;
	}

	private void createNamedMapping(final Page page, final DOMNode parent, final String name, final String action, final String method) throws FrameworkException {

		final Traits traits      = Traits.of(StructrTraits.ACTION_MAPPING);
		final DOMElement trigger = page.createElement("button");

		parent.appendChild(trigger);

		// the export skips a mapping without a trigger element
		final NodeInterface eam = createMapping(trigger, Map.of(ActionMappingTraitDefinition.ACTION_PROPERTY, action));

		eam.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

		if (method != null) {

			eam.setProperty(traits.key(ActionMappingTraitDefinition.METHOD_PROPERTY), method);
		}
	}

	private void assertActionAndMethod(final String name, final String expectedAction, final String expectedMethod) throws FrameworkException {

		final NodeInterface node = app.nodeQuery(StructrTraits.ACTION_MAPPING).name(name).getFirst();

		assertNotNull("The action mapping '" + name + "' did not survive the import", node);

		final ActionMapping mapping = node.as(ActionMapping.class);

		assertEquals("Wrong action on '" + name + "' after the import", expectedAction, mapping.getAction());
		assertEquals("Wrong method on '" + name + "' after the import", expectedMethod, mapping.getMethod());
	}

	/**
	 * A public function configuredFunction, a private function privateFunction that leaves an Item named "private ran"
	 * behind, and in page1 three method buttons: one configured with configuredFunction, and two whose method name is a
	 * template expression naming the private and the public function, which must not be evaluated.
	 */
	private Map<String, String> setupFunctionButtons() {

		final Map<String, String> ids = new LinkedHashMap<>();

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("Item");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Traits methodTraits = Traits.of(StructrTraits.SCHEMA_METHOD);

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "configuredFunction"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'configured'; }")
			);

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "privateFunction"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ $.create('Item', { name: 'private ran' }); return 'private'; }"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY), true)
			);

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			for (final Map.Entry<String, String> entry : Map.of(
				"configured",        "configuredFunction",
				"expressionPrivate", "${'privateFunction'}",
				"expressionPublic",  "${'configuredFunction'}").entrySet()) {

				final DOMElement btn = createElement(page, div, "button", entry.getKey());

				createMapping(btn, Map.of(
					ActionMappingTraitDefinition.ACTION_PROPERTY, "method",
					ActionMappingTraitDefinition.METHOD_PROPERTY, entry.getValue()
				));

				ids.put(entry.getKey(), btn.getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return ids;
	}

	/**
	 * Two Items and, in page1, one update button for each of them.
	 */
	private Map<String, String> setupUpdateButtons() {

		final Map<String, String> ids = new LinkedHashMap<>();

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("Item");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			for (final String name : List.of("A", "B")) {

				final NodeInterface item = app.create("Item", "Item " + name);
				final DOMElement btn     = createElement(page, div, "button", "update-" + name.toLowerCase());

				createMapping(btn, Map.of(
					ActionMappingTraitDefinition.ACTION_PROPERTY,        "update",
					ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY, item.getUuid()
				));

				ids.put("item" + name,   item.getUuid());
				ids.put("button" + name, btn.getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return ids;
	}

	private void assertItemName(final String uuid, final String expectedName) {

		try (final Tx tx = app.tx()) {

			assertEquals("Wrong name of the Item", expectedName, app.getNodeById("Item", uuid).getName());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private int countItems(final String name) {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery("Item").name(name).getAsList().size();

			tx.success();

			return count;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return -1;
	}

	private void postAnonymously(final String elementUuid, final Map<String, Object> payload, final int expectedStatusCode) {

		RestAssured.basePath = "/";

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body(BrowserEventPayload.toJson(payload))
			.expect()
				.statusCode(expectedStatusCode)
			.when()
				.post("/structr/rest/DOMElement/" + elementUuid + "/event");
	}

	/**
	 * Posts what the browser sends for the trigger in page1: its rendered data attributes plus the given values.
	 */
	private io.restassured.response.Response postEvent(final String elementUuid, final Map<String, Object> values, final int expectedStatusCode) {

		final Map<String, Object> payload = browserPayload(elementUuid);

		payload.putAll(values);

		return postPayload(elementUuid, payload, expectedStatusCode);
	}

	private Map<String, Object> browserPayload(final String elementUuid) {

		final Map<String, Object> payload = BrowserEventPayload.of(fetchPageHtml("/html/page1"), elementUuid);

		payload.put("htmlEvent", "click");

		return payload;
	}

	private io.restassured.response.Response postPayload(final String elementUuid, final Map<String, Object> payload, final int expectedStatusCode) {

		RestAssured.basePath = "/";

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body(BrowserEventPayload.toJson(payload))
			.expect()
				.statusCode(expectedStatusCode)
			.when()
				.post("/structr/rest/DOMElement/" + elementUuid + "/event")
			.andReturn();
	}

	private Set<String> requestKeys(final Map<String, String> attrs) {

		final String keys = attrs.get("data-structr-request-keys");

		if (keys == null) {

			return Set.of();
		}

		return new TreeSet<>(Arrays.asList(keys.split(" ")));
	}

	private Map<String, String> getAttributes(final Element element) {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Attribute attr : element.attributes()) {

			map.put(attr.getKey(), attr.getValue());
		}

		return map;
	}

	private String fetchPageHtml(final String path) {

		RestAssured.basePath = "/";

		return RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get(path)
			.andReturn()
				.body().asString();
	}
}
