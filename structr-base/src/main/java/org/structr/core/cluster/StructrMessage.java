package org.structr.core.cluster;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Christian Morgner
 */
public class StructrMessage implements Serializable {

 	private static final long serialVersionUID = 4782435385638563693L;

	private String type    = null;
	private Object payload = null;

	public StructrMessage(final String type, final Object payload) {

		this.type    = type;
		this.payload = payload;
	}

	public String getType() {
		return this.type;
	}

	public Object getRawPayload() {
		return this.payload;
	}

	public List getPayloadAsList() {
		return (List)this.payload;
	}
}
