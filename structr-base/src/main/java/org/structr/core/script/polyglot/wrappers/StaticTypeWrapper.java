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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import org.structr.core.api.MethodCall;
import org.structr.core.api.MethodSignature;
import org.structr.core.api.Methods;
import org.structr.schema.action.EvaluationHints;

public class StaticTypeWrapper implements ProxyObject {

	private final static Logger logger = LoggerFactory.getLogger(StaticTypeWrapper.class);
	private final Class referencedClass;
	private final ActionContext actionContext;

	public StaticTypeWrapper(final ActionContext actionContext, final Class referencedClass) {

		this.actionContext   = actionContext;
		this.referencedClass = referencedClass;
	}

	@Override
	public Object getMember(String key) {

		final MethodSignature signature = Methods.getMethodSignatureOrNull(referencedClass, null, key);
		if (signature != null && signature.isStatic()) {

			final ProxyExecutable executable = arguments -> {

				try {

					final MethodCall call = signature.createCall(arguments);

					return PolyglotWrapper.wrap(actionContext, call.execute(actionContext.getSecurityContext(), new EvaluationHints()));

				} catch (FrameworkException ex) {
					throw new RuntimeException(ex);
				}
			};

			return executable;
		}

		return null;
	}

	@Override
	public Object getMemberKeys() {
		return null;
	}

	@Override
	public boolean hasMember(String key) {
		return true;
	}

	@Override
	public void putMember(String key, Value value) {
	}
}
