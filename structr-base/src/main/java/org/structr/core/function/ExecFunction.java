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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_EXEC    = "Usage: ${exec(scriptConfigKey [, parameterArray [, logBehaviour ] ])} or ${exec(scriptConfigKey [, parameters... ])}. Example 1: ${exec('my-script', ['param1', 'param2'], 1)}. Example 2: ${exec('my-script', 'param1', 'param2')}";
	public static final String ERROR_MESSAGE_EXEC_JS = "Usage: ${{ $.exec(scriptConfigKey  [, parameterArray [, logBehaviour ] ]); }} or ${{ $.exec(scriptConfigKey [, parameters... ]); }}. Example 1: ${{ $.exec('my-script', ['param1', { value: 'CLIENT_SECRET', masked: true }], 1); }}. Example 2: ${{ $.exec('my-script', 'param1', 'param2'); }}";

	public static final String SCRIPTS_FOLDER = "scripts";

	@Override
	public String getName() {
		return "exec";
	}

	@Override
	public String getSignature() {
		return "scriptConfigKey  [, parameterArray [, logBehaviour ] ]";
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

					final ScriptingProcess scriptingProcess = new ScriptingProcess(ctx.getSecurityContext(), absolutePath);

					if (sources.length > 1) {

						final boolean isNewCallSignature = (sources[1] instanceof Collection<?>);

						if (!isNewCallSignature) {

							// use old behaviour
							for (int i = 1; i < sources.length; i++) {

								if (sources[i] != null) {

									scriptingProcess.addParameter(sources[i].toString());
								}
							}

						} else {

							if (sources.length > 2) {

								if (sources[2] instanceof Number) {

									final int param3 = ((Number)sources[2]).intValue();

									scriptingProcess.setLogBehaviour(param3);

								} else {

									logger.warn("{}(): If using a collection of parameters as second argument, the third argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", sources[2]);
								}
							}

							final Collection params = (Collection)sources[1];

							for (final Object p : params) {

								if (p instanceof Map<?,?>) {

									// must be Map { value: xxx, mask: bool }
									scriptingProcess.addParameter((Map)p);

								} else {

									scriptingProcess.addParameter(p.toString());
								}
							}
						}
					}

					final ExecutorService executorService = Executors.newSingleThreadExecutor();

					try {

						return executorService.submit(scriptingProcess).get();

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
		return "Executes a script configured in structr.conf with the given script name and parameters, returning the output. Parameters can either be given as a list of function parameters or as a collection of parameters. If a collection of parameters is provided as second parameter, the third parameter configures the logging behaviour for the command line (0: do not log command line, 1: log only full path to script, 2: log path to script and each parameter either unmasked or masked)";
	}

	private static class ScriptingProcess extends AbstractProcess<String> {

		private static final String MASK_STRING = "***";

		private final StringBuilder cmdLineBuilder = new StringBuilder();
		private final StringBuilder logLineBuilder = new StringBuilder();

		public ScriptingProcess(final SecurityContext securityContext, final String scriptName) {

			super(securityContext);

			this.cmdLineBuilder.append(scriptName);
			this.logLineBuilder.append(scriptName);
		}

		@Override
		public StringBuilder getCommandLine() {
			return cmdLineBuilder;
		}

		@Override
		public StringBuilder getLogLine() {
			return logLineBuilder;
		}

		@Override
		public String processExited(final int exitCode) {
			return outputStream();
		}

		private void addParameter(final String parameter) {
			this.addParameter(parameter, false);
		}

		private void addParameter(final Map parameterConfig) {

			final Object parameter = parameterConfig.get("value");

			if (parameter == null) {

				throw new ArgumentNullException();
			}

			this.addParameter(parameter.toString(), Boolean.TRUE.equals(parameterConfig.get("mask")));
		}

		private void addParameter(final String parameter, final boolean maskInLog) {

			this.cmdLineBuilder.append(" ").append(parameter);

			if (this.getLogBehaviour() == Settings.EXEC_FUNCTION_LOG_STYLE.CUSTOM) {

				this.logLineBuilder.append(" ").append(maskInLog ? MASK_STRING : parameter);
			}
		}

		@Override
		public void preprocess() {
		}
	}
}
