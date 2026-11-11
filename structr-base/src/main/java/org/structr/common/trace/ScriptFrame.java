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
package org.structr.common.trace;

import java.util.Map;

/**
 * One evaluation of user code. The only kind of frame GraalVM's stack knows about, so it is what a
 * failure's location and its callers' call sites are matched to (by {@link #getSourceName()}).
 */
public class ScriptFrame extends CallFrame {

	/**
	 * What an evaluation is called and which code it runs.
	 *
	 * @param name      what the code is: a method name, or a label such as {@code content}
	 * @param identity  the code itself
	 * @param sessionId the HTTP session to show errors to, if no frame above names one
	 */
	public record Spec(String name, ScriptIdentity identity, String sessionId) {

		public static Spec named(final String name) {
			return new Spec(name, ScriptIdentity.named(name), null);
		}
	}

	/**
	 * Makes the frame for an evaluation, so that a call site can say what kind of code it runs.
	 */
	public interface Factory {

		ScriptFrame create(final Spec spec);
	}

	private final ScriptIdentity identity;

	public ScriptFrame(final Spec spec) {
		this(spec, false);
	}

	protected ScriptFrame(final Spec spec, final boolean boundary) {

		super(spec.name(), spec.sessionId(), boundary);

		this.identity = spec.identity();
	}

	/**
	 * Code continuing the given frame elsewhere: a function the caller's script handed to another
	 * thread or to later. It was written in the caller's source, so it has the caller's identity.
	 */
	protected ScriptFrame(final CallFrame continued, final String fallbackName) {

		super(continued, continued instanceof ScriptFrame caller ? caller.getName() : fallbackName, null, true);

		this.identity = continued instanceof ScriptFrame caller ? caller.identity : ScriptIdentity.named(fallbackName);
	}

	@Override
	public String kind() {
		return "SCRIPT";
	}

	public String getSourceName() {
		return identity.sourceName();
	}

	public String getCodeSourceId() {
		return identity.codeSourceId();
	}

	public String getEntityType() {
		return identity.entityType();
	}

	public String getEntityId() {
		return identity.entityId();
	}

	public String getDeclaringType() {
		return identity.declaringType();
	}

	public boolean isStatic() {
		return identity.isStatic();
	}

	public int getStartRow() {
		return identity.startRow();
	}

	/**
	 * <code>Type[id]:name</code>, <code>Type[static]:name</code>, or the name alone.
	 */
	@Override
	public String describe(final String knownCodeSourceType) {

		final StringBuilder buf = new StringBuilder();

		if (identity.isStatic()) {

			buf.append(identity.declaringType()).append("[static]:");

		} else if (identity.entityId() != null) {

			buf.append(identity.entityType() != null ? identity.entityType() : "Entity").append("[").append(identity.entityId()).append("]:");

		} else if (identity.codeSourceId() != null) {

			final String type = identity.codeSourceType() != null ? identity.codeSourceType() : knownCodeSourceType;

			buf.append(type != null ? type : "").append("[").append(identity.codeSourceId()).append("]:");
		}

		buf.append(getName());

		return buf.toString();
	}

	@Override
	public String describe() {
		return describe(null);
	}

	@Override
	public Map<String, Object> toMap() {

		final Map<String, Object> map = super.toMap();

		map.put("type",     identity.declaringType());
		map.put("id",       identity.codeSourceId());
		map.put("entity",   identity.entityId());
		map.put("isStatic", identity.isStatic());

		return map;
	}
}
