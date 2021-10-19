package com.example.keyboardevents.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.keyboardevents.databinding.ActivityLoginBinding

import com.example.keyboardevents.R

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }

        val rootView = window.decorView.rootView
        val isKeyBoardVisible = isKeyboardShown(rootView)
        Log.d("KeyBoardVisibility", "initial keyboard state is: $isKeyBoardVisible for view: $rootView")
        ViewCompat.setOnApplyWindowInsetsListener(rootView, WindowInsetListener(isKeyBoardVisible))
    }

    private fun isKeyboardShown(rootView: View?): Boolean {
        if (rootView == null) {
            return false
        }
        //rootView.context.applicationContext.wi
        val insets = ViewCompat.getRootWindowInsets(rootView)
        if (insets == null) {
            Log.i("KeyBoardVisibility", "insets is null")
        }
        return insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    /**
     * Works api 30, 31
     * Works api 25 in portrait with android:windowSoftInputMode="adjustResize"
     */
    class WindowInsetListener(initialKeyboardVisibility: Boolean) : OnApplyWindowInsetsListener {
        var isKeyBoardVisible = initialKeyboardVisibility

        @SuppressLint("LongLogTag")
        override fun onApplyWindowInsets(v: View?, insets: WindowInsetsCompat?): WindowInsetsCompat? {
            val keyBoardVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
            Log.d("KeyBoardVisibility", "onApplyWindowInsets called: $v with keyboardVisibility: $keyBoardVisible")
            if (keyBoardVisible != isKeyBoardVisible) {
                Log.i("KeyBoardVisibilityChanged", "Keyboard is now ${if (keyBoardVisible) "visible" else "hidden"}")
                isKeyBoardVisible = keyBoardVisible
            }
            insets?.let {
                v?.onApplyWindowInsets(insets.toWindowInsets())
            }
            return insets
        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}