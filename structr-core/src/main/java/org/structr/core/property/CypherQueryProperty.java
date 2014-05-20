package org.structr.core.property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.SortField;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CypherQueryCommand;

/**
 *
 * @author Christian Morgner
 */
public class CypherQueryProperty extends AbstractReadOnlyProperty<List<GraphObject>> {

	private String cypherQuery = null;

	public CypherQueryProperty(final String name, final String cypherQuery) {

		super(name);
		this.cypherQuery = cypherQuery;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public List<GraphObject> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public List<GraphObject> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, Predicate<GraphObject> predicate) {

		if (obj instanceof AbstractNode) {

			try {

				final Map<String, Object> parameters = new LinkedHashMap<>();

				parameters.put("id", obj.getUuid());
				parameters.put("type", obj.getType());

				return StructrApp.getInstance(securityContext).command(CypherQueryCommand.class).execute(cypherQuery, parameters);

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Integer getSortType() {
		return SortField.INT;
	}
}
