/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcodepro.project;
import javakit.ide.Project;
import javakit.resolver.Resolver;
import javakit.ide.Breakpoints;
import javakit.ide.BuildIssues;
import javakit.ide.ProjectConfig;
import snap.util.FilePathUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;

/**
 * A class to manage build attributes and behavior for a WebSite.
 */
public class ProjectX extends javakit.ide.Project {

    // The project that loaded us
    protected ProjectX _parent;

    // The set of projects this project depends on
    private ProjectSet  _projSet = new ProjectSet(this);

    // A class to read .classpath file
    private ProjectConfigFile  _projConfigFile;

    // The JavaFileBuilder
    private JavaFileBuilder  _javaFileBuilder = new JavaFileBuilder(this);

    // The default file builder
    private ProjectFileBuilder _defaultFileBuilder = new ProjectFileBuilder.DefaultBuilder(this);

    // The last build date
    private Date  _buildDate;

    // The list of Breakpoints
    private Breakpoints  _bpoints;

    /**
     * Creates a new Project for WebSite.
     */
    public ProjectX(WebSite aSite)
    {
        super(aSite);

        // Load dependent projects
        getProjects();
    }

    /**
     * Returns the parent project for this project.
     */
    public ProjectX getParent()
    {
        return _parent;
    }

    /**
     * Returns the top most project.
     */
    public ProjectX getRootProject()
    {
        return _parent != null ? _parent.getRootProject() : this;
    }

    /**
     * Returns the list of projects this project depends on.
     */
    public ProjectX[] getProjects()
    {
        return _projSet.getProjects();
    }

    /**
     * Returns the set of projects this project depends on.
     */
    public ProjectSet getProjectSet()
    {
        return _projSet;
    }

    /**
     * Returns the project class loader.
     */
    protected ClassLoader createClassLoader()
    {
        // If RootProject, return RootProject.ClassLoader
        ProjectX rproj = getRootProject();
        if (rproj != this)
            return rproj.createClassLoader();

        // Get all project ClassPath URLs
        ProjectSet projectSet = getProjectSet();
        String[] projSetClassPaths = projectSet.getClassPaths();
        URL[] urls = FilePathUtils.getURLs(projSetClassPaths);

        // Get System ClassLoader
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        ClassLoader urlClassLoader = new ProjectClassLoaderX(urls, sysClassLoader);

        // Return
        return urlClassLoader;
    }

