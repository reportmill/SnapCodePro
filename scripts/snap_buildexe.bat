
REM SET PATH=%PATH%;"C:\Program Files (x86)\Inno Setup 5"

echo "Creating SnapCode.exe"
pushd Z:\Temp\SnapCode
"C:\Program Files\Java\jdk1.8.0_40\bin\javapackager" -deploy -native exe ^
-outdir "C:\Users\Jeff\SnapApp" -outfile SnapCode -name SnapCode ^
-appclass snapcodepro.app.AppLoader -v -srcdir "Z:\Temp\SnapCode\bin" ^
-srcfiles AppLoader.jar;SnapCode1.jar;jgit.jar;jsch.jar;tools.jar;spell.jar;BuildInfo.txt

echo "Signing SnapCode.exe"
Z:\Temp\Signtool\signtool sign /f Z:\Temp\Signtool\RMComoCert.pfx /p rmcomodo ^
/t http://timestamp.verisign.com/scripts/timstamp.dll C:\Users\Jeff\SnapApp\bundles\SnapCode.exe

echo "Verify Signing SnapCode.exe"
Z:\Temp\Signtool\signtool verify /v /pa C:\Users\Jeff\SnapApp\bundles\SnapCode.exe
