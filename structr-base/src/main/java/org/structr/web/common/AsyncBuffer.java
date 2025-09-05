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
package org.structr.web.common;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Special buffer for asynchronous streaming of chunked output.
 *
 *
 */
public class AsyncBuffer {

	private final Queue<String> queue = new ArrayDeque<>(1000);

	public AsyncBuffer append(final String s) {

		synchronized(queue) {

			if (s != null) {
				queue.add(s);
			}
		}

		return this;
	}

	public Queue<String> getQueue() {
		return queue;
	}
}
