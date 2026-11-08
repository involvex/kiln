package dev.involvex.kiln.feature.apps.export

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ApkExportFormat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class BatchExportScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun enqueue(treeUri: Uri, packageNames: List<String>, format: ApkExportFormat) {
        val request = OneTimeWorkRequestBuilder<BatchExportWorker>()
            .setInputData(
                workDataOf(
                    BatchExportWorker.KEY_TREE_URI to treeUri.toString(),
                    BatchExportWorker.KEY_PACKAGE_NAMES to packageNames.toTypedArray(),
                    BatchExportWorker.KEY_FORMAT to format.name,
                ),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(BatchExportWorker.WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(
            BatchExportWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(BatchExportWorker.WORK_NAME)
    }

    fun observeWork(): Flow<List<WorkInfo>> = workManager.getWorkInfosForUniqueWorkFlow(BatchExportWorker.WORK_NAME)
}
