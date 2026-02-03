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
package org.structr.web.function;

import org.structr.common.SecurityContext;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ToGraphObjectFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "toGraphObject";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("obj");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 3) {

			try {

				final SecurityContext securityContext = ctx.getSecurityContext();

				final Value<String> view = new StaticValue<>("public");
				if (sources.length > 1) {
					view.set(securityContext, sources[1].toString());
				}

				int outputDepth = 3;
				if (sources.length > 2 && sources[2] instanceof Number) {
					outputDepth = ((Number)sources[2]).intValue();
				}

				final Object converted = UiFunction.toGraphObject(sources[0], outputDepth);

				if (converted != null) {
					return converted;
				}

			} catch (Throwable t) {

				logException(caller, t, sources);
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${toGraphObject(obj)}"),
			Usage.javaScript("Usage: ${{ $.toGraphObject(obj) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Converts the given entity to GraphObjectMap.";
	}

	@Override
	public String getLongDescription() {
		return """
		Tries to convert given object or collection containing graph objects into a graph object. 
		If an element in the source can not be converted to a graph object, it is ignored. 
		Graph objects can be used in repeaters for example and thus it can be useful to create custom graph 
		objects for iteration in such contexts. The optional `view` parameter can be used to select the view 
		representation of the entity. If no view is given, the `public` view is used. The optional `depth` 
		parameter defines at which depth the conversion stops. If no depth is given, the default value of 3 is used.""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
						${{
							let coll = $.toGraphObject([
								{id:"o1",name:"objectA"},
								{id:"o2",name:"objectB"}
							]);
							$.print(coll.join(', '));
						}}
						> {id=o1, name=objectA}, {id=o2, name=objectB}
						"""),
				Example.structrScript("""
				${toGraphObject(inheritingTypes('Principal'))}
				> [{value=Principal},{value=Group},{value=LDAPGroup},{value=LDAPUser},{value=User}]
				""")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("source", "object or collection"),
				Parameter.optional("view", "view (default: `public`)"),
				Parameter.optional("depth", "conversion depth (default: 3)")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Since strings can not be converted to graph objects but it can be desirable to use collections of strings in repeaters (e.g. the return value of the `inheriting_types()` function), collections of strings are treated specially and converted to graph objects with `value` => `<string>` as its result. (see example 2)"
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Conversion;
	}
}