    /**
     * Returns the build file for given path.
     */
    public WebFile getBuildFile(String aPath, boolean doCreate, boolean isDir)
    {
        return _projFiles.getBuildFile(aPath, doCreate, isDir);
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFileForClassName(String aClassName)
    {
        return _projFiles.getJavaFileForClassName(aClassName);
    }

    /**
     * Returns the Java for a class file, if it can be found.
     */
    public WebFile getJavaFileForClassFile(WebFile aClassFile)
    {
        return _projFiles.getJavaFileForClassFile(aClassFile);
    }

    /**
     * Returns the class file for a given Java file.
     */
    public WebFile getClassFileForJavaFile(WebFile aJavaFile)
    {
        return _projFiles.getClassFileForJavaFile(aJavaFile);
    }

    /**
     * Returns the class files for a given Java file.
     */
    public WebFile[] getClassFilesForJavaFile(WebFile aJavaFile)
    {
        return _projFiles.getClassFilesForJavaFile(aJavaFile);
    }

    /**
     * Needs unique name so that when debugging SnapCode, we can ignore classes loaded by Project.
     */
    public static class ProjectClassLoaderX extends URLClassLoader {
        public ProjectClassLoaderX(URL[] urls, ClassLoader aPar)
        {
            super(urls, aPar);
        }
    }

    /**
     * Returns the project class loader.
     */
    public ClassLoader createLibClassLoader()
    {
        // Create ClassLoader for ProjectSet.ClassPath URLs and SystemClassLoader.Parent and return
        ProjectSet projectSet = getProjectSet();
        String[] libPaths = projectSet.getLibPaths();
        URL[] urls = FilePathUtils.getURLs(libPaths);

        // Get System ClassLoader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create special URLClassLoader subclass so when debugging SnapCode, we can ignore classes loaded by Project
        return new ProjectClassLoaderX(urls, systemClassLoader);
    }

    /**
     * Clears the class loader.
     */
    protected void clearClassLoader()
    {
        // If ClassLoader closeable, close it
        if (_classLoader instanceof Closeable)
            try {  ((Closeable) _classLoader).close(); }
            catch (Exception e) { throw new RuntimeException(e); }

        // Clear
        _classLoader = null;
        _resolver = null;

        // If parent, forward on
        ProjectX parent = getParent();
        if (parent != null)
            parent.clearClassLoader();
    }

    /**
     * Returns the class for given file.
     */
    public Class getClassForFile(WebFile aFile)
    {
        String className = getClassNameForFile(aFile);
        Resolver resolver = getResolver();
        return resolver.getClassForName(className);
    }

    /**
     * Returns the breakpoints.
     */
    public Breakpoints getBreakpoints()
    {
        // If already set, just return
        if (_bpoints != null) return _bpoints;

        // Create, set, return
        Breakpoints bpoints = new Breakpoints(this);
        return _bpoints = bpoints;
    }

    /**
     * Override to create ProjectConfig from .classpath file.
     */
    @Override
    protected ProjectConfig createProjectConfig()
    {
        // Create ProjectConfigFile
        _projConfigFile = new ProjectConfigFile(this);

        // Read config from file
        ProjectConfig projConfig = _projConfigFile.getProjectConfig();

        // Return
        return projConfig;
    }

    /**
     * Returns whether file is config file.
     */
    @Override
    protected boolean isConfigFile(WebFile aFile)
    {
        WebFile configFile = _projConfigFile.getFile();
        return aFile == configFile;
    }

    /**
     * Reads the settings from project settings file(s).
     */
    public void readSettings()
    {
        _projConfigFile.readFile();
    }

    /**
     * Returns the last build date.
     */
    public Date getBuildDate()
    {
        return _buildDate;
    }

    /**
     * Builds the project.
     */
    public boolean buildProject(TaskMonitor aTM)
    {
        // Build files
        boolean buildSuccess = _javaFileBuilder.buildFiles(aTM);
        buildSuccess |= _defaultFileBuilder.buildFiles(aTM);
        _buildDate = new Date();

        // Return build success
        return buildSuccess;
    }

    /**
     * Finds unused imports from last set of compiled files.
     */
    public void findUnusedImports()
    {
        _javaFileBuilder.findUnusedImports();
    }

    /**
     * Interrupts build.
     */
    public void interruptBuild()
    {
        _javaFileBuilder._interrupt = true;
    }

    /**
     * Removes all build files from project.
     */
    public void cleanProject()
    {
        // If separate build directory, just delete it
        WebFile buildDir = getBuildDir();
        if (buildDir != getSourceDir() && buildDir != getSite().getRootDir()) {

            // Delete BuildDir
            try {
                if (buildDir.getExists())
                    buildDir.delete();
            }

            // Handle Exceptions
            catch (Exception e) { throw new RuntimeException(e); }
        }

        // Otherwise, remove all class files from build directory
        else removeBuildFiles(buildDir);
    }

    /**
     * Returns the file builder for given file.
     */
    public ProjectFileBuilder getFileBuilder(WebFile aFile)
    {
        // If file not in source path, just return
        String filePath = aFile.getPath();
        String sourcePath = getSourceDir().getPath();
        boolean inSrcPath = sourcePath.equals("/") || filePath.startsWith(sourcePath + "/");
        if (!inSrcPath)
            return null;

        // If file already in build path, just return
        String buildPath = getBuildDir().getPath();
        boolean inBuildPath = filePath.startsWith(buildPath + "/") || filePath.equals(buildPath);
        if (inBuildPath)
            return null;

        // Return JavaFileBuilder, DefaultFileBuilder or null
        if (_javaFileBuilder.isBuildFile(aFile))
            return _javaFileBuilder;
        if (_defaultFileBuilder.isBuildFile(aFile))
            return _defaultFileBuilder;

        // Return null since nothing builds given file
        return null;
    }

    /**
     * Adds a build file.
     */
    public void addBuildFilesAll()
    {
        WebFile sourceDir = getSourceDir();
        addBuildFile(sourceDir, true);
    }

    /**
     * Adds a build file.
     */
    public void addBuildFile(WebFile aFile, boolean doForce)
    {
        // If file doesn't exist, just return
        if (!aFile.getExists()) return;
        if (aFile.getName().startsWith(".")) return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == getBuildDir()) return; // If build directory, just return (assuming build dir is in source dir)
            for (WebFile file : aFile.getFiles())
                addBuildFile(file, doForce);
            return;
        }

