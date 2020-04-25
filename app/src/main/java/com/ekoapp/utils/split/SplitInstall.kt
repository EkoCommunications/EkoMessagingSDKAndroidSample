package com.ekoapp.utils.split

import android.content.Context
import com.ekoapp.sample.utils.*
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import timber.log.Timber

data class InstallModuleData(val module: String, val isInstall: Boolean = false)

sealed class InstallModuleSealed {
    class NotInstall(val data: InstallModuleData) : InstallModuleSealed()
    class Installed(val data: InstallModuleData) : InstallModuleSealed()
}

class SplitInstall(val context: Context,
                   private val installManager: SplitInstallManager,
                   private val installRequest: SplitInstallRequest) {

    private val modules = listOf(CHAT_DYNAMIC_FEATURE, SOCIAL_DYNAMIC_FEATURE)

    fun installModule(type: (InstallModuleSealed) -> Unit) {
        modules.forEach {
            type.invoke(InstallModuleData(module = it).getInstallModuleType())
        }
    }

    private fun InstallModuleData.getInstallModuleType(): InstallModuleSealed {
        installManager.installedModules.contains(module).let { isInstall ->
            if (!isInstall) {
                startInstall(module) {
                    return@startInstall InstallModuleSealed.NotInstall(InstallModuleData(module, it.isInstall))
                }
            } else return InstallModuleSealed.Installed(InstallModuleData(module, DOWNLOAD_SUCCESS))
        }

        return InstallModuleSealed.NotInstall(InstallModuleData(module, DOWNLOAD_FAILED))
    }

    private fun startInstall(module: String, result: (InstallModuleData) -> InstallModuleSealed) {
        installManager.startInstall(installRequest)
                .addOnFailureListener {
                    Timber.d("${getCurrentClassAndMethodNames()} addOnFailureListener: ${it.message}")
                    result.invoke(InstallModuleData(module = module, isInstall = DOWNLOAD_FAILED))
                }
                .addOnSuccessListener {
                    result.invoke(InstallModuleData(module = module, isInstall = DOWNLOAD_SUCCESS))
                }
                .addOnCompleteListener {
                    result.invoke(InstallModuleData(module = module, isInstall = it.isComplete))
                }
    }

}