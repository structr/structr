package org.structr.files.ssh;

import org.structr.agent.AbstractTask;
import org.structr.web.entity.FileBase;

/**
 *
 * @author Christian Morgner
 */
public class FulltextIndexingTask extends AbstractTask<FileBase> {

	public FulltextIndexingTask(final FileBase file) {
		super(FulltextIndexingAgent.TASK_NAME, null, file);
	}
}
