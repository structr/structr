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
package org.structr.web.migration;

import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.BulkGraphOperation;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.property.PropertyMap;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.dom.relationship.DOMSiblings;
import org.structr.web.entity.html.relation.ResourceLink;
import org.structr.web.entity.relation.PageLink;
import org.structr.web.entity.relation.Sync;

/**
 * Migrate UI relationships of the pre-0.9 scheme to the new scheme
 *
 *
 */
public class BulkMigrateRelationships extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkMigrateRelationships.class.getName());

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("migrateRelationships", BulkMigrateRelationships.class);

	}

	@Override
	public void execute(final Map<String, Object> map) throws FrameworkException {

		final DatabaseService graphDb        = (DatabaseService)arguments.get("graphDb");
		final RelationshipFactory relFactory = new RelationshipFactory(securityContext);

		if (graphDb != null) {

			final Iterator<AbstractRelationship> relIterator = Iterables.map(relFactory, relFactory.bulkInstantiate(graphDb.getAllRelationships())).iterator();

			final long counter = bulkGraphOperation(securityContext, relIterator, 1000, "MigrateRelationships", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					// Treat only relationships with UUID and which are instantiated as generic relationships
					if (rel.getProperty(GraphObject.id) != null) {

						Class type = rel.getClass();

						if (!type.equals(GenericRelationship.class)) return;

						try {

							String relType   = rel.getType();

							rel.getRelationship().removeProperty("combinedType");
							rel.unlockSystemPropertiesOnce();

							if ("CONTAINS".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, DOMChildren.class.getSimpleName()));

							} else if ("CONTAINS_NEXT_SIBLING".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, DOMSiblings.class.getSimpleName()));

							} else if ("OWNS".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, PrincipalOwnsNode.class.getSimpleName()));

							} else if ("SECURITY".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, Security.class.getSimpleName()));

							} else if ("PAGE".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, PageLink.class.getSimpleName()));

							} else if ("LINK".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, ResourceLink.class.getSimpleName()));

							} else if ("SYNC".equals(relType)) {

								rel.setProperties(securityContext, new PropertyMap(AbstractRelationship.type, Sync.class.getSimpleName()));

							}


						} catch (FrameworkException fex) {

							logger.warn("Unable to migrate relationship {}: {}", new Object[] { rel.getUuid(), fex.getMessage() } );

						}
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.warn("Unable to migrate relationship {}: {}", new Object[] { rel.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to migrate relationship: {}", t.getMessage() );
				}
			});

			logger.info("Finished setting properties on {} nodes", counter);

		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
