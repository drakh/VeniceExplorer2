/*==============================================================================
            Copyright (c) 2012 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary
            
@file 
    ImageTargets.java

@brief
    Sample for ImageTargets

==============================================================================*/

package com.qualcomm.QCARSamples.ImageTargets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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

/** The main activity for the ImageTargets sample. */
public class ImageTargets extends Activity
{
	// setup
	private String					filename						= "main.xml";
	private String					dirname							= "VeniceViewer2";
	private TextView				rotZ;
	private ArrayList<ProjectLevel>	vProjects;
	private LinearLayout			ll;

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
		mSplashScreenImageResource = R.drawable.splash_screen_image_targets;
		mQCARFlags = getInitializationFlags();
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
	}

	/** Native function to deinitialize the application. */
	private native void deinitApplicationNative();

	/** The final call you receive before your activity is destroyed. */
	protected void onDestroy()
	{
		DebugLog.LOGD("ImageTargets::onDestroy");
		super.onDestroy();

		// Dismiss the splash screen time out handler:
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
				// Initialize the ImageTracker
				if (initTracker() > 0)
				{
					// Proceed to next application initialization status
					updateApplicationStatus(APPSTATUS_INIT_APP_AR);
				}
				break;

			case APPSTATUS_INIT_APP_AR:
				// Initialize Augmented Reality-specific application elements
				// that may rely on the fact that the QCAR SDK has been
				// already initialized
				initApplicationAR();

				// Proceed to next application initialization status
				updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
				break;

			case APPSTATUS_LOAD_TRACKER:
				// Load the tracking data set
				//
				// This task instance must be created and invoked on the UI
				// thread and it can be executed only once!
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
				// Hint to the virtual machine that it would be a good time to
				// run the garbage collector.
				//
				// NOTE: This is only a hint. There is no guarantee that the
				// garbage collector will actually be run.
				System.gc();

				// Native post initialization:
				onQCARInitializedNative();

				// The elapsed time since the splash screen was visible:
				long splashScreenTime = System.currentTimeMillis()
						- mSplashScreenStartTime;
				long newSplashScreenTime = 0;
				if (splashScreenTime < MIN_SPLASH_SCREEN_TIME)
				{
					newSplashScreenTime = MIN_SPLASH_SCREEN_TIME
							- splashScreenTime;
				}

				// Request a callback function after a given timeout to dismiss
				// the splash screen:
				mSplashScreenHandler = new Handler();
				mSplashScreenRunnable = new Runnable()
				{
					public void run()
					{
						// Hide the splash screen
						mSplashScreenView.setVisibility(View.INVISIBLE);

						// Activate the renderer
						mRenderer.mIsActive = true;

						// Now add the GL surface view. It is important
						// that the OpenGL ES surface view gets added
						// BEFORE the camera is started and video
						// background is configured.
						addContentView(mGlView, new LayoutParams(
								LayoutParams.FILL_PARENT,
								LayoutParams.FILL_PARENT));

						addContentView(mGlView2, new LayoutParams(
								LayoutParams.FILL_PARENT,
								LayoutParams.FILL_PARENT));
						
						// Start the camera:
						//addContentView(ll);
						updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
					}
				};

				mSplashScreenHandler.postDelayed(mSplashScreenRunnable,
						newSplashScreenTime);
				break;

			case APPSTATUS_CAMERA_STOPPED:
				// Call the native function to stop the camera
				stopCamera();
				break;

			case APPSTATUS_CAMERA_RUNNING:
				// Call the native function to start the camera
				startCamera();
				setProjectionMatrix();
				break;

			default:
				throw new RuntimeException("Invalid application state");
		}
	}

	/** Tells native code whether we are in portait or landscape mode */
	private native void setActivityPortraitMode(boolean isPortrait);

	/** Initialize application GUI elements that are not related to AR. */
	private void initApplication()
	{
		mSplashScreenView = new ImageView(this);
		mSplashScreenView.setImageResource(mSplashScreenImageResource);
		addContentView(mSplashScreenView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		mSplashScreenStartTime = System.currentTimeMillis();

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
		mRenderer2 = new ObjectsRenderer(this);
		mRenderer2.setObjs(vProjects);
		mGlView2.setRenderer(mRenderer2);
		/*
		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setGravity(Gravity.TOP);
		*/

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
						/*
						 * if (an.contentEquals("da")) {
						 * da=Integer.parseInt(av); }
						 */
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

					}
				}
				else if (nodeName.contentEquals("object"))
				{
					ProjectObject po = new ProjectObject();
					for (int k = 0; k < xpp.getAttributeCount(); k++)
					{
						String an = xpp.getAttributeName(k);
						String av = xpp.getAttributeValue(k);
						if (an.contentEquals("model"))
						{
							po.setModel(dirn + "/" + av);
						}
						else if (an.contentEquals("texture"))
						{
							Log.d("ww", "set texture");
							po.setTexture(dirn + "/" + av);
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

						}
					}
					vProjects.get(jj).addModel(po);
				}
				else if (nodeName.contentEquals("action"))
				{

				}
				else if (nodeName.contentEquals("texture"))
				{
					// vProjects.get(jj).addTexture(xpp.getText());
				}
			}
			else if (eventType == XmlPullParser.END_TAG)
			{
				String nodeName = xpp.getName();
				Log.d("main", "end node: " + nodeName);
				if (nodeName.contentEquals("project"))
				{
					jj++;
				}
			}
			eventType = xpp.next();
		}
		Log.d("main", "Num projects: " + vProjects.size());
	}

	public void CreateTextMenu()
	{
		/* <build list of projects> */
		for (int i = 0; i < vProjects.size(); i++)
		{
			TextView chsP = new TextView(this);
			chsP.setTextSize(18);
			chsP.setGravity(Gravity.LEFT);
			chsP.setHeight(32);
			chsP.setShadowLayer(2f, 2f, 2f, Color.BLACK);
			chsP.setPadding(10, 2, 0, 2);
			chsP.setText(vProjects.get(i).getName());
			chsP.setClickable(false);
			/*
			chsP.setClickable(true);
			MyClickListener myh = new MyClickListener(i, this);
			chsP.setOnClickListener(myh);
			*/
			//ll.addView(chsP);
		}
	}

	private class MyClickListener implements OnClickListener
	{
		private int		w;
		ImageTargets	a;

		public MyClickListener(int w, ImageTargets a)
		{
			this.w = w;
			this.a = a;
		}

		public void onClick(View v)
		{
			Log.d("Clicked", "project no:" + getW());
			a.SelectProject(getW());
		}

		public int getW()
		{
			return w;
		}
	}

	public void SelectProject(int w)
	{
		 //mRenderer2.showProject(w);
	}
}
