package com.example.msauth;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.msauth.util.Auth;

import kotlin.Unit;

public class MainActivityJava extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Auth.init(getApplicationContext());

        MicrosoftLogin login = findViewById(R.id.microsoftLogin);
        login.loginUser("Hello, World",claims -> {
            Log.d("Login", claims.get("name").asString());
            return Unit.INSTANCE;
        });
    }
}
