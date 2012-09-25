package com.qualcomm.QCARSamples.ImageTargets;

import java.io.IOException;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback
{
	SurfaceHolder	mHolder;
	SurfaceHolder	mCHolder;
	public Camera	mVCamera;

	CameraPreview(Context context)
	{
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		mCHolder = holder;
		startPreview();
	}

	public void startPreview()
	{
		if (mVCamera == null && mCHolder!=null)
		{
			mVCamera = Camera.open(1);
			Log.d("camera","open camera");
			try
			{
				Camera.Parameters p = mVCamera.getParameters();
				// p.setPreviewSize(ps.width, ps.height);
				// mVCamera.setPreviewDisplay(holder);
				 mVCamera.setParameters(p);
				// mVCamera.startPreview();
				mVCamera.setPreviewDisplay(mCHolder);
				mVCamera.startPreview();
				Log.d("preview","preview started");
			}
			catch (IOException e)
			{
				Log.d("preview","preview filed");
			}
		}
	}

	public void stopPreview()
	{
		stopCam();
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		stopCam();
	}

	public void stopCam()
	{
		if (mVCamera != null)
		{
			mVCamera.stopPreview();
			mVCamera.release();
			mVCamera = null;
			System.gc();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
	}
}