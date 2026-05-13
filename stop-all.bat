@echo off
echo Stopping all Java services...

taskkill /F /IM java.exe

echo All services stopped.
pause