package com.guruai.app.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guruai.app.R
import com.guruai.app.auth.GmailAuth
import com.guruai.app.data.Prefs
import com.guruai.app.util.Constants

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private var selectedTheme = 0
    private lateinit var swatches: List<TextView>
    private lateinit var tvThemeName: TextView
    private lateinit var tvGmailStatus: TextView

    private val gmailSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val email = account?.email ?: ""
                prefs.gmailConnectedEmail = email
                tvGmailStatus.text = "Connected: $email"
                Toast.makeText(this, "Gmail connected!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Gmail sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        val rootScroll = findViewById<ScrollView>(R.id.rootScroll)
        val lockPanel = findViewById<LinearLayout>(R.id.lockPanel)
        val contentPanel = findViewById<LinearLayout>(R.id.contentPanel)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvPassError = findViewById<TextView>(R.id.tvPassError)
        val etGemini = findViewById<EditText>(R.id.etGemini)
        val etGrok = findViewById<EditText>(R.id.etGrok)
        val etSearchApiKey = findViewById<EditText>(R.id.etSearchApiKey)
        val etSearchCx = findViewById<EditText>(R.id.etSearchCx)
        val etWhatsapp = findViewById<EditText>(R.id.etWhatsapp)
        val etMail = findViewById<EditText>(R.id.etMail)
        val cbShowGemini = findViewById<CheckBox>(R.id.cbShowGemini)
        val cbShowGrok = findViewById<CheckBox>(R.id.cbShowGrok)
        val cbShowSearchApiKey = findViewById<CheckBox>(R.id.cbShowSearchApiKey)
        val rgProvider = findViewById<RadioGroup>(R.id.rgProvider)
        val rbGemini = findViewById<RadioButton>(R.id.rbGemini)
        val rbGrok = findViewById<RadioButton>(R.id.rbGrok)
        tvThemeName = findViewById(R.id.tvThemeName)
        tvGmailStatus = findViewById(R.id.tvGmailStatus)

        val swScreenMonitor = findViewById<Switch>(R.id.swScreenMonitor)
        val swWhatsappSync = findViewById<Switch>(R.id.swWhatsappSync)
        val swEmailSync = findViewById<Switch>(R.id.swEmailSync)
        val swAiOnline = findViewById<Switch>(R.id.swAiOnline)

        swatches = listOf(
            findViewById(R.id.swatch0),
            findViewById(R.id.swatch1),
            findViewById(R.id.swatch2),
            findViewById(R.id.swatch3),
            findViewById(R.id.swatch4)
        )
        swatches.forEachIndexed { index, view ->
            view.setOnClickListener { selectTheme(index) }
        }

        findViewById<Button>(R.id.btnConnectGmail).setOnClickListener {
            gmailSignInLauncher.launch(GmailAuth.signInClient(this).signInIntent)
        }

        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            if (etPassword.text.toString() == Constants.MASTER_PASSWORD) {
                lockPanel.visibility = View.GONE
                contentPanel.visibility = View.VISIBLE
                etGemini.setText(prefs.geminiKey)
                etGrok.setText(prefs.grokKey)
                etSearchApiKey.setText(prefs.searchApiKey)
                etSearchCx.setText(prefs.searchCx)
                etWhatsapp.setText(prefs.whatsappToken)
                etMail.setText(prefs.mailToken)
                swScreenMonitor.isChecked = prefs.screenMonitorEnabled
                swWhatsappSync.isChecked = prefs.whatsappSyncEnabled
                swEmailSync.isChecked = prefs.emailSyncEnabled
                swAiOnline.isChecked = prefs.aiOnlineMode
                selectedTheme = prefs.themeIndex
                selectTheme(selectedTheme)
                if (prefs.aiProvider == Constants.PROVIDER_GROK) {
                    rbGrok.isChecked = true
                } else {
                    rbGemini.isChecked = true
                }

                val connectedEmail = GmailAuth.connectedEmail(this) ?: prefs.gmailConnectedEmail
                tvGmailStatus.text = if (connectedEmail.isNotBlank()) "Connected: $connectedEmail" else "Not connected"

                tvPassError.visibility = View.GONE
            } else {
                tvPassError.visibility = View.VISIBLE
            }
        }

        cbShowGemini.setOnCheckedChangeListener { _, checked ->
            etGemini.inputType = if (checked)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etGemini.setSelection(etGemini.text.length)
        }

        cbShowGrok.setOnCheckedChangeListener { _, checked ->
            etGrok.inputType = if (checked)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etGrok.setSelection(etGrok.text.length)
        }

        cbShowSearchApiKey.setOnCheckedChangeListener { _, checked ->
            etSearchApiKey.inputType = if (checked)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etSearchApiKey.setSelection(etSearchApiKey.text.length)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.geminiKey = etGemini.text.toString().trim()
            prefs.grokKey = etGrok.text.toString().trim()
            prefs.searchApiKey = etSearchApiKey.text.toString().trim()
            prefs.searchCx = etSearchCx.text.toString().trim()
            prefs.whatsappToken = etWhatsapp.text.toString().trim()
            prefs.mailToken = etMail.text.toString().trim()
            prefs.screenMonitorEnabled = swScreenMonitor.isChecked
            prefs.whatsappSyncEnabled = swWhatsappSync.isChecked
            prefs.emailSyncEnabled = swEmailSync.isChecked
            prefs.aiOnlineMode = swAiOnline.isChecked
            prefs.themeIndex = selectedTheme
            prefs.aiProvider = if (rgProvider.checkedRadioButtonId == R.id.rbGrok)
                Constants.PROVIDER_GROK else Constants.PROVIDER_GEMINI
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        applyTheme(rootScroll)
    }

    private fun selectTheme(index: Int) {
        selectedTheme = index
        val theme = Constants.THEMES[index]
        tvThemeName.text = "Current: ${theme.name}"
        swatches.forEachIndexed { i, view ->
            view.alpha = if (i == index) 1f else 0.4f
        }
    }

    private fun applyTheme(root: ScrollView) {
        val theme = Constants.THEMES[prefs.themeIndex]
        root.setBackgroundColor(Color.parseColor(theme.background))
    }
}
