/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.function;

import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyFileContentsFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_COPY_FILE_CONTENTS = "Usage: ${ copy_file_contents(sourceFile, targetFile) }";
	public static final String ERROR_MESSAGE_COPY_FILE_CONTENTS_JS = "Usage: ${{ Structr.copy_file_contents(sourceFile, targetFile); }}";

	@Override
	public String getName() {
		return "copy_file_contents";
	}

	@Override
	public String getSignature() {
		return "sourceFile, destinationFile";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final Object toCopy       = sources[0];
			final Object toBeReplaced = sources[1];

			if (toCopy instanceof NodeInterface source && toBeReplaced instanceof NodeInterface target && source.is("File") && target.is("File")) {

				File nodeToCopy       = source.as(File.class);
				File nodeToBeReplaced = target.as(File.class);

				try (final InputStream is = StorageProviderFactory.getStorageProvider(nodeToCopy).getInputStream(); final OutputStream os = StorageProviderFactory.getStorageProvider(nodeToBeReplaced).getOutputStream()) {

					IOUtils.copy(is, os);

					final PropertyKey<Integer> versionKey = Traits.of("File").key("version");
					final PropertyKey<Long> checksumKey   = Traits.of("File").key("checksum");
					final PropertyKey<Long> sizeKey       = Traits.of("File").key("size");
					final PropertyMap changedProperties   = new PropertyMap();

					changedProperties.put(checksumKey, FileHelper.getChecksum(nodeToBeReplaced));
					changedProperties.put(versionKey, 0);
					changedProperties.put(new StringProperty("contentType"), nodeToCopy.getProperty(new StringProperty("contentType")));

					long fileSize = FileHelper.getSize(nodeToBeReplaced);
					if (fileSize > 0) {

						changedProperties.put(sizeKey, fileSize);
					}

					nodeToBeReplaced.unlockSystemPropertiesOnce();
					nodeToBeReplaced.setProperties(nodeToBeReplaced.getSecurityContext(), changedProperties);

					return nodeToBeReplaced;

				} catch (IOException | FrameworkException ex) {

					logger.error("Error: Could not copy file due to exception.", ex);
					return "Error: Could not copy file due to exception.";
				}

			} else {

				logger.warn("Error: entities are not instances of File. Parameters: {}", getParametersAsString(sources));
				return "Error: entities are not nodes.";
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_COPY_FILE_CONTENTS_JS : ERROR_MESSAGE_COPY_FILE_CONTENTS);
	}

	@Override
	public String shortDescription() {
		return "Creates a copy of the file content linked to the given File entity and links it to the other File entity.";
	}
}
