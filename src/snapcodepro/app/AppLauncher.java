package snapcodepro.app;

import javakit.project.Breakpoint;
import javakit.project.ProjectFiles;
import snapcodepro.debug.DebugApp;
import snapcodepro.debug.RunApp;
import snapcodepro.project.ProjectX;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snap.web.WebURL;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to launch Snap apps.
 */
public class AppLauncher {

    // The run config
    RunConfig _config;

    // The file to launch
    WebFile _file;

    // The url to launch
    WebURL _url;

    // The Project
    ProjectX _proj;

    // The last executed file
    static WebFile _lastRunFile;

    /**
     * Returns the WebFile.
     */
    public WebFile getFile()
    {
        return _file;
    }

    /**
     * Returns the WebURL.
     */
    public WebURL getURL()
    {
        return _file.getURL();
    }

    /**
     * Returns the URL String.
     */
    public String getURLString()
    {
        return getURL().getString();
    }

    /**
     * Returns the app args.
     */
    public String getAppArgs()
    {
        return _config != null ? _config.getAppArgs() : null;
    }

    /**
     * Runs the provided file for given run mode.
     */
    public void runFile(AppPane anAppPane, RunConfig aConfig, WebFile aFile, boolean isDebug)
    {
        // Have AppPane save files
        anAppPane.saveFiles();

        // Get file
        WebFile runFile = aFile;

        // Try to replace file with project file
        _proj = ProjectX.getProjectForFile(runFile);
        if (_proj == null) {
            System.err.println("AppLauncher: not project file: " + runFile);
            return;
        }

        // Get class file for given file (should be JavaFile)
        WebFile classFile;
        if (runFile.getType().equals("java")) {
            ProjectFiles projectFiles = _proj.getProjectFiles();
            classFile = projectFiles.getClassFileForJavaFile(runFile);
        }

        // Try generic way to get class file
        else {
            ProjectFiles projectFiles = _proj.getProjectFiles();
            classFile = projectFiles.getBuildFile(runFile.getPath(), false, runFile.isDir());
        }

        // If ClassFile found, set run file
        if (classFile != null)
            runFile = classFile;

        // Set URL
        _file = runFile;
        _url = _file.getURL();
        _config = aConfig;

        // Set last run file
        _lastRunFile = aFile;

        // Run/debug file
        if (isDebug)
            debugApp(anAppPane);
        else runApp(anAppPane);
    }

    /**
     * Runs the provided file as straight app.
     */
    void runApp(AppPane anAppPane)
    {
        // Get run command as string array
        List<String> commands = getCommand();
        String[] command = commands.toArray(new String[commands.size()]);

        // Print run command to console
        System.err.println(String.join(" ", command));

        // Create RunApp and exec
        RunApp proc = new RunApp(getURL(), command);
        anAppPane.getProcPane().execProc(proc);
    }

    /**
     * Runs the provided file as straight app.
     */
    void debugApp(AppPane anAppPane)
    {
        // Get run command as string array (minus actual run)
        List<String> commands = getDebugCommand();
        String[] command = commands.toArray(new String[commands.size()]);

        // Print run command to console
        System.err.println("debug " + String.join(" ", command));

        // Create DebugApp and add project breakpoints
        DebugApp proc = new DebugApp(getURL(), command);
        for (Breakpoint bp : _proj.getBreakpoints())
            proc.addBreakpoint(bp);

        // Add app to process pane and exec
        anAppPane.getProcPane().execProc(proc);
    }

    /**
     * Returns an array of args.
     */
    protected List<String> getCommand()
    {
        // Get basic run command and add to list
        List<String> commands = new ArrayList();
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        commands.add(java);

        // Get Class path and add to list
        String[] cpaths = _proj.getProjectSet().getClassPaths(), cpathsNtv = FilePathUtils.getNativePaths(cpaths);
        String cpath = FilePathUtils.getJoinedPath(cpathsNtv);
        commands.add("-cp");
        commands.add(cpath);

        // Add class name
        commands.add(_proj.getClassNameForFile(getFile()));

        // Add App Args
        if (getAppArgs() != null && getAppArgs().length() > 0)
            commands.add(getAppArgs());

        // Return commands
        return commands;
    }

    /**
     * Returns an array of args.
     */
    protected List<String> getDebugCommand()
    {
        List<String> cmd = getCommand();
        cmd.remove(0);
        return cmd;
    }

    /**
     * Returns the last run file.
     */
    public static WebFile getLastRunFile()
    {
        return _lastRunFile;
    }

}