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
package org.structr.diff.function;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.diff.compare.ExportComparison;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;

/**
 * Compares two deployment exports and returns the report.
 *
 * The platform-level entry to the comparison: an application's own script reaches it as
 * {@code compareExports()}, and a server-side caller reaches the very same code through
 * {@code Functions.get("compareExports")} without depending on this module at all. The report comes from
 * {@link ExportComparison}, which also answers the admin UI's websocket command.
 */
public class CompareExportsFunction extends Function<Object, Object> {

	@Override
	public String getName() {

		return "compareExports";
	}

	@Override
	public String getRequiredModule() {

		return "diff";
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Deployment;
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("leftFileId, rightFileId");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${ compareExports(leftFileId, rightFileId) }"),
			Usage.javaScript("Usage: ${{ $.compareExports(leftFileId, rightFileId); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Compares two deployment exports and reports whether they are versions of one application.";
	}

	@Override
	public String getLongDescription() {

		return "Reads two deployment export archives stored as files, matches their contents entity by entity and returns "
			+ "what differs. The verdict answers the question a deployment has to ask before it overwrites anything: are "
			+ "these two exports versions of the same application, or two different applications. Both archives are read "
			+ "through the calling user's permissions, so a user who cannot read one of the files cannot compare it.";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The returned object has four keys. `summary` counts the entities on each side and the deltas between them, "
				+ "grouped by operation and kind. `congruence` is the verdict. `profiles` describes each export by its schema "
				+ "and pages. `deltas` lists every difference, each with its `operation`, `kind`, `key`, `name` and `signal`.",
			"`congruence.verdict` is one of three values. `SAME_LINEAGE` means the exports are versions of one application and "
				+ "a change between them can be expressed as an update. `SAME_APP_DIFFERENT_LINEAGE` means the same application "
				+ "installed independently, where uuids no longer match. `DIFFERENT_APPS` means a diff between them describes "
				+ "two unrelated applications rather than a change. `congruence.explanation` states the same verdict in a "
				+ "sentence, with the percentages it rests on.",
			"Beside the verdict, `congruence` carries `identityOverlap` and `similarity` as ratios, the four `signals` they are "
				+ "computed from (`pageNames`, `schemaTypes`, `localizations`, `pageStructure`), and `provenance`, which names "
				+ "the Structr version each export came from and when it was taken.",
			"The comparison runs inline and returns when it is done; a large real export parses in roughly 180ms, so two "
				+ "parses and the match stay well under a second.",
			"The files are looked up by uuid, which is what an export has once it is stored. Pass the uuid of the File "
				+ "node, not a path.");
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${ compareExports(left.id, right.id) }", "Compares two exports and returns the report"),
			Example.structrScript("${ compareExports(left.id, right.id).congruence.verdict }", "Answers with SAME_LINEAGE, SAME_APP_DIFFERENT_LINEAGE or DIFFERENT_APPS"),
			Example.javaScript("""
					${{
					    // preflight before a deployment: is the target a version of this app, or a different one?
					    let report = $.compareExports(localExport.id, targetExport.id);

					    if (report.congruence.verdict === 'DIFFERENT_APPS') {
					        throw new Error('Refusing to deploy: ' + report.congruence.explanation);
					    }

					    // what the import would overwrite
					    return report.summary;
					}}
					"""));
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasLengthAndAllElementsNotNull(sources, 2);

		return ExportComparison.of(ctx.getSecurityContext(), sources[0].toString(), sources[1].toString());
	}
}
