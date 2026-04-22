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
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Collection;
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

			final String typeName        = sources[0].toString();
			final Traits type            = Traits.of(typeName);
			final List<String> blacklist = new ArrayList<>(List.of(typeName));

			if (type == null) {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, TypeInfoFunction.UNKNOWN_TYPE_ERROR_MESSAGE.formatted(getName(), typeName));
			}

			if (sources.length == 2) {

				if (sources[1] instanceof Collection<?> blCollection) {

					blCollection.forEach(blEntry -> blacklist.add(blEntry.toString()));

				} else {

					return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, AncestorTypesFunction.UNSUPPORTED_TYPE_PARAMETER_BLACKLIST.formatted(getName()));
				}
			}

			if (type.isServiceClass()) {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, AncestorTypesFunction.UNSUPPORTED_TYPE_PARAMETER_TYPE_SERVICE_CLASS.formatted(getName(), typeName));
			}

			final ArrayList<String> inheritingTypes = new ArrayList<>(Traits.getAllTypes(value -> value.getAllTraits().contains(typeName)));
			inheritingTypes.removeAll(blacklist);

			return inheritingTypes;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${inheritingTypes(type[, blacklist])}. Example ${inheritingTypes('User')}"),
			Usage.javaScript("Usage: ${{ $.inheritingTypes(type[, blacklist]) }}. Example ${{ $.inheritingTypes('User') }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the list of types that inherit the given trait.";
	}

	@Override
	public String getLongDescription() {
		return "You can remove unwanted types from the resulting list by providing a list of unwanted type names as a second parameter.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("type", "type name to fetch subtypes for"),
			Parameter.optional("blacklist", "collection of unwanted type names that are removed from the result")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${inheritingTypes('MyType', merge('UndesiredSubtype'))}", "Returns a list of subtypes of type \"MyType\"")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"If the requested type does not exist, a catchable error is produced (where applicable) and/or null will be returned.",
				"The types in the blacklist collection are not validated and are just removed from the result set.",
				"This function is not applicable to service classes and will produce a catchable error (where applicable) and/or null will be returned."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Schema;
	}
}
