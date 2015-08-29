package org.structr.files.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Christian Morgner
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
					handleDelete();

				} else {

					writer.write(8);
					writer.write(' ');
					writer.write(8);

					if (cursorPosition > 0) {
						cursorPosition--;
					}

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

		if (cursorPosition > 0 && cursorPosition < lineLength) {

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
	public void handleTab() {
	}

	@Override
	public void setBold(final boolean bold) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(bold ? '1' : '0');
		writer.write('m');
	}

	@Override
	public void setTextColor(int color) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write('3');
		writer.write(Integer.toString(color));
		writer.write('m');
	}

	@Override
	public void setBackgroundColor(int color) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write('4');
		writer.write(Integer.toString(color));
		writer.write('m');
	}

	@Override
	public void handleCharacter(final int c) throws IOException {

		// "insert" behaviour when not at end of line
		if (cursorPosition < lineLength-1) {

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

	}

	@Override
	public void setCursorColumnRelative(final int col) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(Integer.toString(col));
		writer.write('a');

	}

	@Override
	public void setCursorPosition(final int x, final int y) throws IOException {

		writer.write(27);
		writer.write('[');
		writer.write(Integer.toString(x));
		writer.write(Integer.toString(y));
		writer.write('H');

	}
}
