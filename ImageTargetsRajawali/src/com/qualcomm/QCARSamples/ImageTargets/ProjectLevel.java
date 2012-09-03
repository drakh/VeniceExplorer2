package com.qualcomm.QCARSamples.ImageTargets;

import java.util.ArrayList;

public class ProjectLevel
{
	public String					projectName;
	public ArrayList<ProjectObject>	objs;
	public ArrayList<String>		Textures;
	public String					htmlFile="";

	ProjectLevel(String pn)
	{
		projectName = pn;
		objs = new ArrayList<ProjectObject>();
		Textures = new ArrayList<String>();
	}
	public void setHtml(String n)
	{
		htmlFile=n;
	}
	public void addTexture(String tn)
	{
		if (!Textures.contains(tn))
		{
			Textures.add(tn);
		}
	}

	public void init()
	{

	}

	public String getName()
	{
		return projectName;
	}

	public void addModel(ProjectObject o)
	{
		objs.add(o);
	}

	public ArrayList<ProjectObject> getModels()
	{
		return objs;
	}
}
