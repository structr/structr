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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
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
		return "execBinary";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("outputStream, scriptConfigKey [, parameters [, logBehaviour ] ]");
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
			Usage.structrScript("Usage: ${execBinary(outputStream, scriptConfigKey [, parameters [, logBehaviour ] ])}. Example: ${exec(response, 'my-script', merge('param1', 'param2'), 1)}"),
			Usage.javaScript("Usage: ${{ $.execBinary(outputStream, scriptConfigKey [, parameters [, logBehaviour ] ]}}. Example: ${{ $.exec($.response, 'my-script', ['param1', { value: 'CLIENT_SECRET', mask: true }], 2); }}")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Scripts are executed using `/bin/sh` - thus this function is only supported in environments where this exists.",
				"All script files are looked up inside the `scripts` folder in the main folder of the installation (not in the files area).",
				"Symlinks are not allowed, director traversal is not allowed.",
				"The key of the script must be all-lowercase.",
				"The script must be executable (`chmod +x`)",
				"The first parameter is usually the builtin keyword `response` and this function is usually used in a page context.",
				"A page using this should have the correct content-type and have the `pageCreatesRawData` flag enabled",
				"Caution: Supplying unvalidated user input to this command may introduce security vulnerabilities.",
				"All parameters are automatically put in double-quotes",
				"All parameters can be passed as a string or as an object containing a `value` field and a `mask` flag.",
				"Double-quotes in parameter values are automatically escaped as `\\\"`"
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("outputStream", "output stream to write the output to"),
				Parameter.mandatory("scriptConfigKey", "configuration key used to resolve the script's filename."),
				Parameter.optional("parameters", "collection of script parameters, each either a raw string or an object containing a `value` field and a `mask` flag."),
				Parameter.optional("logBehaviour", (
								"Specifies the function's call-logging behavior:"
										+ "<p>`0`: skip logging the command line<br>"
										+ "`1`: log only the script's full path<br>"
										+ "`2`: log the script path and all parameters, applying masking as configured</p>"
										+ "The default for this can be set via `%s`.").formatted(Settings.LogScriptProcessCommandLine.getKey()
						)
				)
		);
	}

	@Override
	public String getShortDescription() {
		return "Executes a script returning the returning the raw output directly into the output stream.";
	}

	@Override
	public String getLongDescription() {
		return """
			This method is very similar to `exec()`, but instead of returning the (text) result of the execution, it will copy its input stream to the given output buffer **without modifying the binary data**.
			
			This is important to allow streaming of binary data from a script to the client.
			
			If a page is used to serve binary data, it must have the correct content-type and have the `pageCreatesRawData` flag enabled.
			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${execBinary(response, 'my.create.pdf')}", "Streaming binary content of the `my.create.pdf` script to the client.")
		);
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

				logger.warn("execBinary(): Critical: Expected attribute 'value' to be non-null for parameter in map-representation (ex.: { value: \"myParameter\", mask: true })");

				throw new ArgumentNullException();
			}

			if (maskParameter == null) {

				logger.info("execBinary(): Expected 'mask' attribute to be non-null for parameter in map-representation (ex.: { value: \"myParameter\", mask: true }). Assuming 'mask = false'");
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
