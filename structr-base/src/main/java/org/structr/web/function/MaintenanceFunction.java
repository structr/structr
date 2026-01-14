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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.autocomplete.WrappingHint;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.Tx;
import org.structr.docs.*;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.schema.action.ActionContext;

import java.util.*;

public class MaintenanceFunction extends UiAdvancedFunction {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceFunction.class);

	@Override
	public String getName() {
		return "maintenance";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("command [, arguments...]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final SecurityContext securityContext = ctx.getSecurityContext();
			final Map<String, Object> params      = new LinkedHashMap<>();
			final String commandName              = (String)sources[0];

			if (sources.length > 1) {

				if (sources[1] instanceof Map) {

					params.putAll((Map)sources[1]);

				} else if (sources[1] instanceof GraphObjectMap) {

					params.putAll(((GraphObjectMap)sources[1]).toMap());

				} else {

					final int parameterCount = sources.length;

					if (parameterCount % 2 == 0) {

						throw new FrameworkException(400, "Invalid number of parameters: " + parameterCount + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
					}

					for (int c = 1; c < parameterCount; c += 2) {

						params.put(sources[c].toString(), sources[c + 1]);
					}
				}
			}

			if (securityContext.isSuperUser()) {

				final App app = StructrApp.getInstance(securityContext);

				// determine maintenance command from string
				final Class<Command> commandType = MaintenanceResource.getMaintenanceCommandClass(commandName);
				if (commandType != null) {

					final Command command = app.command(commandType);
					if (command instanceof MaintenanceCommand cmd) {

						RuntimeEventLog.maintenance(commandName, params);

						// flush caches if required
						if (cmd.requiresFlushingOfCaches()) {

							logger.info("Flushing caches before maintenance function.");

							try {

								app.command(FlushCachesCommand.class).execute(Collections.EMPTY_MAP);

							} catch (FrameworkException ex) {
								logger.warn("Flush caches failed before maintenance command {}: {}", commandName, ex.getMessage());
							}
						}

						if (cmd.requiresEnclosingTransaction()) {

							try (final Tx tx = app.tx()) {

								cmd.execute(params);

								tx.success();

								return cmd.getCommandResult();

							} catch (FrameworkException ex) {
								logger.warn("Unable to execute maintenance command {}: {}", commandName, ex.getMessage());
							}

						} else {

							try {

								cmd.execute(params);

								return cmd.getCommandResult();

							} catch (FrameworkException ex) {
								logger.warn("Unable to execute maintenance command {}: {}", commandName, ex.getMessage());
							}
						}

					} else {

						logger.error("{} is not a maintenance command, cannot execute using maintenance()", commandName);
					}

				} else {

					logger.error("Cannot execute maintenance command {}, command doesn't exist", commandName);
				}

			} else {

				logger.error("Cannot execute maintenance command {} with non-admin user {}", commandName, securityContext.getUser(false));
			}

			return null;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${maintenance(command [, key, value [, ... ]])}. Example: ${maintenance('rebuildIndex', 'mode', 'nodesOnly'))')}"),
			Usage.javaScript("Usage: ${{Structr.maintenance(command [, key, value [, ... ]])}}. Example: ${{Structr.maintenance('rebuildIndex', { mode: 'nodesOnly' })}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Allows an admin user to execute a maintenance command from within a scripting context.";
	}

	@Override
	public String getLongDescription() {

		final List<String> lines = new LinkedList<>();

		lines.add("The following maintenance commands exist:");
		lines.add("");
		lines.add("|Name|Description|");
		lines.add("|---|---|");

		for (final Documentable cmd : MaintenanceResource.getMaintenanceCommands()) {

			if (!DocumentableType.Hidden.equals(cmd.getDocumentableType())) {

				lines.add("|" + cmd.getName() + "|" + cmd.getShortDescription() + "|");
			}
		}

		return StringUtils.join(lines, "\n");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("command", "name of the command to execute"),
			Parameter.optional("arguments...", "map (or key-value pairs) of arguments (depends on the command)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			${{
			    $.maintenance('rebuildIndex', { type: 'Article' });
			}}
			""", "Rebuild the index for the type \"Article\"")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"In a StructrScript environment arguments are passed as pairs of 'key1', 'value1'.",
			"In a JavaScript environment, this function takes a map as the second argument."
		);
	}

	public List<Documentable> getContextHints(final String lastToken) {

		final List<Documentable> hints = new LinkedList<>();
		
		if (lastToken == null) {
			return hints;
		}

		final String quoteChar         = lastToken.startsWith("'") ? "'" : lastToken.startsWith("\"") ? "\"" : "'";

		for (final Documentable documentable : MaintenanceResource.getMaintenanceCommands()) {

			if (!DocumentableType.Hidden.equals(documentable.getDocumentableType())) {

				final String name = documentable.getName();

				if (StringUtils.isNotBlank(name)) {

					hints.add(new WrappingHint(documentable, quoteChar + name + quoteChar));
				}
			}
		}

		return hints;
	}
}
