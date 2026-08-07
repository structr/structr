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
package org.structr.process.bpmn;

/**
 * Naming scheme for engine-generated BPMN listener handler methods -- and the record of
 * why it exists. Single source of truth: import qualifies, export un-qualifies, re-import
 * matches versions, tests assert. Nothing else may hand-build or hand-parse these names.
 *
 * <h3>The problem</h3>
 *
 * <p>A {@code <structr:taskListener method="onCreate"/>} makes the importer create a
 * SchemaMethod named {@code onCreate} with NO {@code schemaNode} -- that is, in the global
 * user-function namespace, where SchemaMethod enforces case-insensitive name uniqueness.
 * But handler names are authored per process, and nothing in BPMN says they are global, so
 * the same authored name legitimately occurs many times over:</p>
 *
 * <ul>
 * <li>two unrelated processes each declaring {@code onCreate};</li>
 * <li>two elements of ONE process each declaring {@code onCreate};</li>
 * <li>version 1 and version 2 of the same process -- which coexist by design, because
 *     in-flight instances keep executing the definition they started on, and each version
 *     snapshots its own handler bodies (see {@code BpmnImporter#cloneElementMethods}).</li>
 * </ul>
 *
 * <p>Every one of those cases failed the import with {@code already_exists}. The uniqueness
 * rule itself is correct and stays untouched; what was wrong is that a per-process concept
 * was being written into a global namespace under its bare authored name.</p>
 *
 * <h3>The decision</h3>
 *
 * <p>Qualify the GRAPH name of every engine-generated handler with the scope that makes it
 * unique, while the BPMN file keeps the authored name:</p>
 *
 * <pre>
 *   &lt;authored&gt;__bpmn__&lt;processId&gt;_v&lt;version&gt;[_&lt;elementBpmnId&gt;]
 *
 *   onCreate__bpmn__Process_MC_v2_UserTask_1   (element handler, process Process_MC v2)
 *   onProcessStarted__bpmn__Process_MC_v2      (process-level handler)
 * </pre>
 *
 * <p>The authored name comes FIRST for two reasons: the result stays a valid Structr method
 * name ({@code [a-z_][a-zA-Z0-9_]*}) without further massaging, and un-qualifying is
 * context-free -- everything before the marker is the authored name, so no caller needs to
 * know the process, version or element to recover it. Keeping it reversible without context
 * is what makes this cheap: the exporter, the re-import version matching and the tests all
 * just call {@link #authoredOf(String)}.</p>
 *
 * <p>Runtime dispatch is unaffected: a listener points at its SchemaMethod through the CALLS
 * relationship and never resolves by name (see {@code BpmnTaskListener}), so the qualified
 * name is a label, not an execution concern.</p>
 *
 * <h3>Alternative considered and rejected</h3>
 *
 * <p>Make HAS_METHOD many-to-many so that ONE method node can be co-owned by every element
 * and process that names it (get-or-create by name), with reference-counted deletion so a
 * shared handler survives until its last owner is gone. That works, and it was built and
 * green, but it was rejected as speculative generality: it permanently commits the graph
 * model -- cardinality, cascade semantics, the UI's single-vs-collection assumptions -- to a
 * SHARING capability that no project has actually asked for, and it makes every future
 * change to method ownership reason about the reap.</p>
 *
 * <p>Prefixing is the conservative choice, and the regret is asymmetric: it keeps the boring
 * model (one owner, plain cascading delete) and stays additive -- if a real use case for
 * genuine sharing ever appears, co-ownership can be introduced then, with a requirement in
 * hand. Meanwhile sharing is still perfectly available and more explicit: give the shared
 * code its own function and CALL it from each qualified handler.</p>
 *
 * <h3>The editor path</h3>
 *
 * <p>Handlers created in the process editor go through {@code ensureHandlerMethod} on
 * BpmnElement / BpmnProcess rather than being built client-side, so the editor only ever
 * supplies the AUTHORED name and the scope is applied here. That is deliberate: mirroring
 * {@link #qualify} in JavaScript would fork the one thing that must not be forked, and the
 * editor cannot know a version stamp reliably anyway. The same entry point re-scopes a
 * rename, which is why the editor's "keep the handler name in sync with (event, phase)"
 * behaviour cannot silently strip the scope. See {@code BpmnHandlerMethodNamingTest}.</p>
 *
 * <h3>When to revisit</h3>
 *
 * <p>If a project needs several processes to genuinely share one editable handler body (not
 * merely call a shared function), or if per-version body snapshotting is dropped so that all
 * versions of a process share their code, then this scheme's version component becomes dead
 * weight and co-ownership deserves a second look.</p>
 */
public final class BpmnHandlerNames {

	/**
	 * Separates the authored name from its scope. Deliberately distinctive: a double
	 * underscore around a literal, so it cannot plausibly collide with an authored name
	 * and is recognizable at a glance in the Code area.
	 */
	public static final String SCOPE_MARKER = "__bpmn__";

	private BpmnHandlerNames() {
	}

	/**
	 * The graph name for an engine-generated handler. {@code elementBpmnId} is null for a
	 * process-level handler. Returns {@code authoredName} unchanged when there is nothing
	 * to qualify with, and never double-qualifies an already qualified name.
	 */
	public static String qualify(final String authoredName, final String processId, final String version, final String elementBpmnId) {

		if (authoredName == null || authoredName.isEmpty()) {

			return authoredName;
		}

		if (isQualified(authoredName)) {

			return authoredName;
		}

		// Without a process scope there is nothing to disambiguate against, so leave the
		// name alone rather than inventing a misleading scope.
		if (processId == null || processId.isEmpty()) {

			return authoredName;
		}

		final StringBuilder buf = new StringBuilder(authoredName);

		buf.append(SCOPE_MARKER);
		buf.append(component(processId));
		buf.append("_v").append(component(version));

		if (elementBpmnId != null && !elementBpmnId.isEmpty()) {

			buf.append('_').append(component(elementBpmnId));
		}

		return buf.toString();
	}

	/**
	 * The authored name behind a graph name, i.e. what belongs in the BPMN file. Returns
	 * the input unchanged when it carries no scope -- a user-authored global function, or a
	 * handler someone renamed by hand, is passed through as-is.
	 */
	public static String authoredOf(final String graphName) {

		if (graphName == null) {

			return null;
		}

		final int marker = graphName.indexOf(SCOPE_MARKER);
		if (marker < 1) {

			return graphName;
		}

		return graphName.substring(0, marker);
	}

	/** Whether this graph name was generated by the BPMN importer (carries a scope). */
	public static boolean isQualified(final String graphName) {

		return graphName != null && graphName.indexOf(SCOPE_MARKER) > 0;
	}

	/**
	 * Reduce one scope component to name-safe characters. BPMN ids are XML NCNames and may
	 * contain hyphens, dots and colons, none of which are legal in a Structr method name.
	 */
	private static String component(final String raw) {

		if (raw == null || raw.isEmpty()) {

			return "x";
		}

		return raw.replaceAll("[^A-Za-z0-9]+", "_");
	}
}
