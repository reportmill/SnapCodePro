package snapcodepro.app;

import javakit.ide.*;
import javakit.parse.*;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMember;
import javakit.parse.JavaTextDoc;
import javakit.resolver.Resolver;
import snapcodepro.project.JavaData;
import snapcodepro.project.ProjectX;
import snap.text.TextBoxLine;
import snap.text.TextDoc;
import snap.util.SnapUtils;
import snap.view.View;
import snap.view.ViewEvent;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebURL;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPage extends WebPage implements WebFile.Updater {

    // The JavaTextPane
    JavaTextPane _jtextPane = new JPJavaTextPane();

    /**
     * Creates a new JavaPage.
     */
    public JavaPage()
    {
        super();
    }

    /**
     * Return the AppPane.
     */
    AppPane getAppPane()
    {
        return getBrowser() instanceof AppBrowser ? ((AppBrowser) getBrowser()).getAppPane() : null;
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextPane getTextPane()
    {
        return _jtextPane;
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        return getTextPane().getTextArea();
    }

    /**
     * Creates UI panel.
     */
    protected View createUI()
    {
        return _jtextPane.getUI();
    }

    /**
     * Init UI.
     */
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Create JavaTextDoc
        WebFile javaFile = getFile();
        JavaTextDoc javaTextDoc = JavaTextDoc.newFromSource(javaFile);

        // Get/set Resolver
        ProjectX proj = ProjectX.getProjectForFile(javaFile);
        Resolver resolver = proj.getResolver();
        javaTextDoc.setResolver(resolver);

        // Set TextArea.TextDoc and FirstFocus
        JavaTextArea javaTextArea = getTextArea();
        javaTextArea.setTextDoc(javaTextDoc);
        setFirstFocus(javaTextArea);
    }

    /**
     * Override to reload text from file.
     */
    public void reload()
    {
        super.reload();

        // Create JavaTextDoc
        WebFile javaFile = getFile();
        JavaTextDoc javaTextDoc = JavaTextDoc.newFromSource(javaFile);

        // Get/set Resolver
        ProjectX proj = ProjectX.getProjectForFile(javaFile);
        Resolver resolver = proj.getResolver();
        javaTextDoc.setResolver(resolver);

        // Set TextArea.TextDoc
        JavaTextArea javaTextArea = getTextArea();
        javaTextArea.setTextDoc(javaTextDoc);
    }

    /**
     * Override to set parameters.
     */
    public void setResponse(WebResponse aResp)
    {
        // Do normal version
        super.setResponse(aResp);

        // If no real file, just return
        if (getFile() == null)
            return;

        // Load UI
        getUI();

        // Look for LineNumber
        WebURL aURL = aResp.getURL();
        String lineNumberString = aURL.getRefValue("LineNumber");
        if (lineNumberString != null) {
            int lineNumber = SnapUtils.intValue(lineNumberString);
            getTextArea().selectLine(lineNumber - 1);
        }

        // Look for Sel (selection)
        String sel = aURL.getRefValue("Sel");
        if (sel != null) {
            int start = SnapUtils.intValue(sel);
            sel = sel.substring(sel.indexOf('-') + 1);
            int end = SnapUtils.intValue(sel);
            if (end < start) end = start;
            getTextArea().setSel(start, end);
        }

        // Look for SelLine (select line)
        String selLine = aURL.getRefValue("SelLine");
        if (selLine != null) {
            int lineNum = SnapUtils.intValue(selLine) - 1;
            TextBoxLine tline = lineNum >= 0 && lineNum < getTextArea().getLineCount() ? getTextArea().getLine(lineNum) : null;
            if (tline != null) getTextArea().setSel(tline.getStartCharIndex());
        }

        // Look for Find
        String findString = aURL.getRefValue("Find");
        if (findString != null)
            getTextPane().find(findString, true, true);

        // Look for Member selection request
        String memberName = aURL.getRefValue("Member");
        if (memberName != null) {
            JFile jfile = JavaData.getJavaDataForFile(getFile()).getJFile();
            JClassDecl cd = jfile.getClassDecl();
            JExprId id = null;
            if (cd.getName().equals(memberName)) id = cd.getId();
            else for (JMemberDecl md : cd.getMemberDecls())
                if (md.getName() != null && md.getName().equals(memberName)) {
                    id = md.getId();
                    break;
                }
            if (id != null)
                getTextArea().setSel(id.getStartCharIndex(), id.getEndCharIndex());
        }
    }

    /**
     * Reopen this page as SnapCodePage.
     */
    public void openAsSnapCode()
    {
        WebFile file = getFile();
        WebURL url = file.getURL();
        WebPage page = new SnapEditorPage(this);
        page.setFile(file);
        WebBrowser browser = getBrowser();
        browser.setPage(url, page);
        browser.setURL(file.getURL());
    }

    /**
     * Creates a new file for use with showNewFilePanel method.
     */
    protected WebFile createNewFile(String aPath)
    {
        // Create file
        WebFile file = super.createNewFile(aPath);

        // Get project
        ProjectX proj = ProjectX.getProjectForFile(file);

        // Append package declaration
        StringBuffer sb = new StringBuffer();
        WebFile fileDir = file.getParent();
        String pkgName = proj != null ? proj.getClassNameForFile(fileDir) : file.getSimpleName();
        if (pkgName.length() > 0)
            sb.append("package ").append(pkgName).append(";\n");

        // Append Comment
        sb.append("\n/**\n * A custom class.\n */\n");

        // Append class declaration: "public class <File-Name> extends Object {   }"
        String className = file.getSimpleName();
        sb.append("public class ").append(className).append(" extends Object {\n\n\n\n}");

        // Set text
        file.setText(sb.toString());

        // Return
        return file;
    }

    /**
     * Override to set selection using browser.
     */
    private void setTextSel(int aStart, int anEnd)
    {
        String urls = getFile().getURL().getString() + String.format("#Sel=%d-%d", aStart, anEnd);
        getBrowser().setURLString(urls);
    }

    /**
     * Override to open declaration.
     */
    private void openDeclaration(JNode aNode)
    {
        JavaDecl decl = aNode.getDecl();
        if (decl != null) openDecl(decl);
    }

    /**
     * Open a super declaration.
     */
    private void openSuperDeclaration(JMemberDecl aMemberDecl)
    {
        JavaDecl sdecl = aMemberDecl.getSuperDecl();
        if (sdecl == null) return;
        openDecl(sdecl);
    }

    /**
     * Opens a project declaration.
     */
    private void openDecl(JavaDecl aDecl)
    {
        // Get class name for decl
        String className = aDecl instanceof JavaMember ? ((JavaMember) aDecl).getDeclaringClassName() :
                aDecl.getEvalClassName();
        if (className == null)
            return;

        // Open System class
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("javafx.")) {
            openJavaDecl(aDecl);
            return;
        }

        // Get project
        ProjectX proj = ProjectX.getProjectForSite(getSite());
        if (proj == null) return;

        // Get source file
        WebFile file = proj.getProjectSet().getJavaFile(className);
        if (file == null) return;

        // Get matching node
        JavaData jdata = JavaData.getJavaDataForFile(file);
        JNode node = NodeMatcher.getDeclMatch(jdata.getJFile(), aDecl);

        // Get URL
        String urls = file.getURL().getString();
        if (node != null)
            urls += String.format("#Sel=%d-%d", node.getStartCharIndex(), node.getEndCharIndex());

        // Open URL
        getBrowser().setURLString(urls);
    }

    /**
     * Override to open declaration.
     */
    private void openJavaDecl(JavaDecl aDecl)
    {
        String className = aDecl instanceof JavaMember ? ((JavaMember) aDecl).getDeclaringClassName() :
                aDecl.getEvalClassName();
        if (className == null)
            return;

        String javaPath = '/' + className.replace('.', '/') + ".java";

        // Get URL
        WebURL javaURL = WebURL.getURL("http://reportmill.com/jars/8u05/src.zip!" + javaPath);
        if (className.startsWith("javafx."))
            javaURL = WebURL.getURL("http://reportmill.com/jars/8u05/javafx-src.zip!" + javaPath);
        String urlString = javaURL.getString() + "#Member=" + aDecl.getSimpleName();

        // Open URL
        getBrowser().setURLString(urlString);
    }

    /**
     * Show references for given node.
     */
    private void showReferences(JNode aNode)
    {
        if (getAppPane() == null) return;
        getAppPane().getSearchPane().searchReference(aNode);
        getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
    }

    /**
     * Show declarations for given node.
     */
    private void showDeclarations(JNode aNode)
    {
        if (getAppPane() == null) return;
        getAppPane().getSearchPane().searchDeclaration(aNode);
        getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
    }

    /**
     * Override to update Page.Modified.
     */
    private void setTextModified(boolean aFlag)
    {
        getFile().setUpdater(aFlag ? this : null);
    }

    /**
     * WebFile.Updater method.
     */
    public void updateFile(WebFile aFile)
    {
        getFile().setText(getTextArea().getText());
    }

    /**
     * Override to get ProgramCounter from ProcPane.
     */
    private int getProgramCounterLine()
    {
        AppPane ap = getAppPane();
        return ap != null ? ap.getProcPane().getProgramCounter(getFile()) : -1;
    }

    /**
     * A JavaTextPane for a JavaPage to implement symbol features and such.
     */
    public class JPJavaTextPane extends JavaTextPaneX {

        /**
         * Override to set selection using browser.
         */
        public void setTextSel(int aStart, int anEnd)
        {
            JavaPage.this.setTextSel(aStart, anEnd);
        }

        /**
         * Override to open declaration.
         */
        public void openDeclaration(JNode aNode)
        {
            JavaPage.this.openDeclaration(aNode);
        }

        /**
         * Open a super declaration.
         */
        public void openSuperDeclaration(JMemberDecl aMemberDecl)
        {
            JavaPage.this.openSuperDeclaration(aMemberDecl);
        }

        /**
         * Show references for given node.
         */
        public void showReferences(JNode aNode)
        {
            JavaPage.this.showReferences(aNode);
        }

        /**
         * Show declarations for given node.
         */
        public void showDeclarations(JNode aNode)
        {
            JavaPage.this.showDeclarations(aNode);
        }

        /**
         * Override to update Page.Modified.
         */
        public void setTextModified(boolean aFlag)
        {
            super.setTextModified(aFlag);
            JavaPage.this.setTextModified(aFlag);
        }

        /**
         * Override to get ProgramCounter from ProcPane.
         */
        public int getProgramCounterLine()
        {
            return JavaPage.this.getProgramCounterLine();
        }

        /**
         * Respond to UI controls.
         */
        @Override
        public void respondUI(ViewEvent anEvent)
        {
            // Handle SnapCodeButton
            if (anEvent.equals("SnapCodeButton"))
                openAsSnapCode();

                // Do normal version
            else super.respondUI(anEvent);
        }

        /**
         * Creates the JavaTextArea.
         */
        protected JavaTextArea createTextArea()
        {
            return new JPJavaTextArea();
        }
    }

    /**
     * Override.
     */
    public class JPJavaTextArea extends JavaTextArea {

        /**
         * Returns the project.
         */
        public ProjectX getProject()
        {
            TextDoc textDoc = getTextDoc();
            WebFile file = textDoc.getSourceFile();
            return file != null ? ProjectX.getProjectForFile(file) : null;
        }

        /**
         * Returns BuildIssues from ProjectFile.
         */
        public BuildIssue[] getBuildIssues()
        {
            TextDoc textDoc = getTextDoc();
            WebFile file = textDoc.getSourceFile();
            if (file == null) return new BuildIssue[0];
            ProjectX proj = getProject();
            if (proj == null) return new BuildIssue[0];
            ProjectX rootProj = proj.getRootProject();
            BuildIssues buildIssues = rootProj.getBuildIssues();
            BuildIssue[] buildIssueArray = buildIssues.getIssues(file);
            return buildIssueArray; // Was getRootProject().getBuildIssues().getIssues(getSourceFile()) JK
        }

        /**
         * Returns the project breakpoints.
         */
        private Breakpoints getProjBreakpoints()
        {
            ProjectX proj = getProject();
            if (proj == null) return null;
            ProjectX rootProj = proj.getRootProject();
            Breakpoints breakpoints = rootProj.getBreakpoints();
            return breakpoints; // Was getRootProject().getBreakPoints() JK
        }

    }
}