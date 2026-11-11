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
package org.structr.core.script;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.trace.CallFrame;
import org.structr.common.trace.CodeSourceInfo;
import org.structr.common.trace.ScriptError;
import org.structr.common.trace.ScriptFrame;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reports a failure of user code: the log line, the runtime event and the admin UI notification,
 * each attributed to the node holding the failing code, so the editor marks the line that failed.
 */
public class ScriptErrorReporter implements ScriptError.Reporter {

	// scripting errors have always been logged under this name, and log configurations rely on it
	private static final Logger logger     = LoggerFactory.getLogger(Scripting.class);
	private static final String ERROR_NAME = "Scripting Error";

	@Override
	public void report(final ScriptError error) {

		final CallFrame origin            = error.getOrigin();
		// the editor can only mark code that is stored in a node
		final ScriptFrame script          = origin instanceof ScriptFrame frame && frame.getCodeSourceId() != null ? frame : null;
		final String codeSourceId         = script != null ? script.getCodeSourceId() : null;
		final CodeSourceInfo codeSource   = error.getCodeSource();
		final String name                 = origin.getName();
		final int row                     = Math.max(error.getLine(), 1);
		final Map<String, Object> details = new LinkedHashMap<>();

		details.put("row",       row);
		details.put("column",    Math.max(error.getColumn(), 1));
		details.put("endRow",    Math.max(error.getEndLine(), row));
		details.put("endColumn", Math.max(error.getEndColumn(), 1));

		putIfNotNull(details, "message", error.getMessage());

		if (script != null) {

			if (script.getEntityId() != null) {

				details.put("entityType", script.getEntityType());
				details.put("entityId",   script.getEntityId());
			}

			if (codeSource != null && codeSource.isStaticMethod()) {

				details.put("staticType",     codeSource.staticType());
				details.put("isStaticMethod", true);
			}
		}

		putIfNotNull(details, "name", name);

		final Map<String, Object> eventData   = new LinkedHashMap<>();
		final Map<String, Object> messageData = new LinkedHashMap<>();

		eventData.put("errorName", ERROR_NAME);
		eventData.put("entity",    (StringUtils.isNotBlank(name) ? "\"" + name + "\":" : "" ) + codeSourceId);
		eventData.putAll(details);

		messageData.put("type", "SCRIPTING_ERROR");
		messageData.putAll(details);

		if (script != null) {

			final String nodeType = codeSource != null ? codeSource.type() : null;
			final String nodeId   = codeSource != null ? codeSource.id() : null;

			eventData.put("type",       nodeType);
			eventData.put("id",         nodeId);
			messageData.put("nodeType", nodeType);
			messageData.put("nodeId",   nodeId);
		}

		if (origin.getParent() != null) {

			// the websocket message carries flat values only, so the callers go as text
			eventData.put("trace",     error.getTrace());
			messageData.put("callers", error.getCallers());
		}

		RuntimeEventLog.scripting(ERROR_NAME, eventData);

		// a failed assertion is the script's answer, not a fault, so the admin UI is not told about it
		if (!error.isAssertion() && origin.getSessionId() != null) {

			try {

				TransactionCommand.simpleBroadcastGenericMessage(messageData, Predicate.only(origin.getSessionId()));

			} catch (Throwable t) {

				logger.warn("Unable to send scripting error to the admin UI: {}", t.getMessage());
			}
		}

		logger.error(error.format());
	}

	/**
	 * Looks up the node holding failing code. A failure on a worker thread is reported after its
	 * transaction has ended, so this opens its own.
	 */
	@Override
	public CodeSourceInfo resolve(final String codeSourceId) {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface codeSource = StructrApp.getInstance().getNodeById(codeSourceId);
			CodeSourceInfo info            = null;

			if (codeSource != null) {

				String staticType = null;

				if (codeSource.is(StructrTraits.SCHEMA_METHOD) && codeSource.as(SchemaMethod.class).isStaticMethod()) {

					final AbstractSchemaNode schemaNode = codeSource.as(SchemaMethod.class).getSchemaNode();

					staticType = schemaNode != null ? schemaNode.getName() : codeSource.getName();
				}

				info = new CodeSourceInfo(codeSource.getUuid(), codeSource.getTraits().getName(), staticType);
			}

			tx.success();

			return info;

		} catch (Throwable t) {

			logger.debug("Unable to resolve code source {} of a scripting error", codeSourceId, t);

			return null;
		}
	}

	// ----- private methods -----
	private static void putIfNotNull(final Map<String, Object> map, final String key, final Object value) {

		if (value != null) {

			map.put(key, value);
		}
	}
}
