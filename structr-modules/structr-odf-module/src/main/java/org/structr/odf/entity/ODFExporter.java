/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.io.File;
import java.net.URI;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.pkg.OdfPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import static org.structr.core.GraphObject.createdBy;
import static org.structr.core.GraphObject.createdDate;
import static org.structr.core.GraphObject.id;
import static org.structr.core.GraphObject.lastModifiedDate;
import static org.structr.core.GraphObject.type;
import static org.structr.core.GraphObject.visibilityEndDate;
import static org.structr.core.GraphObject.visibilityStartDate;
import static org.structr.core.GraphObject.visibleToAuthenticatedUsers;
import static org.structr.core.GraphObject.visibleToPublicUsers;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.graph.NodeInterface.deleted;
import static org.structr.core.graph.NodeInterface.hidden;
import static org.structr.core.graph.NodeInterface.name;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.odf.relations.DocumentResult;
import org.structr.odf.relations.DocumentTemplate;
import org.structr.odf.relations.TransformationRules;
import org.structr.transform.VirtualType;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.FileBase;
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

	private final String ODF_IMAGE_PARENT_NAME                 = "draw:frame";
	private final String ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME = "draw:name";
	private final String ODF_IMAGE_ATTRIBUTE_FILE_PATH         = "xlink:href";
	private final String ODF_IMAGE_DIRECTORY                   = "Pictures/";

	protected static final Logger logger = LoggerFactory.getLogger(ODTExporter.class.getName());

	public static final Property<VirtualType> transformationProvider = new EndNode("transformationProvider", TransformationRules.class);
	public static final Property<FileBase> documentTemplate          = new EndNode("documentTemplate", DocumentTemplate.class);
	public static final Property<FileBase> resultDocument            = new EndNode("resultDocument", DocumentResult.class);

	public static final View defaultView = new View(ODTExporter.class, PropertyView.Public, id, type, transformationProvider, documentTemplate, resultDocument);

	public static final View uiView = new View(ODTExporter.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
		transformationProvider, documentTemplate, resultDocument
	);

	@Export
	public void createDocumentFromTemplate() throws FrameworkException {

		OdfDocument templateOdt;
		final FileBase template = getProperty(documentTemplate);
		FileBase output = getProperty(resultDocument);

		try {

			// If no result file is given, create one and set it as result document
			if (output == null) {

				output = FileHelper.createFile(securityContext, new byte[]{}, template.getContentType(), FileBase.class, getName().concat("_").concat(template.getName()));

				output.setProperty(FileBase.parent, template.getProperty(FileBase.parent));

				output.unlockSystemPropertiesOnce();
				output.setProperty(AbstractNode.type, File.class.getSimpleName());

				setProperty(resultDocument, output);

			}

			templateOdt = OdfDocument.loadDocument(template.getFileOnDisk().getAbsolutePath());
			templateOdt.save(output.getOutputStream());
			templateOdt.close();

		} catch (Exception e) {

			logger.error("Error while creating ODS from template", e);

		}
	}

	@Export
	public void exportImage(String uuid) {

		FileBase output = getProperty(resultDocument);

		try {

			final App app = StructrApp.getInstance();
			final Image result = app.nodeQuery(Image.class).and(GraphObject.id, uuid).getFirst();

			String imageName = result.getProperty(new StringProperty("name"));
			String contentType = result.getProperty(new StringProperty("contentType"));

			String templateImagePath = null;

			OdfDocument doc = OdfDocument.loadDocument(output.getFileOnDisk().getAbsolutePath());

			NodeList nodes = doc.getContentRoot().getElementsByTagName(ODF_IMAGE_PARENT_NAME);
			for (int i = 0; i < nodes.getLength(); i++) {

				Node currentNode = nodes.item(i);
				NamedNodeMap attrs = currentNode.getAttributes();
				Node fieldName = attrs.getNamedItem(ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME);
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
			pkg.insert(new URI(result.getFileOnDisk().getAbsolutePath()), ODF_IMAGE_DIRECTORY + imageName, contentType);
			pkg.save(output.getFileOnDisk().getAbsolutePath());
			pkg.close();
			doc.close();

		} catch (Exception e) {

			logger.error("Error while exporting image to document", e);

		}
	}

}
