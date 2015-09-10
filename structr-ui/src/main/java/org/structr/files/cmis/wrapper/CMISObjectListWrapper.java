package org.structr.files.cmis.wrapper;

import java.math.BigInteger;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;

/**
 *
 * @author Christian Morgner
 */
public class CMISObjectListWrapper extends CMISPagingListWrapper<ObjectData> implements ObjectList {

	public CMISObjectListWrapper() {
		super();
	}

	public CMISObjectListWrapper(final BigInteger maxItems, final BigInteger skipCount) {
		super(maxItems, skipCount);
	}

	@Override
	public List<ObjectData> getObjects() {
		return getPagedList();
	}
}
