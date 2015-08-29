package org.structr.files.ssh;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public class StructrSSHFileSystemView implements FileSystemView {

	private StructrSSHFile rootFolder = null;
	private Session session           = null;

	public StructrSSHFileSystemView(final Session session) {

		this.rootFolder = new StructrSSHFile(SecurityContext.getSuperUserInstance());
		this.session    = session;
	}

	@Override
	public SshFile getFile(final String path) {

		if ("/".equals(path)) {
			return rootFolder;
		}

		return rootFolder.findFile(path);
	}

	@Override
	public SshFile getFile(final SshFile baseDir, final String file) {

		if (baseDir instanceof StructrSSHFile) {

			return ((StructrSSHFile)baseDir).findFile(file);
		}

		return rootFolder.findFile(baseDir.getAbsolutePath() + file);
	}

	@Override
	public FileSystemView getNormalizedView() {
		return this;
	}
}
