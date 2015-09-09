package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.MutableDocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableFolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableItemTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePolicyTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableRelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableSecondaryTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import static org.apache.chemistry.opencmis.commons.enums.BaseTypeId.CMIS_DOCUMENT;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.apache.commons.lang3.StringUtils;
import org.structr.cmis.CMISInfo;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.dynamic.File;
import org.structr.files.cmis.config.StructrRepositoryInfo;
import org.structr.schema.ConfigurationProvider;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class CMISRepositoryService extends AbstractStructrCmisService implements RepositoryService {

	private static final Logger logger = Logger.getLogger(CMISRepositoryService.class.getName());

	private final RepositoryInfo repositoryInfo = new StructrRepositoryInfo();

	public CMISRepositoryService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	@Override
	public List<RepositoryInfo> getRepositoryInfos(final ExtensionsData extension) {

		final List<RepositoryInfo> infoList = new LinkedList<>();

		infoList.add(repositoryInfo);

		return infoList;
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {

		if (repositoryId != null && repositoryId.equals(repositoryInfo.getId())) {
			return repositoryInfo;
		}

		throw new CmisObjectNotFoundException(repositoryId);
	}

	@Override
	public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		// important: children are the direct children of a type, as opposed to the descendants

		final Set<TypeDefinition> results = new LinkedHashSet<>();

		if (typeId != null) {

			final BaseTypeId baseTypeId = getBaseTypeId(typeId);
			if (baseTypeId != null) {

				results.addAll(getBaseTypeChildren(baseTypeId, includePropertyDefinitions));

			} else {

				final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeId);
				if (type != null) {

					results.addAll(getTypeChildren(typeId, includePropertyDefinitions));

				} else {

					throw new CmisObjectNotFoundException("Type with ID " + typeId + " does not exist");
				}
			}

		} else {

			results.add(getDocumentTypeDefinition(BaseTypeId.CMIS_DOCUMENT.value(), includePropertyDefinitions, true));
			results.add(getFolderTypeDefinition(BaseTypeId.CMIS_FOLDER.value(), includePropertyDefinitions, true));
			results.add(getItemTypeDefinition(BaseTypeId.CMIS_ITEM.value(), includePropertyDefinitions, true));
			results.add(getPolicyTypeDefinition(BaseTypeId.CMIS_POLICY.value(), includePropertyDefinitions, true));
			results.add(getRelationshipTypeDefinition(BaseTypeId.CMIS_RELATIONSHIP.value(), includePropertyDefinitions, true));
			results.add(getSecondaryTypeDefinition(BaseTypeId.CMIS_SECONDARY.value(), includePropertyDefinitions, true));
		}

		final TypeDefinitionListImpl returnValue = new TypeDefinitionListImpl();
		final List<TypeDefinition> unpagedList   = new LinkedList<>(results);
		final int resultSize                     = unpagedList.size();
		final List<TypeDefinition> pagedList     = applyPaging(unpagedList, maxItems, skipCount);
		int skip                                 = 0;

		if (skipCount != null) {
			skip = skipCount.intValue();
		}

		returnValue.setNumItems(BigInteger.valueOf(resultSize));
		returnValue.setList(pagedList);
		returnValue.setHasMoreItems(resultSize > pagedList.size() + skip);

		return returnValue;
	}

	@Override
	public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth, Boolean includePropertyDefinitions, ExtensionsData extension) {

		// important: descendants are ALL children and children of children, i.e. the whole tree

		/*
		Id typeId: The typeId of an object-type speciﬁed in the repository.
		 - If speciﬁed, then the repository MUST return all of descendant types of the speciﬁed type.
		 - If not speciﬁed, then the Repository MUST return all types and MUST ignore the value of the depth parameter.
		*/

		final List<TypeDefinitionContainer> results = new LinkedList<>();

		if (typeId != null) {

			final BaseTypeId baseTypeId = getBaseTypeId(typeId);
			if (baseTypeId != null) {

				final TypeDefinition typeDefinition     = getTypeDefinition(repositoryId, typeId, extension);
				final TypeDefinitionContainer container = getTypeDefinitionContainer(typeDefinition, includePropertyDefinitions);

				results.add(container);

			} else {

				final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeId);
				if (type != null) {

					final TypeDefinition typeDefinition = extendTypeDefinition(type, includePropertyDefinitions);
					if (typeDefinition != null) {

						results.add(getTypeDefinitionContainer(typeDefinition, includePropertyDefinitions));

					} else {

						throw new CmisObjectNotFoundException("Type with ID " + typeId + " does not exist");
					}

				} else {

					throw new CmisObjectNotFoundException("Type with ID " + typeId + " does not exist");
				}
			}

		} else {

			results.add(getTypeDefinitionContainer(getDocumentTypeDefinition(BaseTypeId.CMIS_DOCUMENT.value(), includePropertyDefinitions, true), includePropertyDefinitions));
			results.add(getTypeDefinitionContainer(getFolderTypeDefinition(BaseTypeId.CMIS_FOLDER.value(), includePropertyDefinitions, true), includePropertyDefinitions));
			results.add(getTypeDefinitionContainer(getItemTypeDefinition(BaseTypeId.CMIS_ITEM.value(), includePropertyDefinitions, true), includePropertyDefinitions));
			results.add(getTypeDefinitionContainer(getPolicyTypeDefinition(BaseTypeId.CMIS_POLICY.value(), includePropertyDefinitions, true), includePropertyDefinitions));
			results.add(getTypeDefinitionContainer(getRelationshipTypeDefinition(BaseTypeId.CMIS_RELATIONSHIP.value(), includePropertyDefinitions, true), includePropertyDefinitions));
			results.add(getTypeDefinitionContainer(getSecondaryTypeDefinition(BaseTypeId.CMIS_SECONDARY.value(), includePropertyDefinitions, true), includePropertyDefinitions));
		}

		return results;
	}

	@Override
	public TypeDefinition getTypeDefinition(final String repositoryId, final String typeId, final ExtensionsData extension) {

		switch (typeId) {

			case "cmis:document":
				return getDocumentTypeDefinition(typeId, true, true);

			case "cmis:folder":
				return getFolderTypeDefinition(typeId, true, true);

			case "cmis:item":
				return getItemTypeDefinition(typeId, true, true);

			case "cmis:policy":
				return getPolicyTypeDefinition(typeId, true, true);

			case "cmis:relationship":
				return getRelationshipTypeDefinition(typeId, true, true);

			case "cmis:secondary":
				return getSecondaryTypeDefinition(typeId, true, true);
		}

		final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeId);
		if (type != null) {

			final TypeDefinition extendedTypeDefinition = extendTypeDefinition(type, true);
			if (extendedTypeDefinition != null) {

				return extendedTypeDefinition;
			}
		}

		throw new CmisObjectNotFoundException("Type with ID " + typeId + " does not exist");
	}

	@Override
	public TypeDefinition createType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return null;
	}

	@Override
	public TypeDefinition updateType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return null;
	}

	@Override
	public void deleteType(String repositoryId, String typeId, ExtensionsData extension) {
	}

	// ----- private methods -----
	private MutableSecondaryTypeDefinition getSecondaryTypeDefinition(final String typeId, final boolean includePropertyDefinitions, final boolean baseType) {

		final TypeDefinitionFactory factory      = TypeDefinitionFactory.newInstance();
		final MutableSecondaryTypeDefinition def = factory.createSecondaryTypeDefinition(CmisVersion.CMIS_1_1, baseType ? null : BaseTypeId.CMIS_SECONDARY.value());

		initializeExtendedType(def, typeId);

		def.setIsCreatable(false);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableRelationshipTypeDefinition getRelationshipTypeDefinition(final String typeId, final boolean includePropertyDefinitions, final boolean baseType) {

		final TypeDefinitionFactory factory         = TypeDefinitionFactory.newInstance();
		final MutableRelationshipTypeDefinition def = factory.createRelationshipTypeDefinition(CmisVersion.CMIS_1_1, baseType ? null : BaseTypeId.CMIS_RELATIONSHIP.value());

		initializeExtendedType(def, typeId);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableItemTypeDefinition getItemTypeDefinition(final String typeId, final boolean includePropertyDefinitions, final boolean baseType) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableItemTypeDefinition def = factory.createItemTypeDefinition(CmisVersion.CMIS_1_1, baseType ? null : BaseTypeId.CMIS_ITEM.value());

		initializeExtendedType(def, typeId);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutablePolicyTypeDefinition getPolicyTypeDefinition(final String typeId, final boolean includePropertyDefinitions, final boolean baseType) {

		final TypeDefinitionFactory factory   = TypeDefinitionFactory.newInstance();
		final MutablePolicyTypeDefinition def = factory.createPolicyTypeDefinition(CmisVersion.CMIS_1_1, baseType ? null : BaseTypeId.CMIS_POLICY.value());

		initializeExtendedType(def, typeId);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableFolderTypeDefinition getFolderTypeDefinition(final String typeId, final boolean includePropertyDefinitions, final boolean baseType) {

		final TypeDefinitionFactory factory   = TypeDefinitionFactory.newInstance();
		final MutableFolderTypeDefinition def = factory.createFolderTypeDefinition(CmisVersion.CMIS_1_1, baseType ? null : BaseTypeId.CMIS_FOLDER.value());

		initializeExtendedType(def, typeId);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableDocumentTypeDefinition getDocumentTypeDefinition(final String typeId, final boolean includePropertyDefinitions, final boolean baseType) {

		final TypeDefinitionFactory factory     = TypeDefinitionFactory.newInstance();
		final MutableDocumentTypeDefinition def = factory.createDocumentTypeDefinition(CmisVersion.CMIS_1_1, baseType ? null : BaseTypeId.CMIS_DOCUMENT.value());

		initializeExtendedType(def, typeId);

		if (!includePropertyDefinitions) {

			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private List<TypeDefinition> getTypeChildren(final String typeId, final Boolean includePropertyDefinitions) {

		final Set<String> subtypes         = new LinkedHashSet<>(SearchCommand.getAllSubtypesAsStringSet(typeId));
		final ConfigurationProvider config = StructrApp.getConfiguration();
		final List<TypeDefinition> result  = new LinkedList<>();

		// subtypes set from Structr contains initial type as well..
		subtypes.remove(typeId);

		for (final String subtype : subtypes) {

			final Class subclass = config.getNodeEntityClass(subtype);
			if (subclass != null) {

				final TypeDefinition extendedTypeDefinition = extendTypeDefinition(subclass, includePropertyDefinitions);
				if (extendedTypeDefinition != null) {

					result.add(extendedTypeDefinition);
				}
			}
		}

		return result;
	}

	private List<TypeDefinition> getBaseTypeChildren(final BaseTypeId baseTypeId, final Boolean includePropertyDefinitions) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final List<TypeDefinition> result  = new LinkedList<>();
		final App app                      = StructrApp.getInstance();

		// static definition of base type children, add new types here!
		switch (baseTypeId) {

			case CMIS_DOCUMENT:
				result.add(extendTypeDefinition(File.class, includePropertyDefinitions));
				break;

			case CMIS_FOLDER:
				result.add(extendTypeDefinition(Folder.class, includePropertyDefinitions));
				break;

			case CMIS_ITEM:

				try (final Tx tx = app.tx()) {

					for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).sort(AbstractNode.name).getAsList()) {

						final Class type = config.getNodeEntityClass(schemaNode.getClassName());
						if (type != null) {

							final CMISInfo info = getCMISInfo(type);
							if (info != null && baseTypeId.equals(info.getBaseTypeId())) {

								final TypeDefinition extendedTypeDefinition = extendTypeDefinition(type, includePropertyDefinitions);
								if (extendedTypeDefinition != null) {

									result.add(extendedTypeDefinition);
								}
							}
						}
					}

					tx.success();

				} catch (final FrameworkException fex) {
					fex.printStackTrace();
				}
				break;
		}

		return result;
	}

	private TypeDefinitionContainer getTypeDefinitionContainer(final TypeDefinition typeDefinition, final Boolean includePropertyDefinitions) {

		final TypeDefinitionContainerImpl result = new TypeDefinitionContainerImpl();
		final List<TypeDefinitionContainer> list = new LinkedList<>();

		result.setTypeDefinition(typeDefinition);
		result.setChildren(list);

		final String typeId         = typeDefinition.getId();
		final BaseTypeId baseTypeId = getBaseTypeId(typeId);

		if (baseTypeId != null) {

			for (final TypeDefinition child : getBaseTypeChildren(baseTypeId, includePropertyDefinitions)) {

				list.add(getTypeDefinitionContainer(child, includePropertyDefinitions));
			}

		} else {

			for (final TypeDefinition child : getTypeChildren(typeDefinition.getId(), includePropertyDefinitions)) {

				list.add(getTypeDefinitionContainer(child, includePropertyDefinitions));
			}

		}

		return result;
	}

	private MutableTypeDefinition extendTypeDefinition(final Class<? extends GraphObject> type, final Boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory  = TypeDefinitionFactory.newInstance();
		final String typeName                = type.getSimpleName();
		MutableTypeDefinition result         = null;

		try {

			// instantiate class to obtain runtime CMIS information
			final GraphObject obj = type.newInstance();
			if (obj != null) {

				final CMISInfo info = obj.getCMISInfo();
				if (info != null) {

					final BaseTypeId baseTypeId = info.getBaseTypeId();
					if (baseTypeId != null) {

						switch (baseTypeId) {

							case CMIS_DOCUMENT:
								result = getDocumentTypeDefinition(typeName, includePropertyDefinitions, false);
								break;

							case CMIS_FOLDER:
								result = getFolderTypeDefinition(typeName, includePropertyDefinitions, false);
								break;

							case CMIS_ITEM:
								result = getItemTypeDefinition(typeName, includePropertyDefinitions, false);
								break;

							case CMIS_POLICY:
								result = getPolicyTypeDefinition(typeName, includePropertyDefinitions, false);
								break;

							case CMIS_RELATIONSHIP:
								result = getRelationshipTypeDefinition(typeName, includePropertyDefinitions, false);
								break;

							case CMIS_SECONDARY:
								result = getSecondaryTypeDefinition(typeName, includePropertyDefinitions, false);
								break;
						}

						if (result != null) {

							// initialize..
							for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(type, PropertyView.All)) {

								// include all dynamic and CMIS-enabled keys in definition
								if (key.isDynamic() || key.isCMISProperty()) {

									// only include primitives here
									final PropertyType dataType = key.getDataType();
									if (dataType != null) {


										final String propertyId         = key.jsonName();
										final String displayName        = propertyId;
										final String description        = StringUtils.capitalize(propertyId);
										final Class declaringClass      = key.getDeclaringClass();
										final boolean isInherited       = !type.getSimpleName().equals(declaringClass.getSimpleName());
										final Cardinality cardinality   = Cardinality.SINGLE;
										final Updatability updatability = Updatability.READWRITE;
										final boolean required          = key.isNotNull();
										final boolean queryable         = key.isIndexed();
										final boolean orderable         = key.isIndexed();

										// extend base type with dynamic property definition
										result.addPropertyDefinition(factory.createPropertyDefinition(
											propertyId,
											displayName,
											description,
											dataType,
											cardinality,
											updatability,
											isInherited,
											required,
											queryable,
											orderable
										));
									}
								}
							}
						}
					}
				}
			}

		} catch (final IllegalAccessException | InstantiationException iex) {
			iex.printStackTrace();
		}


		return result;
	}

	private CMISInfo getCMISInfo(final Class<? extends GraphObject> type) {

		try { return type.newInstance().getCMISInfo(); } catch (Throwable t) {}

		return null;
	}

	private BaseTypeId getBaseTypeId(final String typeId) {

		try { return BaseTypeId.fromValue(typeId); } catch (IllegalArgumentException iex) {}

		return null;
	}

	private void initializeExtendedType(final MutableTypeDefinition type, final String typeId) {

		type.setId(typeId);
		type.setLocalName(typeId);
		type.setQueryName(typeId);
		type.setDisplayName(typeId);
		type.setDescription(typeId);
	}
}
