package snapcodepro.project;

import javakit.parse.*;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaClass;
import javakit.resolver.Resolver;
import javakit.ide.BuildIssue;
import snap.parse.ParseException;
import snap.parse.ParseNode;
import snap.parse.Parser;
import snap.web.WebFile;

import java.util.*;

/**
 * A file object for managing Java files.
 */
public class JavaData {

    // The java file
    private WebFile  _file;

    // The Project that owns this file
    private ProjectX _proj;

    // The set of declarations in this JavaFile
    private Set<JavaDecl>  _decls = new HashSet<>();

    // The set of references in this JavaFile
    private Set<JavaDecl>  _refs = new HashSet<>();

    // The set of files that our file depends on
    private Set<WebFile>  _dependencies = new HashSet<>();

    // The set of files that depend on our file
    private Set<WebFile>  _dependents = new HashSet<>();

    // The parsed version of this JavaFile
    private JFile _jfile;

    /**
     * Creates a new JavaData for given file.
     */
    public JavaData(WebFile aFile)
    {
        _file = aFile;
    }

    /**
     * Returns the project for this JavaFile.
     */
    public ProjectX getProject()
    {
        // If already set, just return
        if (_proj != null) return _proj;

        // Get, set, return
        ProjectX proj = ProjectX.getProjectForFile(_file);
        return _proj = proj;
    }

    /**
     * Returns the class files for this JavaFile.
     */
    public WebFile[] getClassFiles()
    {
        ProjectX proj = getProject();
        WebFile[] classFiles = proj.getClassFilesForJavaFile(_file);
        return classFiles;
    }

    /**
     * Returns the declarations in this JavaFile.
     */
    public synchronized Set<JavaDecl> getDecls()
    {
        // If already loaded, just return
        if (_decls.size() > 0) return _decls;

        // Get Resolver
        ProjectX proj = getProject();
        Resolver resolver = proj.getResolver();

        // Iterate over JavaFile.Class files
        WebFile[] classFiles = getClassFiles();
        for (WebFile classFile : classFiles) {
            String className = proj.getClassNameForFile(classFile);
            JavaClass javaClass = resolver.getJavaClassForName(className);
            if (javaClass == null) {
                System.err.println("JavaData.getDecls: Can't find decl " + className);
                continue;
            }
            _decls.addAll(javaClass.getAllDecls());
        }

        // Return decls
        return _decls;
    }

    /**
     * Returns the references in this JavaFile.
     */
    public Set<JavaDecl> getRefs()
    {
        return _refs;
    }

    /**
     * Returns the set of files that our file depends on.
     */
    public Set<WebFile> getDependencies()
    {
        return _dependencies;
    }

    /**
     * Returns the set of files that depend on our file.
     */
    public Set<WebFile> getDependents()
    {
        return _dependents;
    }

    /**
     * Returns whether dependencies are set.
     */
    public boolean isDependenciesSet()
    {
        return _dset;
    }

    boolean _dset;

    /**
     * Updates dependencies for a given file and list of new/old dependencies.
     *
     * @return whether any dependencies (the declarations or references) have changed since last update.
     */
    public synchronized boolean updateDependencies()
    {
        // Get Java file, project, RootProject, ProjectSet and class files

        // Get Project and Resolver
        ProjectX proj = getProject();
        Resolver resolver = proj.getResolver();

        // Cache JFile
        WebFile jfile = _file;

        // Get Class files
        WebFile[] classFiles = getClassFiles();

        // Get new declarations
        boolean declsChanged = false;
        if (classFiles != null) {
            for (WebFile classFile : classFiles) {
                String className = proj.getClassNameForFile(classFile);
                JavaClass javaClass = resolver.getJavaClassForName(className);
                if (javaClass == null)
                    return false;

                // Update decls
                try {
                    boolean changed = javaClass.updateDecls();
                    if (changed)
                        declsChanged = true;
                }

                catch (Throwable t) {
                    System.err.printf("JavaData.updateDepends failed to get decls in %s: %s\n", classFile, t);
                }
            }
        }

        // If declarations have changed, clear cached list
        if (declsChanged)
            _decls.clear();

        // Cache JFile and clear
        _jfile = null;

        // Get new refs
        Set<JavaDecl> nrefs = new HashSet<>();
        _dset = true;
        if (classFiles != null) {
            for (WebFile classFile : classFiles) {
                ClassData classData = ClassData.getClassDataForFile(classFile);
                try {
                    classData.getRefs(nrefs);
                }

                catch (Throwable t) {
                    System.err.printf("JavaData.updateDepends failed to get refs in %s: %s\n", classFile, t);
                }
            }
        }

        // If references haven't changed, just return
        if (nrefs.equals(_refs))
            return declsChanged;

        // Get set of added/removed refs
        Set<JavaDecl> refsAdded = new HashSet<>(_refs);
        refsAdded.addAll(nrefs);
        Set<JavaDecl> refsRemoved = new HashSet<>(refsAdded);
        refsRemoved.removeAll(nrefs);
        refsAdded.removeAll(_refs);
        _refs = nrefs;

        // Iterate over added refs and add dependencies
        ProjectX rootProj = proj.getRootProject();
        ProjectSet projSet = rootProj.getProjectSet();
        for (JavaDecl ref : refsAdded) {

            // Get Class ref
            JavaClass javaClass = ref instanceof JavaClass ? (JavaClass) ref : null;
            if (javaClass == null) continue;

            // Skip system classes
            String className = javaClass.getRootClassName();
            if (className.startsWith("java") && (className.startsWith("java.") || className.startsWith("javax.") ||
                    className.startsWith("javafx")))
                continue;

            //
            WebFile file = projSet.getJavaFile(className);
            if (file != null && file != jfile && !_dependencies.contains(file)) {
                _dependencies.add(file);
                JavaData.getJavaDataForFile(file)._dependents.add(jfile);
            }
        }

        // Iterate over removed refs and add dependencies
        for (JavaDecl ref : refsRemoved) {

            // Get Class ref
            JavaClass javaClass = ref instanceof JavaClass ? (JavaClass) ref : null;
            if (javaClass == null) continue;

            String className = javaClass.getRootClassName();
            WebFile file = projSet.getJavaFile(className);
            if (file != null && _dependencies.contains(file)) {
                _dependencies.remove(file);
                JavaData.getJavaDataForFile(file)._dependents.remove(jfile);
            }
        }

        // Return true since references changed
        return true;
    }

