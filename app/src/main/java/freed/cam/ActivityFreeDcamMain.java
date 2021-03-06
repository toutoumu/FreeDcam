/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package freed.cam;


import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.KeyEvent;
import android.view.View;

import com.troop.freedcam.R;
import com.troop.freedcam.R.anim;
import com.troop.freedcam.R.id;
import com.troop.freedcam.R.layout;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import freed.ActivityAbstract;
import freed.cam.apis.ApiHandler;
import freed.cam.apis.ApiHandler.ApiEvent;
import freed.cam.apis.basecamera.CameraFragmentAbstract;
import freed.cam.apis.basecamera.CameraFragmentAbstract.CamerUiWrapperRdy;
import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.modules.I_WorkEvent;
import freed.cam.apis.basecamera.parameters.I_ParametersLoaded;
import freed.cam.ui.handler.I_orientation;
import freed.cam.ui.handler.OrientationHandler;
import freed.cam.ui.handler.TimerHandler;
import freed.cam.ui.themesample.PagingView;
import freed.cam.ui.themesample.cameraui.CameraUiFragment;
import freed.cam.ui.themesample.settings.SettingsMenuFragment;
import freed.utils.AppSettingsManager;
import freed.utils.FreeDPool;
import freed.utils.Logger;
import freed.utils.RenderScriptHandler;
import freed.utils.StringUtils;
import freed.viewer.holder.FileHolder;
import freed.viewer.screenslide.ScreenSlideFragment;

/**
 * Created by troop on 18.08.2014.
 */
public class ActivityFreeDcamMain extends ActivityAbstract implements I_orientation, CamerUiWrapperRdy, ApiEvent,I_ParametersLoaded
{
    private final String TAG =ActivityFreeDcamMain.class.getSimpleName();
    //listen to orientation changes
    private OrientationHandler orientationHandler;
    //handels/load the api camerafragments
    private ApiHandler apiHandler;
    private TimerHandler timerHandler;
    //holds the current api camerafragment
    private CameraFragmentAbstract cameraFragment;
    //hold the state if logging to file is true when folder /sdcard/DCIM/DEBUG/ is created
    private boolean savelogtofile;
    //holds the default UncaughtExecptionHandler from activity wich get replaced with own to have a change to save
    //fc to file and pass it back when done and let app crash as it should
    private UncaughtExceptionHandler defaultEXhandler;
    //private SampleThemeFragment sampleThemeFragment;
    private RenderScriptHandler renderScriptHandler;

    private PagingView mPager;
    private PagerAdapter mPagerAdapter;
    private CameraUiFragment cameraUiFragment;
    private SettingsMenuFragment settingsMenuFragment;
    private ScreenSlideFragment screenSlideFragment;

