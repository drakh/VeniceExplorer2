package com.qualcomm.QCARSamples.ImageTargets;

import java.nio.Buffer;
import rajawali.util.BufferUtil;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.Matrix;
import android.view.View;
import android.widget.ImageView;

import android.os.Environment;
import rajawali.renderer.RajawaliRenderer;
import rajawali.util.RajLog;
import android.R.plurals;
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
import java.io.File;
import java.nio.FloatBuffer;
import android.view.SurfaceView;

/** The renderer class for the ImageTargets sample. */
public class ObjectsRenderer extends RajawaliRenderer implements OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener, OnErrorListener
{
	public String					videofile		= "";
	public String					videotext		= "";
	public boolean					isplayvideo		= false;
	private static final float		NS2S			= 1.0f / 1000000000.0f;
	private Handler					mLoad;
	public boolean					izLoaded		= true;
	public boolean					doLoad			= true;
	public boolean					doReset			= false;
	private boolean					doTracking		= true;
	private boolean					actions_enabled	= true;
	public int						curProj			= 0;
	private int						audioid			= 0;
	private MediaPlayer				mMediaPlayer;
	private SurfaceTexture			mVideoTexture;
	private String					dirn;
	private BaseObject3D			tmp_obj;
	private ArrayList<String>		textureNames;
	private ArrayList<TextureInfo>	textureInfos;
	private ArrayList<String>		modelNames;
	private ArrayList<BaseObject3D>	models;
	private ArrayList<ObjectAction>	actions;
	private float					FOV;
	private boolean					isTracking		= false;
	private float					lastTrackTime	= 0;
	private ObjParser				mParser;
	private TextureManager			mTextureManager;
	private TextureManager			mVideoTextureM;
	private ArrayList<ProjectLevel>	ps;
	private VideoMaterial			vmaterial;
	private float[]					temp_texture_coords;
	private TextureInfo				vt;
	private float[]					mm1				= new float[16];
	private float[]					mm2				= new float[16];
	private float[]					mm3				= new float[16];
	private float[]					mm4				= new float[16];
	private float[]					mm				= new float[16];
	protected float[]				mInvMatrix		= new float[16];
	protected float[]				mCamMatrix		= new float[16];
	private float[]					videoMatrix		= new float[16];
	protected Quaternion			q				= new Quaternion();
	private TextureInfo				temp_ti;
	public Number3D					camPos			= new Number3D(0f, 1.4f, 0f);
	public Number3D					stepPos			= new Number3D(0f, 1.4f, 0f);
	public Number3D					markPos			= new Number3D(0f, 1.4f, 0f);

	public float					phi				= 0f;
	public float					theta			= 90f;
	private Context					ctx;

	public ObjectsRenderer(Context context, String d, float fov)
	{
		super(context);
		FOV = fov;
		ctx = context;
		dirn = d;

		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mMediaPlayer.setLooping(false);
		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		mMediaPlayer.setOnErrorListener(this);

		textureNames = new ArrayList<String>();
		textureInfos = new ArrayList<TextureInfo>();
		modelNames = new ArrayList<String>();
		models = new ArrayList<BaseObject3D>();
		actions = new ArrayList<ObjectAction>();
		RajLog.enableDebug(false);
		/* setup markers position */
		MarkerPlane c = new MarkerPlane(1, 1, 1, 1);
		c.setRotX(180);
		c.setRotY(180);
		c.setPosition(new Number3D(-5.256f, 2.701f, 5.386f));
		mm2 = c.getModelViewMatrix();
		c = null;

		c = new MarkerPlane(1, 1, 1, 1);
		c.setRotX(180);
		c.setRotY(180);
		c.setPosition(new Number3D(4.874f, 2.721f, 5.386f));
		mm1 = c.getModelViewMatrix();
		c = null;

		c = new MarkerPlane(1, 1, 1, 1);
		c.setPosition(new Number3D(-2.224, 2.651, 5.384));
		c.setRotX(180);
		c.setRotY(180);
		mm4 = c.getModelViewMatrix();
		c = null;

		c = new MarkerPlane(1, 1, 1, 1);
		c.setPosition(new Number3D(-3.354f, 2.711f, 8.716));
		c.setRotX(180);
		c.setRotY(180);
		mm3 = c.getModelViewMatrix();
		c = null;
	}

