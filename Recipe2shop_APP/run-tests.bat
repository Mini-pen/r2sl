@echo off
echo ========================================
echo    TherapIA - Suite de Tests Unitaires
echo ========================================
echo.

echo [1/4] Nettoyage des tests précédents...
call gradlew.bat cleanTest

echo.
echo [2/4] Exécution des tests unitaires...
call gradlew.bat test --info

echo.
echo [3/4] Exécution des tests instrumentés...
call gradlew.bat connectedAndroidTest

echo.
echo [4/4] Génération du rapport de tests...
call gradlew.bat jacocoTestReport

echo.
echo ========================================
echo    Tests terminés avec succès !
echo ========================================
echo.
echo Rapports disponibles dans :
echo - app/build/reports/tests/
echo - app/build/reports/jacoco/
echo.
pause
