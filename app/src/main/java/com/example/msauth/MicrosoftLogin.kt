package com.example.msauth

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.auth0.android.jwt.Claim
import com.example.msauth.util.Auth.getAccessToken


class MicrosoftLogin(context: Context, attributeSet: AttributeSet) : WebView(context, attributeSet) {
    fun loginUser(appName: String = "Demo App", callback: (Map<String, Claim>) -> Unit) {
        this.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                if (request.url.toString().startsWith("https://voting.nushhwboard.ml/callback?code=")) {
                    val token = request.url.toString().substringAfter("code=").substringBefore("&")
                    getAccessToken(token, callback)
                }

                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                view.evaluateJavascript("""
                    elem = document.querySelector(".row.app-name")
                    if(elem != null){
                        elem.innerText = "$appName"
                    }
                """.trimIndent()){
                    println(it)
                }
            }
        }
        settings.javaScriptEnabled = true
        loadUrl("https://login.microsoftonline.com/d72a7172-d5f8-4889-9a85-d7424751592a/oauth2/authorize?client_id=29ee72fe-a535-4c12-9b2c-c949873ad500&response_type=code")
    }
}
