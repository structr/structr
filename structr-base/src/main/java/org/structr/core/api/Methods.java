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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.Tx;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class Methods {

	private static final Map<String, CacheEntry> methodCache              = new LinkedHashMap<>();

	public static Map<String, AbstractMethod> getAllMethods(final Class type) {

		final Map<String, AbstractMethod> allMethods = new LinkedHashMap<>();

		if (type != null) {

			final Map<String, Method> methods = StructrApp.getConfiguration().getExportedMethodsForType(type);

			for (final Method method : methods.values()) {

				try {
					allMethods.put(method.getName(), Methods.createMethod(method));

				} catch
					(FrameworkException fex) {
					throw new RuntimeException(fex);
				}
			}

		} else {

			try {
				for (final SchemaMethod globalMethod : StructrApp.getInstance().nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).getResultStream()) {

					allMethods.put(globalMethod.getName(), new ScriptMethod(globalMethod));
				}

			} catch (FrameworkException fex) {
				throw new RuntimeException(fex);
			}
		}

		return allMethods;
	}

	public static AbstractMethod resolveMethod(final Class type, final String methodName) {

		// A method can either be a Java method, which we need to call with Method.invoke() via reflection,
		// OR a scripting method which will in turn call Actions.execute(), so we want do differentiate
		// between the two and use the appropriate calling method.

		if (methodName == null) {
			throw new RuntimeException(new FrameworkException(422, "Cannot resolve method without methodName!"));
		}

		// no type => global schema method!
		if (type == null) {

			CacheEntry cacheEntry = methodCache.get(methodName);
			if (cacheEntry == null) {

				cacheEntry = new CacheEntry();
				methodCache.put(methodName, cacheEntry);

				try (final Tx tx = StructrApp.getInstance().tx()) {

					final SchemaMethod method = StructrApp.getInstance().nodeQuery(SchemaMethod.class).andName(methodName).and(SchemaMethod.schemaNode, null).getFirst();
					if (method != null) {

						cacheEntry.method = new ScriptMethod(method);
					}

				} catch (FrameworkException fex) {
					throw new RuntimeException(fex);
				}
			}

			return cacheEntry.method;

		} else {

			final Map<String, Method> methods = StructrApp.getConfiguration().getExportedMethodsForType(type);
			final Method method               = methods.get(methodName);

			if (method != null) {

				try {
					return Methods.createMethod(method);

				} catch (FrameworkException fex) {
					throw new RuntimeException(fex);
				}
			}
		}

		return null;
	}

	public static void clearMethodCache() {
		methodCache.clear();
	}


	// ----- private static methods -----
	private static AbstractMethod createMethod(final Method method) throws FrameworkException {

		final Export exp     = method.getAnnotation(Export.class);
		final String id      = exp.schemaMethodId();

		if (StringUtils.isNotBlank(id)) {

			final String cacheId = id + "_structr_cache_entry";
			CacheEntry cacheEntry = methodCache.get(cacheId);
			if (cacheEntry == null) {

				cacheEntry = new CacheEntry();
				methodCache.put(cacheId, cacheEntry);

				try (final Tx tx = StructrApp.getInstance().tx()) {

					final SchemaMethod schemaMethod = StructrApp.getInstance().get(SchemaMethod.class, id);
					if (schemaMethod != null) {

						cacheEntry.method = new ScriptMethod(schemaMethod);
					}

					tx.success();
				}
			}

			return cacheEntry.method;
		}

		return new ReflectiveMethod(method);

	}

	/*
	@Override
	public final Object invokeMethod(final SecurityContext securityContext, final String methodName, final Map<String, Object> propertySet, final boolean throwExceptionForUnknownMethods, final EvaluationHints hints) throws FrameworkException {

		final Method method = StructrApp.getConfiguration().getExportedMethodsForType(entityType).get(methodName);
		if (method != null) {

			hints.reportExistingKey(methodName);

			return AbstractNode.invokeMethod(securityContext, method, this, propertySet, hints);
		}

		// in the case of REST access we want to know if the method exists or not
		if (throwExceptionForUnknownMethods) {
			throw new FrameworkException(400, "Method " + methodName + " not found in type " + getType());
		}

		return null;
	}

	public static Object invokeMethod(final SecurityContext securityContext, final Method method, final Object entity, final Map<String, Object> propertySet, final EvaluationHints hints) throws FrameworkException {

		System.out.println("########## METHODS: public static Object invokeMethod() with Method parameter and Map property set.");

		try {

			// new structure: first parameter is always securityContext and second parameter can be Map (for dynamically defined methods)
			if (method.getParameterTypes().length == 2 && method.getParameterTypes()[0].isAssignableFrom(SecurityContext.class) && method.getParameterTypes()[1].equals(Map.class)) {
				final Object[] args = new Object[] { securityContext };
				return method.invoke(entity, ArrayUtils.add(args, propertySet));
			}

			// second try: extracted parameter list
			final Object[] args = extractParameters(propertySet, method.getParameterTypes());

			return method.invoke(entity, ArrayUtils.add(args, 0, securityContext));

		} catch (InvocationTargetException itex) {

			final Throwable cause = itex.getCause();

			if (cause instanceof AssertException) {

				final AssertException e = (AssertException)cause;
				throw new FrameworkException(e.getStatus(), e.getMessage());
			}

			if (cause instanceof FrameworkException) {

				throw (FrameworkException) cause;
			}

		} catch (IllegalAccessException | IllegalArgumentException  t) {

			logger.warn("Unable to invoke method {}: {}", method.getName(), t.getMessage());
		}

		return null;
	}

	private static Object[] extractParameters(Map<String, Object> properties, Class[] parameterTypes) {

		final List<Object> values = new ArrayList<>(properties.values());
		final List<Object> parameters = new ArrayList<>();
		int index = 0;

		// only try to convert when both lists have equal size
		// subtract one because securityContext is default and not provided by user
		if (values.size() == (parameterTypes.length - 1)) {

			for (final Class parameterType : parameterTypes) {

				// skip securityContext
				if (!parameterType.isAssignableFrom(SecurityContext.class)) {

					final Object value = convert(values.get(index++), parameterType);
					if (value != null) {

						parameters.add(value);
					}
				}
			}
		}

		return parameters.toArray(new Object[0]);
	}

	// Tries to convert the given value into an object
	// of the given type, using an intermediate type
	// of String for the conversion.
	private static Object convert(Object value, Class type) {

		// short-circuit
		if (type.isAssignableFrom(value.getClass())) {
			return value;
		}

		Object convertedObject = null;

		if (type.equals(String.class)) {

			// strings can be returned immediately
			return value.toString();

		} else if (value instanceof Number) {

			Number number = (Number) value;

			if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
				return number.intValue();

			} else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
				return number.longValue();

			} else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
				return number.doubleValue();

			} else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
				return number.floatValue();

			} else if (type.equals(Short.class) || type.equals(Integer.TYPE)) {
				return number.shortValue();

			} else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
				return number.byteValue();

			}

		} else if (value instanceof List) {

			return value;

		} else if (value instanceof Map) {

			return value;

		} else if (value instanceof Boolean) {

			return value;

		}

		// fallback
		try {

			Method valueOf = type.getMethod("valueOf", String.class);
			if (valueOf != null) {

				convertedObject = valueOf.invoke(null, value.toString());

			} else {

				logger.warn("Unable to find static valueOf method for type {}", type);
			}

		} catch (Throwable t) {

			logger.warn("Unable to deserialize value {} of type {}, Class has no static valueOf method.", new Object[]{value, type});
		}

		return convertedObject;
	}
	*/

	private static class CacheEntry {
		public AbstractMethod method = null;
	}
}
