/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.net;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.net.data.time.PseudoTime;


public interface SharedNodeInterface extends NodeInterface {

	public static final Property<String> lastModifiedPseudoTime = new StringProperty("lastModifiedPseudoTime");
	public static final Property<String> createdPseudoTime      = new StringProperty("createdPseudoTime");

	public static final View uiView = new View(SharedNodeInterface.class, PropertyView.Ui,
		lastModifiedPseudoTime, createdPseudoTime
	);

	static final Set<PropertyKey> nativeKeys = new LinkedHashSet<>(Arrays.asList(
		id, type, owner, visibleToPublicUsers, visibleToAuthenticatedUsers, hidden, deleted,
		createdDate, lastModifiedDate, visibilityStartDate, visibilityEndDate,
		createdBy, lastModifiedBy, lastModifiedPseudoTime, createdPseudoTime
	));



	Map<String, Object> getData();
	PseudoTime getCreationPseudoTime();
	PseudoTime getLastModificationPseudoTime();

	String getUserId();

	void setProperty(final App app, final PropertyKey key, final Object rawValue) throws FrameworkException;


	/*

	private boolean fullyCreated = false;

	@Override
	public void onNodeInstantiation(final boolean isCreation) {
		fullyCreated = !isCreation;
	}

	public Map<String, Object> getData() {

		final Set<PropertyKey> keys    = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(entityType, "shared"));
		final Map<String, Object> data = new HashMap<>();

		keys.removeAll(nativeKeys);

		for (final PropertyKey key : keys) {

			final PropertyConverter converter = key.inputConverter(securityContext);
			Object value = convert(getProperty(key));

			if (converter != null && !(value instanceof String)) {

				try {

					value = converter.revert(value);

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}
			}

			data.put(key.jsonName(), value);
		}

		return data;
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		fullyCreated = true;

		final PeerToPeerService service = getService();
		if (service != null) {

			service.create(this);
		}
	}

	@Override
	public void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {

		final PeerToPeerService service = getService();
		if (service != null) {

			service.delete(properties.get(GraphObject.id));
		}
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PeerToPeerService service = getService();
		if (service != null) {

			final PseudoTime time = service.getTime();

			super.setProperty(lastModifiedPseudoTime, time.toString());
			super.setProperty(createdPseudoTime,      time.toString());
		}

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PeerToPeerService service = getService();
		if (service != null) {

			service.update(this);
		}

		return super.onModification(securityContext, errorBuffer);
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {

		if (fullyCreated) {

			final PeerToPeerService service = getService();
			if (service != null) {

				service.setProperty(getProperty(GraphObject.id), key.jsonName(), convert(value));
			}
		}

		return super.setProperty(key, value);
	}

	public PseudoTime getCreationPseudoTime() {
		return PseudoTime.fromString(getProperty(createdPseudoTime));
	}

	public PseudoTime getLastModificationPseudoTime() {
		return PseudoTime.fromString(getProperty(lastModifiedPseudoTime));
	}

	public void setProperty(final App app, final PropertyKey key, final Object rawValue) throws FrameworkException {

		final PropertyConverter inputConverter = key.inputConverter(securityContext);
		Object value                           = revert(app, rawValue);

		if (inputConverter != null) {
			value = inputConverter.convert(value);
		}

		super.setProperty(key, value);
	}

	public String getUserId() {

		final Principal owner = getOwnerNode();
		if (owner != null) {

			return owner.getName();
		}

		return Principal.SUPERUSER_ID;
	}

	// ----- private methods -----
	private PeerToPeerService getService() {
		return Services.getInstance().getService(PeerToPeerService.class);
	}

	private Object convert(final Object value) {

		Object result = value;

		if (value instanceof GraphObject) {

			result = "!" + ((GraphObject)value).getUuid();

		} else if (value instanceof Collection) {

			final List<Object> list = new LinkedList<>();
			for (final Object item : ((Collection)value)) {

				// recurse
				list.add(convert(item));
			}

			result = "#" + StringUtils.join(list, ",");
		}

		return result;
	}

	private Object revert(final App app, final Object value) throws FrameworkException {

		Object result = value;

		if (value instanceof String) {

			final String str = (String)value;

			if (str.startsWith("#")) {

				if (str.length() > 1) {

					final String[] parts    = str.substring(1).split("[,]+");
					final List<Object> list = new LinkedList<>();

					for (final String part : parts) {
						list.add(revert(app, part));
					}

					result = list;

				} else {

					result = Collections.emptyList();
				}

			} else if (str.startsWith("!") && str.length() == 33) {

				result = app.get(str.substring(1));
			}
		}

		return result;
	}
	*/
}
