/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cmis.wrapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.PolicyIdList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import static org.apache.chemistry.opencmis.commons.enums.PropertyType.BOOLEAN;
import static org.apache.chemistry.opencmis.commons.enums.PropertyType.DATETIME;
import static org.apache.chemistry.opencmis.commons.enums.PropertyType.DECIMAL;
import static org.apache.chemistry.opencmis.commons.enums.PropertyType.INTEGER;
import static org.apache.chemistry.opencmis.commons.enums.PropertyType.STRING;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.api.config.Settings;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.cmis.info.CMISObjectInfo;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @param <T> the CMIS info type this wrapper is based on
 *
 *
 */
public abstract class CMISObjectWrapper<T extends CMISObjectInfo> extends CMISExtensionsData implements ObjectData, Acl {

	protected AllowableActions allowableActions      = null;
	protected List<Ace> aces                         = null;
	protected PropertyMap dynamicPropertyMap         = null;
	protected GregorianCalendar lastModificationDate = null;
	protected GregorianCalendar creationDate         = null;
	protected BaseTypeId baseTypeId                  = null;
	protected String lastModifiedBy                  = null;
	protected String propertyFilter                  = null;
	protected String description                     = null;
	protected String createdBy                       = null;
	protected String type                            = null;
	protected String name                            = null;
	protected String id                              = null;
	protected boolean includeActions                 = false;

