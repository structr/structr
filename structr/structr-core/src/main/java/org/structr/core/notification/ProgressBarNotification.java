/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

package org.structr.core.notification;

import java.text.DecimalFormat;

/**
 * A notification that displays a progress bar in the admin UI. Notifications
 * of this type have two integer values that together specify the displayed
 * progress. The <code>targetProgress</code> value defines the completion
 * threshold, and the <code>currentProgress</code> value specifies the current
 * value. There are several methods that can be used to increase the current
 * progress value. This notification stays visible until 3 seconds after the
 * <code>currentProgress</code> value passes the <code>targetProgress</code>
 * threshold. The delay can be specified via the {@see #setLifespan}
 * method.
 *
 * @author Christian Morgner
 */
public class ProgressBarNotification extends DefaultNotification {

	private DecimalFormat format = new DecimalFormat("0");
	private boolean showAbsoluteValues = false;
	private boolean finished = false;
	private int currentProgress = 0;
	private int targetProgress = 0;
	
	private String errorMessage = null;

	/**
	 * Constructs a new instance of this notification with the given title.
	 *
	 * @param title the title to display
	 */
	public ProgressBarNotification(String title)
	{
		this(title, 0, false);
	}

	/**
	 * Constructs a new instance of this notification with the given title
	 * and the given targetProgress counter.
	 *
	 * @param title the title to display
	 * @param targetProgress the target progress threshold
	 */
	public ProgressBarNotification(String title, int targetProgress)
	{
		this(title, targetProgress, false);
	}

	/**
	 * Constructs a new instance of this notification with the given title
	 * and the given targetProgress. The <code>showAbsoluteValues</code>
	 * flag indicates whether this progress bar displays absolute values,
	 * (e.g. "55/100"), or a percentage (e.g. "55 %").
	 *
	 * @param title the title to display
	 * @param targetProgress the target progress threshold
	 * @param showAbsoluteValues whether to show absolute values or percentage
	 */
	public ProgressBarNotification(String title, int targetProgress, boolean showAbsoluteValues)
	{
		super(title, "");

		this.targetProgress = targetProgress;
		this.creationTime = 0L;
		this.showAbsoluteValues = showAbsoluteValues;
	}

	/**
	 * Increases the current progress value by 1.
	 */
	public void increaseProgress()
	{
		this.currentProgress += 1;
		checkFinished();
	}

	/**
	 * Increases the current progress value by <code>progress</code>.
	 *
	 * @param progress the progress to add
	 */
	public void increaseProgress(int progress)
	{
		this.currentProgress += progress;
		checkFinished();
	}

	/**
	 * Sets the current progress value of this progress bar to the given
	 * value.
	 *
	 * @param progress the current progress value to set
	 */
	public void setProgress(int progress)
	{
		this.currentProgress = progress;
		checkFinished();
	}

	/**
	 * Returns the progress percentage of this progress bar.
	 *
	 * @return the progress percentage
	 */
	public double getProgress()
	{
		double dCurrent = (double)currentProgress;
		double dTarget = (double)targetProgress;
		double ret = 0.0;

		ret = (dCurrent / dTarget) * 100.0;

		return(ret);
	}

	/**
	 * Sets the target progress threshold for 100%.
	 *
	 * @param targetProgress the target progress threshold
	 */
	public void setTargetProgress(int targetProgress)
	{
		this.targetProgress = targetProgress;
	}
	
	/**
	 * Sets the status of this notification to "cancelled".
	 */
	public void setErrorMessage(String errorMessage) {
		
		this.errorMessage = errorMessage;
		
		creationTime = System.currentTimeMillis();
		finished = true;
	}

	@Override
	public String getText()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<div class='progressBarNotification'>");
		ret.append("<div class='progressBar' style='width:").append(getProgress()).append("%;'>&nbsp;</div>");
		ret.append("<div class='progressBarText'>");

		if(errorMessage != null) {
			
			ret.append(errorMessage);
			
		} else {
			
			if(showAbsoluteValues) {

				ret.append(currentProgress);
				ret.append(" / ");
				ret.append(targetProgress);

			} else {

				ret.append(format.format(getProgress()));
				ret.append("&nbsp;%");
			}
		}
		
		ret.append("</div>");
		ret.append("</div>");

		return(ret.toString());
	}

	@Override
	public boolean isExpired()
	{
		checkFinished();

		if(finished)
		{
			return(System.currentTimeMillis() > this.creationTime + lifespan);
		}

		return(false);
	}

	@Override
	public String toString() {
		
		return("ProgressBarNotification");
	}

	// ----- private methods -----
	private void checkFinished() {

		if(targetProgress > 0 && currentProgress >= targetProgress)
		{
			if(creationTime == 0) {
				creationTime = System.currentTimeMillis();
			}
			
			finished = true;
		}
	}
}
