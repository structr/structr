package org.structr.files.ssh;

import java.io.IOException;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;

/**
 *
 * @author Christian Morgner
 */
public class StructrSSHFileSystemFactory implements FileSystemFactory {

	@Override
	public FileSystemView createFileSystemView(final Session session) throws IOException {
		return new StructrSSHFileSystemView(session);
	}
}
