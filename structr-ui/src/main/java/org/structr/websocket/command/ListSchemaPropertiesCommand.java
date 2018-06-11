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
package org.structr.websocket.command;

import java.util.*;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 *
 *
 */
public class ListSchemaPropertiesCommand extends AbstractCommand {

	private static final Property<Boolean> isSelected = new BooleanProperty("isSelected");
	private static final Property<Boolean> isDisabled = new BooleanProperty("isDisabled");

	static {

		StructrWebSocket.addCommand(ListSchemaPropertiesCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String view              = (String)webSocketData.getNodeData().get("view");
		final String id                = webSocketData.getId();
		final List<GraphObject> result = new LinkedList();

		if (view != null) {

			if (id != null) {

				AbstractNode schemaObject = getNode(id);
				if (schemaObject != null) {

					final ConfigurationProvider config = StructrApp.getConfiguration();
					String typeName              = schemaObject.getProperty(AbstractNode.name);

					if (typeName == null && schemaObject instanceof SchemaRelationshipNode) {
						typeName = ((SchemaRelationshipNode) schemaObject).getClassName();
					}

					Class type = config.getNodeEntityClass(typeName);
					if (type == null || GenericNode.class.equals(type)) {

						type = config.getRelationshipEntityClass(typeName);
					}

					if (type != null) {

						final Set<PropertyKey> allProperties    = config.getPropertySet(type, PropertyView.All);
						final Set<PropertyKey> viewProperties   = config.getPropertySet(type, view);
						final Set<PropertyKey> parentProperties = config.getPropertySet(type.getSuperclass(), view);

						for (final PropertyKey key : allProperties) {

							final String declaringClass   = key.getDeclaringClass() != null ? key.getDeclaringClass().getSimpleName() : "GraphObject";
							final String propertyName     = key.jsonName();
							final GraphObjectMap property = new GraphObjectMap();
							final Class valueType         = key.valueType();
							String valueTypeName          = "Unknown";
							boolean _isDisabled           = false;

							if (valueType != null) {
								valueTypeName = valueType.getSimpleName();
							}

							// a property is disabled if it is already present in the view of a superclass
							// (since it has to be configured there instead of locally)
							if (parentProperties.contains(key)) {
								_isDisabled = true;
							}

							property.put(AbstractNode.name, propertyName);
							property.put(isSelected, viewProperties.contains(key));
							property.put(isDisabled, _isDisabled);
							property.put(SchemaProperty.propertyType, valueTypeName);
							property.put(SchemaProperty.notNull, key.isNotNull());
							property.put(SchemaProperty.unique, key.isUnique());
							property.put(SchemaProperty.isDynamic, key.isDynamic());
							property.put(SchemaProperty.declaringClass, declaringClass);

							// store in result
							result.add(property);
						}

					} else {

						getWebSocket().send(MessageBuilder.status().code(404).message("Type " + typeName + " not found.").build(), true);
					}

				} else {

					getWebSocket().send(MessageBuilder.status().code(404).message("Schema node with ID " + id + " not found.").build(), true);
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(422).message("LIST_SCHEMA_PROPERTIES needs an ID.").build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("LIST_SCHEMA_PROPERTIES needs a view name in nodeData.").build(), true);
		}

		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);
		webSocketData.setRawResultCount(1);

		// send only over local connection
		getWebSocket().send(webSocketData, true);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "LIST_SCHEMA_PROPERTIES";

	}

}
