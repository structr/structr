package org.structr.core.graph;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Morgner
 */
public class TypeSemaphore {

	private static final Logger logger = Logger.getLogger(TypeSemaphore.class.getName());
	
	private Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<String, Semaphore>();
	
	public void acquire(final Set<String> types) throws InterruptedException {
	
		if (types != null && !types.isEmpty()) {
			
			logger.log(Level.INFO, "Acquiring permit(s) for {0}", types);

			for (Semaphore semaphore : getSemaphores(types)) {
				semaphore.acquire();
			}
		}
	}
	
	public void release(final Set<String> types) {

		if (types != null && !types.isEmpty()) {
			
			logger.log(Level.INFO, "Releasing permit(s) for {0}", types);

			for (Semaphore semaphore : getSemaphores(types)) {
				semaphore.release();
			}
		}
	}
	
	private Set<Semaphore> getSemaphores(final Set<String> types) {
		
		Set<Semaphore> semaphores = new LinkedHashSet<Semaphore>();
		
		if (types != null) {
			
			for (String type : types) {

				if (type != null) {
					
					Semaphore semaphore = semaphoreMap.get(type);
					if (semaphore == null) {

						semaphore = new Semaphore(1, true);
						semaphoreMap.put(type, semaphore);
					}

					semaphores.add(semaphore);
				}
			}
		}
		
		return semaphores;
	}
}
