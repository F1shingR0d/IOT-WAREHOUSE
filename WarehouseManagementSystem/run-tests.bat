@echo off
rem ============================================================
rem  I.O.T Warehouse (Warehouse Management System) - automated tests
rem  Uses a separate test database (build\test-data, port 1533);
rem  the real data in the "data" folder is not touched.
rem ============================================================
setlocal
cd /d "%~dp0"

echo Compiling...
if not exist build\classes mkdir build\classes
if not exist build\test-classes mkdir build\test-classes
javac -encoding UTF-8 --release 17 -d build\classes -cp "lib\*" src\*.java src\model\*.java src\dao\*.java src\database\*.java src\util\*.java src\ui\*.java src\ui\components\*.java
if errorlevel 1 goto failed
if not exist build\classes\database mkdir build\classes\database
copy /y src\database\*.sql build\classes\database\ >nul
if not exist build\classes\ui\resources mkdir build\classes\ui\resources
copy /y src\ui\resources\*.png build\classes\ui\resources\ >nul
javac -encoding UTF-8 --release 17 -d build\test-classes -cp "build\classes;lib\*" test\*.java
if errorlevel 1 goto failed

java -cp "build\test-classes;build\classes;lib\*" WmsTestRunner
pause
exit /b 0

:failed
echo [ERROR] Compilation failed.
pause
exit /b 1
