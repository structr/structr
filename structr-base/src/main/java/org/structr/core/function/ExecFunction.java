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

import org.structr.api.config.Setting;
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
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.util.AbstractProcess;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecFunction extends AdvancedScriptingFunction {

	public static final String SCRIPTS_FOLDER = Settings.ScriptsPath.getValue();
	private static final String MASK          = "***";

	@Override
	public String getName() {

		return "exec";
	}

	@Override
	public List<Signature> getSignatures() {

		final List<Signature> signatures = new ArrayList<>(Signature.forAllScriptingLanguages("scriptConfigKey [, parameters [, logBehaviour [, input ] ] ]"));

		signatures.addAll(Signature.forAllScriptingLanguages("scriptConfigKey, parameter1 [, parameter2, ... ]"));

		return signatures;
	}

	public String getSignature() {

		return getSignatures().get(0).getSignature();
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			// logBehaviour and input may be null, to pass an input with the default log behaviour
			assertMandatoryArgumentsNotNull(sources, 1, 2);

			final String sanityCheckedAbsolutePathOrNull = getSanityCheckedPathForScriptSetting(sources[0].toString());
			if (sanityCheckedAbsolutePathOrNull != null) {

				final ScriptingProcess scriptingProcess = new ScriptingProcess(ctx.getSecurityContext(), sanityCheckedAbsolutePathOrNull);

				if (sources.length > 1) {

					// two equal modes: the parameters as one collection, followed by logBehaviour and input, or each parameter as an argument of its own
					final boolean parametersAsCollection = (sources[1] instanceof Collection<?>);
					if (!parametersAsCollection) {

						for (int i = 1; i < sources.length; i++) {

							scriptingProcess.addParameter(sources[i]);
						}

					} else {

						if (sources.length > 2) {

							if (sources[2] instanceof Number) {

								final int logBehavior = ((Number)sources[2]).intValue();

								scriptingProcess.setLogBehaviour(logBehavior);

							} else if (sources[2] != null) {

								logger.warn("{}(): If using a collection of parameters as second argument, the third argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", getName(), sources[2]);
							}
						}

						if (sources.length > 3) {

							scriptingProcess.setInput(toInput(sources[3]));
						}

						final Collection params = (Collection)sources[1];

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
				}
			}

		} catch (IOException ex) {

			Function.logException(logger, ex, "{}(): IOException encountered: {}", getName(), ex.getMessage());

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return null;
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${exec(scriptConfigKey [, parameters [, logBehaviour [, input ] ] ])}. Example: ${exec('my-script', merge('param1', 'param2'), 1)}"),
			Usage.javaScript("Usage: ${{ $.exec(scriptConfigKey [, parameters [, logBehaviour [, input ] ] ]); }}. Example: ${{ $.exec('my-script', ['param1', { value: 'CLIENT_SECRET', mask: true }], 2); }}")
		);
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
				"This function does not preserve binary content, it can **not** be used to stream binary data through Structr. Use `execBinary()` for that.",
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

		return "Executes a script returning the standard output of the script.";
	}

	@Override
	public String getLongDescription() {

		return """
			In order to prevent execution of arbitrary code, the script must be registered in structr.conf file using the following syntax.
			`key.for.my.script = my-script.sh`
			
			Upon successful execution, the complete output of the script (not the return value) is returned.
			
			`input` is written to the script's standard input, which is closed afterwards; without `input`, the script reads an empty standard input.
			
			`logBehaviour` controls how and if the command line is logged upon execution. If no value is given, the global setting `%s` will be used.
			""".formatted(Settings.LogScriptProcessCommandLine.getKey());
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.structrScript("${exec('key.for.my.script', merge('param1', 'param2'), 0)}", "Execute a script with 2 parameters and no log output, using merge() to create the parameter list"),
				Example.javaScript("""
						${{
							$.exec('key.for.my.script', ['param1', { value: 'CLIENT_SECRET', mask: true }], 2);
						}}
						""", "Execute a script with 2 parameters, where one is being masked in the log output"),
				Example.javaScript("""
						${{
							$.exec('key.for.login.script', ['admin'], null, password);
						}}
						""", "Pass a password on standard input, so it does not show up in the process list of the host")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	protected String getSanityCheckedPathForScriptSetting(final String scriptKey) throws IOException {

		final Setting<String> scriptSetting = Settings.getStringSetting(scriptKey);
		if (scriptSetting == null) {

			logger.warn("{}(): Key '{}' not found in structr.conf, nothing executed.", getName(), scriptKey);

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

			} else if (!Settings.AllowSymbolicLinksInScriptPaths.getValue() && !pathIsRegularFile) {

				logger.warn("{}(): Script key '{}' = '{}' points to script file '{}' which is either not a file (or a symlink) and not allowed, nothing executed. Set '{}' to true to disable this check.", getName(), scriptKey, scriptName, absolutePath, Settings.AllowSymbolicLinksInScriptPaths.getKey());

			} else if (!Settings.AllowPathTraversalInScriptPaths.getValue() && !pathIsAllowed) {

				logger.warn("{}(): Script key '{}' = '{}' resolves to '{}' which seems to contain a directory traversal attack, nothing executed. Set '{}' to true to disable this check.", getName(), scriptKey, scriptName, absolutePath, Settings.AllowPathTraversalInScriptPaths.getKey());

			} else {

				return absolutePath;
			}
		}

		return null;
	}

	/**
	 * Masked parameters stay masked in every log message, also when the call fails, e.g. when a restart interrupts the process.
	 */
	@Override
	protected String parametersForLog(final Object[] parameters) {

		if (parameters == null) {

			return "[]";
		}

		final List<String> rendered = new ArrayList<>();

		for (int i = 0; i < parameters.length; i++) {

			final Object parameter = parameters[i];

			// only the collection mode has an input argument
			if (i == inputIndex() && parameter != null && parameters[inputIndex() - 2] instanceof Collection<?>) {

				rendered.add(MASK);

			} else if (parameter instanceof Collection<?> collection) {

				rendered.add(collection.stream().map(ExecFunction::parameterForLog).toList().toString());

			} else {

				rendered.add(parameterForLog(parameter));
			}
		}

		return rendered.toString();
	}

	/** At least minLength arguments, and none of the first mandatoryCount null. */
	protected void assertMandatoryArgumentsNotNull(final Object[] sources, final int minLength, final int mandatoryCount) throws ArgumentCountException {

		if (sources.length < minLength) {

			throw ArgumentCountException.tooFew(sources.length, minLength);
		}

		assertArrayHasNoNullValues(Arrays.copyOf(sources, Math.min(sources.length, mandatoryCount)));
	}

	/** The position of the input argument, which is never logged. */
	protected int inputIndex() {

		return 3;
	}

	/** The input of a script: a string as UTF-8, a byte array, or the content of a file. */
	protected static InputStream toInput(final Object value) throws IOException {

		if (value == null) {

			return null;
		}

		if (value instanceof byte[] bytes) {

			return new ByteArrayInputStream(bytes);
		}

		if (value instanceof AbstractNode node && node.is(StructrTraits.FILE)) {

			return node.as(org.structr.web.entity.File.class).getInputStream();
		}

		return new ByteArrayInputStream(value.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static String parameterForLog(final Object parameter) {

		if (parameter instanceof Map<?, ?> map) {

			return Boolean.TRUE.equals(map.get("mask")) ? MASK : String.valueOf(map.get("value"));
		}

		return String.valueOf(parameter);
	}

	protected static class ScriptingProcess extends AbstractProcess<String> {

		private static final String MASK_STRING = "***";

		private final List<String> arguments      = new ArrayList<>();
		private final StringBuilder logLineBuilder = new StringBuilder();
		private InputStream input                  = null;

		public ScriptingProcess(final SecurityContext securityContext, final String scriptName) {

			super(securityContext);

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

			return outputStream();
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

				throw new ArgumentNullException();
			}

			if (maskParameter == null) {

				logger.info("exec(): Expected 'mask' attribute to be non-null for parameter in map-representation (ex.: { value: \"myParameter\", mask: true }). Assuming 'mask = false'");
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
