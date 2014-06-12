package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */
public class ExportContext {

	private CloudListener listener = null;
	private int currentProgress    = 0;
	private int totalSize          = 0;

	public ExportContext(final CloudListener listener, final int totalSize) {
		this.listener  = listener;
		this.totalSize = totalSize;
	}

	public CloudListener getListener() {
		return listener;
	}

	public int getCurrentProgress() {
		return currentProgress;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public void progress() {
		currentProgress++;

		if (listener != null) {
			listener.transmissionProgress(currentProgress, totalSize);
		}
	}

	public void increaseTotal(final int addTotal) {
		totalSize += addTotal;
	}

	public void transmissionStarted() {

		if (listener != null) {
			listener.transmissionStarted();
		}
	}

	public void transmissionFinished() {

		if (listener != null) {
			listener.transmissionFinished();
		}
	}

	public void transmissionAborted() {

		if (listener != null) {
			listener.transmissionAborted();
		}
	}
}
