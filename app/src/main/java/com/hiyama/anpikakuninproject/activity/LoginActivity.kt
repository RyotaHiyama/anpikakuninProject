package com.hiyama.anpikakuninproject.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.hiyama.anpikakuninproject.CommServer
import com.hiyama.anpikakuninproject.MainActivity
import com.hiyama.anpikakuninproject.data.PostTest
import com.hiyama.anpikakuninproject.R
import com.hiyama.anpikakuninproject.data.DataChecker
import com.hiyama.anpikakuninproject.data.JsonParser
import com.hiyama.anpikakuninproject.data.LoginInfo
import com.hiyama.anpikakuninproject.data.SafetyCheckInfo
import com.hiyama.anpikakuninproject.data.UserInfo
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private val commServer = CommServer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val userNameEditText = findViewById<EditText>(R.id.userName)
        val passwordEditText = findViewById<EditText>(R.id.passWord)
        val testTxt = findViewById<TextView>(R.id.testText)

        safetyCheckActivity()

        val loginBtn = findViewById<Button>(R.id.loginBtn)
        loginBtn.setOnClickListener { // ログインするためのボタン
            val userName = userNameEditText.text.toString()
            val passWord = hashSHA256String(passwordEditText.text.toString())
//            val postData = jacksonObjectMapper().writeValueAsString(User.getUserInfo())
//            Log.i("postData",postData )
            if (checkCorrectEntered(userName, passWord)){
                UserInfo.userName = userName
                UserInfo.password = passWord
                commServer.setURL(CommServer.LOGIN)
                Log.i("uuid, pass", UserInfo.userName + UserInfo.password)
                if (!login()){
                    passwordEditText.text.clear()
                }
            }
            overridePendingTransition(0,0)
        }

        val testBtn = findViewById<Button>(R.id.testBtn) //ServerからGETできるかテストするためのボタン
        testBtn.setOnClickListener {
            commServer.setURL(CommServer.TEST)
            val result = getInfo()
            testTxt.text = result
            val postData = jacksonObjectMapper().writeValueAsString(PostTest.getPostData())
            Log.i("postData",postData )
        }

        val postBtn = findViewById<Button>(R.id.postBtn) //ServerにPOSTできるかテストするためのボタン
        postBtn.setOnClickListener {
            commServer.setURL(CommServer.POSTTEST)
            postTest()
        }

        /*------------------ここからpush通知に関すること------------------*/
        // インスタンスIDの自動生成を有効化する場合、true
        // AndroidManifestにて自動生成を禁止にしていない場合、不要
//        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Current Notificationトークンの取得
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // token取得失敗
                    Log.d("getInstanceId failed ${task.exception}", "a")
                    return@OnCompleteListener
                }

                // new Instance ID token
                // ここで取得したtokenをテストする際のインスタンスIDとして設定する
                val token = task.result
                UserInfo.fcmToken = token

                val msg = "InstanceID Token: $token"
                Log.d("msg",msg)
