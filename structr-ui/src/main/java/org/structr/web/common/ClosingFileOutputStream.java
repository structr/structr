/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.common;

import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;

/**
 */
public class ClosingFileOutputStream extends FileOutputStream {

	private static final Logger logger = LoggerFactory.getLogger(ClosingFileOutputStream.class);

	private boolean notifyIndexerAfterClosing = false;
	private boolean closed                    = false;
	private File thisFile                     = null;
	private java.io.File file                 = null;

	public ClosingFileOutputStream(final File thisFile, final boolean append, final boolean notifyIndexerAfterClosing) throws IOException {

		super(thisFile.getFileOnDisk(), append);

		this.notifyIndexerAfterClosing = notifyIndexerAfterClosing;
		this.thisFile                  = thisFile;
		this.file                      = thisFile.getFileOnDisk();
	}

	@Override
	public void close() throws IOException {

		if (closed) {
			return;
		}

		try (Tx tx = StructrApp.getInstance().tx()) {

			super.close();

			final String _contentType           = FileHelper.getContentMimeType(thisFile);
			final PropertyMap changedProperties = new PropertyMap();

			changedProperties.put(StructrApp.key(File.class, "checksum"),     FileHelper.getChecksum(file));
			changedProperties.put(StructrApp.key(File.class, "size"),         file.length());
			changedProperties.put(StructrApp.key(File.class, "contentType"), _contentType);

			if (StringUtils.startsWith(_contentType, "image") || ImageHelper.isImageType(thisFile.getName())) {
				changedProperties.put(NodeInterface.type, Image.class.getSimpleName());
			}

			thisFile.unlockSystemPropertiesOnce();
			thisFile.setProperties(thisFile.getSecurityContext(), changedProperties);

			thisFile.increaseVersion();

			if (notifyIndexerAfterClosing) {
				thisFile.notifyUploadCompletion();
			}

			tx.success();

		} catch (Throwable ex) {

			logger.error("Could not determine or save checksum and size after closing file output stream", ex);

		}

		closed = true;
	}
}
