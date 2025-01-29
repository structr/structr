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
package org.structr.core.property;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.ArraySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A property that stores and retrieves an array of Byte.
 *
 *
 */
public class ByteArrayProperty extends AbstractPrimitiveProperty<Byte[]> {

	private static final Logger logger = LoggerFactory.getLogger(ByteArrayProperty.class.getName());

	public ByteArrayProperty(final String name) {
		super(name);
	}

	public ByteArrayProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}

	public ByteArrayProperty(final String jsonName, final String dbName, final String format) {
		super(jsonName);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return value;
	}

	@Override
	public String typeName() {
		return "Byte[]";
	}

	@Override
	public Class valueType() {
		// This trick results in returning the proper array class for array properties.
		// Necessary because of and since commit 1db80071543018a0766efa2dc895b7bc3e9a0e34
		try {
			return Class.forName("[L" + Byte.class.getName() + ";");
		} catch (ClassNotFoundException ex) {}

		return Byte[].class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<Byte[], byte[]> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Byte[], byte[]> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new ArrayDatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, Byte[]> inputConverter(SecurityContext securityContext) {
		return new ArrayInputConverter(securityContext);
	}

	private class ArrayDatabaseConverter extends PropertyConverter<Byte[], byte[]> {

		public ArrayDatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public byte[] convert(Byte[] source) throws FrameworkException {

			if (source != null) {

				//return Base64.getEncoder().encodeToString(ArrayUtils.toPrimitive(source));
				return ArrayUtils.toPrimitive(source);
			}

			return null;
		}

		@Override
		public Byte[] revert(byte[] source) throws FrameworkException {

			try {

				if (source != null) {

					//return ArrayUtils.toObject(Base64.getDecoder().decode(source));
					return ArrayUtils.toObject(source);
				}


			} catch (final Exception e) {

				throw new FrameworkException(400, "Unable to convert " + source + " to Byte[].", e);
			}

			return null;

		}
	}

	private class ArrayInputConverter extends PropertyConverter<Object, Byte[]> {

		public ArrayInputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Object revert(Byte[] source) throws FrameworkException {

			//return Base64.getEncoder().encodeToString(ArrayUtils.toPrimitive(source));
			try {

				return new String(ArrayUtils.toPrimitive(source), "UTF-8");

			} catch (final UnsupportedEncodingException uee) {

				return new String(ArrayUtils.toPrimitive(source));
			}

		}

		@Override
		public Byte[] convert(Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof String) {

				//return ArrayUtils.toObject(Base64.getDecoder().decode(source.toString()));
				return ArrayUtils.toObject(((String) source).getBytes());
			}

			if (source instanceof List && !((List) source).isEmpty()) {

				final Object firstElement = ((List) source).get(0);
				List<Byte> byteList = new ArrayList<>();

				if (firstElement instanceof Integer) {

					((List<Integer>) source).forEach(i -> byteList.add(i.byteValue()));
					return byteList.toArray(new Byte[byteList.size()]);

				} else if (firstElement instanceof Long) {

					((List<Long>) source).forEach(l -> byteList.add(l.byteValue()));
					return byteList.toArray(new Byte[byteList.size()]);
				}
			}

			return ArrayUtils.toObject(SerializationUtils.serialize((Serializable) source));

		}
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, Byte[] searchValue, boolean exactMatch, Query query) {

		// early exit, return empty search attribute
		if (searchValue == null) {
			return new ArraySearchAttribute(this, "", exactMatch ? occur : Occurrence.OPTIONAL, exactMatch);
		}

		final SearchAttributeGroup group = new SearchAttributeGroup(occur);

		for (byte value : searchValue) {

			group.add(new ArraySearchAttribute(this, value, exactMatch ? occur : Occurrence.OPTIONAL, exactMatch));
		}

		return group;
	}

	@Override
	public Object getExampleValue(String type, String viewName) {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- static methods -----

}
