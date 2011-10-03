/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.resource.PathException;

/**
 *
 * @author Christian Morgner
 */
public class SortConstraint extends ResourceConstraint {

	private static final Logger logger = Logger.getLogger(SortConstraint.class.getName());

	private String sortOrder = null;
	private String sortKey = null;
	
	public SortConstraint(String sortKey, String sortOrder) {
		this.sortOrder = sortOrder;
		this.sortKey = sortKey;
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;
	}
	
	@Override
	public Result processParentResult(Result result, HttpServletRequest request) throws PathException {

		Comparator<GraphObject> comparator = null;

		try {
			if("desc".equals(sortOrder)) {

				comparator = new Comparator<GraphObject>() {
					@Override
					public int compare(GraphObject n1, GraphObject n2) {
						Comparable c1 = (Comparable)n1.getProperty(sortKey);
						Comparable c2 = (Comparable)n2.getProperty(sortKey);
						return(c2.compareTo(c1));
					}
				};

			} else {

				comparator = new Comparator<GraphObject>() {
					@Override
					public int compare(GraphObject n1, GraphObject n2) {
						Comparable c1 = (Comparable)n1.getProperty(sortKey);
						Comparable c2 = (Comparable)n2.getProperty(sortKey);
						return(c1.compareTo(c2));
					}
				};
			}

			if(comparator != null) {
				Collections.sort(result.getResults(), comparator);
			}

		} catch(Throwable t) {

			logger.log(Level.WARNING, "Error while sorting result set with {0}", sortKey);
		}
		
		return result;
	}
}
