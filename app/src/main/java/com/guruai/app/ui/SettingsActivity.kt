<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/rootScroll"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0A0A0A"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Master Settings"
            android:textColor="#F5C518"
            android:textSize="22sp"
            android:textStyle="bold"
            android:paddingBottom="12dp" />

        <!-- Lock gate -->
        <LinearLayout
            android:id="@+id/lockPanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Master lock · enter password"
                android:textColor="#A0A0A0"
                android:paddingBottom="8dp" />

            <EditText
                android:id="@+id/etPassword"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword"
                android:hint="@string/master_password_hint"
                android:textColor="#F5F5F5"
                android:textColorHint="#666666"
                android:background="#1A1A1A"
                android:padding="12dp" />

            <TextView
                android:id="@+id/tvPassError"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="#EF4444"
                android:textSize="12sp"
                android:visibility="gone"
                android:text="Wrong password"
                android:paddingTop="6dp" />

            <Button
                android:id="@+id/btnUnlock"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/unlock"
                android:layout_marginTop="12dp" />
        </LinearLayout>

        <!-- Unlocked content -->
        <LinearLayout
            android:id="@+id/contentPanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Device: Nothing Phone (3a) Lite · Nothing OS"
                android:textColor="#A0A0A0"
                android:textSize="13sp"
                android:paddingBottom="20dp" />

            <!-- Theme picker -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="App Theme"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingBottom="10dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center"
                android:paddingBottom="20dp">

                <TextView
                    android:id="@+id/swatch0"
                    android:layout_width="46dp"
                    android:layout_height="46dp"
                    android:layout_marginEnd="10dp"
                    android:background="#0A0A0A"
                    android:gravity="center"
                    android:text="1"
                    android:textColor="#F5C518" />

                <TextView
                    android:id="@+id/swatch1"
                    android:layout_width="46dp"
                    android:layout_height="46dp"
                    android:layout_marginEnd="10dp"
                    android:background="#000000"
                    android:gravity="center"
                    android:text="2"
                    android:textColor="#FFFFFF" />

                <TextView
                    android:id="@+id/swatch2"
                    android:layout_width="46dp"
                    android:layout_height="46dp"
                    android:layout_marginEnd="10dp"
                    android:background="#071426"
                    android:gravity="center"
                    android:text="3"
                    android:textColor="#38BDF8" />

                <TextView
                    android:id="@+id/swatch3"
                    android:layout_width="46dp"
                    android:layout_height="46dp"
                    android:layout_marginEnd="10dp"
                    android:background="#160C24"
                    android:gravity="center"
                    android:text="4"
                    android:textColor="#A78BFA" />

                <TextView
                    android:id="@+id/swatch4"
                    android:layout_width="46dp"
                    android:layout_height="46dp"
                    android:background="#F5F1E8"
                    android:gravity="center"
                    android:text="5"
                    android:textColor="#C2703D" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvThemeName"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Current: Dark Gold"
                android:textColor="#A0A0A0"
                android:textSize="12sp"
                android:paddingBottom="20dp" />

            <!-- Feature toggles -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Features"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingBottom="8dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="#1A1A1A"
                android:padding="14dp"
                android:layout_marginBottom="8dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Screen Monitoring"
                    android:textColor="#F5F5F5" />

                <Switch
                    android:id="@+id/swScreenMonitor"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="#1A1A1A"
                android:padding="14dp"
                android:layout_marginBottom="8dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Read WhatsApp Messages"
                    android:textColor="#F5F5F5" />

                <Switch
                    android:id="@+id/swWhatsappSync"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="#1A1A1A"
                android:padding="14dp"
                android:layout_marginBottom="8dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Read Gmail / Emails"
                    android:textColor="#F5F5F5" />

                <Switch
                    android:id="@+id/swEmailSync"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="#1A1A1A"
                android:padding="14dp"
                android:layout_marginBottom="20dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="AI Online Mode"
                    android:textColor="#F5F5F5" />

                <Switch
                    android:id="@+id/swAiOnline"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:checked="true" />
            </LinearLayout>

            <!-- AI Provider -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="AI Provider"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingBottom="8dp" />

            <RadioGroup
                android:id="@+id/rgProvider"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:paddingBottom="16dp">

                <RadioButton
                    android:id="@+id/rbGemini"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Gemini"
                    android:textColor="#F5F5F5"
                    android:checked="true" />

                <RadioButton
                    android:id="@+id/rbGrok"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="xAI (Grok)"
                    android:textColor="#F5F5F5" />
            </RadioGroup>

            <!-- Box 1 -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/box1_gemini"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingBottom="6dp" />

            <EditText
                android:id="@+id/etGemini"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword"
                android:hint="AIza…"
                android:textColor="#F5F5F5"
                android:textColorHint="#666666"
                android:background="#1A1A1A"
                android:padding="12dp" />

            <CheckBox
                android:id="@+id/cbShowGemini"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Show key"
                android:textColor="#A0A0A0" />

            <!-- Grok -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="xAI (Grok) API Key"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingTop="16dp"
                android:paddingBottom="6dp" />

            <EditText
                android:id="@+id/etGrok"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword"
                android:hint="xai-…"
                android:textColor="#F5F5F5"
                android:textColorHint="#666666"
                android:background="#1A1A1A"
                android:padding="12dp" />

            <CheckBox
                android:id="@+id/cbShowGrok"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Show key"
                android:textColor="#A0A0A0" />

            <!-- Box 2 -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/box2_messaging"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingTop="16dp"
                android:paddingBottom="6dp" />

            <EditText
                android:id="@+id/etWhatsapp"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword"
                android:hint="WhatsApp Business token"
                android:textColor="#F5F5F5"
                android:textColorHint="#666666"
                android:background="#1A1A1A"
                android:padding="12dp" />

            <EditText
                android:id="@+id/etMail"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword"
                android:hint="Gmail / Mail token"
                android:textColor="#F5F5F5"
                android:textColorHint="#666666"
                android:background="#1A1A1A"
                android:padding="12dp"
                android:layout_marginTop="8dp" />

            <!-- Box 3 -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/box3_lock"
                android:textColor="#F5C518"
                android:textStyle="bold"
                android:paddingTop="16dp"
                android:paddingBottom="6dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="● Active · password gate enabled\nCloses when you leave Settings."
                android:textColor="#22C55E"
                android:background="#1A1A1A"
                android:padding="12dp" />

            <Button
                android:id="@+id/btnSave"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/save"
                android:layout_marginTop="20dp" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
