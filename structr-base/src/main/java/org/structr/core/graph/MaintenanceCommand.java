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
package org.structr.core.graph;

import jakarta.servlet.http.HttpServletResponse;
import org.structr.api.Predicate;
import org.structr.common.error.FrameworkException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Common base interface for commands that can be registered as a maintenance
 * command. Maintenance commands can be called via REST if registered
 * appropriately.
 *
 *
 */
public interface MaintenanceCommand {

	final static String COMMAND_TYPE_KEY         = "type";
	final static String COMMAND_SUBTYPE_KEY      = "subtype";
	final static String COMMAND_TITLE_KEY        = "title";
	final static String COMMAND_MESSAGE_KEY      = "message";

	final static String COMMAND_SUBTYPE_BEGIN    = "BEGIN";
	final static String COMMAND_SUBTYPE_PROGRESS = "PROGRESS";
	final static String COMMAND_SUBTYPE_END      = "END";
	final static String COMMAND_SUBTYPE_WARNING  = "WARNING";

	public void execute(Map<String, Object> attributes) throws FrameworkException;
	public boolean requiresEnclosingTransaction();
	public boolean requiresFlushingOfCaches();
	public Map<String, String> getCustomHeaders();

	default Object getCommandResult() {
		return Collections.EMPTY_LIST;
	}

	default int getCommandStatusCode() {
		return HttpServletResponse.SC_OK;
	}

	default void publishBeginMessage (final String type, final Map additionalInfo) {

		final Map<String, Object> msgData = new HashMap();
		msgData.put(COMMAND_TYPE_KEY,    type);
		msgData.put(COMMAND_SUBTYPE_KEY, COMMAND_SUBTYPE_BEGIN);

		if (additionalInfo != null) {
			msgData.putAll(additionalInfo);
		}

		TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());
	}

	default void publishProgressMessage (final String type, final String message) {

		final Map<String, Object> msgData = new HashMap();
		msgData.put(COMMAND_TYPE_KEY,    type);
		msgData.put(COMMAND_SUBTYPE_KEY, COMMAND_SUBTYPE_PROGRESS);
		msgData.put(COMMAND_MESSAGE_KEY, message);

		TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());
	}

	default void publishEndMessage (final String type, final Map additionalInfo) {

		final Map<String, Object> msgData = new HashMap();
		msgData.put(COMMAND_TYPE_KEY,    type);
		msgData.put(COMMAND_SUBTYPE_KEY, COMMAND_SUBTYPE_END);

		if (additionalInfo != null) {
			msgData.putAll(additionalInfo);
		}

		TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());
	}

	default void publishWarningMessage (final String title, final String text) {

		final Map<String, Object> warningMsgData = new HashMap();
		warningMsgData.put(COMMAND_TYPE_KEY,    COMMAND_SUBTYPE_WARNING);
		warningMsgData.put(COMMAND_TITLE_KEY,   title);
		warningMsgData.put(COMMAND_MESSAGE_KEY, text);

		TransactionCommand.simpleBroadcastGenericMessage(warningMsgData, Predicate.all());
	}

}
