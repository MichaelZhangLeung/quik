package com.example.visit.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anmi.camera.uvcplay.api.VisitLiveModule
import com.anmi.camera.uvcplay.api.VisitParams
import com.anmi.camera.uvcplay.broadcast.UsbConnectionReceiver
import com.example.visit.demo.ui.theme.UVCRtmpStreamTheme
import dev.alejandrorosas.apptemplate.UvcMainActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UVCRtmpStreamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 调用我们自定义的测试面板
                    SDKTestPanel()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun SDKTestPanel() {
    val context = LocalContext.current

    // 使用 Column 垂直排列按钮
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center, // 垂直居中
        horizontalAlignment = Alignment.CenterHorizontally // 水平居中
    ) {
        Text(
            text = "外访SDK测试",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 测试按钮
        Button(
            onClick = {
                Toast.makeText(context, "正在启动 SDK 测试...", Toast.LENGTH_SHORT).show()
                Log.e("SDKTestPanel", "[ur_play]开始测试,activity:" + UvcMainActivity::class.java.name)
                val now = System.currentTimeMillis()
                VisitLiveModule.startVisit(context, VisitParams("1000", "$now", "$now", "", true))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "开始测试")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                Toast.makeText(context, "其他测试", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = "其他测试启动")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UVCRtmpStreamTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun SDKTestPanelPreview() {
    UVCRtmpStreamTheme {
        SDKTestPanel()
    }
}
