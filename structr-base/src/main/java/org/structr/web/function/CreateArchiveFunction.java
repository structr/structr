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

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class CreateArchiveFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_CREATE_ARCHIVE    = "Usage: ${create_archive(archiveFileName, files [, CustomFileType])}. Example: ${create_archive(\"archive\", find(\"File\"))}";
	public static final String ERROR_MESSAGE_CREATE_ARCHIVE_JS = "Usage: ${{Structr.createArchive(archiveFileName, files [, CustomFileType])}}. Example: ${{Structr.createArchive(\"archive\", Structr.find(\"File\"))}}";

	@Override
	public String getName() {
		return "create_archive";
	}

	@Override
	public String getSignature() {
		return "fileName, files [, customFileTypeName ]";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (!(sources[1] instanceof NodeInterface || sources[1] instanceof Collection || sources.length < 2)) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		try {

			java.io.File newArchive = java.io.File.createTempFile(sources[0].toString(), "zip");

			ZipArchiveOutputStream zaps = new ZipArchiveOutputStream(newArchive);
			zaps.setEncoding("UTF8");
			zaps.setUseLanguageEncodingFlag(true);
			zaps.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
			zaps.setFallbackToUTF8(true);

			if (sources[1] instanceof NodeInterface n && n.is("File")) {

				File file = n.as(File.class);
				addFileToZipArchive(file.getName(), file, zaps);

			} else if (sources[1] instanceof NodeInterface n && n.is("Folder")) {

				Folder folder = n.as(Folder.class);
				addFilesToArchive(folder.getName() + "/", folder.getFiles(), zaps);
				addFoldersToArchive(folder.getName() + "/", folder.getFolders(), zaps);

			} else 	if (sources[1] instanceof Collection) {

				final Collection<GraphObject> coll = (Collection)sources[1];

				for (GraphObject fileOrFolder : coll) {

					if (fileOrFolder.is("File")) {

						File file = fileOrFolder.as(File.class);
						addFileToZipArchive(file.getName(), file, zaps);

					} else if (fileOrFolder.is("folder")) {

						Folder folder = fileOrFolder.as(Folder.class);
						addFilesToArchive(folder.getName(), folder.getFiles(), zaps);
						addFoldersToArchive(folder.getName(), folder.getFolders(), zaps);

					} else {

						logParameterError(caller, sources, ctx.isJavaScriptContext());
						return usage(ctx.isJavaScriptContext());
					}
				}
			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}

			zaps.close();

			String archiveClass = null;

			if (sources.length > 2) {

				archiveClass = sources[2].toString();

			}

			if (archiveClass == null) {

				archiveClass = "File";
			}

			try (final FileInputStream fis = new FileInputStream(newArchive)) {
				return FileHelper.createFile(ctx.getSecurityContext(), fis, "application/zip", archiveClass, sources[0].toString() + ".zip");
			}

		} catch (IOException e) {

			logException(caller, e, sources);
		}
		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {

		return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_ARCHIVE_JS : ERROR_MESSAGE_CREATE_ARCHIVE);
	}

	@Override
	public String shortDescription() {

		return "Packs the given files and folders into zipped archive.";
	}

	private void addFileToZipArchive(final String path, final File file, final ArchiveOutputStream aps) throws IOException {

		logger.info("Adding File \"{}\" to new archive...", path);

		ZipArchiveEntry entry = new ZipArchiveEntry(path);
		aps.putArchiveEntry(entry);

		try (final InputStream in = file.getInputStream()) {

			IOUtils.copy(in, aps);
		}

		aps.closeArchiveEntry();
	}

	private void addFilesToArchive(final String path, final Iterable<File> list, final ArchiveOutputStream aps) throws IOException {

		for (final File fileForArchive : list) {

			addFileToZipArchive(path + fileForArchive.getName(), fileForArchive,  aps);
		}
	}

	private void addFoldersToArchive(final String path, final Iterable<Folder> list, final ArchiveOutputStream aps) throws IOException {

		for (final Folder folder : list) {

			addFilesToArchive(path + folder.getName(), folder.getFiles(), aps);
			addFoldersToArchive(path + folder.getName(), folder.getFolders(), aps);
		}
	}
}