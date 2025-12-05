/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.pkg.OdfPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.structr.core.property.EndNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.odf.entity.ODFExporter;
import org.structr.odf.traits.wrappers.ODFExporterTraitWrapper;
import org.structr.schema.action.EvaluationHints;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;
import java.util.Set;

/**
 * Base class for ODF exporter
 */
public class ODFExporterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String RESULT_DOCUMENT_PROPERTY         = "resultDocument";
	public static final String DOCUMENT_TEMPLATE_PROPERTY       = "documentTemplate";
	public static final String TRANSFORMATION_PROVIDER_PROPERTY = "transformationProvider";

	//General ODF specific constants and field specifiers
	//Images

	private static final String ODF_IMAGE_PARENT_NAME                 = "draw:frame";
	private static final String ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME = "draw:name";
	private static final String ODF_IMAGE_ATTRIBUTE_FILE_PATH         = "xlink:href";
	private static final String ODF_IMAGE_DIRECTORY                   = "Pictures/";

	public ODFExporterTraitDefinition() {
		super(StructrTraits.ODF_EXPORTER);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("createDocumentFromTemplate", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					createDocumentFromTemplate(securityContext, entity.as(ODFExporter.class));
					return null;
				}
			},

			new JavaMethod("exportImage", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final Map<String, Object> map = arguments.toMap();
					final String uuid             = (String)map.get("uuid");

					exportImage(entity.as(ODFExporter.class), uuid);
					return null;
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<NodeInterface> resultDocumentProperty         = new EndNode(traitsInstance, RESULT_DOCUMENT_PROPERTY, StructrTraits.ODF_EXPORTER_EXPORTS_TO_FILE);
		final PropertyKey<NodeInterface> documentTemplateProperty       = new EndNode(traitsInstance, DOCUMENT_TEMPLATE_PROPERTY, StructrTraits.ODF_EXPORTER_USES_TEMPLATE_FILE);
		final PropertyKey<NodeInterface> transformationProviderProperty = new EndNode(traitsInstance, TRANSFORMATION_PROVIDER_PROPERTY, StructrTraits.ODF_EXPORTER_GETS_TRANSFORMATION_FROM_VIRTUAL_TYPE);

		return newSet(
			resultDocumentProperty,
			documentTemplateProperty,
			transformationProviderProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					GraphObjectTraitDefinition.CREATED_DATE_PROPERTY, NodeInterfaceTraitDefinition.HIDDEN_PROPERTY,
					GraphObjectTraitDefinition.LAST_MODIFIED_BY_PROPERTY, GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY,
					GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY,
					TRANSFORMATION_PROVIDER_PROPERTY, DOCUMENT_TEMPLATE_PROPERTY, RESULT_DOCUMENT_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					GraphObjectTraitDefinition.LAST_MODIFIED_BY_PROPERTY, TRANSFORMATION_PROVIDER_PROPERTY, DOCUMENT_TEMPLATE_PROPERTY, RESULT_DOCUMENT_PROPERTY
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ODFExporter.class, (traits, node) -> new ODFExporterTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public void createDocumentFromTemplate(final SecurityContext securityContext, final ODFExporter entity) throws FrameworkException {

		final File template = entity.getDocumentTemplate();
		File output         = entity.getResultDocument();
		OdfDocument templateOdt;

		try {

			if (template == null) {
				throw new FrameworkException(422, "Template not set");
			}

			// If no result file is given, create one and set it as result document
			if (output == null) {

				output = FileHelper.createFile(securityContext, new byte[]{}, template.getContentType(), StructrTraits.FILE, entity.getName().concat("_").concat(template.getName()), false).as(File.class);

				output.setParent(template.getParent());

				entity.setResultDocument(output);
			}

			templateOdt = OdfDocument.loadDocument(StorageProviderFactory.getStorageProvider(template).getInputStream());
			templateOdt.save(output.getOutputStream());
			templateOdt.close();

		} catch (Exception e) {

			final Logger logger = LoggerFactory.getLogger(ODFExporterTraitDefinition.class);
			logger.error("Error while creating ODS from template", e);

		}
	}

	public void exportImage(final ODFExporter entity, final String uuid) {

		final File output = entity.getResultDocument();

		try {

			final App app              = StructrApp.getInstance();
			final Image result         = app.nodeQuery(StructrTraits.IMAGE).key(Traits.of(StructrTraits.IMAGE).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid).getFirst().as(Image.class);
			final String imageName     = result.getName();
			final String contentType   = result.getContentType();

			String templateImagePath = null;

			OdfDocument doc = OdfDocument.loadDocument(StorageProviderFactory.getStorageProvider(output).getInputStream());

			NodeList nodes = doc.getContentRoot().getElementsByTagName(ODF_IMAGE_PARENT_NAME);
			for (int i = 0; i < nodes.getLength(); i++) {

				Node currentNode   = nodes.item(i);
				NamedNodeMap attrs = currentNode.getAttributes();
				Node fieldName    = attrs.getNamedItem(ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME);

				if (fieldName != null && fieldName.getTextContent().equals(imageName)) {

					NamedNodeMap childAttrs = currentNode.getFirstChild().getAttributes();
					Node filePath = childAttrs.getNamedItem(ODF_IMAGE_ATTRIBUTE_FILE_PATH);
					templateImagePath = filePath.getTextContent();
					filePath.setTextContent(ODF_IMAGE_DIRECTORY + imageName);
				}

			}

			OdfPackage pkg = doc.getPackage();
			if (templateImagePath != null && templateImagePath.length() > 0) {

				pkg.remove(templateImagePath);

			}

			pkg.insert(StorageProviderFactory.getStorageProvider(result).getInputStream(), ODF_IMAGE_DIRECTORY + imageName, contentType);
			pkg.save(StorageProviderFactory.getStorageProvider(result).getOutputStream());
			pkg.close();
			doc.close();

		} catch (Exception e) {

			final Logger logger = LoggerFactory.getLogger(ODFExporterTraitDefinition.class);
			logger.error("Error while exporting image to document", e);

		}
	}
}
