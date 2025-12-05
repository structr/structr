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

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class IsValidUuidFunction extends CoreFunction {

	@Override
	public String getName() {
		return "isValidUuid";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources.length >= 1 && sources[0] instanceof String potentialUuid) {

			return Settings.isValidUuid(potentialUuid);
		}

		return false;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${isValidUuid(string)}. Example: ${isValidUuid(retrieve('requestParameterId'))}"),
			Usage.javaScript("Usage: ${{ $.isValidUuid(string); }}. Example: ${{ $.isValidUuid(retrieve('requestParameterId')); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Tests if a given string is a valid UUID.";
	}

	@Override
	public String getLongDescription() {
		return "Returns true if the provided string is a valid UUID according to the configuration (see `%s`). Returns false otherwise, including when the argument is not a string.".formatted(Settings.UUIDv4AllowedFormats.getKey());
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("string", "Input string to be evaluated as a valid UUID")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
					${{
						let uuid = $.request.nodeId;

						if ($.isValidUuid(uuid)) {

							let node = $.find('MyNodeType', uuid);

							if ($.empty(node)) {

								// process further

							} else {

								return 'Invalid parameter!';
							}

						} else {

							return 'Invalid parameter!';
						}
					}}
					""", "Validate user input to prevent errors")
		);
	}
}
