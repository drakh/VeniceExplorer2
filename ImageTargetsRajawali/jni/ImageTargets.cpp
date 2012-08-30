#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

#ifdef USE_OPENGL_ES_1_1
#include <GLES/gl.h>
#include <GLES/glext.h>
#else
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#endif

#include <QCAR/QCAR.h>
#include <QCAR/CameraDevice.h>
#include <QCAR/Renderer.h>
#include <QCAR/VideoBackgroundConfig.h>
#include <QCAR/Trackable.h>
#include <QCAR/Tool.h>
#include <QCAR/Tracker.h>
#include <QCAR/TrackerManager.h>
#include <QCAR/ImageTracker.h>
#include <QCAR/CameraCalibration.h>
#include <QCAR/UpdateCallback.h>
#include <QCAR/DataSet.h>

#include "SampleUtils.h"

#ifdef __cplusplus
extern "C" {
#endif

// Textures:

// OpenGL ES 2.0 specific:
#ifdef USE_OPENGL_ES_2_0
unsigned int shaderProgramID = 0;
GLint vertexHandle = 0;
GLint normalHandle = 0;
GLint textureCoordHandle = 0;
GLint mvpMatrixHandle = 0;
#endif

// Screen dimensions:
unsigned int screenWidth = 0;
unsigned int screenHeight = 0;

// Indicates whether screen is in portrait (true) or landscape (false) mode
bool isActivityInPortraitMode = false;

// The projection matrix used for rendering virtual objects:
QCAR::Matrix44F projectionMatrix;

// Constants:
static const float kObjectScale = 3.f;

QCAR::DataSet* dataSetBienale = 0;

// Object to receive update callbacks from QCAR SDK
class ImageTargets_UpdateCallback: public QCAR::UpdateCallback {
	virtual void QCAR_onUpdate(QCAR::State& /*state*/) {
	}
};

ImageTargets_UpdateCallback updateCallback;

JNIEXPORT int JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_getOpenGlEsVersionNative(JNIEnv *, jobject)
{
#ifdef USE_OPENGL_ES_1_1        
	return 1;
#else
	return 2;
#endif
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setActivityPortraitMode(JNIEnv *, jobject, jboolean isPortrait)
{
	isActivityInPortraitMode = isPortrait;
}

JNIEXPORT int JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initTracker(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initTracker");

	// Initialize the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::Tracker* tracker = trackerManager.initTracker(QCAR::Tracker::IMAGE_TRACKER);
	if (tracker == NULL)
	{
		LOG("Failed to initialize ImageTracker.");
		return 0;
	}

	LOG("Successfully initialized ImageTracker.");
	return 1;
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitTracker(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitTracker");

	// Deinit the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	trackerManager.deinitTracker(QCAR::Tracker::IMAGE_TRACKER);
}

JNIEXPORT int JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_loadTrackerData(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_loadTrackerData");

	// Get the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
			trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
	if (imageTracker == NULL)
	{
		LOG("Failed to load tracking data set because the ImageTracker has not"
				" been initialized.");
		return 0;
	}

	// Create the data sets:
	dataSetBienale = imageTracker->createDataSet();
	if (dataSetBienale == 0)
	{
		LOG("Failed to create a new tracking data.");
		return 0;
	}

	// Load the data sets:
	if (!dataSetBienale->load("bienale.xml", QCAR::DataSet::STORAGE_APPRESOURCE))
	{
		LOG("Failed to load data set.");
		return 0;
	}

	// Activate the data set:
	if (!imageTracker->activateDataSet(dataSetBienale))
	{
		LOG("Failed to activate data set.");
		return 0;
	}

	LOG("Successfully loaded and activated data set.");
	return 1;
}

JNIEXPORT int JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_destroyTrackerData(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_destroyTrackerData");

	// Get the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
			trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
	if (imageTracker == NULL)
	{
		LOG("Failed to destroy the tracking data set because the ImageTracker has not"
				" been initialized.");
		return 0;
	}

	if (dataSetBienale != 0)
	{
		if (imageTracker->getActiveDataSet() == dataSetBienale &&
				!imageTracker->deactivateDataSet(dataSetBienale))
		{
			LOG("Failed to destroy the tracking data set StonesAndChips because the data set "
					"could not be deactivated.");
			return 0;
		}

		if (!imageTracker->destroyDataSet(dataSetBienale))
		{
			LOG("Failed to destroy the tracking data set StonesAndChips.");
			return 0;
		}

		LOG("Successfully destroyed the data set StonesAndChips.");
		dataSetBienale = 0;
	}

	return 1;
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_onQCARInitializedNative(JNIEnv *, jobject)
{
	// Register the update callback where we handle the data set swap:
	QCAR::registerCallback(&updateCallback);

	// Comment in to enable tracking of up to 2 targets simultaneously and
	// split the work over multiple frames:
	QCAR::setHint(QCAR::HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 4);
	QCAR::setHint(QCAR::HINT_IMAGE_TARGET_MULTI_FRAME_ENABLED, 1);
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_renderFrame(JNIEnv *, jobject)
{
	bool tracking=false;
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	QCAR::State state = QCAR::Renderer::getInstance().begin();
	QCAR::Renderer::getInstance().drawVideoBackground();
	for(int tIdx = 0; tIdx < state.getNumActiveTrackables(); tIdx++)
	{
		tracking=true;
		// Get the trackable:
		const QCAR::Trackable* trackable = state.getActiveTrackable(tIdx);
		QCAR::Matrix44F modelViewMatrix =
		QCAR::Tool::convertPose2GLMatrix(trackable->getPose());

		if (strcmp(trackable->getName(), "chips") == 0)
		{
			LOG(trackable->getName());
		}

	}
	QCAR::Renderer::getInstance().end();
	return tracking;
}
JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_renderFrame2(JNIEnv* env, jobject obj)
{
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	QCAR::State state = QCAR::Renderer::getInstance().begin();
	QCAR::Renderer::getInstance().drawVideoBackground();
	jclass cls=env->GetObjectClass(obj);
	jmethodID mid=env->GetMethodID(cls,"setTrackers","(Ljava/lang/String;FFFFFFFFFFFFFFFF)V");
	//jmethodID mid=env->GetMethodID(cls,"setTrackers","(Ljava/lang/String;)V");
	for(int tIdx = 0; tIdx < state.getNumActiveTrackables(); tIdx++)
	{
		// Get the trackable:
		const QCAR::Trackable* trackable = state.getActiveTrackable(tIdx);
		QCAR::Matrix44F modelViewMatrix = QCAR::Tool::convertPose2GLMatrix(trackable->getPose());
		jstring jstr=env->NewStringUTF(trackable->getName());
		env->CallVoidMethod(obj, mid, jstr,
				modelViewMatrix.data[0], modelViewMatrix.data[1], modelViewMatrix.data[2], modelViewMatrix.data[3],
				modelViewMatrix.data[4], modelViewMatrix.data[5], modelViewMatrix.data[6], modelViewMatrix.data[7],
				modelViewMatrix.data[8], modelViewMatrix.data[9], modelViewMatrix.data[10], modelViewMatrix.data[11],
				modelViewMatrix.data[12], modelViewMatrix.data[13], modelViewMatrix.data[14], modelViewMatrix.data[15]
				                     );
		LOG("Model coords: %f, %f %f",  modelViewMatrix.data[3], modelViewMatrix.data[7], modelViewMatrix.data[11]);
		//env->CallVoidMethod(obj, mid, jstr);
	}
	QCAR::Renderer::getInstance().end();
}
void configureVideoBackground() {
    // Get the default video mode:
    QCAR::CameraDevice& cameraDevice = QCAR::CameraDevice::getInstance();
    QCAR::VideoMode videoMode = cameraDevice.
                                getVideoMode(QCAR::CameraDevice::MODE_DEFAULT);


    // Configure the video background
    QCAR::VideoBackgroundConfig config;
    config.mEnabled = true;
    config.mSynchronous = true;
    config.mPosition.data[0] = 0.0f;
    config.mPosition.data[1] = 0.0f;

    if (isActivityInPortraitMode)
    {
        //LOG("configureVideoBackground PORTRAIT");
        config.mSize.data[0] = videoMode.mHeight
                                * (screenHeight / (float)videoMode.mWidth);
        config.mSize.data[1] = screenHeight;

        if(config.mSize.data[0] < screenWidth)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenWidth;
            config.mSize.data[1] = screenWidth *
                              (videoMode.mWidth / (float)videoMode.mHeight);
        }
    }
    else
    {
        //LOG("configureVideoBackground LANDSCAPE");
        config.mSize.data[0] = screenWidth;
        config.mSize.data[1] = videoMode.mHeight
                            * (screenWidth / (float)videoMode.mWidth);

        if(config.mSize.data[1] < screenHeight)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenHeight
                                * (videoMode.mWidth / (float)videoMode.mHeight);
            config.mSize.data[1] = screenHeight;
        }
    }

    LOG("Configure Video Background : Video (%d,%d), Screen (%d,%d), mSize (%d,%d)", videoMode.mWidth, videoMode.mHeight, screenWidth, screenHeight, config.mSize.data[0], config.mSize.data[1]);

    // Set the config:
    QCAR::Renderer::getInstance().setVideoBackgroundConfig(config);
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initApplicationNative(
		JNIEnv* env, jobject obj, jint width, jint height)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initApplicationNative");

	// Store screen dimensions
	screenWidth = width;
	screenHeight = height;
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initApplicationNative finished");
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitApplicationNative(
		JNIEnv* env, jobject obj)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitApplicationNative");

}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_startCamera(JNIEnv *,
		jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_startCamera");

	// Initialize the camera:
	if (!QCAR::CameraDevice::getInstance().init())
	return;

	// Configure the video background
	configureVideoBackground();

	// Select the default mode:
	if (!QCAR::CameraDevice::getInstance().selectVideoMode(
					QCAR::CameraDevice::MODE_DEFAULT))
	return;

	if (!QCAR::CameraDevice::getInstance().start())
	return;
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::Tracker* imageTracker = trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER);
	if(imageTracker != 0)
	imageTracker->start();
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_stopCamera(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_stopCamera");

	// Stop the tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::Tracker* imageTracker = trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER);
	if(imageTracker != 0)
	imageTracker->stop();

	QCAR::CameraDevice::getInstance().stop();
	QCAR::CameraDevice::getInstance().deinit();
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setProjectionMatrix(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setProjectionMatrix");

	// Cache the projection matrix:
	const QCAR::CameraCalibration& cameraCalibration =
	QCAR::CameraDevice::getInstance().getCameraCalibration();
	projectionMatrix = QCAR::Tool::getProjectionGL(cameraCalibration, 2.0f,
			2000.0f);
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_activateFlash(JNIEnv*, jobject, jboolean flash)
{
	return QCAR::CameraDevice::getInstance().setFlashTorchMode((flash==JNI_TRUE)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_autofocus(JNIEnv*, jobject)
{
	return QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setFocusMode(JNIEnv*, jobject, jint mode)
{
	int qcarFocusMode;

	switch ((int)mode)
	{
		case 0:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_NORMAL;
		break;

		case 1:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO;
		break;

		case 2:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_INFINITY;
		break;

		case 3:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_MACRO;
		break;

		default:
		return JNI_FALSE;
	}

	return QCAR::CameraDevice::getInstance().setFocusMode(qcarFocusMode) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_initRendering(
		JNIEnv* env, jobject obj)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_initRendering");
	glClearColor(0.0f, 0.0f, 0.0f, QCAR::requiresAlpha() ? 0.0f : 1.0f);
}

JNIEXPORT void JNICALL
Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_updateRendering(
		JNIEnv* env, jobject obj, jint width, jint height)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_updateRendering");

	// Update screen dimensions
	screenWidth = width;
	screenHeight = height;

	// Reconfigure the video background
	configureVideoBackground();
}

#ifdef __cplusplus
}
#endif
