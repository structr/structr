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
package org.structr.odf.traits.definitions;

import org.odftoolkit.simple.TextDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.odf.entity.ODTExporter;
import org.structr.odf.traits.wrappers.ODTExporterTraitWrapper;
import org.structr.schema.action.EvaluationHints;
import org.structr.storage.StorageProviderFactory;
import org.structr.transform.VirtualType;
import org.structr.web.entity.File;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Reads a nodes attributes and tries to replace matching attributes in the
 * given ODT-File template.
 */
public class ODTExporterTraitDefinition extends AbstractNodeTraitDefinition {

	private static final String ODT_FIELD_TAG_NAME        = "text:user-field-decl";
	private static final String ODT_FIELD_ATTRIBUTE_NAME  = "text:name";
	private static final String ODT_FIELD_ATTRIBUTE_VALUE = "office:string-value";

	public ODTExporterTraitDefinition() {
		super("ODTExporter");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ODTExporter.class, (traits, node) -> new ODTExporterTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("exportAttributes", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final Map<String, Object> data = arguments.toMap();
					final String uuid              = (String)data.get("uuid");

					exportAttributes(securityContext, entity.as(ODTExporter.class), uuid);
					return null;
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public void exportAttributes(final SecurityContext securityContext, final ODTExporter entity, final String uuid) throws FrameworkException {

		final File output                = entity.getResultDocument();
		final VirtualType transformation = entity.getTransformationProvider();

		try {

			final App app                        = StructrApp.getInstance(securityContext);
			final ResultStream result            = app.nodeQuery("AbstractNode").and(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid).getResultStream();
			final ResultStream transformedResult = transformation.transformOutput(securityContext, StructrTraits.NODE_INTERFACE, result);

			Map<String, Object> nodeProperties = new HashMap<>();
			GraphObjectMap node = (GraphObjectMap) Iterables.first(transformedResult);
			node.getPropertyKeys(null).forEach(
				p -> nodeProperties.put(p.dbName(), node.getProperty(p))
			);

			TextDocument text = TextDocument.loadDocument(StorageProviderFactory.getStorageProvider(output).getInputStream());

			NodeList nodes = text.getContentRoot().getElementsByTagName(ODT_FIELD_TAG_NAME);
			for (int i = 0; i < nodes.getLength(); i++) {

				Node currentNode = nodes.item(i);
				NamedNodeMap attrs = currentNode.getAttributes();
				Node fieldName = attrs.getNamedItem(ODT_FIELD_ATTRIBUTE_NAME);
				Object nodeFieldValue = nodeProperties.get(fieldName.getNodeValue());
				Node currentContent = attrs.getNamedItem(ODT_FIELD_ATTRIBUTE_VALUE);

				if (nodeFieldValue != null) {
					if (nodeFieldValue instanceof String[]) {

						String[] arr = (String[]) nodeFieldValue;
						List<String> list = new ArrayList<>(Arrays.asList(arr));

						StringBuilder sb = new StringBuilder();
						list.forEach(
							s -> sb.append(s + "\n")
						);

						currentContent.setNodeValue(sb.toString());

					} else if (nodeFieldValue instanceof Collection) {

						Collection col = (Collection) nodeFieldValue;
						StringBuilder sb = new StringBuilder();
						col.forEach(
							s -> sb.append(s + "\n")
						);

						currentContent.setNodeValue(sb.toString());

					} else {

						currentContent.setNodeValue(nodeFieldValue.toString());

					}
				}

			}

			text.save(StorageProviderFactory.getStorageProvider(output).getOutputStream());
			text.close();

		} catch (Exception e) {
			final Logger logger = LoggerFactory.getLogger(ODTExporterTraitDefinition.class);
			logger.error("Error while exporting to ODT", e);
		}
	}
}
