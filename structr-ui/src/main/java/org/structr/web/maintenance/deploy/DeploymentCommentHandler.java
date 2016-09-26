/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.maintenance.deploy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.CommentHandler;

/**
 *
 */
public class DeploymentCommentHandler implements CommentHandler {

	private static final Set<Character> separators     = new LinkedHashSet<>(Arrays.asList(new Character[] { ',', ';', '(', ')', ' ', '\t', '\n', '\r' } ));
	private static final Logger logger                 = Logger.getLogger(DeploymentCommentHandler.class.getName());
	private static final Map<String, Handler> handlers = new LinkedHashMap<>();

	static {

		handlers.put("public-only", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(AbstractNode.visibleToPublicUsers, true);
			node.setProperty(AbstractNode.visibleToAuthenticatedUsers, false);
		});

		handlers.put("public", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(AbstractNode.visibleToPublicUsers, true);
			node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
		});

		handlers.put("protected", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(AbstractNode.visibleToPublicUsers, false);
			node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
		});

		handlers.put("private", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(AbstractNode.visibleToPublicUsers, false);
			node.setProperty(AbstractNode.visibleToAuthenticatedUsers, false);
		});

		handlers.put("link", (Page page, DOMNode node, final String parameters) -> {

			if (node instanceof LinkSource) {

				final Linkable file = StructrApp.getInstance().nodeQuery(Linkable.class).and(AbstractFile.path, parameters).getFirst();
				if (file != null) {

					final LinkSource linkSource = (LinkSource)node;
					linkSource.setProperty(LinkSource.linkable, file);
				}
			}
		});
	}

	private char[] source       = null;
	private int currentPosition = 0;
	private int sourceLength    = 0;

	@Override
	public boolean handleComment(final Page page, final DOMNode node, final String comment) throws FrameworkException {
		return parseInstructions(page, node, comment.trim());
	}

	// ----- private methods -----
	private boolean parseInstructions(final Page page, final DOMNode node, final String src) throws FrameworkException {

		this.source          = src.toCharArray();
		this.sourceLength    = src.length();
		this.currentPosition = 0;
		boolean hit          = false;

		while (currentPosition < sourceLength) {

			if (findSequence("@structr:")) {

				// comment contains instruction, can be removed upon import
				hit = true;

				// currentPosition is now at the first letter of the actual instruction
				final String token    = getNextToken();
				final Handler handler = handlers.get(token);
				String parameters     = null;

				if (handler != null) {

					// additional characters..
					if (hasMore()) {

						final char c = source[currentPosition++];
						switch (c) {

							case '(':
								parameters = getUntil(')');
								break;

							case ',':
								// next instruction, ignore
								break;
						}
					}

					handler.apply(page, node, parameters);

				} else {

					logger.log(Level.WARNING, "Unknown token {0}, expected one of {1}.", new Object[] { token, handlers.keySet() });
					break;
				}
			}
		}

		return hit;
	}

	private boolean findSequence(final String sequence) {

		final StringBuilder buf = new StringBuilder();
		final char[] seq        = sequence.toCharArray();
		final int len           = seq.length;
		boolean sequenceStarted = false;

		for (int i=0; i<len; i++) {

			if (hasMore()) {

				final char c = Character.toLowerCase(source[currentPosition++]);
				final char s = Character.toLowerCase(seq[i]);

				// skip whitespace before sequence
				if (!sequenceStarted && Character.isWhitespace(c) || separators.contains(c)) {

					// reset sequence to start
					i = -1;
					continue;
				}

				if (c != s) {
					return false;
				}

				sequenceStarted = true;
				buf.append(c);

			} else {

				logger.log(Level.WARNING, "Premature end of sequence, expected {0}, got {1}", new Object[] { sequence, buf.toString() });
			}
		}

		return buf.toString().equals(sequence);
	}

	private String getNextToken() {

		final StringBuilder buf = new StringBuilder();

		while (hasMore()) {

			final char c = source[currentPosition];

			if (Character.isWhitespace(c) || separators.contains(c)) {
				break;
			}

			buf.append(c);

			// advance position only if the character was actually used
			currentPosition++;
		}

		return buf.toString();
	}

	private String getUntil(final char separator) {

		final StringBuilder buf = new StringBuilder();

		while (hasMore()) {

			final char c = source[currentPosition++];

			if (c == separator) {
				break;
			}

			buf.append(c);
		}

		return buf.toString();
	}

	private boolean hasMore() {
		return currentPosition < sourceLength;
	}

	private static interface Handler {
		void apply(final Page page, final DOMNode node, final String parameters) throws FrameworkException;
	}
}
