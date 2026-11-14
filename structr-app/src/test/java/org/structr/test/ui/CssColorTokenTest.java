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
package org.structr.test.ui;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The admin UI has two looks and a dark colour scheme, and all of them get their colours from tokens (custom
 * properties). A colour written into a rule looks right in the classic look and wrong everywhere else, so this
 * test fails on it.
 *
 * <p>A colour belongs in a token: a role token in css/main.css for what the whole UI shares, or a token at the top
 * of the area's own file. The new look sets the same tokens in css/next/. Token definitions, the stylesheets of
 * third-party libraries (css/lib) and the content of url() are not checked.
 */
public class CssColorTokenTest {

	private static final Pattern COMMENT     = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
	private static final Pattern DECLARATION = Pattern.compile("(?:(?<=[;{}])|^)\\s*([\\w-]+)\\s*:\\s*([^;{}]+)(?=[;}])", Pattern.MULTILINE);
	private static final Pattern NOT_A_COLOR = Pattern.compile("url\\([^)]*\\)|\"[^\"]*\"|'[^']*'");
	private static final Pattern LITERAL     = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|\\b(?:rgb|hsl)a?\\([^)]*\\)");
	private static final Pattern COLOR_NAME  = Pattern.compile("(?<![\\w-])(?:white|black|red|green|blue|gray|grey|orange|yellow|silver|purple|pink|brown|gold|navy|teal|cyan|magenta|lime|maroon|olive"
		+ "|lightgray|lightgrey|darkgray|darkgrey|dimgray|dimgrey|whitesmoke|gainsboro|darkred|darkgreen|darkblue|darkorange|lightblue|lightgreen|lightyellow|lawngreen|crimson|tomato|firebrick)(?![\\w-])", Pattern.CASE_INSENSITIVE);
	private static final Pattern COLOR_PROP  = Pattern.compile("color|background|border|shadow|fill|stroke|outline");
	private static final int MAX_REPORTED    = 40;

	@Test
	public void testColorsGoThroughTokens() throws IOException {

		final Path css                = Paths.get(System.getProperty("basedir", "."), "src/main/resources/structr/css");
		final List<String> violations = new ArrayList<>();

		assertTrue("The stylesheets of the admin UI are expected in " + css.toAbsolutePath(), Files.isDirectory(css));

		for (final Path dir : List.of(css, css.resolve("next"))) {

			try (final Stream<Path> files = Files.list(dir)) {

				for (final Path file : files.filter(f -> f.getFileName().toString().endsWith(".css")).sorted().toList()) {

					for (final String violation : findFixedColors(Files.readString(file, StandardCharsets.UTF_8))) {

						violations.add(css.relativize(file) + ":" + violation);
					}
				}
			}
		}

		final StringBuilder message = new StringBuilder("Fixed colours in the stylesheets of the admin UI. Use a role token from css/main.css, or add a token at the top of the area's file:");

		violations.stream().limit(MAX_REPORTED).forEach(v -> message.append("\n  ").append(v));

		if (violations.size() > MAX_REPORTED) {
			message.append("\n  and ").append(violations.size() - MAX_REPORTED).append(" more");
		}

		assertEquals(message.toString(), 0, violations.size());
	}

	@Test
	public void testTheCheckFindsFixedColors() {

		final String sample = """
			/* a comment with a colour: #123456 */
			:root {
				--token-with-a-value: #84ba39;
			}
			#header a:hover {
				color: var(--color-text-primary);
				background: url("data:image/svg+xml;utf8,<svg fill='%23fff' stroke='#000'/>") no-repeat;
				white-space: nowrap;
				border: 1px solid #ccc;
				box-shadow: 0 0 4px rgba(0, 0, 0, .2),
					0 0 8px red;
				fill: currentColor
			}
			""";

		assertEquals(List.of("9  border: #ccc", "10  box-shadow: rgba(0, 0, 0, .2)", "10  box-shadow: red"), findFixedColors(sample));
	}

	// ----- private methods -----
	private List<String> findFixedColors(final String source) {

		final List<String> found = new ArrayList<>();

		// comments go, their line breaks stay, so the line numbers hold
		final String css = COMMENT.matcher(source).replaceAll(m -> m.group().replaceAll("[^\\n]", " "));
		final Matcher d  = DECLARATION.matcher(css);

		while (d.find()) {

			final String property = d.group(1);

			// a custom property is a token, and a token is where a colour belongs
			if (property.startsWith("--")) {
				continue;
			}

			final String value = NOT_A_COLOR.matcher(d.group(2)).replaceAll("");
			final long line    = css.substring(0, d.start(1)).chars().filter(c -> c == '\n').count() + 1;
			final Matcher l    = LITERAL.matcher(value);

			while (l.find()) {
				found.add(line + "  " + property + ": " + l.group());
			}

			if (COLOR_PROP.matcher(property).find()) {

				final Matcher n = COLOR_NAME.matcher(value);

				while (n.find()) {
					found.add(line + "  " + property + ": " + n.group());
				}
			}
		}

		return found;
	}
}
