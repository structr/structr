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

import org.structr.core.entity.Group;
import org.structr.core.entity.PrincipalInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;
import org.structr.web.servlet.EventSourceServlet;

import java.util.HashSet;
import java.util.Set;

public class SendEventFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_SEND_EVENT    = "Usage: ${send_event(eventType, message, recipient(s))}. Example: ${send_event(\"message\", \"Welcome!\", find('User', 'name', 'Bob'))}";
	public static final String ERROR_MESSAGE_SEND_EVENT_JS = "Usage: ${{Structr.send_event(eventType, message, recipient(s))}}. Example: ${{Structr.send_event(\"message\", \"Welcome!\", $.find('User', 'name', 'Bob'))}}";

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public String getName() {
		return "send_event";
	}

	@Override
	public String getSignature() {
		return "eventType, message, recipient(s)";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			final String name             = sources[0].toString();
			final String message          = sources[1].toString();

			if (sources[2] instanceof User) {

				return EventSourceServlet.sendEvent(name, message, (User)sources[1]);

			} else if (sources[2] instanceof Group) {

				return EventSourceServlet.sendEvent(name, message, (Group)sources[2]);

			} else if (sources[2] instanceof Iterable) {

				final Set<PrincipalInterface> targets = new HashSet<>();

				for (Object obj : (Iterable)sources[2]) {

					if (PrincipalInterface.class.isAssignableFrom(obj.getClass())) {
						targets.add((PrincipalInterface)obj);
					} else {
						logger.warn("{}: Ignoring non-principal {}", getName(), obj);
					}
				}

				return EventSourceServlet.sendEvent(name, message, targets);
			}

			return false;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return false;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SEND_EVENT_JS : ERROR_MESSAGE_SEND_EVENT);
	}

	@Override
	public String shortDescription() {
		return "Triggers the sending of a sever-sent event to a given list of recipients. The message will only be sent if they have an open connection.";
	}
}
