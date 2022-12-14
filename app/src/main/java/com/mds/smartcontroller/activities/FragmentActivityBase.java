package com.mds.smartcontroller.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.mds.smartcontroller.R;
import com.mds.smartcontroller.utils.PermissionRequest;

/**
 * Super class that handles dynamic Android permissions.
 */
public class FragmentActivityBase
       extends FragmentActivity {
    /**
     * Available for sub-classes to set with PermissionRequest#with() call.
     */
    protected PermissionRequest mPermissionRequest;

    /**
     * Handle the onPostCreate() hook to call permission helper to handle all
     * permission requests using the API 23 permission model framework.
     * <p>
     * The framework will callback to request this application to provide a
     * descriptive reason for the permission request that is then displayed to
     * the user. The user has the opportunity to grant or deny the permission
     * request. The callback is also handled automatically by the permission
     * helper class.
     *
     * @param savedInstanceState A saved state or null.
     */
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        // Submit a permission request to ensure that this app has the
        // required permissions for writing and reading external storage.
        mPermissionRequest = PermissionRequest
            .with(this)
            .permissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                         Manifest.permission.WRITE_EXTERNAL_STORAGE,
                         Manifest.permission.INTERNET)
            .rationale(R.string.permission_read_write_rationale)
            .granted(R.string.permission_read_write_granted)
            .denied(R.string.permission_read_write_denied)
            .snackbar(findViewById(android.R.id.content))
            .submit();

        // Always call super class method.
        super.onPostCreate(savedInstanceState);
    }

    /**
     * API 23 (M) callback received when a permissions request has been
     * completed. Redirect callback to permission helper.
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Redirect hook call to permission helper method.
        if (mPermissionRequest != null) {
            mPermissionRequest.onRequestPermissionsResult(requestCode,
                                                          permissions,
                                                          grantResults);
            mPermissionRequest = null; // request no longer needed
        }
    }
}
