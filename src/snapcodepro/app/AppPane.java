package snapcodepro.app;

import snapcodepro.project.Project;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.FileUtils;
import snap.util.ListUtils;
import snap.util.Prefs;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;

import java.util.ArrayList;
import java.util.List;

/**
 * The main view class for Projects.
 */
public class AppPane extends ViewOwner {

    // The list of sites
    List<WebSite> _sites = new ArrayList();

    // The AppPaneToolBar
    AppPaneToolBar _toolBar = new AppPaneToolBar(this);

    // The main SplitView that holds sidebar and browser
    SplitView _mainSplit;

    // The SplitView that holds FilesPane and ProcPane
    SplitView _sideBarSplit;

    // The FilesPane
    AppFilesPane _filesPane = new AppFilesPane(this);

    // The ProcPane manages run/debug processes
    ProcPane _procPane = new ProcPane(this);

    // The AppBroswer for displaying editors
    AppBrowser _browser;

    // The pane that the browser sits in
    SplitView _browserBox;

    // The SupportTray
    SupportTray _supportTray = new SupportTray(this);

    // The Problems pane
    BuildIssuesPane _problemsPane = new BuildIssuesPane(this);

    // The RunConsole
    RunConsole _runConsole = new RunConsole(this);

    // The DebugVarsPane
    DebugVarsPane _debugVarsPane = new DebugVarsPane(this);

    // The DebugExprsPane
    DebugExprsPane _debugExprsPane = new DebugExprsPane(this);

    // The BreakpointsPanel
    BreakpointsPanel _breakpointsPanel = new BreakpointsPanel(this);

    // The SearchPane
    SearchPane _searchPane = new SearchPane(this);

    // The currently open AppPane
    static AppPane _openAppPane;

    // A PropChangeListener to watch for site file changes
    PropChangeListener _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Returns the browser.
     */
    public AppBrowser getBrowser()
    {
        return _browser;
    }

    /**
     * Returns the browser box.
     */
    public SplitView getBrowserBox()
    {
        return _browserBox;
    }

    /**
     * Returns the toolbar.
     */
    public AppPaneToolBar getToolBar()
    {
        return _toolBar;
    }

    /**
     * Returns the files pane.
     */
    public AppFilesPane getFilesPane()
    {
        return _filesPane;
    }

    /**
     * Returns the processes pane.
     */
    public ProcPane getProcPane()
    {
        return _procPane;
    }

    /**
     * Returns whether is showing SideBar (holds FilesPane and ProcPane).
     */
    public boolean isShowSideBar()
    {
        return _showSideBar;
    }

    boolean _showSideBar = true;

    /**
     * Sets whether to show SideBar (holds FilesPane and ProcPane).
     */
    public void setShowSideBar(boolean aValue)
    {
        if (aValue == isShowSideBar()) return;
        _showSideBar = aValue;
        if (aValue)
            _mainSplit.addItemWithAnim(_sideBarSplit, 220, 0);
        else _mainSplit.removeItemWithAnim(_sideBarSplit);
    }

    /**
     * Returns whether SupportTray is visible.
     */
    public boolean isSupportTrayVisible()
    {
        return getBrowserBox().getItemCount() > 1;
    }

    /**
     * Sets SupportTray visible.
     */
    public void setSupportTrayVisible(boolean aValue)
    {
        // If value already set, or if asked to close ExplicitlyOpened SupportTray, just return
        if (aValue == isSupportTrayVisible() || !aValue && _supportTray.isExplicitlyOpened()) return;

        // Get SupportTray UI and SplitView
        View supTrayUI = _supportTray.getUI();
        SplitView spane = getBrowserBox();

        // Add/remove SupportTrayUI with animator
        if (aValue) spane.addItemWithAnim(supTrayUI, 240);
        else spane.removeItemWithAnim(supTrayUI);

        // Update ShowTrayButton
        setViewText("ShowTrayButton", aValue ? "Hide Tray" : "Show Tray");
    }

    /**
     * Returns the SupportTray index.
     */
    public int getSupportTrayIndex()
    {
        return isSupportTrayVisible() ? _supportTray.getSelIndex() : -1;
    }

    /**
     * Sets SupportTray visible to given index.
     */
    public void setSupportTrayIndex(int anIndex)
    {
        setSupportTrayVisible(true);
        _supportTray.setSelIndex(anIndex);
    }

