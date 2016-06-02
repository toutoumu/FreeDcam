package com.freedcam.apis.camera1.camera.parameters.device;

import android.hardware.Camera;
import android.os.Handler;

import com.freedcam.apis.basecamera.camera.parameters.manual.AbstractManualParameter;
import com.freedcam.apis.basecamera.camera.parameters.modes.MatrixChooserParameter;
import com.freedcam.apis.camera1.camera.CameraHolderApi1;
import com.freedcam.apis.camera1.camera.CameraUiWrapper;
import com.freedcam.apis.camera1.camera.parameters.CamParametersHandler;
import com.freedcam.apis.camera1.camera.parameters.KEYS;
import com.freedcam.apis.camera1.camera.parameters.manual.BaseFocusManual;
import com.troop.androiddng.DngProfile;

/**
 * Created by troop on 01.06.2016.
 */
public class Lenovo_K920 extends BaseQcomDevice {
    public Lenovo_K920(Handler uihandler, Camera.Parameters parameters, CameraUiWrapper cameraUiWrapper) {
        super(uihandler, parameters, cameraUiWrapper);
    }

    @Override
    public AbstractManualParameter getManualFocusParameter() {
        return new BaseFocusManual(parameters, KEYS.KEY_MANUAL_FOCUS_POSITION,KEYS.KEY_MAX_FOCUS_VCM_INDEX, KEYS.KEY_MIN_FOCUS_VCM_INDEX,KEYS.KEY_FOCUS_MODE_MANUAL,camParametersHandler,1,1);
    }

    @Override
    public DngProfile getDngProfile(int filesize)
    {
        switch (filesize)
        {
            case 19992576:  //lenovo k920
                return new DngProfile(64, 5328,3000, DngProfile.Mipi, DngProfile.GBRG, 0,matrixChooserParameter.GetCustomMatrix(MatrixChooserParameter.NEXUS6));
        }
        return null;
    }
}
