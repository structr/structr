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

import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.util.AbstractBinaryProcess;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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

		final List<Signature> signatures = new ArrayList<>(Signature.forAllScriptingLanguages("output, scriptConfigKey [, parameters [, logBehaviour [, input ] ] ]"));

		signatures.addAll(Signature.forAllScriptingLanguages("output, scriptConfigKey, parameter1 [, parameter2, ... ]"));

		return signatures;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			// logBehaviour and input may be null, to pass an input with the default log behaviour
			assertMandatoryArgumentsNotNull(sources, 2, 3);

			final String sanityCheckedAbsolutePathOrNull = getSanityCheckedPathForScriptSetting(sources[1].toString());
			if (sanityCheckedAbsolutePathOrNull != null) {

				final OutputStream out;

				if (sources[0] instanceof AbstractNode node && node.is(StructrTraits.FILE)) {

					final File f = node.as(File.class);
					out = f.getOutputStream();

				} else if (sources[0] instanceof OutputStream) {

					out = (OutputStream)sources[0];

				} else {

					if (ctx.supportsExceptionHandling()) {

						throw new FrameworkException(422, "%s: Unable to execute: 'output' parameter is not a file or an OutputStream: %s".formatted(getName(), sources[0]));

					} else {

						logger.warn("{}(): Unable to execute: 'output' parameter is not a file or an OutputStream: {}.", getName(), sources[0]);

						return null;
					}
				}

				final ScriptingProcess scriptingProcess = new ScriptingProcess(ctx.getSecurityContext(), sanityCheckedAbsolutePathOrNull, out);

				if (sources.length > 2) {

					// two equal modes: the parameters as one collection, followed by logBehaviour and input, or each parameter as an argument of its own
					final boolean parametersAsCollection = (sources[2] instanceof Collection<?>);
					if (!parametersAsCollection) {

						for (int i = 2; i < sources.length; i++) {

							scriptingProcess.addParameter(sources[i]);
						}

					} else {

						if (sources.length > 3) {

							if (sources[3] instanceof Number) {

								final int logBehavior = ((Number)sources[3]).intValue();

								scriptingProcess.setLogBehaviour(logBehavior);

							} else if (sources[3] != null) {

								logger.warn("{}(): If using a collection of parameters as third argument, the fourth argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", getName(), sources[3]);
							}
						}

						if (sources.length > 4) {

							scriptingProcess.setInput(toInput(sources[4]));
						}

						final Collection params = (Collection)sources[2];

						for (final Object param : params) {

							scriptingProcess.addParameter(param);
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

					// a file's stream is opened here, so it is closed here too, which also stores the content
					if (sources[0] instanceof AbstractNode) {

						out.close();
					}
				}
			}

		} catch (IOException ex) {

			Function.logException(logger, ex, "{}(): IOException encountered: {}", getName(), ex.getMessage());

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage());

			return null;
		}

		return null;
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"Scripts are executed directly, without a shell in between: a script needs a shebang line (e.g. `#!/bin/sh`), otherwise it is run with `/bin/sh`.",
				"All script files are looked up inside the `scripts` folder in the main folder of the installation (not in the files area).",
				"Symlinks are are allowed, if `scripts.path.allowsymboliclinks` is enabled.",
				"Directory traversal are allowed, if `scripts.path.allowpathtraversal` is enabled.",
				"The key of the script must be all-lowercase.",
				"The script must be executable (`chmod +x`)",
				"The first parameter can be a file or `response` which streams the output to the HTTP response and is only possible in a page context.",
				"A page using this should have the correct content-type and have the `pageCreatesRawData` flag enabled",
				"Caution: Supplying unvalidated user input to this command may introduce security vulnerabilities.",
				"Secrets like passwords belong in `input`, not in `parameters`: parameters are command-line arguments, which every user on the host can see with `ps` while the script runs. The script reads its input from stdin, e.g. with `read -r PASSWORD`.",
				"Every parameter is passed to the script as one argument, exactly as given: quotes, spaces, `$(...)` or backticks in a value are not interpreted.",
				"All parameters can be passed as a string or as an object containing a `value` field and a `mask` flag.",
				"The parameters can be given as one collection, which allows `logBehaviour` and `input` to follow, or each as an argument of its own. Both modes are fully supported; a single parameter that is itself a collection must be given in the first mode."
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("output", "file or output stream to write the output to"),
				Parameter.mandatory("scriptConfigKey", "configuration key used to resolve the script's filename"),
				Parameter.optional("parameters", "collection of script parameters, each either a raw string or an object containing a `value` field and a `mask` flag"),
				Parameter.optional("logBehaviour", (
								"Specifies the function's call-logging behavior:"
										+ "<p>`0`: skip logging the command line<br>"
										+ "`1`: log only the script's full path<br>"
										+ "`2`: log the script path and all parameters, applying masking as configured</p>"
										+ "The default for this can be set via `%s`").formatted(Settings.LogScriptProcessCommandLine.getKey())),
				Parameter.optional("input", "text, byte array or file the script reads from its standard input, e.g. a password; never logged and, unlike a parameter, not visible in the process list of the host"));
	}

	@Override
	public String getShortDescription() {

		return "Executes a script returning the returning the raw output directly into the output stream.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function is very similar to `exec()`, but instead of returning the (text) result of the execution, it will copy its input stream to the given output **without modifying the binary data**.
			
			This is important to allow streaming of binary data from a script to the client.
			
			If a page is used to serve binary data, it must have the correct content-type and have the `pageCreatesRawData` flag enabled.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${execBinary(response, 'my.create.pdf')}", "Streaming binary content of the `my.create.pdf` script to the client."));
	}

	@Override
	protected int inputIndex() {

		return 4;
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	private static class ScriptingProcess extends AbstractBinaryProcess<String> {

		private static final String MASK_STRING = "***";

		private final List<String> arguments      = new ArrayList<>();
		private final StringBuilder logLineBuilder = new StringBuilder();
		private InputStream input                  = null;

		public ScriptingProcess(final SecurityContext securityContext, final String scriptName, final OutputStream out) {

			super(securityContext, out);

			this.arguments.add(scriptName);
			this.logLineBuilder.append(scriptName);
		}

		@Override
		public StringBuilder getCommandLine() {

			return null;
		}

		// no shell in between: parameter values reach the script as they are, $(...) or a quote in them is not interpreted
		@Override
		public List<String> getCommandArguments() {

			return arguments;
		}

		@Override
		public StringBuilder getLogLine() {

			return logLineBuilder;
		}

		@Override
		public InputStream getInput() {

			return input;
		}

		private void setInput(final InputStream input) {

			this.input = input;
		}

		@Override
		public String processExited(final int exitCode) {

			return errorStream();
		}

		private void addParameter(final Object parameter) {

			if (parameter instanceof Map<?,?> map) {

				this.addParameter(map);

			} else if (parameter == null) {

				throw new ArgumentNullException();

			} else {

				this.addParameter(parameter.toString(), false);
			}
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

			this.arguments.add(parameter);

			if (this.getLogBehaviour() == Settings.SCRIPT_PROCESS_LOG_STYLE.CUSTOM) {

				this.logLineBuilder.append(" ").append(maskInLog ? MASK_STRING : "\"" + parameter + "\"");
			}
		}

		@Override
		public void preprocess() {
		}
	}
}
