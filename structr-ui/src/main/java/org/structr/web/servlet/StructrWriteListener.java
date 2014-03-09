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
package org.structr.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 *
 * @author Axel Morgner
 */
public class StructrWriteListener implements WriteListener {
	
	private static final Logger logger = Logger.getLogger(StructrWriteListener.class.getName());

	private static final int COPY_BUFFER_SIZE = 4096;
	private byte[] buffer = new byte[COPY_BUFFER_SIZE];
	
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

			// read some content into the copy buffer
			int len = content.read(buffer);

			// If we are at EOF then complete
			if (len < 0) {
//				out.flush();
//				
//				try {
//					out.close();
//				} catch (Throwable t) {
//					// ignore
//					logger.log(Level.WARNING, "Could not close servlet output stream");
//				}
//				content.close();
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

}
