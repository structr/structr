/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class CreateArchiveFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "createArchive";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("fileName, files [, customFileTypeName ]");
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

			if (sources[1] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				File file = n.as(File.class);
				addFileToZipArchive(file.getName(), file, zaps);

			} else if (sources[1] instanceof NodeInterface n && n.is(StructrTraits.FOLDER)) {

				Folder folder = n.as(Folder.class);
				addFilesToArchive(folder.getName() + "/", folder.getFiles(), zaps);
				addFoldersToArchive(folder.getName() + "/", folder.getFolders(), zaps);

			} else 	if (sources[1] instanceof Iterable) {

				final Iterable<GraphObject> coll = (Iterable)sources[1];

				for (GraphObject fileOrFolder : coll) {

					if (fileOrFolder.is(StructrTraits.FILE)) {

						File file = fileOrFolder.as(File.class);
						addFileToZipArchive(file.getName(), file, zaps);

					} else if (fileOrFolder.is(StructrTraits.FOLDER)) {

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

				archiveClass = StructrTraits.FILE;
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${createArchive(archiveFileName, files [, customFileType])}. Example: ${createArchive('archive', find('File'))}"),
			Usage.javaScript("Usage: ${{Structr.createArchive(archiveFileName, files [, customFileType])}}. Example: ${{Structr.createArchive('archive', Structr.find('File'))}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates and returns a ZIP archive with the given files (and folders).";
	}

	@Override
	public String getLongDescription() {
		return "This function creates a ZIP archive with the given files and folder and stores it as a File with the given name in Structr's filesystem. The second parameter can be either a single file, a single folder or a list of files and folders, but all of the objects must be Structr entities. The third parameter can be used to set the node type of the resulting archive to something other than `File`, although the given type must be a subtype of `File`.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("archiveFileName", "name of the resulting archive (without the .zip suffix)"),
			Parameter.mandatory("filesOrFolders", "file, folder or list thereof to add to the archive"),
			Parameter.optional("customFileType", "custom archive type other than `File` (must be a subtype of `File`)")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${createArchive('logs', find('Folder', 'name', 'logs'))}", "Create an archive named `logs.zip` with the contents of all Structr Folders named \"logs\""),
			Example.javaScript("""
			${{
				// find a single folder with an absolute path
				let folders = $.find('Folder', { path: '/data/logs' }));
				if (folders.length > 0) {

					// use the first folder here
					let archive = $.createArchive('logs', folders[0]);
				}
			}}
			""", "Create an archive named `logs.zip` with the contents of exactly one Structr Folder"),
			Example.javaScript("""
			${{
				// find all the folders with the name "logs"
				let folders = $.find('Folder', { name: 'logs' }));
				let archive = $.createArchive('logs', folders);
			}}
			""", "Create an archive named `logs.zip` with the contents of all Structr Folders named \\\"logs\\\""),
			Example.javaScript("""
			${{
				let parentFolder = $.getOrCreate('Folder', { name: 'archives' });
				let files        = $.methodParameters.files;
				let name         = $.methodParameters.name;

				let archive = $.createArchive(name, files);

				archive.parent = parentFolder;
			}}
			""", "Create an archive and put it in a specific parent folder")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The resulting file will be named `archiveFileName` + `.zip` and will be put in the root folder of the structr filesystem.",
			"The second parameter can be a single file, a collection of files, a folder, a collection of folders or a mixture.",
			"If you use the result of a `find()` call to collect the files and folder for the archive, please note that there can be multiple folders with the same name that might end up in the archive.",
			"You can set the destination folder of the created archive by setting the `parent` property of the returned entity."
		);
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
