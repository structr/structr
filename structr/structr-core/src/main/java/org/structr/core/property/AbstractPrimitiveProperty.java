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
package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;


/**
 * Abstract base class for primitive properties.
 * 
 * @author Christian Morgner
 */
public abstract class AbstractPrimitiveProperty<T> extends Property<T> {

	private static final Logger logger = Logger.getLogger(AbstractPrimitiveProperty.class.getName());
	
	public AbstractPrimitiveProperty(String name) {
		super(name);
	}

	public AbstractPrimitiveProperty(String jsonName, String dbName) {
		super(jsonName, dbName);
	}

	public AbstractPrimitiveProperty(String jsonName, String dbName, T defaultValue) {
		super(jsonName, dbName, defaultValue);
	}
	
	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		Object value = null;
		
		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		if (propertyContainer != null) {
			
			if (propertyContainer.hasProperty(dbName())) {

				if (propertyContainer != null) {

					value = propertyContainer.getProperty(dbName());
				}
			}
		}
		
		if(applyConverter) {

			// apply property converters
			PropertyConverter converter = databaseConverter(securityContext, obj);
			if (converter != null) {

				try {
					value = converter.revert(value);

				} catch(Throwable t) {

					// CHM: remove debugging code later
					t.printStackTrace();

					logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
						dbName(),
						getClass().getSimpleName(),
						t.getMessage()
					});
				}
			}
		}

		// no value found, use schema default
		if (value == null) {
			value = defaultValue();
		}
		
		return (T)value;
	}
	
	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
		PropertyConverter converter = databaseConverter(securityContext, obj);
		final Object convertedValue;

		if (converter != null) {

			convertedValue = converter.convert(value);

		} else {

			convertedValue = value;
		}

		final Object oldValue = getProperty(securityContext, obj, true);

		// don't make any changes if
		// - old and new value both are null
		// - old and new value are not null but equal
		if (((convertedValue == null) && (oldValue == null)) || ((convertedValue != null) && (oldValue != null) && convertedValue.equals(oldValue))) {

			return;
		}

		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		if (propertyContainer != null) {

			// Commit value directly to database
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					try {

						// save space
						if (convertedValue == null) {

							propertyContainer.removeProperty(dbName());

						} else {

							// Setting last modified date explicetely is not allowed
							if (!AbstractPrimitiveProperty.this.equals(AbstractNode.lastModifiedDate)) {

								propertyContainer.setProperty(dbName(), convertedValue);

								// set last modified date if not already happened
								propertyContainer.setProperty(AbstractNode.lastModifiedDate.dbName(), System.currentTimeMillis());

							} else {

								logger.log(Level.FINE, "Tried to set lastModifiedDate explicitely (action was denied)");

							}
						}

					} finally {}

					return null;

				}

			});
		}
		
	}
	
	@Override
	public Class relatedType() {
		return null;
	}
	
	@Override
	public boolean isCollection() {
		return false;
	}
}
