package snapcodepro.app;
import javakit.project.Breakpoint;
import javakit.project.Breakpoints;
import javakit.project.BuildIssue;
import javakit.ide.JavaTextPane;
import snapcodepro.debug.RunApp;
import snapcodepro.project.ProjectX;
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
    private List<WebSite>  _sites = new ArrayList<>();

    // The ProjectFilesPane
    private ProjectFilesPane  _projFilesPane;

    // The AppPaneToolBar
    protected AppPaneToolBar  _toolBar = new AppPaneToolBar(this);

    // The main SplitView that holds sidebar and browser
    private SplitView  _mainSplit;

    // The SplitView that holds FilesPane and ProcPane
    private SplitView  _sideBarSplit;

    // The FilesPane
    protected AppFilesPane  _filesPane = new AppFilesPane(this);

    // The ProcPane manages run/debug processes
    private ProcPane  _procPane = new ProcPane(this);

    // The pane that the browser sits in
    private SplitView _browserBox;

    // The SupportTray
    private SupportTray  _supportTray = new SupportTray(this);

    // The Problems pane
    private BuildIssuesPane  _problemsPane = new BuildIssuesPane(this);

    // The RunConsole
    private RunConsole  _runConsole = new RunConsole(this);

    // The DebugVarsPane
    private DebugVarsPane  _debugVarsPane = new DebugVarsPane(this);

    // The DebugExprsPane
    private DebugExprsPane  _debugExprsPane = new DebugExprsPane(this);

    // The BreakpointsPanel
    private BreakpointsPanel  _breakpointsPanel = new BreakpointsPanel(this);

    // The SearchPane
    private SearchPane  _searchPane = new SearchPane(this);

    // Whether to show side bar
    private boolean  _showSideBar = true;

    // The menu bar
    private MenuBar  _menuBar;

    // The currently open AppPane
    private static AppPane  _openAppPane;

    // A PropChangeListener to watch for site file changes
    private PropChangeListener  _siteFileLsnr = pc -> siteFileChanged(pc);

    /**
     * Constructor.
     */
    public AppPane()
    {
        super();

        _projFilesPane = new ProjectFilesPane(this);
    }

    /**
     * Returns the FilesPane.
     */
    public ProjectFilesPane getProjFilesPane()  { return _projFilesPane; }

    /**
     * Returns the browser.
     */
    public AppBrowser getBrowser()  { return _projFilesPane.getBrowser(); }

    /**
     * Returns the toolbar.
     */
    public AppPaneToolBar getToolBar()  { return _toolBar; }

    /**
     * Returns the files pane.
     */
    public AppFilesPane getFilesPane()  { return _filesPane; }

    /**
     * Returns the processes pane.
     */
    public ProcPane getProcPane()  { return _procPane; }

    /**
     * Returns whether is showing SideBar (holds FilesPane and ProcPane).
     */
    public boolean isShowSideBar()  { return _showSideBar; }

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
        return _browserBox.getItemCount() > 1;
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

        // Add/remove SupportTrayUI with animator
        if (aValue)
            _browserBox.addItemWithAnim(supTrayUI, 240);
        else _browserBox.removeItemWithAnim(supTrayUI);

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
    public WebSite getRootSite()  { return _sites.get(0); }

    /**
     * Returns the number of sites.
     */
    public int getSiteCount()  { return _sites.size(); }

    /**
     * Returns the individual site at the given index.
     */
    public WebSite getSite(int anIndex)  { return _sites.get(anIndex); }

    /**
     * Returns the list of sites.
     */
    public List<WebSite> getSites()  { return _sites; }

    /**
     * Adds a site to sites list.
     */
    public void addSite(WebSite aSite)
    {
        // If site already added, just return
        if (_sites.contains(aSite)) return;

        // Create project for site
        ProjectX proj = ProjectX.getProjectForSite(aSite);
        if (proj == null)
            proj = new ProjectX(aSite);

        // Add listener to update ProcPane and JavaPage.TextArea(s) when Breakpoint/BuildIssue added/removed
        proj.getBreakpoints().addPropChangeListener(pc -> projBreakpointsDidChange(pc));
        proj.getBuildIssues().addPropChangeListener(pc -> projBuildIssuesDidChange(pc));

        // Add site
        _sites.add(aSite);
        SitePane sitePane = SitePane.get(aSite, true);
        sitePane.setAppPane(this);
        aSite.addFileChangeListener(_siteFileLsnr);

        // Add dependent sites
        for (ProjectX p : proj.getProjects())
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
    public static AppPane getOpenAppPane()  { return _openAppPane; }

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
    public WebFile getSelectedFile()  { return _projFilesPane.getSelectedFile(); }

    /**
     * Sets the selected site file.
     */
    public void setSelectedFile(WebFile aFile)
    {
        _projFilesPane.setSelectedFile(aFile);
    }

    /**
     * Returns the selected site.
     */
    public WebSite getSelectedSite()
    {
        WebFile file = getSelectedFile();
        WebSite site = file != null ? file.getSite() : null;
        if (!ListUtils.containsId(getSites(), site))
            site = getSite(0);
        return site;
    }

    /**
     * Returns the build directory.
     */
    public WebFile getBuildDir()
    {
        WebSite site = getSelectedSite();
        ProjectX proj = site != null ? ProjectX.getProjectForSite(site) : null;
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
        // Get ProjectFilesPane
        View projectFilesUI = _projFilesPane.getUI();
        ColView colView = getView("BrowserAndStatusBar", ColView.class);
        colView.addChild(projectFilesUI, 0);

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
        ProgressBar progressBar = getView("ProgressBar", ProgressBar.class);
        boolean loading = getBrowser().isLoading();
        if (loading && !progressBar.isVisible()) {
            progressBar.setVisible(true);
            progressBar.setProgress(-1);
        }
        else if (!loading && progressBar.isVisible()) {
            progressBar.setProgress(0);
            progressBar.setVisible(false);
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
                Prefs.getDefaultPrefs().flush();
                WelcomePanel.getShared().showPanel();
            });
        }
    }

    /**
     * Returns the MenuBar.
     */
    public MenuBar getMenuBar()
    {
        if (_menuBar != null) return _menuBar;
        return _menuBar = createMenuBar();
    }

    /**
     * Creates the MenuBar.
     */
    protected MenuBar createMenuBar()
    {
        ViewBuilder<MenuItem> mib = new ViewBuilder<>(MenuItem.class);

        // Create FileMenu and menu items
        mib.name("NewMenuItem").text("New").save().setShortcut("Shortcut+N");
        mib.save(); //SeparatorMenuItem
        mib.name("OpenMenuItem").text("Open").save().setShortcut("Shortcut+O");
        mib.name("CloseMenuItem").text("Close").save().setShortcut("Shortcut+W");
        mib.name("SaveMenuItem").text("Save").save().setShortcut("Shortcut+S");
        mib.name("SaveAsMenuItem").text("Save As...").save().setShortcut("Shortcut+Shift+S");
        mib.name("RevertMenuItem").text("Revert to Saved").save().setShortcut("Shortcut+U");
        mib.name("QuitMenuItem").text("Quit").save().setShortcut("Shortcut+Q");
        Menu fileMenu = mib.buildMenu("FileMenu", "File");

        // Create EditMenu menu items
        mib.name("UndoMenuItem").text("Undo").save().setShortcut("Shortcut+Z");
        mib.name("RedoMenuItem").text("Redo").save().setShortcut("Shortcut+Shift+Z");
        mib.save(); //SeparatorMenuItem
        mib.name("CutMenuItem").text("Cut").save().setShortcut("Shortcut+X");
        mib.name("CopyMenuItem").text("Copy").save().setShortcut("Shortcut+C");
        mib.name("PasteMenuItem").text("Paste").save().setShortcut("Shortcut+V");
        mib.name("SelectAllMenuItem").text("Select All").save().setShortcut("Shortcut+A");
        Menu editMenu = mib.buildMenu("EditMenu", "Edit");

        // Create HelpMenu and menu items
        mib.name("SupportPageMenuItem").text("Support Page").save();
        mib.name("JavaDocMenuItem").text("Tutorial").save();
        mib.name("ShowJavaHomeMenuItem").text("Show Java Home").save();
        Menu helpMenu = mib.buildMenu("HelpMenu", "Help");

        // Create MenuBar
        MenuBar menuBar = new MenuBar();
        menuBar.addMenu(fileMenu);
        menuBar.addMenu(editMenu);
        menuBar.addMenu(helpMenu);
        menuBar.setOwner(this);

        // Return
        return menuBar;
    }

    /**
     * Returns the support tray.
     */
    public SupportTray getSupportTray()  { return _supportTray; }

    /**
     * Returns the problems pane.
     */
    public BuildIssuesPane getProblemsPane()  { return _problemsPane; }

    /**
     * Returns the RunConsole.
     */
    public RunConsole getRunConsole()  { return _runConsole; }

    /**
     * Returns the DebugVarsPane.
     */
    public DebugVarsPane getDebugVarsPane()  { return _debugVarsPane; }

    /**
     * Returns the DebugExprsPane.
     */
    public DebugExprsPane getDebugExprsPane()  { return _debugExprsPane; }

    /**
     * Returns the BreakpointsPanel.
     */
    public BreakpointsPanel getBreakpointsPanel()  { return _breakpointsPanel; }

    /**
     * Returns the SearchPane.
     */
    public SearchPane getSearchPane()  { return _searchPane; }

    /**
     * Saves any unsaved files.
     */
    public int saveFiles()
    {
        WebFile rootDir = getSelectedSite().getRootDir();
        return _filesPane.saveFiles(rootDir, true);
    }

    /**
     * Runs a panel for a new file.
     */
    public void showNewFilePanel()
    {
        _filesPane.showNewFilePanel();
    }

    /**
     * Called when Project.BreakPoints change.
     */
    private void projBreakpointsDidChange(PropChange pc)
    {
        if (pc.getPropertyName() != Breakpoints.ITEMS_PROP) return;

        // Handle Breakpoint added
        Breakpoint nval = (Breakpoint) pc.getNewValue();
        if (nval != null) {

            // Tell active processes about breakpoint change
            for (RunApp rp : getProcPane().getProcs())
                rp.addBreakpoint(nval);
        }

        // Handle Breakpoint removed
        else {

            Breakpoint oval = (Breakpoint) pc.getOldValue();

            // Make current JavaPage.TextArea resetLater
            WebPage page = getBrowser().getPage(oval.getFile().getURL());
            if (page instanceof JavaPage)
                ((JavaPage) page).getTextPane().buildIssueOrBreakPointMarkerChanged();

            // Tell active processes about breakpoint change
            for (RunApp rp : getProcPane().getProcs())
                rp.removeBreakpoint(oval);
        }
    }

    /**
     * Called when Project.BuildIssues change.
     */
    private void projBuildIssuesDidChange(PropChange pc)
    {
        if (pc.getPropertyName() != Breakpoints.ITEMS_PROP) return;

        // Handle BuildIssue added
        BuildIssue issueAdded = (BuildIssue) pc.getNewValue();
        if (issueAdded != null) {

            // Make current JavaPage.TextArea resetLater
            WebFile issueFile = issueAdded.getFile();
            WebPage page = getBrowser().getPage(issueFile.getURL());
            if (page instanceof JavaPage) {
                JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
                javaTextPane.buildIssueOrBreakPointMarkerChanged();
            }

            // Update FilesPane.FilesTree
            getFilesPane().updateFile(issueFile);
        }

        // Handle BuildIssue removed
        else {

            BuildIssue issueRemoved = (BuildIssue) pc.getOldValue();
            WebFile issueFile = issueRemoved.getFile();

            // Make current JavaPage.TextArea resetLater
            WebPage page = getBrowser().getPage(issueFile.getURL());
            if (page instanceof JavaPage) {
                JavaTextPane<?> javaTextPane = ((JavaPage) page).getTextPane();
                javaTextPane.buildIssueOrBreakPointMarkerChanged();
            }

            // Update FilesPane.FilesTree
            getFilesPane().updateFile(issueFile);
        }
    }
}