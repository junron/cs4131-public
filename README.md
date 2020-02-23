# Microsoft Authentication
Preconfigured Microsoft Office Authentication for Android

## Installation:

1. Add jitpack.io to **root** `build.gradle`
    ```gradle
    allprojects {
        repositories {
            jcenter()
            maven { url "https://jitpack.io" }
        }
    }
    ```
1. Add dependency to `app/build.gradle`
    ```gradle
    implementation 'com.github.junron:cs4131-public:ms-auth-SNAPSHOT'
    ```
1. Add view:
    ```xml
    <com.example.msauth.MicrosoftLogin
        android:id="@+id/microsoftLogin"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
    ```
1. Add code to Activity:
    **Kotlin**
    ```kotlin
    // Initialize Volley
    Auth.init(applicationContext)

    microsoftLogin.loginUser(appName = "App name") {
       println("Name: " + it["name"]?.asString())
    }
    ```
   **Java**
   ```java
   Auth.init(getApplicationContext());

   MicrosoftLogin login = findViewById(R.id.microsoftLogin);
   login.loginUser("Hello, World",claims -> {
      Log.d("Login", claims.get("name").asString());
      return Unit.INSTANCE;
   });
   ```
1. Done!