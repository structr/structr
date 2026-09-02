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
package org.structr.core.script.polyglot.util;

import org.structr.core.script.Snippet;

public abstract class JSFunctionTranspiler {

    /**
     * Wraps an embedded JavaScript snippet in an <b>async arrow function that is not called</b>, so
     * the snippet can use await.
     *
     * <p>Two properties of this shape are load-bearing and easy to undo by accident:</p>
     *
     * <ul>
     * <li><b>The function is not invoked here.</b> It is the module's completion value, and the host
     * calls it (see {@code Scripting.evaluatePolyglot}). Because the context sets
     * {@code js.interop-complete-promises}, GraalJS then resolves the returned promise at the call
     * boundary and hands back the value, or throws the rejection.</li>
     * <li><b>There is no top-level await.</b> A module that uses one returns the module evaluation
     * promise instead of its completion value, and that promise fulfils with {@code undefined} -- so
     * the script's return value would be silently lost. That is what reverted the two earlier
     * attempts at this (13c9d3c4c3 and af04612079).</li>
     * </ul>
     *
     * <p>The prologue stays on the first line so the user's code keeps its original line numbers, which
     * is what {@code Snippet.getStartRow()} and the error reporting in {@code Scripting} assume.</p>
     */
    public static String transpileSource(final Snippet snippet) {

        if (snippet.embed()) {

            final String transpiledSource = "(async () => {" + snippet.getSource() + "\n})";
            snippet.setTranscribedSource(transpiledSource);

            return snippet.getTranscribedSource();
        }

        return snippet.getSource();
    }
}
