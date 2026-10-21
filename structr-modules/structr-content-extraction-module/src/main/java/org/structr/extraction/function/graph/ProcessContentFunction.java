/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.function.graph;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.processing.ProcessingResult;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.filesystem.FolderTreeWalker;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessContentFunction extends ContentExtractionFunction {

	private static final String FORCE     = "force";
	private static final String QUEUE     = "queue";
	private static final String RECURSIVE = "recursive";
	private static final String WAIT      = "wait";

	private static final Set<String> ACCEPTED = accepted();

	@Override
	public String getName() {

		return NAMESPACE + ".processContent";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("fileOrFolderOrList [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();
		final Map<String, Object> options     = optionsArgument(sources, 1, ACCEPTED);
		final boolean recursive               = booleanOption(options, RECURSIVE, true);
		final boolean force                   = booleanOption(options, FORCE, false);
		final boolean wait                    = booleanOption(options, WAIT, false);
		final String queue                    = options.containsKey(QUEUE) ? stringOption(options, QUEUE) : null;
		final Map<String, Object> overrides   = new HashMap<>(options);
		final List<File> files                = files(sources, recursive);

		overrides.keySet().removeAll(Set.of(FORCE, QUEUE, RECURSIVE, WAIT));

		for (final File file : files) {

			if (!file.isGranted(Permission.write, securityContext)) {

				throw new FrameworkException(403, getName() + "(): processing " + file.getPath() + " requires write permission on the file.");
			}
		}

		if (files.isEmpty()) {

			return wait ? List.of() : null;
		}

		try {

			// check the options once now, instead of failing for every file in the job
			ContentOptionsResolver.resolve(files.getFirst(), overrides);

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getName() + "(): " + ex.getMessage());
		}

		if (wait) {

			final List<Map<String, Object>> results = new ArrayList<>();

			for (final File file : files) {

				final ProcessingResult result = ContentExtraction.processNow(file, overrides, force);

				results.add(result.toMap());
			}

			return toGraphObjectMaps(results);
		}

		return ContentExtraction.schedule(securityContext.getUser(false), files.stream().map(File::getUuid).toList(), overrides, force, queue);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.processContent(fileOrFolderOrList [, options ]) }"), Usage.javaScript("Usage: ${{ $.documents.processContent(fileOrFolderOrList [, options ]); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Extracts files, splits their content into chunks and stores the chunks as ExtractedContentNodes.";
	}

	@Override
	public String getLongDescription() {

		return """
			Processes a File, all files in a Folder (with subfolders, unless `recursive` is false), or a list of both. For each file, the content is \
			extracted, split into chunks, and stored as `ExtractedContentNode`s connected to the file (`file.extractedContentNodes`); earlier chunks \
			are replaced. The options come from the function call, then from the nearest `ContentExtractionConfiguration` \
			(attached to the file or one of its folders), then from the `contentextraction.*` settings.

			This is the explicit way to (re)process files, regardless of whether they are marked for automatic extraction. Files whose content \
			and effective options are unchanged since their last extraction are skipped unless `force` is true, so after changing a folder's \
			configuration, calling this function on the folder re-processes exactly the affected files.

			By default the work runs as a background job in the queue `contentextraction.extraction.queue` and the function returns the job id. \
			With `wait: true` it runs immediately and returns one result per file.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("fileOrFolderOrList", "a File, a Folder, or a list of Files and Folders"),
			Parameter.optional("options", """
				an object with any of: `force` (process unchanged files too), `recursive` (include subfolders, default true), `wait` (run now and \
				return results instead of a job id), `queue` (job queue name), and the options of `documents.splitContent()` and `documents.extractContent()`: \
				`splitter`, `chunkSize`, `chunkOverlap`, `sizeUnit`, `separators`, `includeHeadingPath`, `tokenEncoding`, `ocr`, `ocrLanguages`, \
				`maxCharacters`, `includeEmbedded`, `includeImages`"""));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("""
				${{
				    // re-process a folder after changing its chunk size
				    let folder = $.first($.find('Folder', { name: 'manuals' }));

				    return $.documents.processContent(folder);
				}}
				""", "Re-process a folder in the background"),
			Example.structrScript("${ documents.processContent(first(find('File', 'name', 'scan.pdf')), { ocr: 'always', force: true, wait: true }) }", "Extract one file right away, with OCR on every page"));
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Processing a file requires write permission on it. The chunks are readable by everyone who can read the file.",
			"Progress of background jobs is shown in the job queue panel; the job can be paused and aborted there."
		);
	}

	// ----- private methods -----
	private List<File> files(final Object[] sources, final boolean recursive) throws FrameworkException {

		if (sources == null || sources.length == 0 || sources[0] == null) {

			throw new FrameworkException(422, getName() + "(): expected a File, a Folder or a list of them as argument 1.");
		}

		final List<Object> targets = sources[0] instanceof Iterable<?> iterable ? toList(iterable) : List.of(sources[0]);
		final List<File> files     = new ArrayList<>();
		final Set<String> seen     = new HashSet<>();

		for (final Object target : targets) {

			if (target instanceof NodeInterface node && node.is(StructrTraits.FILE)) {

				addOnce(node.as(File.class), files, seen);

			} else if (target instanceof NodeInterface node && node.is(StructrTraits.FOLDER)) {

				for (final File file : FolderTreeWalker.files(node.as(Folder.class), recursive)) {

					addOnce(file, files, seen);
				}

			} else {

				throw new FrameworkException(422, getName() + "(): expected Files and Folders, got " + target + ".");
			}
		}

		return files;
	}

	private static List<Object> toList(final Iterable<?> iterable) {

		final List<Object> list = new ArrayList<>();

		for (final Object o : iterable) {

			list.add(o);
		}

		return list;
	}

	private static void addOnce(final File file, final List<File> files, final Set<String> seen) {

		if (seen.add(file.getUuid())) {

			files.add(file);
		}
	}

	private static Set<String> accepted() {

		final Set<String> accepted = new HashSet<>(ContentOptionsResolver.OPTIONS);

		accepted.addAll(Set.of(FORCE, QUEUE, RECURSIVE, WAIT));

		return Set.copyOf(accepted);
	}
}
