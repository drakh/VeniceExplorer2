package com.qualcomm.QCARSamples.ImageTargets;

import rajawali.BaseObject3D;
import rajawali.math.*;
import android.util.Log;

public class ProjectObject
{
	private String		modelName		= "";
	private String		modelTexture	= "";
	private String		modelVideo		= "";
	private String		modelVideoText	= "";
	private String		ActionName		= "";
	private int			doublesided		= 0;
	private int			video			= 0;
	private int			hidden			= 0;
	private int			interactive		= 0;
	public BaseObject3D	obj;
	private Number3D	center;
	private float		radius;

	ProjectObject()
	{

	}

	public void setCenter(Number3D c)
	{
		center = c;
	}

	public void setDS(String ds)
	{
		doublesided = Integer.parseInt(ds);
	}

	public void setVideo(String v)
	{
		video = Integer.parseInt(v);
		Log.d("vidijou", "v: " + video);
	}

	public void setTexture(String path)
	{
		modelTexture = path;
	}

	public void setModel(String path)
	{
		modelName = path;
	}

	public void setVideoTexture(String t)
	{
		modelVideo = t;
	}

	public String getVideoTexture()
	{
		return modelVideo;
	}

	public void setVideoText(String s)
	{
		modelVideoText = s;
	}

	public String getVideoText()
	{
		return modelVideoText;
	}

	public void setInteractive(String ia)
	{
		if (ia.equalsIgnoreCase("true"))
		{
			interactive = 1;
		}
	}

	public void setVisible(String v)
	{
		if (Integer.parseInt(v) == 0) hidden = 1;
	}

	public String getModel()
	{
		return modelName;
	}

	public String getTexture()
	{
		return modelTexture;
	}

	public boolean isVisible()
	{
		if (hidden == 0) return true;
		else return false;
	}

	public boolean isDoubleSided()
	{
		boolean r = false;
		if (doublesided == 1) r = true;
		return r;
	}

	public void setObj(BaseObject3D o)
	{
		obj = o;
	}

	public boolean isVideo()
	{
		boolean r = false;
		if (video == 1) r = true;
		return r;
	}

	public int isInteractive()
	{
		return interactive;
	}

	public void SetActionName(String n)
	{
		ActionName = n;
	}

	public String getActionName()
	{
		return ActionName;
	}

}