//                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            })
        /*------------------ここまで------------------*/

    }

    private fun login(): Boolean {
        val result = loginInfo()
        while(commServer.responseCode == -1){/* wait for response */}
        if (commServer.responseCode == HttpURLConnection.HTTP_OK){
            Log.i("Return Value From Server", "Value: $result")
            val loginResult = JsonParser.loginResultParse(result)
            return if (loginResult == null){
                Toast.makeText(this, "ログインの際にサーバから予期せぬメッセージを受信しました", Toast.LENGTH_LONG).show()
                false
            } else {
                LoginInfo.initialize(loginResult)

                if (LoginInfo.success){
                    Toast.makeText(this, "${UserInfo.userName}さん ようこそ！", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                } else {
                    incorrectSignIn(this)
                    false
                }
            }
        } else {
            incorrectSignIn(this)
            return false
        }
    }

    private fun postTest(): Boolean{
        val postSuccess = findViewById<TextView>(R.id.postSuccess)
        val postMessage = findViewById<TextView>(R.id.postMessage)
        val postToken = findViewById<TextView>(R.id.postToken)

        val result = postInfo()
        Log.i("postTestResult", result)
        while(commServer.responseCode == -1){/* wait for response */}
        Log.i("resposeCode", commServer.responseCode.toString())
        if (commServer.responseCode == HttpURLConnection.HTTP_OK){
            Log.i("Return Value From Server", "Value: $result")
            if (result != "null"){
                val postResultTest = JsonParser.loginResultParse(result)
                Log.i("postResuleTest", postResultTest.toString())
                return if (postResultTest == null){
                    Toast.makeText(this, "ログインの際にサーバから予期せぬメッセージを受信しました", Toast.LENGTH_LONG).show()
                    false
                } else {
                    LoginInfo.initialize(postResultTest)
                    postSuccess.text = LoginInfo.success.toString()
                    Log.i("success", LoginInfo.success.toString())
                    postMessage.text = LoginInfo.message
                    Log.i("message", LoginInfo.message)
                    postToken.text = LoginInfo.token
                    Log.i("token", LoginInfo.token)
                    true
                }
            } else {
                incorrectSignIn(this)
                return false
            }
        } else {
            incorrectSignIn(this)
            return false
        }
    }

    private fun safetyCheckActivity(){
        commServer.setURL(CommServer.SAFETY_CHECK)
        val result = getInfo()
        while(commServer.responseCode == -1){/* wait for response */}
        if (commServer.responseCode == HttpURLConnection.HTTP_OK){
            Log.i("Return Value From Server", "Value: $result")
            val safetyCheck = JsonParser.safetyCheckParse(result)
            SafetyCheckInfo.initialize(safetyCheck!!)
            if (SafetyCheckInfo.check == "True"){
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RyotaHiyama"))
                startActivity(intent)
            } else {
                /* do nothing */
            }
        }
    }

    @UiThread
    private fun getInfo(): String{
        var result = ""
        runBlocking {
            result = commServer.getInfoBackGroundRunner("UTF-8")
            Log.i("GET",result)
        }
        return result
    }

    @UiThread
    private fun postInfo(): String{ //posttest
        val postTxt = findViewById<TextView>(R.id.postText)
        var result = ""
        runBlocking { // postして結果が返ってくるまで待機
            result = commServer.postInfoBackGroundRunner("UTF-8")
            Log.i("POST",result)
            postTxt.text = result
        }
        return result
    }

    @UiThread
    private fun loginInfo(): String{ //posttest
        var result = ""
        runBlocking { // postして結果が返ってくるまで待機
            result = commServer.postInfoBackGroundRunner("UTF-8")
            Log.i("POST",result)
        }
        return result
    }

    private fun incorrectSignIn(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("●サインイン失敗")
            .setMessage("ユーザ名もしくはパスワードが間違っています")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun checkCorrectEntered(userName:String, password:String) : Boolean {
        Log.e(">>>","USERNAME:$userName, PASSWORD:$password")

        /* 文字列が入力されていない */
        if(userName.isEmpty() || password.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("●サインイン失敗")
                .setMessage("ユーザ名もしくはパスワードが入力されていません")
                .setPositiveButton("OK") { _, _ -> }
                .show()
            return false
        }

        /* ユーザ名の確認 */
        if(!DataChecker.isUserId(userName, this)) return false

        return true
    }

    private fun hashSHA256String(target: String): String {
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(target.toByteArray())
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(hashBytes.size * 2)
        hashBytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    fun saveAccount() {
        val editor :SharedPreferences.Editor = getSharedPreferences("ID_STORAGE", MODE_PRIVATE).edit()
        val primaryUserNumber = findViewById<EditText>(R.id.userName).text.toString()
        val primaryPassword = findViewById<EditText>(R.id.passWord).text.toString()
        editor.putString("PRIMARY_USERNUMBER", primaryUserNumber)
        editor.putString("PRIMARY_PASSWORD", primaryPassword)
        editor.commit()
    }

}