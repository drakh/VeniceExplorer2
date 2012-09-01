package com.qualcomm.QCARSamples.ImageTargets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.Matrix;

import android.os.Environment;
import rajawali.renderer.RajawaliRenderer;
import rajawali.util.RajLog;
import android.content.Context;
import rajawali.BaseObject3D;
import rajawali.lights.PointLight;
import rajawali.parser.ObjParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import rajawali.math.Number3D;
import rajawali.materials.*;
import rajawali.primitives.*;
import java.util.ArrayList;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.AudioManager;
import android.graphics.SurfaceTexture;
import rajawali.bounds.*;
import rajawali.math.Quaternion;

/** The renderer class for the ImageTargets sample. */
public class ObjectsRenderer extends RajawaliRenderer
{

	private ObjParser				mParser;
	private TextureManager			mTextureManager;
	private ArrayList<ProjectLevel>	ps;
	private MarkerPlane c3;
	private float[] mm1=new float[16];//marker 1 matrix
	private float[] mm2=new float[16];//marker 2 matrix
	private float[] mm3=new float[16];//marker 3 matrix
	private float[] mm4=new float[16];//marker 4 matrix
	private float[] mm=new float[16];//current tracking marker
	protected float[] mInvMatrix = new float[16];//inverse matrix of pose
	protected float[] mCamMatrix=new float[16];//camera matrix
	protected Quaternion q=new Quaternion();
	public ObjectsRenderer(Context context)
	{
		super(context);
		RajLog.enableDebug(false);
		/* setup markers position*/
		MarkerPlane c=new MarkerPlane(1,1,1,1);
		c.setRotX(180);
		c.setRotY(180);
		c.setPosition(new Number3D(-5.256f, 2.701f, 5.386f));
		mm2=c.getModelViewMatrix();
		c=null;
		
		c=new MarkerPlane(1,1,1,1);
		c.setRotX(180);
		c.setRotY(180);
		c.setPosition(new Number3D(4.874f, 2.721f, 5.386f));
		mm1=c.getModelViewMatrix();
		c=null;
		
		c= new MarkerPlane(1,1,1,1);
		c.setPosition(new Number3D(-2.224,2.651,5.384));
		c.setRotX(180);
		c.setRotY(180);
		mm4=c.getModelViewMatrix();
		c=null;
		
		c=new MarkerPlane(1,1,1,1);
		c.setPosition(new Number3D(-3.354f, 2.711f, 8.716));
		c.setRotX(180);
		c.setRotY(180);
		mm3=c.getModelViewMatrix();
		c=null;
		System.gc();
	}

	protected void initScene()
	{
		mTextureManager = new TextureManager();
		mCamera.setFarPlane(50f);
		mCamera.setNearPlane(0.1f);
		mCamera.setY(1.4f);
		LoadObjects(ps.get(0));
		System.gc();
	}

	public void setPosition(String n, float[] CM)
	{
		if(n.contentEquals("marker1"))
	    {
	    	mm=mm1;
	    }
	    else if(n.contentEquals("marker2"))
	    {
	    	mm=mm2;
	    }
	    else if(n.contentEquals("marker3"))
	    {
	    	mm=mm3;
	    }
	    else if(n.contentEquals("marker4"))
	    {
	    	mm=mm4;
	    }
		Matrix.invertM(mInvMatrix, 0, mm, 0);
		Matrix.multiplyMM(mCamMatrix, 0, mInvMatrix, 0, CM, 0);
		Number3D a_x=new Number3D(mCamMatrix[0],mCamMatrix[1],mCamMatrix[2]);
		Number3D a_y=new Number3D(mCamMatrix[4],mCamMatrix[5],mCamMatrix[6]);
		Number3D a_z=new Number3D(mCamMatrix[8],mCamMatrix[9],mCamMatrix[10]);
		q.fromAxes(a_x,a_y,a_z);
		float[] rm=new float[16];
		q.toRotationMatrix(rm);
		float pitch=Math.round(Math.toDegrees(q.getPitch(false)));
		float yaw=Math.round(Math.toDegrees(q.getYaw(false)));
		float roll=Math.round(Math.toDegrees(q.getRoll(false)));
		if(n.contentEquals("marker4"))
		{
			mCamera.setRotX(180+pitch);
			mCamera.setRotY(-1*yaw);
			mCamera.setRotZ(0);
			mCamera.setPosition(new Number3D(mCamMatrix[12],1.4f,-1*mCamMatrix[14]));
		}
		else if(n.contentEquals("marker2") || n.contentEquals("marker1"))
		{
			mCamera.setRotX(180-pitch);
			mCamera.setRotY(-1*yaw+180);
			mCamera.setRotZ(0);
			mCamera.setPosition(new Number3D(-1*mCamMatrix[12],1.4f,mCamMatrix[14]));
		}
		else if(n.contentEquals("marker3"))
		{
			mCamera.setRotX(0);
			mCamera.setRotY(-1*yaw-90);
			mCamera.setRotZ(180+pitch);
			mCamera.setPosition(new Number3D(mCamMatrix[14],1.4f,mCamMatrix[12]));
		}
		System.gc();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onDrawFrame(GL10 glUnused)
	{
		// mTexture.updateTexImage();
		super.onDrawFrame(glUnused);
		// Log.d("main","rendering");
	}

	public void setObjs(ArrayList<ProjectLevel> p)
	{
		this.ps = p;
	}
	public void LoadObjects(ProjectLevel p)
	{
		for (int i = 0; i < p.getModels().size(); i++)
		{
			mParser = new ObjParser(this, p.getModels().get(i).getModel());
			mParser.parse();
			BaseObject3D obj = mParser.getParsedObject();
			obj.setDepthMaskEnabled(true);
			obj.setDepthTestEnabled(true);
			obj.setBlendingEnabled(true);
			obj.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			if (p.getModels().get(i).isDoubleSided())
			{
				//obj.setDoubleSided(true);
			}
			if (p.getModels().get(i).isVideo())
			{
				// Log.d("isvideo", "yeees");
				// obj.setMaterial(vmaterial);
				// obj.addTexture(vt);
			}
			else
			{
				obj.setMaterial(new SimpleMaterial());
				String tn = p.getModels().get(i).getTexture();
				//if (!textureNames.contains(tn))
				//{
					//textureNames.add(tn);// store texture names for unique
											// textures
					//int idx = textureNames.indexOf(tn);// get index

					Bitmap mBM = BitmapFactory.decodeFile(Environment
							.getExternalStorageDirectory() + "/" + tn);
					TextureInfo ti = mTextureManager.addTexture(mBM);
					//textureInfos.add(idx, ti);// store texture info with same
												// index as texture name
					obj.addTexture(ti);
				//}
				//else
				//{
					//int idx = textureNames.indexOf(tn);
					//obj.addTexture(textureInfos.get(idx));
				//}
			}
			addChild(obj);
			/*
			BoundingBox bb = obj.getGeometry().getBoundingBox();
			Number3D mi = bb.getMin();
			Number3D mx = bb.getMax();
			Number3D cnt = new Number3D((mi.x + mx.x) / 2, (mi.y + mx.y) / 2,
					(mi.z + mx.z) / 2);
			p.getModels().get(i).setCenter(cnt);
			p.getModels().get(i).setObj(obj);
			*/
		}
		System.gc();
	}
}