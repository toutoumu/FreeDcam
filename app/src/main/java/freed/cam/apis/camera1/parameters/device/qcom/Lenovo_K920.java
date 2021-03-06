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

package freed.cam.apis.camera1.parameters.device.qcom;

import android.hardware.Camera.Parameters;

import freed.cam.apis.KEYS;
import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.parameters.manual.AbstractManualParameter;
import freed.cam.apis.basecamera.parameters.modes.MatrixChooserParameter;
import freed.cam.apis.camera1.parameters.device.BaseQcomDevice;
import freed.cam.apis.camera1.parameters.manual.focus.BaseFocusManual;
import freed.dng.DngProfile;

/**
 * Created by troop on 01.06.2016.
 */
public class Lenovo_K920 extends BaseQcomDevice {


    public Lenovo_K920(Parameters parameters, CameraWrapperInterface cameraUiWrapper) {
        super(parameters, cameraUiWrapper);
    }

    @Override
    public AbstractManualParameter getManualFocusParameter() {
        return new BaseFocusManual(parameters, KEYS.KEY_MANUAL_FOCUS_POSITION,KEYS.MAX_FOCUS_POS_INDEX, KEYS.MIN_FOCUS_POS_INDEX,KEYS.KEY_FOCUS_MODE_MANUAL, cameraUiWrapper,1,1);
    }
    @Override
    public boolean IsDngSupported() {
        return true;
    }
    @Override
    public DngProfile getDngProfile(int filesize)
    {
        switch (filesize)
        {
            case 19992576:  //lenovo k920
                return new DngProfile(64, 5328,3000, DngProfile.Mipi, DngProfile.GBRG, 0, matrixChooserParameter.GetCustomMatrix(MatrixChooserParameter.NEXUS6));
        }
        return null;
    }
}
