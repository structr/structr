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
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.property.GenericProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The scripting errors the runtime event log holds that are not seen yet, each with the call trace that led to it (see
 * org.structr.common.trace.ScriptError.getTrace), for the application map: errors with the same trace and the
 * same message count as one, the most recent first. A trace is a list of frames, the failing one first, then its
 * callers; each frame as { k: kind, n: name, d: description, r: row, c: column, m: HTTP method, at: the ids of the
 * map elements it may be shown as (see ApplicationMap.locate) }, where row and column of a caller are those of its
 * call. A scripting error that was not called from anywhere has its failing frame only.
 *
 * The result is { traces: [{ m: message, c: count, t: time of the last one, f: frames }] }.
 */
public class CallTraces {

	// the event type of scripting errors in the runtime event log, and the most traces the map is given
	private static final String SCRIPTING = "Scripting";
	private static final int MAX_TRACES   = 500;

	public static GraphObjectMap of(final App app) throws FrameworkException {

		// an error is seen once the code it happened in was saved again, as the Pages area counts it
		return of(app, RuntimeEventLog.getEvents(event -> SCRIPTING.equals(event.getType()) && !event.getSeen()));
	}

	public static GraphObjectMap of(final App app, final Collection<RuntimeEvent> events) throws FrameworkException {

		final Map<String, Trace> traces = new LinkedHashMap<>();

		for (final RuntimeEvent event : events) {

			final Map<String, Object> data         = event.getData();
			final List<Map<String, Object>> frames = framesOf(data);
			final String message                   = data.get("message") != null ? data.get("message").toString() : null;

			if (frames.isEmpty()) {
				continue;
			}

			final String key  = frames.stream().map(f -> f.get("k") + "|" + f.get("id") + "|" + f.get("n") + "|" + f.get("r")).collect(Collectors.joining(">")) + "#" + message;
			final Trace trace = traces.computeIfAbsent(key, k -> new Trace(frames, message));

			trace.count++;
			trace.last = Math.max(trace.last, event.absoluteTimestamp());
		}

		// the elements the code of each frame may be shown as, asked once for every id
		final Set<String> ids = new LinkedHashSet<>();

		for (final Trace trace : traces.values()) {

			for (final Map<String, Object> frame : trace.frames) {

				if (frame.get("id") != null) {
					ids.add(frame.get("id").toString());
				}
			}
		}

		final Map<String, Object> located = ApplicationMap.locate(app, ids);
		final List<Object> list           = new ArrayList<>();

		for (final Trace trace : traces.values().stream().sorted(Comparator.comparingLong((Trace t) -> t.last).reversed()).limit(MAX_TRACES).toList()) {

			final List<Object> frames = new ArrayList<>();

			for (final Map<String, Object> frame : trace.frames) {

				final Map<String, Object> shown = new LinkedHashMap<>(frame);
				final Object id                 = shown.remove("id");

				shown.put("at", id != null ? located.getOrDefault(id.toString(), List.of(id.toString())) : List.of());
				frames.add(shown);
			}

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("m", trace.message);
			map.put("c", trace.count);
			map.put("t", trace.last);
			map.put("f", frames);

			list.add(map);
		}

		final GraphObjectMap result = new GraphObjectMap();

		result.put(new GenericProperty<>("traces"), list);

		return result;
	}

	// ----- private methods -----
	/**
	 * The frames of a scripting error, the failing one first: those of its trace, or, when it was not called from
	 * anywhere, the failing code the event names.
	 */
	private static List<Map<String, Object>> framesOf(final Map<String, Object> data) {

		final List<Map<String, Object>> frames = new ArrayList<>();

		if (data.get("trace") instanceof List<?> trace) {

			for (final Object item : trace) {

				if (item instanceof Map<?, ?> frame) {
					frames.add(frame(frame.get("kind"), frame.get("name"), frame.get("description"), frame.get("id"), frame.get("row"), frame.get("column"), frame.get("method")));
				}
			}

		} else if (data.get("id") != null || data.get("name") != null) {

			frames.add(frame("SCRIPT", data.get("name"), null, data.get("id"), data.get("row"), data.get("column"), null));
		}

		return frames;
	}

	private static Map<String, Object> frame(final Object kind, final Object name, final Object description, final Object id, final Object row, final Object column, final Object method) {

		final Map<String, Object> frame = new LinkedHashMap<>();

		frame.put("k", Objects.toString(kind, null));
		frame.put("n", Objects.toString(name, null));

		putIfNotNull(frame, "d",  description);
		putIfNotNull(frame, "id", id);
		putIfNotNull(frame, "r",  row);
		putIfNotNull(frame, "c",  column);
		putIfNotNull(frame, "m",  method);

		return frame;
	}

	private static void putIfNotNull(final Map<String, Object> map, final String key, final Object value) {

		if (value != null) {
			map.put(key, value);
		}
	}

	private static class Trace {

		private final List<Map<String, Object>> frames;
		private final String message;
		private int count = 0;
		private long last = 0L;

		private Trace(final List<Map<String, Object>> frames, final String message) {

			this.frames  = frames;
			this.message = message;
		}
	}
}
