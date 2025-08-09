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
import com.example.dailybite.databinding.FragmentNewPostBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class NewPostFragment : Fragment() {
    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!
    private val vm: NewPostViewModel by viewModels()

    private var selectedImage: Uri? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImage = uri
            binding.imgPreview.setImageURI(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val meals = listOf("בוקר","צהריים","ערב","נשנוש")
        binding.spMealType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, meals)

        binding.btnPickImage.setOnClickListener {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnPublish.setOnClickListener {
            val uri = selectedImage ?: return@setOnClickListener
            val mealType = binding.spMealType.selectedItem?.toString() ?: "נשנוש"
            val desc = binding.etDescription.text?.toString()?.trim().orEmpty()
            val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                vm.publish(mealType, desc, bytes)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}