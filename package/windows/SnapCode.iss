;This file will be executed next to the application bundle image
;I.e. current directory will contain folder SnapCode with application files
[Setup]
AppId={{SnapCode}}
AppName=SnapCode
AppVersion=1.0
AppVerName=SnapCode 1.0
AppPublisher=SnapCode
AppComments=
AppCopyright=
;AppPublisherURL=http://reportmill.com/
;AppSupportURL=http://reportmill.com/
;AppUpdatesURL=http://reportmill.com/
DefaultDirName={localappdata}\SnapCode
DisableStartupPrompt=Yes
DisableDirPage=Auto
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=Yes
DefaultGroupName=ReportMill
;Optional License
LicenseFile=
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=SnapCode
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=SnapCode\SnapCode.ico
UninstallDisplayIcon={app}\SnapCode.ico
UninstallDisplayName=SnapCode
WizardImageStretch=No
WizardSmallImageFile=SnapCode-setup-icon.bmp   

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "SnapCode\SnapCode.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "SnapCode\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userstartmenu}\SnapCode"; Filename: "{app}\SnapCode.exe"; IconFilename: "{app}\SnapCode.ico"; Check: returnTrue()
Name: "{commondesktop}\SnapCode"; Filename: "{app}\SnapCode.exe";  IconFilename: "{app}\SnapCode.ico"; Check: returnTrue()

[Run]
Filename: "{app}\SnapCode.exe"; Description: "{cm:LaunchProgram,SnapCode}"; Flags: nowait postinstall skipifsilent

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
