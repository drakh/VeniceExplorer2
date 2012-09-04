package com.qualcomm.QCARSamples.ImageTargets;

import android.view.ViewGroup;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qualcomm.QCAR.QCAR;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.webkit.WebView;
//matej
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

/** The main activity for the ImageTargets sample. */
public class ImageTargets extends Activity implements SensorEventListener
{
	/*step detector*/
	private String					loadingText				= "Loading, please wait";
	private boolean					detecting				= false;
	private static int				mLimit					= 15;
	private static float			mLastValues[]			= new float[3 * 2];
	private static float			mScale[]				= new float[2];
	private static float			mYOffset				= 0;
	private static float			mLastDirections[]		= new float[3 * 2];
	private static float			mLastExtremes[][]		= {
			new float[3 * 2], new float[3 * 2]				};
	private static float			mLastDiff[]				= new float[3 * 2];
	private static int				mLastMatch				= -1;
	private int						steps					= 0;
	private float					step_len				= 0.67f;
	private int						da						= 0;
	
	private BroadcastReceiver batteryPluged;
	private BroadcastReceiver batteryUnplugged;
	//gui
	private FrameLayout mLayout;
	private ScrollView scrollContainer;
	private FrameLayout mGUI;
	private TextView mProjView;
	private LinearLayout mLL;
	private SensorManager			mSensorManager			= null;
	private TextView mMenuBtn;
	private TextView mInfoBtn;
	private boolean MenuVisible=true;
	private boolean InfoVisible=true;
	private WebView InfoText;
	private float[]					orientation=new float[3];
	
	// setup
	private String					filename						= "main.xml";
	private String					dirname							= "VeniceViewer";
	private ArrayList<ProjectLevel>	vProjects;
	public static final float		EPSILON					= 0.000000001f;
	private static final float		NS2S					= 1.0f / 1000000000.0f;
	private float					timestamp				= 0f;
	float							gyroVal					= 0;
	boolean							moving					= false;
	float							accVal					= 9.8f;
	private boolean checkLoad=false;
	// Application status constants:
	private static final int		APPSTATUS_UNINITED				= -1;
	private static final int		APPSTATUS_INIT_APP				= 0;
	private static final int		APPSTATUS_INIT_QCAR				= 1;
	private static final int		APPSTATUS_INIT_TRACKER			= 2;
	private static final int		APPSTATUS_INIT_APP_AR			= 3;
	private static final int		APPSTATUS_LOAD_TRACKER			= 4;
	private static final int		APPSTATUS_INITED				= 5;
	private static final int		APPSTATUS_CAMERA_STOPPED		= 6;
	private static final int		APPSTATUS_CAMERA_RUNNING		= 7;
	private static final String		NATIVE_LIB_SAMPLE				= "ImageTargets";
	private static final String		NATIVE_LIB_QCAR					= "QCAR";
	private QCARSampleGLView		mGlView;
	private GLSurfaceView			mGlView2;
	private ImageView				mSplashScreenView;
	private Handler					mSplashScreenHandler;
	public Handler mLoadingHandler;
	private String tutorial="";
	private Runnable				mSplashScreenRunnable;
	private static final long		MIN_SPLASH_SCREEN_TIME			= 2000;
	long							mSplashScreenStartTime			= 0;
	private ObjectsRenderer			mRenderer2;
	private ImageTargetsRenderer	mRenderer;
	private int						mScreenWidth					= 0;
	private int						mScreenHeight					= 0;
	private int						mAppStatus						= APPSTATUS_UNINITED;

	// The async tasks to initialize the QCAR SDK
	private InitQCARTask			mInitQCARTask;
	private LoadTrackerTask			mLoadTrackerTask;
	// An object used for synchronizing QCAR initialization, dataset loading and
	// the Android onDestroy() life cycle event. If the application is destroyed
	// while a data set is still being loaded, then we wait for the loading
	// operation to finish before shutting down QCAR.
	private Object					mShutdownLock					= new Object();

	// QCAR initialization flags
	private int						mQCARFlags						= 0;

	// The textures we will use for rendering:
	private int						mSplashScreenImageResource		= 0;

