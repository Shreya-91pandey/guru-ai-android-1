package com.guruai.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.guruai.app.R
import com.guruai.app.agent.AgentLoop
import com.guruai.app.agent.GetTimeTool
import com.guruai.app.agent.ToolRegistry
import com.guruai.app.agent.WebSearchTool
import com.guruai.app.data.GeminiClient
import com.guruai.app.data.GrokClient
import com.guruai.app.data.Prefs
import com.guruai.app.memory.MemoryStore
import com.guruai.app.service.GuruAccessibilityService
import com.guruai.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: Prefs
    private lateinit var memoryStore: MemoryStore
    private lateinit var tvChat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnMic: Button
    private val history = mutableListOf<Pair<String, String>>()
    private var cameraImageUri: Uri? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var tts: TextToSpeech? = null

    private lateinit var toolRegistry: ToolRegistry
    private lateinit var agentLoop: AgentLoop

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            Toast.makeText(this, "Mic permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraImageUri
        if (success && uri != null) {
            analyzeImage(uri, "captured")
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            analyzeImage(uri, "selected")
        }
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            analyzeFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        memoryStore = MemoryStore(this)

        toolRegistry = ToolRegistry().apply {
            register(GetTimeTool())
            register(WebSearchTool())
        }
        agentLoop = AgentLoop(
            llmClient = { prompt -> callAi(prompt) },
            toolRegistry = toolRegistry
        )

        tvChat = findViewById(R.id.tvChat)
        tvStatus = findViewById(R.id.tvStatus)
        etInput = findViewById(R.id.etInput)
        btnMic = findViewById(R.id.btnMic)

        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSend).setOnClickListener { send() }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { showAttachMenu() }
        findViewById<Button>(R.id.btnReadScreen).setOnClickListener { readScreen() }
        findViewById<Button>(R.id.btnMenu).setOnClickListener { showHistoryMenu() }
        btnMic.setOnClickListener { toggleMic() }

        applyTheme()
        loadHistory()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        applyTheme()
    }

    private suspend fun callAi(prompt: String): String {
        return if (prefs.aiProvider == Constants.PROVIDER_GROK) {
            if (prefs.grokKey.isBlank()) {
                "xAI (Grok) API key missing. Add it in Settings."
            } else {
                GrokClient(prefs.grokKey).chat(prompt, emptyList())
            }
        } else {
            GeminiClient(prefs.geminiKey).chat(prompt, emptyList())
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { memoryStore.getAllMessages() }
            if (saved.isNotEmpty()) {
                saved.forEach { msg ->
                    history.add(msg.role to msg.content)
                    val label = if (msg.role == "user") "You" else "Guru"
                    tvChat.append("\n\n$label:\n${msg.content}")
                }
            }
        }
    }

    // ---------- Hamburger menu / History ----------

    private fun showHistoryMenu() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_history, null)
        dialog.setContentView(view)

        val container = view.findViewById<LinearLayout>(R.id.historyListContainer)

        lifecycleScope.launch {
            val grouped = withContext(Dispatchers.IO) { memoryStore.getMessagesGroupedByDate() }
            container.removeAllViews()
            if (grouped.isEmpty()) {
                val empty = TextView(this@MainActivity)
                empty.text = "No history yet."
                empty.setTextColor(Color.parseColor("#888888"))
                empty.setPadding(8, 8, 8, 8)
                container.addView(empty)
            } else {
                grouped.forEach { (date, messages) ->
                    val dateHeader = TextView(this@MainActivity)
                    dateHeader.text = date
                    dateHeader.setTextColor(Color.parseColor("#F5C518"))
                    dateHeader.textSize = 14f
                    dateHeader.setPadding(4, 24, 4, 8)
                    container.addView(dateHeader)

                    val preview = messages.take(3).joinToString("\n") { msg ->
                        val label = if (msg.role == "user") "You" else "Guru"
                        "$label: ${msg.content.take(60)}"
                    }
                    val previewView = TextView(this@MainActivity)
                    previewView.text = preview
                    previewView.setTextColor(Color.parseColor("#CCCCCC"))
                    previewView.textSize = 12f
                    previewView.setPadding(4, 0, 4, 4)
                    container.addView(previewView)
                }
            }
        }

        view.findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { memoryStore.clearAll() }
                history.clear()
                tvChat.text = "Hey! I'm Guru. Add Gemini key in Settings."
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "History cleared", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun readScreen() {
        if (!prefs.screenMonitorEnabled) {
            Toast.makeText(this, "Turn on Screen Monitoring in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        val svc = GuruAccessibilityService.get()
        if (svc == null) {
            Toast.makeText(this, "Enable Accessibility for Guru AI in system settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val screenText = svc.readVisibleText()
        append("user", "[Read screen request]")

        if (!prefs.aiOnlineMode) {
            append("assistant", "AI Online Mode is off. Turn it on in Settings to analyze the screen.")
            return
        }

        lifecycleScope.launch {
            val prompt = "The user asked to read the current screen. Here is the accessibility text snapshot:\n\n$screenText\n\nSummarize clearly and help with next steps."
            val reply = withContext(Dispatchers.IO) { callAi(prompt) }
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun toggleMic() {
        if (isListening) {
            stopListening()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startListening()
            } else {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (isListening) restartListening()
            }

            override fun onError(error: Int) {
                if (isListening) restartListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    etInput.setText(spoken)
                    send()
                }
                if (isListening) restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        btnMic.text = "⏹"
        launchRecognizerIntent()
    }

    private fun restartListening() {
        if (isListening) launchRecognizerIntent()
    }

    private fun launchRecognizerIntent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            btnMic.text = "🎤"
        }
    }

    private fun stopListening() {
        isListening = false
        btnMic.text = "🎤"
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun showAttachMenu() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_attach_menu, null)
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            launchCamera()
        }
        view.findViewById<LinearLayout>(R.id.optionPhotos).setOnClickListener {
            dialog.dismiss()
            pickImageLauncher.launch("image/*")
        }
        view.findViewById<LinearLayout>(R.id.optionFiles).setOnClickListener {
            dialog.dismiss()
            pickFileLauncher.launch("*/*")
        }
        dialog.show()
    }

    private fun launchCamera() {
        val imagesDir = File(getExternalFilesDir("images"), "")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "guru_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "com.guruai.app.fileprovider",
            imageFile
        )
        cameraImageUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun analyzeImage(uri: Uri, source: String) {
        append("user", "[Photo $source] Analyzing…")

        if (!prefs.aiOnlineMode) {
            append("assistant", "AI Online Mode is off. Turn it on in Settings to analyze photos.")
            return
        }
        if (prefs.geminiKey.isBlank()) {
            append("assistant", "Add your Gemini API key in Settings first.")
            return
        }

        lifecycleScope.launch {
            val reply = withContext(Dispatchers.IO) {
                GeminiClient(prefs.geminiKey).analyzeImage(
                    contentResolver,
                    uri,
                    "Describe what you see in this image and give useful, relevant information or help based on it."
                )
            }
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun analyzeFile(uri: Uri) {
        append("user", "[File selected] Reading…")

        if (!prefs.aiOnlineMode) {
            append("assistant", "AI Online Mode is off. Turn it on in Settings to read files.")
            return
        }

        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                } catch (e: Exception) {
                    null
                }
            }

            if (content.isNullOrBlank()) {
                append("assistant", "Could not read this file. Try a plain text file (.txt) for now — other formats are coming soon.")
                return@launch
            }

            val trimmedContent = content.take(6000)
            val prompt = "Here is the content of a file the user shared:\n\n$trimmedContent\n\nSummarize it and highlight anything important or useful."

            val reply = withContext(Dispatchers.IO) { callAi(prompt) }
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun applyTheme() {
        val theme = Constants.THEMES[prefs.themeIndex]
        val bg = Color.parseColor(theme.background)
        val surface = Color.parseColor(theme.surface)
        val accent = Color.parseColor(theme.accent)
        val textPrimary = Color.parseColor(theme.textPrimary)
        val textSecondary = Color.parseColor(theme.textSecondary)

        findViewById<LinearLayout>(R.id.rootLayout).setBackgroundColor(bg)
        findViewById<LinearLayout>(R.id.topBar).setBackgroundColor(surface)
        findViewById<LinearLayout>(R.id.bottomBar).setBackgroundColor(surface)
        findViewById<TextView>(R.id.tvTitle).setTextColor(accent)
        tvStatus.setTextColor(textSecondary)
        tvChat.setTextColor(textPrimary)
        etInput.setTextColor(textPrimary)
        etInput.setBackgroundColor(surface)

        val btnMenu = findViewById<Button>(R.id.btnMenu)
        btnMenu.setBackgroundColor(surface)
        btnMenu.setTextColor(accent)

        val btnSettings = findViewById<Button>(R.id.btnSettings)
        btnSettings.setBackgroundColor(accent)
        btnSettings.setTextColor(bg)

        val btnReadScreen = findViewById<Button>(R.id.btnReadScreen)
        btnReadScreen.setBackgroundColor(surface)
        btnReadScreen.setTextColor(accent)

        val btnSend = findViewById<Button>(R.id.btnSend)
        btnSend.setBackgroundColor(accent)
        btnSend.setTextColor(bg)

        val btnPlus = findViewById<Button>(R.id.btnPlus)
        btnPlus.setBackgroundColor(surface)
        btnPlus.setTextColor(accent)

        btnMic.setBackgroundColor(surface)
        btnMic.setTextColor(accent)
    }

    private fun refreshStatus() {
        val a11y = if (GuruAccessibilityService.isEnabled()) "on" else "off"
        val monitor = if (prefs.screenMonitorEnabled) "monitor on" else "monitor off"
        val key = if (prefs.geminiKey.isNotBlank()) "Gemini OK" else "add Gemini key"
        val mode = if (prefs.aiOnlineMode) "Online" else "Offline"
        tvStatus.text = "Accessibility: $a11y · $monitor · $key · $mode · ${Constants.DEVICE_MODEL}"
    }

    private fun append(role: String, text: String) {
        history.add(role to text)
        val label = if (role == "user") "You" else "Guru"
        tvChat.append("\n\n$label:\n$text")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                memoryStore.saveMessage(role, text)
            }
        }
    }

    private fun send() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        etInput.setText("")
        append("user", text)

        if (!prefs.aiOnlineMode) {
            append("assistant", "AI Online Mode is off. Turn it on in Settings to chat with Gemini, or ask about something saved locally.")
            return
        }

        val needsTool = text.contains("time", ignoreCase = true) ||
            text.contains("समय", ignoreCase = true) ||
            text.contains("search", ignoreCase = true) ||
            text.contains("खोज", ignoreCase = true)

        lifecycleScope.launch {
            val reply = withContext(Dispatchers.IO) {
                if (needsTool) {
                    agentLoop.run(text)
                } else {
                    callAi(buildPromptWithHistory(text))
                }
            }
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun buildPromptWithHistory(userMessage: String): String {
        if (history.size <= 1) return userMessage
        val recent = history.dropLast(1).takeLast(10).joinToString("\n") { (role, text) ->
            val label = if (role == "user") "User" else "Guru"
            "$label: $text"
        }
        return "Recent conversation:\n$recent\n\nUser: $userMessage"
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guru_reply")
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
