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
package org.structr.rest.resource;

import org.structr.common.helper.ServerLogReader;
import org.structr.core.function.GetAvailableServerLogsFunction;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;

import java.util.List;
import java.util.regex.Pattern;

@Documentation(name="Request log endpoint", type=ConceptType.RestEndpoint, shortDescription="HTTP endpoint that returns the request log written with log.requests enabled, one line per request, for admin users only. URL path is /_requestLog, with the optional request parameters lines (default 50), filter (only lines containing this text), since (the offset of a previous response, to read only what was logged after it) and logFile (one of availableLogFiles in the response, the current log first and the rotated ones newest first; since cannot be used with a compressed one).", parent="System endpoints")
public class RequestLogResource extends ServerLogResource {

	public RequestLogResource() {

		super("_requestLog");
	}

	@Override
	protected List<String> getLogFiles() {

		return GetAvailableServerLogsFunction.newestFirst(GetAvailableServerLogsFunction.getListOfRequestLogFileNames(true));
	}

	@Override
	protected Pattern getEntryStart() {

		return ServerLogReader.ENTRY_PER_LINE;
	}

	@Override
	protected boolean hasLevels() {

		return false;
	}

	@Override
	protected String getLogName() {

		return "request log";
	}
}