	// The menu item for swapping data sets:
	boolean							mIsStonesAndChipsDataSetActive	= false;

	/** Static initializer block to load native libraries on start-up. */
	
	static
	{
		loadLibrary(NATIVE_LIB_QCAR);
		loadLibrary(NATIVE_LIB_SAMPLE);
		int h = 480;
		mYOffset = h * 0.5f;
		mScale = new float[2];
		mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
		mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
	}

	/** An async task to initialize QCAR asynchronously. */
	private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
	{
		// Initialize with invalid value
		private int	mProgressValue	= -1;

		protected Boolean doInBackground(Void... params)
		{
			// Prevent the onDestroy() method to overlap with initialization:
			synchronized (mShutdownLock)
			{
				QCAR.setInitParameters(ImageTargets.this, mQCARFlags);

				do
				{
					mProgressValue = QCAR.init();
					publishProgress(mProgressValue);
				}
				while (!isCancelled() && mProgressValue >= 0
						&& mProgressValue < 100);

				return (mProgressValue > 0);
			}
		}

		protected void onProgressUpdate(Integer... values)
		{
		}

		protected void onPostExecute(Boolean result)
		{
			// Done initializing QCAR, proceed to next application
			// initialization status:
			if (result)
			{
				DebugLog.LOGD("InitQCARTask::onPostExecute: QCAR initialization"
						+ " successful");

				updateApplicationStatus(APPSTATUS_INIT_TRACKER);
			}
			else
			{
				// Create dialog box for display error:
				AlertDialog dialogError = new AlertDialog.Builder(
						ImageTargets.this).create();
				dialogError.setButton("Close",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
									int which)
							{
								// Exiting application
								System.exit(1);
							}
						});

				String logMessage;

				// NOTE: Check if initialization failed because the device is
				// not supported. At this point the user should be informed
				// with a message.
				if (mProgressValue == QCAR.INIT_DEVICE_NOT_SUPPORTED)
				{
					logMessage = "Failed to initialize QCAR because this "
							+ "device is not supported.";
				}
				else
				{
					logMessage = "Failed to initialize QCAR.";
				}

				// Log error:
				DebugLog.LOGE("InitQCARTask::onPostExecute: " + logMessage
						+ " Exiting.");

				// Show dialog box with error message:
				dialogError.setMessage(logMessage);
				dialogError.show();
			}
		}
	}

	/** An async task to load the tracker data asynchronously. */
	private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
	{
		protected Boolean doInBackground(Void... params)
		{
			// Prevent the onDestroy() method to overlap:
			synchronized (mShutdownLock)
			{
				// Load the tracker data set:
				return (loadTrackerData() > 0);
			}
		}

		protected void onPostExecute(Boolean result)
		{
			DebugLog.LOGD("LoadTrackerTask::onPostExecute: execution "
					+ (result ? "successful" : "failed"));

			if (result)
			{
				// The stones and chips data set is now active:
				mIsStonesAndChipsDataSetActive = true;

				// Done loading the tracker, update application status:
				updateApplicationStatus(APPSTATUS_INITED);
			}
			else
			{
				// Create dialog box for display error:
				AlertDialog dialogError = new AlertDialog.Builder(
						ImageTargets.this).create();
				dialogError.setButton("Close",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
									int which)
							{
								// Exiting application
								System.exit(1);
							}
						});

				// Show dialog box with error message:
				dialogError.setMessage("Failed to load tracker data.");
				dialogError.show();
			}
		}
	}

	private void storeScreenDimensions()
	{
		// Query display dimensions
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
	}

	/**
	 * Called when the activity first starts or the user navigates back to an
	 * activity.
	 */
	protected void onCreate(Bundle savedInstanceState)
	{
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		setRequestedOrientation(screenOrientation);
		setActivityPortraitMode(screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		storeScreenDimensions();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE);
		DebugLog.LOGD("ImageTargets::onCreate");
		chkConfig();
		super.onCreate(savedInstanceState);
		
		mLayout=new FrameLayout(this);//main layout
		mLayout.setForegroundGravity(Gravity.CENTER);
		setContentView(mLayout);
		
		mGUI=new FrameLayout(this);
		
		/*menu*/
		scrollContainer=new ScrollView(this);
		scrollContainer.setLayoutParams(new ViewGroup.LayoutParams(300, mScreenHeight-100));
		scrollContainer.setY(50f);
		mLL=new LinearLayout(this);
		mLL.setOrientation(LinearLayout.VERTICAL);
		mLL.setGravity(Gravity.TOP);
		mLL.setScrollContainer(true);
		mLL.setBackgroundColor(Color.DKGRAY);
		mLL.setAlpha(0.8f);
		scrollContainer.addView(mLL);
		mGUI.addView(scrollContainer);
		
		/* info text*/
		InfoText=new WebView(this);
		InfoText.setLayoutParams(new ViewGroup.LayoutParams(mScreenWidth-600, mScreenHeight-100));
		InfoText.setY(50f);
		InfoText.setX(300f);
		InfoText.setBackgroundColor(Color.WHITE);
		InfoText.setAlpha(0.5f);
		mGUI.addView(InfoText);
		
		/*info button*/
		mInfoBtn=new TextView(this);
		mInfoBtn.setBackgroundResource(R.drawable.back_b);
		mInfoBtn.setLayoutParams(new ViewGroup.LayoutParams(90, 25));
		mInfoBtn.setText("Info");
		mInfoBtn.setX(mScreenWidth-110);
		mInfoBtn.setY(mScreenHeight-45);
		mInfoBtn.setGravity(Gravity.RIGHT);
		mInfoBtn.setTextColor(Color.DKGRAY);
		mInfoBtn.setShadowLayer(2f, 2f, 2f, Color.LTGRAY);
		mInfoBtn.setTextSize(14);
		mInfoBtn.setPadding(0, 0, 10, 0);
		mInfoBtn.setClickable(true);
		MyInfoClickListener mic = new MyInfoClickListener();
		mInfoBtn.setOnClickListener(mic);
		mGUI.addView(mInfoBtn);
		
		/*menu button*/
		mMenuBtn=new TextView(this);
		mMenuBtn.setBackgroundResource(R.drawable.back_b);
		mMenuBtn.setLayoutParams(new ViewGroup.LayoutParams(90, 25));
		mMenuBtn.setText("Menu");
		mMenuBtn.setX(10);
		mMenuBtn.setY(15);
		mMenuBtn.setGravity(Gravity.RIGHT);
		mMenuBtn.setTextColor(Color.DKGRAY);
		mMenuBtn.setShadowLayer(2f, 2f, 2f, Color.LTGRAY);
		mMenuBtn.setTextSize(14);
		mMenuBtn.setPadding(0, 0, 10, 0);
		mMenuBtn.setClickable(true);
		MyMMenuClickListener mmc=new MyMMenuClickListener();
		mMenuBtn.setOnClickListener(mmc);
		mGUI.addView(mMenuBtn);
		
		/* project name*/
		mProjView=new TextView(this);
		mProjView.setTextSize(24);
		mProjView.setGravity(Gravity.CENTER_HORIZONTAL);
		mProjView.bringToFront();
		mProjView.setHeight(52);
		mProjView.setShadowLayer(2f, 2f, 2f, Color.BLACK);
		mProjView.setPadding(10, 10, 0, 10);
		mGUI.addView(mProjView);
		
		/* splash screen*/
		mSplashScreenImageResource = R.drawable.splash_screen_image_targets;
		mQCARFlags = getInitializationFlags();
		
		CreateTextMenu();
		
		mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		initListeners();
		
		updateApplicationStatus(APPSTATUS_INIT_APP);
	}

	/** Configure QCAR with the desired version of OpenGL ES. */
	private int getInitializationFlags()
	{
		int flags = 0;

		// Query the native code:
		if (getOpenGlEsVersionNative() == 1)
		{
			flags = QCAR.GL_11;
		}
		else
		{
			flags = QCAR.GL_20;
		}

		return flags;
	}

	/**
	 * native method for querying the OpenGL ES version. Returns 1 for OpenGl ES
	 * 1.1, returns 2 for OpenGl ES 2.0.
	 */
	public native int getOpenGlEsVersionNative();

	/** Native tracker initialization and deinitialization. */
	public native int initTracker();

	public native void deinitTracker();

	/** Native functions to load and destroy tracking data. */
	public native int loadTrackerData();

	public native void destroyTrackerData();

	/** Native sample initialization. */
	public native void onQCARInitializedNative();

	/** Native methods for starting and stoping the camera. */
	private native void startCamera();

	private native void stopCamera();

	/**
	 * Native method for setting / updating the projection matrix for AR content
	 * rendering
	 */
	private native void setProjectionMatrix();

	/** Called when the activity will start interacting with the user. */
	@Override
	protected void onResume()
	{
		DebugLog.LOGD("ImageTargets::onResume");
		super.onResume();

		// QCAR-specific resume operation
		QCAR.onResume();

		// We may start the camera only if the QCAR SDK has already been
		// initialized
		if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
		{
			updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);

			// Reactivate flash if it was active before pausing the app
			if (mFlash)
			{
				boolean result = activateFlash(mFlash);
				DebugLog.LOGI("Turning flash " + (mFlash ? "ON" : "OFF") + " "
						+ (result ? "WORKED" : "FAILED") + "!!");
			}
		}

		// Resume the GL view:
		if (mGlView != null)
		{
			mGlView.setVisibility(View.VISIBLE);
			mGlView.onResume();
		}
		if (mGlView2 != null)
		{
			mGlView2.setVisibility(View.VISIBLE);
			mGlView2.onResume();
		}
		initListeners();
	}

	/** Called when the system is about to start resuming a previous activity. */
	protected void onPause()
	{
		DebugLog.LOGD("ImageTargets::onPause");
		super.onPause();

		if (mGlView != null)
		{
			mGlView.setVisibility(View.INVISIBLE);
			mGlView.onPause();
		}
		if (mGlView2 != null)
		{
			mGlView2.setVisibility(View.INVISIBLE);
			mGlView2.onResume();
		}
		if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
		{
			updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
		}
		// QCAR-specific pause operation
		QCAR.onPause();
		mSensorManager.unregisterListener(this);
		//onStop();
		onDestroy();
	}
	/** Native function to deinitialize the application. */
	private native void deinitApplicationNative();

	/** The final call you receive before your activity is destroyed. */
	protected void onDestroy()
	{
		DebugLog.LOGD("ImageTargets::onDestroy");
		super.onDestroy();

		// Dismiss the splash screen time out handler:
		if(mLoadingHandler!=null) 
		{
			mLoadingHandler=null;
		}
		if (mSplashScreenHandler != null)
		{
			mSplashScreenHandler.removeCallbacks(mSplashScreenRunnable);
			mSplashScreenRunnable = null;
			mSplashScreenHandler = null;
		}

		// Cancel potentially running tasks
		if (mInitQCARTask != null
				&& mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
		{
			mInitQCARTask.cancel(true);
			mInitQCARTask = null;
		}

		if (mLoadTrackerTask != null
				&& mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
		{
			mLoadTrackerTask.cancel(true);
			mLoadTrackerTask = null;
		}
		if(batteryPluged!=null)
		{
			this.unregisterReceiver(batteryPluged);
			batteryPluged=null;
		}
		if(batteryUnplugged!=null)
		{
			this.unregisterReceiver(batteryUnplugged);
			batteryUnplugged=null;
		}
		// Ensure that all asynchronous operations to initialize QCAR and
		// loading
		// the tracker datasets do not overlap:
		synchronized (mShutdownLock)
		{

			// Do application deinitialization in native code
			deinitApplicationNative();
			destroyTrackerData();
			deinitTracker();
			QCAR.deinit();
		}
		System.gc();
	}
	/**
	 * NOTE: this method is synchronized because of a potential concurrent
	 * access by ImageTargets::onResume() and InitQCARTask::onPostExecute().
	 */
	private synchronized void updateApplicationStatus(int appStatus)
	{
		// Exit if there is no change in status
		if (mAppStatus == appStatus) return;

		// Store new status value
		mAppStatus = appStatus;

		// Execute application state-specific actions
		switch (mAppStatus)
		{
			case APPSTATUS_INIT_APP:
				initApplication();
				updateApplicationStatus(APPSTATUS_INIT_QCAR);
				break;

			case APPSTATUS_INIT_QCAR:
				// Initialize QCAR SDK asynchronously to avoid blocking the
				// main (UI) thread.
				// This task instance must be created and invoked on the UI
				// thread and it can be executed only once!
				try
				{
					mInitQCARTask = new InitQCARTask();
					mInitQCARTask.execute();
				}
				catch (Exception e)
				{
					DebugLog.LOGE("Initializing QCAR SDK failed");
				}
				break;

			case APPSTATUS_INIT_TRACKER:
				if (initTracker() > 0)
				{
					updateApplicationStatus(APPSTATUS_INIT_APP_AR);
				}
				break;

			case APPSTATUS_INIT_APP_AR:
				initApplicationAR();
				updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
				break;

			case APPSTATUS_LOAD_TRACKER:
				try
				{
					mLoadTrackerTask = new LoadTrackerTask();
					mLoadTrackerTask.execute();
				}
				catch (Exception e)
				{
					DebugLog.LOGE("Loading tracking data set failed");
				}
				break;

			case APPSTATUS_INITED:
				System.gc();
				onQCARInitializedNative();
				long splashScreenTime = System.currentTimeMillis()
						- mSplashScreenStartTime;
				long newSplashScreenTime = 0;
				if (splashScreenTime < MIN_SPLASH_SCREEN_TIME)
				{
					newSplashScreenTime = MIN_SPLASH_SCREEN_TIME
							- splashScreenTime;
				}
				mLoadingHandler=new Handler(){
					public void handleMessage(Message msg)
					{
						String m=(String) msg.obj;
						showLoading();
					}
				};
				mSplashScreenHandler = new Handler();
				mSplashScreenRunnable = new Runnable()
				{
					public void run()
					{
						mSplashScreenView.setVisibility(View.INVISIBLE);
						mRenderer.mIsActive = true;
						mLayout.addView(mGlView);
						mLayout.addView(mGlView2);
						mLayout.addView(mGUI);						
						updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
					}
				};

				mSplashScreenHandler.postDelayed(mSplashScreenRunnable,
						newSplashScreenTime);
				break;

			case APPSTATUS_CAMERA_STOPPED:
				stopCamera();
				break;
			case APPSTATUS_CAMERA_RUNNING:
				startCamera();
				setProjectionMatrix();
				break;
			default:
				throw new RuntimeException("Invalid application state");
		}
	}
	public void showLoading()
	{
		checkLoad=true;
		mProjView.setText(loadingText);	
	}
	public void hideLoading()
	{
		if(checkLoad==true && mRenderer2.izLoaded==true)
		{
			int cp=mRenderer2.curProj;
			mProjView.setText(vProjects.get(cp).getName());
			checkLoad=false;
		}
	}
	/** Tells native code whether we are in portait or landscape mode */
	private native void setActivityPortraitMode(boolean isPortrait);

	/** Initialize application GUI elements that are not related to AR. */
	private void initApplication()
	{
		mSplashScreenView = new ImageView(this);
		mSplashScreenView.setImageResource(mSplashScreenImageResource);
		mLayout.addView(mSplashScreenView);
		mSplashScreenStartTime = System.currentTimeMillis();		
		batteryPluged=new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				hideGUI();
				HideTutorial();
			}
		};
		batteryUnplugged=new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				showGUI();	
				mRenderer2.setDockingPos();
			}
		};
		this.registerReceiver(batteryPluged, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
		this.registerReceiver(batteryUnplugged, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));

	}
	public void showGUI()
	{
		mGUI.setVisibility(View.VISIBLE);
		ShowDefaultTutorial();
		HideMenu();
	}
	public void hideGUI()
	{
		mProjView.setText("");
		mGUI.setVisibility(View.GONE);
		mRenderer2.resetRenderer();
	}
	/** Native function to initialize the application. */
	private native void initApplicationNative(int width, int height);

	/** Initializes AR application components. */
	private void initApplicationAR()
	{
		initApplicationNative(mScreenWidth, mScreenHeight);

		// Create OpenGL ES view:
		int depthSize = 16;
		int stencilSize = 0;
		boolean translucent = QCAR.requiresAlpha();
		
		/* camera */
		mGlView = new QCARSampleGLView(this);
		mGlView.init(mQCARFlags, translucent, depthSize, stencilSize);
		mRenderer = new ImageTargetsRenderer();
		mRenderer.setAc(this);
		mGlView.setRenderer(mRenderer);

		/* objects */
		mGlView2 = new GLSurfaceView(this);
		mGlView2.setEGLContextClientVersion(2);
		mGlView2.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		mGlView2.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		mGlView2.setZOrderMediaOverlay(true);
		mRenderer2 = new ObjectsRenderer(this,dirname/*,mLoadingHandler*/);
		mRenderer2.setObjs(vProjects);
		mGlView2.setRenderer(mRenderer2);
	}

	public void setPos(String n, float[] mMV)
	{
		mRenderer2.setPosition(n, mMV);
	}

	/** Tells native code to switch dataset as soon as possible */
	private native void switchDatasetAsap();

	private boolean	mFlash	= false;

	private native boolean activateFlash(boolean flash);

	private native boolean autofocus();

	private native boolean setFocusMode(int mode);

	/** A helper for loading native libraries stored in "libs/armeabi*". */
	public static boolean loadLibrary(String nLibName)
	{
		try
		{
			System.loadLibrary(nLibName);
			DebugLog.LOGI("Native library lib" + nLibName + ".so loaded");
			return true;
		}
		catch (UnsatisfiedLinkError ulee)
		{
			DebugLog.LOGE("The library lib" + nLibName
					+ ".so could not be loaded");
		}
		catch (SecurityException se)
		{
			DebugLog.LOGE("The library lib" + nLibName
					+ ".so was not allowed to be loaded");
		}

		return false;
	}

	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters p)
	{
		Camera.Size result = null;
		for (Camera.Size size : p.getSupportedPreviewSizes())
		{
			if (size.width <= width && size.height <= height)
			{
				if (result == null)
				{
					result = size;
				}
				else
				{
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea)
					{
						result = size;
					}
				}
			}
		}
		return result;
	}

	public void chkConfig()
	{
		Log.d("main", "checkit");
		File folder = new File(Environment.getExternalStorageDirectory() + "/"
				+ dirname);
		if (!folder.exists())
		{
			folder.mkdir();
		}
		folder = new File(Environment.getExternalStorageDirectory() + "/"
				+ dirname + "/" + filename);
		try
		{
			vProjects = new ArrayList();
			parseXML(folder, dirname);
		}
		catch (XmlPullParserException e)
		{
			Log.d("main", "xml error");
		}
		catch (IOException e)
		{
			Log.d("main", "io error");

		}

	}

	public void parseXML(File file, String dirn) throws XmlPullParserException,
			IOException
	{
		Log.d("main", "xml parser?");
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		FileInputStream fis = new FileInputStream(file);
		xpp.setInput(new InputStreamReader(fis));
		int eventType = xpp.getEventType();
		int jj = 0;
		ProjectObject po;
		while (eventType != XmlPullParser.END_DOCUMENT)
		{
			if (eventType == XmlPullParser.START_DOCUMENT)
			{
				Log.d("main", "tralala");
			}
			else if (eventType == XmlPullParser.START_TAG)
			{
				String nodeName = xpp.getName();
				Log.d("main", nodeName);
				if (nodeName.contentEquals("venice"))
				{
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						
						 if (an.contentEquals("htmlfile")) 
						 {
							 tutorial=av; 
						 }
						
					}
				}
				else if (nodeName.contentEquals("project"))
				{
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("name"))
						{
							vProjects.add(jj, new ProjectLevel(av));
						}
						else if(an.contentEquals("htmlfile"))
						{
							vProjects.get(jj).setHtml(av);
						}

					}
				}
				else if (nodeName.contentEquals("object"))
				{
					po = new ProjectObject();
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("model"))
						{
							po.setModel(av);
						}
						else if (an.contentEquals("texture"))
						{
							Log.d("parser","texture attribute: "+av+"|"+jj);
							vProjects.get(jj).addTexture(av);
							po.setTexture(av);
						}
						else if (an.contentEquals("doublesided"))
						{
							po.setDS(av);
						}
						else if (an.contentEquals("usevideo"))
						{
							po.setVideo(av);
						}
						else if (an.contentEquals("interactive"))
						{
							po.setInteractive(av);
						}
						else if (an.contentEquals("visible"))
						{
							po.setVisible(av);
						}
					}
					vProjects.get(jj).addModel(po);
				}
				
				else if (nodeName.contentEquals("action"))
				{

				}
				else if (nodeName.contentEquals("texture"))
				{
					String tn=xpp.nextText();
					Log.d("parser","texture: "+tn+"|"+jj);
					vProjects.get(jj).addTexture(tn);
				}
			}
			
			else if (eventType == XmlPullParser.END_TAG)
			{
				String nodeName = xpp.getName();
				Log.d("parser", "end node: " + nodeName);
				if (nodeName.contentEquals("project"))
				{
					jj++;
				}
			}
			eventType = xpp.next();
		}
		Log.d("main", "Num projects: " + vProjects.size());
	}

	public void SelectProject(int w)
	{
		HideMenu();
		HideTutorial();
		mRenderer2.showProject(w);
	}
	public void initListeners()
	{
		mSensorManager.registerListener(this, mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_UI);
	}
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{

	}
	public void onSensorChanged(SensorEvent event)
	{
		hideLoading();
		switch (event.sensor.getType())
		{

			case Sensor.TYPE_GYROSCOPE:
				gyroFunction(event);
				break;
			case Sensor.TYPE_ORIENTATION:
				orientation[0] = event.values[1] + 180;
				orientation[1] = event.values[0];
				orientation[2] = event.values[2];
				//setCameraPos();
				//if (!detecting)
				//{
					//storeCurrentRotPos();
				//}
				break;
			case Sensor.TYPE_ACCELEROMETER:
				accFunction(event);
				if (detecting) detectStep(event);
				break;
		}
	}
	public void gyroFunction(SensorEvent event)
	{
		if (timestamp * NS2S > 2)
		{
			final float dT = (event.timestamp - timestamp) * NS2S;
			final float rot_v = event.values[1];
			gyroVal += (rot_v - gyroVal) / 10;
			float omegaMagnitude = (float) Math.sqrt(event.values[0]
					* event.values[0] + gyroVal * gyroVal + event.values[2]
					* event.values[2]);
			if (omegaMagnitude > EPSILON && moving == true)
			{
				float rot =(float) Math.toDegrees(gyroVal * dT);
				mRenderer2.doGyroRot(rot);
			}
		}
		timestamp = event.timestamp;
	}
	public void accFunction(SensorEvent event)
	{
		float omegaMagnitude = (float) Math.sqrt(event.values[0]
				* event.values[0] + event.values[1] * event.values[1]
				+ event.values[2] * event.values[2]);
		float prevAcc = accVal;
		accVal += (omegaMagnitude - accVal) / 2.5;
		if (Math.abs(prevAcc - accVal) >= 0.02)
		{
			moving = true;
		}
		else
		{
			moving = false;
		}
	}
	
	private void detectStep(SensorEvent event)
	{
		if (event == null) return;

		else
		{
			float vSum = 0;
			for (int i = 0; i < 3; i++)
			{
				final float v = mYOffset + event.values[i] * mScale[0];
				vSum += v;
			}
			int k = 0;
			float v = vSum / 3;

			float direction = (v > mLastValues[k] ? 1
					: (v < mLastValues[k] ? -1 : 0));
			if (direction == -mLastDirections[k])
			{
				// Direction changed
				int extType = (direction > 0 ? 0 : 1); // minumum or
														// maximum?
				mLastExtremes[extType][k] = mLastValues[k];
				float diff = Math.abs(mLastExtremes[extType][k]
						- mLastExtremes[1 - extType][k]);

				if (diff > mLimit)
				{

					boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
					boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
					boolean isNotContra = (mLastMatch != 1 - extType);

					if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
							&& isNotContra)
					{
						onStep();
						mLastMatch = extType;
					}
					else
					{
						mLastMatch = -1;
					}
				}
				mLastDiff[k] = diff;
			}
			mLastDirections[k] = direction;
			mLastValues[k] = v;
		}
	}
	
	public void onStep()
	{
		mRenderer2.onStep(step_len);
		//float[] ap = CylindricalToCartesian(mRenderer2.phi, step_len, cam_y);
		//ap[0] = cam_x - ap[0];
		//ap[2] = cam_z - ap[2];
		//mRenderer.setCamPos(ap[0], ap[1], ap[2]);
		//storeCurrentRotPos();
		//steps++;
	}
	public void CreateTextMenu()
	{
		/* <build list of projects> */
		for (int i = 0; i < vProjects.size(); i++)
		{
			TextView chsP = new TextView(this);
			chsP.setTextSize(18);
			chsP.setGravity(Gravity.LEFT);
			chsP.setHeight(72);
			chsP.setShadowLayer(2f, 2f, 2f, Color.BLACK);
			chsP.setPadding(10, 10, 0, 10);
			chsP.setText(vProjects.get(i).getName());
			chsP.setClickable(true);
			MyMenuClickListener myh = new MyMenuClickListener(i);
			chsP.setOnClickListener(myh);
			mLL.addView(chsP);
		}
		HideTutorial();
		HideMenu();
	}
	public void ShowHideMenu()
	{
		if(MenuVisible==true)
		{
			HideMenu();
		}
		else
		{
			ShowMenu();
		}
	}
	public void ShowMenu()
	{
		scrollContainer.setVisibility(View.VISIBLE);
		mMenuBtn.setBackgroundResource(R.drawable.info_b);
		HideTutorial();
		MenuVisible=true;
	}
	public void HideMenu()
	{
		
		scrollContainer.setVisibility(View.GONE);
		mMenuBtn.setBackgroundResource(R.drawable.back_b);
		MenuVisible=false;
	}
	public void ShowDefaultTutorial()
	{
		ShowTutorial("file://"+Environment.getExternalStorageDirectory()+"/"+dirname+"/"+tutorial);
	}
	public void ShowProjectTutorial(int i)
	{
		String f=vProjects.get(i).htmlFile;
		ShowTutorial("file://"+Environment.getExternalStorageDirectory()+"/"+dirname+"/"+f);
	}
	public void ShowTutorial(String uri)
	{
		InfoText.loadUrl(uri);
		InfoText.setVisibility(View.VISIBLE);
		mInfoBtn.setBackgroundResource(R.drawable.info_b);
		InfoVisible=true;
	}
	public void HideTutorial()
	{
		InfoText.setVisibility(View.GONE);
		mInfoBtn.setBackgroundResource(R.drawable.back_b);
		InfoVisible=false;
	}
	public void ShowHideTutorial()
	{
		Log.d("tutr","shown: "+InfoVisible);
		if(InfoVisible==true)
		{
			HideTutorial();
		}
		else
		{
			if(mRenderer2.doLoad==false)
			{
				int c=mRenderer2.curProj;
				ShowProjectTutorial(c);
			}
			else
			{
			ShowDefaultTutorial();
			}
		}
	}
	private class MyInfoClickListener implements OnClickListener
	{
		public MyInfoClickListener()
		{
			
		}
		public void onClick(View v)
		{
			ShowHideTutorial();
		}
	}
	private class MyMMenuClickListener implements OnClickListener
	{
		public MyMMenuClickListener()
		{
			
		}
		public void onClick(View v)
		{
			ShowHideMenu();
		}
	}
	private class MyMenuClickListener implements OnClickListener
	{
		private int				w;

		public MyMenuClickListener(int w)
		{
			this.w = w;
		}

		public void onClick(View v)
		{
			SelectProject(getW());
		}

		public int getW()
		{
			return w;
		}
	}
}
