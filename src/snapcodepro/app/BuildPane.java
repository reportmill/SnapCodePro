package snapcodepro.app;

import snapcodepro.project.ProjectX;
import snap.view.*;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;
import snap.web.WebSite;

/**
 * A UI pane to show and manage the build directory.
 */
public class BuildPane extends ViewOwner {

    // The SitePane that owns this BuildPane
    SitePane _sitePane;

    // The Site
    WebSite _site;

    // The Project
    ProjectX _proj;

    // The Build dir
    WebFile _buildDir;

    // The FileBrowser
    BrowserView<WebFile> _fileBrowser;

    // The PageBrowser
    WebBrowser _pageBrowser;

    /**
     * Creates a new BuildPane for given site.
     */
    protected BuildPane(SitePane aSP)
    {
        _sitePane = aSP;
        _site = aSP.getSite();
        _proj = ProjectX.getProjectForSite(_site);
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        return _proj.getBuildDir();
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Get/configure FileBrowser
        _fileBrowser = getView("FileBrowser", BrowserView.class);
        _fileBrowser.setPrefColCount(3);
        _fileBrowser.setResolver(new FileResolver());
        _fileBrowser.setItems(getBuildDir());

        // Get/configure PageBrowser
        _pageBrowser = new WebBrowser() {
            protected Class<? extends WebPage> getPageClass(WebResponse aResp)
            {
                String type = aResp.getPathType();
                if (type.equals("class")) return ClassInfoPage.class;
                return super.getPageClass(aResp);
            }
        };

        // Configure PageBrowser
        _pageBrowser.setName("PageBrowser");
        _pageBrowser.setGrowHeight(true);
        getUI(ChildView.class).addChild(_pageBrowser);
    }

    /**
     * ResetUI.
     */
    protected void resetUI()
    {
        // Reset BuildDirText
        setViewText("BuildDirText", _proj.getBuildDir().getPath());
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle FileBrowser
        if (anEvent.equals("FileBrowser")) {
            WebFile file = _fileBrowser.getSelItem();
            if (file != null && !file.isDir())
                _pageBrowser.setURL(file.getURL());
        }

        // Handle OpenButton
        if (anEvent.equals("OpenButton"))
            snap.gfx.GFXEnv.getEnv().openFile(_proj.getBuildDir());

        // Handle BuildButton
        if (anEvent.equals("BuildButton"))
            _sitePane.buildSite(true);

        // Handle CleanButton
        if (anEvent.equals("CleanButton"))
            _sitePane.cleanSite();
    }

    /**
     * The TreeResolver to provide data to File browser.
     */
    private class FileResolver extends TreeResolver<WebFile> {

        /**
         * Returns the parent of given item.
         */
        public WebFile getParent(WebFile anItem)
        {
            return anItem.getParent();
        }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(WebFile anItem)
        {
            return anItem.isDir();
        }

        /**
         * Returns the children.
         */
        public WebFile[] getChildren(WebFile aPar)
        {
            return aPar.getFiles();
        }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(WebFile anItem)
        {
            return anItem.getName();
        }
    }

}