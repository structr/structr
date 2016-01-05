package org.structr.core.parser.function;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.util.AbstractProcess;

/**
 *
 */
public class ExecFunction extends Function<Object, Object> {

	private static final Logger logger = Logger.getLogger(ExecFunction.class.getName());

	public static final String ERROR_MESSAGE_EXEC    = "Usage: ${exec(fileName [, parameters...]}. Example ${exec('my-script')}";
	public static final String ERROR_MESSAGE_EXEC_JS = "Usage: ${{Structr.exec(fileName [, parameters...]}}. Example ${{Structr.exec('my-script')}}";

	@Override
	public String getName() {
		return "exec()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final String scriptKey = sources[0].toString();
			final String script    = StructrApp.getConfigurationValue(scriptKey);

			if (StringUtils.isNotBlank(script)) {

				final StringBuilder scriptBuilder = new StringBuilder(script);
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

					iex.printStackTrace();

				} finally {

					executorService.shutdown();
				}

			} else {

				logger.log(Level.WARNING, "No script found for key {0} in structr.conf, nothing executed.", scriptKey);
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_EXEC_JS : ERROR_MESSAGE_EXEC);
	}

	@Override
	public String shortDescription() {
		return "Calls the given exported / dynamic method on the given entity";
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
