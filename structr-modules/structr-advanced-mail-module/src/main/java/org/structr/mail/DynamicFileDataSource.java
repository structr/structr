/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.mail;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.web.entity.File;

import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;


/**
 * Data Source for dynamic files which has two purposes
 * 1. Only generate the dynamic file content once (the getInputStream() method is called twice when sending an email. once to determine the Content-Transfer-Encoding and once for the actual content)
 * 2. Generate the dynamic file content at attachment time so the currently store'd variables are used and can be overwritten without losing the file contents
 */
public class DynamicFileDataSource implements DataSource {

	private static final Logger logger = LoggerFactory.getLogger(DynamicFileDataSource.class);

	private final String contentType;
	private final String fileName;
	private String fileContent;
	private String encoding = "UTF-8";

	public DynamicFileDataSource (final File fileNode) {

		contentType = fileNode.getContentType();
		fileName    = fileNode.getName();

		if (contentType != null) {

			final String charset = StringUtils.substringAfterLast(contentType, "charset=").trim().toUpperCase();
			try {
				if (!"".equals(charset) && Charset.isSupported(charset)) {
					encoding = charset;
				}
			} catch (IllegalCharsetNameException ice) {
				logger.warn("Charset is not supported '{}'. Using 'UTF-8'", charset);
			}
		}

		try {
			fileContent = IOUtils.toString(fileNode.getInputStream(), encoding);
		} catch (IOException ex) {
			logger.warn("Unable to open input stream for {}: {}", fileName, ex.getMessage());
			fileContent = "";
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return IOUtils.toInputStream(fileContent, encoding);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("Writing to dynamic file is not allowed.");
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getName() {
		return fileName;
	}
}
