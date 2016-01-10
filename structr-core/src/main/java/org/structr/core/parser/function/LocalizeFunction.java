/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Localization;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LocalizeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LOCALIZE    = "Usage: ${localize(key[, domain])}. Example ${localize('HELLO_WORLD', 'myDomain')}";
	public static final String ERROR_MESSAGE_LOCALIZE_JS = "Usage: ${{Structr.localize(key[, domain])}}. Example ${{Structr.localize('HELLO_WORLD', 'myDomain')}}";

	@Override
	public String getName() {
		return "localize()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			final SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance();
			Query query = StructrApp.getInstance(superUserSecurityContext).nodeQuery(Localization.class).and(Localization.locale, ctx.getLocale().toString()).and(Localization.name, sources[0].toString());
			List<Localization> localizations;

			if (sources.length == 2) {

				query.and(Localization.domain, sources[1].toString());

				localizations = query.getAsList();

				if (localizations.isEmpty()) {
					// no domain-specific localization found. fall back to no domain

					query = StructrApp.getInstance(superUserSecurityContext)
						.nodeQuery(Localization.class)
						.and(Localization.locale, ctx.getLocale().toString())
						.and(Localization.name, sources[0].toString())
						.blank(Localization.domain);

					localizations = query.getAsList();

				}

			} else {

				query.blank(Localization.domain);

				localizations = query.getAsList();

			}

			if (localizations.size() > 1) {
				// Ambiguous localization found

				if (sources.length > 1) {

					return "Ambiguous localization for key '" + sources[0] + "' and domain '" + sources[1] + "' found. Please fix.";

				} else {

					return "Ambiguous localization for key '" + sources[0] + "' found. Please fix.";

				}

			} else if (localizations.size() == 1) {
				// The desired outcome: Exactly one localization found

				return localizations.get(0).getProperty(Localization.localizedName);

			}

			// no localization found - return the key
			return sources[0];

		} else if (sources.length == 1 || sources.length == 2) {

			// silently ignore null values
			return "";

		} else {

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());

		}

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOCALIZE_JS : ERROR_MESSAGE_LOCALIZE);
	}

	@Override
	public String shortDescription() {
		return "";
	}

}
