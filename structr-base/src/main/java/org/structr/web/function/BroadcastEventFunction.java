/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.schema.action.ActionContext;
import org.structr.web.servlet.EventSourceServlet;

public class BroadcastEventFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_BROADCAST_EVENT    = "Usage: ${broadcast_event(eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]] )}. Example: ${broadcast_event(\"message\", \"Welcome!\", true, false)}";
	public static final String ERROR_MESSAGE_BROADCAST_EVENT_JS = "Usage: ${{Structr.broadcast_event(eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]] )}}. Example: ${{Structr.broadcast_event(\"message\", \"Welcome!\", true, false)}}";

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public String getName() {
		return "broadcast_event";
	}

	@Override
	public String getSignature() {
		return "eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String name             = sources[0].toString();
			final String message          = sources[1].toString();
			final Boolean authenticated   = sources.length <= 2 || !(sources[2] instanceof Boolean) || (boolean) sources[2];
			final Boolean anonymous       = sources.length > 3 && sources[3] instanceof Boolean && (boolean) sources[3];

			EventSourceServlet.broadcastEvent(name, message, authenticated, anonymous);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_BROADCAST_EVENT_JS : ERROR_MESSAGE_BROADCAST_EVENT);
	}

	@Override
	public String shortDescription() {
		return "Triggers the sending of a sever-sent event all authenticated and/or anonymous users with an open connection.";
	}
}
