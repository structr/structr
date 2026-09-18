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

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.testng.annotations.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Deployment coverage for event action mappings.
 *
 * The export writes an ActionMapping by enumerating its properties by hand, so a property added to the
 * type is not exported until someone edits DeployCommand as well. That is silent: the mapping imports
 * cleanly and the action still works, only the forgotten setting is gone. The tests here cover the two
 * ways that hand-written list goes wrong: a property missing from it, and a property present under the
 * wrong key or filled from the wrong getter.
 */
public class EventActionMappingDeploymentTest extends DeploymentTestBase {

	/** Names that tell the two mappings of the value round-trip apart after the import. */
	private static final String METHOD_FLAVOURED = "mapping-with-method";
	private static final String FLOW_FLAVOURED   = "mapping-with-flow";

	@Test
	public void testNotificationSettingsSurviveARoundtrip() {

		try (final Tx tx = app.tx()) {

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);

			final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			eam.setProperty(traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(traits.key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			eam.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY), "inline-text-message");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_DELAY_PROPERTY), 3000);
			eam.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_TEXT_PROPERTY), "Gespeichert ({status})");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_CSS_CLASS_PROPERTY), "toast toast-success");

			eam.setProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY), "inline-text-message");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_DELAY_PROPERTY), 4000);
			eam.setProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_TEXT_PROPERTY), "Fehler: {message}");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_CSS_CLASS_PROPERTY), "toast toast-error");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// export, wipe the database, import again
		doImportExportRoundtrip(true);

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> mappings = app.nodeQuery(StructrTraits.ACTION_MAPPING).getAsList();

			assertEquals("Expected exactly one action mapping after the roundtrip", 1, mappings.size());

			final ActionMapping imported = mappings.get(0).as(ActionMapping.class);

			assertNotNull("The action mapping did not survive the roundtrip", imported);

			assertEquals("Success notification text was lost in the deployment roundtrip", "Gespeichert ({status})", imported.getSuccessNotificationsText());

			assertEquals("Success notification CSS class was lost in the deployment roundtrip", "toast toast-success", imported.getSuccessNotificationsCssClass());

			assertEquals("Failure notification text was lost in the deployment roundtrip", "Fehler: {message}", imported.getFailureNotificationsText());

			assertEquals("Failure notification CSS class was lost in the deployment roundtrip", "toast toast-error", imported.getFailureNotificationsCssClass());

			// the settings that were already exported have to keep working
			assertEquals("Success notification mode was lost", "inline-text-message", imported.getSuccessNotifications());
			assertEquals("Success notification delay was lost", Integer.valueOf(3000), imported.getSuccessNotificationsDelay());
			assertEquals("Failure notification delay was lost", Integer.valueOf(4000), imported.getFailureNotificationsDelay());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEveryConfigurationPropertyIsExported() {

		try (final Tx tx = app.tx()) {

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);

			final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// a minimal mapping is enough: the export writes every key it enumerates, null values included
			eam.setProperty(traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(traits.key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(traits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Set<String> exported = exportedActionMappingKeys();
		final Set<String> missing  = new TreeSet<>();

		for (final PropertyKey key : configurationProperties()) {

			if (!exported.contains(key.jsonName())) {

				missing.add(key.jsonName());
			}
		}

		assertTrue("These properties are configuration a deployment has to carry, but DeployCommand.exportActionMapping"
			+ " does not write them, so they are silently lost on every export and import: " + missing
			+ ". Add a putData line for each.", missing.isEmpty());
	}

	@Test
	public void testEveryExportedPropertyKeepsItsValue() {

		// action, method, flow and dataType are a coupled group: a migration step clears the two of them that
		// the action does not use, so one mapping cannot carry a value for all four. The method-flavoured one
		// carries everything except flow, the flow-flavoured one carries flow.
		final Map<String, Object> withMethod = distinctValuePerConfigurationProperty();

		withMethod.put(ActionMappingTraitDefinition.ACTION_PROPERTY, "method");
		withMethod.remove(ActionMappingTraitDefinition.FLOW_PROPERTY);

		final Map<String, Object> withFlow = Map.of(ActionMappingTraitDefinition.ACTION_PROPERTY, "flow", ActionMappingTraitDefinition.FLOW_PROPERTY,   "value-for-flow");

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			createActionMapping(page, div, METHOD_FLAVOURED, withMethod);
			createActionMapping(page, div, FLOW_FLAVOURED,   withFlow);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// export, wipe the database, import again
		doImportExportRoundtrip(true);

		try (final Tx tx = app.tx()) {

			final Map<String, String> wrong = new TreeMap<>();

			collectMismatches(METHOD_FLAVOURED, withMethod, wrong);
			collectMismatches(FLOW_FLAVOURED,   withFlow,   wrong);

			assertTrue("Every distinct value was written to a property of its own name, so a property that comes back"
				+ " with a different value is exported from the wrong getter, written under the wrong key, or dropped"
				+ " by the import: " + wrong, wrong.isEmpty());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private void createActionMapping(final Page page, final DOMNode parent, final String name, final Map<String, Object> values) throws FrameworkException {

		final DOMElement btn    = page.createElement("button");
		final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
		final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

		parent.appendChild(btn);

		// the export skips a mapping without a trigger element
		eam.setProperty(traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
		eam.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

		for (final Map.Entry<String, Object> entry : values.entrySet()) {

			eam.setProperty(traits.key(entry.getKey()), entry.getValue());
		}
	}

	private void collectMismatches(final String name, final Map<String, Object> expected, final Map<String, String> wrong) throws FrameworkException {

		final Traits traits          = Traits.of(StructrTraits.ACTION_MAPPING);
		final NodeInterface imported = app.nodeQuery(StructrTraits.ACTION_MAPPING).name(name).getFirst();

		assertNotNull("The action mapping '" + name + "' did not survive the roundtrip", imported);

		for (final Map.Entry<String, Object> entry : expected.entrySet()) {

			final Object actual = imported.getProperty(traits.key(entry.getKey()));
			if (!Objects.equals(entry.getValue(), actual)) {

				wrong.put(name + "." + entry.getKey(), "expected " + entry.getValue() + ", got " + actual);
			}
		}
	}

	/**
	 * The properties a deployment has to carry: everything the ActionMapping trait itself declares, minus the
	 * relationships, which are carried by the nodes they connect rather than written into the entry. Reading them
	 * off the trait rather than off a view keeps a property that no view lists (dataType) inside the coverage.
	 */
	private List<PropertyKey> configurationProperties() {

		final List<PropertyKey> keys = new ArrayList<>();

		for (final PropertyKey key : Traits.getTrait(StructrTraits.ACTION_MAPPING).getPropertyKeys().values()) {

			if (key instanceof RelationProperty) {

				continue;
			}

			keys.add(key);
		}

		keys.sort(Comparator.comparing(PropertyKey::jsonName));

		return keys;
	}

	/**
	 * A value per configuration property that no other property carries, so a mix-up between two of them shows up
	 * as a mismatch rather than as two equal strings.
	 */
	private Map<String, Object> distinctValuePerConfigurationProperty() {

		final Map<String, Object> values = new TreeMap<>();
		int index                        = 0;

		for (final PropertyKey key : configurationProperties()) {

			final Class valueType = key.valueType();
			if (String.class.equals(valueType)) {

				values.put(key.jsonName(), "value-for-" + key.jsonName());

			} else if (Integer.class.equals(valueType)) {

				values.put(key.jsonName(), 1000 + index);

			} else {

				fail("No test value for ActionMapping." + key.jsonName() + " of type " + valueType
					+ ". Add one here, otherwise the new property is exported without anything checking its value.");
			}

			index++;
		}

		return values;
	}

	/**
	 * Exports the app and returns the keys of the single action mapping entry.
	 */
	private Set<String> exportedActionMappingKeys() {

		final Path tmp = Paths.get("/tmp/structr-eam-export-test" + System.currentTimeMillis() + System.nanoTime());

		try {

			final Map<String, Object> params = new HashMap<>();

			params.put("mode", "export");
			params.put("target", tmp.toString());

			app.command(DeployCommand.class).execute(params);

			final Path file = tmp.resolve("events/action-mapping.json");

			assertTrue("The export did not write " + file, Files.exists(file));

			try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

				final List<Map<String, Object>> entries = new Gson().fromJson(reader, List.class);

				assertEquals("Expected exactly one exported action mapping", 1, entries.size());

				return new TreeSet<>(entries.get(0).keySet());
			}

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception while exporting: " + t.getMessage());

			return Set.of();

		} finally {

			try { FileUtils.deleteDirectory(tmp.toFile()); } catch (Throwable ignore) {}
		}
	}
}
