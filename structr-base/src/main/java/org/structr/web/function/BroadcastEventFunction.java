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
package org.structr.web.function;

import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.servlet.EventSourceServlet;

import java.util.List;

public class BroadcastEventFunction extends UiAdvancedFunction {

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String eventType        = sources[0].toString();
			final String message          = sources[1].toString();
			final Boolean authenticated   = sources.length <= 2 || !(sources[2] instanceof Boolean) || (boolean) sources[2];
			final Boolean anonymous       = sources.length > 3 && sources[3] instanceof Boolean && (boolean) sources[3];

			EventSourceServlet.broadcastEvent(eventType, message, authenticated, anonymous);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String getShortDescription() {
		return "Triggers the sending of a sever-sent event to all authenticated and/or anonymous users with an open connection.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `broadcastEvent()` function implements the server-side part of server-sent events based on the EventSource servlet. Server-sent events allow you to send messages from the server to the client asynchronously, e.g. you can update data or trigger a reload based on events that happen on ther server.

		See https://developer.mozilla.org/en-US/docs/Web/API/EventSource for more information about server-sent events.

		In order to use server-sent events, you need to enable the EventSource servlet in structr.conf. After that, you can use the below code in your HTML frontend to start receiving events.

		**Example setup in HTML**
		```
		// this needs to be done in your frontend HTML, not on the server
		var source = new EventSource("/structr/EventSource", { withCredentials: true });
		source.onmessage = function(event) {
			console.log(event);
		};
		```
		""";
	}

	@Override
	public String getName() {
		return "broadcastEvent";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${broadcastEvent(eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]] )}. Example: ${broadcastEvent(\"message\", \"Welcome!\", true, false)}"),
			Usage.javaScript("Usage: ${{ $.broadcastEvent(eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]] )}}. Example: ${{ $.broadcastEvent(\"message\", \"Welcome!\", true, false)}}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("eventType", "type of event to send, usually `message`"),
			Parameter.mandatory("message", "message to send"),
			Parameter.optional("authenticatedUsers", "whether to send messages to authenticated users, defaults to `true`"),
			Parameter.optional("anonymousUser", "whether to send messages to anonymous users, defaults to `false`")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${ broadcastEvent('message', 'Hello world!', true, false) }", "Send a generic message to the frontend"),
			Example.javaScript("${{ $.broadcastEvent('message', JSON.stringify({id: 'APP_MAINTENANCE_SOON', message: 'Application going down for maintenance soon!', date: new Date().getTime()}), true, false); }}", "Send a JSON message to the frontend")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"In order to use server-sent events, you need to enable the EventSource servlet in structr.conf.",
			"If you want to use the generic `onmessage` event handler in your frontend, the messageType **must** be set to `message`. For message types other than `message`, you need to add a dedicated event listener to the EventSource instance using `addEventListener()` in your frontend."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}
