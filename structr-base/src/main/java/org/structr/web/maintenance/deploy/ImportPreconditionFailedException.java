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

public class ImportPreconditionFailedException extends RuntimeException {

	private String title = "Deployment Import not started";
	private String html  = null;

	public ImportPreconditionFailedException(final String message) {

		super(message);
	}

	public ImportPreconditionFailedException(final String title, final String message) {

		this(message);

		this.title = title;
	}

	public ImportPreconditionFailedException(final String title, final String message, final String html) {

		this(title, message);

		this.html = html;
	}

	public String getTitle () {
		return title;
	}

	public String getMessageHtml () {

		if (html != null) {
			return html;
		}

		return getMessage();
	}
}
