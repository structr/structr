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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnExporter;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.process.traits.wrappers.BpmnDefinitionsTraitWrapper;

/**
 * Trait definition for BpmnDefinitions -- the {@code <bpmn:definitions>}
 * file root. Holds file-level metadata (target namespace, exporter info,
 * namespace declarations) and references its child {@code BpmnProcess}
 * entries plus an optional {@code BpmnCollaboration}.
 *
 * <p>Per-process state (processId, processName, isExecutable, attached
 * elements / sequence flows / methods / process listeners) lives on
 * {@link BpmnProcessTraitDefinition}. A definitions file with a single
 * process holds one BpmnProcess; a collaboration file holds many.</p>
 *
 * <p>Static methods exposed via REST:
 * <ul>
 *   <li>{@code POST /BpmnDefinitions/importBpmn} (static) -- import a
 *       BPMN 2.0.2 XML string.</li>
 *   <li>{@code POST /BpmnDefinitions/{id}/exportBpmn} -- export this file
 *       back to XML.</li>
 * </ul>
 * To start a process instance, call {@code startProcess} on a BpmnProcess.</p>
 */
public class BpmnDefinitionsTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String TARGET_NAMESPACE_PROPERTY     = "targetNamespace";
	public static final String EXPORTER_PROPERTY             = "exporter";
	public static final String EXPORTER_VERSION_PROPERTY     = "exporterVersion";
	public static final String NAMESPACE_DECLARATIONS        = "namespaceDeclarations";
	public static final String GLOBAL_DEFINITIONS_PROPERTY   = "globalDefinitions";
	public static final String PROCESSES_PROPERTY            = "processes";
	public static final String COLLABORATION_PROPERTY        = "collaboration";
	public static final String DIAGRAMS_PROPERTY             = "diagrams";
	public static final String SECURITY_LEVEL_PROPERTY       = "securityLevel";

	// Security level constants
	public static final String SECURITY_LEVEL_LOW            = "low";
	public static final String SECURITY_LEVEL_HIGH           = "high";

	public BpmnDefinitionsTraitDefinition() {

		super(ProcessTraits.BPMN_DEFINITIONS);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(BpmnDefinitions.class, (traits, node) -> new BpmnDefinitionsTraitWrapper(traits, node));
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> targetNamespace                    = new StringProperty(TARGET_NAMESPACE_PROPERTY);
		final Property<String> exporter                           = new StringProperty(EXPORTER_PROPERTY);
		final Property<String> exporterVersion                    = new StringProperty(EXPORTER_VERSION_PROPERTY);
		final Property<String> namespaceDeclarations              = new StringProperty(NAMESPACE_DECLARATIONS);
		final Property<Iterable<NodeInterface>> globalDefinitions = new EndNodes(traitsInstance, GLOBAL_DEFINITIONS_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION);
		final Property<Iterable<NodeInterface>> processes         = new EndNodes(traitsInstance, PROCESSES_PROPERTY,         ProcessTraits.BPMN_DEFINITIONS_HAS_PROCESS);
		final Property<NodeInterface>           collaboration     = new EndNode(traitsInstance,  COLLABORATION_PROPERTY,     ProcessTraits.BPMN_DEFINITIONS_HAS_COLLABORATION);
		final Property<Iterable<NodeInterface>> diagrams          = new EndNodes(traitsInstance, DIAGRAMS_PROPERTY,           ProcessTraits.BPMN_DEFINITIONS_HAS_DIAGRAM);

		// Post the multi-process refactor, both ActionMapping CONTROLS and
		// VisibilityMapping FOR target BpmnProcess (not BpmnDefinitions), so
		// their inverse properties live on BpmnProcess. BpmnDefinitions has no
		// incoming rels from AM / VM directly.
		final Property<String> securityLevel                      = new EnumProperty(SECURITY_LEVEL_PROPERTY, Set.of(SECURITY_LEVEL_LOW, SECURITY_LEVEL_HIGH)).defaultValue(SECURITY_LEVEL_HIGH).indexed();

		return newSet(targetNamespace, exporter, exporterVersion, namespaceDeclarations, globalDefinitions, processes, collaboration, diagrams, securityLevel);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				SECURITY_LEVEL_PROPERTY, GLOBAL_DEFINITIONS_PROPERTY, PROCESSES_PROPERTY, COLLABORATION_PROPERTY, DIAGRAMS_PROPERTY),
			PropertyView.Ui, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				TARGET_NAMESPACE_PROPERTY, EXPORTER_PROPERTY, EXPORTER_VERSION_PROPERTY, NAMESPACE_DECLARATIONS,
				SECURITY_LEVEL_PROPERTY, GLOBAL_DEFINITIONS_PROPERTY, PROCESSES_PROPERTY, COLLABORATION_PROPERTY, DIAGRAMS_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("exportBpmn", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					if (entity instanceof NodeInterface node) {

						return new BpmnExporter().exportBpmn(node.as(BpmnDefinitions.class));
					}

					return null;
				}

				@Override
				public String getDescription() {

					return "Exports this BpmnDefinitions (file root, possibly multi-process) to BPMN 2.0.2 XML and returns the XML string.";
				}
			},

			new JavaMethod("importBpmn", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final Map<String, Object> args = arguments.toMap();
					final Object xmlArg            = args.get("xml");

					if (!(xmlArg instanceof String) || ((String) xmlArg).isEmpty()) {

						throw new FrameworkException(422, "Missing required parameter 'xml' (BPMN 2.0.2 XML string).");
					}

					final String xml                      = (String) xmlArg;
					final Object filenameArg              = args.get("filename");
					final String filename                 = (filenameArg instanceof String && !((String) filenameArg).isEmpty()) ? (String) filenameArg : "imported.bpmn";
					final SecurityContext securityContext = actionContext.getSecurityContext();

					// Persist the source XML in Structr's virtual file system so
					// imports leave a record callers can re-trigger from scripts
					// or audit later. File creation failure is logged but does
					// not block the import -- the actual definition is what the
					// caller cares about.
					try {

						FileHelper.createFile(securityContext, xml.getBytes(StandardCharsets.UTF_8), "application/xml", StructrTraits.FILE, filename, true);

					} catch (IOException ioe) {

						// Non-fatal: the importer can still run on the raw XML.
					}

					final BpmnImporter importer = new BpmnImporter(securityContext);

					return importer.importBpmn(xml);
				}

				@Override
				public String getDescription() {

					return "Imports a BPMN 2.0.2 XML string and creates a new BpmnDefinitions (file root) with one or more BpmnProcess children. Pass 'xml' (required) and optional 'filename' for the persisted File node.";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}

	/**
	 * Resolve the "subject" argument from a startProcess call into a single
	 * NodeInterface. Used by the BpmnProcess startProcess method.
	 * Accepts: null (returned as null), a NodeInterface, a UUID string, or a
	 * Map with an "id" field. A list is rejected with a clear error: each
	 * process instance has at most one subject; for batch operations,
	 * callers should loop and invoke startProcess per item.
	 */
	public static NodeInterface resolveSubject(final ActionContext actionContext, final Object arg) throws FrameworkException {

		if (arg == null) {

			return null;
		}

		if (arg instanceof Iterable<?> || arg.getClass().isArray()) {

			throw new FrameworkException(422, "startProcess accepts at most one 'subject' per call. " +
				"For batch operations, loop and invoke startProcess once per subject.");
		}

		if (arg instanceof NodeInterface) {

			return (NodeInterface) arg;
		}

		final App app = StructrApp.getInstance(actionContext.getSecurityContext());
		String uuid = null;

		if (arg instanceof String) {

			uuid = (String) arg;

		} else if (arg instanceof Map) {

			final Object idObj = ((Map<?, ?>) arg).get("id");
			if (idObj instanceof String) {

				uuid = (String) idObj;
			}
		}

		if (uuid == null || uuid.isEmpty()) {

			throw new FrameworkException(422, "Cannot resolve subject from value of type " + arg.getClass().getName() +
				" (expected NodeInterface, UUID string, or {id: \"<uuid>\"})");
		}

		final NodeInterface node = app.getNodeById(uuid);
		if (node == null) {

			throw new FrameworkException(422, "Subject with id '" + uuid + "' not found");
		}

		return node;
	}
}