	protected void initScene()
	{
		// mVideoTextureM = new TextureManager();

		mTextureManager = new TextureManager();
		mCamera.setFarPlane(50f);
		mCamera.setNearPlane(0.1f);

		/* camera init */
		Number3D la = SphericalToCartesian(phi, theta, 1);
		mCamera.setPosition(camPos);
		mCamera.setLookAt(new Number3D((camPos.x + la.x), (camPos.y + la.y), (camPos.z + la.z)));
		mCamera.setFieldOfView(FOV);
		lastTrackTime = System.nanoTime() * NS2S;
		System.gc();
	}

	public void clearScene()
	{
		if (mMediaPlayer.isPlaying())
		{
			mMediaPlayer.stop();
		}
		stopVideo();
		clearChildren();
		mTextureManager.reset();
		textureNames.clear();
		textureInfos.clear();
		modelNames.clear();
		models.clear();
		actions.clear();
		System.gc();
	}

	public void setPosition(String n, float[] CM)
	{
		if (doTracking == true)
		{
			Log.d("tracking","dotrack");
			isTracking = true;
			float c_phi = 0f;
			float c_theta = 0f;
			Number3D c_camPos = new Number3D();
			float curTime = System.nanoTime() * NS2S;
			if (n.contentEquals("marker1"))
			{
				mm = mm1;
			}
			else if (n.contentEquals("marker2"))
			{
				mm = mm2;
			}
			else if (n.contentEquals("marker3"))
			{
				mm = mm3;
			}
			else if (n.contentEquals("marker4"))
			{
				mm = mm4;
			}
			Matrix.invertM(mInvMatrix, 0, mm, 0);
			Matrix.multiplyMM(mCamMatrix, 0, mInvMatrix, 0, CM, 0);
			Number3D a_x = new Number3D(mCamMatrix[0], mCamMatrix[1], mCamMatrix[2]);
			Number3D a_y = new Number3D(mCamMatrix[4], mCamMatrix[5], mCamMatrix[6]);
			Number3D a_z = new Number3D(mCamMatrix[8], mCamMatrix[9], mCamMatrix[10]);
			q.fromAxes(a_x, a_y, a_z);
			float[] rm = new float[16];
			q.toRotationMatrix(rm);
			float pitch = Math.round(Math.toDegrees(q.getPitch(false)));
			float yaw = Math.round(Math.toDegrees(q.getYaw(false)));
			float roll = Math.round(Math.toDegrees(q.getRoll(false)));
			if (n.contentEquals("marker4"))
			{
				c_phi = -1 * yaw;
				c_theta = 180 + pitch;
				c_camPos.x = mCamMatrix[12];
				c_camPos.z = -1 * mCamMatrix[14];
			}
			else if (n.contentEquals("marker2") || n.contentEquals("marker1"))
			{
				c_phi = -1 * yaw + 180;
				c_theta = 180 - pitch;
				c_camPos.x = -1 * mCamMatrix[12];
				c_camPos.z = mCamMatrix[14];
			}
			else if (n.contentEquals("marker3"))
			{
				c_phi = -1 * yaw - 90;
				c_theta = 180 + pitch;
				c_camPos.x = mCamMatrix[14];
				c_camPos.z = mCamMatrix[12];
			}
			float dt = (float) (curTime - lastTrackTime);

			lastTrackTime = curTime;
			c_theta = c_theta + 90f;
			c_phi = c_phi % 360;
			c_theta = c_theta % 360;
			phi += (c_phi - phi) / 4;

			// theta += (c_theta - theta) / 4;
			// theta=c_theta;
			// phi=c_phi;
			markPos.x = c_camPos.x;
			markPos.z = c_camPos.z;
			stepPos.x = c_camPos.x;
			stepPos.z = c_camPos.z;
			// smoothedValue += timeSinceLastUpdate * (newValue - smoothedValue)
			// /
			// smoothing
			// theta = theta + 90f;
			// Number3D la = SphericalToCartesian(phi, theta, 1);
			// mCamera.setPosition(camPos);
			// mCamera.setLookAt(new Number3D((camPos.x + la.x), (camPos.y +
			// la.y),
			// (camPos.z + la.z)));
		}
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
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onDrawFrame(GL10 glUnused)
	{
		if (doReset == true)
		{
			resetScene();
			System.gc();
			doReset = false;
		}
		if (izLoaded == false)
		{
			clearScene();
			Message m = new Message();
			((ImageTargets) ctx).mLoadingHandler.sendMessage(m);
			loadScene();
			izLoaded = true;
			System.gc();
		}
		countCamPos();
		if (actions_enabled == true)
		{
			for (int i = 0; i < actions.size(); i++)
			{
				actions.get(i).checkAction(mCamera.getPosition(), this);
			}
		}
		/*
		 * if (mMediaPlayer.isPlaying() == true) {
		 * mVideoTexture.updateTexImage();
		 * mVideoTexture.getTransformMatrix(videoMatrix); int l =
		 * temp_texture_coords.length; float[] textureCoords = new float[l]; for
		 * (int i = 0; i < l; i = i + 2) { float x = videoMatrix[0] *
		 * temp_texture_coords[i] + videoMatrix[4] * temp_texture_coords[i + 1]
		 * + videoMatrix[12] * 1.f; float y = videoMatrix[1] *
		 * temp_texture_coords[i] + videoMatrix[5] * temp_texture_coords[i + 1]
		 * + videoMatrix[13] * 1.f; textureCoords[i] = x; textureCoords[i + 1] =
		 * y; } tmp_obj.getGeometry().setTextureCoords(textureCoords);// set //
		 * transformed // texture // coords Log.d("----------", "----------");
		 * for (int i = 0; i < textureCoords.length; i++) {
		 * Log.d("original texture", "coord: " + temp_texture_coords[i]);
		 * Log.d("transformed texture", "coord: " + textureCoords[i]); }
		 * 
		 * Log.d("----------", "----------");
		 * tmp_obj.getGeometry().changeBufferData
		 * (tmp_obj.getGeometry().getTexCoordBufferInfo(),
		 * tmp_obj.getGeometry().getTextureCoords(), 0); }
		 */
		super.onDrawFrame(glUnused);
	}

	public void resetRenderer()
	{
		doReset = true;
	}

	public void resetScene()
	{
		clearScene();
		curProj = 0;
		izLoaded = true;
		doLoad = true;
		doTracking = true;
	}

	protected void loadScene()
	{
		LoadObjects(ps.get(curProj));
		doTracking = true;
	}

	public void setObjs(ArrayList<ProjectLevel> p)
	{
		this.ps = p;
	}

	public void LoadTextures(ProjectLevel p)
	{
		for (int i = 0; i < p.Textures.size(); i++)
		{
			File f = new File(Environment.getExternalStorageDirectory() + "/" + dirn, p.Textures.get(i));
			if (f.exists())
			{
				String ttn = Environment.getExternalStorageDirectory() + "/" + dirn + "/" + p.Textures.get(i);
				Bitmap mBM = BitmapFactory.decodeFile(ttn);
				textureNames.add(i, p.Textures.get(i));
				textureInfos.add(i, mTextureManager.addTexture(mBM));
			}
			else
			{
				textureNames.add(i, null);
				textureInfos.add(i, null);
			}
		}
	}

	public TextureInfo getTextureByName(String n)
	{
		int ti = textureNames.indexOf(n);
		if (ti != -1)
		{
			return textureInfos.get(ti);
		}
		else return null;
	}

	public void LoadObjects(ProjectLevel p)
	{

		LoadTextures(p);
		for (int i = 0; i < p.getModels().size(); i++)
		{
			int objects = 0;
			ProjectObject pr_obj = p.getModels().get(i);
			String modelName = pr_obj.getModel();
			String onn = dirn + "/" + modelName;
			File f = new File(Environment.getExternalStorageDirectory() + "/" + dirn, modelName);

			if (f.exists())// does the file really exists?
			{
				mParser = new ObjParser(this, onn);
				mParser.parse();
				BaseObject3D obj = mParser.getParsedObject();
				obj.setMaterial(new SimpleMaterial());
				obj.setDepthMaskEnabled(true);
				obj.setDepthTestEnabled(true);
				obj.setBlendingEnabled(true);
				obj.setBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				if (pr_obj.isDoubleSided())
				{
					obj.setDoubleSided(true);
				}
				if (!pr_obj.isVisible())
				{
					Log.d("invisible", "invisible");
					obj.setVisible(false);
				}
				int ti = p.Textures.indexOf(pr_obj.getTexture());
				if (textureInfos.get(ti) != null)
				{
					obj.addTexture(textureInfos.get(ti));
				}
				if (pr_obj.isInteractive() == 1)
				{
					ObjectAction oa = p.getActionByName(pr_obj.getActionName());
					if (oa != null)
					{

						ObjectAction obj_act = (ObjectAction) oa.clone();
						obj_act.setModelName(modelName);
						BoundingBox bb = obj.getGeometry().getBoundingBox();
						Number3D mi = bb.getMin();
						Number3D mx = bb.getMax();
						Number3D cnt = new Number3D(mi.x + ((mx.x - mi.x) / 2), mi.y + ((mx.y - mi.y) / 2), mi.z + ((mx.z - mi.z) / 2));
						Log.d("center", "x: " + cnt.x + ", y:" + cnt.y + ", z:" + cnt.z);
						obj_act.setCenter(cnt, mCamera.getPosition());
						if (obj_act.getType().contentEquals("p"))
						{
							obj_act.setPlane(obj.getGeometry(), mCamera.getPosition());
						}
						Log.d("videotexture", pr_obj.getVideoTexture());
						obj_act.setVideo(pr_obj.getVideoTexture());
						obj_act.setVideoText(pr_obj.getVideoText());

						actions.add(obj_act);
					}
				}
				addChild(obj);
				models.add(objects, obj);
				modelNames.add(objects, modelName);
				objects++;
			}
		}
		System.gc();
	}

	public boolean checkTracking()
	{
		float ts = System.nanoTime() * NS2S;
		float dt = (ts - lastTrackTime);
		if (doTracking == true)
		{
			if ((isTracking == true && dt >= 0.5f))
			{
				isTracking = false;
				doTracking = false;
			}
		}
		else
		{
			isTracking = false;
		}
		return isTracking;
	}

	public void countCamPos()
	{
		if (checkTracking() == false)// if not tracking smooth steps positions
		{
			camPos.x += (stepPos.x - camPos.x) / 5;
			camPos.z += (stepPos.z - camPos.z) / 5;
		}
		else
		{
			camPos.x += (markPos.x - camPos.x) / 9;
			camPos.z += (markPos.z - camPos.z) / 9;
			stepPos.x = camPos.x;
			stepPos.z = camPos.z;
		}
		// check bounds of camera
		if (camPos.z > 5.386f) camPos.z = 5.386f;
		if (camPos.z < -5.386f) camPos.z = -5.386f;
		if (camPos.x > 8.716f) camPos.x = 8.716f;
		if (camPos.x < -8.716f) camPos.x = -8.716f;

		// phi=(phi+360)%360;
		Number3D la = SphericalToCartesian(phi, theta, 1);
		mCamera.setPosition(camPos);
		mCamera.setLookAt(new Number3D((camPos.x + la.x), (camPos.y + la.y), (camPos.z + la.z)));
	}

	public void doGyroRot(float add)
	{
		if (checkTracking() == false)
		{
			phi -= add;// add gyro rotation to current rotation
		}
	}

	public void doOrientationRot(float p)
	{
		theta = p;
	}

	public void setDockingPos()
	{
		camPos.x = 6.414f;
		camPos.z = 3.684f;
		theta = 90f;
		phi = 0f;
		Number3D la = SphericalToCartesian(phi, theta, 1);
		mCamera.setPosition(camPos);
		mCamera.setLookAt(new Number3D((camPos.x + la.x), (camPos.y + la.y), (camPos.z + la.z)));
	}

	public void onStep(float sl)
	{
		if (checkTracking() == false)
		{
			Number3D ap = CylindricalToCartesian(phi, sl, camPos.y);
			stepPos.x += ap.x;
			stepPos.z += ap.z;
		}
	}

	public void onBufferingUpdate(MediaPlayer arg0, int arg1)
	{
	}

	public void onPrepared(MediaPlayer mediaplayer)
	{
		mediaplayer.start();
	}

	public void onCompletion(MediaPlayer mediaplayer)
	{
		mediaplayer.stop();
		mediaplayer.reset();
	}

	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		return false;
	}