    private boolean activityIsResumed= false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(layout.freedcam_main_activity);

        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT)
            renderScriptHandler = new RenderScriptHandler(getApplicationContext());

        //load the camera ui
        mPager = (PagingView)findViewById(id.viewPager_fragmentHolder);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(1);

        //check for permission on M>
        if (VERSION.SDK_INT >= VERSION_CODES.M)
        {
            if (checkSelfPermission(permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED || checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                //ask for permissions and wait for onRequestPermissionsResult()
                requestPermissions(new String[]{
                        permission.CAMERA,
                        permission.READ_EXTERNAL_STORAGE,
                        permission.WRITE_EXTERNAL_STORAGE,
                        permission.RECORD_AUDIO,
                        permission.ACCESS_COARSE_LOCATION,
                        permission.ACCESS_FINE_LOCATION,
                        permission.ACCESS_WIFI_STATE,
                        permission.CHANGE_WIFI_STATE,}, 1);
            }
            else
                createHandlers();
        }
        else
            createHandlers();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (savelogtofile) {
            Logger.StopLogging();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraFragment != null && orientationHandler != null)
            orientationHandler.Start();
        activityIsResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(orientationHandler != null)
            orientationHandler.Stop();
        activityIsResumed = false;
    }

    //gets called when permission was request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        //we have permissions?
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
                && grantResults[3] == PackageManager.PERMISSION_GRANTED
                && grantResults[4] == PackageManager.PERMISSION_GRANTED
                && grantResults[5] == PackageManager.PERMISSION_GRANTED
                && grantResults[6] == PackageManager.PERMISSION_GRANTED
                && grantResults[7] == PackageManager.PERMISSION_GRANTED)
        {
            //yes we have it
            createHandlers();
            //when screenslide fragment gets created it looks up already the files bevor permissions are granted.
            //if its the first start the viewer show "no files". to avoid that, load files again when permissions are set
            screenSlideFragment.LoadFiles();
        }
        else //woot using camera and deny perms close app
            finish();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //that finaly create all stuff needed
    private void createHandlers()
    {
        Logger.d(TAG, "createHandlers()");
        //Get default handler for uncaught exceptions. to let fc app as it should
        defaultEXhandler = Thread.getDefaultUncaughtExceptionHandler();
        //set up own ex handler to have a change to catch the fc bevor app dies
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread,final Throwable e)
            {
                //yeahaw app crash print ex to logger
                if (thread != Looper.getMainLooper().getThread())
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            Logger.LogUncaughtEX(e);
                        }
                    });
                else
                    Logger.LogUncaughtEX(e);

                //set back default exhandler and let app die
                defaultEXhandler.uncaughtException(thread,e);
            }
        });

        //check if DEBUG folder exist for log to file
        checkSaveLogToFile();
        //listen to phone orientation changes
        orientationHandler = new OrientationHandler(this, this);
        //used for videorecording timer
        //TODO move that into camerauifragment
        timerHandler = new TimerHandler(this);
        //setup apihandler and register listner for apiDetectionDone
        //api handler itself checks first if its a camera2 full device
        //and if yes loads camera2fragment else load camera1fragment
        apiHandler = new ApiHandler(getApplicationContext(),this, appSettingsManager, renderScriptHandler);
        //check if camera is camera2 full device
        apiHandler.CheckApi();
    }

    //if a DEBUG folder is inside /DCIM/FreeDcam/ logging to file gets started
    private void checkSaveLogToFile()
    {
        File debugfile = new File(StringUtils.GetInternalSDCARD() + StringUtils.freedcamFolder +"DEBUG");
        if (debugfile.exists()) {
            savelogtofile = true;
            Logger.StartLogging();
        }
    }

    /**
     * gets called from ApiHandler when apidetection has finished
     * thats loads the CameraFragment
     */
    @Override
    public void apiDetectionDone()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadCameraFragment();

            }
        });

    }

    /*
    load the camerafragment to ui
     */
    private void loadCameraFragment()
    {
        Logger.d(TAG, "loading cameraWrapper");
        //if a camera fragment exists stop and destroy it
        unloadCameraFragment();
        //get new cameraFragment
        cameraFragment = apiHandler.getCameraFragment();
        cameraFragment.Init(this);
        //load the cameraFragment to ui
        //that starts the camera represent by that fragment when the surface/textureviews
        //are created and calls then onCameraUiWrapperRdy(I_CameraUiWrapper cameraUiWrapper)
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(anim.left_to_right_enter, anim.left_to_right_exit);
        transaction.add(id.cameraFragmentHolder, cameraFragment, "CameraFragment");
        transaction.commitAllowingStateLoss();
        Logger.d(TAG, "loaded cameraWrapper");

    }

    /**
     * gets thrown when the cameraFragment is created sucessfull and all items are up like modulehandler
     * and rdy to register listners
     * @param cameraUiWrapper the cameraWrapper to register the listners
     */
    @Override
    public void onCameraUiWrapperRdy(CameraWrapperInterface cameraUiWrapper)
    {
        //set orientatiohandler to module handler that it knows when a work is in progress
        orientationHandler.Start();
        //to avoid that orientation gets set while working
        cameraUiWrapper.GetModuleHandler().SetWorkListner(orientationHandler);
        //note the ui that cameraFragment is loaded
        if (cameraUiWrapper != null)
            cameraUiWrapper.GetParameterHandler().AddParametersLoadedListner(this);
        if (cameraUiWrapper.GetModuleHandler() != null)
            cameraUiWrapper.GetModuleHandler().AddWorkFinishedListner(newImageRecieved);
        if (cameraUiFragment != null) {
            cameraUiFragment.SetCameraUIWrapper(cameraUiWrapper);
        }
        if (settingsMenuFragment != null)
            settingsMenuFragment.SetCameraUIWrapper(cameraUiWrapper);
        Logger.d(TAG, "add events");
        //register timer to to moduleevent handler that it get shown/hidden when its video or not
        //and start/stop working when recording starts/stops
        cameraUiWrapper.GetModuleHandler().AddRecoderChangedListner(timerHandler);
        cameraUiWrapper.GetModuleHandler().addListner(timerHandler);
    }

    /**
     * Unload the current active camerafragment
     */
    private void unloadCameraFragment()
    {
        Logger.d(TAG, "destroying cameraWrapper");
        if(orientationHandler != null)
            orientationHandler.Stop();

        if (cameraFragment != null)
        {
            //kill the cam befor the fragment gets removed to make sure when
            //new cameraFragment gets created and its texture view is created the cam get started
            //when its done in textureview/surfaceview destroy method its already to late and we get a security ex lack of privilege
            if (cameraFragment.GetCameraUiWrapper() != null)
                cameraFragment.GetCameraUiWrapper().StopCamera();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(anim.right_to_left_enter, anim.right_to_left_exit);
            transaction.remove(cameraFragment);
            transaction.commitAllowingStateLoss();
            cameraFragment = null;
        }
        Logger.d(TAG, "destroyed cameraWrapper");
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (activityIsResumed) {
            int appSettingsKeyShutter = 0;

            if (appSettingsManager.getString(AppSettingsManager.SETTING_EXTERNALSHUTTER).equals(StringUtils.VoLP))
                appSettingsKeyShutter = KeyEvent.KEYCODE_VOLUME_UP;
            if (appSettingsManager.getString(AppSettingsManager.SETTING_EXTERNALSHUTTER).equals(StringUtils.VoLM))
                appSettingsKeyShutter = KeyEvent.KEYCODE_VOLUME_DOWN;
            if (appSettingsManager.getString(AppSettingsManager.SETTING_EXTERNALSHUTTER).equals(StringUtils.Hook) || appSettingsManager.getString(AppSettingsManager.SETTING_EXTERNALSHUTTER).equals(""))
                appSettingsKeyShutter = KeyEvent.KEYCODE_HEADSETHOOK;

            if (keyCode == KeyEvent.KEYCODE_3D_MODE || keyCode == KeyEvent.KEYCODE_POWER || keyCode == appSettingsKeyShutter || keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                Logger.d(TAG, "KeyUp");
                cameraFragment.GetModuleHandler().DoWork();
            }
            //shutterbutton full pressed
            if (keyCode == KeyEvent.KEYCODE_CAMERA) {
                cameraFragment.GetModuleHandler().DoWork();
            }
            // shutterbutton half pressed
       /* if (keyCode == KeyEvent.KEYCODE_FOCUS)
            cameraUiWrapper.StartFocus();*/
            return true;
        }
        else
            return super.onKeyDown(keyCode,event);
    }


    private int currentorientation;


    /**
     * Set the orientaion to the current camerafragment
     * @param orientation the new phone orientation
     */
    @Override
    public void OrientationChanged(int orientation)
    {
        if (orientation != currentorientation)
        {
            currentorientation = orientation;
            if (cameraFragment.GetCameraUiWrapper() != null && cameraFragment.GetCameraUiWrapper().GetCameraHolder() != null && cameraFragment.GetCameraUiWrapper().GetParameterHandler() != null)
                cameraFragment.GetCameraUiWrapper().GetParameterHandler().SetPictureOrientation(orientation);
        }
    }


    @Override
    public void SwitchCameraAPI(String value)
    {
        loadCameraFragment();
    }

    @Override
    public void closeActivity()
    {
        moveTaskToBack(true);
    }

    /**
     * Loads all files stored in DCIM/FreeDcam from internal and external SD
     * and notfiy the stored screenslide fragment in sampletheme that
     * files got changed
     */
    @Override
    public void LoadFreeDcamDCIMDirsFiles() {
        super.LoadFreeDcamDCIMDirsFiles();
        screenSlideFragment.NotifyDATAhasChanged();
    }

    @Override
    public void DisablePagerTouch(boolean disable)
    {
        if (disable)
            mPager.EnableScroll(false);
        else
            mPager.EnableScroll(true);
    }


    /**
     * Loads the files stored from that folder
     * and notfiy the stored screenslide fragment in sampletheme that
     * files got changed
     * @param fileHolder the folder to lookup
     * @param types the file format to load
     */
    @Override
    public void LoadFolder(FileHolder fileHolder, FormatTypes types) {
        super.LoadFolder(fileHolder, types);
        screenSlideFragment.NotifyDATAhasChanged();
    }


    private final ScreenSlideFragment.I_ThumbClick onThumbClick = new ScreenSlideFragment.I_ThumbClick() {
        @Override
        public void onThumbClick(int position,View view) {
            mPager.setCurrentItem(2);
        }
    };

    private final ScreenSlideFragment.I_ThumbClick onThumbBackClick = new ScreenSlideFragment.I_ThumbClick() {
        @Override
        public void onThumbClick(int position,View view)
        {
            if (mPager != null)
                mPager.setCurrentItem(1);
        }

    };


    @Override
    public void ParametersLoaded(CameraWrapperInterface cameraWrapper) {
        if (cameraUiFragment != null) {
            cameraUiFragment.SetCameraUIWrapper(cameraWrapper);
        }
        if (settingsMenuFragment != null)
            settingsMenuFragment.SetCameraUIWrapper(cameraWrapper);
        if (screenSlideFragment != null)
            screenSlideFragment.LoadFiles();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter
    {

        public ScreenSlidePagerAdapter(FragmentManager fm)
        {
            super(fm);

        }

        @Override
        public Fragment getItem(int position)
        {
            if (position == 0)
            {
                if (settingsMenuFragment == null)
                {
                    settingsMenuFragment = new SettingsMenuFragment();
                    settingsMenuFragment.SetCameraUIWrapper(cameraFragment);
                }
                return settingsMenuFragment;
            }
            else if (position == 2)
            {
                if (screenSlideFragment == null) {
                    screenSlideFragment = new ScreenSlideFragment();
                    screenSlideFragment.setWaitForCameraHasLoaded();
                    screenSlideFragment.SetOnThumbClick(onThumbBackClick);
                }

                return screenSlideFragment;
            }
            else
            {
                if (cameraUiFragment == null)
                    cameraUiFragment = CameraUiFragment.GetInstance(onThumbClick,cameraFragment);
                return cameraUiFragment;
            }
        }

        @Override
        public int getCount()
        {
            return 3;
        }

    }


    private final I_WorkEvent newImageRecieved = new I_WorkEvent()
    {
        @Override
        public void WorkHasFinished(final FileHolder fileHolder)
        {
            Logger.d(TAG, "newImageRecieved:" + fileHolder.getFile().getAbsolutePath());
            FreeDPool.Execute(new Runnable() {
                @Override
                public void run()
                {
                    int mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnails_size);
                    final Bitmap b = getBitmapHelper().getBitmap(fileHolder.getFile(), true, mImageThumbSize, mImageThumbSize);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            AddFile(fileHolder);
                            if (screenSlideFragment != null)
                                screenSlideFragment.NotifyDATAhasChanged();
                            if (cameraUiFragment != null)
                                cameraUiFragment.SetThumbImage(b);
                        }
                    });
                }
            });

        }
    };
}
