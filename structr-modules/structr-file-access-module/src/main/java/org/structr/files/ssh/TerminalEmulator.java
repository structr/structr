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
package org.structr.files.ssh;

import org.structr.util.Writable;

import java.io.IOException;

/**
 *
 *
 */
public interface TerminalEmulator extends Writable {

	void setTerminalHandler(final TerminalHandler handler) throws IOException;
	void restoreRootTerminalHandler() throws IOException;
	void start();
	void stopEmulator();

	void clearLineBuffer();
	void clearTabCount();
	StringBuilder getLineBuffer();
	void handleCtrlKey(final int key) throws IOException;

	void handleCursorUp() throws IOException;
	void handleCursorDown() throws IOException;
	void handlePageUp() throws IOException;
	void handlePageDown() throws IOException;
	void handleInsert() throws IOException;
	void handleHome() throws IOException;
	void handleEnd() throws IOException;
	void handleCursorLeft() throws IOException;
	void handleCursorRight() throws IOException;
	void handleBackspace() throws IOException;
	void handleDelete() throws IOException;
	void handleNewline() throws IOException;
	void handleTab(final int tabCount) throws IOException;
	void handleShiftTab() throws IOException;

	void handleString(final String text) throws IOException;
	void handleCharacter(final int c) throws IOException;
	void setEcho(final boolean echo);

	void setBold(final boolean bold) throws IOException;
	void setTextColor(final int color) throws IOException;
	void setBackgroundColor(final int color) throws IOException;
	void setCursorColumnAbsolute(final int col) throws IOException;
	void setCursorColumnRelative(final int col) throws IOException;
	void setCursorPosition(final int x, final int y) throws IOException;
	void saveCursor() throws IOException;
	void restoreCursor() throws IOException;
}
