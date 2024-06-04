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
package org.structr.web.maintenance.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.CommentHandler;
import org.structr.web.maintenance.DeployCommand;

import java.util.*;

/**
 *
 */
public class DeploymentCommentHandler implements CommentHandler {

	private static final Set<Character> separators     = new LinkedHashSet<>(Arrays.asList(new Character[] { ',', ';', '(', ')', ' ', '\t', '\n', '\r' } ));
	private static final Logger logger                 = LoggerFactory.getLogger(DeploymentCommentHandler.class.getName());
	private static final Map<String, Handler> handlers = new LinkedHashMap<>();

	static {

		handlers.put("public-only", (Page page, DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(AbstractNode.visibleToPublicUsers,        true);
			changedProperties.put(AbstractNode.visibleToAuthenticatedUsers, false);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("public", (Page page, DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(AbstractNode.visibleToPublicUsers,        true);
			changedProperties.put(AbstractNode.visibleToAuthenticatedUsers, true);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("protected", (Page page, DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(AbstractNode.visibleToPublicUsers,        false);
			changedProperties.put(AbstractNode.visibleToAuthenticatedUsers, true);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("private", (Page page, DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(AbstractNode.visibleToPublicUsers,        false);
			changedProperties.put(AbstractNode.visibleToAuthenticatedUsers, false);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("hidden", (Page page, DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(AbstractNode.hidden, true);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("link", (Page page, DOMNode node, final String parameters) -> {

			if (node instanceof LinkSource) {

				final Linkable file = StructrApp.getInstance().nodeQuery(Linkable.class).and(StructrApp.key(AbstractFile.class, "path"), parameters).getFirst();
				if (file != null) {

					final LinkSource linkSource = (LinkSource)node;

					linkSource.setLinkable(file);
				}
			}
		});

		handlers.put("pagelink", (Page page, DOMNode node, final String parameters) -> {

			if (node instanceof LinkSource) {
				DeployCommand.addDeferredPagelink(node.getUuid(), parameters);
			}
		});

		handlers.put("content", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(StructrApp.key(Content.class, "contentType"), parameters);
		});

		handlers.put("name", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(StructrApp.key(DOMNode.class, "name"), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("show", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(StructrApp.key(DOMNode.class, "showConditions"), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("hide", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(StructrApp.key(DOMNode.class, "hideConditions"), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("show-for-locales", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(StructrApp.key(DOMNode.class, "showForLocales"), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("hide-for-locales", (Page page, DOMNode node, final String parameters) -> {
			node.setProperty(StructrApp.key(DOMNode.class, "hideForLocales"), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("owner", (Page page, DOMNode node, final String parameters) -> {

			final List<Principal> principals = StructrApp.getInstance().nodeQuery(Principal.class).andName(parameters).getAsList();

			if (principals.isEmpty()) {

				logger.warn("Unknown owner! Found no node of type Principal named '{}', ignoring.", parameters);
				DeployCommand.addMissingPrincipal(parameters);

			} else if (principals.size() > 1) {

				logger.warn("Ambiguous owner! Found {} nodes of type Principal named '{}', ignoring.", principals.size(), parameters);
				DeployCommand.addAmbiguousPrincipal(parameters);

			} else {

				node.setProperty(AbstractNode.owner, principals.get(0));
			}
		});

		handlers.put("grant", (Page page, DOMNode node, final String parameters) -> {

			final String[] parts  = parameters.split("[,]+");
			if (parts.length == 2) {

				final List<Principal> principals = StructrApp.getInstance().nodeQuery(Principal.class).andName(parts[0]).getAsList();

				if (principals.isEmpty()) {

					logger.warn("Unknown grantee! Found no node of type Principal named '{}', ignoring.", parts[0]);
					DeployCommand.addMissingPrincipal(parts[0]);

				} else if (principals.size() > 1) {

					logger.warn("Ambiguous grantee! Found {} nodes of type Principal named '{}', ignoring.", principals.size(), parts[0]);
					DeployCommand.addAmbiguousPrincipal(parts[0]);

				} else {

					final Principal grantee = principals.get(0);

					for (final char c : parts[1].toCharArray()) {

						switch (c) {

							case 'a':
								node.grant(Permission.accessControl, grantee);
								break;

							case 'r':
								node.grant(Permission.read, grantee);
								break;

							case 'w':
								node.grant(Permission.write, grantee);
								break;

							case 'd':
								node.grant(Permission.delete, grantee);
								break;

							default:
								logger.warn("Invalid @grant permission {}, must be one of [a, r, w, d].", c);
						}
					}
				}

			} else {

				logger.warn("Invalid @grant instruction {}, must be like @structr:grant(userName,rw).", parameters);
			}
		});
	}

	private char[] source       = null;
	private int currentPosition = 0;
	private int sourceLength    = 0;

	@Override
	public boolean containsInstructions(final String comment) {

		try {

			return parseInstructions(null, null, comment, false);

		} catch (FrameworkException fex) {
			logger.warn("Unexpected exception, no changes should be made in this method: {}", fex.getMessage());
		}

		return false;
	}

	@Override
	public boolean handleComment(final Page page, final DOMNode node, final String comment, final boolean apply) throws FrameworkException {
		return parseInstructions(page, node, comment.trim(), apply);
	}

	// ----- private methods -----
	private boolean parseInstructions(final Page page, final DOMNode node, final String src, final boolean apply) throws FrameworkException {

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
								parameters = getUntilClosingParenthesis();
								break;

							case ',':
								// next instruction, ignore
								break;
						}
					}

					// only apply if instructed to (can be used to check
					// if the comment source actually contains instructions)
					if (apply) {

						handler.apply(page, node, parameters);
					}

				} else {

					logger.warn("Unknown token {}, expected one of {}.", new Object[] { token, handlers.keySet() });
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

	private String getUntilClosingParenthesis() {

		final StringBuilder buf = new StringBuilder();
		int count               = 1;

		while (hasMore()) {

			final char c = source[currentPosition++];

			if (c == '(') {
				count++;
			}

			if (c == ')' && --count == 0) {
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
