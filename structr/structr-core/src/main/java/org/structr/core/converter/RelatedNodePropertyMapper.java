
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.core.converter;

import org.structr.common.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class RelatedNodePropertyMapper extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(RelatedNodePropertyMapper.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		if (value != null) {

			Object param = value.get();

			if ((param != null) && (param instanceof ParameterHolder)) {

				ParameterHolder holder   = (ParameterHolder) param;
				PropertyKey targetKey    = holder.getTargetKey();
				Class targetType         = holder.getTargetType();
				AbstractNode relatedNode = getRelatedNode(targetType);

				if (relatedNode != null) {

					try {
						relatedNode.setProperty(targetKey, source);
					} catch (FrameworkException fex) {

						logger.log(Level.WARNING, "Unable to set remote node property {0} on type {1}", new Object[] { targetKey.name(),
							targetType });
					}

				}

			}

		}

		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if (value != null) {

			Object param = value.get();

			if ((param != null) && (param instanceof ParameterHolder)) {

				ParameterHolder holder   = (ParameterHolder) param;
				PropertyKey targetKey    = holder.getTargetKey();
				Class targetType         = holder.getTargetType();
				AbstractNode relatedNode = getRelatedNode(targetType);

				if (relatedNode != null) {

					return relatedNode.getProperty(targetKey);

				}

			}

		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	private AbstractNode getRelatedNode(Class targetType) {

		AbstractNode relatedNode = null;

		if ((currentObject != null) && (currentObject instanceof AbstractNode)) {

			AbstractNode localNode = (AbstractNode) currentObject;
			RelationClass rel   = EntityContext.getRelationClass(localNode.getClass(), targetType);

			if (rel != null) {

				relatedNode = rel.getRelatedNode(securityContext, localNode);

				if (relatedNode == null) {

					try {
						relatedNode = rel.addRelatedNode(securityContext, localNode);
					} catch (FrameworkException ex) {
						logger.log(Level.WARNING, "Could not add related node", ex);
					}

				}

			}

		}

		return relatedNode;
	}

	//~--- inner classes --------------------------------------------------

	public static class ParameterHolder {

		private PropertyKey targetKey = null;
		private Class targetType      = null;

		//~--- constructors -------------------------------------------

		public ParameterHolder(PropertyKey targetKey, Class targetType) {

			this.targetKey  = targetKey;
			this.targetType = targetType;
		}

		//~--- get methods --------------------------------------------

		public PropertyKey getTargetKey() {
			return targetKey;
		}

		public Class getTargetType() {
			return targetType;
		}
	}
}
