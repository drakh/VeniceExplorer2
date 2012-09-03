package com.qualcomm.QCARSamples.ImageTargets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.Matrix;
import android.view.View;
import android.widget.ImageView;

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
import android.os.Handler;
import android.os.Message;

/** The renderer class for the ImageTargets sample. */
public class ObjectsRenderer extends RajawaliRenderer implements
OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener,
OnErrorListener
{
	private Handler mLoad;
	public boolean					izLoaded	= true; 
	public boolean					doLoad		= true;
	public boolean doReset=false;
	public int						curProj		= 0;	
	private MediaPlayer				mMediaPlayer;
	private SurfaceTexture			mTexture;
	private String dirn;
	private ArrayList<String>		textureNames;
	private ArrayList<TextureInfo>	textureInfos;
	
	private boolean isTracking=false;
	private long lastTrackTime=0;
	private ObjParser				mParser;
	private TextureManager			mTextureManager;
	private TextureManager mVideoTextureM;
	private ArrayList<ProjectLevel>	ps;

	private float[] mm1=new float[16];//marker 1 matrix
	private float[] mm2=new float[16];//marker 2 matrix
	private float[] mm3=new float[16];//marker 3 matrix
	private float[] mm4=new float[16];//marker 4 matrix
	private float[] mm=new float[16];//current tracking marker
	
	protected float[] mInvMatrix = new float[16];//inverse matrix of pose
	protected float[] mCamMatrix=new float[16];//camera matrix
	protected Quaternion q=new Quaternion();
	
	public Number3D camPos=new Number3D(0f,1.4f,0f);
	
	public float phi=0f;
	public float theta=90f;
	private Context ctx;
	public ObjectsRenderer(Context context, String d/*, Handler h*/)
	{
		super(context);
		ctx=context;
		dirn=d;
		textureNames = new ArrayList<String>();
		textureInfos = new ArrayList<TextureInfo>();
		//mLoad=h;
		RajLog.enableDebug(true);
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
		//System.gc();
	}

	protected void initScene()
	{
		mVideoTextureM=new TextureManager();		
		mTextureManager = new TextureManager();
		mCamera.setFarPlane(50f);
		mCamera.setNearPlane(0.1f);
		
		/* camera init*/
		Number3D la=SphericalToCartesian(phi,theta,1);
		mCamera.setPosition(camPos);
		mCamera.setLookAt(new Number3D((camPos.x+la.x), (camPos.y+la.y), (camPos.z+la.z)));
		System.gc();
	}
	public void clearScene()
	{
		clearChildren();
		super.mNumChildren=0;
		mTextureManager.reset();
		textureNames.clear();
		textureInfos.clear();
		System.gc();
	}
	public void setPosition(String n, float[] CM)
	{
		isTracking=true;
		
		lastTrackTime=System.currentTimeMillis();
		
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
			phi=-1*yaw;
			theta=180+pitch;
			camPos.x=mCamMatrix[12];
			camPos.z=-1*mCamMatrix[14];
		}
		else if(n.contentEquals("marker2") || n.contentEquals("marker1"))
		{	
			phi=-1*yaw+180;
			theta=180-pitch;
			
			camPos.x=-1*mCamMatrix[12];
			camPos.z=mCamMatrix[14];
		}
		else if(n.contentEquals("marker3"))
		{
			phi=-1*yaw-90;
			theta=180+pitch;
			camPos.x=mCamMatrix[14];
			camPos.z=mCamMatrix[12];
		}
		
		theta=theta+90f;
		Number3D la=SphericalToCartesian(phi,theta,1);
		mCamera.setPosition(camPos);
		mCamera.setLookAt(new Number3D((camPos.x+la.x), (camPos.y+la.y), (camPos.z+la.z)));
	}
	
	public Number3D SphericalToCartesian(float phi, float theta, float r)
	{
		Number3D coords = new Number3D();
		float p = (float) Math.toRadians(phi);
		float t = (float) Math.toRadians(theta);
		float sinPhi = (float) (Math.round(Math.sin(p) * 1000)) / 1000;
		float cosPhi = (float) (Math.round(Math.cos(p) * 1000)) / 1000;
		float sinTheta = (float) (Math.round(Math.sin(t) * 1000)) / 1000;
		float cosTheta = (float) (Math.round(Math.cos(t) * 1000)) / 1000;
		float ay = r * cosTheta;
		float ax = r * sinPhi * sinTheta;
		float az = r * cosPhi * sinTheta;
		coords.x = ax;
		coords.y = ay;
		coords.z = az;
		return coords;
	}
	
	public Number3D CylindricalToCartesian(float phi, float r, float h)
	{
		Number3D coords = new Number3D();
		float p = (float) Math.toRadians(phi);
		float sinPhi = (float) (Math.round(Math.sin(p) * 1000)) / 1000;
		float cosPhi = (float) (Math.round(Math.cos(p) * 1000)) / 1000;
		float ax = r * sinPhi;
		float az = r * cosPhi;
		coords.x = ax;
		coords.y = h;
		coords.z = az;
		return coords;
	}
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		Log.d("created surface","created");
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onDrawFrame(GL10 glUnused)
	{
		// mTexture.updateTexImage();
		if(doReset==true)
		{
			resetScene();
			System.gc();
			doReset=false;
		}
		if (izLoaded == false)
		{
			Log.d("render","load scene");
			clearScene();
			Message m=new Message();
			((ImageTargets)ctx).mLoadingHandler.sendMessage(m);
			loadScene();
			izLoaded=true;
			System.gc();
		}
		super.onDrawFrame(glUnused);
		
		//iterate thru objects for interactivity
		/*
		if(doLoad==true)
		{
			
		}
		*/
	}
	public void resetRenderer()
	{
		doReset=true;
	}
	public void resetScene()
	{
		Log.d("render","reset scene start");
		clearScene();
		curProj=0;
		izLoaded=true;
		doLoad=true;
		Log.d("render","reset scene end");
	}
	protected void loadScene()
	{
		LoadObjects(ps.get(curProj));
	}
	
	public void setObjs(ArrayList<ProjectLevel> p)
	{
		this.ps = p;
	}
	public void LoadTextures(ProjectLevel p)
	{
		Log.d("textures size","s: "+p.Textures.size());
		for(int i=0;i<p.Textures.size();i++)
		{
			String ttn=Environment.getExternalStorageDirectory() + "/"+dirn+"/"+ p.Textures.get(i);
			Bitmap mBM = BitmapFactory.decodeFile(ttn);
			Log.d("textures", ttn);
			textureInfos.add(i, mTextureManager.addTexture(mBM));
		}
	}
	public void LoadObjects(ProjectLevel p)
	{
		
		LoadTextures(p);
		
		for (int i = 0; i < p.getModels().size(); i++)
		{
			String onn=dirn+"/"+p.getModels().get(i).getModel();
			Log.d("model: ",onn);
			mParser = new ObjParser(this, onn);
			mParser.parse();
			BaseObject3D obj = mParser.getParsedObject();
			obj.setMaterial(new SimpleMaterial());
			obj.setDepthMaskEnabled(true);
			obj.setDepthTestEnabled(true);
			obj.setBlendingEnabled(true);
			obj.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			if (p.getModels().get(i).isDoubleSided())
			{
				obj.setDoubleSided(true);
			}
			int ti=p.Textures.indexOf(p.getModels().get(i).getTexture());
			Log.d("texture inf",p.getModels().get(i).getTexture()+"|"+ti);
			obj.addTexture(textureInfos.get(ti));
			
			BoundingBox bb = obj.getGeometry().getBoundingBox();
			Number3D mi = bb.getMin();
			Number3D mx = bb.getMax();
			Number3D cnt = new Number3D((mi.x + mx.x) / 2, (mi.y + mx.y) / 2, (mi.z + mx.z) / 2);
			addChild(obj);
			p.getModels().get(i).setCenter(cnt);
			p.getModels().get(i).setObj(obj);
			//if (p.getModels().get(i).isVideo())
			//{
				// Log.d("isvideo", "yeees");
				// obj.setMaterial(vmaterial);
				// obj.addTexture(vt);
			//}
		}
		System.gc();
	}
	public void onBufferingUpdate(MediaPlayer arg0, int arg1)
	{
	}
	public void onPrepared(MediaPlayer mediaplayer)
	{
		//mMediaPlayer.start();
	}
	public void onCompletion(MediaPlayer arg0)
	{
	}
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		return false;
	}
	public void onSurfaceDestroyed()
	{
		//mMediaPlayer.release();
		//super.onSurfaceDestroyed();
	}
	public void showProject(int k)
	{
		if (doLoad == true || curProj != k)
		{
			curProj = k;
			izLoaded = false;
			doLoad = false;
		}
		/*
		 * mMediaPlayer.stop(); hideModels(); ProjectLevel p = ps.get(k); for
		 * (int i = 0; i < p.getModels().size(); i++) {
		 * p.getModels().get(i).obj.setVisible(true); if
		 * (p.getModels().get(i).isVideo()) { try {
		 * 
		 * mMediaPlayer.setDataSource(Environment .getExternalStorageDirectory()
		 * + "/" + p.getModels().get(i).getTexture());
		 * mMediaPlayer.prepareAsync(); Log.d("video", "loading"); } catch
		 * (IOException e) { Log.d("video", "not loaded"); } } }
		 */
	}
}