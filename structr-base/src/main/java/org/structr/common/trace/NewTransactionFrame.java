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
 * The worker of doInNewTransaction(): the caller's function, running in a transaction of its own on
 * another thread. Nothing on the caller's side sees its failures, so they end here.
 */
public class NewTransactionFrame extends ScriptFrame {

	public NewTransactionFrame(final CallFrame caller) {
		super(caller, "doInNewTransaction");
	}

	@Override
	public String kind() {
		return "NEW_TRANSACTION";
	}

	@Override
	public String continuationLabel() {
		return "doInNewTransaction()";
	}
}
