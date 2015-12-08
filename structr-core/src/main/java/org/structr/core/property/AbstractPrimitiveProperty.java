/**
 * Copyright (C) 2010-2015 Structr GmbH
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
package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.TransactionCommand;


/**
 * Abstract base class for primitive properties.
 *
 *
 * @param <T>
 */
public abstract class AbstractPrimitiveProperty<T> extends Property<T> {

	private static final Logger logger = Logger.getLogger(AbstractPrimitiveProperty.class.getName());

	public static final String STRING_EMPTY_FIELD_VALUE		= new String(new byte[] { 0 } );

	private boolean internalSystemProperty = false;
	protected GraphObject entity;
	protected SecurityContext securityContext;


	public AbstractPrimitiveProperty(final String name) {
		super(name);
	}

	public AbstractPrimitiveProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}

	public AbstractPrimitiveProperty(final String jsonName, final String dbName, final T defaultValue) {
		super(jsonName, dbName, defaultValue);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		Object value = null;

		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		if (propertyContainer != null) {

			// this may throw a java.lang.IllegalStateException: Relationship[<id>] has been deleted in this tx
			if (propertyContainer.hasProperty(dbName())) {

				value = propertyContainer.getProperty(dbName());
			}
		}

		if (applyConverter) {

			// apply property converters
			PropertyConverter converter = databaseConverter(securityContext, obj);
			if (converter != null) {

				try {
					value = converter.revert(value);

				} catch (Throwable t) {

					t.printStackTrace();

					logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
						dbName(),
						getClass().getSimpleName(),
						t
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
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final T value) throws FrameworkException {

		final PropertyConverter converter = databaseConverter(securityContext, obj);
		final Object convertedValue;

		if (converter != null) {

			convertedValue = converter.convert(value);

		} else {

			convertedValue = value;
		}

		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		if (propertyContainer != null) {

			if (!TransactionCommand.inTransaction()) {

				logger.log(Level.SEVERE, "setProperty outside of transaction");
				throw new FrameworkException(500, "setProperty outside of transaction.");
			}

			// notify only non-system properties
			if (!unvalidated) {

				// collect modified properties
				if (obj instanceof AbstractNode) {

					TransactionCommand.nodeModified(
						securityContext.getCachedUser(),
						(AbstractNode)obj,
						AbstractPrimitiveProperty.this,
						propertyContainer.hasProperty(dbName()) ? propertyContainer.getProperty(dbName()) : null,
						value
					);

				} else if (obj instanceof AbstractRelationship) {

					TransactionCommand.relationshipModified(
						securityContext.getCachedUser(),
						(AbstractRelationship)obj,
						AbstractPrimitiveProperty.this,
						propertyContainer.hasProperty(dbName()) ? propertyContainer.getProperty(dbName()) : null,
						value
					);
				}
			}

			// catch all sorts of errors and wrap them in a FrameworkException
			try {

				// save space
				if (convertedValue == null) {

					propertyContainer.removeProperty(dbName());

				} else {

					if (!internalSystemProperty) {

						propertyContainer.setProperty(dbName(), convertedValue);

					} else {

						logger.log(Level.FINE, "Tried to set internal system property (action was denied).");

					}
				}

				updateAccessInformation(securityContext, propertyContainer);

			} catch (Throwable t) {

				// throw FrameworkException with the given cause
				throw new FrameworkException(500, t);
			}

			if (isIndexed()) {

				// do indexing, needs to be done after
				// setProperty to make spatial index
				// work
				if (!isPassivelyIndexed()) {

					index(obj, convertedValue);
				}
			}
		}

		return null;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public String getValueForEmptyFields() {
		return STRING_EMPTY_FIELD_VALUE;
	}

	public AbstractPrimitiveProperty<T> internalSystemProperty() {

		this.internalSystemProperty = true;
		return this;
	}

	// ----- private methods -----
	private void updateAccessInformation(final SecurityContext securityContext, final PropertyContainer propertyContainer) throws FrameworkException {

		try {

			final Principal user = securityContext.getUser(false);
			String modifiedById  = null;

			if (user != null) {

				if (user instanceof SuperUser) {

					// "virtual" UUID of superuser
					modifiedById = Principal.SUPERUSER_ID;

				} else {

					modifiedById = user.getUuid();
				}

				propertyContainer.setProperty(AbstractNode.lastModifiedBy.dbName(),   modifiedById);
			}

			if (!securityContext.dontModifyAccessTime()) {

				propertyContainer.setProperty(AbstractNode.lastModifiedDate.dbName(), System.currentTimeMillis());
			}


		} catch (Throwable t) {

			// fail without throwing an exception here
			t.printStackTrace();
		}
	}
}
