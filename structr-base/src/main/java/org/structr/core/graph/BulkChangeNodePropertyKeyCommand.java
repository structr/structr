/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.docs.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Change the property key from the old to the new value on all nodes matching the type.
 *
 * Example: "email":"foo@bar.com" => "eMail":"foo@bar.com"
 *
 * If no type property is found, change the property key on all nodes.
 * If a property with the new key is already present, the command will abort.
 *
 *
 */
public class BulkChangeNodePropertyKeyCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkChangeNodePropertyKeyCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final String oldKey	= (String) properties.get("oldKey");
		final String newKey	= (String) properties.get("newKey");
		String type             = null;

		if (StringUtils.isNotBlank(oldKey) && StringUtils.isNotBlank(newKey)) {

			if (properties.containsKey("type")) {

				type = (String) properties.get("type");

				properties.remove("type");
			}

			final long count = bulkGraphOperation(securityContext, getNodeQuery(type, true), 1000, "ChangeNodePropertyKey", new BulkGraphOperation<NodeInterface>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, NodeInterface node) {

					for (Entry entry : properties.entrySet()) {

						String key = (String) entry.getKey();

						PropertyKey propertyKey = node.getTraits().key(key);
						if (propertyKey != null) {

							Node dbNode = node.getNode();

							if (dbNode.hasProperty(newKey)) {

								logger.error("Node {} has already a property with key {}", node, newKey);
								throw new IllegalStateException("Node has already a property of the new key");

							}

							if (dbNode.hasProperty(oldKey)) {

								dbNode.setProperty(newKey, dbNode.getProperty(oldKey));
								dbNode.removeProperty(oldKey);

							}
						}
					}

					return true;
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, NodeInterface node) {
					logger.warn("Unable to set properties of node {}: {}", node.getUuid(), t.getMessage());
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set node properties: {}", t.getMessage() );
				}
			});


			logger.info("Fixed {} nodes ...", count);

		} else {

			logger.info("No values for oldKey and/or newKey found, aborting.");

		}

		logger.info("Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getType() {
		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {
		return "changeNodePropertyKey";
	}

	@Override
	public String getShortDescription() {
		return "Migrates property values from one property key to another.";
	}

	@Override
	public String getLongDescription() {
		return "This command can for example be used to move all name values to a description property.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("oldKey", "source key"),
			Parameter.mandatory("newKey", "target key")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}
}
