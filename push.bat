@echo off
set REPO=https://github.com/adeetyap1/Nereus-ProjectV2.git
if not "%1"=="" set REPO=%1
git remote remove origin 2>nul
git remote add origin %REPO%
git push -u origin main
pause
