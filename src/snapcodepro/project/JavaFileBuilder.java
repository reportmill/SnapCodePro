/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcodepro.project;
import javakit.ide.Project;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.*;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilder implements ProjectFileBuilder {

    // The project we work for
    protected Project  _proj;

    // A list of files to be compiled
    protected Set<WebFile>  _buildFiles = Collections.synchronizedSet(new HashSet<>());

    // Whether to interrupt current build
    protected boolean  _interrupt;

    /**
     * Creates a new JavaFileBuilder for given Project.
     */
    public JavaFileBuilder(Project aProject)
    {
        _proj = aProject;
    }

    /**
     * Returns whether file is build file.
     */
    public boolean isBuildFile(WebFile aFile)
    {
        return aFile.getType().equals("java");
    }

    /**
     * Returns whether given file needs to be built.
     */
    public boolean getNeedsBuild(WebFile aFile)  { return true; }

    /**
     * Adds a compile file.
     */
    public void addBuildFile(WebFile aFile)
    {
        _buildFiles.add(aFile);
    }

    /**
     * Remove a build file.
     */
    public void removeBuildFile(WebFile aFile)
    {
        _buildFiles.remove(aFile);
    }

    /**
     * Compiles files.
     */
    public boolean buildFiles(TaskMonitor aTaskMonitor)  { return true; }

    /**
     * Interrupts build.
     */
    public void interruptBuild()
    {
        _interrupt = true;
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()  { }
}