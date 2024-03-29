/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcodepro.apptools;
import javakit.parse.JFile;
import javakit.parse.JNode;
import javakit.project.JavaAgent;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMethod;
import javakit.resolver.JavaParameterizedType;
import javakit.ide.NodeMatcher;
import javakit.ide.JavaTextUtils;
import snap.gfx.Image;
import snapcodepro.app.ProjectPane;
import snapcodepro.app.ProjectTool;
import snapcodepro.app.SitePane;
import snapcodepro.project.JavaData;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import snap.web.WebSite;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class manages project search.
 */
public class SearchPane extends ProjectTool {

    // The current search
    private Search  _search;

    // The current selected result
    private Result  _selResult;

    /**
     * Creates a new search pane for app pane.
     */
    public SearchPane(ProjectPane projPane)
    {
        super(projPane);
    }

    /**
     * Returns the current search.
     */
    public Search getSearch()  { return _search; }

    /**
     * Returns the current search results.
     */
    public List<Result> getResults()
    {
        return _search != null ? _search._results : null;
    }

    /**
     * Returns the current selected search result.
     */
    public Result getSelResult()  { return _selResult; }

    /**
     * Sets the current selected search result.
     */
    public void setSelResult(Result aResult)
    {
        _selResult = aResult;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Configure ResultsList
        ListView<Result> resultsList = getView("ResultsList", ListView.class);
        resultsList.setCellConfigure(this::configureResultListCell);
        resultsList.setRowHeight(24);

        setFirstFocus("SearchText");
    }

    /**
     * Reset UI.
     */
    public void resetUI()
    {
        // Update ClearButton
        setViewEnabled("ClearButton", _search != null);

        // Update SearchResultsText
        String resultsStr = "";
        if (_search != null) {
            int hits = 0;
            for (Result results : _search._results)
                hits += results._count;
            String typ = _search._kind == Search.Kind.Declaration ? "declarations" : "references";
            resultsStr = String.format("'%s' - %d %s", _search._string, hits, typ);
        }
        setViewValue("SearchResultsText", resultsStr);

        // Update ResultsList
        setViewItems("ResultsList", getResults());
        setViewSelItem("ResultsList", getSelResult());
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SearchText
        if (anEvent.equals("SearchText")) {
            String string = anEvent.getStringValue();
            if (string != null && string.length() > 0)
                search(string);
            else _search = null;
        }

        // Handle ClearButton
        if (anEvent.equals("ClearButton"))
            _search = null;

        // Handle ResultsList
        if (anEvent.equals("ResultsList")) {

            // Update SelResult
            Result result = (Result) anEvent.getSelItem();
            setSelResult(result);

            // Set in browser
            if (result != null) {
                String resultURL = result.getURLString();
                getBrowser().setURLString(resultURL);
            }
        }
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Search"; }

    /**
     * Search for given term.
     */
    public void search(String aString)
    {
        _search = new Search();
        _search._string = aString;
        for (WebSite site : _projPane.getSites())
            search(site.getRootDir(), _search._results, aString.toLowerCase());
        resetLater();
    }

    /**
     * Search for given string in given file with list for results.
     */
    protected void search(WebFile aFile, List<Result> theResults, String aString)
    {
        // If hidden file, just return
        SitePane sitePane = SitePane.get(aFile.getSite());
        if (sitePane.isHiddenFile(aFile))
            return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _projPane.getBuildDir())
                return;
            for (WebFile file : aFile.getFiles())
                search(file, theResults, aString);
        }

