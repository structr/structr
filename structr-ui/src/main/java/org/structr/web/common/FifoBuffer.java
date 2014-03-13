/*
 *  Copyright (C) 2014 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class FifoBuffer extends InputStream implements WriteListener, Appendable {

	private static final Logger logger = Logger.getLogger(FifoBuffer.class.getName());

	private static final int COPY_BUFFER_SIZE = 4096;
	private byte[] buffer = new byte[COPY_BUFFER_SIZE];
	
	private final StringBuilder sb = new StringBuilder(8192);
	private ByteArrayInputStream content    = null;

	private AsyncContext async;
	private ServletOutputStream out;

	public void prepare(final AsyncContext async, final ServletOutputStream out) {
		this.async = async;
		this.out = out;
	}

	public void flush() {
		
		try {
			
			content = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));

		} catch (IOException ex) {

			Logger.getLogger(FifoBuffer.class.getName()).log(Level.SEVERE, null, ex);

		}

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
		logger.log(Level.SEVERE, "Async error", t);
		async.complete();
	}
	
	@Override
	public int read() throws IOException {
		return (content != null ? content.read() : -1);
	}

	@Override
	public FifoBuffer append(CharSequence csq) {
		sb.append(csq);
		return this;
	}

	@Override
	public FifoBuffer append(CharSequence csq, int start, int end) {
		sb.append(csq, start, end);
		return this;
	}

	@Override
	public FifoBuffer append(char c) {
		sb.append(c);
		return this;
	}

}
