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
import org.structr.core.property.PropertyKey;
import org.structr.docs.Language;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MergePropertiesFunction extends CoreFunction {

	@Override
	public String getName() {
		return "merge_properties";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("source, target, keys");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof GraphObject && sources[1] instanceof GraphObject) {

				final Set<PropertyKey> mergeKeys = new LinkedHashSet<>();
				final GraphObject source         = (GraphObject)sources[0];
				final GraphObject target         = (GraphObject)sources[1];
				final int paramCount             = sources.length;

				for (int i = 2; i < paramCount; i++) {

					final String keyName  = sources[i].toString();
					final PropertyKey key = target.getTraits().key(keyName);

					mergeKeys.add(key);
				}

				for (final PropertyKey key : mergeKeys) {

					final Object sourceValue = source.getProperty(key);
					if (sourceValue != null) {

						target.setProperty(key, sourceValue);
					}
				}

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${merge_properties(source, target , mergeKeys...)}. Example: ${merge_properties(this, parent, \"eMail\")}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Copies property values from source entity to target entity, using the given list of keys.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
