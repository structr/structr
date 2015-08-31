package org.structr.files.ssh;

import java.io.IOException;

/**
 *
 * @author Christian Morgner
 */
public interface TerminalEmulator {

	public void setTerminalHandler(final TerminalHandler handler) throws IOException;
	public void restoreRootTerminalHandler() throws IOException;
	public void start();
	public void stopEmulator();

	public void clearLineBuffer();
	public StringBuilder getLineBuffer();
	public void handleCtrlKey(final int key) throws IOException;

	public void handleCursorUp() throws IOException;
	public void handleCursorDown() throws IOException;
	public void handlePageUp() throws IOException;
	public void handlePageDown() throws IOException;
	public void handleInsert() throws IOException;
	public void handleHome() throws IOException;
	public void handleEnd() throws IOException;
	public void handleCursorLeft() throws IOException;
	public void handleCursorRight() throws IOException;
	public void handleBackspace() throws IOException;
	public void handleDelete() throws IOException;
	public void handleNewline() throws IOException;
	public void handleTab(final int tabCount) throws IOException;

	public void handleString(final String text) throws IOException;
	public void handleCharacter(final int c) throws IOException;
	public void setEcho(final boolean echo);

	public void print(final String text) throws IOException;
	public void println(final String text) throws IOException;
	public void println() throws IOException;

	public void setBold(final boolean bold) throws IOException;
	public void setTextColor(final int color) throws IOException;
	public void setBackgroundColor(final int color) throws IOException;
	public void setCursorColumnAbsolute(final int col) throws IOException;
	public void setCursorColumnRelative(final int col) throws IOException;
	public void setCursorPosition(final int x, final int y) throws IOException;
	public void saveCursor() throws IOException;
	public void restoreCursor() throws IOException;

	public void flush() throws IOException;
}
