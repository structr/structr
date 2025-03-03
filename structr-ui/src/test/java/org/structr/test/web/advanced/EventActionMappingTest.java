/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class EventActionMappingTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(EventActionMappingTest.class);

	@Test
	public void testDialogAttributes() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dialogType"), "okcancel");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dialogTitle"), "example-dialog-title-${me.name}");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dialogText"), "example-dialog-text-${me.name}");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc              = Jsoup.parse(html);
		final Element button            = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);

		expectedValues.put("data-structr-dialog-type", "okcancel");
		expectedValues.put("data-structr-dialog-title", "example-dialog-title-admin");
		expectedValues.put("data-structr-dialog-text", "example-dialog-text-admin");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");
		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");
		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testSuccessNotificationAttributesForSystemAlert() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotifications"), "system-alert");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);
		expectedValues.put("data-structr-success-notifications", "system-alert");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testSuccessNotificationAttributesForInlineTextMessage() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotifications"), "inline-text-message");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);
		expectedValues.put("data-structr-success-notifications", "inline-text-message");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testSuccessNotificationAttributesForCustomLinkedDialog() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;
		String notificationUuid             = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final DOMElement notificationElement = page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParent().appendChild(notificationElement);

			notificationUuid = notificationElement.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotifications"), "custom-dialog-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotificationElements"), List.of(notificationElement));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);
		expectedValues.put("data-structr-success-notifications", "custom-dialog-linked");
		expectedValues.put("data-structr-success-notifications-custom-dialog-element", "[data-structr-id='" + notificationUuid + "']");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testSuccessNotificationAttributesForCustomDialog() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final DOMElement notificationElement = page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParent().appendChild(notificationElement);

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotifications"), "custom-dialog");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotificationsPartial"), "#notification-element");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);
		expectedValues.put("data-structr-success-notifications", "custom-dialog");
		expectedValues.put("data-structr-success-notifications-partial", "#notification-element");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications-custom-dialog-element");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testSuccessNotificationAttributesForEvent() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked, fire-event)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotifications"), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successNotificationsEvent"), "success-notification-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);
		expectedValues.put("data-structr-success-notifications", "fire-event");
		expectedValues.put("data-structr-success-notifications-event", "success-notification-event");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications-custom-dialog-element");
		expectedNullValues.add("data-structr-success-notifications-partial");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}
	@Test
	public void testFailureNotificationAttributesForSystemAlert() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotifications"), "system-alert");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);
		expectedValues.put("data-structr-failure-notifications", "system-alert");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testFailureNotificationAttributesForInlineTextMessage() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotifications"), "inline-text-message");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);
		expectedValues.put("data-structr-failure-notifications", "inline-text-message");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testFailureNotificationAttributesForCustomLinkedDialog() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;
		String notificationUuid             = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final DOMElement notificationElement = page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParent().appendChild(notificationElement);

			notificationUuid = notificationElement.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotifications"), "custom-dialog-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotificationElements"), List.of(notificationElement));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);
		expectedValues.put("data-structr-failure-notifications", "custom-dialog-linked");
		expectedValues.put("data-structr-failure-notifications-custom-dialog-element", "[data-structr-id='" + notificationUuid + "']");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testFailureNotificationAttributesForEvent() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotifications"), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotificationsEvent"), "failure-notification-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);
		expectedValues.put("data-structr-failure-notifications", "fire-event");
		expectedValues.put("data-structr-failure-notifications-event", "failure-notification-event");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-failure-notifications-custom-dialog-element");
		expectedNullValues.add("data-structr-failure-notifications-partial");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testFailureNotificationAttributesForCustomDialog() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final DOMElement notificationElement = page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParent().appendChild(notificationElement);

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotifications"), "custom-dialog");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureNotificationsPartial"), "#notification-element");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);
		expectedValues.put("data-structr-failure-notifications", "custom-dialog");
		expectedValues.put("data-structr-failure-notifications-partial", "#notification-element");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-failure-notifications-custom-dialog-element");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}
	@Test
	public void testSuccessBehaviourAttributesForPartialReload() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "partial-refresh");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successPartial"), "#name-of-success-partial");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);

		expectedValues.put("data-structr-success-target", "#name-of-success-partial");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-failure-target");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testSuccessBehaviourAttributesForLinkedPartialReload() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;
		String divUuid                      = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();
			divUuid    = div.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successTargets"), List.of(div));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-success-target", "[data-structr-id='" + divUuid + "']");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}


		final Map<String, String> divAttrs = getAttributes(div);

		// reload target must have data-structr-id attribute
		assertEquals("Wrong value for EAM attribute data-structr-id on reload target", divUuid, divAttrs.get("data-structr-id"));
	}

	@Test
	public void testSuccessBehaviourAttributesForURL() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "navigate-to-url");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successURL"), "/success");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-success-target", "url:/success");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testSuccessBehaviourAttributesForEvent() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successEvent"), "success-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-success-target", "event:success-event");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testSuccessBehaviourAttributesForFullPageReload() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "full-page-reload");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-success-target", "url:");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testSuccessBehaviourAttributesForSignout() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "sign-out");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-success-target", "sign-out");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testSuccessBehaviourAttributesForNone() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("successBehaviour"), "none");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testFailureBehaviourAttributesForPartialReload() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "partial-refresh");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failurePartial"), "#name-of-failure-partial");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);

		expectedValues.put("data-structr-failure-target", "#name-of-failure-partial");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-target");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}

	@Test
	public void testFailureBehaviourAttributesForLinkedPartialReload() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;
		String divUuid                      = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();
			divUuid    = div.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureTargets"), List.of(div));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-failure-target", "[data-structr-id='" + divUuid + "']");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}


		final Map<String, String> divAttrs = getAttributes(div);

		// reload target must have data-structr-id attribute
		assertEquals("Wrong value for EAM attribute data-structr-id on reload target", divUuid, divAttrs.get("data-structr-id"));
	}

	@Test
	public void testFailureBehaviourAttributesForURL() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "navigate-to-url");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureURL"), "/failure");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-failure-target", "url:/failure");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testFailureBehaviourAttributesForEvent() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureEvent"), "failure-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-failure-target", "event:failure-event");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testFailureBehaviourAttributesForFullPageReload() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div      = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "full-page-reload");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-failure-target", "url:");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testFailureBehaviourAttributesForSignout() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key("_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "sign-out");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", buttonUuid);

		expectedValues.put("data-structr-failure-target", "sign-out");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), buttonAttrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, buttonAttrs.get(key));
		}
	}

	@Test
	public void testFailureBehaviourAttributesForNone() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "admin"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key("_html_id"), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("triggerElements"), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("event"), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("action"), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key("failureBehaviour"), "none");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		final String html = RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/html/page1")
			.andReturn()
			.body().asString();

		System.out.println(html);

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id", uuid);

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add("data-structr-reload-target");    // reload-target is deprecated, replaced by success-target and failure-target
		expectedNullValues.add("data-structr-dialog-type");
		expectedNullValues.add("data-structr-dialog-title");
		expectedNullValues.add("data-structr-dialog-text");

		expectedNullValues.add("data-structr-success-notifications");
		expectedNullValues.add("data-structr-success-notifications-partial");
		expectedNullValues.add("data-structr-success-notifications-event");

		expectedNullValues.add("data-structr-failure-notifications");
		expectedNullValues.add("data-structr-failure-notifications-partial");
		expectedNullValues.add("data-structr-failure-notifications-event");

		expectedNullValues.add("data-structr-success-target");
		expectedNullValues.add("data-structr-failure-target");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}

		for (final String key : expectedNullValues) {

			assertEquals("Wrong value for EAM attribute " + key, null, attrs.get(key));
		}
	}
	// ----- private methods -----
	final Map<String, String> getAttributes(final Element element) {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Attribute attr : element.attributes()) {

			map.put(attr.getKey(), attr.getValue());
		}

		return map;
	}
}
