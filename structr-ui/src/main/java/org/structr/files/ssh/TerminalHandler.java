package org.structr.files.ssh;

import java.io.IOException;
import java.util.List;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public interface TerminalHandler {

	public List<String> getCommandHistory();

	public void displayPrompt() throws IOException;

	public void handleExit();
	public void handleLine(final String line) throws IOException;
	public void handleLogoutRequest() throws IOException;
	public void handleCtrlC() throws IOException;
	public void handleTab(final int tabCount) throws IOException;

	public void setUser(final User user);
	public User getUser();
}
