/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.PropertyKey;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class BulkFixNodePropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkFixNodePropertiesCommand.class.getName());
	
	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String propertyName      = (String)attributes.get("name");
		final String entityTypeName    = (String)attributes.get("type");
		long nodeCount                 = 0L;

		if (propertyName != null && entityTypeName != null) {

			final Class type = EntityContext.getEntityClassForRawType(entityTypeName);
			if (type != null) {
				
				final PropertyKey propertyToFix   = EntityContext.getPropertyKeyForName(type, propertyName);
				final Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(true, false, Search.andExactType(type.getSimpleName()));
				final List<AbstractNode> nodes    = result.getResults();

				if (propertyToFix != null && type != null) {

					logger.log(Level.INFO, "Start fixing {0} properties of type {1}", new Object[] { propertyToFix.dbName(), type.getSimpleName() } );

					final Iterator<AbstractNode> nodeIterator = nodes.iterator();
					while (nodeIterator.hasNext()) {

						nodeCount += Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Integer>() {

							@Override
							public Integer execute() throws FrameworkException {

								int count = 0;

								while (nodeIterator.hasNext()) {

									AbstractNode abstractNode = nodeIterator.next();
									Node databaseNode         = abstractNode.getNode();

									if (databaseNode.hasProperty(propertyToFix.dbName())) {

										PropertyConverter dbConverter = propertyToFix.databaseConverter(securityContext, abstractNode);
										PropertyConverter inConverter = propertyToFix.inputConverter(securityContext);
										Object rawValue               = databaseNode.getProperty(propertyToFix.dbName());
										Object correctedValue         = null;

										try {

											// try to use converter first
											correctedValue = dbConverter.convert(rawValue);

										} catch(Throwable t0) {

											try {
												// conversion did not work, try input converter
												correctedValue = inConverter.convert(rawValue);

											} catch (Throwable t1) {

												logger.log(Level.WARNING, "Unable to fix property type for {0} on {1}, please specify conversionType!", new Object[] {
													propertyToFix.dbName(),
													type.getSimpleName()
												}
												);
											}
										}

										if (correctedValue != null) {

											try {

//												logger.log(Level.INFO, "Setting {0} of {1} {2} to {3} {4}", new Object[] {
//													propertyToFix.name(),
//													abstractNode.getType(),
//													abstractNode.getUuid(),
//													correctedValue.getClass(),
//													correctedValue
//												});

												abstractNode.setProperty(propertyToFix, correctedValue);


											} catch(Throwable t) {


												logger.log(Level.WARNING, "Unable to set property {0} on {1}: {2}", new Object[] {
													propertyToFix.dbName(),
													type.getSimpleName(),
													t.getMessage()
												}
												);

											}
										}
									}

									// restart transaction after 1000 iterations
									if (++count == 100) {
										break;
									}
								}

								return count;
							}

						});

						logger.log(Level.INFO, "Fixed {0} nodes ...", nodeCount);
					}

					logger.log(Level.INFO, "Done");

					return;
				}
			}
		}

		logger.log(Level.INFO, "Unable to determine property and/or entity type to fix.");
	}
	
}
