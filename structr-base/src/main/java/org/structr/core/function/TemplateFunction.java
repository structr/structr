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
import org.structr.schema.action.ActionContext;

import java.util.List;

public class TemplateFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_TEMPLATE    = "Usage: ${template(name, locale, source)}. Example: ${template(\"TEXT_TEMPLATE_1\", \"en_EN\", this)}";
	public static final String ERROR_MESSAGE_TEMPLATE_JS = "Usage: ${{Structr.template(name, locale, source)}}. Example: ${{Structr.template(\"TEXT_TEMPLATE_1\", \"en_EN\", Structr.get('this'))}}";

	@Override
	public String getName() {
		return "template";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("name, locale, entity");
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_TEMPLATE_JS : ERROR_MESSAGE_TEMPLATE);
	}

	@Override
	public String getShortDescription() {
		return "Returns a MailTemplate object with the given name, replaces the placeholders with values from the given entity";
	}
}
