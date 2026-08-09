@echo off
@REM REM Set Java21 or above for running the application
@REM REM set JAVA_HOME=C:\Program Files\Java\jdk-21
@REM REM set PATH=%JAVA_HOME%\bin;%PATH%
start javaw -jar apibanker-${project.version}.jar
exit

