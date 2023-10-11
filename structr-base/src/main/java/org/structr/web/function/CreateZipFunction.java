/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

public class CreateZipFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_CREATE_ZIP    = "Usage: ${create_zip(archiveFileName, files [, password [, encryptionMethod ] ])}. Example: ${create_zip(\"archive\", find(\"File\"))}";
	public static final String ERROR_MESSAGE_CREATE_ZIP_JS = "Usage: ${{Structr.createZip(archiveFileName, files [, password [, encryptionMethod ] ])}}. Example: ${{Structr.createZip(\"archive\", Structr.find(\"File\"))}}";

	@Override
	public String getName() {
		return "create_zip";
	}

	@Override
	public String getSignature() {
		return "archiveFileName, files [, password [, encryptionMethod ] ]";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (sources == null || sources.length < 1 || sources[0] == null || sources[1] == null || !(sources[1] instanceof File || sources[1] instanceof Folder || sources[1] instanceof Collection)) {

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
			encryptionMethod       = "aes".equals(encryptionMethodString.toLowerCase()) ? EncryptionMethod.AES : EncryptionMethod.ZIP_STANDARD;
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

			zipFile.setCharset(Charset.forName("UTF-8"));

			if (sources[1] instanceof File) {

				File file = (File) sources[1];
				addFileToZipArchive(file.getProperty(AbstractFile.name), file, zipFile, params);

			} else if (sources[1] instanceof Folder) {

				Folder folder = (Folder) sources[1];

				addFilesToArchive(folder.getProperty(Folder.name) + "/", folder.getFiles(), zipFile, params);
				addFoldersToArchive(folder.getProperty(Folder.name) + "/", folder.getFolders(), zipFile, params);

			} else if (sources[1] instanceof Collection) {

				final Collection filesOrFolders = (Collection) sources[1];

				if (filesOrFolders.isEmpty()) {

					logParameterError(caller, sources, "Collection in parameter 1 is empty - unable to create empty zip file.", ctx.isJavaScriptContext());

					return usage(ctx.isJavaScriptContext());

				} else {

					for (Object fileOrFolder : filesOrFolders) {

						if (fileOrFolder instanceof File) {

							File file = (File) fileOrFolder;
							addFileToZipArchive(file.getProperty(AbstractFile.name), file, zipFile, params);

						} else if (fileOrFolder instanceof Folder) {

							Folder folder = (Folder) fileOrFolder;
							addFilesToArchive(folder.getProperty(Folder.name) + "/", folder.getFiles(), zipFile, params);
							addFoldersToArchive(folder.getProperty(Folder.name) + "/", folder.getFolders(), zipFile, params);

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
	public String usage(boolean inJavaScriptContext) {

		return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_ZIP_JS : ERROR_MESSAGE_CREATE_ZIP);
	}

	@Override
	public String shortDescription() {

		return "Create a ZIP archive file with the given files and folders.";
	}

	private void addFileToZipArchive(final String path, final File file, final ZipFile zipFile, final ZipParameters params) throws IOException {

		logger.info("Adding File \"{}\" to new ZIP archive...", path);
		params.setFileNameInZip(path);
		zipFile.addStream(file.getInputStream(), params);
	}

	private void addFilesToArchive(final String path, final Iterable<File> list, final ZipFile zipFile, final ZipParameters params) throws IOException {

		for (final File fileForArchive : list) {

			params.setFileNameInZip(fileForArchive.getName());
			addFileToZipArchive(path + fileForArchive.getProperty(AbstractFile.name), fileForArchive, zipFile, params);
		}
	}

	private void addFoldersToArchive(final String path, final Iterable<Folder> list, final ZipFile zipFile, final ZipParameters params) throws IOException {

		for (final Folder folder : list) {

			addFilesToArchive(path + folder.getProperty(Folder.name) + "/", folder.getFiles(), zipFile, params);
			addFoldersToArchive(path + folder.getProperty(Folder.name) + "/", folder.getFolders(), zipFile, params);
		}
	}
}