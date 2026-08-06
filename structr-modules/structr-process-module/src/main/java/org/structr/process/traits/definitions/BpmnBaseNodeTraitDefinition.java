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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;

import java.util.Map;
import java.util.Set;

/**
 * Shared base trait for every node sourced from a BPMN file.
 *
 * <p>Provides the two properties every BPMN node carries:
 * <ul>
 *   <li>{@code bpmnId} - the {@code id} attribute on the BPMN XML element. Stable
 *       across re-imports for a given conceptual element; the importer relies on
 *       it for cross-element references during parsing.</li>
 *   <li>{@code version} - the import generation. Auto-incremented integer string
 *       ({@code "1"}, {@code "2"}, ...) assigned by the importer per
 *       {@code processId}. All nodes created in one import share the same value.
 *       Running ProcessInstances stay anchored to the version they started on
 *       via their {@code definition} relationship; new imports produce a fresh
 *       version, isolating in-flight processes from edits.</li>
 * </ul>
 *
 * <p>Composed into all BPMN node types (BpmnDefinitions, BpmnElement,
 * BpmnSequenceFlow, BpmnDi*, BpmnGlobalDefinition, BpmnPerformer,
 * BpmnTaskListener, BpmnProcessListener) at registration time.</p>
 */
public class BpmnBaseNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String BPMN_ID_PROPERTY  = "bpmnId";
	public static final String VERSION_PROPERTY  = "version";

	public BpmnBaseNodeTraitDefinition() {

		super(ProcessTraits.BPMN_BASE_NODE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> bpmnId  = new StringProperty(BPMN_ID_PROPERTY).indexed();
		final Property<String> version = new StringProperty(VERSION_PROPERTY).indexed();

		return newSet(bpmnId, version);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(PropertyView.Public, newSet(BPMN_ID_PROPERTY, VERSION_PROPERTY), PropertyView.Ui,     newSet(BPMN_ID_PROPERTY, VERSION_PROPERTY));
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
