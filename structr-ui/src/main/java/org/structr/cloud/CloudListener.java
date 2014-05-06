package org.structr.cloud;

/**
 * Listener interface that enables you to get updated on the
 * progress of CloudService operations.
 *
 * @author Christian Morgner
 */
public interface CloudListener {

	/**
	 * Called when a transmission is started.
	 */
	public void transmissionStarted();

	/**
	 * Called when a transmission is finished, even if
	 * transmissionAborted() was called before.
	 */
	public void transmissionFinished();

	/**
	 * Called when a transmission is aborted.
	 */
	public void transmissionAborted();

	/**
	 * Called on every transmission progress event. Please
	 * note that both the current and the total values can
	 * change (increase) at any time due to the lazy handling
	 * of file chunks.
	 * 
	 * @param current
	 * @param total
	 */
	public void transmissionProgress(int current, int total);
}
