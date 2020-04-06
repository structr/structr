/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.script;

import org.structr.core.GraphObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class StructrPolyglotGraphObjectWrapper {

	public static GraphObject getProxy(GraphObject graphObject) {

		return (GraphObject)Proxy.newProxyInstance(GraphObject.class.getClassLoader(), new Class[] {GraphObject.class}, new GraphObjectInvocationHandler(graphObject) );
	}

	protected static class GraphObjectInvocationHandler implements InvocationHandler {
		private final GraphObject graphObject;

		public GraphObjectInvocationHandler(final GraphObject graphObject) {

			this.graphObject = graphObject;
		}

		public GraphObject getOriginalObject() {

			return this.graphObject;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			return method.invoke(graphObject, args);
		}
	}

}
