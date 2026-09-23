package com.example.strong_vault.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/** "All files access" (MANAGE_EXTERNAL_STORAGE) is required for direct, non-SAF read/write of the
 *  vault, which lives outside this app's own sandbox. It's a special permission Android requires
 *  to be granted from Settings, not via a normal runtime prompt. */
object StoragePermission {
    fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}
