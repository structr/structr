/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.Map;
import java.util.Map.Entry;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

/**
 * Sets the properties found in the property set on all nodes matching the type.
 * If no type property is found, set the properties on all nodes.
 *
 *
 */
public class BulkSetNodePropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkSetNodePropertiesCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		executeWithCount(properties);
	}

	public long executeWithCount(final Map<String, Object> properties) throws FrameworkException {

		final PropertyKey<String> typeProperty = Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY);
		final PropertyKey<String> idProperty   = Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY);
		final String type                      = (String)properties.get("type");

		if (StringUtils.isBlank(type)) {

			throw new FrameworkException(422, "Type must not be empty");
		}

		if (!Traits.exists(type)) {

			throw new FrameworkException(422, "Invalid type " + type);
		}

		final App app = StructrApp.getInstance();

		// remove "type" so it won't be set later
		properties.remove("type");

		// to be able to change the type (i.e. the labels) of a node, we cannot rely on the node query here, hence we need to fetch ALL nodes
		final long count = bulkGraphOperation(securityContext, app.nodeQuery(), 1000, "SetNodeProperties", new BulkGraphOperation<NodeInterface>() {

			@Override
			public boolean handleGraphObject(final SecurityContext securityContext, final NodeInterface node) {

				// Treat only "our" nodes
				if (node.getProperty(idProperty) != null && type.equals(node.getProperty(typeProperty))) {

					for (Entry entry : properties.entrySet()) {

						String key = (String) entry.getKey();
						Object val = null;

						// allow to set new type
						if (key.equals("newType")) {
							key = "type";
						}

						PropertyKey propertyKey = node.getTraits().key(key);
						if (propertyKey != null) {

							final PropertyConverter inputConverter = propertyKey.inputConverter(securityContext);
							if (inputConverter != null) {

								try {

									val = inputConverter.convert(entry.getValue());

								} catch (FrameworkException ex) {
									logger.error(ExceptionUtils.getStackTrace(ex));
								}

							} else {

								val = entry.getValue();
							}

							try {
								node.unlockSystemPropertiesOnce();
								node.setProperty(propertyKey, val);

							} catch (FrameworkException fex) {
								logger.warn("Unable to set node property {} of node {} to {}: {}", propertyKey, node.getUuid(), val, fex.getMessage());

							}
						}
					}
				}

				return true;
			}

			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, NodeInterface node) {
				logger.warn("Unable to set properties of node {}: {}", new Object[] { node.getUuid(), t.getMessage() } );
			}

			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
				logger.warn("Unable to set node properties: {}", t.getMessage() );
			}
		});


		logger.info("Fixed {} nodes ...", count);
		logger.info("Done");

		return count;
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}
}