	public void onSurfaceDestroyed()
	{
		super.onSurfaceDestroyed();
	}

	public BaseObject3D getModelByName(String n)
	{
		int oi = modelNames.indexOf(n);// get object index
		if (oi != -1) return models.get(oi);
		else return null;
	}

	public void showProject(int k)
	{
		if (doLoad == true || curProj != k)
		{
			curProj = k;
			izLoaded = false;
			doLoad = false;
		}
	}

	public void setupVideoTexture()
	{

		// vmaterial = new VideoMaterial();
		// vt = mVideoTextureM.addVideoTexture();
		// int textureid = vt.getTextureId();

		// mVideoTexture = new SurfaceTexture(textureid);

		// mMediaPlayer = new MediaPlayer();
		// mMediaPlayer.setOnPreparedListener(this);
		// mMediaPlayer.setOnBufferingUpdateListener(this);
		// mMediaPlayer.setOnCompletionListener(this);
		// mMediaPlayer.setOnErrorListener(this);
		// mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		// mMediaPlayer.setSurface(new Surface(mVideoTexture));
		// mMediaPlayer.setDisplay(videoPlayer.getHolder());
		// mMediaPlayer.setLooping(true);
	}

	public void stopAudio()
	{
		if (mMediaPlayer.isPlaying())
		{
			mMediaPlayer.stop();
			mMediaPlayer.reset();

		}
		/*
		 * if (audioid != 0) { mAudioPlayer.stop(audioid);
		 * mAudioPlayer.unload(audioid); }
		 */
	}

