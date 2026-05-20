@echo off
setlocal EnableExtensions

pushd "%~dp0"
set "PROJECT_DIR=%CD%"

rem --- FORCE JDK 17 (ignore any pre-set Java 8) ---
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME=C:\Program Files (x86)\Android\openjdk\jdk-17.0.14"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [!] Could not find a JDK 17 at the expected paths.
  echo     Set JAVA_HOME manually to a JDK 17 and re-run.
  exit /b 1
)

set "ANDROID_SDK_ROOT=C:\Program Files (x86)\Android\android-sdk"
set "SDKM12=%ANDROID_SDK_ROOT%\cmdline-tools\12.0\bin\sdkmanager.bat"
set "PATH=%JAVA_HOME%\bin;%ANDROID_SDK_ROOT%\platform-tools;%PATH%"

echo === PROJECT     : %PROJECT_DIR%
echo === SDK root    : %ANDROID_SDK_ROOT%
echo === sdkmanager  : %SDKM12%
echo === JAVA_HOME   : %JAVA_HOME%
echo.

if not exist "%SDKM12%" (
  echo [!] sdkmanager not found at "%SDKM12%"
  exit /b 2
)
if not exist "%PROJECT_DIR%\gradlew.bat" (
  echo [!] gradlew.bat not found in "%PROJECT_DIR%"
  exit /b 3
)

echo [*] Ensuring cmdline-tools;latest...
"%SDKM12%" --sdk_root="%ANDROID_SDK_ROOT%" --install "cmdline-tools;latest" || (echo [!] failed & exit /b 10)

set "SDKM=%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat"
if not exist "%SDKM%" set "SDKM=%SDKM12%"

echo [*] Installing platform-tools...
"%SDKM%" --sdk_root="%ANDROID_SDK_ROOT%" --install "platform-tools" || (echo [!] failed & exit /b 11)

echo [*] Installing platforms;android-34...
"%SDKM%" --sdk_root="%ANDROID_SDK_ROOT%" --install "platforms;android-34" || (echo [!] failed & exit /b 12)

echo [*] Installing build-tools;34.0.0...
"%SDKM%" --sdk_root="%ANDROID_SDK_ROOT%" --install "build-tools;34.0.0" || (echo [!] failed & exit /b 13)

echo [*] Accept licenses (answer 'y' to each)...
"%SDKM%" --sdk_root="%ANDROID_SDK_ROOT%" --licenses
if errorlevel 1 ( echo [!] license accept failed & exit /b 14 )

echo [*] Building with JDK: %JAVA_HOME%
set "ORG_GRADLE_JAVA_HOME=%JAVA_HOME%"
call gradlew.bat --no-daemon -Dorg.gradle.java.home="%JAVA_HOME%" assembleDebug
set "ERR=%ERRORLEVEL%"
if not "%ERR%"=="0" ( echo [!] Build failed: %ERR% & exit /b %ERR% )

echo [OK] Build finished. Check app\build\outputs\apk\debug\
popd
exit /b 0
