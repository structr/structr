/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.schema.SchemaService;
import org.structr.transform.VirtualType;
import org.structr.web.entity.FileBase;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads a nodes attributes and tries to replace matching attributes in the
 * given ODT-File template.
 */
public class ODTExporter extends ODFExporter {

	private final String ODT_FIELD_TAG_NAME        = "text:user-field-decl";
	private final String ODT_FIELD_ATTRIBUTE_NAME  = "text:name";
	private final String ODT_FIELD_ATTRIBUTE_VALUE = "office:string-value";

	@Export
	public void exportAttributes(String uuid) throws FrameworkException {

		FileBase output = getProperty(resultDocument);
		VirtualType transformation = getProperty(transformationProvider);

		try {

			final App app = StructrApp.getInstance();
			final Result result = app.nodeQuery(AbstractNode.class).and(GraphObject.id, uuid).getResult();
			final Result transformedResult = transformation.transformOutput(securityContext, AbstractNode.class, result);

			Map<String, Object> nodeProperties = new HashMap<>();
			GraphObjectMap node = (GraphObjectMap) transformedResult.get(0);
			node.getPropertyKeys(null).forEach(
				p -> nodeProperties.put(p.dbName(), node.getProperty(p))
			);

			OdfTextDocument text = OdfTextDocument.loadDocument(output.getFileOnDisk().getAbsolutePath());

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

			text.save(output.getFileOnDisk().getAbsolutePath());
			text.close();

		} catch (Exception e) {
			logger.error("Error while exporting to ODT", e);
		}
	}

	static {
		SchemaService.registerBuiltinTypeOverride("ODTExporter", ODTExporter.class.getName());
	}
}
