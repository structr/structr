/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.schema.action;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 *
 */
public class ActionEntry implements Comparable<ActionEntry> {

	private Actions.Type type  = null;
	private String call        = null;
	private String name        = null;
	private int position       = 0;

	public ActionEntry(final String sourceName, final String value, final boolean isJava) {

		int positionOffset = 0;

		if (sourceName.startsWith("___onSave")) {

			this.type = Actions.Type.Save;
			positionOffset = 9;

		} else if (sourceName.startsWith("___onCreate")) {

			this.type = Actions.Type.Create;
			positionOffset = 11;

		} else if (sourceName.startsWith("___onDelete")) {

			this.type = Actions.Type.Delete;
			positionOffset = 11;

		} else {

			this.type = isJava ? Actions.Type.Java : Actions.Type.Custom;
			positionOffset = 3;
		}

		switch (type) {

			case Custom:
			case Java:
				this.name = sourceName.substring(positionOffset);
				break;

			default:
				// try to identify a position
				final String positionString = sourceName.substring(positionOffset);
				if (!positionString.isEmpty()) {

					try { position = Integer.parseInt(positionString); } catch (Throwable t) { /* ignore */ }
				}
				break;
		}

		this.call = value.trim();
	}

	public String getSource(final String objVariable) {
		return getSource(objVariable, false);
	}

	public String getSource(final String objVariable, final boolean includeParameters) {

		final StringBuilder buf = new StringBuilder();

		if (Actions.Type.Java.equals(type)) {

			buf.append(this.call);

		} else {

			buf.append(Actions.class.getSimpleName());
			buf.append(".execute(securityContext, ").append(objVariable).append(", \"${");
			buf.append(StringEscapeUtils.escapeJava(call));
			buf.append("}\"");

			if (includeParameters) {
				buf.append(", parameters");
			}

			buf.append(", \"");

			if (this.type.equals(Actions.Type.Custom)) {

				buf.append(this.name);

			} else {

				buf.append(this.type.getLogName());
			}

			buf.append("\")");
		}

		return buf.toString();
	}

	@Override
	public int compareTo(ActionEntry o) {
		return getPosition().compareTo(o.getPosition());
	}

	public Actions.Type getType() {
		return type;
	}

	public String getCall() {
		return call;
	}

	public Integer getPosition() {
		return position;
	}

	public String getName() {
		return name;
	}
}
