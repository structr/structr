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
package org.structr.core.script.polyglot.util;

import org.structr.core.script.Snippet;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class JSFunctionTranspiler {

    public static String transpileSource(final Snippet snippet) {

        if (snippet.embed()) {

            final String transpiledSource = "(() => {" + snippet.getSource() + "\n})();";
            snippet.setTranscribedSource(transpiledSource);
            return snippet.getTranscribedSource();
        }

        return snippet.getSource();
    }
}
