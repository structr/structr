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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AncestorTypesFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "ancestorTypes";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			if (!Traits.exists(typeName)) {

				throw new FrameworkException(422, new StringBuilder(getName()).append("(): Type '").append(typeName).append("' not found.").toString());
			}

			final Traits type                     = Traits.of(typeName);
			final ArrayList<String> ancestorTypes = new ArrayList();
			final List<String> blackList          = new ArrayList(Arrays.asList(typeName, StructrTraits.NODE_INTERFACE, StructrTraits.PROPERTY_CONTAINER, StructrTraits.GRAPH_OBJECT, StructrTraits.ACCESS_CONTROLLABLE));

			if (sources.length == 2) {

				if (sources[1] instanceof List) {

					blackList.addAll((List)sources[1]);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Expected 'blacklist' parameter to be of type List.").toString());
				}
			}

			if (type != null) {

				if (type.isServiceClass() == false) {

					ancestorTypes.addAll(type.getAllTraits());
					ancestorTypes.removeAll(blackList);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Not applicable to service class '").append(type.getName()).append("'.").toString());
				}

			} else {

				logger.warn("{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}

			return ancestorTypes;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String getShortDescription() {
		return "Returns the list of parent types of the given type **including** the type itself.";
	}

	@Override
	public String getLongDescription() {
		return "The blacklist of type names can be extended by passing a list as the second parameter. If omitted, the function uses the following blacklist: [AccessControllable, GraphObject, NodeInterface, PropertyContainer].";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("type", "type to fetch ancestor types for"),
			Parameter.optional("blacklist", "blacklist to remove unwanted types from result")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${ancestorTypes('MyType')}", "Return all ancestor types of MyType"),
			Example.structrScript("${ancestorTypes('MyType', merge('MyOtherType))}", "Remove MyOtherType from the returned result")
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type [, blacklist ]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${ancestorTypes(type[, blacklist])}. Example ${ancestorTypes('User', merge('Principal'))}"),
			Usage.javaScript("Usage: ${{ $.ancestorTypes(type[, blacklist])}. Example ${{ $.ancestorTypes('User', ['Principal']) }}")
		);
	}
}
