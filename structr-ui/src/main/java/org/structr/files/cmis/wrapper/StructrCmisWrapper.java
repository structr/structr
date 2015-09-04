package org.structr.files.cmis.wrapper;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.structr.core.GraphObject;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public abstract class StructrCmisWrapper extends CMISExtensionsData implements ObjectInFolderList {

	private final List<ObjectInFolderData> objects = new LinkedList<>();
	private String pathSegment                     = null;

	public StructrCmisWrapper(final String pathSegment) {
		this.pathSegment = pathSegment;
	}

	protected abstract <T extends GraphObject> ObjectData wrap(final T item) throws FrameworkException;

	public void wrap(final List<? extends GraphObject> list) throws FrameworkException {

		for (final GraphObject element : list) {

			final ObjectInFolderDataImpl data = new ObjectInFolderDataImpl(wrap(element));
			data.setPathSegment(pathSegment);

			objects.add(data);
		}
	}

	@Override
	public List<ObjectInFolderData> getObjects() {
		return objects;
	}

	@Override
	public Boolean hasMoreItems() {
		return false;
	}

	@Override
	public BigInteger getNumItems() {
		return BigInteger.valueOf(objects.size());
	}
}
