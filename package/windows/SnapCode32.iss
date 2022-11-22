;This file will be executed next to the application bundle image
;I.e. current directory will contain folder SnapCode with application files
[Setup]
AppId={{SnapCode32}}
AppName=SnapCode32
AppVersion=1.0
AppVerName=SnapCode32 1.0
AppPublisher=SnapCode
AppComments=
AppCopyright=
;AppPublisherURL=http://reportmill.com/
;AppSupportURL=http://reportmill.com/
;AppUpdatesURL=http://reportmill.com/
DefaultDirName={localappdata}\SnapCode32
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
OutputBaseFilename=SnapCode32
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=SnapCode32\SnapCode32.ico
UninstallDisplayIcon={app}\SnapCode32.ico
UninstallDisplayName=SnapCode32
WizardImageStretch=No
WizardSmallImageFile=SnapCode32-setup-icon.bmp   

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "SnapCode32\SnapCode32.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "SnapCode32\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userstartmenu}\SnapCode32"; Filename: "{app}\SnapCode32.exe"; IconFilename: "{app}\SnapCode32.ico"; Check: returnTrue()
Name: "{commondesktop}\SnapCode32"; Filename: "{app}\SnapCode32.exe";  IconFilename: "{app}\SnapCode32.ico"; Check: returnTrue()

[Run]
Filename: "{app}\SnapCode32.exe"; Description: "{cm:LaunchProgram,SnapCode32}"; Flags: nowait postinstall skipifsilent

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
