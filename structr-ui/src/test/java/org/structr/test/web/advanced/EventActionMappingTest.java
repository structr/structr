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
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ParameterMappingTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.testng.annotations.Test;

import java.util.*;

import static org.hamcrest.Matchers.equalTo;
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

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DIALOG_TYPE_PROPERTY), "okcancel");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DIALOG_TITLE_PROPERTY), "example-dialog-title-${me.name}");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DIALOG_TEXT_PROPERTY), "example-dialog-text-${me.name}");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc              = Jsoup.parse(html);
		final Element button            = doc.getElementById("button");
		final Map<String, String> attrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);

		expectedValues.put("data-structr-dialog-type", "okcancel");
		expectedValues.put("data-structr-dialog-title", "example-dialog-title-admin");
		expectedValues.put("data-structr-dialog-text", "example-dialog-text-admin");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY), "system-alert");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);
		expectedValues.put("data-structr-success-notifications", "system-alert");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY), "inline-text-message");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);
		expectedValues.put("data-structr-success-notifications", "inline-text-message");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;
		String notificationUuid             = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY), "custom-dialog-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY), List.of(notificationElement));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);
		expectedValues.put("data-structr-success-notifications", "custom-dialog-linked");
		expectedValues.put("data-structr-success-notifications-custom-dialog-element", "[data-structr-id='" + notificationUuid + "']");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY), "custom-dialog");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY), "#notification-element");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);
		expectedValues.put("data-structr-success-notifications", "custom-dialog");
		expectedValues.put("data-structr-success-notifications-partial", "#notification-element");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked, fire-event)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_EVENT_PROPERTY), "success-notification-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);
		expectedValues.put("data-structr-success-notifications", "fire-event");
		expectedValues.put("data-structr-success-notifications-event", "success-notification-event");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY), "system-alert");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);
		expectedValues.put("data-structr-failure-notifications", "system-alert");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY), "inline-text-message");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);
		expectedValues.put("data-structr-failure-notifications", "inline-text-message");

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;
		String notificationUuid             = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY), "custom-dialog-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATION_ELEMENTS_PROPERTY), List.of(notificationElement));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);
		expectedValues.put("data-structr-failure-notifications", "custom-dialog-linked");
		expectedValues.put("data-structr-failure-notifications-custom-dialog-element", "[data-structr-id='" + notificationUuid + "']");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_EVENT_PROPERTY), "failure-notification-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);
		expectedValues.put("data-structr-failure-notifications", "fire-event");
		expectedValues.put("data-structr-failure-notifications-event", "failure-notification-event");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// failure notifications (possible values are system-alert, inline-text-message, custom-dialog, custom-dialog-linked)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY), "custom-dialog");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY), "#notification-element");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);
		expectedValues.put("data-structr-failure-notifications", "custom-dialog");
		expectedValues.put("data-structr-failure-notifications-partial", "#notification-element");


		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_PARTIAL_PROPERTY), "#name-of-success-partial");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);

		expectedValues.put("data-structr-success-target", "#name-of-success-partial");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;
		String divUuid                      = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(div));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-success-target", "[data-structr-id='" + divUuid + "']");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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
		assertEquals("Wrong value for EAM attribute data-structr-id on reload target", divUuid, divAttrs.get(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY));
	}

	@Test
	public void testSuccessBehaviourAttributesForURL() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "navigate-to-url");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY), "/success");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-success-target", "url:/success");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_EVENT_PROPERTY), "success-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-success-target", "event:success-event");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "full-page-reload");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-success-target", "url:");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "sign-out");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-success-target", "sign-out");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "none");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div  = page1.getElementsByTagName("div").get(0);
			final DOMElement btn   = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);
			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "partial-refresh");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_PARTIAL_PROPERTY), "#name-of-failure-partial");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);

		expectedValues.put("data-structr-failure-target", "#name-of-failure-partial");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;
		String divUuid                      = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_TARGETS_PROPERTY), List.of(div));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-failure-target", "[data-structr-id='" + divUuid + "']");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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
		assertEquals("Wrong value for EAM attribute data-structr-id on reload target", divUuid, divAttrs.get(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY));
	}

	@Test
	public void testFailureBehaviourAttributesForURL() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "navigate-to-url");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_URL_PROPERTY), "/failure");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-failure-target", "url:/failure");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "fire-event");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_EVENT_PROPERTY), "failure-event");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-failure-target", "event:failure-event");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "full-page-reload");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-failure-target", "url:");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String buttonUuid                   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

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
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "sign-out");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc    = Jsoup.parse(html);
		final Element div     = doc.getElementById("parent-container");
		final Element button  = doc.getElementById("button");
		final Map<String, String> buttonAttrs = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, buttonUuid);

		expectedValues.put("data-structr-failure-target", "sign-out");



		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			uuid = btn.getUuid();

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), "none");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		final String html = fetchPageHtml("/html/page1");

		final Document doc   = Jsoup.parse(html);
		final Element button = doc.getElementById("button");
		final Map<String, String> attrs      = getAttributes(button);

		final Map<String, String> expectedValues = new LinkedHashMap<>();

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put(DOMNodeTraitDefinition.DATA_STRUCTR_ID_PROPERTY, uuid);

		final Set<String> expectedNullValues = new LinkedHashSet<>();

		expectedNullValues.add("data-structr-id-expression");
		expectedNullValues.add(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY);    // reload-target is deprecated, replaced by success-target and failure-target
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
	public void testCloneNodeIncludingReloadTarget() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		final List<String> buttonIds        = new LinkedList<>();
		final List<String> divIds           = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(div));

			// now clone the div
			div.getParent().appendChild(div.cloneNode(true));

			// collect IDs
			for (final DOMNode node : page1.getElementsByTagName("div")) {
				divIds.add(node.getUuid());
			}

			for (final DOMNode node : page1.getElementsByTagName("button")) {
				buttonIds.add(node.getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		final String html  = fetchPageHtml("/html/page1");
		final Document doc = Jsoup.parse(html);

		final Map<String, String> expectedValues1 = new LinkedHashMap<>();
		final Map<String, String> expectedValues2 = new LinkedHashMap<>();
		final Map<String, String> attrs1          = getAttributes(doc.getElementsByTag("button").get(0));
		final Map<String, String> attrs2          = getAttributes(doc.getElementsByTag("button").get(1));

		// verify that the div is a reload target (exposes data-structr-id attribute)
		final Map<String, String> div1Attrs = getAttributes(doc.getElementsByTag("div").get(0));
		final Map<String, String> div2Attrs = getAttributes(doc.getElementsByTag("div").get(1));

		assertEquals("Wrong value for cloned EAM attribute data-structr-id", divIds.get(0), div1Attrs.get("data-structr-id"));
		assertEquals("Wrong value for cloned EAM attribute data-structr-id", divIds.get(1), div2Attrs.get("data-structr-id"));

		expectedValues1.put("data-structr-event", "click");
		expectedValues1.put("data-structr-action", "create");
		expectedValues1.put("data-structr-target", "Project");
		expectedValues1.put("data-structr-id",     buttonIds.get(0));
		expectedValues1.put("data-structr-success-target", "[data-structr-id='" + divIds.get(0) + "']");

		for (final String key : expectedValues1.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues1.get(key), attrs1.get(key));
		}

		expectedValues2.put("data-structr-event", "click");
		expectedValues2.put("data-structr-action", "create");
		expectedValues2.put("data-structr-target", "Project");
		expectedValues2.put("data-structr-id",     buttonIds.get(1));
		expectedValues2.put("data-structr-success-target", "[data-structr-id='" + divIds.get(1) + "']");

		for (final String key : expectedValues2.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues2.get(key), attrs2.get(key));
		}
	}

	@Test
	public void testCloneNodeExcludingReloadTarget() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		final List<String> buttonIds        = new LinkedList<>();
		final List<String> divIds           = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(div));

			// now clone the button
			btn.getParent().appendChild(btn.cloneNode(true));

			// collect IDs
			for (final DOMNode node : page1.getElementsByTagName("div")) {
				divIds.add(node.getUuid());
			}

			for (final DOMNode node : page1.getElementsByTagName("button")) {
				buttonIds.add(node.getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		final String html  = fetchPageHtml("/html/page1");
		final Document doc = Jsoup.parse(html);

		final Map<String, String> expectedValues1 = new LinkedHashMap<>();
		final Map<String, String> expectedValues2 = new LinkedHashMap<>();
		final Map<String, String> attrs1          = getAttributes(doc.getElementsByTag("button").get(0));
		final Map<String, String> attrs2          = getAttributes(doc.getElementsByTag("button").get(1));

		// verify that the div is a reload target (exposes data-structr-id attribute)
		final Map<String, String> divAttrs = getAttributes(doc.getElementsByTag("div").get(0));

		assertEquals("Wrong value for cloned EAM attribute data-structr-id", divIds.get(0), divAttrs.get("data-structr-id"));

		expectedValues1.put("data-structr-event", "click");
		expectedValues1.put("data-structr-action", "create");
		expectedValues1.put("data-structr-target", "Project");
		expectedValues1.put("data-structr-id",     buttonIds.get(0));
		expectedValues1.put("data-structr-success-target", "[data-structr-id='" + divIds.get(0) + "']");

		for (final String key : expectedValues1.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues1.get(key), attrs1.get(key));
		}

		expectedValues2.put("data-structr-event", "click");
		expectedValues2.put("data-structr-action", "create");
		expectedValues2.put("data-structr-target", "Project");
		expectedValues2.put("data-structr-id",     buttonIds.get(1));
		expectedValues2.put("data-structr-success-target", "[data-structr-id='" + divIds.get(0) + "']");

		for (final String key : expectedValues2.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues2.get(key), attrs2.get(key));
		}
	}

	@Test
	public void testClonePage() {

		final PropertyKey<String> htmlIdKey = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY);
		String btnId                     = null;
		String divId                        = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			btn.setProperty(htmlIdKey, "button");
			div.setProperty(htmlIdKey, "parent-container");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(div));

			// now clone the page
			final Page page2 = page1.cloneNode(true).as(Page.class);

			page2.setName("page2");

			final DOMNode clonedDiv = page2.getElementsByTagName("div").get(0);
			final DOMNode clonedBtn = page2.getElementsByTagName("button").get(0);

			divId = clonedDiv.getUuid();
			btnId = clonedBtn.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		final String html       = fetchPageHtml("/html/page1");
		final String clonedHtml = fetchPageHtml("/html/page2");
		final Document doc = Jsoup.parse(clonedHtml);

		final Map<String, String> expectedValues = new LinkedHashMap<>();
		final Map<String, String> attrs          = getAttributes(doc.getElementsByTag("button").get(0));

		// verify that the div is a reload target (exposes data-structr-id attribute)
		final Map<String, String> div1Attrs = getAttributes(doc.getElementsByTag("div").get(0));

		assertEquals("Wrong value for cloned EAM attribute data-structr-id", divId, div1Attrs.get("data-structr-id"));

		expectedValues.put("data-structr-event", "click");
		expectedValues.put("data-structr-action", "create");
		expectedValues.put("data-structr-target", "Project");
		expectedValues.put("data-structr-id",     btnId);
		expectedValues.put("data-structr-success-target", "[data-structr-id='" + divId + "']");

		for (final String key : expectedValues.keySet()) {
			assertEquals("Wrong value for EAM attribute " + key, expectedValues.get(key), attrs.get(key));
		}
	}

	@Test
	public void testWrappedResultInCustomMethodOutput() {

		String objectUuid = null;
		String buttonUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			// create schema
			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.addType("Test");

			final JsonMethod method1 = type.addMethod("testMethod", "{ return { test1: 1, test2: 'test1' }; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create EAM
			final Page page1        = Page.createSimplePage(securityContext, "page1");
			final DOMNode div       = page1.getElementsByTagName("div").get(0);
			final DOMElement btn    = page1.createElement("button");
			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			div.appendChild(btn);

			// save uuid for later
			buttonUuid = btn.getUuid();

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.METHOD_PROPERTY), "testMethod");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			objectUuid = app.create("Test").getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: testMethod, structrTarget: '" + objectUuid + "' }")
			.expect()
			.statusCode(200)
			.body("result.test1", equalTo(1))
			.body("result.test2", equalTo("test1"))
			.when()
			.post("/structr/rest/DOMElement/" + buttonUuid + "/event");
	}

	@Test
	public void testRawResultInCustomMethodOutput() {

		String objectUuid = null;
		String buttonUuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			// create schema
			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.addType("Test");

			final JsonMethod method1 = type.addMethod("testMethod", "{ return { test1: 1, test2: 'test1' }; }");

			method1.setReturnRawResult(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			// create EAM
			final Page page1        = Page.createSimplePage(securityContext, "page1");
			final DOMNode div       = page1.getElementsByTagName("div").get(0);
			final DOMElement btn    = page1.createElement("button");
			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			div.appendChild(btn);

			// save uuid for later
			buttonUuid = btn.getUuid();

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.METHOD_PROPERTY), "testMethod");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			objectUuid = app.create("Test").getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: testMethod, structrTarget: '" + objectUuid + "' }")
			.expect()
			.statusCode(200)
			.body("test1", equalTo(1))
			.body("test2", equalTo("test1"))
			.when()
			.post("/structr/rest/DOMElement/" + buttonUuid + "/event");
	}

	@Test
	public void testDeletionOfActionMappingsWithPage() {

		// create EAM
		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");
			final Content text   = page1.createTextNode("Create");

			div.appendChild(btn);
			btn.appendChild(text);

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// base setup
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// success follow-up actions (possible values are partial-refresh, partial-refresh-linked, navigate-to-url, fire-event, full-page-reload, sign-out, none)
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh-linked");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(div));

			// create parameter mappings (to test cascading delete)
			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "param1"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam)
			);

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "param2"),
				new NodeAttribute<>(Traits.of(StructrTraits.PARAMETER_MAPPING).key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), eam)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// delete all DOM nodes
		try (final Tx tx = app.tx()) {

			app.deleteAllNodesOfType(StructrTraits.DOM_NODE);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// verify that EAMs have been deleted as well
		try (final Tx tx = app.tx()) {

			assertEquals("Pages not deleted via deleteAllNodesOfType()", 0, app.nodeQuery(StructrTraits.PAGE).getAsList().size());
			assertEquals("ActionMappings not deleted when deleting a page", 0, app.nodeQuery(StructrTraits.ACTION_MAPPING).getAsList().size());
			assertEquals("ParameterMappings not deleted when deleting a page", 0, app.nodeQuery(StructrTraits.PARAMETER_MAPPING).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testResponseForNonExistingMethod() {

		String objectUuid  = null;
		String button1Uuid = null;
		String button2Uuid = null;
		String button3Uuid = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			// create schema
			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.addType("Test");

			type.addMethod("instanceMethod", "{ return { test1: 1, test2: 'test1' }; }");
			type.addMethod("staticMethod", "{ return { test1: 2, test2: 'test2' }; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "userDefinedFunction"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return { test1: 3, test2: 'test3' }; }")
			);

			// create EAM
			final Page page1        = Page.createSimplePage(securityContext, "page1");
			final DOMNode div       = page1.getElementsByTagName("div").get(0);

			// button1 for instance method
			{
				final DOMElement btn = page1.createElement("button");
				final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

				div.appendChild(btn);

				// save uuid for later
				button1Uuid = btn.getUuid();

				// base setup
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method");
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.METHOD_PROPERTY), "instanceMethod");
			}

			// button2 for static method
			{
				final DOMElement btn = page1.createElement("button");
				final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

				div.appendChild(btn);

				// save uuid for later
				button2Uuid = btn.getUuid();

				// base setup
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method");
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.METHOD_PROPERTY), "staticMethod");
			}

			// button3 for user-defined function
			{
				final DOMElement btn = page1.createElement("button");
				final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

				div.appendChild(btn);

				// save uuid for later
				button3Uuid = btn.getUuid();

				// base setup
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method");
				eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.METHOD_PROPERTY), "userDefinedFunction");
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			objectUuid = app.create("Test").getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		// test instance method
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: instanceMethod, structrTarget: '" + objectUuid + "' }")
			.expect()
			.statusCode(200)
			.body("result.test1", equalTo(1))
			.body("result.test2", equalTo("test1"))
			.when()
			.post("/structr/rest/DOMElement/" + button1Uuid + "/event");

		// test static method
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: staticMethod, structrTarget: 'Test' }")
			.expect()
			.statusCode(200)
			.body("result.test1", equalTo(2))
			.body("result.test2", equalTo("test2"))
			.when()
			.post("/structr/rest/DOMElement/" + button2Uuid + "/event");

		// test user-defined function
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: userDefinedFunction }")
			.expect()
			.statusCode(200)
			.body("result.test1", equalTo(3))
			.body("result.test2", equalTo("test3"))
			.when()
			.post("/structr/rest/DOMElement/" + button3Uuid + "/event");

		// now test the three cases with nonexisting methods
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: instanceMethodNotExisting, structrTarget: '" + objectUuid + "' }")
			.expect()
			.statusCode(422)
			.when()
			.post("/structr/rest/DOMElement/" + button1Uuid + "/event");

		// test static method
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: staticMethodNotExisting, structrTarget: 'Test' }")
			.expect()
			.statusCode(422)
			.when()
			.post("/structr/rest/DOMElement/" + button2Uuid + "/event");

		// test user-defined function
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ htmlEvent: click, structrMethod: userDefinedFunctionNotExisting }")
			.expect()
			.statusCode(422)
			.when()
			.post("/structr/rest/DOMElement/" + button3Uuid + "/event");

	}


	// ----- private methods -----
	final Map<String, String> getAttributes(final Element element) {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Attribute attr : element.attributes()) {

			map.put(attr.getKey(), attr.getValue());
		}

		return map;
	}

	private String fetchPageHtml(final String path) {

		final String html = RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get(path)
			.andReturn()
				.body().asString();

//		System.out.println(html);

		return html;
	}
}
