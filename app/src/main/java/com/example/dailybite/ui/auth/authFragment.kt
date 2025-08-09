package com.example.dailybite.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.dailybite.R
import com.example.dailybite.databinding.FragmentAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val vm: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // אם כבר מחוברים - נווט ישר לפיד
        vm.checkLoggedIn()

        binding.btnContinue.setOnClickListener {
            vm.signInAnon()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                binding.btnContinue.isEnabled = !s.loading
                if (s.loggedIn) {
                    findNavController().navigate(R.id.action_auth_to_feed)
                }
                // אפשר להציג s.error למשתמש לפי הצורך
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}