    /**
     * Returns the top level site.
     */
    public WebSite getRootSite()
    {
        return _sites.get(0);
    }

    /**
     * Returns the number of sites.
     */
    public int getSiteCount()
    {
        return _sites.size();
    }

    /**
     * Returns the individual site at the given index.
     */
    public WebSite getSite(int anIndex)
    {
        return _sites.get(anIndex);
    }

    /**
     * Returns the list of sites.
     */
    public List<WebSite> getSites()
    {
        return _sites;
    }

    /**
     * Adds a site to sites list.
     */
    public void addSite(WebSite aSite)
    {
        // If site already added, just return
        if (_sites.contains(aSite)) return;

        // Create project for site
        Project proj = Project.getProjectForSite(aSite);
        if (proj == null)
            proj = new Project(aSite);

        // Add site
        _sites.add(getSiteCount(), aSite);  // Add site
        SitePane.get(aSite, true).setAppPane(this);
        aSite.addFileChangeListener(_siteFileLsnr);

        // Add dependent sites
        for (Project p : proj.getProjects())
            addSite(p.getSite());

        // Clear root files and Reset UI
        _filesPane._rootFiles = null;
        resetLater();
    }

    /**
     * Removes a site from sites list.
     */
    public void removeSite(WebSite aSite)
    {
        _sites.remove(aSite);
        aSite.removeFileChangeListener(_siteFileLsnr);
        _filesPane._rootFiles = null;
        resetLater();
    }

    /**
     * Shows the AppPane window.
     */
    public void show()
    {
        // Set AppPane as OpenSite and show window
        getUI();
        _openAppPane = this;
        getWindow().setSaveName("AppPane");
        getWindow().setSaveSize(true);
        getWindow().setVisible(true);

        // Open site and show home page
        SitePane.get(getSite(0)).openSite();
        showHomePage();
    }

