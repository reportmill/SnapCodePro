package snapcodepro.project;

import javakit.ide.ProjectConfig;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a project and the set of projects it depends on.
 */
public class ProjectSet {

    // The master project
    Project _proj;

    // The list of projects this project depends on
    Project[] _projects;

    // The array of class paths and library paths
    String[] _cpaths, _lpaths;

    /**
     * Creates a new ProjectSet for given Project.
     */
    public ProjectSet(Project aProj)
    {
        _proj = aProj;
    }

    /**
     * Returns the project.
     */
    public Project getProject()
    {
        return _proj;
    }

    /**
     * Returns the list of projects this project depends on.
     */
    public Project[] getProjects()
    {
        // If already set, just return
        if (_projects != null) return _projects;

        // Create list of projects from ClassPath.ProjectPaths
        ProjectConfig projConfig = _proj.getProjectConfig();
        String[] projPaths = projConfig.getProjectPaths();
        List<Project> projs = new ArrayList<>();

        // Get parent site
        WebSite projectSite = _proj.getSite();
        WebSite parSite = projectSite.getURL().getSite(); // Get parent site

        // Iterate over project paths
        for (String projPath : projPaths) {

            // Get URL and site for project path
            WebURL projURL = parSite.getURL(projPath);
            WebSite projSite = projURL.getAsSite();

            // Get Project
            Project proj = Project.getProjectForSite(projSite);
            if (proj == null)
                proj = new Project(projSite);
            proj._parent = _proj;

            // Add to list
            ProjectSet childProjectSet = proj.getProjectSet();
            Project[] childProjects = childProjectSet.getProjects();
            ListUtils.addAllUnique(projs, childProjects);
            ListUtils.addUnique(projs, proj);
        }

        // Return list
        return _projects = projs.toArray(new Project[0]);
    }

    /**
     * Adds a dependent project.
     */
    public void addProject(String aPath)
    {
        // Get project path
        String projPath = aPath;
        if (!projPath.startsWith("/"))
            projPath = '/' + projPath;

        // Add to ProjectConfig
        ProjectConfig projConfig = _proj.getProjectConfig();
        projConfig.addSrcPath(projPath);

        // Clear caches
        _projects = null;
        _cpaths = _lpaths = null;
    }

    /**
     * Removes a dependent project.
     */
    public void removeProject(String aPath)
    {
        ProjectConfig projConfig = _proj.getProjectConfig();
        projConfig.removeSrcPath(aPath);

        // Clear caches
        _projects = null;
        _cpaths = _lpaths = null;
    }

    /**
     * Returns the child project with given name.
     */
    public Project getProject(String aName)
    {
        String name = aName;
        if (name.startsWith("/")) name = name.substring(1);
        for (Project proj : getProjects())
            if (proj.getName().equals(name))
                return proj;
        return null;
    }

    /**
     * Returns a file for given path.
     */
    public WebFile getFile(String aPath)
    {
        WebFile file = _proj.getFile(aPath);
        if (file != null) return file;
        for (Project p : getProjects()) {
            file = p.getFile(aPath);
            if (file != null) return file;
        }
        return null;
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath)
    {
        // Look for file in root project, then dependent projects
        WebFile file = _proj.getSourceFile(aPath, false, false);
        if (file == null)
            for (Project proj : getProjects())
                if ((file = proj.getSourceFile(aPath, false, false)) != null) break;
        return file;
    }

    /**
     * Returns the build file for given path.
     */
    public WebFile getBuildFile(String aPath)
    {
        // Look for file in root project, then dependent projects
        WebFile file = _proj.getBuildFile(aPath, false, false);
        if (file == null)
            for (Project proj : getProjects())
                if ((file = proj.getBuildFile(aPath, false, false)) != null) break;
        return file;
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        // If already set, just return
        if (_cpaths != null) return _cpaths;

        // Get Project ClassPaths
        String[] classPaths = _proj.getClassPaths();

        // Get dependent projects
        Project[] projs = getProjects();
        if (projs.length == 0)
            return _cpaths = classPaths;

        // Get list for LibPaths with base paths
        List<String> classPathsList = new ArrayList<>();
        Collections.addAll(classPathsList, classPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projs) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(classPathsList, projClassPaths);
        }

        // Set/return
        return _cpaths = classPathsList.toArray(new String[0]);
    }

    /**
     * Returns the paths needed to compile/run project, except build directory.
     */
    public String[] getLibPaths()
    {
        // If already set, just return
        if (_lpaths != null) return _lpaths;

        // Get LibPaths for this proj
        ProjectConfig projConfig = _proj.getProjectConfig();
        String[] libPaths = projConfig.getLibPathsAbsolute();

        // Get dependent projects (if none, just return LibPaths)
        Project[] projs = getProjects();
        if (projs.length == 0)
            return _lpaths = libPaths;

        // Get list for LibPaths with base paths
        List<String> libPathsList = new ArrayList<>();
        Collections.addAll(libPathsList, libPaths);

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : projs) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(libPathsList, projClassPaths);
        }

        // Set/return
        return _lpaths = libPathsList.toArray(new String[0]);
    }

    /**
     * Adds a build file.
     */
    public void addBuildFilesAll()
    {
        _proj.addBuildFilesAll();
        for (Project p : getProjects())
            p.addBuildFilesAll();
    }

    /**
     * Builds the project.
     */
    public boolean buildProjects(TaskMonitor aTM)
    {
        boolean success = true;
        for (Project p : getProjects())
            if (!p.buildProject(aTM)) {
                success = false;
                break;
            }
        if (success)
            success = _proj.buildProject(aTM);

        // Find unused imports
        _proj.findUnusedImports();
        for (Project p : getProjects()) p.findUnusedImports();
        return success;
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFile(String aClassName)
    {
        WebFile file = _proj.getJavaFileForClassName(aClassName);
        if (file != null) return file;
        for (Project p : getProjects()) {
            file = p.getJavaFileForClassName(aClassName);
            if (file != null) return file;
        }
        return null;
    }

}