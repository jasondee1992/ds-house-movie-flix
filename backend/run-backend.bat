@echo off
setlocal

title HomeFlix Backend
cd /d "%~dp0"

set "PYTHON_EXE=%~dp0..\.venv\Scripts\python.exe"
if exist "%PYTHON_EXE%" goto :start

set "PYTHON_EXE=%~dp0.venv\Scripts\python.exe"
if exist "%PYTHON_EXE%" goto :start

echo.
echo [ERROR] Hindi makita ang Python virtual environment.
echo.
echo Inaasahang makita ito sa isa sa mga sumusunod:
echo   %~dp0..\.venv\Scripts\python.exe
echo   %~dp0.venv\Scripts\python.exe
echo.
echo I-setup muna ang project virtual environment at dependencies.
pause
exit /b 1

:start
echo.
echo Starting HomeFlix backend...
echo API:  http://localhost:8000
echo Docs: http://localhost:8000/docs
echo.
echo Pindutin ang Ctrl+C para ihinto ang server.
echo.

"%PYTHON_EXE%" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if not "%EXIT_CODE%"=="0" echo [ERROR] Huminto ang backend with exit code %EXIT_CODE%.
if "%EXIT_CODE%"=="0" echo Huminto na ang backend.
pause
exit /b %EXIT_CODE%
