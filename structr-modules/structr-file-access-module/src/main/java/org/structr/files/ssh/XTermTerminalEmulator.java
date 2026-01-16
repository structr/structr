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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 *
 */
public class XTermTerminalEmulator extends AbstractTerminalEmulator {

	public XTermTerminalEmulator(final InputStream in, final OutputStream out, final TerminalHandler handler) {
		super(in, out, handler);
	}

	@Override
	public void handlePageUp() throws IOException {
	}

	@Override
	public void handlePageDown() throws IOException {
	}

	@Override
	public void handleInsert() throws IOException {
	}

	@Override
	public void handleHome() throws IOException {

		final int offset = lineLength;
		for (int i=0; i<offset; i++) {

			handleCursorLeft();
		}
	}

	@Override
	public void handleEnd() throws IOException {

		final int offset = lineLength - cursorPosition;
		for (int i=0; i<offset; i++) {

			handleCursorRight();
		}
	}

	@Override
	public void handleCursorLeft() throws IOException {

		if (cursorPosition > 0 && echo) {

			if (echo) {

				writer.write(27);
				writer.write(91);
				writer.write(68);
			}

			cursorPosition--;
		}
	}

	@Override
	public void handleCursorRight() throws IOException {

		if (cursorPosition < lineLength) {

			if (echo) {

				writer.write(27);
				writer.write(91);
				writer.write(67);
			}

			cursorPosition++;
		}
	}

	@Override
	public void handleBackspace() throws IOException {

		if (echo) {

			if (cursorPosition > 0) {

				if (cursorPosition < lineLength) {

					writer.write(8);
					cursorPosition--;

					handleDelete();

				} else {

					writer.write(8);
					writer.write(' ');
					writer.write(8);

					cursorPosition--;
					lineBuffer.deleteCharAt(cursorPosition);
				}
			}

		} else {

			if (cursorPosition > 0) {

				cursorPosition--;
				lineBuffer.deleteCharAt(cursorPosition);
			}

		}

		lineLength = lineBuffer.length();
	}

	@Override
	public void handleDelete() throws IOException {

		if (cursorPosition >= 0 && cursorPosition < lineLength) {

			if (echo) {

				writer.write(27);
				writer.write('[');
				writer.write('1');
				writer.write('P');
			}

			lineBuffer.deleteCharAt(cursorPosition);

			lineLength = lineBuffer.length();
		}
	}

	@Override
	public void handleNewline() throws IOException {

		final String line = lineBuffer.toString();
		lineBuffer.setLength(0);
		cursorPosition = 0;
		lineLength = 0;

		println();

		handleLineInternal(line);

		// let the terminal handler display its prompt
		terminalHandler.displayPrompt();
	}

	@Override
	public void println() throws IOException {
		writer.write(10);
		writer.write(13);
	}

	@Override
	public void handleTab(final int tabCount) throws IOException {
		terminalHandler.handleTab(tabCount);
	}

	@Override
	public void handleShiftTab() throws IOException {
		terminalHandler.handleShiftTab();
	}

	@Override
	public void setBold(final boolean bold) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(bold ? '1' : '0');
		writer.write('m');
		writer.flush();
	}

	@Override
	public void setTextColor(int color) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write('3');
		writer.write(Integer.toString(color));
		writer.write('m');
		writer.flush();
	}

	@Override
	public void setBackgroundColor(int color) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write('4');
		writer.write(Integer.toString(color));
		writer.write('m');
		writer.flush();
	}

	@Override
	public void handleString(final String text) throws IOException {

		final int len = text.length();

		for (int i=0; i<len; i++) {
			handleCharacter(text.codePointAt(i));
		}
	}

	@Override
	public void handleCharacter(final int c) throws IOException {

		// "insert" behaviour when not at end of line
		if (cursorPosition < lineLength) {

			if (echo) {

				writer.write(27);
				writer.write('[');
				writer.write('1');
				writer.write('@');
			}
		}

		if (echo) {
			writer.write(c);
		}

		lineBuffer.insert(cursorPosition, new String(new int[] { c }, 0, 1));
		cursorPosition++;

		lineLength = lineBuffer.length();
	}

	@Override
	public void setCursorColumnAbsolute(final int col) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(Integer.toString(col));
		writer.write('`');
		writer.flush();
	}

	@Override
	public void setCursorColumnRelative(final int col) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(Integer.toString(col));
		writer.write('a');
		writer.flush();
	}

	@Override
	public void setCursorPosition(final int x, final int y) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(Integer.toString(x));
		writer.write(Integer.toString(y));
		writer.write('H');
		writer.flush();
	}

	@Override
	public void saveCursor() throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write('s');
		writer.flush();
	}

	@Override
	public void restoreCursor() throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write('u');
		writer.flush();

	}
}
