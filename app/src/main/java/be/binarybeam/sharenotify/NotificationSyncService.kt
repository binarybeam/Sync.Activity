package be.binarybeam.sharenotify

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.File

class NotificationSyncService : NotificationListenerService() {
    private lateinit var androidId: String
    var i = 5
    private var lastMap: String = ""
    var packages = HashMap<String, String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val files = File(filesDir, "owner.txt")

        if (sbn!!.packageName == "android" || !sbn.isClearable || !files.exists() || sbn.packageName == packageName) return

        val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE)
        val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        val devi = files.readText()
        val map = HashMap<String, String>()

        map["title"] = title.toString()
        map["text"] = text.toString()
        map["app"] = sbn.packageName

        if (map.toString() == lastMap) return
        lastMap = map.toString()

        Firebase.database.reference.child("device").child(devi.substring(0, devi.indexOf(":")))
            .child("notify")
            .child(System.currentTimeMillis().toString()).setValue(map)
    }

    @SuppressLint("HardwareIds", "ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        val channelId = "notification_sync_channel"
        val channelId2 = "notification_from_apps"
        val channelId3 = "alerts"
        val channelName = "Notification Sync Service Channel"
        val channelName2 = "Notification from apps"
        val channelName3 = "Alerts and Updates"

        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).lowercase()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val channel2 = NotificationChannel(channelId2, channelName2, NotificationManager.IMPORTANCE_HIGH)
            val channel3 = NotificationChannel(channelId3, channelName3, NotificationManager.IMPORTANCE_HIGH)

            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel2)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel3)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("")
            .setContentText("")
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        }
        else startForeground(1, notificationBuilder.build())
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        Firebase.database.reference.child("device").child(androidId).child("notify")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (snapshot1 in snapshot.children) {
                            val packName = snapshot1.child("app").value.toString()
                            val drawable: Int
                            val appName: String

                            val pointFile = File(filesDir, "points.txt")
                            val avlPoints = if (pointFile.exists()) File(filesDir, "points.txt").readText().toInt() else 100
                            if (avlPoints > 0) {
                                if (i > 100) i = 5
                                if (packages.containsKey(packName)) {
                                    appName = packages[packName].toString().replace(":comma:", "") + "  ●  " + snapshot1.child("title").value.toString()
                                    drawable = if (packName == "com.android.chrome") R.drawable.chrome

                                    else if (packName == "com.facebook.katana") R.drawable.facebook
                                    else if (packName == "com.whatsapp") R.drawable.whatsapp
                                    else if (packName.contains("com.instagram.android")) R.drawable.instagram
                                    else if (packName.contains("amazon.mShop.android.shopping")) R.drawable.logo
                                    else if (packName.contains("com.snapchat.android")) R.drawable.snapchat
                                    else if (packName.contains("com.spotify.music")) R.drawable.spotify
                                    else if (packName.contains("org.telegram.messenger")) R.drawable.telegram
                                    else if (packName.contains("com.twitter.android")) R.drawable.twitter
                                    else if (packName.contains("com.whatsapp.w4b")) R.drawable.whatsapp
                                    else if (packName.contains("com.google.android.apps.messaging")) R.drawable.wechat
                                    else if (packName.contains("com.google.android.youtube")) R.drawable.youtube
                                    else if (packName.contains("com.google.android.gm")) R.drawable.gmail
                                    else R.mipmap.ic_launcher
                                }
                                else {
                                    appName = packName + "  ●  " + snapshot1.child("title").value.toString()
                                    drawable = R.mipmap.ic_launcher
                                }

                                File(filesDir, "points.txt").writeText((avlPoints - 1).toString())
                                val builder = NotificationCompat.Builder(applicationContext, channelId2)
                                    .setContentTitle(appName)
                                    .setWhen(snapshot1.key!!.toLong())
                                    .setSmallIcon(drawable)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setStyle(NotificationCompat.BigTextStyle().bigText(snapshot1.child("text").value.toString()))
                                    .setAutoCancel(true)

                                notificationManager.notify(i, builder.build())
                                i++
                            }
                        }
                        Firebase.database.reference.child("device").child(androidId).child("notify").removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        Firebase.database.reference.child("device").child(androidId).child("R")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("UnspecifiedImmutableFlag")
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.child("text").exists()) {
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                        val text = snapshot.child("text").value.toString()
                        val title: String = if (snapshot.child("title").exists()) snapshot.child("title").value.toString()
                        else getString(R.string.app_name)

                        val builder = NotificationCompat.Builder(applicationContext, channelId3)
                            .setContentTitle(title)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentText(text)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)

                        notificationManager.notify(2, builder.build())
                        Firebase.database.reference.child("device").child(androidId).child("R")
                            .removeValue()
                    }
                    else if (snapshot.exists()) {
                        val intent = Intent(applicationContext, PermissionActivity::class.java)
                        var runningStatus = false
                        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                        val packageName = applicationContext.packageName
                        val processInfos = activityManager.runningAppProcesses

                        for (processInfo in processInfos) {
                            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.processName == packageName) {
                                runningStatus = true
                                break
                            }
                        }
                        val device = snapshot.value.toString()
                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                        )

                        File(filesDir, "request.txt").writeText(device)
                        if (runningStatus) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        } else {
                            val builder = NotificationCompat.Builder(applicationContext, channelId3)
                                .setContentTitle("Request from " + device.substring(device.indexOf(":") + 1))
                                .setContentText("Grant the access to this device if you want to sync notification")
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setVibrate(longArrayOf(500L, 200L, 500L))

                            notificationManager.notify(3, builder.build())
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopSelf()
    }

    private fun updatePackages() {
        Firebase.database.reference.child("asset").child("package")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    packages.clear()
                    for (dataSnapshot in snapshot.children) {
                        packages[dataSnapshot.key!!.replace(":", ".")] = dataSnapshot.value.toString().replace(",", ":comma:")
                    }
                    File(filesDir, "package.txt").writeText(packages.toString().replace("{", "").replace("}", ""))
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}