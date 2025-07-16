/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.CommentHandler;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;

import java.util.*;

/**
 *
 */
public class DeploymentCommentHandler implements CommentHandler {

	private static final Set<Character> separators     = new LinkedHashSet<>(Arrays.asList(',', ';', '(', ')', ' ', '\t', '\n', '\r'));
	private static final Logger logger                 = LoggerFactory.getLogger(DeploymentCommentHandler.class.getName());
	private static final Map<String, Handler> handlers = new LinkedHashMap<>();

	static {

		handlers.put("public-only", (final Page page, final DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        true);
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("public", (final Page page, final DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        true);
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("protected", (final Page page, final DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        false);
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("private", (final Page page, final DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        false);
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("hidden", (final Page page, final DOMNode node, final String parameters) -> {
			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY), true);
			node.setProperties(node.getSecurityContext(), changedProperties);
		});

		handlers.put("link", (final Page page, final DOMNode node, final String parameters) -> {

			if (node.is(StructrTraits.LINK_SOURCE)) {

				final NodeInterface file = StructrApp.getInstance().nodeQuery(StructrTraits.LINKABLE).key(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), parameters).getFirst();
				if (file != null) {

					final LinkSource linkSource = node.as(LinkSource.class);

					linkSource.setLinkable(file.as(Linkable.class));
				}
			}
		});

		handlers.put("pagelink", (final Page page, final DOMNode node, final String parameters) -> {

			if (node.is(StructrTraits.LINK_SOURCE)) {
				DeployCommand.addDeferredPagelink(node.getUuid(), parameters);
			}
		});

		handlers.put("content", (final Page page, final DOMNode node, final String parameters) -> {
			node.setProperty(Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), parameters);
		});

		handlers.put("name", (final Page page, final DOMNode node, final String parameters) -> {
			node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("show", (final Page page, final DOMNode node, final String parameters) -> {
			node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("hide", (final Page page, final DOMNode node, final String parameters) -> {
			node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("show-for-locales", (final Page page, final DOMNode node, final String parameters) -> {
			node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("hide-for-locales", (final Page page, final DOMNode node, final String parameters) -> {
			node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY), DOMNode.unescapeForHtmlAttributes(DOMNode.unescapeForHtmlAttributes(parameters)));
		});

		handlers.put("owner", (final Page page, final DOMNode node, final String name) -> {

			final List<NodeInterface> principals = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).name(name).getAsList();

			if (principals.isEmpty()) {

				DeployCommand.encounteredMissingPrincipal("Unknown owner", name);

			} else if (principals.size() > 1) {

				DeployCommand.encounteredAmbiguousPrincipal("Ambiguous owner", name, principals.size());

			} else {

				node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), principals.get(0));
			}
		});

		handlers.put("grant", (final Page page, final DOMNode node, final String parameters) -> {

			final String[] parts  = parameters.split("[,]+");
			if (parts.length == 2) {

				final List<NodeInterface> principals = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).name(parts[0]).getAsList();

				if (principals.isEmpty()) {

					DeployCommand.encounteredMissingPrincipal("Unknown grantee", parts[0]);

				} else if (principals.size() > 1) {

					DeployCommand.encounteredAmbiguousPrincipal("Ambiguous grantee", parts[0], principals.size());

				} else {

					final NodeInterface granteeNode = principals.get(0);
					final AccessControllable ac     = node.as(AccessControllable.class);
					final Principal grantee         = granteeNode.as(Principal.class);

					for (final char c : parts[1].toCharArray()) {

						switch (c) {

							case 'a':
								ac.grant(Permission.accessControl, grantee);
								break;

							case 'r':
								ac.grant(Permission.read, grantee);
								break;

							case 'w':
								ac.grant(Permission.write, grantee);
								break;

							case 'd':
								ac.grant(Permission.delete, grantee);
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

					logger.warn("Unknown token {}, expected one of {}.", token, handlers.keySet());
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

	private interface Handler {
		void apply(final Page page, final DOMNode node, final String parameters) throws FrameworkException;
	}
}
