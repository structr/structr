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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.docs.Example;
import org.structr.docs.Parameter;

import java.util.List;

public class TemplateFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "template";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name, locale, entity");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources != null && sources.length != 3) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			if (sources[2] instanceof GraphObject) {

				final Traits traits                 = Traits.of(StructrTraits.MAIL_TEMPLATE);
				final PropertyKey<String> localeKey = traits.key(MailTemplateTraitDefinition.LOCALE_PROPERTY);
				final PropertyKey<String> textKey   = traits.key(MailTemplateTraitDefinition.TEXT_PROPERTY);
				final App app                       = StructrApp.getInstance(ctx.getSecurityContext());
				final String name                   = sources[0].toString();
				final String locale                 = sources[1].toString();
				final GraphObject template          = app.nodeQuery(StructrTraits.MAIL_TEMPLATE).name(name).key(localeKey, locale).getFirst();
				final GraphObject templateInstance  = (GraphObject)sources[2];

				if (template != null) {

					final String text = template.getProperty(textKey);
					if (text != null) {

						// recursive replacement call, be careful here
						return Scripting.replaceVariables(ctx, templateInstance, text, "template()");
					}

				} else {

					logger.warn("No MailTemplate found for parameters: {}", getParametersAsString(sources));
				}

				return "";
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${template(name, locale, source)}."),
			Usage.javaScript("Usage: ${{ $.template(name, locale, source)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a MailTemplate object with the given name, replaces the placeholders with values from the given entity.";
	}

	@Override
	public String getLongDescription() {
		return "Loads a node of type `MailTemplate` with the given name and locale values and uses the given source entity to resolve template expressions in the content field of the loaded node, returning the resulting text.";
	}


	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${template('TEXT_TEMPLATE_1', 'en', this)}", "Passing the Structr me object, representing the current user"),
				Example.javaScript("${{ return $.template('TEXT_TEMPLATE_1', 'en', $.this)}}"),
				Example.javaScript("""
						${{
						    return $.template('MAIL_SUBJECT', 'de', $.toGraphObject({name: "Mr. Foo"}))
						}}
						""", "passing an arbitrary JavaScript object")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("name", "Mail-Template name"),
				Parameter.mandatory("locale", "Mail-Template locale"),
				Parameter.mandatory("source", "source entity for given expressions")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Short example for mail-template: `Welcome, ${this.name}!`",
				"This function is quite similar to the `replace()` function which serves a similar purpose but works on any string rather than on a mail template.",
				"The third parameter 'source' expects a node or relationship object fetched from the database. If the third parameter is an arbitrary design JavaScript object, it has to be wrapped with the `toGraphObject()` function, before being passed as the parameter."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}
}
