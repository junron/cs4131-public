package com.example.msauth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.msauth.util.Auth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Auth.init(applicationContext)

        microsoftLogin.loginUser("Hi") {
            println(it["name"]?.asString())
        }
    }

}
