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

import org.structr.common.error.ArgumentTypeException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The trailing options argument of schedule().
 *
 * Everything optional lives here instead of in a positional tail, so a caller naming a queue does not
 * have to pass a title and a null callback to reach it. An unsupported key is named rather than
 * dropped: a misspelled queue would otherwise put the job in a queue of its own and nothing would say
 * so.
 */
public class ScheduleOptions {

	/**
	 * The queue the job runs in.
	 *
	 * Each queue runs one job at a time and queues run in parallel, so two jobs that must not touch the
	 * same data at once belong in one queue. A job that names none runs in the default queue, and that
	 * includes a job scheduled from inside another job: a queue is not inherited.
	 */
	public static final String QUEUE = "queue";

	private static final Set<String> ACCEPTED = Set.of(QUEUE);

	private final Map<String, Object> options;

	private ScheduleOptions(final Map<String, Object> options) {

		this.options = options != null ? options : Collections.emptyMap();
	}

	/**
	 * The options object, wherever the caller actually put it.
	 *
	 * The options follow an optional argument, so schedule(fn, { queue: 'imports' }) leaves the object
	 * one position early, where it would otherwise be read as the job title via toString().
	 */
	public static ScheduleOptions fromAnyOf(final Object[] sources, final int index, final int... earlierSlots) {

		for (final int slot : earlierSlots) {

			if (isOptionsAt(sources, slot)) {

				return new ScheduleOptions((Map)sources[slot]);
			}
		}

		if (isOptionsAt(sources, index)) {

			return new ScheduleOptions((Map)sources[index]);
		}

		if (sources != null && sources.length > index) {

			throw new ArgumentTypeException("schedule(): expected an options object as argument " + (index + 1)
				+ ", got " + sources[index].getClass().getSimpleName() + " '" + sources[index] + "'.");
		}

		return new ScheduleOptions(null);
	}

	/** Whether the argument at the given position is an options object rather than a value. */
	public static boolean isOptionsAt(final Object[] sources, final int index) {

		return sources != null && sources.length > index && sources[index] instanceof Map;
	}

	/**
	 * Rejects keys schedule() does not act on.
	 *
	 * An option that is accepted and then ignored cannot be observed from a script: the job runs, and it
	 * runs in the wrong queue.
	 */
	public ScheduleOptions validate() {

		for (final String key : options.keySet()) {

			if (!ACCEPTED.contains(key)) {

				throw new ArgumentTypeException("schedule(): unknown option '" + key + "'. schedule accepts "
					+ String.join(", ", new TreeSet<>(ACCEPTED)) + ".");
			}
		}

		return this;
	}

	public String getQueue() {

		final Object value = options.get(QUEUE);

		return (value != null) ? value.toString() : null;
	}
}
