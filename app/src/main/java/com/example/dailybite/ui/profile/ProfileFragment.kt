package com.example.dailybite.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.dailybite.databinding.FragmentProfileBinding
import com.example.dailybite.data.auth.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var authRepo: AuthRepository
    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                binding.ivProfile.setImageURI(selectedImageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = authRepo.currentUidOrNull() ?: return

        // טוען נתוני פרופיל
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.etName.setText(doc.getString("name") ?: "")
                    val imagePath = doc.getString("profileImagePath") ?: ""
                    if (imagePath.isNotEmpty()) {
                        storage.reference.child(imagePath).downloadUrl
                            .addOnSuccessListener { uri ->
                                Glide.with(this).load(uri).into(binding.ivProfile)
                            }
                    }
                }
            }

        // בחירת תמונה
        binding.ivProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        // שמירת שינויים
        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                saveProfile()
            }
        }
    }

    private suspend fun saveProfile() {
        val uid = authRepo.currentUidOrNull() ?: return
        val name = binding.etName.text.toString().trim()

        var imagePath = ""

        // העלאת תמונה אם נבחרה
        selectedImageUri?.let { uri ->
            imagePath = "profile_images/$uid.jpg"
            storage.reference.child(imagePath)
                .putFile(uri)
                .await()
        }

        // עדכון ב-Firestore
        val updates = mutableMapOf<String, Any>(
            "name" to name
        )
        if (imagePath.isNotEmpty()) {
            updates["profileImagePath"] = imagePath
        }

        firestore.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "עדכון הפרופיל נכשל", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}