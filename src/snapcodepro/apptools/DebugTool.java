package snapcodepro.apptools;
import javakit.project.Breakpoint;
import javakit.project.Project;
import javakit.project.ProjectFiles;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import snapcodepro.app.*;
import snapcodepro.debug.DebugApp;
import snapcodepro.debug.RunApp;
import snapcodepro.project.ProjectSet;
import snapcodepro.project.ProjectX;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This project tool class handles running/debugging a process.
 */
public class DebugTool extends ProjectTool {

    // The last executed file
    private static WebFile  _lastRunFile;

    /**
     * Constructor.
     */
    public DebugTool(ProjectPane projectPane)
    {
        super(projectPane);
    }

    /**
     * Run application.
     */
    public void runDefaultConfig(boolean withDebug)
    {
        WebSite site = getRootSite();
        RunConfig config = RunConfigs.get(site).getRunConfig();
        runConfigOrFile(config, null, withDebug);
    }

    /**
     * Run application.
     */
    public void runConfigForName(String configName, boolean withDebug)
    {
        RunConfigs runConfigs = RunConfigs.get(getRootSite());
        RunConfig runConfig = runConfigs.getRunConfig(configName);
        if (runConfig != null) {
            runConfigs.getRunConfigs().remove(runConfig);
            runConfigs.getRunConfigs().add(0, runConfig);
            runConfigs.writeFile();
            runDefaultConfig(false);
        }
    }

    /**
     * Runs a given RunConfig or file as a separate process.
     */
    public void runConfigOrFile(RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Automatically save all files
        AppPane appPane = (AppPane) _projPane;
        AppFilesPane filesPane = appPane.getFilesPane();
        filesPane.saveAllFiles();
        appPane.saveFiles();

        // Get site and RunConfig (if available)
        WebSite site = getRootSite();
        RunConfig config = aConfig != null || aFile != null ? aConfig : RunConfigs.get(site).getRunConfig();

        // Get file
        WebFile runFile = aFile;
        if (runFile == null && config != null)
            runFile = site.createFileForPath(config.getMainFilePath(), false);
        if (runFile == null)
            runFile = _lastRunFile;
        if (runFile == null)
            runFile = getSelFile();

        // Try to replace file with project file
        Project proj = ProjectX.getProjectForFile(runFile);
        if (proj == null) {
            System.err.println("DebugTool: not project file: " + runFile);
            return;
        }

        // Get class file for given file (should be JavaFile)
        ProjectFiles projectFiles = proj.getProjectFiles();
        WebFile classFile;
        if (runFile.getType().equals("java"))
            classFile = projectFiles.getClassFileForJavaFile(runFile);

            // Try generic way to get class file
        else classFile = projectFiles.getBuildFile(runFile.getPath(), false, runFile.isDir());

        // If ClassFile found, set run file
        if (classFile != null)
            runFile = classFile;

        // Set last run file
        _lastRunFile = runFile;

        // Run/debug file
        String[] runArgs = getRunArgs(proj, config, runFile, isDebug);
        WebURL url = runFile.getURL();
        runAppForArgs(runArgs, url, isDebug);
    }

    /**
     * Runs the provided file as straight app.
     */
    public void runAppForArgs(String[] args, WebURL aURL, boolean isDebug)
    {
        // Print run command to console
        String commandLineStr = String.join(" ", args);
        if (isDebug)
            commandLineStr = "debug " + commandLineStr;
        System.err.println(commandLineStr);

        // Get process
        RunApp proc;
        if (!isDebug)
            proc = new RunApp(aURL, args);
        else {
            proc = new DebugApp(aURL, args);
            Project proj = getProject();
            for (Breakpoint breakpoint : proj.getBreakpoints())
                proc.addBreakpoint(breakpoint);
        }

        // Create RunApp and exec
        ProcPane procPane = ((AppPane) _projPane).getProcPane();
        procPane.execProc(proc);
    }

    /**
     * Returns an array of args for given config and file.
     */
    private static String[] getRunArgs(Project aProj, RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Get basic run command and add to list
        List<String> commands = new ArrayList<>();

        // If not debug, add Java command path
        if (!isDebug) {
            String javaCmdPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            commands.add(javaCmdPath);
        }

        // Get Class path and add to list
        ProjectSet projectSet = ((ProjectX) aProj).getProjectSet();
        String[] classPaths = projectSet.getClassPaths();
        String[] classPathsNtv = FilePathUtils.getNativePaths(classPaths);
        String classPath = FilePathUtils.getJoinedPath(classPathsNtv);
        commands.add("-cp");
        commands.add(classPath);

        // Add class name
        String className = aProj.getClassNameForFile(aFile);
        commands.add(className);

        // Add App Args
        String appArgs = aConfig != null ? aConfig.getAppArgs() : null;
        if (appArgs != null && appArgs.length() > 0)
            commands.add(appArgs);

        // Return commands
        return commands.toArray(new String[0]);
    }
}