        // Handle JavaFile
        else if (isSearchTextFile(aFile)) {
            String text = aFile.getText().toLowerCase();
            Result result = null;
            int len = aString.length();
            for (int start = text.indexOf(aString); start >= 0; start = text.indexOf(aString, start + len)) {
                if (result == null)
                    theResults.add(result = new Result(aFile));
                else result._count++;
            }
        }
    }

    /**
     * Returns whether file is to be included in search text.
     */
    protected boolean isSearchTextFile(WebFile aFile)
    {
        String type = aFile.getType();
        String[] types = { "java", "snp", "txt", "js" };
        return ArrayUtils.contains(types, type);
    }

    /**
     * Search for given element reference.
     */
    public void searchReference(JNode aNode)
    {
        // Get JavaDecl for node
        JavaDecl decl = aNode.getDecl();
        if (decl == null) {
            beep();
            return;
        }

        // If method, get root method
        if (decl instanceof JavaMethod) {
            JavaMethod method = (JavaMethod) decl;
            while (method.getSuper() != null)
                method = method.getSuper();
            decl = method;
        }

        // If param class
        if (decl instanceof JavaParameterizedType)
            decl = decl.getEvalClass();

        // Configure search
        _search = new Search();
        _search._string = decl.getMatchName();
        _search._kind = Search.Kind.Reference;

        // Iterate over all project sites
        for (WebSite site : _projPane.getSites())
            searchReference(site.getRootDir(), _search._results, decl);

        // Update UI
        resetLater();
    }

    /**
     * Search for given element reference.
     */
    public void searchReference(WebFile aFile, List<Result> theResults, JavaDecl aDecl)
    {
        // If hidden file, just return
        SitePane sitePane = SitePane.get(aFile.getSite());
        if (sitePane.isHiddenFile(aFile))
            return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _projPane.getBuildDir())
                return;
            WebFile[] dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles)
                searchReference(file, theResults, aDecl);
        }

        // Handle JavaFile
        else if (aFile.getType().equals("java")) {

            // Get JavaAgent, JavaData and references
            JavaAgent javaAgent = JavaAgent.getAgentForFile(aFile);
            JavaData javaData = JavaData.getJavaDataForFile(aFile);
            Set<JavaDecl> refs = javaData.getRefs();

            // Iterate over references
            for (JavaDecl decl : refs) {
                if (aDecl.matches(decl)) {
                    JFile jfile = javaAgent.getJFile();
                    List<JNode> nodes = new ArrayList<>();
                    NodeMatcher.getRefMatches(jfile, aDecl, nodes);
                    for (JNode node : nodes)
                        theResults.add(new Result(node));
                    return;
                }
            }
        }
    }

    /**
     * Search for given element reference.
     */
    public void searchDeclaration(JNode aNode)
    {
        // Get JavaDecl for node
        JavaDecl decl = aNode.getDecl();
        if (decl == null) {
            beep();
            return;
        }

        // If method, get root method
        if (decl instanceof JavaMethod) {
            JavaMethod method = (JavaMethod) decl;
            while (method.getSuper() != null)
                method = method.getSuper();
            decl = method;
        }

        // Configure search
        _search = new Search();
        _search._string = decl.getMatchName();
        _search._kind = Search.Kind.Declaration;

        // Iterate over all project sites
        for (WebSite site : _projPane.getSites())
            searchDeclaration(site.getRootDir(), _search._results, decl);

        // Update UI
        resetLater();
    }

    /**
     * Search for given element reference.
     */
    public void searchDeclaration(WebFile aFile, List<Result> theResults, JavaDecl aDecl)
    {
        // If hidden file, just return
        SitePane sitePane = SitePane.get(aFile.getSite());
        if (sitePane.isHiddenFile(aFile))
            return;

        // Handle directory
        if (aFile.isDir()) {
            if (aFile == _projPane.getBuildDir())
                return;

            WebFile[] dirFiles = aFile.getFiles();
            for (WebFile file : dirFiles)
                searchDeclaration(file, theResults, aDecl);
        }

        // Handle JavaFile
        else if (aFile.getType().equals("java")) {

            // Get JavaAgent, JavaData, and declarations
            JavaAgent javaAgent = JavaAgent.getAgentForFile(aFile);
            JavaData javaData = JavaData.getJavaDataForFile(aFile);
            Set<JavaDecl> decls = javaData.getDecls();

            // Iterate over decls
            for (JavaDecl decl : decls) {
                if (aDecl.matches(decl)) {
                    JFile jfile = javaAgent.getJFile();
                    List<JNode> nodes = new ArrayList<>();
                    NodeMatcher.getDeclMatches(jfile, aDecl, nodes);
                    for (JNode node : nodes)
                        theResults.add(new Result(node));
                    return;
                }
            }
        }
    }

    /**
     * A class to hold a search.
     */
    public static class Search {

        // The search string
        String _string;

        // The kind
        Kind _kind = Kind.Text;

        // The results
        List<Result> _results = new ArrayList<>();

        // Constants for kind
        public enum Kind {Text, Reference, Declaration}
    }

    /**
     * A class to hold a search result.
     */
    public class Result {

        // The file
        WebFile _file;

        // The JNode
        JNode _node;

        // The match count
        int _count = 1;

        /**
         * Creates a new result.
         */
        public Result(WebFile aFile)
        {
            _file = aFile;
        }

        /**
         * Creates a new result.
         */
        public Result(JNode aNode)
        {
            _node = aNode;
            _file = _node.getFile().getSourceFile();
        }

        /**
         * Standard toString implementation.
         */
        public String getDescriptor()
        {
            JavaDecl decl = _node != null ? _node.isDecl() ? _node.getDecl() : _node.getEnclosingDecl() : null;
            if (decl != null) return decl.getPrettyName();
            String s = _file.getName() + " - " + _file.getParent().getPath();
            s += " (" + _count + " match" + (_count == 1 ? "" : "es") + ")";
            return s;
        }

        /**
         * Returns an image.
         */
        public Image getImage()
        {
            JavaDecl decl = _node != null ? _node.isDecl() ? _node.getDecl() : _node.getEnclosingDecl() : null;
            if (decl == null)
                return ViewUtils.getFileIconImage(_file);
            return JavaTextUtils.getImageForJavaDecl(decl);
        }

        /**
         * Returns a URL for result.
         */
        public String getURLString()
        {
            String urls = _file.getURL().getString();
            if (_search._kind == Search.Kind.Text)
                urls += "#Find=" + _search._string;
            else if (_node != null)
                urls += String.format("#Sel=%d-%d", _node.getStartCharIndex(), _node.getEndCharIndex());
            return urls;
        }
    }

    /**
     * Called to configure cell.
     */
    private void configureResultListCell(ListCell<Result> aCell)
    {
        Result result = aCell.getItem();
        if (result == null)
            return;
        aCell.setText(result.getDescriptor());
        aCell.setImage(result.getImage());
        aCell.getGraphic().setPadding(2, 4, 2, 4);
    }
}