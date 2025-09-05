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
package org.structr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 *
 */
public class StreamReader extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(StreamReader.class.getName());

	private final Queue<String> queue = new ConcurrentLinkedQueue<>();
	private BufferedReader reader     = null;
	private AtomicBoolean running     = null;

	public StreamReader(final InputStream is, final AtomicBoolean running) {

		super("StreamReader");

		this.reader  = new BufferedReader(new InputStreamReader(is));
		this.running = running;

		this.setDaemon(true);
	}

	public String getBuffer() {

		final StringBuilder buf = new StringBuilder();
		for (final String line : queue) {

			buf.append(line);
			buf.append("\n");
		}

		return buf.toString();
	}

	@Override
	public void run() {

		while (running.get()) {

			try {

				String line = null;

				do {
					line = reader.readLine();
					if (line != null) {

						queue.add(line);
					}

				} while (line != null);

				Thread.sleep(10);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}
	}
}
