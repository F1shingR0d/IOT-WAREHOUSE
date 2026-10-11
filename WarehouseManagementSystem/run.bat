@echo off
rem ============================================================
rem  I.O.T Warehouse (Warehouse Management System) - compile and run without NetBeans
rem  Requires JDK 17 or newer (javac and java on the PATH).
rem ============================================================
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] The Java JDK was not found. Install JDK 17 or newer and try again.
    pause
    exit /b 1
)

echo Compiling I.O.T Warehouse...
if not exist build\classes mkdir build\classes
javac -encoding UTF-8 --release 17 -d build\classes -cp "lib\*" src\*.java src\model\*.java src\dao\*.java src\database\*.java src\util\*.java src\ui\*.java src\ui\components\*.java
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)
if not exist build\classes\database mkdir build\classes\database
copy /y src\database\*.sql build\classes\database\ >nul
if not exist build\classes\ui\resources mkdir build\classes\ui\resources
copy /y src\ui\resources\*.png build\classes\ui\resources\ >nul

echo Starting the program...
start "I.O.T Warehouse" javaw -cp "build\classes;lib\*" Main
endlocal
