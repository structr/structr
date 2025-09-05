/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sshd.common.channel.exception.SshChannelClosedException;

import java.io.*;
import java.util.List;

/**
 *
 *
 */
public abstract class AbstractTerminalEmulator extends Thread implements TerminalEmulator {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTerminalEmulator.class.getName());

	protected final StringBuilder lineBuffer      = new StringBuilder();
	protected TerminalHandler rootTerminalHandler = null;
	protected TerminalHandler terminalHandler     = null;
	protected Reader reader                       = null;
	protected Writer writer                       = null;
	protected boolean running                     = false;
	protected boolean echo                        = true;
	protected int cursorPosition                  = 0;
	protected int lineLength                      = 0;
	protected int commandBufferIndex              = 0;
	protected int tabCount                        = 0;

	public AbstractTerminalEmulator(final InputStream in, final OutputStream out, final TerminalHandler rootTerminalHandler) {

		this.rootTerminalHandler = rootTerminalHandler;
		this.terminalHandler     = rootTerminalHandler;
		this.reader              = new InputStreamReader(in);
		this.writer              = new OutputStreamWriter(out);
	}

	@Override
	public void stopEmulator() {
		this.running = false;
	}

	@Override
	public void setTerminalHandler(final TerminalHandler handler) throws IOException {
		this.terminalHandler = handler;
	}

	@Override
	public void restoreRootTerminalHandler() throws IOException {

		this.terminalHandler = rootTerminalHandler;
		setEcho(true);
	}

	@Override
	public void handleCursorUp() throws IOException {

		final List<String> commandHistory = terminalHandler.getCommandHistory();
		if (commandHistory != null && echo) {

			final int commandBufferSize = commandHistory.size();

			if (commandBufferIndex >= 0 && commandBufferIndex < commandBufferSize) {

				displaySelectedCommand(commandHistory.get(commandBufferSize - commandBufferIndex - 1));

				if (commandBufferIndex < commandBufferSize - 1) {
					commandBufferIndex++;
				}
			}
		}
	}

	@Override
	public void handleCursorDown() throws IOException {

		final List<String> commandHistory = terminalHandler.getCommandHistory();
		if (commandHistory != null && echo) {

			if (commandBufferIndex > 0) {

				final int commandBufferSize = commandHistory.size();

				if (commandBufferIndex >= 0 && commandBufferIndex <= commandBufferSize) {

					commandBufferIndex--;
					displaySelectedCommand(commandHistory.get(commandBufferSize - commandBufferIndex - 1));
				}

			} else {

				displaySelectedCommand("");
			}
		}
	}

	@Override
	public void handleCtrlKey(final int key) throws IOException {

		// 0 is Ctrl-A, 1 is Ctrl-B, etc..
		switch (key) {

			case 3:
				terminalHandler.handleCtrlC();
				break;

			case 4:

				if (lineLength == 0) {
					terminalHandler.handleLogoutRequest();
				}
				break;
		}
	}

	@Override
	public void run() {

		running = true;

		while (running) {

			try {

				int c = reader.read();

				// global tab count
				if (c == 9) {

					tabCount++;

				} else {

					tabCount = 0;
				}

				switch (c) {

					case 9:
						handleTab(tabCount);
						break;

					case 13:
						handleNewline();
						break;

					case 27:
						// escape sequence
						c = reader.read();
						switch (c) {

							case 91:
								// cursor keys
								c = reader.read();
								switch (c) {

									case 50:
										// insert
										c = reader.read();
										switch (c) {

											case 126:

												handleInsert();
												break;
										}
										break;

									case 51:
										// delete
										c = reader.read();
										switch (c) {

											case 126:

												handleDelete();
												break;
										}
										break;

									case 53:
										// page up
										c = reader.read();
										switch (c) {

											case 126:

												handlePageUp();
												break;
										}
										break;

									case 54:
										// page down
										c = reader.read();
										switch (c) {

											case 126:

												handlePageDown();
												break;
										}
										break;

									case 65:
										// up
										handleCursorUp();
										break;

									case 66:
										// down
										handleCursorDown();
										break;

									case 67:

										handleCursorRight();
										break;

									case 68:

										handleCursorLeft();
										break;

									case 70:

										handleEnd();
										break;

									case 72:

										handleHome();
										break;

									case 90:
										handleShiftTab();
										break;
								}
								break;

						}
						break;

					case 127:

						handleBackspace();
						break;

					default:

						if (c < 27) {

							handleCtrlKey(c);

						} else {

							// read unicode character
							handleCharacter(c);
						}
						break;
				}
				writer.flush();

			} catch (Throwable t) {

				if (t instanceof SshChannelClosedException) {
					logger.warn("SSH Channel closed unexpectedly");
					terminalHandler.handleExit();
					return;
				}

				logger.warn("Exception", t);

				try {

					writer.write('\n');
					writer.write(t.getMessage());
					writer.write('\n');

				} catch (Throwable t2) {
					logger.warn("", t);
				}
			}
		}

		terminalHandler.handleExit();
	}

	@Override
	public void print(final Object... text) throws IOException {

		if (text != null) {

			for (final Object o : text) {

				if (o != null) {

					writer.write(o.toString().replaceAll("\n", "\r\n"));

				} else {

					writer.write("null");
				}
			}
		}
		writer.flush();
	}

	@Override
	public void println(final Object... text) throws IOException {
		print(text);
		println();
		writer.flush();
	}

	@Override
	public void clearLineBuffer() {
		lineBuffer.setLength(0);
		cursorPosition = 0;
		lineLength = 0;
	}

	@Override
	public StringBuilder getLineBuffer() {
		return lineBuffer;
	}

	@Override
	public void setEcho(final boolean echo) {
		this.echo = echo;
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void clearTabCount() {
		tabCount = 0;
	}

	// ----- protected methods -----
	protected void handleLineInternal(final String line) throws IOException {

		terminalHandler.handleLine(line);
		commandBufferIndex = 0;
	}

	// ----- private methods -----
	private void displaySelectedCommand(final String selectedCommand) throws IOException {

		lineBuffer.setLength(0);
		lineBuffer.append(selectedCommand);
		lineLength = lineBuffer.length();

		int loopCount = cursorPosition;
		for (int i=0; i<loopCount; i++) {
			handleCursorLeft();
		}

		writer.write(27);
		writer.write('[');
		writer.write('K');

		print(selectedCommand);

		cursorPosition = lineLength;
	}
}
