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
package org.structr.appmap;

import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEvent;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.App;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The scripting errors of the runtime event log, at the elements of the map they happened in:
 * a method directly, a script of an HTML element at its page or shared component, since the
 * map shows neither HTML elements nor repeaters and action mappings as elements of their own.
 * The log lives in memory, so the errors are those since the last start of the instance.
 */
public class ScriptErrors {

	// the type of a scripting error in the event log, see RuntimeEventLog.scripting
	private static final String SCRIPTING = "Scripting";

	// map elements that are drawn inside their page, not as elements of their own
	private static final Set<String> INSIDE = Set.of(ApplicationMap.VIEW, ApplicationMap.ACTION);

	public static void apply(final ApplicationMap map, final App app) throws FrameworkException {

		// the log is newest first, so the first error of a code source is its latest
		final Map<String, List<RuntimeEvent>> bySource = new LinkedHashMap<>();

		for (final RuntimeEvent event : RuntimeEventLog.getEvents(e -> SCRIPTING.equals(e.getType()))) {

			final Object source = event.getData().get(RuntimeEvent.ID_PROPERTY);

			if (source != null) {
				bySource.computeIfAbsent(source.toString(), k -> new LinkedList<>()).add(event);
			}
		}

		if (bySource.isEmpty()) {
			return;
		}

		final Map<String, Object> located = ApplicationMap.locate(app, bySource.keySet());

		for (final Map.Entry<String, List<RuntimeEvent>> entry : bySource.entrySet()) {

			final String source               = entry.getKey();
			final ApplicationMap.MapNode node = elementOf(map, located.get(source));

			if (node != null) {

				final RuntimeEvent latest = entry.getValue().get(0);
				final Object message      = latest.getData().get("message");
				final Object row          = latest.getData().get("row");

				// the line only means something in the code the element shows, which is its own
				node.addErrors(entry.getValue().size(), message != null ? message.toString() : null, source.equals(node.getId()) && row instanceof Number n ? n.intValue() : null, latest.absoluteTimestamp());
			}
		}
	}

	// ----- private methods -----
	// the first element of the map in the chain of places an object may be shown as, see ApplicationMap.locate
	private static ApplicationMap.MapNode elementOf(final ApplicationMap map, final Object chain) {

		if (chain instanceof List<?> ids) {

			for (final Object id : ids) {

				final ApplicationMap.MapNode node = map.getNode(id.toString());

				if (node != null && !INSIDE.contains(node.getKind())) {
					return node;
				}
			}
		}

		return null;
	}
}
