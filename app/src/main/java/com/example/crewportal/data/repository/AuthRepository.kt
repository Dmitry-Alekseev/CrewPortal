package com.example.crewportal.data.repository

class AuthRepository(private val preferencesRepository: PreferencesRepository) {
    private val corporateLogin = "CPD9842"
    private val corporatePassword = "Airbus1998"

    fun validate(login: String, password: String): Boolean {
        return login.trim().equals(corporateLogin, ignoreCase = false) && password == corporatePassword
    }

    suspend fun signIn(login: String, rememberMe: Boolean) {
        preferencesRepository.setLoginState(loggedIn = true, remember = rememberMe, login = login.trim())
    }

    suspend fun signOut() = preferencesRepository.logout()
}
