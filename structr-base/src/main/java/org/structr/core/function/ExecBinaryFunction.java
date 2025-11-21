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
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.util.AbstractBinaryProcess;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecBinaryFunction extends ExecFunction {

	@Override
	public String getName() {
		return "exec_binary";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("outputStream, scriptConfigKey [, parameterCollection [, logBehaviour ] ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String sanityCheckedAbsolutePathOrNull = getSanityCheckedPathForScriptSetting(sources[1].toString());

			if (sanityCheckedAbsolutePathOrNull != null) {

				final OutputStream out                  = (OutputStream)sources[0];
				final ScriptingProcess scriptingProcess = new ScriptingProcess(ctx.getSecurityContext(), sanityCheckedAbsolutePathOrNull, out);

				if (sources.length > 2) {

					final boolean isNewCallSignature = (sources[2] instanceof Collection<?>);

					if (!isNewCallSignature) {

						logger.warn("{}(): Deprecation Warning: The call signature for this method has changed. The old signature of providing all arguments to the script is still supported but will be removed in a future version. Please consider upgrading to the new signature: {}", getName(), getSignature());

						for (int i = 2; i < sources.length; i++) {

							if (sources[i] != null) {

								scriptingProcess.addParameter(sources[i].toString());
							}
						}

					} else {

						if (sources.length > 3) {

							if (sources[3] instanceof Number) {

								final int logBehavior = ((Number)sources[3]).intValue();

								scriptingProcess.setLogBehaviour(logBehavior);

							} else {

								logger.warn("{}(): If using a collection of parameters as third argument, the fourth argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", getName(), sources[2]);
							}
						}

						final Collection params = (Collection)sources[2];

						for (final Object param : params) {

							if (param instanceof Map<?,?>) {

								scriptingProcess.addParameter((Map)param);

							} else {

								if (param == null) {

									throw new ArgumentNullException();
								}

								scriptingProcess.addParameter(param.toString());
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${exec_binary(outputStream, scriptConfigKey [, parameterCollection [, logBehaviour ] ])}. Example: ${exec(response, 'my-script', merge('param1', 'param2'), 1)}"),
			Usage.javaScript("Usage: ${{Structr.exec_binary(outputStream, scriptConfigKey [, parameterCollection [, logBehaviour ] ]}}. Example: ${{ $.exec($.response, 'my-script', ['param1', { value: 'CLIENT_SECRET', mask: true }], 2); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Executes a script configured in structr.conf with the given configuration key, a collection of parameters and the desired logging behaviour, returning the raw output directly into the output stream. The logging behaviour for the command line has three possible values: [0] do not log command line [1] log only full path to script [2] log path to script and each parameter either unmasked or masked. In JavaScript the function is most flexible - each parameter can be given as a simple string or as a configuration map with a 'value' and a 'mask' flag.";
	}

	private static class ScriptingProcess extends AbstractBinaryProcess<String> {

		private static final String MASK_STRING = "***";

		private final StringBuilder cmdLineBuilder = new StringBuilder();
		private final StringBuilder logLineBuilder = new StringBuilder();

		public ScriptingProcess(final SecurityContext securityContext, final String scriptName, final OutputStream out) {

			super(securityContext, out);

			// put quotes around full path for script to allow for spaces etc.
			this.cmdLineBuilder.append("\"" + scriptName + "\"");

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
			return errorStream();
		}

		private void addParameter(final String parameter) {
			this.addParameter(parameter, false);
		}

		private void addParameter(final Map parameterConfig) {

			final Object parameter     = parameterConfig.get("value");
			final Object maskParameter = parameterConfig.get("mask");

			if (parameter == null) {

				logger.warn("exec_binary(): Critical: Expected attribute 'value' to be non-null for parameter in map-representation (ex.: { value: \"myParameter\", mask: true })");

				throw new ArgumentNullException();
			}

			if (maskParameter == null) {

				logger.info("exec_binary(): Expected 'mask' attribute to be non-null for parameter in map-representation (ex.: { value: \"myParameter\", mask: true }). Assuming 'mask = false'");
			}

			this.addParameter(parameter.toString(), Boolean.TRUE.equals(maskParameter));
		}

		private void addParameter(final String parameter, final boolean maskInLog) {

			// put quotes around full path for script to allow for spaces etc.
			// also escape quotes in the parameter to not break the quoting
			final String safeParam = "\"" + parameter.replaceAll("\"", "\\\\\"") + "\"";

			this.cmdLineBuilder.append(" ").append(safeParam);

			if (this.getLogBehaviour() == Settings.SCRIPT_PROCESS_LOG_STYLE.CUSTOM) {

				this.logLineBuilder.append(" ").append(maskInLog ? MASK_STRING : safeParam);
			}
		}

		@Override
		public void preprocess() {
		}
	}
}
