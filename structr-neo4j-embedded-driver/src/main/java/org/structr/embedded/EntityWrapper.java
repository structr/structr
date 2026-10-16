/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.embedded;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.TransientFailureException;
import org.neo4j.values.storable.LongArray;
import org.neo4j.values.storable.Values;
import org.structr.api.ConstraintViolationException;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.Iterables;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

abstract class EntityWrapper<T extends Entity> implements PropertyContainer<String> {

	private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES = Map.of(
		Boolean.class,   Boolean.TYPE,
		Byte.class,      Byte.TYPE,
		Character.class, Character.TYPE,
		Short.class,     Short.TYPE,
		Integer.class,   Integer.TYPE,
		Long.class,      Long.TYPE,
		Float.class,     Float.TYPE,
		Double.class,    Double.TYPE
	);

	private final Map<String, Object> propertyCacheForDeletedEntities = new HashMap<>();
	protected final EmbeddedDatabaseService database;
	protected final EmbeddedIdentity        identity;
	protected final T entity;

	protected boolean deleted = false;

	public EntityWrapper(final EmbeddedDatabaseService db, final T entity) {

		this.identity = new EmbeddedIdentity(entity.getElementId());
		this.entity   = entity;
		this.database = db;
	}

	protected abstract String getQueryPrefix();

	@Override
	public int hashCode() {

		return getId().hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		return other.hashCode() == this.hashCode();
	}

	@Override
	public Identity<String> getId() {

		return identity;
	}

	@Override
	public boolean hasProperty(final String name) {

		if (isDeleted()) {

			return propertyCacheForDeletedEntities.containsKey(name);
		}

		return entity.hasProperty(name);
	}

	@Override
	public Object getProperty(final String name) {

		if (isDeleted()) {

			// use cached properties for deleted entities

			return propertyCacheForDeletedEntities.get(name);
		}

		// one property read instead of two (hasProperty and getProperty), Neo4j does not store null values
		final Object value = entity.getProperty(name, null);
		if (value != null) {

			return revertFromStorage(value);
		}

		return null;
	}

	@Override
	public Object getProperty(final String name, final Object defaultValue) {

		final Object value = getProperty(name);
		if (value == null) {

			return defaultValue;
		}

		return value;
	}

	@Override
	public void setProperty(final String key, final Object value) {

		if (value != null) {

			final Object v = convertForStorage(value);

			try {

				entity.setProperty(key, v);

			} catch (IllegalArgumentException e) {

				e.printStackTrace();
				throw new ConstraintViolationException(e, "constraint_violation", e.getMessage());

			} catch (TransientFailureException tex) {

				throw database.terminated(tex);
			}

		} else {

			removeProperty(key);
		}
	}

	@Override
	public void setProperties(final Map<String, Object> values) {

		for (final Entry<String, Object> entry : values.entrySet()) {

			setProperty(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void removeProperty(final String key) {

		try {

			entity.removeProperty(key);

		} catch (TransientFailureException tex) {

			throw database.terminated(tex);
		}
	}

	@Override
	public Iterable<String> getPropertyKeys() {

		return entity.getPropertyKeys();
	}

	@Override
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {

		propertyCacheForDeletedEntities.putAll(entity.getAllProperties());

		try {

			entity.delete();

		} catch (TransientFailureException tex) {

			throw database.terminated(tex);
		}

		deleted = true;
	}

	@Override
	public boolean isDeleted() {

		return deleted;
	}

	// ----- package-private methods -----
	static Object convertForStorage(final Object value) {

		if (value != null) {

			if (value instanceof Iterable i) {

				final List<?> list = Iterables.toList(i);
				if (!list.isEmpty()) {

					return convertArray(toTypedArray(list));
				}
			}

			if (value.getClass().isArray()) {

				return convertArray(value);
			}
		}

		return value;
	}

	static Object revertFromStorage(final Object value) {

		if (value != null) {

			final Class type = value.getClass();
			if (type.isArray()) {

				final Class<?> componentType = type.getComponentType();
				final Class<?> wrapperType = switch (type.getComponentType().getName()) {
					case "boolean" -> Boolean.class;

					case "byte" -> Byte.class;
					case "char" -> Character.class;
					case "short" -> Short.class;
					case "int" -> Integer.class;
					case "long" -> Long.class;
					case "float" -> Float.class;
					case "double" -> Double.class;
					default -> componentType;
				};

				final int    length   = Array.getLength(value);
				final Object newArray = Array.newInstance(wrapperType, length);

				for (int i = 0; i < length; i++) {

					Array.set(newArray, i, Array.get(value, i));
				}

				return newArray;
			}
		}

		return value;
	}

	// ----- private methods -----
	/**
	 * Copies a list into an array of the type all its elements share (e.g. String[] or Double[]), because
	 * Neo4j only stores typed arrays. A list of nulls becomes a String[], as before.
	 */
	private static Object[] toTypedArray(final List<?> list) {

		Class<?> elementType = null;

		for (final Object element : list) {

			if (element != null) {

				if (elementType == null) {

					elementType = element.getClass();

				} else if (!elementType.isInstance(element)) {

					// mixed types: Neo4j rejects the array with its own error
					elementType = Object.class;
					break;
				}
			}
		}

		final Class<?> arrayType = elementType != null ? elementType : String.class;

		return list.toArray((Object[]) Array.newInstance(arrayType, 0));
	}

	/**
	 * Converts an array of wrapper objects (e.g. Double[]) into the corresponding primitive array. Other
	 * arrays (Strings, temporal values, primitives), and arrays with null elements, which Neo4j cannot
	 * store, are returned unchanged.
	 */
	private static Object convertArray(final Object value) {

		final Class<?> primitiveType = PRIMITIVE_TYPES.get(value.getClass().getComponentType());
		if (primitiveType == null) {

			return value;
		}

		final int    length   = Array.getLength(value);
		final Object newArray = Array.newInstance(primitiveType, length);

		for (int i = 0; i < length; i++) {

			final Object element = Array.get(value, i);
			if (element == null) {

				return value;
			}

			Array.set(newArray, i, element);
		}

		return newArray;
	}
}
