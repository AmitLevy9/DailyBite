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
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.dailybite.R
import com.example.dailybite.databinding.FragmentEditPostBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class EditPostFragment : Fragment() {

    @javax.inject.Inject lateinit var storage: com.google.firebase.storage.FirebaseStorage
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val vm: EditPostViewModel by viewModels()
    private val args: EditPostFragmentArgs by navArgs()

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

        // טעינת תמונה קיימת
        storage.reference.child(args.imageStoragePath)
            .downloadUrl
            .addOnSuccessListener { uri -> binding.imgPreview.load(uri) }

        // ספינר סוגי ארוחה
        val meals = resources.getStringArray(R.array.meal_types).toList().ifEmpty {
            listOf("בוקר","צהריים","ערב","נשנוש")
        }
        binding.spMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, meals)

        val idx = meals.indexOf(args.mealType).let { if (it >= 0) it else 0 }
        binding.spMealType.setSelection(idx)

        // תיאור קיים
        binding.etDescription.setText(args.description)

        // החלפת תמונה
        binding.btnReplaceImage.setOnClickListener {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // שמירה
        binding.btnSave.setOnClickListener {
            val meal = binding.spMealType.selectedItem?.toString() ?: args.mealType
            val desc = binding.etDescription.text?.toString()?.trim().orEmpty()
            if (desc.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "יש להזין תיאור", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                binding.progress.isVisible = true
                val bytes: ByteArray? = withContext(Dispatchers.IO) {
                    newImageUri?.let { uri ->
                        requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                }
                vm.save(
                    postId = args.postId,
                    mealType = meal,
                    description = desc,
                    imageStoragePath = args.imageStoragePath,
                    newImageBytes = bytes
                )
            }
        }

        // תצפית על state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                binding.progress.isVisible = s.loading
                binding.btnSave.isEnabled = !s.loading
                binding.btnReplaceImage.isEnabled = !s.loading
                binding.spMealType.isEnabled = !s.loading
                binding.etDescription.isEnabled = !s.loading

                s.error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                    // אם הוספת vm.consumeError() ב־VM, קראי לו כאן
                }
                if (s.success) {
                    android.widget.Toast.makeText(requireContext(), "נשמר בהצלחה", android.widget.Toast.LENGTH_SHORT).show()
                    // אם הוספת vm.consumeSuccess() ב־VM, קראי לו כאן
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