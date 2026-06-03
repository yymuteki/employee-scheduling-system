@echo off
:: Use JDK 17 - adjust this path if your JDK 17 is installed elsewhere
set JAVA_HOME=E:\JAVA
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo 错误: 找不到 JDK 17，请修改 start.bat 中的 JAVA_HOME 路径
    pause
    exit /b 1
)
%JAVA_HOME%\bin\java -version 2>&1 | findstr "17" >nul
if errorlevel 1 (
    echo 错误: 需要 JDK 17，当前 JAVA_HOME 版本不是 17
    pause
    exit /b 1
)
set DEEPSEEK_API_KEY=sk-cb48db4721fb4e5a9429aa751ea68780
echo Starting Schedule App...
"%JAVA_HOME%\bin\java" -jar target\schedule-app-1.0.0.jar
pause
