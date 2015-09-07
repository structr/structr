package org.structr.cmis.wrapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.PolicyIdList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISObjectInfo;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @param <T> the CMIS info type this wrapper is based on
 *
 * @author Christian Morgner
 */
public abstract class CMISObjectWrapper<T extends CMISObjectInfo> extends CMISExtensionsData implements ObjectData {

	private BaseTypeId baseTypeId                  = null;
	private PropertyMap dynamicPropertyMap         = null;
	private GregorianCalendar lastModificationDate = null;
	private GregorianCalendar creationDate         = null;
	private String lastModifiedBy                  = null;
	private String createdBy                       = null;
	private String type                            = null;
	private String name                            = null;
	private String id                              = null;

	public abstract void createProperties(final BindingsObjectFactory factory, final List<PropertyData<?>> properties);

	public CMISObjectWrapper(final BaseTypeId baseTypeId) {
		this.baseTypeId = baseTypeId;
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
		this.lastModifiedBy = translateIdToUsername(lastModifiedBy);
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) throws FrameworkException {
		this.createdBy = translateIdToUsername(createdBy);
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
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

		final BindingsObjectFactory objFactory = new BindingsObjectFactoryImpl();
		final List<PropertyData<?>> properties = new LinkedList<>();

		properties.add(objFactory.createPropertyIdData(PropertyIds.BASE_TYPE_ID, baseTypeId.value()));
		properties.add(objFactory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID, type));
		properties.add(objFactory.createPropertyIdData(PropertyIds.OBJECT_ID, id));

		properties.add(objFactory.createPropertyStringData(PropertyIds.NAME, name));

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
							final Boolean booleanValue = (Boolean)entry.getValue();
							properties.add(objFactory.createPropertyBooleanData(key.jsonName(), booleanValue));
							break;

						case DATETIME:
							final GregorianCalendar dateValue = new GregorianCalendar();
							dateValue.setTime((Date)entry.getValue());
							properties.add(objFactory.createPropertyDateTimeData(key.jsonName(), dateValue));
							break;

						case DECIMAL:
							final BigDecimal decimalValue = BigDecimal.valueOf((Double)entry.getValue());
							properties.add(objFactory.createPropertyDecimalData(key.jsonName(), decimalValue));
							break;

						case INTEGER:
							final BigInteger integerValue = BigInteger.valueOf((Long)entry.getValue());
							properties.add(objFactory.createPropertyIntegerData(key.jsonName(), integerValue));
							break;

						case STRING:
							final String stringValue = (String)entry.getValue();
							properties.add(objFactory.createPropertyStringData(key.jsonName(), stringValue));
							break;
					}


				}
			}
		}

		// initialize type-dependent properties
		createProperties(objFactory, properties);

		return objFactory.createPropertiesData(properties);
	}

	@Override
	public AllowableActions getAllowableActions() {
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
		return null;
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
	}

	// ----- public static methods -----
	public static CMISObjectWrapper wrap(final GraphObject source) throws FrameworkException {

		CMISObjectWrapper wrapper = null;
		if (source != null) {

			final CMISInfo cmisInfo = source.getCMISInfo();
			if (cmisInfo != null) {

				final BaseTypeId baseTypeId = cmisInfo.getBaseTypeId();
				if (baseTypeId != null) {

					switch (baseTypeId) {

						case CMIS_DOCUMENT:
							wrapper = new CMISDocumentWrapper();
							wrapper.initializeFrom(cmisInfo.getDocumentInfo());
							break;

						case CMIS_FOLDER:
							wrapper = new CMISFolderWrapper();
							wrapper.initializeFrom(cmisInfo.getFolderInfo());
							break;

						case CMIS_ITEM:
							wrapper = new CMISItemWrapper();
							wrapper.initializeFrom(cmisInfo.geItemInfo());
							break;

						case CMIS_POLICY:
							wrapper = new CMISPolicyWrapper();
							wrapper.initializeFrom(cmisInfo.getPolicyInfo());
							break;

						case CMIS_RELATIONSHIP:
							wrapper = new CMISRelationshipWrapper();
							wrapper.initializeFrom(cmisInfo.getRelationshipInfo());
							break;

						case CMIS_SECONDARY:
							wrapper = new CMISSecondaryWrapper();
							wrapper.initializeFrom(cmisInfo.getSecondaryInfo());
							break;
					}
				}
			}
		}

		return wrapper;
	}

	// ----- private methods -----
	private String translateIdToUsername(final String id) throws FrameworkException {

		if (Principal.SUPERUSER_ID.equals(id)) {
			return StructrApp.getConfigurationValue(Services.SUPERUSER_USERNAME);
		}

		final Principal principal = StructrApp.getInstance().get(Principal.class, id);
		if (principal != null) {

			return principal.getName();
		}

		return Principal.ANONYMOUS;
	}
}
