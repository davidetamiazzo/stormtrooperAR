/*
* This class overwrites the ArFragment to allow the writing in external storage to take pictures
*/
package com.google.ar.sceneform.samples.hellosceneform;

import android.Manifest;

import com.google.ar.sceneform.ux.ArFragment;

public class WritingArFragment extends ArFragment {

    @Override
    public String[] getAdditionalPermissions() {
        String[] additionalPermissions = super.getAdditionalPermissions();
        int permissionLength = additionalPermissions != null ? additionalPermissions.length : 0;
        String[] permissions = new String[permissionLength + 1];
        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (permissionLength > 0) {
            System.arraycopy(additionalPermissions, 0, permissions, 1, additionalPermissions.length);
        }
        return permissions;
    }
}