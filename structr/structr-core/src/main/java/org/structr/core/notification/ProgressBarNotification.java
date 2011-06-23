package org.structr.core.notification;

import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 *
 * @author Christian Morgner
 */
public class ProgressBarNotification extends DefaultNotification {

	private static final Logger logger=  Logger.getLogger(ProgressBarNotification.class.getName());

	private DecimalFormat format = new DecimalFormat("0");
	private boolean showAbsoluteValues = false;
	private boolean finished = false;
	private int currentProgress = 0;
	private int targetProgress = 0;

	public ProgressBarNotification(String title)
	{
		this(title, 0, false);
	}

	public ProgressBarNotification(String title, int targetProgress)
	{
		this(title, targetProgress, false);
	}

	public ProgressBarNotification(String title, int targetProgress, boolean showAbsoluteValues)
	{
		super(title, "");

		this.targetProgress = targetProgress;
		this.creationTime = 0L;
		this.showAbsoluteValues = showAbsoluteValues;
	}

	public void increaseProgress()
	{
		this.currentProgress += 1;
		checkFinished();
	}

	public void increaseProgress(int progress)
	{
		this.currentProgress += progress;
		checkFinished();
	}

	public void setProgress(int progress)
	{
		this.currentProgress = progress;
		checkFinished();
	}

	public double getProgress()
	{
		double dCurrent = (double)currentProgress;
		double dTarget = (double)targetProgress;
		double ret = 0.0;

		ret = (dCurrent / dTarget) * 100.0;

		return(ret);
	}

	public void setTargetProgress(int targetProgress)
	{
		this.targetProgress = targetProgress;
	}

	@Override
	public String getText()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<div class='progressBarNotification'>");
		ret.append("<div class='progressBar' style='width:").append(getProgress()).append("%;'>&nbsp;</div>");
		ret.append("<div class='progressBarText'>");

		if(showAbsoluteValues) {

			ret.append(currentProgress);
			ret.append(" / ");
			ret.append(targetProgress);

		} else {

			ret.append(format.format(getProgress()));
			ret.append("&nbsp;%");
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
