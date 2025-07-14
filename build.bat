@echo off
echo [BUILD] Compiling project...

REM pastikan ada folder build/
if not exist build (
    mkdir build
)

REM compile semua file .java ke folder build/
javac -cp "lib\emoji-java-5.1.1.jar;lib\json-20250517.jar" -d build ^
lib\EmojiPicker.java ^
src\chat\client\ChatClient.java ^
src\chat\server\ChatServer.java ^
src\GUI\ClientUI.java

if %errorlevel% neq 0 (
    echo [BUILD] Failed.
    pause
    exit /b
)

echo [BUILD] Success.
pause
