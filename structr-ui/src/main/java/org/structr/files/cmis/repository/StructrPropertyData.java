package org.structr.files.cmis.repository;

import org.structr.cmis.common.CMISExtensionsData;
import java.util.LinkedList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.PropertyData;

/**
 *
 * @author Christian Morgner
 */
public class StructrPropertyData<T> extends CMISExtensionsData implements PropertyData<T> {

	private final List<T> values = new LinkedList<>();
	private String displayName   = null;
	private String name          = null;
	private String id            = null;

	public StructrPropertyData(final String id, final String name, final String displayName, final T... values) {

		this.displayName = displayName;
		this.name        = name;
		this.id          = id;

		for (final T value : values) {
			this.values.add(value);
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getLocalName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getQueryName() {
		return getLocalName();
	}

	@Override
	public List<T> getValues() {
		return values;
	}

	@Override
	public T getFirstValue() {

		if (!values.isEmpty()) {
			return values.get(0);
		}

		return null;
	}

}
