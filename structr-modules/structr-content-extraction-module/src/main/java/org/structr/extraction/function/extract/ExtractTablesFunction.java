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
package org.structr.extraction.function.extract;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.ExtractedTable;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtractTablesFunction extends ContentExtractionFunction {

	private static final Set<String> ACCEPTED = Set.of(OPTION_MAX_CHARACTERS, OPTION_INCLUDE_EMBEDDED);

	@Override
	public String getName() {

		return NAMESPACE + ".extractTables";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file                   = fileArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);

		try {

			final List<ExtractedTable> tables = ContentExtraction.extractTables(file, extractionOptions(options));

			return toGraphObjectMaps(tables.stream().map(ExtractedTable::toMap).toList());

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);
		}
	}

	@Override
	public String getShortDescription() {

		return "Extracts the tables of a file, with their cells.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the tables of a file as a list of objects with `index`, `page`, `headingPath` (the section headings the table appears under), \
			`embeddedPath` (the archive entry or attachment the table is in, if any) and `rows`, a list of rows that are lists of cell texts. Works for formats with real tables, such as DOCX, XLSX, ODT, ODS, PPTX and HTML; \
			each spreadsheet sheet is one table. PDF does not store tables as such, so tables in PDFs appear as text only.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("file", "the File to read"),
			Parameter.optional("options", "an object with any of: `maxCharacters` (text beyond is dropped) and `includeEmbedded` (also read tables of embedded documents)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("""
			${{
			    let file   = $.first($.find('File', { name: 'prices.xlsx' }));
			    let table  = $.documents.extractTables(file)[0];

			    // the first row holds the column names
			    let header = table.rows[0];

			    return table.rows.slice(1).map(row => Object.fromEntries(header.map((name, i) => [name, row[i]])));
			}}
			""", "Rows of a spreadsheet as objects"));
	}
}