	public void playAudio(String f)
	{
		stopAudio();
		// audioid = mAudioPlayer.load(Environment.getExternalStorageDirectory()
		// + "/" + dirn + "/" + f, 1);
		try
		{
			// mMediaPlayer.reset();

			mMediaPlayer.setDataSource(Environment.getExternalStorageDirectory() + "/" + dirn + "/" + f);
			mMediaPlayer.prepareAsync();
			// mMediaPlayer.prepare();
			// mMediaPlayer.seekTo(0);
			// mMediaPlayer.start();
		}
		catch (Exception e)
		{

		}

	}

	public void startVideo(BaseObject3D obj, String f, String t)
	{
		videofile = f;
		videotext = t;
		isplayvideo = true;

		// ((ImageTargets) ctx).startVideo(f);
		// act.startVideo(f);
		/*
		 * stopVideo(); // store object texture coordinates FloatBuffer tc =
		 * obj.getGeometry().getTextureCoords(); tc.rewind();
		 * temp_texture_coords = new float[tc.capacity()]; for (int i = 0; i <
		 * tc.capacity(); i++) { temp_texture_coords[i] = tc.get(i); } temp_ti =
		 * obj.getTextureInfoList().get(0); tmp_obj = obj;
		 * tmp_obj.setMaterial(vmaterial);// set video material
		 * tmp_obj.addTexture(vt);// set video texture try {
		 * mMediaPlayer.setDataSource(Environment.getExternalStorageDirectory()
		 * + "/" + dirn + "/" + f); mMediaPlayer.prepareAsync(); } catch
		 * (Exception e) {
		 * 
		 * }
		 */
	}

