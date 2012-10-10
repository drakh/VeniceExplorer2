package com.qualcomm.QCARSamples.ImageTargets;

import java.util.ArrayList;

public class ProjectLevel
{
	public String					projectName;
	public String					description;
	public String					loadingText	= "";
	public ArrayList<ProjectObject>	objs;
	public ArrayList<String>		Textures;
	public ArrayList<String>		ActionNames;
	public ArrayList<ObjectAction>	Actions;
	public String					htmlFile	= "";
	private int						ac			= 0;

	ProjectLevel(String pn)
	{
		projectName = pn;
		objs = new ArrayList<ProjectObject>();
		Textures = new ArrayList<String>();
		ActionNames = new ArrayList<String>();
		Actions = new ArrayList<ObjectAction>();
	}

	public void addAction(String n, ObjectAction a)
	{
		ActionNames.add(ac, n);
		Actions.add(ac, a);
		ac++;
	}

	public ObjectAction getActionByName(String n)
	{
		int ai = ActionNames.indexOf(n);
		if (ai != -1)
		{
			return Actions.get(ai);
		}
		else return null;
	}

	public void setHtml(String n)
	{
		htmlFile = n;
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
	public void setDesc(String s)
	{
		description=s;
	}
	public String getDesc()
	{
		return description;
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
