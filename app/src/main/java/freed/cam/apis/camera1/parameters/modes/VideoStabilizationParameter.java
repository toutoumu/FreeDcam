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

package freed.cam.apis.camera1.parameters.modes;

import android.hardware.Camera.Parameters;

import freed.cam.apis.KEYS;
import freed.cam.apis.basecamera.CameraWrapperInterface;

/**
 * Created by Ar4eR on 10.12.15.
 */
public class VideoStabilizationParameter extends  BaseModeParameter {
    private final String[] vs_values = {KEYS.TRUE, KEYS.FALSE};
    public VideoStabilizationParameter(Parameters parameters, CameraWrapperInterface parameterChanged)
    {
        super(parameters, parameterChanged);
        if (parameters.get(KEYS.VIDEO_STABILIZATION_SUPPORTED).equals(KEYS.TRUE))
        {
            isSupported = true;
            key_value = KEYS.VIDEO_STABILIZATION;
        }
        else
            isSupported = false;

    }

    @Override
    public boolean IsSupported() {
        return isSupported;
    }

    @Override
    public String[] GetValues() {
        return vs_values;
    }

    @Override
    public String GetValue()
    {
        String vs = parameters.get(KEYS.VIDEO_STABILIZATION);
        if (vs != null && !vs.equals(""))
            return vs;
        else
            return "error";
    }

}