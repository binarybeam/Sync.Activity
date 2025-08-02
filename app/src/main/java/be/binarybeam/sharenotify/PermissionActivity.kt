@file:Suppress("DEPRECATION")

package be.binarybeam.sharenotify

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.provider.Settings
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import be.binarybeam.sharenotify.databinding.ActivityPermissionBinding
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File
import java.text.SimpleDateFormat

class PermissionActivity : AppCompatActivity() {
    private lateinit var id: ActivityPermissionBinding
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private lateinit var shaker: Vibrator
    private var androidId = ""

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = ActivityPermissionBinding.inflate(layoutInflater)
        shaker = getSystemService(VIBRATOR_SERVICE) as Vibrator
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).lowercase()

        setContentView(id.root)
        clickers()
        editListeners()

        val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val accessFile = File(filesDir, "accessId.txt")

        updateGenerator(accessFile)
        MobileAds.initialize(this) { }

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, 38)
        }

        id.accessId.setOnClickListener {
            if (id.cardView5.isVisible) return@setOnClickListener
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", androidId)

            clipboardManager.setPrimaryClip(clipData)
            vibrate()

            show(id.imageView)
            show(id.cardView5)

            Handler().postDelayed({
                hide(id.imageView)
                hide(id.cardView5)
            }, 1500)
        }

        Firebase.database.reference.child("device/$androidId/request").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    show(id.request)

                    id.positiveNoteBtn.setOnClickListener { deleteRequest("accept", snapshot.value.toString()) }
                    id.negativeNoteBtn.setOnClickListener { deleteRequest("reject", snapshot.value.toString()) }
                }
                else if (id.request.isVisible) hide(id.request)
            }
            override fun onCancelled(error: DatabaseError) { }
        })

        id.blurView.setupWith(rootView, RenderScriptBlur(this)).setBlurRadius(25f)
        id.useBlur.setupWith(rootView, RenderScriptBlur(this)).setBlurRadius(25f)
        id.pointBlur.setupWith(rootView, RenderScriptBlur(this)).setBlurRadius(25f)

        id.blurView.setOnClickListener {
            vibrate()
            hide(id.blurView)
        }

        id.pointBlur.setOnClickListener {
            vibrate()
            hide(id.pointBlur)
        }

        id.understood.setOnClickListener {
            vibrate()
            hide(id.useBlur)
        }

        id.backPress.setOnClickListener { finish() }
        id.request.setOnClickListener { }
    }

    private fun deleteRequest(status: String, user: String) {
        hide(id.request)
        vibrate()
        Firebase.database.reference.child("device/$androidId/request").removeValue()

        if (status == "accept") {
            Firebase.database.reference.child("device/$androidId/detail/C").setValue(user)
            File(filesDir, "owner.txt").writeText(user)
        }
    }

    private fun editListeners() {
        id.accessIdInput.addTextChangedListener {
            if (it.toString().isEmpty()) id.submitCard.setCardBackgroundColor(getColor(R.color.fg_toggle_dark))
            else id.submitCard.setCardBackgroundColor(getColor(R.color.blue))

            if (id.finalSubmit.isVisible) {
                hide(id.finalSubmit)
                hide(id.details)
            }
        }
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun clickers() {
        id.submitCard.setOnClickListener {
            val input = id.accessIdInput.text.toString()
            if (id.submitLoader.isVisible || input.isEmpty()) return@setOnClickListener
            id.accessIdInput.isEnabled = false

            vibrate()
            show(id.submitLoader)
            hide(id.submitArrow)

            Firebase.database.reference.child("device/$input/detail").get().addOnSuccessListener {
                if (it.exists()) {
                    val deviceName = it.child("N").value.toString()
                    val connection = if (it.child("C").exists()) it.child("C").value.toString() else ""
                    val time = it.child("T").value.toString().toLong()

                    show(id.details)
                    show(id.submitArrow)
                    show(id.finalSubmit)

                    id.deviceName.text = deviceName
                    id.time.text = SimpleDateFormat("dd MMM yyyy").format(time)
                    if (connection.isEmpty()) id.status.text = "Not connected" else { id.status.text = "Connected" }

                    id.finalSubmit.setOnClickListener {
                        vibrate()
                        id.accessIdInput.isEnabled = false

                        show(id.submitLoader)
                        hide(id.finalSubmit)
                        hide(id.submitArrow)

                        Firebase.database.reference.child("device/$input/request").setValue("$androidId:${Build.BRAND} ${Build.MODEL}").addOnSuccessListener {
                            hide(id.submitLoader)
                            hide(id.finalSubmit)
                            show(id.submitArrow)
                            hide(id.details)

                            id.accessIdInput.isEnabled = true
                            id.accessIdInput.setText("")
                            Toast.makeText(applicationContext, "Request sent.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else {
                    show(id.submitArrow)
                    Toast.makeText(applicationContext, "No device found.", Toast.LENGTH_SHORT).show()
                }

                vibrate()
                hide(id.submitLoader)
                id.accessIdInput.isEnabled = true
            }
        }

        id.idGGenerator.setOnClickListener {
            vibrate()
            val accessFile = File(filesDir, "accessId.txt")

            if (try { accessFile.readText() } catch (e: Exception) { "" }.isEmpty()) {
                show(id.idGenLoader)
                id.idGGenerator.visibility = View.INVISIBLE

                Firebase.database.reference.child("device/$androidId/detail").setValue(
                    hashMapOf(
                        "N" to "${Build.BRAND} ${Build.MODEL}",
                        "T" to System.currentTimeMillis()
                    )
                ).addOnSuccessListener {
                    show(id.blurView)
                    accessFile.writeText(androidId)
                    updateGenerator(accessFile)
                }
            }
            else show(id.blurView)
        }

        id.useBtn.setOnClickListener {
            vibrate()
            show(id.useBlur)
        }

        id.pointsBtn.setOnClickListener {
            vibrate()
            show(id.pointBlur)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateGenerator(accessFile: File) {
        val accessId = try { accessFile.readText() } catch (e: Exception) { "" }
        if (accessId.isEmpty()) id.idGGenerator.text = "GENERATE ACCESS ID"
        else id.idGGenerator.text = "GET ACCESS ID"
        id.accessId.text = accessId
    }

    private fun isNotificationEnabled() : Boolean {
        val notificationListenerComponent = ComponentName(packageName, NotificationSyncService::class.java.name)
        val isNotificationListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(notificationListenerComponent.packageName)
        return isNotificationListenerEnabled
    }

    private fun requestNotificationAccess() {
        val notificationListenerSettingsIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(notificationListenerSettingsIntent)
    }

    private fun isPostNotificationPermissionEnabled() : Boolean {
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return false }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
        }
    }

    private fun loadAd() {
        RewardedInterstitialAd.load(this, "ca-app-pub-6502185715658294/8048991425", AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                rewardedInterstitialAd = ad
                rewardedInterstitialAd?.show(this@PermissionActivity) { }

                rewardedInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdClicked() { }

                    override fun onAdDismissedFullScreenContent() {
                        rewardedInterstitialAd = null
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        rewardedInterstitialAd = null
                        Toast.makeText(applicationContext, adError.message, Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdImpression() { }

                    override fun onAdShowedFullScreenContent() { }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedInterstitialAd = null
                Toast.makeText(applicationContext, adError.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun vibrate() { shaker.vibrate(25) }

    private fun show(view: View) {
        view.visibility = View.VISIBLE
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
    }

    private fun hide(view: View) {
        view.visibility = View.GONE
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out))
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (id.blurView.isVisible) hide(id.blurView)
        else if (id.useBlur.isVisible) hide(id.useBlur)
        else if (id.pointBlur.isVisible) hide(id.pointBlur)
        else if (!id.request.isVisible) super.onBackPressed()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showPrompt(title: String, description: String, positive: String, negative: String, accent: Any, icon: Any) {
        show(id.request)
        vibrate()

        if (negative.isEmpty()) {
            id.negativeNoteBtn.visibility = View.GONE
            id.midDivider.visibility = View.GONE
        }
        else {
            show(id.negativeNoteBtn)
            show(id.midDivider)
            id.negativeNoteBtn.text = negative
        }

        if (positive.isEmpty()) id.positiveNoteBtn.visibility = View.GONE
        else {
            id.positiveNoteBtn.visibility = View.VISIBLE
            id.positiveNoteBtn.text = positive
        }

        if (title.isNotEmpty()) {
            id.headNote.visibility = View.VISIBLE
            id.headNote.text = Html.fromHtml(title)
        }
        else id.headNote.visibility = View.GONE

        if (description.isNotEmpty()) {
            id.bioNote.visibility = View.VISIBLE
            id.bioNote.text = Html.fromHtml(description)
        }
        else id.bioNote.visibility = View.GONE

        if (accent.toString().isNotEmpty() && accent.toString().isDigitsOnly()) {
            id.positiveNoteBtn.setTextColor(getColor(accent as Int))
            id.noteIcon.setColorFilter(getColor(accent))
        }
        else {
            try {
                id.positiveNoteBtn.setTextColor(Color.parseColor(accent.toString()))
                id.noteIcon.setColorFilter(Color.parseColor(accent.toString()))
            }
            catch (e: Exception) {
                id.positiveNoteBtn.setTextColor(getColor(R.color.blue))
                id.noteIcon.setColorFilter(getColor(R.color.blue))
            }
        }

        if (icon.toString().isEmpty()) id.noteIcon.visibility = View.GONE
        else {
            id.noteIcon.visibility = View.VISIBLE
            if (icon.toString().isDigitsOnly()) id.noteIcon.setImageDrawable(getDrawable(icon as Int))
            else Glide.with(applicationContext).load(icon).placeholder(R.drawable.logo).into(id.noteIcon)
        }
    }
}