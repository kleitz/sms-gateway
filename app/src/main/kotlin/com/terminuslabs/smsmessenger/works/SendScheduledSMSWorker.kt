package com.terminuslabs.smsmessenger.works

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.lang.Exception

class SendScheduledSMSWorker (appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        try{
            // Do the work here--in this case, upload the images.
            Tasks.sendScheduledMessages(applicationContext)

            // Indicate whether the work finished successfully with the Result
            return Result.success()

        }catch (e: Exception){
            Log.e("AppError", "Error", e)
            return Result.retry()
        }
    }
}
