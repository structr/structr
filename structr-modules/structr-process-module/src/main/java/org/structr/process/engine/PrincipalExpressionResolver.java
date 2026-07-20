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
package org.structr.process.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Structr's BPMN principal-reference expressions used inside
 * {@code <bpmn:formalExpression>} bodies on humanPerformer / potentialOwner
 * elements. Intentionally tiny and non-evaluating — pattern match only.
 *
 * Grammar (single principal reference):
 * <pre>
 *   ref      ::= initiator | user(NAME) | group(NAME)
 *   initiator::= "${initiator}"
 *   NAME     ::= any non-comma non-paren character sequence (trimmed)
 * </pre>
 *
 * A full expression body is a comma-separated list of {@code ref}s. The list
 * form is only meaningful for {@code <potentialOwner>} (multiple candidates);
 * for {@code <humanPerformer>} we use the first resolved entry.
 *
 * Document expression language URI: {@value #LANGUAGE_URI}. We accept any
 * language URI on input (logging a warning for unknown ones) but only
 * actually interpret this one.
 */
public final class PrincipalExpressionResolver {

	private static final Logger logger = LoggerFactory.getLogger(PrincipalExpressionResolver.class);

	public static final String LANGUAGE_URI = "http://structr.org/process/expression/v1";

	/** Matches {@code user(name)} or {@code group(name)} — captures kind and name. */
	private static final Pattern PRINCIPAL_FN = Pattern.compile("^(user|group)\\s*\\(\\s*([^)]+?)\\s*\\)$");

	/** Splits a CSV expression into entries — outside of any parentheses. */
	private static final Pattern SPLIT_TOP_LEVEL_COMMA = Pattern.compile(",(?![^(]*\\))");

	private final App app;
	private final NodeInterface processInstance;

	public PrincipalExpressionResolver(final App app, final NodeInterface processInstance) {

		this.app = app;
		this.processInstance = processInstance;
	}

	/**
	 * Resolve a CSV expression body to a list of Principal nodes. Entries
	 * that resolve to nothing are skipped with a logged warning. Entries
	 * with bad syntax raise FrameworkException so authors hear about typos.
	 */
	public List<NodeInterface> resolveAll(final String expressionBody, final String contextLabel) throws FrameworkException {

		final List<NodeInterface> result = new LinkedList<>();
		if (expressionBody == null) {

			return result;
		}
		final String trimmed = expressionBody.trim();
		if (trimmed.isEmpty()) {

			return result;
		}

		for (final String rawEntry : SPLIT_TOP_LEVEL_COMMA.split(trimmed)) {

			final String entry = rawEntry.trim();
			if (entry.isEmpty()) {

				continue;
			}
			final NodeInterface resolved = resolveOne(entry, contextLabel);
			if (resolved != null) {

				result.add(resolved);
			}
		}
		return result;
	}

	/**
	 * Resolve a single entry. Returns null if the entry is well-formed but
	 * the referenced principal doesn't exist (logged). Throws
	 * FrameworkException(422) if the entry has a syntax error.
	 */
	public NodeInterface resolveOne(final String entry, final String contextLabel) throws FrameworkException {

		// ${initiator}
		if ("${initiator}".equals(entry)) {

			final Traits instTraits = processInstance.getTraits();
			final NodeInterface initiator = processInstance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.INITIATOR_PROPERTY));
			if (initiator == null) {

				logger.warn("Expression '${{initiator}}' in {} resolved to null (process instance has no recorded initiator).", contextLabel);
			}
			return initiator;
		}

		// user(name) / group(name)
		final Matcher m = PRINCIPAL_FN.matcher(entry);
		if (m.matches()) {

			final String kind = m.group(1);
			final String name = m.group(2).trim();
			final String typeName = "user".equals(kind) ? StructrTraits.USER : StructrTraits.GROUP;
			final NodeInterface node = app.nodeQuery(typeName).name(name).getFirst();
			if (node == null) {

				logger.warn("Expression '{}' in {} did not resolve: no {} named '{}'", entry, contextLabel, kind, name);
			}
			return node;
		}

		throw new FrameworkException(422,
			"Invalid principal expression '" + entry + "' in " + contextLabel +
			". Expected one of: ${initiator}, user(<name>), group(<name>)."
		);
	}
}
