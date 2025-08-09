package com.example.dailybite.ui.post

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.dailybite.databinding.FragmentEditPostBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class EditPostFragment : Fragment() {

    @javax.inject.Inject lateinit var storage: com.google.firebase.storage.FirebaseStorage
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val vm: EditPostViewModel by viewModels()

    private var newImageUri: Uri? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            newImageUri = uri
            binding.imgPreview.setImageURI(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = requireArguments().getString("postId")!!
        val imagePath = requireArguments().getString("imageStoragePath")!!
        // טוענים את התמונה הקיימת מ־Storage
        storage.reference.child(imagePath)
            .downloadUrl
            .addOnSuccessListener { uri ->
                binding.imgPreview.load(uri)
            }
        val mealTypeArg = requireArguments().getString("mealType") ?: "נשנוש"
        val descriptionArg = requireArguments().getString("description") ?: ""

        // ספינר סוגי ארוחה
        val meals = listOf("בוקר","צהריים","ערב","נשנוש")
        binding.spMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, meals)
        val idx = meals.indexOf(mealTypeArg).let { if (it >= 0) it else meals.lastIndex }
        binding.spMealType.setSelection(idx)

        // פרה־פילד של התיאור
        binding.etDescription.setText(descriptionArg)

        // טעינת תמונה קיימת
        // אם את משתמשת ב-Coil + Storage url, אפשר להביא כתובת הורדה כאן (אופציונלי).
        // כדי לשמור את זה פשוט, נשאיר את התמונה ריקה אם אין לנו URI מוכן.

        binding.btnReplaceImage.setOnClickListener {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSave.setOnClickListener {
            val meal = binding.spMealType.selectedItem?.toString() ?: mealTypeArg
            val desc = binding.etDescription.text?.toString()?.trim().orEmpty()
            if (desc.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "יש להזין תיאור", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bytes = newImageUri?.let { uri ->
                requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            vm.save(postId, meal, desc, imagePath, bytes)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                binding.progress.isVisible = s.loading
                binding.btnSave.isEnabled = !s.loading
                s.error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
                if (s.success) {
                    android.widget.Toast.makeText(requireContext(), "נשמר בהצלחה", android.widget.Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}