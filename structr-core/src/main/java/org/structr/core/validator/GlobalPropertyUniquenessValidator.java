/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.UuidProperty;

//~--- classes ----------------------------------------------------------------

/**
 * A validator that ensures global uniqueness of a given property value,
 * regardless of the entity's type.
 *
 *
 */
public class GlobalPropertyUniquenessValidator<T> implements PropertyValidator<T> {

	private static final Logger logger = LoggerFactory.getLogger(GlobalPropertyUniquenessValidator.class.getName());

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer) {

		
		if (value == null) {

			errorBuffer.add(new EmptyPropertyToken(object.getType(), key));

			return false;

		}

		if (key != null) {

			List<? extends GraphObject> result = null;
			boolean nodeExists        = false;

			try {

// old code, slow because of nodeQuery(AbstractNode.class) => does andTypes() internally which adds all subclasses to the query
//				{		
//
//					long t0 = System.nanoTime();
//
//					result = StructrApp.getInstance().nodeQuery(AbstractNode.class).and(key, value).getAsList();
//
//					long t1 = System.nanoTime() - t0;
//					logger.debug("old code => nodeQuery(AbstractNode.class): {} ns, {} result(s)", new Object[] {t1, result.size()});
//				}

// also slow because it adds a type to the query internally
//				if (key instanceof UuidProperty) {	
//
//					long t0 = System.nanoTime();
//					result = Collections.EMPTY_LIST;
//
//					NodeInterface node = StructrApp.getInstance().getNodeById(value.toString());
//					if (node != null) {
//						result = Arrays.asList(node);
//					}
//
//					long t1 = System.nanoTime() - t0;
//					logger.debug("getNodeById: {} ns, {} result(s)", new Object[] {t1, result.size()});
//				}

				// fastest query for ids, uses internal UUID cache
				if (key instanceof UuidProperty) {

					long t0 = System.nanoTime();
					result = Collections.EMPTY_LIST;
					
					GraphObject obj = StructrApp.getInstance().get(value.toString());
					if (obj != null) {
						result = Arrays.asList(obj);
					}
					
					long t1 = System.nanoTime() - t0;
					logger.debug("get(): {} ns, {} results", new Object[] {t1, result.size()});
				
				} else {

					// fallback for any other property
					result = Collections.EMPTY_LIST;
					if (object instanceof NodeInterface) {

						long t0 = System.nanoTime();
						
						result = StructrApp.getInstance().nodeQuery().and(key, value).getAsList();
						
						long t1 = System.nanoTime() - t0;
						logger.debug("instanceOf NodeInterface => nodeQuery: {} ns, {} result(s)", new Object[] {t1, result.size()});

					} else if (object instanceof RelationshipInterface) {

						long t0 = System.nanoTime();
						
						result = StructrApp.getInstance().relationshipQuery().and(key, value).getAsList();
						
						long t1 = System.nanoTime()- t0;
						logger.debug("instanceOf RelationshipInterface => nodeQuery: {} ns, {} result(s)", new Object[] {t1, result.size()});
					} else {
						
						logger.error("GraphObject is neither NodeInterface nor RelationshipInterface");
						
					}
				}
				
				nodeExists = !result.isEmpty();

			} catch (FrameworkException fex) {

				logger.warn("Unable to fetch list of nodes for uniqueness check", fex);
				// handle error
			}

			if (nodeExists) {

				for (final GraphObject foundNode : result) {

					if (foundNode.getId() != object.getId()) {

						errorBuffer.add(new UniqueToken(object.getType(), key, foundNode.getUuid()));

						return false;
					}
				}
			}
		}

		return true;

	}

	@Override
	public boolean requiresSynchronization() {
		return true;
	}
}
