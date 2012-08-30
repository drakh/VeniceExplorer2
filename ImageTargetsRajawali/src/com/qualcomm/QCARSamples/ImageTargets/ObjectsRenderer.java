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
	private MarkerPlane c1;
	private MarkerPlane c2;
	private MarkerPlane c3;
	private MarkerPlane c4;
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
	}

	protected void initScene()
	{
		mTextureManager = new TextureManager();
		mCamera.setFarPlane(50f);
		mCamera.setNearPlane(0.1f);
		mCamera.setFieldOfView(120);
		//mCamera.setY(1.4f);
		//mCamera.setLookAt(0f,1.4f,-5f);

		LoadObjects(ps.get(0));
		
		c4= new MarkerPlane(1,1,1,1);
		c4.setPosition(new Number3D(2.224,2.651,5.384-0.1f));
		c4.setMaterial(new SimpleMaterial());
		mm4=c4.getModelViewMatrix();
		addChild(c4);
		
		c3=new MarkerPlane(1,1,1,1);
		c3.setRotY(-90);
		c3.setPosition(new Number3D(-8.716+0.1f, 2.711f, 3.354f));
		c3.setMaterial(new SimpleMaterial());
		c3.setOrientation();
		mm3=c3.getModelViewMatrix();
		addChild(c3);

		c1=new MarkerPlane(1,1,1,1);
		c1.setRotY(180);
		c1.setPosition(new Number3D(4.874f, 2.721f, -5.386f+0.1f));
		c1.setMaterial(new SimpleMaterial());
		mm1=c1.getModelViewMatrix();
		addChild(c1);
		
		c2=new MarkerPlane(1,1,1,1);
		c2.setRotY(180);
		c2.setPosition(new Number3D(-5.256f, 2.701f, -5.386f+0.1f));
		c2.setMaterial(new SimpleMaterial());
		mm2=c2.getModelViewMatrix();
		addChild(c2);
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
		//mCamera.setOrientation(q);
		mCamera.setRotationMatrix(rm);
		Log.d("Main:","ax"+a_x.x+"|"+a_x.y+"|"+a_x.z);
		Log.d("Main:","ax"+a_y.x+"|"+a_y.y+"|"+a_y.z);
		Log.d("Main:","ax"+a_z.x+"|"+a_z.y+"|"+a_z.z);
		Log.d("Main:","x: "+mCamMatrix[12]);
		Log.d("Main:","y: "+mCamMatrix[13]);
		Log.d("Main:","z: "+mCamMatrix[14]);
		mCamera.setPosition(new Number3D(mCamMatrix[12],mCamMatrix[13],mCamMatrix[14]));
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
		Log.d("objloader", "objects in scene:" + getNumChildren());
	}
}