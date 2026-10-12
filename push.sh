#!/bin/sh
REPO="${1:-https://github.com/adeetyap1/Nereus-ProjectV2.git}"
git remote remove origin 2>/dev/null
git remote add origin "$REPO"
git push -u origin main
