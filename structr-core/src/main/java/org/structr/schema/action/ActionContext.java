package org.structr.schema.action;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Ownership;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class ActionContext {

	public String convertValueForHtml(final Object value) {

		if (value != null) {

			// TODO: do more intelligent conversion here
			return value.toString();
		}

		return null;
	}

	public boolean returnRawValue(final SecurityContext securityContext) {
		return false;
	}

	public Object getReferencedProperty(final SecurityContext securityContext, final NodeInterface entity, final String refKey) throws FrameworkException {

		final String DEFAULT_VALUE_SEP = "!";
		final String[] parts           = refKey.split("[\\.]+");
		String referenceKey            = parts[parts.length - 1];
		String defaultValue            = null;

		if (StringUtils.contains(referenceKey, DEFAULT_VALUE_SEP)) {

			String[] ref = StringUtils.split(referenceKey, DEFAULT_VALUE_SEP);
			referenceKey = ref[0];
			if (ref.length > 1) {

				defaultValue = ref[1];

			} else {

				defaultValue = "";
			}
		}

		GraphObject _data = null;

		// walk through template parts
		for (int i = 0; (i < parts.length); i++) {

			String part = parts[i];
			String lowerCasePart = part.toLowerCase();

			if (_data != null) {

				Object value = _data.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), part));

				if (value instanceof GraphObject) {
					_data = (GraphObject) value;

					continue;

				}

				// special keyword "size"
				if (i > 0 && "size".equals(lowerCasePart)) {

					Object val = _data.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), parts[i - 1]));

					if (val instanceof List) {

						return ((List) val).size();

					}

				}

				if (value == null) {

					// Need to return null here to avoid _data sticking to the (wrong) parent object
					return null;

				}

			}

			// special keyword "now":
			if ("now".equals(lowerCasePart)) {

				// Return current date converted in format
				// Note: We use "createdDate" here only as an arbitrary property key to get the database converter
				return AbstractNode.createdDate.inputConverter(securityContext).revert(new Date());

			}

			// special keyword "me"
			if ("me".equals(lowerCasePart)) {

				Principal me = (Principal) securityContext.getUser(false);

				if (me != null) {

					_data = me;

					continue;
				}

			}

			// the following keywords work only on root level
			// so that they can be used as property keys for data objects
			if (_data == null) {

				// details data object id
				if ("id".equals(lowerCasePart)) {

					return entity.getUuid();
				}

				// special keyword "this"
				if ("this".equals(lowerCasePart)) {

					_data = entity;

					continue;

				}

				// special keyword "element"
				if ("element".equals(lowerCasePart)) {

					_data = entity;

					continue;

				}

				// special keyword "owner"
				if ("owner".equals(lowerCasePart)) {

					Ownership rel = entity.getIncomingRelationship(PrincipalOwnsNode.class);
					if (rel != null) {

						_data = rel.getSourceNode();
					}

					continue;

				}

			}

		}

		if (_data != null) {

			PropertyKey referenceKeyProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), referenceKey);
			PropertyConverter converter      = referenceKeyProperty.inputConverter(securityContext);
			Object value                     = _data.getProperty(referenceKeyProperty);

			if (value != null && converter != null) {
				value = converter.revert(value);
			}

			return value != null ? value : defaultValue;

		}

		return null;


	}
}
