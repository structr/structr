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
package org.structr.common.error;

import java.util.ArrayList;
import java.util.List;

/**
 * A buffer that collects error tokens to allow for i18n and human readable
 * output.
 *
 *
 */
public class ErrorBuffer {

	private final List<ErrorToken> tokens = new ArrayList<>();
	private int status                    = 0;

	public void add(final ErrorToken token) {
		tokens.add(token);
	}

	public boolean hasError() {
		return !tokens.isEmpty();
	}

	public List<ErrorToken> getErrorTokens() {
		return tokens;
	}

	public void setStatus(final int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
