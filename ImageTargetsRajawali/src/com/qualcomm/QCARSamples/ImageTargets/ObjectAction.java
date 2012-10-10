package com.qualcomm.QCARSamples.ImageTargets;

import rajawali.materials.SimpleMaterial;
import rajawali.math.Plane;
import rajawali.math.Number3D;
import java.util.ArrayList;
import rajawali.BaseObject3D;
import android.util.Log;
import rajawali.Geometry3D;
import java.nio.FloatBuffer;

public class ObjectAction implements Cloneable
{
	private String				typ				= "";
	private float				radius			= 0;
	private String				current_model	= "";
	private String				videofile		= "";
	private String				videotext		= "";
	private Number3D			model_center;
	public oAction				onEnter;
	public oAction				onLeave;
	public oAction				onCross;
	private ArrayList<String>	axes;
	private Plane				checkPlane;
	private Plane.PlaneSide		side;
	private boolean				isinside		= false;

	public ObjectAction()
	{
		axes = new ArrayList<String>();
	}

	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			// This should never happen
			throw new InternalError(e.toString());
		}
	}

	public void setVideo(String v)
	{
		videofile = v;
	}

	public void setVideoText(String v)
	{
		videotext = v;
	}

	public void setCenter(Number3D c, Number3D cam)
	{
		model_center = c;
		float dx = (-1 * cam.x) - model_center.x;
		float dz = cam.z - model_center.z;
		// initial position inside or outside?
		if (typ.contentEquals("c"))
		{
			isinside = isInCircle(dx, dz);
		}
		else if (typ.contentEquals("s"))
		{
			isinside = isInSquare(dx, dz);
		}
	}

	public boolean isInSquare(float dx, float dz)
	{
		if (Math.abs(dx) <= radius && Math.abs(dz) <= radius)
		{
			return true;
		}
		else return false;
	}

	public boolean isInCircle(float dx, float dz)
	{
		float dist = (float) Math.sqrt(dx * dx + dz * dz);
		if (dist > radius)
		{
			return false;
		}
		else return true;
	}

	public void setModelName(String n)
	{
		current_model = n;
	}

	public void setType(String t)
	{
		typ = t;
	}

	public String getType()
	{
		return typ;
	}

	public void setPlane(Geometry3D geometry, Number3D cam_pos)
	{
		FloatBuffer vertices = geometry.getVertices();
		vertices.rewind();
		Number3D vertex1 = new Number3D();
		Number3D vertex2 = new Number3D();
		Number3D vertex3 = new Number3D();
		int c = 0;
		while (vertices.hasRemaining())
		{
			if (c == 0)
			{
				vertex1.x = vertices.get();
				vertex1.y = vertices.get();
				vertex1.z = vertices.get();
				// Log.d("vertex",
				// "v1: "+vertex1.x+"|"+vertex1.y+"|"+vertex1.z);
			}
			if (c == 1)
			{
				vertex2.x = vertices.get();
				vertex2.y = vertices.get();
				vertex2.z = vertices.get();
				// Log.d("vertex",
				// "v2: "+vertex2.x+"|"+vertex2.y+"|"+vertex2.z);
			}
			if (c == 2)
			{
				vertex3.x = vertices.get();
				vertex3.y = vertices.get();
				vertex3.z = vertices.get();
				// Log.d("vertex",
				// "v3: "+vertex3.x+"|"+vertex3.y+"|"+vertex3.z);
			}
			c++;
			if (c > 2) break;
		}
		checkPlane = new Plane(vertex1, vertex2, vertex3);
		side = checkPlane.getPointSide(cam_pos);
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

	public void addAxes(String a)
	{
		axes.add(a);
	}

	public void checkAction(Number3D cam, ObjectsRenderer r)// camera position,
															// renderer
	{
		float dx = (-1 * cam.x) - model_center.x;
		float dz = cam.z - model_center.z;
		if (typ.contentEquals("b"))
		{
			BaseObject3D obj = r.getModelByName(current_model);
			if (obj != null)
			{
				for (int i = 0; i < axes.size(); i++)
				{
					String a = axes.get(i);
					if (a.contentEquals("x"))
					{
						obj.setX(cam.x);
					}
					else if (a.contentEquals("y"))
					{
						obj.setZ(cam.z);
					}
				}
			}
		}
		else if (typ.contentEquals("c"))
		{
			boolean curinside = isInCircle(dx, dz);
			if (curinside == true && isinside == false && onEnter != null)
			{
				runAction(onEnter, r);
			}
			if (curinside == false && isinside == true && onLeave != null)
			{
				runAction(onLeave, r);
			}
			isinside = curinside;
		}
		else if (typ.contentEquals("s"))
		{
			boolean curinside = isInSquare(dx, dz);
			if (curinside == true && isinside == false && onEnter != null)
			{
				runAction(onEnter, r);
			}
			if (curinside == false && isinside == true && onLeave != null)
			{
				runAction(onLeave, r);
			}
			isinside = curinside;
		}
		else if (typ.contains("p"))
		{

			Number3D cp = new Number3D(-1 * cam.x, cam.y, cam.z);
			Plane.PlaneSide curside = checkPlane.getPointSide(cp);
			if (curside != side && onCross != null)
			{
				runAction(onCross, r);
			}
			side = curside;
		}
	}

	public void runAction(oAction a, ObjectsRenderer r)
	{
		String t = a.getType();
		if (t != null)
		{
			Log.d("actionrun", t);
			if (t.contentEquals("changetexture"))
			{
				changeTexture(a, r);
			}
			else if (t.contentEquals("showmodels"))
			{
				showModels(a, r);
			}
			else if (t.contentEquals("hidemodels"))
			{
				hideModels(a, r);
			}
			else if (t.contentEquals("playaudio"))
			{
				playAudio(a, r);
			}
			else if (t.contentEquals("stopaudio"))
			{
				stopAudio(a, r);
			}
			else if (t.contentEquals("startvideo"))
			{
				startVideo(a, r);
			}
			else if (t.contentEquals("stopvideo"))
			{
				stopVideo(a, r);
			}
		}
	}

	public void showModels(oAction a, ObjectsRenderer r)
	{
		Log.d("run", "show models");
		ArrayList<String> models = a.getObjects();
		for (int i = 0; i < models.size(); i++)
		{
			Log.d("model",models.get(i));
			BaseObject3D obj = r.getModelByName(models.get(i));
			obj.setVisible(true);
		}
	}

	public void hideModels(oAction a, ObjectsRenderer r)
	{
		ArrayList<String> models = a.getObjects();
		for (int i = 0; i < models.size(); i++)
		{
			BaseObject3D obj = r.getModelByName(models.get(i));
			obj.setVisible(false);
		}
	}

	public void changeTexture(oAction a, ObjectsRenderer r)
	{
		String textureName = a.getTexture();
		BaseObject3D obj = r.getModelByName(current_model);
		obj.setMaterial(new SimpleMaterial(), false);
		obj.addTexture(r.getTextureByName(textureName));
	}

	public void startVideo(oAction a, ObjectsRenderer r)
	{
		Log.d("run", "videostart");
		if (!videofile.contentEquals(""))
		{
			Log.d("run", "do start video");
			BaseObject3D obj = r.getModelByName(current_model);
			r.startVideo(obj, videofile, videotext);
		}
	}

	public void stopVideo(oAction a, ObjectsRenderer r)
	{
		r.stopVideo();
	}

	public void playAudio(oAction a, ObjectsRenderer r)
	{
		ArrayList<String> audios = a.getAudios();
		if (audios.size() > 0)
		{
			r.playAudio(audios.get(0));
		}
	}

	public void stopAudio(oAction a, ObjectsRenderer r)
	{
		r.stopAudio();
	}
}