    /**
     * Close this AppPane.
     */
    public void hide()
    {
        // Flush and refresh sites
        for (WebSite site : getSites()) {
            SitePane.get(site).closeSite();
            try {
                site.flush();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            site.resetFiles();
        }
        _openAppPane = null;
    }

    /**
     * Returns the current open AppPane.
     */
    public static AppPane getOpenAppPane()
    {
        return _openAppPane;
    }

    /**
     * Shows the home page.
     */
    public void showHomePage()
    {
        getBrowser().setURL(getHomePageURL());
    }

    /**
     * Returns the HomePageURL.
     */
    public WebURL getHomePageURL()
    {
        WebURL url = SitePane.get(getSite(0)).getHomePageURL();
        return url != null ? url : getFilesPane().getHomePageURL();
    }

    /**
     * Returns the selected file.
     */
    public WebFile getSelectedFile()
    {
        return _sf;
    }

    WebFile _sf;

    /**
     * Sets the selected site file.
     */
    public void setSelectedFile(WebFile aFile)
    {
        // If file already set, just return
        if (aFile == null || aFile == getSelectedFile()) return;
        _sf = aFile;

        // Set selected file and update tree
        if (_sf != null && _sf.isFile() || _sf.isRoot())
            getBrowser().setFile(_sf);
        _filesPane.resetLater();
    }

    /**
     * Returns the selected directory.
     */
    public WebFile getSelectedDir()
    {
        WebFile sf = getSelectedFile();
        return sf.isDir() ? sf : sf.getParent();
    }

    /**
     * Returns the selected site.
     */
    public WebSite getSelectedSite()
    {
        WebFile file = getSelectedFile();
        WebSite site = file != null ? file.getSite() : null;
        if (!ListUtils.containsId(getSites(), site)) site = getSite(0);
        return site;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        WebSite site = getSelectedSite();
        Project proj = site != null ? Project.getProjectForSite(site) : null;
        return proj != null ? proj.getBuildDir() : null;
    }

    /**
     * Called when site file changes with File PropChange.
     */
    void siteFileChanged(PropChange aPC)
    {
        // Get file and update in FilesPane
        WebFile file = (WebFile) aPC.getSource();
        if (file.getExists())
            _filesPane.updateFile(file);
    }

    /**
     * Creates the UI.
     */
    protected View createUI()
    {
        // Create basic UI
        _mainSplit = (SplitView) super.createUI();

        // Create ColView to hold ToolBar and MainSplit
        ColView vbox = new ColView();
        vbox.setFillWidth(true);
        vbox.setChildren(_toolBar.getUI(), _mainSplit); //return vbox;

        // Create ColView holding MenuBar and EditorPane UI (with key listener so MenuBar catches shortcut keys)
        View mbarView = MenuBar.createMenuBarView(getMenuBar(), vbox);
        return mbarView;
    }

    /**
     * Initializes UI panel.
     */
    protected void initUI()
    {
        // Get AppBrowser
        _browser = getView("Browser", AppBrowser.class);
        _browser.setAppPane(this);

        // Listen to Browser PropChanges, to update ActivityText, ProgressBar, Window.Title
        _browser.addPropChangeListener(pc -> resetLater());

        // Get SideBarSplit and add FilesPane, ProcPane
        _sideBarSplit = getView("SideBarSplitView", SplitView.class);
        _sideBarSplit.setBorder(null);
        View filesPaneUI = _filesPane.getUI();
        filesPaneUI.setGrowHeight(true);
        View procPaneUI = _procPane.getUI();
        procPaneUI.setPrefHeight(250);
        _sideBarSplit.setItems(filesPaneUI, procPaneUI);
        _sideBarSplit.setClipToBounds(true);

        // Get browser box
        _browserBox = getView("BrowserBox", SplitView.class);
        _browserBox.setGrowWidth(true);
        _browserBox.setBorder(null);
        for (View c : _browserBox.getChildren()) c.setBorder(null);
        _browserBox.getChild(0).setGrowHeight(true); // So support tray has constant size

        // Add key binding to OpenMenuItem and CloseWindow
        //addKeyActionHandler("OpenMenuItem", "meta O");
        //addKeyActionHandler("CloseFileAction", "meta W");

        // Configure Window
        getWindow().setTitle("SnapCode Project");
        //getRootView().setMenuBar(getMenuBar());

        // Register for WelcomePanel on close
        enableEvents(getWindow(), WinClose);
    }

    /**
     * Resets UI panel.
     */
    public void resetUI()
    {
        // Reset window title
        WebPage page = getBrowser().getPage();
        getWindow().setTitle(page != null ? page.getTitle() : "SnapCode");

        // Set ActivityText, StatusText
        setViewText("ActivityText", getBrowser().getActivity());
        setViewText("StatusText", getBrowser().getStatus());

        // Update ProgressBar
        ProgressBar pb = getView("ProgressBar", ProgressBar.class);
        boolean loading = getBrowser().isLoading();
        if (loading && !pb.isVisible()) {
            pb.setVisible(true);
            pb.setProgress(-1);
        } else if (!loading && pb.isVisible()) {
            pb.setProgress(0);
            pb.setVisible(false);
        }

        // Reset FilesPane and SupportTray
        _filesPane.resetLater();
        _procPane.resetLater();
        _supportTray.resetLater();
    }

    /**
     * Responds to UI panel controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle OpenMenuItem
        if (anEvent.equals("OpenMenuItem")) {
            getToolBar().selectSearchText();
            anEvent.consume();
        }

        // Handle QuitMenuItem
        if (anEvent.equals("QuitMenuItem")) {
            WelcomePanel.getShared().quitApp();
            anEvent.consume();
        }

        // Handle NewFileMenuItem, NewFileButton
        if (anEvent.equals("NewFileMenuItem") || anEvent.equals("NewFileButton")) {
            showNewFilePanel();
            anEvent.consume();
        }

        // Handle CloseMenuItem, CloseFileAction
        if (anEvent.equals("CloseMenuItem") || anEvent.equals("CloseFileAction")) {
            getToolBar().removeOpenFile(getSelectedFile());
            anEvent.consume();
        }

        // Handle ShowTrayButton
        if (anEvent.equals("ShowTrayButton")) {
            boolean isVisible = isSupportTrayVisible();
            _supportTray.setExplicitlyOpened(!isVisible);
            setSupportTrayVisible(!isVisible);
        }

        // Handle ProcessesList
        if (anEvent.equals("ProcessesList"))
            setSupportTrayIndex(2);

        // Handle ShowJavaHomeMenuItem
        if (anEvent.equals("ShowJavaHomeMenuItem")) {
            String java = System.getProperty("java.home");
            FileUtils.openFile(java);
        }

        // Handle WinClosing
        if (anEvent.isWinClose()) {
            hide();
            runLater(() -> {
                Prefs.get().flush();
                WelcomePanel.getShared().showPanel();
            });
        }
    }

    /**
     * Returns the MenuBar.
     */
    public MenuBar getMenuBar()
    {
        return _mbar != null ? _mbar : (_mbar = createMenuBar());
    }

    MenuBar _mbar;

    /**
     * Creates the MenuBar.
     */
    protected MenuBar createMenuBar()
    {
        MenuBar mbar = new MenuBar();
        Menu fileMenu = createMenu("FileMenu", "File");
        mbar.addMenu(fileMenu);
        fileMenu.addItem(createMenuItem("NewMenuItem", "New", "Shortcut+N"));
        fileMenu.addItem(new MenuItem()); //SeparatorMenuItem
        fileMenu.addItem(createMenuItem("OpenMenuItem", "Open", "Shortcut+O"));
        fileMenu.addItem(createMenuItem("CloseMenuItem", "Close", "Shortcut+W"));
        fileMenu.addItem(createMenuItem("SaveMenuItem", "Save", "Shortcut+S"));
        fileMenu.addItem(createMenuItem("SaveAsMenuItem", "Save As...", "Shortcut+Shift+S"));
        fileMenu.addItem(createMenuItem("RevertMenuItem", "Revert to Saved", "Shortcut+U"));
        fileMenu.addItem(createMenuItem("QuitMenuItem", "Quit", "Shortcut+Q"));
        Menu editMenu = createMenu("EditMenu", "Edit");
        mbar.addMenu(editMenu);
        editMenu.addItem(createMenuItem("UndoMenuItem", "Undo", "Shortcut+Z"));
        editMenu.addItem(createMenuItem("RedoMenuItem", "Redo", "Shortcut+Shift+Z"));
        editMenu.addItem(new MenuItem()); //SeparatorMenuItem
        editMenu.addItem(createMenuItem("CutMenuItem", "Cut", "Shortcut+X"));
        editMenu.addItem(createMenuItem("CopyMenuItem", "Copy", "Shortcut+C"));
        editMenu.addItem(createMenuItem("PasteMenuItem", "Paste", "Shortcut+V"));
        editMenu.addItem(createMenuItem("SelectAllMenuItem", "Select All", "Shortcut+A"));
        Menu helpMenu = createMenu("HelpMenu", "Help");
        mbar.addMenu(helpMenu);
        helpMenu.addItem(createMenuItem("SupportPageMenuItem", "Support Page", null));
        helpMenu.addItem(createMenuItem("JavaDocMenuItem", "Tutorial", null));
        helpMenu.addItem(createMenuItem("ShowJavaHomeMenuItem", "Show Java Home", null));
        mbar.setOwner(this);
        return mbar;
    }

    /**
     * Creates a JMenu for name and text.
     */
    Menu createMenu(String aName, String theText)
    {
        Menu m = new Menu();
        m.setText(theText);
        m.setName(aName);
        //m.setStyle(new Style().setFontSize(14).setPadding(2,5,3,5).toString());
        return m;
    }

    /**
     * Creates a JMenuItem for name and text and key accelerator description.
     */
    MenuItem createMenuItem(String aName, String theText, String aKey)
    {
        MenuItem mi = new MenuItem();
        mi.setText(theText);
        mi.setName(aName);
        if (aKey != null) mi.setShortcut(aKey);
        return mi;
    }

    /**
     * Returns the support tray.
     */
    public SupportTray getSupportTray()
    {
        return _supportTray;
    }

    /**
     * Returns the problems pane.
     */
    public BuildIssuesPane getProblemsPane()
    {
        return _problemsPane;
    }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()
    {
        return _runConsole;
    }

    /**
     * Returns the DebugVarsPane.
     */
    public DebugVarsPane getDebugVarsPane()
    {
        return _debugVarsPane;
    }

    /**
     * Returns the DebugExprsPane.
     */
    public DebugExprsPane getDebugExprsPane()
    {
        return _debugExprsPane;
    }

    /**
     * Returns the BreakpointsPanel.
     */
    public BreakpointsPanel getBreakpointsPanel()
    {
        return _breakpointsPanel;
    }

    /**
     * Returns the SearchPane.
     */
    public SearchPane getSearchPane()
    {
        return _searchPane;
    }

    /**
     * Saves any unsaved files.
     */
    public int saveFiles()
    {
        return _filesPane.saveFiles(getSelectedSite().getRootDir(), true);
    }

    /**
     * Runs a panel for a new file.
     */
    public void showNewFilePanel()
    {
        _filesPane.showNewFilePanel();
    }

}