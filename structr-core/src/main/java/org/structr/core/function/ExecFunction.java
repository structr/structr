/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.util.AbstractProcess;

public class ExecFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_EXEC    = "Usage: ${exec(fileName [, parameters...]}. Example ${exec('my-script')}";
	public static final String ERROR_MESSAGE_EXEC_JS = "Usage: ${{Structr.exec(fileName [, parameters...]}}. Example ${{Structr.exec('my-script')}}";

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

			if (scriptSetting != null) {

				final StringBuilder scriptBuilder = new StringBuilder(scriptSetting.getValue());
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

			} else {

				logger.warn("No script found for key \"{}\" in structr.conf, nothing executed.", scriptKey);
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
