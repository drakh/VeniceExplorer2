/*==============================================================================
            Copyright (c) 2012 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary
            
@file 
    ImageTargetsRenderer.java

@brief
    Sample for ImageTargets

==============================================================================*/

package com.qualcomm.QCARSamples.ImageTargets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.qualcomm.QCAR.QCAR;

/** The renderer class for the ImageTargets sample. */
public class ImageTargetsRenderer implements GLSurfaceView.Renderer
{
	public boolean			mIsActive	= false;
	private ImageTargets	act;

	/** Native function for initializing the renderer. */
	public native void initRendering();

	/** Native function to update the renderer. */
	public native void updateRendering(int width, int height);

	public void setAc(ImageTargets Acc)
	{
		act=Acc;
	}

	/** Called when the surface is created or recreated. */
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		DebugLog.LOGD("GLRenderer::onSurfaceCreated");

		// Call native function to initialize rendering:
		initRendering();

		// Call QCAR function to (re)initialize rendering after first use
		// or after OpenGL ES context was lost (e.g. after onPause/onResume):
		QCAR.onSurfaceCreated();
	}

	/** Called when the surface changed size. */
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		DebugLog.LOGD("GLRenderer::onSurfaceChanged");

		// Call native function to update rendering when render surface
		// parameters have changed:
		updateRendering(width, height);

		// Call QCAR function to handle render surface size changes:
		QCAR.onSurfaceChanged(width, height);
	}

	/** The native render function. */
	public native void renderFrame();

	public native void renderFrame2();

	/** Called to draw the current frame. */
	public void onDrawFrame(GL10 gl)
	{
		if (!mIsActive) return;
		renderFrame2();
	}

	public void setTrackers(String n, 
			float f0, float f1, float f2, float f3,
			float f4, float f5, float f6, float f7, 
			float f8, float f9, float f10, float f11, 
			float f12, float f13, float f14, float f15)
	{
		float[] mMVP = { 
					f0, f1, f2, f3, 
					f4, f5, f6, f7, 
					f8, f9, f10, f11, 
					f12,f13, f14, f15 };
		DebugLog.LOGD("tracker: " + n + "|" + f12 + "|" + f13 + "|" + f14);
		act.setPos(n, mMVP);
	}
}
