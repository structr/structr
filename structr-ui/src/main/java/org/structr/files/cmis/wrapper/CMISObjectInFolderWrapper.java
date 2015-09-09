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
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class CMISObjectInFolderWrapper extends CMISExtensionsData implements ObjectInFolderList {

	private final List<ObjectInFolderData> objects = new LinkedList<>();
	private Boolean includeAllowableActions        = false;

	public CMISObjectInFolderWrapper(final Boolean includeAllowableActions) {
		this.includeAllowableActions = includeAllowableActions;
	}

	public ObjectInFolderData wrapObjectData(final ObjectData element, final String pathSegment) {

		final ObjectInFolderDataImpl data = new ObjectInFolderDataImpl(element);
		data.setPathSegment(pathSegment);

		return data;
	}

	public ObjectData wrapGraphObject(final GraphObject item) throws FrameworkException {
		return CMISObjectWrapper.wrap(item, includeAllowableActions);
	}

	public void wrap(final List<? extends GraphObject> list) throws FrameworkException {

		for (final GraphObject element : list) {
			objects.add(wrapObjectData(wrapGraphObject(element), element.getProperty(AbstractNode.name)));
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
