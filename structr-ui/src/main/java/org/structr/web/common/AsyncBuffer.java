/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * Special buffer for asynchronous streaming of chunked output.
 *
 * @author Axel Morgner
 */
public class AsyncBuffer implements WriteListener {

	private static final Logger logger = Logger.getLogger(AsyncBuffer.class.getName());

	private final Queue<String> queue = new LinkedList<>();

	private AsyncContext async;
	private ServletOutputStream out;

	private boolean completed = false;

	public void prepare(final AsyncContext async, final ServletOutputStream out) {

		this.async = async;
		this.out = out;

		if (async != null) {
			out.setWriteListener(this);
		}
	}

	public void flush() {

		try {

			write();

		} catch (IOException ex) {

			Logger.getLogger(AsyncBuffer.class.getName()).log(Level.SEVERE, null, ex);

		}

	}

	private void write() throws IOException {

		// Write to client if buffer is filled and client is ready
		while (async == null || out.isReady()) {

			String s = queue.poll();

			if (s == null) {
				out.flush();
				return;
			}

			if (!completed) {
				// write out the copy buffer.  
				out.write(s.getBytes("UTF-8"));
			}

		}

	}

	public void finish() {

		flush();
		completed = true;
		if (async != null) {
			async.complete();
		}

	}

	@Override
	public void onWritePossible() throws IOException {

		write();

	}

	@Override
	public void onError(Throwable t) {
		logger.log(Level.FINE, "Async error", t);
		if (async != null) {
			async.complete();
		}
	}

	public AsyncBuffer append(final String s) {
		queue.add(s);
		return this;
	}

}
