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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.util.List;

public class UnarchiveFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "unarchive";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("archiveFile, [, parentFolder ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (sources == null || sources.length < 1 || sources.length > 2
				|| (sources[0] != null && !(sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE))
				|| (sources.length == 2 && sources[1] != null && !(sources[1] instanceof NodeInterface n && n.is(StructrTraits.FOLDER))))) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		try {

			final File archiveFile = ((NodeInterface)sources[0]).as(File.class);
			Folder parentFolder = null;

			if (sources.length == 2 && sources[1] != null) {

				parentFolder = ((NodeInterface) sources[1]).as(Folder.class);
			}

			FileHelper.unarchive(ctx.getSecurityContext(), archiveFile, parentFolder == null ? null : parentFolder.getUuid());

		} catch (final Exception e) {

			logException(caller, e, sources);
		}
		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${unarchive(archiveFile [, parentFolder ])}."),
			Usage.javaScript("Usage: ${{$.unarchive(archiveFile [, parentFolder ])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Unarchives given file to an optional parent folder.";
	}

	@Override
	public String getLongDescription() {
		return """
			If the optional parent folder argument is not specified, the archive contents are extracted into the same directory as the archive file.
			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${unarchive(first(find('File', 'name', 'archive.zip')), first(find('Folder', 'name', 'parent')) )}"),
				Example.javaScript("${{ $.unarchive($.first($.find('File', 'name', 'archive.zip')), $.first($.find('Folder', 'name', 'parent')) )}}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("archiveFile", "file node"),
				Parameter.optional("parentFolder", "parent folder node")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The supported file types are ar, arj, cpio, dump, jar, tar, zip and 7z."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}