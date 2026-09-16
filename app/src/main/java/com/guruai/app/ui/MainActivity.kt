package com.guruai.app.ui

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
import com.guruai.app.auth.GmailAuth
import com.guruai.app.data.GeminiClient
import com.guruai.app.data.GmailClient
import com.guruai.app.data.GmailMessage
import com.guruai.app.data.GrokClient
import com.guruai.app.data.OfflineLlmClient
import com.guruai.app.data.Prefs
import com.guruai.app.memory.KnowledgeStore
import com.guruai.app.memory.MemoryStore
import com.guruai.app.service.GuruAccessibilityService
import com.guruai.app.service.WhatsAppNotificationListener
import com.guruai.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: Prefs
    private lateinit var memoryStore: MemoryStore
    private lateinit var knowledgeStore: KnowledgeStore
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnMic: Button
    private lateinit var btnSend: Button
    private val history = mutableListOf<Pair<String, String>>()
    private var cameraImageUri: Uri? = null
    private var accentColor: Int = Color.parseColor("#F5C518")
    private var surfaceColor: Int = Color.parseColor("#121212")
    private var textPrimaryColor: Int = Color.parseColor("#F5F5F5")
    private var backgroundColor: Int = Color.parseColor("#0A0A0A")

    private var typingView: View? = null
    private var lastGmailMessages: List<GmailMessage> = emptyList()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isSpeaking = false
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
        com.tom_roush.pdfbox.util.PDFBoxResourceLoader.init(applicationContext)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        memoryStore = MemoryStore(this)
        knowledgeStore = KnowledgeStore(this)

        toolRegistry = ToolRegistry().apply {
            register(GetTimeTool())
            register(WebSearchTool(prefs.searchApiKey, prefs.searchCx))
        }
        agentLoop = AgentLoop(
            llmClient = { prompt -> callAi(prompt) },
            toolRegistry = toolRegistry
        )

        chatContainer = findViewById(R.id.chatContainer)
        chatScroll = findViewById(R.id.chatScroll)
        tvStatus = findViewById(R.id.tvStatus)
        etInput = findViewById(R.id.etInput)
        btnMic = findViewById(R.id.btnMic)
        btnSend = findViewById(R.id.btnSend)

        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnSend.setOnClickListener { send() }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { showAttachMenu() }
        findViewById<Button>(R.id.btnReadScreen).setOnClickListener { readScreen() }
        findViewById<Button>(R.id.btnMenu).setOnClickListener { showHistoryMenu() }
        btnMic.setOnClickListener { toggleMic() }

        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                btnSend.isEnabled = hasText
                btnSend.alpha = if (hasText) 1f else 0.4f
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        btnSend.isEnabled = false
        btnSend.alpha = 0.4f

        applyTheme()
        addWelcomeMessage()
        loadHistory()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    if (isListening) restartListening()
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    if (isListening) restartListening()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        applyTheme()
    }

    // ---------- AI provider call with fallback (Gemini + Grok only) ----------

    private suspend fun callAi(prompt: String): String {
        val providers = mutableListOf<Pair<String, suspend () -> String>>()

        if (prefs.geminiKey.isNotBlank()) {
            providers.add("Gemini" to { GeminiClient(prefs.geminiKey).chat(prompt, emptyList()) })
        }
        if (prefs.grokKey.isNotBlank()) {
            providers.add("Grok" to { GrokClient(prefs.grokKey).chat(prompt, emptyList()) })
        }

        val preferredName = when (prefs.aiProvider) {
            Constants.PROVIDER_GROK -> "Grok"
            else -> "Gemini"
        }
        val ordered = providers.sortedByDescending { it.first == preferredName }

        var lastError = "Could not reach any AI provider."
        for ((name, call) in ordered) {
            val result = try {
                call()
            } catch (e: Exception) {
                "error: ${e.message}"
            }
            val looksLikeFailure = result.startsWith("Gemini error") ||
                result.startsWith("Grok error") ||
                result.startsWith("error:") ||
                result.contains("API key missing")

            if (!looksLikeFailure) {
                return result
            }
            lastError = result
        }

        val offlinePath = prefs.offlineModelPath
        if (offlinePath.isNotBlank() && File(offlinePath).exists()) {
            val offlineResult = withContext(Dispatchers.Default) {
                OfflineLlmClient.generate(applicationContext, offlinePath, prompt)
            }
            if (!offlineResult.startsWith("(Offline model error")) {
                return "$offlineResult\n\n[offline mode — no internet]"
            }
        }

        return lastError
    }

    // ---------- Friendly error wrapping ----------

    private fun friendlyReply(raw: String): String {
        val lower = raw.lowercase()
        val looksLikeError = lower.contains("exception") ||
            lower.contains("failed to connect") ||
            lower.contains("unable to resolve host") ||
            lower.contains("timeout") ||
            lower.contains("no ai provider key") ||
            raw.startsWith("error:") ||
            raw.startsWith("Gemini error") ||
            raw.startsWith("Grok error") ||
            raw.startsWith("Could not reach")
        return if (looksLikeError) {
            "Lagta hai internet connection weak hai ya AI thoda busy hai abhi. Thodi der baad dobara try karo. 🙏"
        } else {
            raw
        }
    }

    private fun addWelcomeMessage() {
        addMessageView("assistant", "Hey! I'm Guru. Add Gemini key in Settings.")
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { memoryStore.getAllMessages() }
            if (saved.isNotEmpty()) {
                saved.forEach { msg ->
                    history.add(msg.role to msg.content)
                    addMessageView(msg.role, msg.content)
                }
            }
        }
    }

    // ---------- Message rendering (bubble + copy + alignment) ----------

    private fun addMessageView(role: String, text: String): View {
        val label = if (role == "user") "You" else "Guru"
        val isUser = role == "user"

        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.HORIZONTAL
        outer.gravity = if (isUser) Gravity.END else Gravity.START
        val outerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        outerParams.bottomMargin = 16
        outer.layoutParams = outerParams

        val bubble = LinearLayout(this)
        bubble.orientation = LinearLayout.VERTICAL
        val bubbleBg = if (isUser) accentColor else surfaceColor
        bubble.setBackgroundColor(bubbleBg)
        bubble.setPadding(24, 16, 24, 16)
        bubble.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val textColorForBubble = if (isUser) backgroundColor else textPrimaryColor

        val labelView = TextView(this)
        labelView.text = label
        labelView.setTextColor(if (isUser) backgroundColor else accentColor)
        labelView.textSize = 12f
        labelView.setPadding(0, 0, 0, 4)
        bubble.addView(labelView)

        val textView = TextView(this)
        textView.text = text
        textView.setTextColor(textColorForBubble)
        textView.textSize = 15f
        textView.maxWidth = (resources.displayMetrics.widthPixels * 0.78).toInt()
        bubble.addView(textView)

        if (role == "assistant") {
            val copyRow = LinearLayout(this)
            copyRow.orientation = LinearLayout.HORIZONTAL
            copyRow.gravity = Gravity.END

            val copyButton = TextView(this)
            copyButton.text = "Copy"
            copyButton.setTextColor(accentColor)
            copyButton.textSize = 12f
            copyButton.setPadding(0, 12, 0, 0)
            copyButton.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Guru reply", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@MainActivity, "Copied", Toast.LENGTH_SHORT).show()
            }
            copyRow.addView(copyButton)
            bubble.addView(copyRow)
        }

        outer.addView(bubble)
        chatContainer.addView(outer)
        chatScroll.post { chatScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        return outer
    }

    private fun showTyping() {
        typingView = addMessageView("assistant", "Guru type kar raha hai…")
    }

    private fun hideTyping() {
        typingView?.let { chatContainer.removeView(it) }
        typingView = null
    }

    // ---------- Knowledge / Notes ----------

    private fun showSaveNoteDialog() {
        val input = EditText(this)
        input.hint = "Type something for Guru to remember…"
        input.setTextColor(Color.parseColor("#000000"))
        input.setHintTextColor(Color.parseColor("#888888"))
        input.setPadding(40, 30, 40, 30)

        AlertDialog.Builder(this)
            .setTitle("Save a note")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            knowledgeStore.save(title = text.take(40), content = text)
                        }
                        Toast.makeText(this@MainActivity, "Saved to Guru's memory", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                prefs.conversationSummary = ""
                prefs.summarizedUpToCount = 0
                chatContainer.removeAllViews()
                addWelcomeMessage()
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
            showTyping()
            val prompt = "The user asked to read the current screen. Here is the accessibility text snapshot:\n\n$screenText\n\nSummarize clearly and help with next steps."
            val rawReply = withContext(Dispatchers.IO) { callAi(prompt) }
            hideTyping()
            val reply = friendlyReply(rawReply)
            append("assistant", reply)
            speak(reply)
        }
    }

    // ---------- Mic (continuous toggle, pauses while Guru speaks) ----------

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
                if (isListening && !isSpeaking) restartListening()
            }

            override fun onError(error: Int) {
                if (isListening && !isSpeaking) restartListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()
                if (!spoken.isNullOrBlank() && !isSpeaking) {
                    etInput.setText(spoken)
                    send()
                }
                if (isListening && !isSpeaking) restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        btnMic.text = "⏹"
        if (!isSpeaking) launchRecognizerIntent()
    }

    private fun restartListening() {
        if (isListening && !isSpeaking) launchRecognizerIntent()
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

    // ---------- Attach menu ----------

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
        view.findViewById<LinearLayout>(R.id.optionSaveNote).setOnClickListener {
            dialog.dismiss()
            showSaveNoteDialog()
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
            showTyping()
            val rawReply = withContext(Dispatchers.IO) {
                GeminiClient(prefs.geminiKey).analyzeImage(
                    contentResolver,
                    uri,
                    "Describe what you see in this image and give useful, relevant information or help based on it."
                )
            }
            hideTyping()
            val reply = friendlyReply(rawReply)
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
            val fileName = getFileName(uri)
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                contentResolver.getType(uri) == "application/pdf"

            val content = withContext(Dispatchers.IO) {
                try {
                    if (isPdf) {
                        com.guruai.app.util.PdfTextExtractor.extractText(this@MainActivity, uri)
                    } else {
                        contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (content.isNullOrBlank()) {
                val msg = if (isPdf) {
                    "Is PDF mein text nahi mila — shayad yeh scanned/photo PDF hai (abhi sirf text-wali PDF chalti hain)."
                } else {
                    "Could not read this file. Try a plain text (.txt) or text-based PDF file."
                }
                append("assistant", msg)
                return@launch
            }

            val savedContent = content.take(2_000_000)

            withContext(Dispatchers.IO) {
                knowledgeStore.save(title = fileName, content = savedContent)
            }

            showTyping()
            val trimmedContent = content.take(6000)
            val prompt = "Here is the content of a file the user shared:\n\n$trimmedContent\n\nSummarize it and highlight anything important or useful."

            val rawReply = withContext(Dispatchers.IO) { callAi(prompt) }
            hideTyping()
            val reply = friendlyReply(rawReply)
            append("assistant", "Saved \"$fileName\" to Guru's memory (${content.length} characters). Here's a summary:\n\n$reply")
            speak(reply)
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "file"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            // fallback to lastPathSegment
        }
        return name
    }

    private fun applyTheme() {
        val theme = Constants.THEMES[prefs.themeIndex]
        val bg = Color.parseColor(theme.background)
        val surface = Color.parseColor(theme.surface)
        val accent = Color.parseColor(theme.accent)
        val textPrimary = Color.parseColor(theme.textPrimary)
        val textSecondary = Color.parseColor(theme.textSecondary)

        accentColor = accent
        surfaceColor = surface
        textPrimaryColor = textPrimary
        backgroundColor = bg

        findViewById<LinearLayout>(R.id.rootLayout).setBackgroundColor(bg)
        findViewById<LinearLayout>(R.id.topBar).setBackgroundColor(surface)
        findViewById<LinearLayout>(R.id.bottomBar).setBackgroundColor(surface)
        findViewById<TextView>(R.id.tvTitle).setTextColor(accent)
        tvStatus.setTextColor(textSecondary)
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

    // ---------- Chat + Summarization + Knowledge ----------

    private fun append(role: String, text: String) {
        history.add(role to text)
        addMessageView(role, text)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                memoryStore.saveMessage(role, text)
            }
            maybeSummarize()
        }
    }

    private suspend fun maybeSummarize() {
        val newSinceLastSummary = history.size - prefs.summarizedUpToCount
        if (newSinceLastSummary < 20) return
        if (!prefs.aiOnlineMode) return

        val toSummarize = history.drop(prefs.summarizedUpToCount).dropLast(4)
        if (toSummarize.isEmpty()) return

        val transcript = toSummarize.joinToString("\n") { (role, text) ->
            val label = if (role == "user") "User" else "Guru"
            "$label: $text"
        }

        val existingSummary = prefs.conversationSummary
        val prompt = if (existingSummary.isBlank()) {
            "Summarize this conversation in a few short sentences, keeping key facts, names, and preferences:\n\n$transcript"
        } else {
            "Here is an existing summary of earlier conversation:\n$existingSummary\n\nHere are new messages to fold in:\n$transcript\n\nGive one updated, concise summary combining both, keeping key facts, names, and preferences."
        }

        val newSummary = withContext(Dispatchers.IO) { callAi(prompt) }
        prefs.conversationSummary = newSummary
        prefs.summarizedUpToCount = history.size - 4
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

        val whatsappKeywords = listOf("whatsapp", "व्हाट्सएप", "वाट्सएप")
        val isWhatsappRequest = whatsappKeywords.any { text.contains(it, ignoreCase = true) }

        if (isWhatsappRequest) {
            handleWhatsAppRequest(text)
            return
        }

        val gmailKeywords = listOf("gmail", "email", "mail", "जीमेल", "ईमेल")
        val isGmailRequest = gmailKeywords.any { text.contains(it, ignoreCase = true) }

        if (isGmailRequest) {
            val isReplyRequest = text.contains("reply", ignoreCase = true) ||
                text.contains("जवाब", ignoreCase = true)
            if (isReplyRequest) {
                handleGmailReplyRequest(text)
            } else {
                handleGmailListRequest()
            }
            return
        }

        val needsTool = text.contains("time", ignoreCase = true) ||
            text.contains("समय", ignoreCase = true) ||
            text.contains("search", ignoreCase = true) ||
            text.contains("खोज", ignoreCase = true) ||
            text.contains("check kar", ignoreCase = true) ||
            text.contains("check karo", ignoreCase = true) ||
            text.contains("net per", ignoreCase = true) ||
            text.contains("net pe", ignoreCase = true) ||
            text.contains("google", ignoreCase = true) ||
            text.contains("pata karo", ignoreCase = true)

        lifecycleScope.launch {
            showTyping()
            val rawReply = withContext(Dispatchers.IO) {
                if (needsTool) {
                    agentLoop.run(text)
                } else {
                    val relevantNotes = knowledgeStore.findRelevant(text)
                    callAi(buildPromptWithHistory(text, relevantNotes.map { it.content }))
                }
            }
            hideTyping()
            val reply = friendlyReply(rawReply)
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun handleWhatsAppRequest(userText: String) {
        if (!prefs.whatsappSyncEnabled) {
            append("assistant", "Turn on \"Read WhatsApp Messages\" in Settings first.")
            return
        }
        if (!WhatsAppNotificationListener.isListenerConnected(this)) {
            append("assistant", "Guru needs notification access to read WhatsApp. Opening settings — find Guru AI and turn it on.")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            return
        }

        val nameMatch = Regex("(?:from|ka|ki)\\s+([A-Za-z\\u0900-\\u097F]+)", RegexOption.IGNORE_CASE)
            .find(userText)?.groupValues?.get(1)

        val messages = if (!nameMatch.isNullOrBlank()) {
            WhatsAppNotificationListener.getRecentFrom(nameMatch)
        } else {
            WhatsAppNotificationListener.getRecent()
        }

        if (messages.isEmpty()) {
            append("assistant", "No recent WhatsApp messages found" + if (!nameMatch.isNullOrBlank()) " from $nameMatch." else ".")
            return
        }

        val summary = messages.takeLast(10).joinToString("\n") { "${it.sender}: ${it.text}" }
        append("assistant", "Recent WhatsApp messages:\n\n$summary\n\nWant me to draft a reply to any of these? Just tell me what to say — I'll prepare it, but you'll tap Send yourself.")
    }

    // ---------- Gmail ----------

    private fun handleGmailListRequest() {
        if (!prefs.emailSyncEnabled) {
            append("assistant", "Turn on \"Read Gmail / Emails\" in Settings first.")
            return
        }

        lifecycleScope.launch {
            showTyping()
            val token = withContext(Dispatchers.IO) { GmailAuth.getAccessToken(this@MainActivity) }
            if (token == null) {
                hideTyping()
                append("assistant", "Gmail connect nahi hai. Settings mein \"Connect Gmail\" dabao aur sign in karo.")
                return@launch
            }
            val messages = withContext(Dispatchers.IO) { GmailClient.listRecent(token, 5) }
            lastGmailMessages = messages
            hideTyping()
            if (messages.isEmpty()) {
                append("assistant", "Koi naya email nahi mila inbox mein.")
            } else {
                val summary = messages.joinToString("\n\n") { "From: ${it.from}\nSubject: ${it.subject}\n${it.snippet}" }
                append("assistant", "Recent emails:\n\n$summary\n\nKisi ek ka reply banwana ho to bolo: \"reply likho: <kya kehna hai>\" — main draft bana dunga, bhejna khud karna.")
            }
        }
    }

    private fun handleGmailReplyRequest(userText: String) {
        if (lastGmailMessages.isEmpty()) {
            append("assistant", "Pehle \"email check karo\" bolo, phir batana kisko reply karna hai.")
            return
        }

        val target = lastGmailMessages.first()

        lifecycleScope.launch {
            showTyping()
            val token = withContext(Dispatchers.IO) { GmailAuth.getAccessToken(this@MainActivity) }
            if (token == null) {
                hideTyping()
                append("assistant", "Gmail connect nahi hai. Settings mein \"Connect Gmail\" dabao aur sign in karo.")
                return@launch
            }

            val draftPrompt = "Original email:\nFrom: ${target.from}\nSubject: ${target.subject}\n${target.snippet}\n\nUser wants this reply drafted: $userText\n\nWrite a short, polite email reply body only (no subject line, no signature placeholder)."
            val body = withContext(Dispatchers.IO) { callAi(draftPrompt) }

            val success = withContext(Dispatchers.IO) {
                GmailClient.createDraft(token, target.from, "Re: ${target.subject}", body)
            }
            hideTyping()

            val reply = if (success) {
                "Draft ban gaya ${target.from} ke liye — Gmail app kholke Drafts mein check karo aur khud Send karo:\n\n$body"
            } else {
                "Draft banane mein dikkat aayi. Dobara try karo."
            }
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun buildPromptWithHistory(userMessage: String, relevantNotes: List<String> = emptyList()): String {
        val recent = history.dropLast(1).takeLast(10).joinToString("\n") { (role, text) ->
            val label = if (role == "user") "User" else "Guru"
            "$label: $text"
        }
        val summary = prefs.conversationSummary
        return buildString {
            if (relevantNotes.isNotEmpty()) {
                append("Relevant saved notes:\n")
                relevantNotes.forEach { append("- $it\n") }
                append("\n")
            }
            if (summary.isNotBlank()) {
                append("Summary of earlier conversation:\n$summary\n\n")
            }
            if (recent.isNotBlank()) {
                append("Recent conversation:\n$recent\n\n")
            }
            append("User: $userMessage")
        }
    }

    private fun speak(text: String) {
        speechRecognizer?.stopListening()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guru_reply")
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
