/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.function;

import java.io.IOException;
import org.python.google.common.io.Files;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.FileBase;
import static org.structr.web.entity.FileBase.checksum;
import static org.structr.web.entity.FileBase.size;
import static org.structr.web.entity.FileBase.version;

public class CopyFileContentsFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_POST = "Usage: ${ copy_file_contents(file1_uuid,file2_uuid) }. Example: ${ copy_file_contents('abcdefgh123','ijklmnop456') }";
	public static final String ERROR_MESSAGE_POST_JS = "Usage: ${{ Structr.copy_file_contentscreate_deep_file_copy(file1_uuid,file2_uuid) }}. Example: ${{ Structr.copy_file_contents('abcdefgh123','ijklmnop456') }}";

	@Override
	public Object apply(ActionContext ctx, GraphObject entity, Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			final Object toCopy = sources[0];
			final Object toBeReplaced = sources[1];

			if (toCopy instanceof FileBase && toBeReplaced instanceof FileBase) {

				FileBase nodeToCopy = (FileBase) toCopy;
				FileBase nodeToBeReplaced = (FileBase) toBeReplaced;

				try {

					java.io.File fileToCopy = nodeToCopy.getFileOnDisk();
					if (!fileToCopy.exists()) {

						logger.warn("Error: Given source file does not exist. Parameters: {}", getParametersAsString(sources));
						return "Error: Given source file does not exist.";

					}

					java.io.File fileToBeReplaced = nodeToBeReplaced.getFileOnDisk();

					if (!fileToBeReplaced.exists()) {

						// Call afterCreation method to ensure that the file is properly initialized.
						nodeToBeReplaced.afterCreation(nodeToBeReplaced.getSecurityContext());

					}

					Files.copy(fileToCopy, fileToBeReplaced);

					final PropertyMap changedProperties = new PropertyMap();
					changedProperties.put(checksum, FileHelper.getChecksum(fileToBeReplaced));
					changedProperties.put(version, 0);
					changedProperties.put(new StringProperty("contentType"), nodeToCopy.getProperty(new StringProperty("contentType")));

					long fileSize = FileHelper.getSize(nodeToBeReplaced);
					if (fileSize > 0) {
						changedProperties.put(size, fileSize);
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

		} else {

			logParameterError(entity, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_POST_JS : ERROR_MESSAGE_POST);
	}

	@Override
	public String shortDescription() {
		return "Creates a copy of the file content linked to the given FileBase entity and links it to the other FileBase entity.";
	}

	@Override
	public String getName() {
		return "copy_file_contents()";
	}

}
