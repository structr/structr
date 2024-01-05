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
package org.structr.odf.entity;

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.pkg.OdfPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.StringProperty;
import org.structr.storage.StorageProviderFactory;
import org.structr.schema.SchemaService;
import org.structr.transform.VirtualType;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;

/**
 * Base class for ODF exporter
 */
public interface ODFExporter extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("ODFExporter");
		final JsonObjectType file = (JsonObjectType)schema.getType("File");
		final JsonObjectType virt = (JsonObjectType)schema.getType("VirtualType");

		type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ODFExporter"));

		type.addPropertyGetter("resultDocument",         File.class);
		type.addPropertyGetter("documentTemplate",       File.class);
		type.addPropertyGetter("transformationProvider", VirtualType.class);

		type.addMethod("setResultDocument")
			.addParameter("resultDocument", File.class.getName())
			.setSource("setProperty(resultDocumentProperty, (org.structr.dynamic.File)resultDocument);")
			.addException(FrameworkException.class.getName());


		type.addMethod("createDocumentFromTemplate")
			.addParameter("ctx", SecurityContext.class.getName())
			.setSource(ODFExporter.class.getName() + ".createDocumentFromTemplate(this, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("exportImage")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("uuid", String.class.getName())
			.setSource(ODFExporter.class.getName() + ".exportImage(this, uuid);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.relate(file, "EXPORTS_TO",               Cardinality.OneToOne, "resultDocumentForExporter", "resultDocument");
		type.relate(file, "USES_TEMPLATE",            Cardinality.OneToOne, "documentTemplateForExporter", "documentTemplate");
		type.relate(virt, "GETS_TRANSFORMATION_FROM", Cardinality.OneToOne, "odfExporter", "transformationProvider");

		type.addViewProperty(PropertyView.Public, "transformationProvider");
		type.addViewProperty(PropertyView.Public, "documentTemplate");
		type.addViewProperty(PropertyView.Public, "resultDocument");

		type.addViewProperty(PropertyView.Ui, "transformationProvider");
		type.addViewProperty(PropertyView.Ui, "documentTemplate");
		type.addViewProperty(PropertyView.Ui, "resultDocument");
	}}

	//General ODF specific constants and field specifiers
	//Images

	static final String ODF_IMAGE_PARENT_NAME                 = "draw:frame";
	static final String ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME = "draw:name";
	static final String ODF_IMAGE_ATTRIBUTE_FILE_PATH         = "xlink:href";
	static final String ODF_IMAGE_DIRECTORY                   = "Pictures/";

	public File getDocumentTemplate();
	public File getResultDocument();
	public void setResultDocument(final File resultDocument) throws FrameworkException;
	public VirtualType getTransformationProvider();

	/*
	static final Logger logger = LoggerFactory.getLogger(ODTExporter.class.getName());

	public static final Property<VirtualType> transformationProvider = new EndNode("transformationProvider", TransformationRules.class);
	public static final Property<File> documentTemplate              = new EndNode("documentTemplate", DocumentTemplate.class);
	public static final Property<File> resultDocument                = new EndNode("resultDocument", DocumentResult.class);

	public static final View defaultView = new View(ODTExporter.class, PropertyView.Public, id, type, transformationProvider, documentTemplate, resultDocument);

	public static final View uiView = new View(ODTExporter.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
		transformationProvider, documentTemplate, resultDocument
	);
	*/

	public static void createDocumentFromTemplate(final ODFExporter thisNode, final SecurityContext securityContext) throws FrameworkException {

		final File template                   = thisNode.getDocumentTemplate();
		File output                           = thisNode.getResultDocument();
		OdfDocument templateOdt;

		try {

			if (template == null) {
				throw new FrameworkException(422, "Template not set");
			}

			// If no result file is given, create one and set it as result document
			if (output == null) {

				output = FileHelper.createFile(securityContext, new byte[]{}, template.getContentType(), File.class, thisNode.getName().concat("_").concat(template.getName()), false);

				output.setParent(template.getParent());

				output.unlockSystemPropertiesOnce();
				output.setProperty(AbstractNode.type, File.class.getSimpleName());

				thisNode.setResultDocument(output);
			}

			templateOdt = OdfDocument.loadDocument(StorageProviderFactory.getStorageProvider(template).getInputStream());
			templateOdt.save(output.getOutputStream());
			templateOdt.close();

		} catch (Exception e) {

			final Logger logger = LoggerFactory.getLogger(ODFExporter.class);
			logger.error("Error while creating ODS from template", e);

		}
	}

	public static void exportImage(final ODFExporter thisNode, final String uuid) {

		final File output = thisNode.getResultDocument();

		try {

			final App app      = StructrApp.getInstance();
			final Image result = app.nodeQuery(Image.class).and(GraphObject.id, uuid).getFirst();

			String imageName = result.getProperty(new StringProperty("name"));
			String contentType = result.getProperty(new StringProperty("contentType"));

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

			final Logger logger = LoggerFactory.getLogger(ODFExporter.class);
			logger.error("Error while exporting image to document", e);

		}
	}
}