        // Get FileBuilder for file and add
        ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
        if (fileBuilder == null)
            return;
        boolean needsBuild = fileBuilder.getNeedsBuild(aFile);
        if (doForce || needsBuild)
            fileBuilder.addBuildFile(aFile);
    }

    /**
     * Adds a build file.
     */
    public void addBuildFileForce(WebFile aFile)
    {
        ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
        if (fileBuilder == null) return;
        fileBuilder.addBuildFile(aFile);
    }

    /**
     * Removes a build file.
     */
    protected void removeBuildFile(WebFile aFile)
    {
        ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
        if (fileBuilder != null) fileBuilder.removeBuildFile(aFile);
    }

    /**
     * Removes all build files from given directory.
     */
    private void removeBuildFiles(WebFile aDir)
    {
        // Get directory files
        WebFile[] dirFiles = aDir.getFiles();

        // Iterate over files and remove class files
        for (int i = dirFiles.length - 1; i >= 0; i--) {

            // Handle Class file
            WebFile file = dirFiles[i];
            if (file.getType().equals("class")) {
                try { file.delete(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
            // Handle Dir: Recurse
            else if (file.isDir())
                removeBuildFiles(file);
        }
    }

    /**
     * Called when file added.
     */
    public void fileAdded(WebFile aFile)
    {
        if (isConfigFile(aFile))
            readSettings();
        addBuildFile(aFile, false);
    }

    /**
     * Called when file removed.
     */
    public void fileRemoved(WebFile aFile)
    {
        // Remove build files
        removeBuildFile(aFile);

        // Remove BuildIssues for file
        ProjectX rootProj = getRootProject();
        BuildIssues buildIssues = rootProj.getBuildIssues();
        buildIssues.removeIssuesForFile(aFile);
    }

    /**
     * Called when file saved.
     */
    public void fileSaved(WebFile aFile)
    {
        // If File is config file, read file
        if (isConfigFile(aFile))
            readSettings();

        // If plain file, add as BuildFile
        if (!aFile.isDir())
            addBuildFile(aFile, false);
    }

    /**
     * Deletes the project.
     */
    public void deleteProject(TaskMonitor aTM) throws Exception
    {
        // Start TaskMonitor
        aTM.startTasks(1);
        aTM.beginTask("Deleting files", -1);

        // Clear ClassLoader
        clearClassLoader();

        // Delete SandBox, Site
        WebSite projSite = getSite();
        WebSite projSiteSandbox = projSite.getSandbox();
        projSiteSandbox.deleteSite();
        projSite.deleteSite();

        // Finish TaskMonitor
        aTM.endTask();
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return "Project: " + getSite();
    }

    /**
     * Returns the project for a given site.
     */
    public static ProjectX getProjectForFile(WebFile aFile)
    {
        WebSite fileSite = aFile.getSite();
        return getProjectForSite(fileSite);
    }

    /**
     * Returns the project for a given site.
     */
    public static synchronized ProjectX getProjectForSite(WebSite aSite)
    {
        Project proj = Project.getProjectForSite(aSite);
        return (ProjectX) proj;
    }
}