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
package org.structr.common;

import javax.activation.DataSource;
import org.apache.commons.mail.EmailAttachment;

public class DynamicMailAttachment extends EmailAttachment {

	private boolean dynamic;
	private DataSource ds;

	public DynamicMailAttachment() {
		dynamic = false;
	}

	public void setIsDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public void setDataSource(DataSource ds) {
		this.ds = ds;
	}

	public boolean isDynamic() {
		return this.dynamic;
	}

	public DataSource getDataSource() {
		return this.ds;
	}
}