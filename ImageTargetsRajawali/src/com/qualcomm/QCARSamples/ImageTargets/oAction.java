package com.qualcomm.QCARSamples.ImageTargets;

import java.util.ArrayList;
import android.util.Log;

public class oAction implements Cloneable
{
	private ArrayList<String>	Textures;
	private ArrayList<String>	Objects;
	private ArrayList<String>	Audios;
	private ArrayList<String>	Videos;
	private String				type;
	private int					currenttexture	= 0;

	public oAction()
	{
		Textures = new ArrayList<String>();
		Objects = new ArrayList<String>();
		Audios = new ArrayList<String>();
		Videos = new ArrayList<String>();
	}

	public String getTexture()
	{
		if (currenttexture < Textures.size()) currenttexture++;
		return Textures.get((currenttexture - 1));
	}

	public void setType(String t)
	{
		type = t;
	}

	public String getType()
	{
		return type;
	}

	public void addTexture(String s)
	{
		Textures.add(s);
	}

	public void addObject(String s)
	{
		Objects.add(s);
	}

	public void addAudio(String s)
	{
		Audios.add(s);
	}

	public void addVideo(String s)
	{
		Videos.add(s);
	}

	public ArrayList<String> getObjects()
	{
		return Objects;
	}

	public ArrayList<String> getAudios()
	{
		return Audios;
	}

	public ArrayList<String> getVideos()
	{
		return Videos;
	}
}
