/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.util.AbstractProcess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_EXEC    = "Usage: ${exec(fileName [, parameters...]}. Example ${exec('my-script')}";
	public static final String ERROR_MESSAGE_EXEC_JS = "Usage: ${{Structr.exec(fileName [, parameters...]}}. Example ${{Structr.exec('my-script')}}";

	public static final String SCRIPTS_FOLDER = "scripts";

	@Override
	public String getName() {
		return "exec";
	}

	@Override
	public String getSignature() {
		return "scriptName [, parameterMap ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final String scriptKey              = sources[0].toString();
			final Setting<String> scriptSetting = Settings.getStringSetting(scriptKey);

			if (scriptSetting == null) {

				logger.warn("{}(): No script found for key '{}' in structr.conf, nothing executed.", getName(), scriptKey);

			} else if (!scriptSetting.isDynamic()) {

				logger.warn("{}(): Key '{}' in structr.conf is builtin. This is not allowed, nothing executed.", getName(), scriptKey);

			} else {

				final String scriptName              = scriptSetting.getValue();
				final Path scriptPath                = Paths.get(SCRIPTS_FOLDER.concat(File.separator).concat(scriptName));
				final String absolutePath            = scriptPath.toAbsolutePath().toString();
				final String canonicalPath           = scriptPath.toFile().getCanonicalPath();

				final boolean pathExists        = Files.exists(scriptPath);
				final boolean pathIsRegularFile = Files.isRegularFile(scriptPath, LinkOption.NOFOLLOW_LINKS);
				final boolean pathIsAllowed     = absolutePath.equals(canonicalPath);

				if (!pathExists) {

					logger.warn("{}(): No file found for script key '{}' = '{}' ({}), nothing executed.", getName(), scriptKey, scriptName, absolutePath);

				} else if (!pathIsRegularFile) {

					logger.warn("{}(): Script key '{}' = '{}' points to script file '{}' which is either not a file (or a symlink) and not allowed, nothing executed.", getName(), scriptKey, scriptName, absolutePath);

				} else if (!pathIsAllowed) {

					logger.warn("{}(): Script key '{}' = '{}' resolves to '{}' which seems to contain a directory traversal attack, nothing executed.", getName(), scriptKey, scriptName, absolutePath);

				} else {

					final StringBuilder scriptBuilder = new StringBuilder(absolutePath);
					if (sources.length > 1) {

						for (int i = 1; i < sources.length; i++) {
							if (sources[i] != null) {

								scriptBuilder.append(" ").append(sources[i].toString());
							}
						}
					}

					final ExecutorService executorService = Executors.newSingleThreadExecutor();
					final ScriptingProcess process        = new ScriptingProcess(ctx.getSecurityContext(), scriptBuilder.toString());

					try {

						return executorService.submit(process).get();

					} catch (InterruptedException | ExecutionException iex) {

						logException(caller, iex, sources);

					} finally {

						executorService.shutdown();
					}
				}
			}

		} catch (IOException ex) {

			Function.logException(logger, ex, "{}(): IOException encountered: {}", new Object[]{ getName(), ex.getMessage() });

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_EXEC_JS : ERROR_MESSAGE_EXEC);
	}

	@Override
	public String shortDescription() {
		return "Executes a script configured in structr.conf with the given script name and parameters, returning the output";
	}

	private static class ScriptingProcess extends AbstractProcess<String> {

		private final StringBuilder commandLine = new StringBuilder();

		public ScriptingProcess(final SecurityContext securityContext, final String commandLine) {

			super(securityContext);

			this.commandLine.append(commandLine);
		}

		@Override
		public StringBuilder getCommandLine() {
			return commandLine;
		}

		@Override
		public String processExited(final int exitCode) {
			return outputStream();
		}

		@Override
		public void preprocess() {
		}
	}
}
