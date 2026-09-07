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
     * <li><b>The function is not invoked here.</b> It is the module's completion value, and
     * {@code Scripting.evaluatePolyglot} calls it. <em>How</em> it is called decides who settles the
     * promise: through a plain arrow for the outermost evaluation, so the promise reaches the host
     * pending and {@code PolyglotWrapper.unwrapThenable} settles it and can stop early; directly for a
     * nested one, where {@code js.interop-complete-promises} resolves it at the call boundary because
     * the host cannot settle anything from inside a running evaluation.</li>
     * <li><b>There is no top-level await.</b> A module using one answers the module evaluation promise
     * instead of its completion value, and that promise fulfils with {@code undefined}, so the
     * script's return value is silently lost.</li>
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
