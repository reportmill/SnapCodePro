package snapcodepro.app;
import snap.props.PropChange;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.View;
import snap.view.ViewOwner;
import snap.viewx.TextPage;
import snap.viewx.WebBrowser;
import snap.viewx.WebBrowserHistory;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * This ViewOwner displays files for editing.
 */
public class ProjectFilesPane extends ViewOwner {

    // The ProjectPane
    private AppPane  _projPane;

    // A list of open files
    List<WebFile>  _openFiles = new ArrayList<>();

    // The currently selected file
    private WebFile  _selFile;

    // The WebBrowser for displaying editors
    private WebBrowser  _browser;

    /**
     * Constructor.
     */
    public ProjectFilesPane(AppPane aProjPane)
    {
        super();
        _projPane = aProjPane;
    }

    /**
     * Returns whether a file is an "OpenFile" (whether it needs a File Bookmark).
     */
    protected boolean isOpenFile(WebFile aFile)
    {
        // If directory, return false
        if (aFile.isDir()) return false;

        //
        WebSite[] projSites = _projPane.getSites();
        return ArrayUtils.containsId(projSites, aFile.getSite()) || aFile.getType().equals("java");
    }

    /**
     * Returns the open files.
     */
    public WebFile[] getOpenFiles()  { return _openFiles.toArray(new WebFile[0]); }

    /**
     * Adds a file to OpenFiles list.
     */
    public void addOpenFile(WebFile aFile)
    {
        // If already open file, just return
        if (aFile == null || !isOpenFile(aFile)) return;
        if (ListUtils.containsId(_openFiles, aFile))
            return;

        _openFiles.add(aFile);
        _projPane.getToolBar().buildFileTabs();
    }

    /**
     * Removes a file from OpenFiles list.
     */
    public int removeOpenFile(WebFile aFile)
    {
        // Remove file from list (just return if not available)
        int index = ListUtils.indexOfId(_openFiles, aFile);
        if (index < 0) return index;
        _openFiles.remove(index);

        // If removed file is selected file, set browser file to last file (that is still in OpenFiles list)
        if (aFile == _selFile) {
            WebURL url = getFallbackURL();
            if (!url.equals(_projPane.getHomePageURL()))
                getBrowser().setTransition(WebBrowser.Instant);
            getBrowser().setURL(url);
        }

        // Rebuild file tabs and return
        _projPane.getToolBar().buildFileTabs();
        return index;
    }

    /**
     * Removes a file from OpenFiles list.
     */
    public void removeAllOpenFilesExcept(WebFile aFile)
    {
        for (WebFile file : _openFiles.toArray(new WebFile[0]))
            if (file != aFile) removeOpenFile(file);
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelectedFile()  { return _selFile; }

    /**
     * Sets the selected site file.
     */
    public void setSelectedFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelectedFile()) return;
        _selFile = aFile;

        // Set selected file and update tree
        if (_selFile.isFile() || _selFile.isRoot())
            getBrowser().setFile(_selFile);

        addOpenFile(aFile);

        // Reset FilesPane
        _projPane._filesPane.resetLater();
    }

    /**
     * Returns the browser.
     */
    public WebBrowser getBrowser()
    {
        if (_browser != null) return _browser;
        getUI();
        return _browser;
    }

    /**
     * Sets the browser URL.
     */
    public void setBrowserURL(WebURL aURL)  { _browser.setURL(aURL); }

    /**
     * Sets the browser URL.
     */
    public void setBrowserFile(WebFile aFile)  { _browser.setFile(aFile); }

    /**
     * Sets the browser URL.
     */
    public void setPageForURL(WebURL aURL, WebPage aPage)  { _browser.setPage(aURL, aPage); }

    /**
     * Reloads a file.
     */
    public void reloadFile(WebFile aFile)  { _browser.reloadFile(aFile); }

    /**
     * Shows history.
     */
    public void showHistory()
    {
        WebBrowser browser = getBrowser();
        WebBrowserHistory history = browser.getHistory();
        StringBuilder sb = new StringBuilder();
        for (WebURL url : history.getLastURLs())
            sb.append(url.getString()).append('\n');

        WebURL fileURL = WebURL.getURL("/tmp/History.txt");
        WebFile file = fileURL.createFile(false);
        file.setText(sb.toString());
        browser.setFile(file);
    }

    /**
     * Returns the URL to fallback on when open file is closed.
     */
    private WebURL getFallbackURL()
    {
        // Return the most recently opened of the remaining OpenFiles, or the Project.HomePageURL
        WebBrowser browser = getBrowser();
        WebURL[] urls = browser.getHistory().getNextURLs();
        for (WebURL url : urls) {
            WebFile file = url.getFile();
            if (_openFiles.contains(file))
                return url.getFileURL();
        }

        urls = browser.getHistory().getLastURLs();
        for (WebURL url : urls) {
            WebFile file = url.getFile();
            if (_openFiles.contains(file))
                return url.getFileURL();
        }

        //
        return _projPane.getHomePageURL();
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Create browser
        _browser = new AppBrowser();
        _browser.setGrowHeight(true);

        // Return
        return _browser;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Listen to Browser PropChanges, to update ActivityText, ProgressBar, Window.Title
        _browser.addPropChangeListener(pc -> browserDidPropChange(pc));

    }

    /**
     * Called when Browser has changes.
     */
    private void browserDidPropChange(PropChange aPC)
    {
        _projPane.resetLater();
    }

    /**
     * A custom browser subclass.
     */
    private class AppBrowser extends WebBrowser {

        /**
         * Override to make sure that AppPane is in sync.
         */
        public void setPage(WebPage aPage)
        {
            // Do normal version
            if (aPage == getPage()) return;
            super.setPage(aPage);

            // Select page file
            WebFile file = aPage != null ? aPage.getFile() : null;
            setSelectedFile(file);
        }

        /**
         * Creates a WebPage for given file.
         */
        protected Class<? extends WebPage> getPageClass(WebResponse aResp)
        {
            // Get file and data
            WebFile file = aResp.getFile();
            String type = aResp.getPathType();

            // Handle Project Root directory
            if (file != null && file.isRoot() && ArrayUtils.containsId(_projPane.getSites(), file.getSite()))
                return SitePane.SitePage.class;

            // Handle Java
            if (type.equals("java")) {
                if (file != null && SnapEditorPage.isSnapEditSet(file))
                    return SnapEditorPage.class;
                return JavaPage.class;
            }

            //if(type.equals("snp")) return snapbuild.app.EditorPage.class;
            if (type.equals("rpt"))
                return getPageClass("com.reportmill.app.ReportPageEditor", TextPage.class);
            if (type.equals("class") && ArrayUtils.containsId(_projPane.getSites(), file.getSite()))
                return ClassInfoPage.class;
            if (type.equals("pgd"))
                return JavaShellPage.class;

            // Do normal version
            return super.getPageClass(aResp);
        }
    }
}
