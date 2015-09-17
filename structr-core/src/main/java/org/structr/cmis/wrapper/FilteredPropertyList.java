package org.structr.cmis.wrapper;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Christian Morgner
 */
public class FilteredPropertyList {

	private final Set<String> acceptedProperties = new LinkedHashSet<>();
	private final List<PropertyData<?>> list     = new LinkedList<>();
	private boolean doFilter                     = false;

	public FilteredPropertyList(final String propertyFilter) {

		initialize(propertyFilter);

		if (StringUtils.isNotBlank(propertyFilter) && !"*".equals(propertyFilter)) {
			doFilter = true;
		}
	}

	public List<PropertyData<?>> getList() {
		return list;
	}

	public void add(final PropertyData<?> property) {

		if (property != null && (!doFilter || acceptedProperties.contains(property.getId()))) {

			list.add(property);
		}
	}

	private void initialize(final String source) {

		if (source != null) {

			for (final String part : source.split("[,]+")) {

				final String trimmedPart = part.trim();
				if (!trimmedPart.isEmpty()) {

					acceptedProperties.add(trimmedPart);
				}
			}
		}
	}
}
