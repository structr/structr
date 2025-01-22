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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.html.Button;
import org.structr.web.entity.html.Div;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "dialogType"), "okcancel");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dialogTitle"), "example-dialog-title-${me.name}");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dialogText"), "example-dialog-text-${me.name}");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotifications"), "system-alert");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotifications"), "inline-text-message");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;
		String notificationUuid             = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final Div notificationElement = (Div)page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParentNode().appendChild(notificationElement);

			notificationUuid = notificationElement.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotifications"), "custom-dialog-linked");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotificationElements"), List.of(notificationElement));

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final Div notificationElement = (Div)page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParentNode().appendChild(notificationElement);

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotifications"), "custom-dialog");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotificationsPartial"), "#notification-element");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked, fire-event)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotifications"), "fire-event");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successNotificationsEvent"), "success-notification-event");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotifications"), "system-alert");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotifications"), "inline-text-message");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;
		String notificationUuid             = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final Div notificationElement = (Div)page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParentNode().appendChild(notificationElement);

			notificationUuid = notificationElement.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotifications"), "custom-dialog-linked");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotificationElements"), List.of(notificationElement));

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotifications"), "fire-event");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotificationsEvent"), "failure-notification-event");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");

			buttonUuid = btn.getUuid();

			final Div notificationElement = (Div)page1.createElement("div");
			notificationElement.setProperty(htmlIdKey, "notification-element");
			div.getParentNode().appendChild(notificationElement);

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotifications"), "custom-dialog");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureNotificationsPartial"), "#notification-element");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "partial-refresh");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successPartial"), "#name-of-success-partial");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;
		String divUuid                      = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();
			divUuid    = div.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "partial-refresh-linked");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successTargets"), List.of(div));

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "navigate-to-url");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successURL"), "/success");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "fire-event");
			eam.setProperty(StructrApp.key(ActionMapping.class, "successEvent"), "success-event");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "full-page-reload");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "sign-out");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "successBehaviour"), "none");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "partial-refresh");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failurePartial"), "#name-of-failure-partial");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;
		String divUuid                      = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();
			divUuid    = div.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "partial-refresh-linked");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureTargets"), List.of(div));

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "navigate-to-url");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureURL"), "/failure");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "fire-event");
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureEvent"), "failure-event");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "full-page-reload");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

		final PropertyKey<String> htmlIdKey = StructrApp.key(DOMElement.class, "_html_id");
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Div div      = (Div)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			buttonUuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "sign-out");

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

		final org.jsoup.nodes.Document doc    = Jsoup.parse(html);
		final org.jsoup.nodes.Element div     = doc.getElementById("parent-container");
		final org.jsoup.nodes.Element button  = doc.getElementById("button");
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

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final Element div  = (Element)page1.getElementsByTagName("div").item(0);
			final Button btn   = (Button)page1.createElement("button");
			final Content text = (Content)page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(StructrApp.key(Button.class, "_html_id"), "button");

			uuid = btn.getUuid();

			final ActionMapping eam = app.create("ActionMapping");

			// base setup
			eam.setProperty(StructrApp.key(ActionMapping.class, "triggerElements"), List.of(btn));
			eam.setProperty(StructrApp.key(ActionMapping.class, "event"), "click");
			eam.setProperty(StructrApp.key(ActionMapping.class, "action"), "create");
			eam.setProperty(StructrApp.key(ActionMapping.class, "dataType"), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, naviate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"), "none");

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

		final org.jsoup.nodes.Document doc   = Jsoup.parse(html);
		final org.jsoup.nodes.Element button = doc.getElementById("button");
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
	final Map<String, String> getAttributes(final org.jsoup.nodes.Element element) {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final org.jsoup.nodes.Attribute attr : element.attributes()) {

			map.put(attr.getKey(), attr.getValue());
		}

		return map;
	}
}
