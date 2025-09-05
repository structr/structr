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
package org.structr.core.property;

import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.function.CryptFunction;

/**
 * A {@link StringProperty} that stores an encrypted string.
 */
public class EncryptedStringProperty extends StringProperty {

	public EncryptedStringProperty(final String name) {
		this(name, name);
	}

	public EncryptedStringProperty(final String name, final String dbName) {
		super(name, dbName);
	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String clearText) throws FrameworkException {

		if (clearText != null) {

			return super.setProperty(securityContext, obj, clearText);
			//return super.setProperty(securityContext, obj, CryptFunction.encrypt(clearText));

		} else {

			return super.setProperty(securityContext, obj, null);
		}
	}

	@Override
	public String getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		final String encryptedString = super.getProperty(securityContext, obj, applyConverter, predicate);
		if (encryptedString != null) {

			return encryptedString;
			//return CryptFunction.decrypt(encryptedString);
		}

		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext) {

		return new PropertyConverter<>(securityContext) {

			@Override
			public String revert(final Object source) throws FrameworkException {

				if (source != null) {

					return CryptFunction.decrypt(source.toString());
				}

				return null;
			}

			@Override
			public Object convert(final String source) throws FrameworkException {

				if (source != null) {

					return CryptFunction.encrypt(source);
				}

				return null;
			}
		};
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {

		return new PropertyConverter<>(securityContext) {

			@Override
			public String revert(Object source) throws FrameworkException {

				if (source != null) {

					return CryptFunction.decrypt(source.toString());
				}

				return null;
			}

			@Override
			public Object convert(String source) throws FrameworkException {

				if (source != null) {

					return CryptFunction.encrypt(source);
				}

				return null;
			}
		};
	}
}
