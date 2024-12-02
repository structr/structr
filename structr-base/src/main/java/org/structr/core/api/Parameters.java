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
package org.structr.core.api;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;

/**
 * Base class for parameters that can be defined by Method implementations.
 */
public class Parameters extends LinkedHashMap<String, String> {

	private boolean hasSecurityContextAndMapParameters = false;
	private boolean requiredSecurityContextFirst      = false;

	public String formatForErrorMessage() {

		final List<String> elements = new LinkedList<>();

		for (final String type : values()) {

			// do not include internal SecurityContext parameter in error message
			if (!"SecurityContext".equals(type)) {

				elements.add(type);
			}
		}

		return StringUtils.join(elements);
	}

	public boolean requiresSecurityContextAsFirstArgument() {
		return requiredSecurityContextFirst;
	}

	public void setRequiresSecurityContextAsFirstArgument(final boolean value) {
		requiredSecurityContextFirst = value;
	}

	public boolean hasSecurityContextAndMapParameters() {
		return hasSecurityContextAndMapParameters;
	}

	public void setHasExportedMethodDefaultSignature(final boolean value) {
		hasSecurityContextAndMapParameters = value;
	}

	public String getNameByIndex(final int index) {
		return Iterables.nth(this.keySet(), index);
	}

	public String getTypeByNameOrIndex(final String name, final int index) {

		if (name != null) {

			final String value = this.get(name);
			if (value != null) {

				return value;
	}
		}

		// fallback: get by index
		return Iterables.nth(this.values(), index);
	}

	// ----- public static methods -----
	public static Parameters fromMethod(final Method method) {

		final Parameters parameters = new Parameters();
		int index                   = 0;

		for (final java.lang.reflect.Parameter p : method.getParameters()) {

			final Class nonPrimitiveType = ClassUtils.primitiveToWrapper(p.getType());

			if (nonPrimitiveType.isAssignableFrom(SecurityContext.class) && index == 0) {
				parameters.setRequiresSecurityContextAsFirstArgument(true);
			}

			// default export method signature detected, store this info for later use
			if (parameters.requiresSecurityContextAsFirstArgument() && nonPrimitiveType.isAssignableFrom(Map.class) && index == 1) {
				parameters.setHasExportedMethodDefaultSignature(true);
			}

			// Convert primitive types to corresponding object type to avoid int != Integer
			parameters.put(p.getName(), nonPrimitiveType.getSimpleName());

			index++;
		}

		return parameters;
	}

	public static Parameters fromSchemaMethod(final SchemaMethod method) {

		final Parameters parameters = new Parameters();

		for (final SchemaMethodParameter p : method.getParameters()) {

			parameters.put(p.getName(), p.getParameterType());
		}

		return parameters;
	}
}
