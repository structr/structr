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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.odf.entity.relationship.ODFExporterEXPORTS_TOFile;
import org.structr.odf.entity.relationship.ODFExporterGETS_TRANSFORMATION_FROMVirtualType;
import org.structr.odf.entity.relationship.ODFExporterUSES_TEMPLATEFile;
import org.structr.storage.StorageProviderFactory;
import org.structr.transform.VirtualType;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for ODF exporter
 */
public abstract class ODFExporter extends AbstractNode {

	//General ODF specific constants and field specifiers
	//Images

	static final String ODF_IMAGE_PARENT_NAME                 = "draw:frame";
	static final String ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME = "draw:name";
	static final String ODF_IMAGE_ATTRIBUTE_FILE_PATH         = "xlink:href";
	static final String ODF_IMAGE_DIRECTORY                   = "Pictures/";

	public static final Property<File> resultDocumentProperty                = new EndNode<>("resultDocument", ODFExporterEXPORTS_TOFile.class);
	public static final Property<File> documentTemplateProperty              = new EndNode<>("documentTemplate", ODFExporterUSES_TEMPLATEFile.class);
	public static final Property<VirtualType> transformationProviderProperty = new EndNode<>("transformationProvider", ODFExporterGETS_TRANSFORMATION_FROMVirtualType.class);

	public static final View defaultView = new View(ODFExporter.class, PropertyView.Public,
		transformationProviderProperty, documentTemplateProperty, resultDocumentProperty
	);

	public static final View uiView = new View(ODFExporter.class, PropertyView.Ui,
		transformationProviderProperty, documentTemplateProperty, resultDocumentProperty
	);

	public File getDocumentTemplate() {
		return getProperty(documentTemplateProperty);
	}

	public File getResultDocument() {
		return getProperty(resultDocumentProperty);
	}

	public void setResultDocument(final File resultDocument) throws FrameworkException {
		setProperty(resultDocumentProperty, resultDocument);
	}

	public VirtualType getTransformationProvider() {
		return getProperty(transformationProviderProperty);
	}

	@Export
	public void createDocumentFromTemplate(final SecurityContext securityContext) throws FrameworkException {

		final File template     = getDocumentTemplate();
		File output             = getResultDocument();
		OdfDocument templateOdt;

		try {

			if (template == null) {
				throw new FrameworkException(422, "Template not set");
			}

			// If no result file is given, create one and set it as result document
			if (output == null) {

				output = FileHelper.createFile(securityContext, new byte[]{}, template.getContentType(), File.class, getName().concat("_").concat(template.getName()), false);

				output.setParent(template.getParent());

				output.unlockSystemPropertiesOnce();
				output.setProperty(AbstractNode.typeHandler, File.class.getSimpleName());

				setResultDocument(output);
			}

			templateOdt = OdfDocument.loadDocument(StorageProviderFactory.getStorageProvider(template).getInputStream());
			templateOdt.save(output.getOutputStream());
			templateOdt.close();

		} catch (Exception e) {

			final Logger logger = LoggerFactory.getLogger(ODFExporter.class);
			logger.error("Error while creating ODS from template", e);

		}
	}

	@Export
	public void exportImage(final String uuid) {

		final File output = getResultDocument();

		try {

			final App app      = StructrApp.getInstance();
			final Image result = app.nodeQuery("Image").and(GraphObject.id, uuid).getFirst();

			String imageName = result.getProperty(Image.name);
			String contentType = result.getProperty(Image.contentTypeProperty);

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
