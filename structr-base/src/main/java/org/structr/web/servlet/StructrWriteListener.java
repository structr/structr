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
package org.structr.web.servlet;


import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;


/**
 *
 *
 */
public class StructrWriteListener implements WriteListener {
	
	private static final Logger logger = LoggerFactory.getLogger(StructrWriteListener.class.getName());

	private static final int COPY_BUFFER_SIZE = 4096;
	private final byte[] buffer = new byte[COPY_BUFFER_SIZE];
	
	private final InputStream content;
	private final AsyncContext async;
	private final ServletOutputStream out;

	public StructrWriteListener(final InputStream content, final AsyncContext async, final ServletOutputStream out) {
		this.content = content;
		this.async = async;
		this.out = out;
	}

	@Override
	public void onWritePossible() throws IOException {

		// while we are able to write without blocking
		while (out.isReady()) {

			// Read content into the output buffer
			int len = content.read(buffer);

			// EOF?
			if (len < 0) {
				async.complete();
				return;
			}

			// write out the copy buffer.  
			out.write(buffer, 0, len);
		}
	}

	@Override
	public void onError(Throwable t) {
		logger.error("Async error", t);
		async.complete();
	}

}
