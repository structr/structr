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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyMap;
import org.structr.rest.logging.entity.LogEvent;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.Date;
import java.util.Map;

public class LogEventFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_LOG_EVENT    = "Usage: ${log_event(action, message [, subject [, object ]] )}. Example: ${log_event('read', 'Book has been read')}";
	public static final String ERROR_MESSAGE_LOG_EVENT_JS = "Usage: ${{Structr.logEvent(action, message [, subject [, object ]] )}}. Example: ${{Structr.logEvent('read', 'Book has been read')}}";

	@Override
	public String getName() {
		return "log_event";
	}

	@Override
	public String getSignature() {
		return "action, message [, subject [, object ]]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources.length == 1 && sources[0] instanceof Map) {

			// support javascript objects here
			final Map map = (Map)sources[0];

			final String action  = DOMNode.objectToString(map.get("action"));
			final String message = DOMNode.objectToString(map.get("message"));
			final String subject = DOMNode.objectToString(map.get("subject"));
			final String object  = DOMNode.objectToString(map.get("object"));

			return StructrApp.getInstance().create(LogEvent.class,
				new NodeAttribute(LogEvent.actionProperty, action),
				new NodeAttribute(LogEvent.messageProperty, message),
				new NodeAttribute(LogEvent.timestampProperty, new Date()),
				new NodeAttribute(LogEvent.subjectProperty, subject),
				new NodeAttribute(LogEvent.objectProperty, object)
			);

		} else {

			try {

				assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 4);

				final String action = sources[0].toString();
				final String message = sources[1].toString();

				final LogEvent logEvent = StructrApp.getInstance().create(LogEvent.class,
					new NodeAttribute(LogEvent.actionProperty, action),
					new NodeAttribute(LogEvent.messageProperty, message),
					new NodeAttribute(LogEvent.timestampProperty, new Date())
				);

				switch (sources.length) {

					case 4:
						final String object = sources[3].toString();
						logEvent.setProperties(logEvent.getSecurityContext(), new PropertyMap(LogEvent.objectProperty, object));
						// no break, next case should be included

					case 3:
						final String subject = sources[2].toString();
						logEvent.setProperties(logEvent.getSecurityContext(), new PropertyMap(LogEvent.subjectProperty, subject));
						break;
				}

				return logEvent;

			} catch (IllegalArgumentException e) {

				logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOG_EVENT_JS : ERROR_MESSAGE_LOG_EVENT);
	}

	@Override
	public String shortDescription() {
		return "Logs an event to the Structr log";
	}
}
