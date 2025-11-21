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

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.rest.traits.definitions.LogEventTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class LogEventFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "log_event";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("action, message [, subject [, object ]]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Traits traits  = Traits.of(StructrTraits.LOG_EVENT);

		if (sources.length == 1 && sources[0] instanceof Map map) {

			// support javascript objects here

			final String action  = DOMNode.objectToString(map.get(LogEventTraitDefinition.ACTION_PROPERTY));
			final String message = DOMNode.objectToString(map.get(LogEventTraitDefinition.MESSAGE_PROPERTY));
			final String subject = DOMNode.objectToString(map.get(LogEventTraitDefinition.SUBJECT_PROPERTY));
			final String object  = DOMNode.objectToString(map.get(LogEventTraitDefinition.OBJECT_PROPERTY));

			return StructrApp.getInstance().create(StructrTraits.LOG_EVENT,
				new NodeAttribute(traits.key(LogEventTraitDefinition.ACTION_PROPERTY), action),
				new NodeAttribute(traits.key(LogEventTraitDefinition.MESSAGE_PROPERTY), message),
				new NodeAttribute(traits.key(LogEventTraitDefinition.TIMESTAMP_PROPERTY), new Date()),
				new NodeAttribute(traits.key(LogEventTraitDefinition.SUBJECT_PROPERTY), subject),
				new NodeAttribute(traits.key(LogEventTraitDefinition.OBJECT_PROPERTY), object)
			);

		} else {

			try {

				assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 4);

				final String action = sources[0].toString();
				final String message = sources[1].toString();

				final NodeInterface logEvent = StructrApp.getInstance().create(StructrTraits.LOG_EVENT,
					new NodeAttribute(traits.key(LogEventTraitDefinition.ACTION_PROPERTY), action),
					new NodeAttribute(traits.key(LogEventTraitDefinition.MESSAGE_PROPERTY), message),
					new NodeAttribute(traits.key(LogEventTraitDefinition.TIMESTAMP_PROPERTY), new Date())
				);

				switch (sources.length) {

					case 4:
						final String object = sources[3].toString();
						logEvent.setProperties(logEvent.getSecurityContext(), new PropertyMap(traits.key(LogEventTraitDefinition.OBJECT_PROPERTY), object));
						// no break, next case should be included

					case 3:
						final String subject = sources[2].toString();
						logEvent.setProperties(logEvent.getSecurityContext(), new PropertyMap(traits.key(LogEventTraitDefinition.SUBJECT_PROPERTY), subject));
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${log_event(action, message [, subject [, object ]] )}. Example: ${log_event('read', 'Book has been read')}"),
			Usage.javaScript("Usage: ${{Structr.logEvent(action, message [, subject [, object ]] )}}. Example: ${{Structr.logEvent('read', 'Book has been read')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Logs an event to the Structr log.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
