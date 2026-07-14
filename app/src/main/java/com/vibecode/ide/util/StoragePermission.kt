package com.vibecode.ide.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * On Android 11+ (API 30+), writing to shared/public storage paths like
 * /storage/emulated/0/VibeCodeProjects requires the "All files access"
 * special permission (MANAGE_EXTERNAL_STORAGE). Declaring it in the manifest
 * is NOT enough — the user must explicitly grant it via Settings, which this
 * helper opens. On API < 30, the classic runtime READ/WRITE_EXTERNAL_STORAGE
 * permissions are used instead.
 */
object StoragePermission {

    fun hasAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Legacy (API < 30) runtime permissions to request via ActivityResultContracts.RequestMultiplePermissions. */
    val legacyPermissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    /** Intent to open the system "All files access" screen for this app (API 30+). */
    fun manageAllFilesIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
}
