@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
if not defined JAVA_HOME goto useJava
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto runMaven
echo JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
exit /b 1

:useJava
set JAVA_EXE=java.exe

:runMaven
"%JAVA_EXE%" %MAVEN_OPTS% %MAVEN_ARGS% -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
if ERRORLEVEL 1 exit /b %ERRORLEVEL%
endlocal
