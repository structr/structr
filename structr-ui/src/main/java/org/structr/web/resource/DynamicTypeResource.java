package org.structr.web.resource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.resource.Resource;
import org.structr.rest.resource.TypeResource;
import org.structr.rest.resource.TypedIdResource;
import org.structr.rest.resource.UuidResource;
import org.structr.web.common.UiFactoryDefinition;
import org.structr.web.entity.DataNode;
import org.structr.web.entity.PropertyDefinition;

/**
 *
 * @author Christian Morgner (christian@morgner.de)
 */
public class DynamicTypeResource extends TypeResource {

	private static final Logger logger = Logger.getLogger(TypeResource.class.getName());

	private String kind = null;
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		if (part != null) {

			this.kind = EntityContext.normalizeEntityName(part);
			
			if (PropertyDefinition.exists(this.kind)) {
				
				entityClass = UiFactoryDefinition.extender.getType(kind);
				
				return true;
			}
		}

		return false;

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			if (entityClass == null) {

				throw new NotFoundException();
			}

			searchAttributes.add(Search.andExactType(DataNode.class.getSimpleName()));
			searchAttributes.add(Search.andExactProperty(DataNode.kind, kind));
			searchAttributes.addAll(extractSearchableAttributesFromRequest(securityContext));
			
			// do search
			Result results = Services.command(securityContext, SearchNodeCommand.class).execute(
				includeDeletedAndHidden,
				publicOnly,
				searchAttributes,
				sortKey,
				sortDescending,
				pageSize,
				page,
				offsetId
			);
			
			return results;
			
		} else {

			logger.log(Level.WARNING, "type was null");
		}

		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				return createNode(propertySet);

			}

		};

		// execute transaction: create new node
		AbstractNode newNode    = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);

		if (newNode != null) {

			result.addHeader("Location", buildLocationHeader(newNode));
		}
		
		// finally: return 201 Created
		return result;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		throw new IllegalPathException();

	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public AbstractNode createNode(final Map<String, Object> propertySet) throws FrameworkException {

		PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);
		properties.put(AbstractNode.type, DataNode.class.getSimpleName());
		properties.put(DataNode.kind, kind);
		
		return (AbstractNode) Services.command(securityContext, CreateNodeCommand.class).execute(properties);
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			TypedIdResource constraint = new TypedIdResource(securityContext, (UuidResource) next, this);

			constraint.configureIdProperty(idProperty);

			return constraint;

		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {

		return rawType;

	}

	@Override
	public Class getEntityClass() {
		return entityClass;
	}

	@Override
	public String getResourceSignature() {

		return getUriPart();

	}

	@Override
	public boolean isCollectionResource() {

		return true;

	}
}
