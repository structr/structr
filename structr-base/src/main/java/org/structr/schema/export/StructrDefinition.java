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
package org.structr.schema.export;

import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonParameter;
import org.structr.core.traits.Traits;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 *
 */
public interface StructrDefinition {

	StructrDefinition resolveJsonPointerKey(final String key);

	default String[] listToArray(final Collection<String> list) {
		return list.toArray(new String[0]);
	}

	default Field getFieldOrNull(final Traits type, final String fieldName) {

		/*
		if (type == null) {
			return null;
		}

		try {

			return type.getField(fieldName);

		} catch (Throwable ignore) {}
		*/

		return null;
	}

	default Method getMethodOrNull(final Traits type, final JsonMethod method) {

		/*
		if (type == null) {
			return null;
		}

		try {

			final List<Class> types = new ArrayList<>();
			final String name       = method.getName();

			for (final JsonParameter parameter : method.getParameters()) {

				String parameterType = parameter.getType();

				// remove generics from type spec
				if (parameterType.contains("<")) {
					parameterType = parameterType.substring(0, parameterType.indexOf("<"));
				}

				switch (parameterType) {

					case "boolean":
						types.add(Boolean.TYPE);
						break;

					case "float":
						types.add(Float.TYPE);
						break;

					case "double":
						types.add(Double.TYPE);
						break;

					case "int":
						types.add(Integer.TYPE);
						break;

					case "long":
						types.add(Long.TYPE);
						break;

					case "String":
						types.add(String.class);
						break;

					default:
						types.add(Class.forName(parameterType));
						break;
				}

			}

			return type.getMethod(name);

		} catch (NoSuchMethodException nsmex) {
		} catch (Throwable ignore) {
			ignore.printStackTrace();
		}
		*/

		return null;
	}
}





























