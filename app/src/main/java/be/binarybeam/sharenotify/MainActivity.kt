package be.binarybeam.sharenotify

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import be.binarybeam.sharenotify.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(filesDir, "accessId.txt")
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).lowercase()

        if (packageName != "be.binarybeam.sharenotify") Toast.makeText(this, "You need permit to mod (patch) this app", Toast.LENGTH_SHORT).show()
        else if (file.exists()) {
            startActivity(Intent(applicationContext, PermissionActivity::class.java))
            finish()
        }
        else {
            binding.cardView1.setOnClickListener {
                if (binding.anim1.isVisible) return@setOnClickListener

                binding.anim1.visibility = View.VISIBLE
                binding.textView1.visibility = View.GONE

                Firebase.database.reference.child("device/$androidId/detail").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.child("C").exists()) File(filesDir, "owner.txt").writeText(snapshot.child("C").value.toString())
                            file.writeText(androidId)
                        }

                        startActivity(Intent(applicationContext, PermissionActivity::class.java))
                        finish()
                    }
                    override fun onCancelled(error: DatabaseError) { }
                })
            }
        }
    }
}