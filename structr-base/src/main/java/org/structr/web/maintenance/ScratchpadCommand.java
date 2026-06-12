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
package org.structr.web.maintenance;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.structr.api.service.Command;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScratchpadCommand extends Command implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(ScratchpadCommand.class.getName());

	public static final String MDC_SCRATCHPAD_TAG = "structrScratchMDC";

	private static long scratchCounter = 0;

	private static final String COMMAND_KEY    = "command";
	private static final String SCRIPT_KEY     = "script";
	private static final String SCRATCH_ID_KEY = "scratchId";

	private SecurityContext securityContext = null;

	Object result;
	private int statusCode = HttpServletResponse.SC_OK;

	@Override
	public Class getServiceClass() {
		return null;
	}

	@Override
	public void initialized() {
		this.securityContext = (SecurityContext)getArgument("securityContext");
	}

	@Override
	public void execute(Map<String, Object> parameters) throws FrameworkException {

		try {
			final String command = (String) parameters.get(COMMAND_KEY);

			if (command == null) {
				statusCode = HttpServletResponse.SC_BAD_REQUEST;
				result = "No command specified";
				return;
			}

			if ("getNewScratchId".equals(command)) {

				final long scratchCounterForRun = scratchCounter;

				scratchCounter++;

				final String scratchLogIdentifier = getScratchLogString(scratchCounterForRun);

				final GraphObjectMap result = new GraphObjectMap();
				result.setProperty(new LongProperty(SCRATCH_ID_KEY), scratchCounterForRun);
				result.setProperty(new StringProperty("logString"), scratchLogIdentifier);

				this.result = result;

			} else if ("run".equals(command)) {

				final String script  = (String) parameters.get(SCRIPT_KEY);
				final Long scratchId = (Long) parameters.get(SCRATCH_ID_KEY);

				if (script == null) {
					statusCode = HttpServletResponse.SC_BAD_REQUEST;
					result = "Unable to run null script";
					return;
				}
				if (scratchId == null) {
					statusCode = HttpServletResponse.SC_BAD_REQUEST;
					result = "Unable to run script without scratchId";
					return;
				}

				try (final Tx tx = StructrApp.getInstance().tx()) {

					MDC.put(MDC_SCRATCHPAD_TAG, getScratchLogString(scratchId));

					final GraphObjectMap result = new GraphObjectMap();
					result.setProperty(new LongProperty(SCRATCH_ID_KEY), scratchId);
					result.setProperty(new GenericProperty("scriptResult"), Scripting.evaluate(new ActionContext(securityContext), null, "${" + script.trim() + "}", "scratchpad"));

					this.result = result;

					tx.success();

				} catch (final FrameworkException ex) {

					logger.warn("Error while executing scratchpad script: {}", script, ex);

					statusCode = 422;
				}

			} else {

				logger.error("Unknown command: {}", command);
				result = "Unknown command " + command;
				statusCode = 422;
			}

		} catch (Throwable t) {

			statusCode = 422;
			result = t.getMessage();

			logger.error("Error while executing scratchpad", t);
		}
	}

	private String getScratchLogString (final long scratchId) {
		return "[scratch_" + scratchId + "]";
	}

	@Override
	public int getCommandStatusCode() {
		return statusCode;
	}

	@Override
	public Object getCommandResult() {
		return result;
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	@Override
	public Map<String, String> getCustomHeaders() {
		return Collections.EMPTY_MAP;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Hidden;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of();
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<org.structr.docs.Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}
}