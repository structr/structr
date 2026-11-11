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

/**
 * Which code an evaluation runs, as far as a report and the editor need to know it.
 *
 * @param sourceName     the name the code was compiled under, which GraalVM reports for its stack frames
 * @param codeSourceId   the uuid of the node holding the code
 * @param codeSourceType the type of that node, if known without looking it up
 * @param entityType     the type of {@code this}
 * @param entityId       the uuid of {@code this}
 * @param declaringType  the type declaring the method
 * @param isStatic       a static method, which needs a declaring type
 * @param startRow       the row the code starts at within its code source
 */
public record ScriptIdentity(String sourceName, String codeSourceId, String codeSourceType, String entityType, String entityId, String declaringType, boolean isStatic, int startRow) {

	public ScriptIdentity {

		isStatic = isStatic && declaringType != null;
	}

	/**
	 * Code known by nothing but the name it was compiled under.
	 */
	public static ScriptIdentity named(final String sourceName) {
		return builder(sourceName).build();
	}

	public static Builder builder(final String sourceName) {
		return new Builder(sourceName);
	}

	public static final class Builder {

		private final String sourceName;
		private String codeSourceId   = null;
		private String codeSourceType = null;
		private String entityType     = null;
		private String entityId       = null;
		private String declaringType  = null;
		private boolean isStatic      = false;
		private int startRow          = 0;

		private Builder(final String sourceName) {
			this.sourceName = sourceName;
		}

		/**
		 * The node holding the code, and its type if known without looking it up.
		 */
		public Builder codeSource(final String id, final String type) {

			this.codeSourceId   = id;
			this.codeSourceType = type;

			return this;
		}

		/**
		 * The object the code runs on.
		 */
		public Builder entity(final String type, final String id) {

			this.entityType = type;
			this.entityId   = id;

			return this;
		}

		/**
		 * The type declaring the method the code belongs to; a static method needs one.
		 */
		public Builder declaredBy(final String type, final boolean isStatic) {

			this.declaringType = type;
			this.isStatic      = isStatic;

			return this;
		}

		public Builder startRow(final int startRow) {

			this.startRow = startRow;

			return this;
		}

		public ScriptIdentity build() {
			return new ScriptIdentity(sourceName, codeSourceId, codeSourceType, entityType, entityId, declaringType, isStatic, startRow);
		}
	}
}
