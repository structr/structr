package org.structr.core.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
 *
 * @author Christian Morgner
 */
public class JointProperty extends StringProperty {

	private List<PropertyKey> keys = new ArrayList<>();
	private String separator       = null;

	public JointProperty(final String name, final String separator, final PropertyKey... keys) {
		this(name, name, separator, keys);
	}

	public JointProperty(final String jsonName, final String dbName, final String separator, final PropertyKey... keys) {
		super(jsonName, dbName);

		this.separator = separator;
		this.keys.addAll(Arrays.asList(keys));
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate){

		StringBuilder buf = new StringBuilder();

		for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {

			final PropertyKey key                  = it.next();
			final PropertyConverter inputConverter = key.inputConverter(securityContext);

			if (inputConverter != null) {

				try {
					buf.append(inputConverter.revert(key.getProperty(securityContext, obj, applyConverter, predicate)));

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

			} else {

				buf.append(key.getProperty(securityContext, obj, applyConverter, predicate));
			}

			if (it.hasNext()) {
				buf.append(separator);
			}
		}

		return buf.toString();
	}

	@Override
	public void setProperty(SecurityContext securityContext, final GraphObject obj, String value) throws FrameworkException {

		final String[] parts = StringUtils.split(value, separator);
		final int len        = parts.length;

		for (int i=0; i<len; i++) {

			final PropertyKey key                  = keys.get(i);
			final PropertyConverter inputConverter = key.inputConverter(securityContext);

			if (inputConverter != null) {

				key.setProperty(securityContext, obj, inputConverter.convert(parts[i]));

			} else {

				key.setProperty(securityContext, obj, parts[i]);
			}
		}
	}
}
