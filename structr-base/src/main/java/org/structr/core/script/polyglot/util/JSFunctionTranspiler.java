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
    private static final Pattern importPattern = Pattern.compile("import([ \\n\\t]*(?:[^ \\n\\t\\{\\}]+[ \\n\\t]*,?)?(?:[ \\n\\t]*\\{(?:[ \\n\\t]*[^ \\n\\t\"'\\{\\}]+[ \\n\\t]*,?)+\\})?[ \\n\\t]*)from[ \\n\\t]*(['\"])([^'\"\\n]+)(?:['\"])");

    public static String transpileSource(final Snippet snippet) {

        snippet.setMimeType("application/javascript+module");

        if (snippet.embed()) {

            final String transpiledSource;
            // Regex that matches import statements

            if (importPattern.matcher(snippet.getSource()).find()) {

                final Map<Boolean, List<String>> partitionedScript = snippet.getSource().lines().collect(Collectors.partitioningBy(x -> importPattern.matcher(x).find()));
                final String importStatements = String.join("\n", partitionedScript.get(true));
                final String code = String.join("\n", partitionedScript.get(false));

                transpiledSource = importStatements + "\n" +
                        "async function main() {\n" +
                        code +
                        "\n}\n\nawait main();";
            } else {

                transpiledSource = "async function main() {" + snippet.getSource() + "\n}\n\nawait main();";
            }

            snippet.setTranscribedSource(transpiledSource);
        }

        if (snippet.getTranscribedSource() == null) {

            return snippet.getSource();
        }

        return snippet.getTranscribedSource();
    }
}
