package org.structr.cloud.message;

import java.util.List;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class ListPages implements Message<List<String>> {

	private List<String> pages = null;

	public ListPages() {}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		try {
			this.pages = context.listPages();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return this;
	}

	@Override
	public void postProcess(CloudConnection connection, final CloudContext context) {
	}

	@Override
	public List<String> getPayload() {
		return pages;
	}
}
