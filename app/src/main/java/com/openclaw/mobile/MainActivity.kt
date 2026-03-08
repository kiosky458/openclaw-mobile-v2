package com.openclaw.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val textView = findViewById<TextView>(R.id.text_view)
        textView.text = "OpenClaw Mobile\nv1.0.1 (測試版)\n\n此版本確認可正常啟動\n下一步將逐步加入功能"
    }
}
