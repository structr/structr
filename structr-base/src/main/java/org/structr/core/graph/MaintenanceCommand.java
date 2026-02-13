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
package org.structr.core.graph;

import jakarta.servlet.http.HttpServletResponse;
import org.structr.api.Predicate;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Documentable;
import org.structr.docs.ontology.ConceptType;

import java.util.*;

/**
 * Common base interface for commands that can be registered as a maintenance
 * command. Maintenance commands can be called via REST if registered
 * appropriately.
 *
 *
 */
public interface MaintenanceCommand extends Documentable {

	String COMMAND_TYPE_KEY         = "type";
	String COMMAND_SUBTYPE_KEY      = "subtype";
	String COMMAND_TITLE_KEY        = "title";
	String COMMAND_MESSAGE_KEY      = "message";

	String COMMAND_SUBTYPE_BEGIN    = "BEGIN";
	String COMMAND_SUBTYPE_PROGRESS = "PROGRESS";
	String COMMAND_SUBTYPE_END      = "END";
	String COMMAND_SUBTYPE_WARNING  = "WARNING";
	String COMMAND_SUBTYPE_INFO     = "INFO";

	void execute(Map<String, Object> attributes) throws FrameworkException;
	boolean requiresEnclosingTransaction();
	boolean requiresFlushingOfCaches();
	Map<String, String> getCustomHeaders();

	@Override
	default List<ConceptReference> getParentConcepts() {

		final List<ConceptReference> parentConcepts = new LinkedList<>();

		parentConcepts.add(ConceptReference.of(ConceptType.Topic, "Maintenance Commands"));

		return parentConcepts;
	}

	default Object getCommandResult() {
		return Collections.EMPTY_LIST;
	}

	default int getCommandStatusCode() {
		return HttpServletResponse.SC_OK;
	}

	default void publishBeginMessage (final String type, final Map<String, Object> additionalInfo) {

		publishCustomMessage(type, COMMAND_SUBTYPE_BEGIN, null, additionalInfo);
	}

	default void publishProgressMessage (final String type, final String message) {

		publishProgressMessage(type, message, null);
	}

	default void publishProgressMessage (final String type, final String message, final Map<String, Object> additionalInfo) {

		publishCustomMessage(type, COMMAND_SUBTYPE_PROGRESS, message, additionalInfo);
	}

	default void publishEndMessage (final String type, final Map<String, Object> additionalInfo) {

		publishCustomMessage(type, COMMAND_SUBTYPE_END, null, additionalInfo);
	}

	default void publishWarningMessage (final String title, final String text) {

		final Map<String, Object> warningMsgData = new HashMap<>();
		warningMsgData.put(COMMAND_TYPE_KEY,    COMMAND_SUBTYPE_WARNING);
		warningMsgData.put(COMMAND_TITLE_KEY,   title);
		warningMsgData.put(COMMAND_MESSAGE_KEY, text);

		TransactionCommand.simpleBroadcastGenericMessage(warningMsgData, Predicate.all());
	}

	default void publishInfoMessage (final String title, final String text) {

		final Map<String, Object> warningMsgData = new HashMap<>();
		warningMsgData.put(COMMAND_TYPE_KEY,    COMMAND_SUBTYPE_INFO);
		warningMsgData.put(COMMAND_TITLE_KEY,   title);
		warningMsgData.put(COMMAND_MESSAGE_KEY, text);

		TransactionCommand.simpleBroadcastGenericMessage(warningMsgData, Predicate.all());
	}

	default void publishCustomMessage (final String type, final String subType, final String message) {

		publishCustomMessage(type, subType, message, null);
	}

	default void publishCustomMessage (final String type, final String subType, final String message, final Map<String, Object> additionalInfo) {

		final Map<String, Object> msgData = new HashMap<>();
		msgData.put(COMMAND_TYPE_KEY,    type);
		msgData.put(COMMAND_SUBTYPE_KEY, subType);

		if (message != null) {
			msgData.put(COMMAND_MESSAGE_KEY, message);
		}

		if (additionalInfo != null) {
			// careful with additional info. "type", "subtype" and "message" overwrite control data
			msgData.putAll(additionalInfo);
		}

		TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());
	}

}
