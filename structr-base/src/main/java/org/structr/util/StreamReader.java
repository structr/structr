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
package org.structr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 *
 */
public class StreamReader extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(StreamReader.class.getName());

	private final Queue<String> queue = new ConcurrentLinkedQueue<>();
	private BufferedReader reader     = null;

	public StreamReader(final InputStream is) {

		super("StreamReader");

		this.reader = new BufferedReader(new InputStreamReader(is));

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

		// until the process closes the stream, so nothing it wrote before exiting is lost
		try {

			String line = null;

			while ((line = reader.readLine()) != null) {

				queue.add(line);
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}
}
