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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecFunction extends AdvancedScriptingFunction {

	public static final String SCRIPTS_FOLDER = Settings.ScriptsPath.getValue();

	@Override
	public String getName() {
		return "exec";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("scriptConfigKey [, parameters [, logBehaviour ] ]");
	}

	public String getSignature() {
		return getSignatures().get(0).getSignature();
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final String sanityCheckedAbsolutePathOrNull = getSanityCheckedPathForScriptSetting(sources[0].toString());

			if (sanityCheckedAbsolutePathOrNull != null) {

				final ScriptingProcess scriptingProcess = new ScriptingProcess(ctx.getSecurityContext(), sanityCheckedAbsolutePathOrNull);

				if (sources.length > 1) {

					final boolean isNewCallSignature = (sources[1] instanceof Collection<?>);

					if (!isNewCallSignature) {

						logger.warn("{}(): Deprecation Warning: The call signature for this function has changed. The old signature of providing all arguments to the script is still supported but will be removed in a future version. Please consider upgrading to the new signature: {}", getName(), getSignature());

						for (int i = 1; i < sources.length; i++) {

							if (sources[i] != null) {

								scriptingProcess.addParameter(sources[i].toString());
							}
						}

					} else {

						if (sources.length > 2) {

							if (sources[2] instanceof Number) {

								final int logBehavior = ((Number)sources[2]).intValue();

								scriptingProcess.setLogBehaviour(logBehavior);

							} else {

								logger.warn("{}(): If using a collection of parameters as second argument, the third argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", getName(), sources[2]);
							}
						}

						final Collection params = (Collection)sources[1];

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
			Usage.structrScript("Usage: ${exec(scriptConfigKey [, parameters [, logBehaviour ] ])}. Example: ${exec('my-script', merge('param1', 'param2'), 1)}"),
			Usage.javaScript("Usage: ${{ $.exec(scriptConfigKey  [, parameters [, logBehaviour ] ]); }}. Example: ${{ $.exec('my-script', ['param1', { value: 'CLIENT_SECRET', mask: true }], 2); }}")
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
				"This function does not preserve binary content, it can *not* be used to stream binary data through Structr. Use `execBinary()` for that.",
				"Caution: Supplying unvalidated user input to this command may introduce security vulnerabilities.",
				"All parameter values are automatically put in double-quotes",
				"All parameters can be passed as a string or as an object containing a `value` field and a `mask` flag.",
				"Double-quotes in parameter values are automatically escaped as `\\\"`"
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
								+ "The default for this can be set via `%s`").formatted(Settings.LogScriptProcessCommandLine.getKey()
						)
				)
		);
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
						""", "Execute a script with 2 parameters, where one is being masked in the log output")
		);
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

	protected static class ScriptingProcess extends AbstractProcess<String> {

		private static final String MASK_STRING = "***";

		private final StringBuilder cmdLineBuilder = new StringBuilder();
		private final StringBuilder logLineBuilder = new StringBuilder();

		public ScriptingProcess(final SecurityContext securityContext, final String scriptName) {

			super(securityContext);

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
			return outputStream();
		}

		private void addParameter(final String parameter) {
			this.addParameter(parameter, false);
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
