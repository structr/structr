/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.process.deployment;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.deployment.BpmnDeploymentHandler;
import org.structr.test.web.advanced.DeploymentTestBase;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
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
import java.util.Set;
import java.util.TreeSet;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Deployment coverage for the ActionMapping properties this module adds.
 *
 * The process module registers four process-control properties on the base ActionMapping type, and
 * DeployCommand knows nothing about them: they are carried by this module's own deployment file, from
 * another hand-written list. A property added to the type therefore needs an entry in one of two lists,
 * and is silently dropped from every export until it gets one. This test asks the trait what it declares
 * and the two export files what they write, so neither list can fall behind without failing here.
 */
public class EventActionMappingProcessDeploymentTest extends DeploymentTestBase {

	@Test
	public void testEveryConfigurationPropertyIsExportedByBaseOrByTheProcessModule() {

		try (final Tx tx = app.tx()) {

			final Page page      = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final DOMElement btn = page.createElement("button");

			div.appendChild(btn);

			final Traits traits     = Traits.of(StructrTraits.ACTION_MAPPING);
			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			// the export skips a mapping without a trigger element
			eam.setProperty(traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));

			// the module writes only the properties that have a value, so every one of them gets one
			for (final PropertyKey key : configurationProperties()) {

				eam.setProperty(traits.key(key.jsonName()), valueFor(key));
			}

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

		assertTrue("These properties are configuration a deployment has to carry, but neither"
			+ " DeployCommand.exportActionMapping nor BpmnDeploymentHandler.ACTION_MAPPING_PATCH_KEYS writes them,"
			+ " so they are silently lost on every export and import: " + missing
			+ ". Add each one to the list that owns it.", missing.isEmpty());
	}

	/**
	 * Everything the ActionMapping trait itself declares, minus the relationships, which are carried by the
	 * nodes they connect rather than written into an entry. With this module loaded the trait also carries
	 * the process-control properties it registers.
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

	private Object valueFor(final PropertyKey key) {

		final Class valueType = key.valueType();
		if (String.class.equals(valueType)) {

			return "value-for-" + key.jsonName();
		}

		if (Integer.class.equals(valueType)) {

			return 1000;
		}

		fail("No test value for ActionMapping." + key.jsonName() + " of type " + valueType
			+ ". Add one here, otherwise the new property is never checked against the export.");

		return null;
	}

	/**
	 * Exports the app and returns the keys of the single action mapping, from both files that carry it.
	 */
	private Set<String> exportedActionMappingKeys() {

		final Path tmp = Paths.get("/tmp/structr-eam-process-export-test" + System.currentTimeMillis() + System.nanoTime());

		try {

			final Map<String, Object> params = new HashMap<>();

			params.put("mode", "export");
			params.put("target", tmp.toString());

			app.command(DeployCommand.class).execute(params);

			final Set<String> keys = new TreeSet<>();

			keys.addAll(readBaseKeys(tmp.resolve("events/action-mapping.json")));
			keys.addAll(readPatchKeys(tmp.resolve("modules/process/" + BpmnDeploymentHandler.DEPLOYMENT_FILE_NAME)));

			return keys;

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception while exporting: " + t.getMessage());

			return Set.of();

		} finally {

			try { FileUtils.deleteDirectory(tmp.toFile()); } catch (Throwable ignore) {}
		}
	}

	private Set<String> readBaseKeys(final Path file) throws Exception {

		assertTrue("The export did not write " + file, Files.exists(file));

		try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

			final List<Map<String, Object>> entries = new Gson().fromJson(reader, List.class);

			assertEquals("Expected exactly one exported action mapping", 1, entries.size());

			return new TreeSet<>(entries.get(0).keySet());
		}
	}

	private Set<String> readPatchKeys(final Path file) throws Exception {

		assertTrue("The export did not write " + file, Files.exists(file));

		try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

			final Map<String, Object> document        = new Gson().fromJson(reader, Map.class);
			final List<Map<String, Object>> patches   = (List<Map<String, Object>>) document.get("propertyPatches");
			final Set<String> keys                    = new TreeSet<>();

			if (patches != null) {

				for (final Map<String, Object> patch : patches) {

					if (StructrTraits.ACTION_MAPPING.equals(patch.get("type"))) {

						keys.addAll(((Map<String, Object>) patch.get("properties")).keySet());
					}
				}
			}

			return keys;
		}
	}
}
