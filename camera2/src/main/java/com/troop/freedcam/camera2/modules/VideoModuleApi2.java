package com.troop.freedcam.camera2.modules;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.troop.filelogger.Logger;
import com.troop.freedcam.camera2.BaseCameraHolderApi2;
import com.troop.freedcam.camera2.parameters.ParameterHandlerApi2;
import com.troop.freedcam.camera2.parameters.modes.VideoProfilesApi2;
import com.troop.freedcam.i_camera.modules.AbstractModule;
import com.troop.freedcam.i_camera.modules.AbstractModuleHandler;
import com.troop.freedcam.i_camera.modules.I_RecorderStateChanged;
import com.troop.freedcam.i_camera.modules.ModuleEventHandler;
import com.troop.freedcam.i_camera.modules.VideoMediaProfile;
import com.troop.freedcam.ui.AppSettingsManager;
import com.troop.freedcam.utils.StringUtils;
import com.troop.freedcam.utils.VideoUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.troop.freedcam.camera2.BaseCameraHolderApi2.getSizeForPreviewDependingOnImageSize;

/**
 * Created by troop on 26.11.2015.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoModuleApi2 extends AbstractModuleApi2
{
    private static String TAG = VideoModuleApi2.class.getSimpleName();
    BaseCameraHolderApi2 cameraHolder;
    boolean isRecording = false;
    Size previewSize;
    VideoMediaProfile currentVideoProfile;
    private Surface previewsurface;
    private Surface recorderSurface;

    public MediaRecorder mediaRecorder;

    public VideoModuleApi2(BaseCameraHolderApi2 cameraHandler, AppSettingsManager Settings, ModuleEventHandler eventHandler) {
        super(cameraHandler, Settings, eventHandler);
        this.cameraHolder = cameraHandler;
        this.Settings = Settings;
        this.name = AbstractModuleHandler.MODULE_VIDEO;
    }

    @Override
    public String ModuleName() {
        return name;
    }

    @Override
    public boolean DoWork()
    {
        if (isRecording)
            stopRecording();
        else
            startRecording();
        return true;
    }

    @Override
    public boolean IsWorking() {
        return super.IsWorking();
    }

    @Override
    public void LoadNeededParameters()
    {
        Logger.d(TAG, "LoadNeededParameters");
        cameraHolder.ModulePreview = this;
        VideoProfilesApi2 profilesApi2 = (VideoProfilesApi2) ParameterHandler.VideoProfiles;
        currentVideoProfile = profilesApi2.GetCameraProfile(Settings.getString(AppSettingsManager.SETTING_VIDEPROFILE));
        cameraHolder.StartPreview();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void UnloadNeededParameters()
    {
        Logger.d(TAG, "UnloadNeededParameters");
        cameraHolder.CaptureSessionH.CloseCaptureSession();
        previewsurface = null;
    }

    @Override
    public String LongName() {
        return "Video";
    }

    @Override
    public String ShortName() {
        return "Vid";
    }

    private void startRecording()
    {

        /*int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        MediaRecorder.setOrientationHint(orientation);*/
        Logger.d(TAG, "startRecording");
        startPreviewVideo();
    }

    private void stopRecording()
    {
        Logger.d(TAG, "stopRecording");
        mediaRecorder.stop();
        mediaRecorder.reset();
        baseCameraHolder.CaptureSessionH.RemoveSurface(recorderSurface);
        recorderSurface = null;
        isRecording = false;

        eventHandler.onRecorderstateChanged(I_RecorderStateChanged.STATUS_RECORDING_STOP);
        baseCameraHolder.CaptureSessionH.CreateCaptureSession();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void startPreview()
    {
        previewSize = new Size(currentVideoProfile.videoFrameWidth,currentVideoProfile.videoFrameHeight);
        baseCameraHolder.CaptureSessionH.SetTextureViewSize(previewSize.getWidth(), previewSize.getHeight(), 270,90,true);

        SurfaceTexture texture = baseCameraHolder.textureView.getSurfaceTexture();

        texture.setDefaultBufferSize(currentVideoProfile.videoFrameWidth,currentVideoProfile.videoFrameHeight);
        previewsurface = new Surface(texture);
       /* if (baseCameraHolder.mProcessor != null) {
            baseCameraHolder.mProcessor.kill();
        }*/
        baseCameraHolder.CaptureSessionH.AddSurface(previewsurface,true);


        baseCameraHolder.CaptureSessionH.CreateCaptureSession();

    }

    @Override
    public void stopPreview() {
        UnloadNeededParameters();
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startPreviewVideo()
    {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.reset();
        mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Logger.d(TAG, "error MediaRecorder:" + what + "extra:" + extra);
            }
        });

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(StringUtils.getFilePath(Settings.GetWriteExternal(), ".mp4"));

        mediaRecorder.setVideoEncodingBitRate(currentVideoProfile.videoBitRate);
        mediaRecorder.setVideoFrameRate(currentVideoProfile.videoFrameRate);
        mediaRecorder.setVideoSize(currentVideoProfile.videoFrameWidth, currentVideoProfile.videoFrameHeight);
        mediaRecorder.setVideoEncoder(currentVideoProfile.videoCodec);

        mediaRecorder.setAudioEncoder(currentVideoProfile.audioCodec);
        mediaRecorder.setAudioChannels(currentVideoProfile.audioChannels);
        mediaRecorder.setAudioEncodingBitRate(currentVideoProfile.audioBitRate);
        mediaRecorder.setAudioSamplingRate(currentVideoProfile.audioSampleRate);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage());
            eventHandler.onRecorderstateChanged(I_RecorderStateChanged.STATUS_RECORDING_STOP);
            return;
        }
        recorderSurface = mediaRecorder.getSurface();
        baseCameraHolder.CaptureSessionH.AddSurface(recorderSurface,true);

        baseCameraHolder.CaptureSessionH.CreateCaptureSession(previewrdy);
    }

    private CameraCaptureSession.StateCallback previewrdy = new CameraCaptureSession.StateCallback()
    {

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession)
        {
            baseCameraHolder.mCaptureSession = cameraCaptureSession;
            try {
                baseCameraHolder.mCaptureSession.setRepeatingRequest(baseCameraHolder.mPreviewRequestBuilder.build(),
                        baseCameraHolder.mCaptureCallback, null);
                //baseCameraHolder.SetLastUsedParameters(baseCameraHolder.mPreviewRequestBuilder);
                mediaRecorder.start();
                isRecording = true;
                eventHandler.onRecorderstateChanged(I_RecorderStateChanged.STATUS_RECORDING_START);
            } catch (CameraAccessException e)
            {
                baseCameraHolder.mCaptureSession = null;
                Logger.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession)
        {

        }
    };
}
