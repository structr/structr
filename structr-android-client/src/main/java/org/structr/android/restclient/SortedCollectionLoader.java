/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.android.restclient;

import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous connector that fetches a sorted list of entities from a structr REST
 * server. The execute method takes at least one parameter, the entity type. Optional
 * parameters are: a String that specifies the sort key, a boolean that indicates
 * whether the results should be sorted ascending (true) or descending (false), and
 * zero or more {@see SortedCollectionLoader.Param} instances that specify filter
 * and/or paging parameters for the REST method call.
 * 
 * <p>The following example shows how you can use this class in your activity.</p>
 * <pre>
 * new SortedCollectionLoader(new CollectionHandler() {
 *
 *	public void handleProgress(Progress... progress) {
 *		// handle progress / exception
 *	}
 *
 *	public void handleResults(List&lt;? extends StructrObject&gt; results) {
 *		// handle results
 *	}
 * 
 * }).execute(Example.class, sortyKey, sortAscendingDescending, "/examples);
 * </pre>
 *
 * @author Christian Morgner
 */
public class SortedCollectionLoader extends StructrConnector<List<? extends StructrObject>> {

	private CollectionHandler updater = null;

	public SortedCollectionLoader(CollectionHandler updater) {
		this.updater = updater;
	}

	@Override
	protected List<? extends StructrObject> doInBackground(Object... parameters) {

		List<Param> params = new ArrayList<Param>();
		String sortKey = "id";
		Boolean asc = true;
		Class type = null;
		
		try {
			for(Object obj : parameters) {
				if(obj instanceof Class) {
					type = (Class)obj;
				} else if(obj instanceof String) {
					sortKey = (String)obj;
				} else if(obj instanceof Boolean) {
					asc = (Boolean)obj;
				} else if(obj instanceof Param) {
					params.add((Param)obj);
				}
			}

			return StructrObject.dbList(type, sortKey, asc, params.toArray());

		} catch(Throwable t) {
			publishProgress(new Progress(t));
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Progress... progress) {
		updater.handleProgress(progress);
	}

	@Override
	protected void onPostExecute(List<? extends StructrObject> list) {
		updater.handleResults(list);
	}

	public static class Param {
		
		String name = null;
		Object value = null;
		
		public Param(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String toString() {
			
			StringBuilder buf = new StringBuilder("&");
			buf.append(name);
			buf.append("=");
			buf.append(value);
			
			return buf.toString();
		}
	}
}