    /**
     * Removes dependencies.
     */
    public void removeDependencies()
    {
        for (WebFile dep : _dependencies) JavaData.getJavaDataForFile(dep)._dependents.remove(_file);
        _dependencies.clear();
        _decls.clear();
        _refs.clear();
        _dset = false;
    }

    /**
     * Returns the parsed Java file.
     */
    public JFile getJFile()
    {
        // If already set, just return
        if (_jfile != null) return _jfile;

        // Create, set, return
        JFile jfile = createJFile();
        return _jfile = jfile;
    }

    /**
     * Returns the parsed Java file.
     */
    protected JFile createJFile()
    {
        // Get Java string and parser and generate JavaFile
        String string = _file.getText();
        JavaParser javaParser = JavaParser.getShared();
        JFile jfile = javaParser.getJavaFile(string);

        // Set SourceFile and Resolver
        jfile.setSourceFile(_file);
        ProjectX proj = getProject();
        Resolver resolver = proj.getResolver();
        jfile.setResolver(resolver);

        // Return
        return jfile;
    }

    /**
     * Returns a set of unused imports.
     */
    public List<BuildIssue> getUnusedImports()
    {
        String string = _file.getText();
        Parser importsParser = JavaParser.getShared().getImportsParser();

        // Get parse node
        ParseNode node = null;
        try {
            if (string != null && string.length() > 0)
                node = importsParser.parse(string);
        }
        catch (ParseException e) {
            System.err.println("JavaData.getUnusedImports Parse Exception");
            e.printStackTrace();
        }

        // If no node, just return
        if (node == null)
            return Collections.EMPTY_LIST;

        // Get JFile
        JFile jfile = node.getCustomNode(JFile.class);
        jfile.setSourceFile(_file);
        ProjectX proj = getProject();
        Resolver resolver = proj.getResolver();
        jfile.setResolver(resolver);

        // Get importDecls
        List<JImportDecl> imports = jfile.getImportDecls();
        if (imports.size() == 0)
            return Collections.EMPTY_LIST;

        // Iterate over class references and remove used ones
        Set<JavaDecl> refs = getRefs();
        Set<JImportDecl> importsSet = new HashSet<>(imports);
        for (JavaDecl ref : refs) {
            if (!(ref instanceof JavaClass)) continue;
            String simpleName = ref.getSimpleName();
            JImportDecl implDecl = jfile.getImport(simpleName);
            if (implDecl != null) {
                importsSet.remove(implDecl);
                if (importsSet.size() == 0)
                    return Collections.EMPTY_LIST;
            }
        }

        // Iterate over references and remove used ones
    /*for(JavaDecl ref : refs) { if(ref.isClass() || ref.isConstructor()) continue;
        String sname = ref.getEvalType().getSimpleName(); JImportDecl idecl = jfile.getImport(sname);
        if(idecl!=null) { iset.remove(idecl); if(iset.size()==0) return Collections.EMPTY_LIST; } }*/

        // Do full parse and eval
        importsSet = getJFile().getUnusedImports();
        if (importsSet.size() == 0)
            return Collections.EMPTY_LIST;

        // Create BuildIssues
        List<BuildIssue> issues = new ArrayList<>();
        for (JImportDecl idecl : importsSet)
            issues.add(new BuildIssue().init(_file, BuildIssue.Kind.Warning,
                    "The import " + idecl.getName() + " is never used",
                    idecl.getLineIndex(), 0, idecl.getStartCharIndex(), idecl.getEndCharIndex()));

        // Return
        return issues;
    }

    /**
     * Returns the JavaData for given file.
     */
    public static JavaData getJavaDataForFile(WebFile aFile)
    {
        // Get JavaData for given source file
        JavaData javaData = (JavaData) aFile.getProp(JavaData.class.getName());

        // If missing, create/set
        if (javaData == null) {
            javaData = new JavaData(aFile);
            aFile.setProp(JavaData.class.getName(), javaData);
        }

        // Return
        return javaData;
    }
}