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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;

/**
 *
 *
 */
public class JoinProperty extends StringProperty {

	private static final Logger logger = LoggerFactory.getLogger(JoinProperty.class.getName());

	private List<PropertyKey> keys = new ArrayList<>();

	public JoinProperty(final String name, final String separator, final PropertyKey... keys) {
		this(name, name, separator, keys);
	}

	public JoinProperty(final String jsonName, final String dbName, final String messageFormat, final PropertyKey... keys) {
		super(jsonName);

		this.dbName = dbName;
		this.format = messageFormat;
		this.keys.addAll(Arrays.asList(keys));

		this.passivelyIndexed();
	}

	@Override
	public String getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		final ArrayList<Object> arguments = new ArrayList<>();

		for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {

			final PropertyKey key                  = it.next();
			final PropertyConverter inputConverter = key.inputConverter(securityContext, false);

			if (inputConverter != null) {

				try {
					final Object value = inputConverter.revert(key.getProperty(securityContext, obj, applyConverter, predicate));
					if (value != null) {

						arguments.add(value);
					}

				} catch (FrameworkException fex) {
					logger.warn("", fex);
				}

			} else {

				final Object value = key.getProperty(securityContext, obj, applyConverter, predicate);
				if (value != null) {

					arguments.add(value);
				}
			}
		}

		try {
			return MessageFormat.format(format, arguments.toArray());

		} catch (Throwable t) { }

		return null;
	}

	@Override
	public Object setProperty(SecurityContext securityContext, final GraphObject obj, String value) throws FrameworkException {

		final MessageFormat formatter = new MessageFormat(format, Locale.GERMAN);
		Object[] values                   = null;
		int len                           = 0;

		try {
			values = formatter.parse(value);
			len    = values.length;

		} catch (ParseException pex) {
			throw new FrameworkException(422, pex.getMessage());
		}

		for (int i=0; i<len; i++) {

			final PropertyKey key                  = keys.get(i);
			final PropertyConverter inputConverter = key.inputConverter(securityContext, false);

			if (inputConverter != null) {

				key.setProperty(securityContext, obj, inputConverter.convert(values[i]));

			} else {

				key.setProperty(securityContext, obj, values[i]);
			}
		}

		return null;
	}
}