	public abstract void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties);

	public CMISObjectWrapper(final BaseTypeId baseTypeId, final String propertyFilter, final Boolean includeActions) {

		this.includeActions     = includeActions != null && includeActions;
		this.propertyFilter     = propertyFilter;
		this.baseTypeId         = baseTypeId;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public BaseTypeId getBaseTypeId() {
		return baseTypeId;
	}

	public GregorianCalendar getLastModifiedDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(GregorianCalendar lastModifiedDate) {
		this.lastModificationDate = lastModifiedDate;
	}

	public GregorianCalendar getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(GregorianCalendar creationDate) {
		this.creationDate = creationDate;
	}

	public String getLastModifiedBy() {
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy) throws FrameworkException {
		this.lastModifiedBy = CMISObjectWrapper.translateIdToUsername(lastModifiedBy);
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) throws FrameworkException {
		this.createdBy = CMISObjectWrapper.translateIdToUsername(createdBy);
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setBaseTypeId(final BaseTypeId baseTypeId) {
		this.baseTypeId = baseTypeId;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setId(final String id) {
		this.id = id;
	}

	@Override
	public Properties getProperties() {

		final FilteredPropertyList properties  = new FilteredPropertyList(propertyFilter);
		final BindingsObjectFactory objFactory = new BindingsObjectFactoryImpl();

		properties.add(objFactory.createPropertyIdData(PropertyIds.BASE_TYPE_ID, baseTypeId.value()));
		properties.add(objFactory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, type));
		properties.add(objFactory.createPropertyIdData(PropertyIds.OBJECT_ID, id));

		properties.add(objFactory.createPropertyStringData(PropertyIds.NAME, name));
		properties.add(objFactory.createPropertyStringData(PropertyIds.DESCRIPTION, description));

		properties.add(objFactory.createPropertyStringData(PropertyIds.CREATED_BY, createdBy));
		properties.add(objFactory.createPropertyStringData(PropertyIds.LAST_MODIFIED_BY, lastModifiedBy));

		properties.add(objFactory.createPropertyDateTimeData(PropertyIds.CREATION_DATE, creationDate));
		properties.add(objFactory.createPropertyDateTimeData(PropertyIds.LAST_MODIFICATION_DATE, lastModificationDate));

		// add dynamic properties
		if (dynamicPropertyMap != null) {

			for (final Entry<PropertyKey, Object> entry : dynamicPropertyMap.entrySet()) {

				final PropertyKey key       = entry.getKey();
				final PropertyType dataType = key.getDataType();

				if (dataType != null) {

					switch (dataType) {

						case BOOLEAN:
							properties.add(objFactory.createPropertyBooleanData(key.jsonName(), (Boolean)entry.getValue()));
							break;

						case DATETIME:
							properties.add(objFactory.createPropertyDateTimeData(key.jsonName(), valueOrNull((Date)entry.getValue())));
							break;

						case DECIMAL:
							properties.add(objFactory.createPropertyDecimalData(key.jsonName(), valueOrNull((Double)entry.getValue())));
							break;

						case INTEGER:
							// INTEGER includes int and long so we have to cover both cases here
							properties.add(objFactory.createPropertyIntegerData(key.jsonName(), intOrNull(entry.getValue())));
							break;

						case STRING:
							properties.add(objFactory.createPropertyStringData(key.jsonName(), (String)entry.getValue()));
							break;
					}


				}
			}
		}

		// initialize type-dependent properties
		createProperties(objFactory, properties);

		// filter properties according to filter expression
		return objFactory.createPropertiesData(properties.getList());
	}

	@Override
	public AllowableActions getAllowableActions() {

		if (includeActions) {
			return allowableActions;
		}

		return null;
	}

	@Override
	public List<ObjectData> getRelationships() {
		return null;
	}

	@Override
	public ChangeEventInfo getChangeEventInfo() {
		return null;
	}

	@Override
	public Acl getAcl() {
		return this;
	}

	@Override
	public Boolean isExactAcl() {
		return false;
	}

	@Override
	public PolicyIdList getPolicyIds() {
		return null;
	}

	@Override
	public List<RenditionData> getRenditions() {
		return null;
	}

	public void initializeFrom(final T info) throws FrameworkException {

		setName(info.getName());
		setId(info.getUuid());
		setType(info.getType());
		setCreatedBy(info.getCreatedBy());
		setLastModifiedBy(info.getLastModifiedBy());
		setCreationDate(info.getCreationDate());
		setLastModificationDate(info.getLastModificationDate());

		dynamicPropertyMap = info.getDynamicProperties();
		allowableActions   = info.getAllowableActions();
		aces               = info.getAccessControlEntries();
	}

	// ----- public static methods -----
	public static CMISObjectWrapper wrap(final GraphObject source, final String propertyFilter, final Boolean includeAllowableActions) throws FrameworkException {

		CMISObjectWrapper wrapper = null;
		if (source != null) {

			final CMISInfo cmisInfo = source.getCMISInfo();
			if (cmisInfo != null) {

				final BaseTypeId baseTypeId = cmisInfo.getBaseTypeId();
				if (baseTypeId != null) {

					switch (baseTypeId) {

						case CMIS_DOCUMENT:
							wrapper = new CMISDocumentWrapper(propertyFilter, includeAllowableActions);
							wrapper.initializeFrom(cmisInfo.getDocumentInfo());
							break;

						case CMIS_FOLDER:
							wrapper = new CMISFolderWrapper(propertyFilter, includeAllowableActions);
							wrapper.initializeFrom(cmisInfo.getFolderInfo());
							break;

						case CMIS_ITEM:
							wrapper = new CMISItemWrapper(propertyFilter, includeAllowableActions);
							wrapper.initializeFrom(cmisInfo.getItemInfo());
							break;

						case CMIS_POLICY:
							wrapper = new CMISPolicyWrapper(propertyFilter, includeAllowableActions);
							wrapper.initializeFrom(cmisInfo.getPolicyInfo());
							break;

						case CMIS_RELATIONSHIP:
							wrapper = new CMISRelationshipWrapper(propertyFilter, includeAllowableActions);
							wrapper.initializeFrom(cmisInfo.getRelationshipInfo());
							break;

						case CMIS_SECONDARY:
							wrapper = new CMISSecondaryWrapper(propertyFilter, includeAllowableActions);
							wrapper.initializeFrom(cmisInfo.getSecondaryInfo());
							break;
					}
				}
			}
		}

		return wrapper;
	}

	// ----- interface Acl -----
	@Override
	public List<Ace> getAces() {
		return aces;
	}

	@Override
	public Boolean isExact() {
		return true;
	}

	// ----- public static methods -----
	public static String translateIdToUsername(final String id) throws FrameworkException {

		if (Principal.SUPERUSER_ID.equals(id)) {
			return Settings.SuperUserName.getValue();
		}

		final Principal principal = StructrApp.getInstance().get(Principal.class, id);
		if (principal != null) {

			return principal.getName();
		}

		return Principal.ANONYMOUS;
	}

	public static Principal translateUsernameToPrincipal(final String username) throws FrameworkException {

		if (Settings.SuperUserName.getValue().equals(username)) {
			return new SuperUser();
		}

		final Principal principal = StructrApp.getInstance().nodeQuery(Principal.class).andName(username).getFirst();
		if (principal != null) {

			return principal;
		}

		// return null for anonymous and all other cases
		return null;
	}

	// ----- protected methods -----
	protected BigDecimal valueOrNull(final Double source) {

		if (source != null) {
			return BigDecimal.valueOf(source);
		}

		return null;
	}

	protected BigInteger intOrNull(final Object source) {

		if (source != null && source instanceof Number) {

			final Number number = (Number)source;
			return BigInteger.valueOf(number.longValue());
		}

		return null;
	}

	protected GregorianCalendar valueOrNull(final Date source) {

		if (source != null) {

			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(source);

			return cal;
		}

		return null;
	}
}
