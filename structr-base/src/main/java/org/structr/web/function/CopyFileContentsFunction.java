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
package org.structr.web.function;

import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.FileTraitDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class CopyFileContentsFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "copy_file_contents";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("sourceFile, destinationFile");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final Object toCopy       = sources[0];
			final Object toBeReplaced = sources[1];

			if (toCopy instanceof NodeInterface source && toBeReplaced instanceof NodeInterface target && source.is(StructrTraits.FILE) && target.is(StructrTraits.FILE)) {

				File nodeToCopy       = source.as(File.class);
				File nodeToBeReplaced = target.as(File.class);

				try (final InputStream is = StorageProviderFactory.getStorageProvider(nodeToCopy).getInputStream(); final OutputStream os = StorageProviderFactory.getStorageProvider(nodeToBeReplaced).getOutputStream()) {

					IOUtils.copy(is, os);

					final PropertyKey<Integer> versionKey    = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.VERSION_PROPERTY);
					final PropertyKey<Long> checksumKey      = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CHECKSUM_PROPERTY);
					final PropertyKey<Long> sizeKey          = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.SIZE_PROPERTY);
					final PropertyKey<String> contentTypeKey = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CONTENT_TYPE_PROPERTY);
					final PropertyMap changedProperties      = new PropertyMap();

					changedProperties.put(checksumKey, FileHelper.getChecksum(nodeToBeReplaced));
					changedProperties.put(versionKey, 0);
					changedProperties.put(contentTypeKey, nodeToCopy.getProperty(contentTypeKey));

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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${ copy_file_contents(sourceFile, targetFile) }"),
			Usage.javaScript("Usage: ${{ Structr.copyFileContents(sourceFile, targetFile); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Copies the content of sourceFile to targetFile and updates the meta-data accordingly.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("sourceFile", "source file to copy content from"),
			Parameter.mandatory("targetFile", "target file to copy content to")
		);
	}
}
