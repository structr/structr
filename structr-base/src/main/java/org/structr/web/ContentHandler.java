package org.structr.web;

import org.structr.common.error.FrameworkException;

import java.io.IOException;

public interface ContentHandler {
	void handleScript(String script, int row, int column) throws FrameworkException, IOException;

	void handleIncompleteScript(String script) throws FrameworkException, IOException;

	void handleText(String text) throws FrameworkException;

	void possibleStartOfScript(int row, int column);
}
