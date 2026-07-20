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
package org.structr.process.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnCollaboration;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.process.entity.BpmnDiDiagram;
import org.structr.process.entity.BpmnGlobalDefinition;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnDefinitionsTraitDefinition;

public class BpmnDefinitionsTraitWrapper extends AbstractNodeTraitWrapper implements BpmnDefinitions {

	public BpmnDefinitionsTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getBpmnId() {
		return wrappedObject.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
	}

	@Override
	public String getTargetNamespace() {
		return wrappedObject.getProperty(traits.key(BpmnDefinitionsTraitDefinition.TARGET_NAMESPACE_PROPERTY));
	}

	@Override
	public String getExporter() {
		return wrappedObject.getProperty(traits.key(BpmnDefinitionsTraitDefinition.EXPORTER_PROPERTY));
	}

	@Override
	public String getExporterVersion() {
		return wrappedObject.getProperty(traits.key(BpmnDefinitionsTraitDefinition.EXPORTER_VERSION_PROPERTY));
	}

	@Override
	public String getNamespaceDeclarations() {
		return wrappedObject.getProperty(traits.key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS));
	}

	@Override
	public String getSecurityLevel() {
		return wrappedObject.getProperty(traits.key(BpmnDefinitionsTraitDefinition.SECURITY_LEVEL_PROPERTY));
	}

	@Override
	public Iterable<BpmnGlobalDefinition> getGlobalDefinitions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnDefinitionsTraitDefinition.GLOBAL_DEFINITIONS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnGlobalDefinition.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<BpmnProcess> getProcesses() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnDefinitionsTraitDefinition.PROCESSES_PROPERTY);

		return Iterables.map(n -> n.as(BpmnProcess.class), wrappedObject.getProperty(key));
	}

	@Override
	public BpmnCollaboration getCollaboration() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(BpmnDefinitionsTraitDefinition.COLLABORATION_PROPERTY));

		return node != null ? node.as(BpmnCollaboration.class) : null;
	}

	@Override
	public Iterable<BpmnDiDiagram> getDiagrams() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnDefinitionsTraitDefinition.DIAGRAMS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnDiDiagram.class), wrappedObject.getProperty(key));
	}
}
