package com.qualcomm.QCARSamples.ImageTargets;

import rajawali.math.Plane;
import rajawali.math.Number3D;

public class ObjectAction
{
	private String	typ				= "";
	private float	radius			= 0;
	private String	current_model	= "";
	private oAction	onEnter;
	private oAction	onLeave;
	private oAction	onCross;

	public ObjectAction()
	{
	}

	public void setType(String t)
	{
		typ = t;
	}

	public void setRadius(float r)
	{
		radius = r;
	}

	public void setAction(String w)
	{
		if (w.contentEquals("onenter"))
		{
			onEnter = new oAction();
		}
		else if (w.contentEquals("onleave"))
		{
			onLeave = new oAction();
		}
		else if (w.contentEquals("oncross"))
		{
			onCross = new oAction();
		}
	}
	public void addTexture(String w, String t)
	{
		
	}
}
