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

	// the most characters getTail returns, not counting the line about omitted lines
	public static final int LOGGED_CHARS = 8_192;

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

	/**
	 * The last lines of what was read, for the log: a process can write far more than a log entry should
	 * hold, and the cause of an error is usually at the end. At most {@value #LOGGED_CHARS} characters
	 * are kept, so a few very long lines cannot flood the log either.
	 */
	public String getTail(final int lines) {

		final String[] all      = queue.toArray(new String[0]);
		final StringBuilder buf = new StringBuilder();
		int first               = all.length;
		int chars               = 0;

		while (first > 0 && all.length - first < lines && chars + all[first - 1].length() + 1 <= LOGGED_CHARS) {

			chars += all[--first].length() + 1;
		}

		// a last line longer than the limit is kept, cut to its end
		final boolean cut = first == all.length && first > 0 && lines > 0;

		if (cut) {

			first--;
		}

		if (first > 0) {

			buf.append("(").append(first).append(" earlier lines omitted)\n");
		}

		for (int i = first; i < all.length; i++) {

			buf.append(cut ? all[i].substring(all[i].length() - LOGGED_CHARS) : all[i]);
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
