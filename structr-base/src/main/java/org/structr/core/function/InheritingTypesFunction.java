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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InheritingTypesFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "inheritingTypes";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type [, blacklist ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			if (!Traits.exists(typeName)) {

				throw new FrameworkException(422, new StringBuilder(getName()).append("(): Type '").append(typeName).append("' not found.").toString());
			}

			final Traits type               = Traits.of(typeName);
			final ArrayList inheritingTypes = new ArrayList();
			final List<String> blackList    = new ArrayList(Arrays.asList(typeName));

			if (sources.length == 2) {

				if (sources[1] instanceof List) {

					blackList.addAll((List)sources[1]);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Expected 'blacklist' parameter to be of type List.").toString());
				}
			}

			if (type != null) {

				if (type.isServiceClass() == false) {

					inheritingTypes.addAll(Traits.getAllTypes(value -> value.getAllTraits().contains(typeName)));
					inheritingTypes.removeAll(blackList);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Not applicable to service class '").append(type.getName()).append("'.").toString());
				}

			} else {

				logger.warn("{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}

			return inheritingTypes;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${inheritingTypes(type[, blacklist])}. Example ${inheritingTypes('User')}"),
			Usage.javaScript("Usage: ${Structr.inheritingTypes(type[, blacklist])}. Example ${Structr.inheritingTypes('User')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the list of subtypes of the given type **including** the type itself.";
	}

	@Override
	public String getLongDescription() {
		return "You can remove unwanted types from the resulting list by providing a list of unwanted type names as a second parameter.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("type", "type name to fetch subtypes for"),
			Parameter.optional("blacklist", "list of unwanted type names that are removed from the result")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${inheritingTypes('MyType', merge('UndesiredSubtype'))}", "Returns a list of subtypes of type \"MyType\"")
		);
	}
}