	public void stopVideo()
	{
		videofile = "";
		isplayvideo = false;
		/*
		 * Log.d("video", "stop"); if (mMediaPlayer.isPlaying()) {
		 * mMediaPlayer.stop(); mMediaPlayer.seekTo(0); } if (tmp_obj != null) {
		 * tmp_obj.getGeometry().setTextureCoords(temp_texture_coords);
		 * tmp_obj.getGeometry
		 * ().changeBufferData(tmp_obj.getGeometry().getTexCoordBufferInfo(),
		 * tmp_obj.getGeometry().getTextureCoords(), 0); tmp_obj.setMaterial(new
		 * SimpleMaterial());// set basic material
		 * tmp_obj.addTexture(temp_ti);// set default texture }
		 */

	}

	public void setActions(boolean a)
	{
		actions_enabled = a;
	}

	public void setVideoSurface(SurfaceView v)
	{
		/*
		 * videoPlayer=v; videoPlayer.setZOrderOnTop(false);
		 * videoPlayer.setVisibility(View.GONE); mMediaPlayer = new
		 * MediaPlayer(); mMediaPlayer.setOnPreparedListener(this);
		 * mMediaPlayer.setOnBufferingUpdateListener(this);
		 * mMediaPlayer.setOnCompletionListener(this);
		 * mMediaPlayer.setOnErrorListener(this);
		 * mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		 */
		// mMediaPlayer.setDisplay(videoPlayer.getHolder());
	}
}