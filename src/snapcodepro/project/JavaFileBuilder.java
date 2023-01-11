/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcodepro.project;

import javakit.ide.BuildIssue;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;

import java.util.*;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilder implements ProjectFileBuilder {

    // The project we work for
    ProjectX _proj;

    // A list of files to be compiled
    Set<WebFile> _buildFiles = Collections.synchronizedSet(new HashSet());

    // Whether to interrupt current build
    boolean _interrupt;

    // The SnapCompiler used for last compiles
    SnapCompiler _compiler;

    // The final set of compiled files
    Set<WebFile> _compiledFiles, _errorFiles;

    /**
     * Creates a new JavaFileBuilder for given Project.
     */
    public JavaFileBuilder(ProjectX aProject)
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
    public boolean getNeedsBuild(WebFile aFile)
    {
        // See if Java file has out of date Class file
        WebFile classFile = _proj.getClassFileForJavaFile(aFile);
        boolean needsBuild = !classFile.getExists() || classFile.getLastModTime() < aFile.getLastModTime();

        // If not out of date, updateDependencies, compatibilities
        if (!needsBuild && !JavaData.getJavaDataForFile(aFile).isDependenciesSet()) {
            JavaData javaData = JavaData.getJavaDataForFile(aFile);
            javaData.updateDependencies();
            needsBuild = true;
            //int c = updateCompatability(aFile); if(c<0) needsBuild=true; if(c!=-2) jdata.updateDependencies();
        }

        // Return NeedsBuild
        return needsBuild;
    }

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
        // Remove from build files
        _buildFiles.remove(aFile);

        // Get dependent files and add to BuildFiles
        JavaData jdata = JavaData.getJavaDataForFile(aFile);
        Set<WebFile> dependents = jdata.getDependents();
        for (WebFile dependant : dependents)
            if (dependant.getExists())
                addBuildFile(dependant);

        // Remove JavaFile Dependencies
        jdata.removeDependencies();

        // Get JavaFile.ClassFiles and remove them
        WebFile[] cfiles = _proj.getClassFilesForJavaFile(aFile);
        if (cfiles == null) return;
        for (WebFile cfile : cfiles)
            try {
                cfile.delete();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Compiles files.
     */
    public boolean buildFiles(TaskMonitor aTaskMonitor)
    {
        if (_buildFiles.size() == 0) return true;
        List<WebFile> files = new ArrayList(_buildFiles);
        _buildFiles.clear();
        SnapCompiler compiler = new SnapCompiler(_proj);
        Set<WebFile> compiledFiles = new HashSet(), errorFiles = new HashSet();

        // Reset Interrupt flag
        _interrupt = false;

        // Iterate over build files and compile
        boolean compileSuccess = true; //long time = System.currentTimeMillis();
        for (int i = 0; i < files.size(); i++) {
            WebFile file = files.get(i);

            // If interrupted, add remaining build files and return
            if (_interrupt) {
                for (int j = i, jMax = files.size(); j < jMax; j++) addBuildFile(files.get(j));
                return false;
            }

            // Update progress
            if (compiledFiles.contains(file)) continue; //System.err.println("Skipping " + finfo);
            int count = compiledFiles.size() + 1;
            String msg = String.format("Compiling %s (%d of %d)", _proj.getClassNameForFile(file), count, files.size());
            aTaskMonitor.beginTask(msg, -1);

            // Get compile file
            boolean result = compiler.compile(file);
            aTaskMonitor.endTask();

            // If compile failed, re-add file to BuildFiles and continue
            if (!result) {
                compiledFiles.add(file);
                errorFiles.add(file);
                addBuildFile(file);
                if (compiler._errorCount >= 1000) _interrupt = true;
                compileSuccess = false;
                continue;
            }

            // Add Compiler.CompiledFiles to CompiledFiles
            compiledFiles.addAll(compiler.getCompiledJavaFiles());

            // If there were modified files, clear Project.ClassLoader
            if (compiler.getModifiedJavaFiles().size() > 0)
                _proj.clearClassLoader();

            // Iterate over JavaFiles for modified ClassFiles and update
            for (WebFile jfile : compiler.getModifiedJavaFiles()) {

                // Delete class files for removed inner classes
                deleteZombieClassFiles(jfile);

                // Update dependencies and get files that need to be updated
                JavaData jdata = JavaData.getJavaDataForFile(jfile);
                boolean dependsChanged = jdata.updateDependencies();
                if (!dependsChanged) continue;

                // Iterate over Java files dependent on loop JavaFile and mark for update
                Set<WebFile> updateFiles = jdata.getDependents();
                for (WebFile ufile : updateFiles) {
                    ProjectX proj = ProjectX.getProjectForFile(ufile);
                    if (proj == _proj) {
                        if (!compiledFiles.contains(ufile) && !ListUtils.containsId(files, ufile))
                            files.add(ufile);
                    } else proj.addBuildFileForce(ufile);
                }
            }
        }

        // Finalize TaskMonitor
        aTaskMonitor.beginTask("Build Completed", -1);
        aTaskMonitor.endTask();

        // Set compiler/files for findUnusedImports
        _compiler = compiler;
        _compiledFiles = compiledFiles;
        _errorFiles = errorFiles;

        // Finalize ActivityText and return
        //System.out.println("Build time: " + (System.currentTimeMillis()-time)/1000f + " seconds");
        return compileSuccess;
    }

    /**
     * Checks last set of compiled files for unused imports.
     */
    public void findUnusedImports()
    {
        if (_compiler == null) return;
        for (WebFile cfile : _compiledFiles) {
            JavaData jdata = JavaData.getJavaDataForFile(cfile);
            if (_errorFiles.contains(cfile)) continue;
            for (BuildIssue bissue : jdata.getUnusedImports())
                _compiler.report(bissue);
        }
        _compiler = null;
        _compiledFiles = _errorFiles = null;
    }

    /**
     * Delete inner-class class files that were generated in older version of class.
     */
    private void deleteZombieClassFiles(WebFile aJavaFile)
    {
        // Get all ClassFiles for JavaFile and delete those older than JavaFile
        WebFile[] cfiles = _proj.getClassFilesForJavaFile(aJavaFile);
        if (cfiles == null) return;
        for (WebFile cfile : cfiles) {
            if (cfile.getLastModTime() < aJavaFile.getLastModTime()) {
                try {
                    cfile.delete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}