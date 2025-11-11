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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class CreateZipFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "create_zip";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("archiveFileName, files [, password [, encryptionMethod ] ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (sources == null || sources.length < 1 || sources[0] == null || sources[1] == null || !(sources[1] instanceof NodeInterface || sources[1] instanceof Collection)) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		String name             = null;
		String password         = null;

		String encryptionMethodString     = null;
		EncryptionMethod encryptionMethod = EncryptionMethod.ZIP_STANDARD;

		if (sources[0] instanceof String) {

			final String nameParam = sources[0].toString().trim();
			name = nameParam + (nameParam.endsWith(".zip") ? "" : ".zip");

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		if (sources.length > 2 && sources[2] != null && sources[2] instanceof String) {

			password = (String) sources[2];
		}

		if (sources.length > 3 && sources[3] != null && sources[3] instanceof String) {

			encryptionMethodString = (String) sources[3];
			encryptionMethod       = "aes".equalsIgnoreCase(encryptionMethodString) ? EncryptionMethod.AES : EncryptionMethod.ZIP_STANDARD;
		}

		try {

			ZipFile            zipFile = null;
			final ZipParameters params = new ZipParameters();

			if (password != null) {

				params.setEncryptFiles(true);
				params.setEncryptionMethod(encryptionMethod);
				zipFile = new ZipFile(name, password.toCharArray());

			} else {

				zipFile = new ZipFile(name);
			}

			zipFile.setCharset(StandardCharsets.UTF_8);

			if (sources[1] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				File file = n.as(File.class);
				addFileToZipArchive(file.getName(), file, zipFile, params);

			} else if (sources[1] instanceof NodeInterface n && n.is(StructrTraits.FOLDER)) {

				Folder folder = n.as(Folder.class);

				addFilesToArchive(folder.getName() + "/", folder.getFiles(), zipFile, params);
				addFoldersToArchive(folder.getName() + "/", folder.getFolders(), zipFile, params);

			} else if (sources[1] instanceof Collection) {

				final Collection<GraphObject> filesOrFolders = (Collection) sources[1];

				if (filesOrFolders.isEmpty()) {

					logParameterError(caller, sources, "Collection in parameter 1 is empty - unable to create empty zip file.", ctx.isJavaScriptContext());

					return usage(ctx.isJavaScriptContext());

				} else {

					for (GraphObject fileOrFolder : filesOrFolders) {

						if (fileOrFolder.is(StructrTraits.FILE)) {

							final File file = fileOrFolder.as(File.class);

							addFileToZipArchive(file.getName(), file, zipFile, params);

						} else if (fileOrFolder.is(StructrTraits.FOLDER)) {

							final Folder folder = fileOrFolder.as(Folder.class);

							addFilesToArchive(folder.getName() + "/", folder.getFiles(), zipFile, params);
							addFoldersToArchive(folder.getName() + "/", folder.getFolders(), zipFile, params);

						} else {

							logParameterError(caller, sources, ctx.isJavaScriptContext());
							return usage(ctx.isJavaScriptContext());
						}
					}
				}

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}

			return FileHelper.createFile(ctx.getSecurityContext(), zipFile.getFile(), "application/zip", name);

		} catch (IOException e) {

			logException(caller, e, sources);
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${create_zip(archiveFileName, files [, password [, encryptionMethod ] ])}. Example: ${create_zip('archive', find('File'))}"),
			Usage.javaScript("Usage: ${{Structr.createZip(archiveFileName, files [, password [, encryptionMethod ] ])}}. Example: ${{Structr.createZip('archive', Structr.find('File'))}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Create a ZIP archive file with the given files and folders.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	private void addFileToZipArchive(final String path, final File file, final ZipFile zipFile, final ZipParameters params) throws IOException {

		logger.info("Adding File \"{}\" to new ZIP archive...", path);
		params.setFileNameInZip(path);
		zipFile.addStream(file.getInputStream(), params);
	}

	private void addFilesToArchive(final String path, final Iterable<File> list, final ZipFile zipFile, final ZipParameters params) throws IOException {

		for (final File fileForArchive : list) {

			params.setFileNameInZip(fileForArchive.getName());
			addFileToZipArchive(path + fileForArchive.getName(), fileForArchive, zipFile, params);
		}
	}

	private void addFoldersToArchive(final String path, final Iterable<Folder> list, final ZipFile zipFile, final ZipParameters params) throws IOException {

		for (final Folder folder : list) {

			addFilesToArchive(path + folder.getName() + "/", folder.getFiles(), zipFile, params);
			addFoldersToArchive(path + folder.getName() + "/", folder.getFolders(), zipFile, params);
		}
	}
}
