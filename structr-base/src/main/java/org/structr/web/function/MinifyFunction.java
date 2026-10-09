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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.PathHelper;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;
import org.structr.web.common.Minifier;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class MinifyFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "minify";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file [, outputPath ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final SecurityContext securityContext = ctx.getSecurityContext();
			final File source                     = resolveSource(securityContext, sources[0]);
			final String sourcePath               = source.getPath();
			final String contentType              = source.getContentType();
			final Minifier.Kind kind              = Minifier.detect(contentType, source.getName());

			if (kind == null) {

				throw new FrameworkException(422, "Unable to minify " + sourcePath + ": unsupported content type " + contentType + ", supported are JavaScript, CSS, HTML, SVG, XML and JSON");
			}

			final String outputPath = resolveOutputPath(sourcePath, sources.length > 1 && sources[1] != null ? sources[1].toString() : null);
			if (outputPath.equals(sourcePath)) {

				throw new FrameworkException(422, "Unable to minify " + sourcePath + ": the output path must differ from the source path");
			}

			final Charset charset = charsetOf(contentType);
			final String content;

			try (final InputStream is = source.getInputStream()) {

				content = new String(is.readAllBytes(), charset);
			}

			final byte[] minified = Minifier.minify(kind, content, source.getName()).getBytes(charset);
			final String type     = StringUtils.isNotBlank(contentType) ? contentType : kind.getContentType();

			return write(securityContext, outputPath, minified, type);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		} catch (IOException ex) {

			throw new FrameworkException(500, "Unable to minify: " + ex.getMessage());
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${minify(file [, outputPath ])}. Example: ${minify('/js/app.js')}"),
			Usage.javaScript("Usage: ${{ $.minify(file [, outputPath ]) }}. Example: ${{ $.minify('/js/app.js') }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Writes a minified copy of a JavaScript, CSS, HTML, SVG, XML or JSON file and returns the new file.";
	}

	@Override
	public String getLongDescription() {

		return """
		The type of the file is detected from its content type, or from its extension when the content type is missing or not specific, and decides how the file is minified: JavaScript with terser, CSS with csso, HTML, SVG and XML by removing comments and redundant whitespace, JSON by removing all whitespace. Inline scripts and styles in HTML are minified as well.

		Without `outputPath`, the minified file is written next to the source with `.min` before the extension, so `/js/app.js` becomes `/js/app.min.js`. A relative `outputPath` is resolved against the folder of the source file. Missing folders are created, and an existing file at the output path is overwritten, so calling `minify()` again after changing the source updates the minified file.

		A file that cannot be parsed is not written; the error names the problem, for JavaScript with its line and column.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("file", "file to minify, as a file object or as its absolute path"),
			Parameter.optional("outputPath", "path of the minified file, absolute or relative to the folder of the source file")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("$.minify('/js/app.js')", "Writes /js/app.min.js"),
			Example.javaScript("$.minify($.first($.find('File', 'name', 'styles.css')), '/dist/styles.css')", "Writes the minified stylesheet to /dist/styles.css")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	// ----- private methods -----
	private File resolveSource(final SecurityContext securityContext, final Object value) throws FrameworkException {

		final NodeInterface node = value instanceof NodeInterface n ? n : FileHelper.getFileByAbsolutePath(securityContext, value.toString());

		if (node == null || !node.is(StructrTraits.FILE)) {

			throw new FrameworkException(422, "Unable to minify " + value + ": not a file");
		}

		return node.as(File.class);
	}

	private String resolveOutputPath(final String sourcePath, final String outputPath) {

		if (StringUtils.isBlank(outputPath)) {

			final String name = PathHelper.getName(sourcePath);
			final int dot     = name.lastIndexOf('.');
			final String min  = dot > 0 ? name.substring(0, dot) + ".min" + name.substring(dot) : name + ".min";

			return join(PathHelper.getFolderPath(sourcePath), min);
		}

		if (outputPath.startsWith(PathHelper.PATH_SEP)) {

			return PathHelper.PATH_SEP + PathHelper.clean(outputPath);
		}

		return join(PathHelper.getFolderPath(sourcePath), outputPath);
	}

	private String join(final String folder, final String path) {

		return PathHelper.PATH_SEP + PathHelper.clean(folder + PathHelper.PATH_SEP + path);
	}

	private File write(final SecurityContext securityContext, final String path, final byte[] data, final String contentType) throws FrameworkException, IOException {

		final NodeInterface existing = FileHelper.getFileByAbsolutePath(securityContext, path);
		if (existing != null) {

			if (!existing.is(StructrTraits.FILE)) {

				throw new FrameworkException(422, "Unable to minify: " + path + " exists and is not a file");
			}

			final File file = existing.as(File.class);

			FileHelper.setFileData(file, data, contentType);

			return file;
		}

		final String folderPath = PathHelper.getFolderPath(path);
		final Folder folder     = PathHelper.PATH_SEP.equals(folderPath) ? null : asFolder(FileHelper.createFolderPath(securityContext, folderPath), folderPath);

		return FileHelper.createFile(securityContext, new ByteArrayInputStream(data), contentType, StructrTraits.FILE, PathHelper.getName(path), folder).as(File.class);
	}

	private Folder asFolder(final NodeInterface node, final String path) throws FrameworkException {

		if (node == null || !node.is(StructrTraits.FOLDER)) {

			throw new FrameworkException(422, "Unable to minify: " + path + " is not a folder");
		}

		return node.as(Folder.class);
	}

	private Charset charsetOf(final String contentType) {

		final String parameter = StringUtils.substringAfter(StringUtils.defaultString(contentType).toLowerCase(Locale.ROOT), "charset=");
		final String name      = StringUtils.substringBefore(parameter, ";").replace("\"", "").trim();

		try {

			return name.isEmpty() ? StandardCharsets.UTF_8 : Charset.forName(name);

		} catch (IllegalArgumentException ex) {

			return StandardCharsets.UTF_8;
		}
	}
}
