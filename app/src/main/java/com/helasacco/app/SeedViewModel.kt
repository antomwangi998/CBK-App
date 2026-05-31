package com.helasacco.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helasacco.app.data.repository.AuthRepository
import com.helasacco.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeedViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            authRepository.createUser(
                username = "admin",
                password = "Admin@1234",
                role = UserRole.SUPER_ADMIN,
                fullName = "System Administrator",
                branchId = null,
            )
        }
    }
}
