package com.walhalla.piapmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jcraft.jsch.ChannelExec
import com.walhalla.piapmanager.ui.theme.PiAPManagerTheme
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MainActivity00 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PiAPManagerTheme {


                val scrollState = rememberScrollState()

                var output by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    SshSession.runCommandStreamed("whoami") { line ->
                        output += "$line\n"
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    println(innerPadding)
//                    Greeting(
//                        name = "cccc @@@ $x",
//                        modifier = Modifier.padding(innerPadding)
//                    )

                    var commandToRun by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(commandToRun) {
                        if (commandToRun != null) {
                            SshSession.runCommandStreamed(commandToRun!!) { line ->
                                val cleanLine = line.replace(Regex("\u001B\\[[;\\d]*m"), "").trim()
                                println(cleanLine)
                                output += "$cleanLine\n"
                            }
                            commandToRun = null
                        }
                    }

                    Column {

                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { commandToRun = "ping -c 4 8.8.8.8" }) {
                                Text("Ping")
                            }
                            Button(onClick = { commandToRun = "reboot" }) {
                                Text("Reboot")
                            }
                        }

                        Row(modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()){
                            Button(onClick = { commandToRun = "top" }) {
                                Text("System Stats")
                            }
                            Button(onClick = { commandToRun = "sudo wifite --kill --wps -vv --random-mac -i wlan1 --new-hs" }) {
                                Text("wifite")
                            }
//                            Button(onClick = { commandToRun = "wifite --kill --wps -vv --random-mac -i wlan1 --new-hs" }) {
//                                Text("wifite")
//                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(8.dp)
                        ) {
                            Text(text = output)
                        }


                    }


                }


                LaunchedEffect(output) {
                    // Ждём, пока maxValue обновится
                    snapshotFlow { scrollState.maxValue }
                        .filter { it > 0 }
                        .first()
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello@@@@ $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PiAPManagerTheme {
        Greeting("Android")
    }
}


object SshSession {
    private var session: Session? = null

    fun connect() {
        if (session == null || !session!!.isConnected) {
            val jsch = JSch()
            session = jsch.getSession("kali", "192.168.1.187", 22).apply {
                setPassword("kali")

                setConfig("StrictHostKeyChecking", "no")
                connect(3000)
            }
        }
    }

    fun disconnect() {
        session?.disconnect()
        session = null
    }

    suspend fun runCommandStreamed(command: String, onLine: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            var channel: ChannelExec? = null

            try {
                connect()



                channel = session!!.openChannel("exec") as ChannelExec
                channel.setCommand(command)
                channel.setErrStream(System.err)
                channel.setPty(true)
                channel.inputStream = null
                val input = channel.inputStream.bufferedReader()

                channel.connect()


                input.useLines { lines ->
                    lines.forEach { line ->
                        withContext(Dispatchers.Main) {
                            onLine(line) // Потоковый вывод строки в UI
                        }
                    }
                }
            } catch (e: com.jcraft.jsch.JSchException) {
                withContext(Dispatchers.Main) {
                    onLine("Error: ${e.message}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onLine("Error: ${e.message}")
                }
            } finally {
                channel?.disconnect()
            }
        }

//    suspend fun runCommand(command: String): String = withContext(Dispatchers.IO) {
//        connect()
//        val channel = session!!.openChannel("exec") as ChannelExec
//        channel.setCommand(command)
//        val input = channel.inputStream
//        channel.connect()
//        val result = input.bufferedReader().use { it.readText() }
//        channel.disconnect()
//        return@withContext result.trim()
//    }
}