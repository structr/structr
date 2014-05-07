package org.structr.cloud;

import java.util.List;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class ListPagesPacket implements Message<List<String>> {

	private List<String> pages = null;

	public ListPagesPacket() {}

	@Override
	public Message process(final ServerContext context) {

		try {
			this.pages = context.listPages();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return this;
	}

	@Override
	public void postProcess(final ServerContext context) {
	}

	@Override
	public List<String> getPayload() {
		return pages;
	}
}
