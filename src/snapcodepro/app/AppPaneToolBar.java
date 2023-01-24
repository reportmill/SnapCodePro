package snapcodepro.app;
import snap.geom.HPos;
import snap.geom.Polygon;
import snap.geom.VPos;
import snap.gfx.*;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebFile;
import snap.web.WebSite;
import snap.web.WebURL;
import java.util.*;

/**
 * ToolBar.
 */
public class AppPaneToolBar extends ViewOwner {

    // The AppPane
    private AppPane  _appPane;

    // The AppPane
    private ProjectFilesPane  _projFilesPane;

    // The file tabs box
    private BoxView  _fileTabsBox;

    // The view for the currently selected view
    private FileTab  _selTab;

    // A placeholder for fill from toolbar button under mouse
    private Paint  _tempFill;

    // RunConfigsPage
    private RunConfigsPage  _runConfigsPage;

    // Constant for file tab attributes
    static Font TAB_FONT = new Font("Arial Bold", 12);
    static Color TAB_COLOR = new Color(.5, .65, .8, .8);
    static Color TAB_BORDER_COLOR = new Color(.33, .33, .33, .66);
    static Border TAB_BORDER = Border.createLineBorder(TAB_BORDER_COLOR, 1);
    static Border TAB_CLOSE_BORDER1 = Border.createLineBorder(Color.BLACK, .5);
    static Border TAB_CLOSE_BORDER2 = Border.createLineBorder(Color.BLACK, 1);

    // Shared images
    static Image SIDEBAR_EXPAND = Image.get(AppPane.class, "SideBar_Expand.png");
    static Image SIDEBAR_COLLAPSE = Image.get(AppPane.class, "SideBar_Collapse.png");

    /**
     * Creates a new AppPaneToolBar.
     */
    public AppPaneToolBar(AppPane anAppPane)
    {
        _appPane = anAppPane;
        _projFilesPane = _appPane.getProjFilesPane();
    }

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()  { return _appPane; }

    /**
     * Returns the RootSite.
     */
    public WebSite getRootSite()  { return _appPane.getRootSite(); }

    /**
     * Selects the search text.
     */
    public void selectSearchText()
    {
        runLater(() -> requestFocus("SearchComboBox"));
    }

    /**
     * Override to add menu button.
     */
    protected View createUI()
    {
        // Do normal version
        SpringView uin = (SpringView) super.createUI();

        // Add MenuButton
        MenuButton menuButton = new MenuButton();
        menuButton.setName("RunMenuButton");
        menuButton.setBounds(207, 29, 15, 14);
        menuButton.setItems(Arrays.asList(getRunMenuButtonItems()));
        menuButton.getGraphicAfter().setPadding(0, 0, 0, 0);
        uin.addChild(menuButton);

        // Add FileTabsPane pane
        _fileTabsBox = new ScaleBox();
        _fileTabsBox.setPadding(4, 0, 0, 4);
        _fileTabsBox.setAlignX(HPos.LEFT);
        _fileTabsBox.setBounds(0, 45, uin.getWidth() - 10, 24);
        _fileTabsBox.setGrowWidth(true);
        _fileTabsBox.setLeanY(VPos.BOTTOM);
        uin.addChild(_fileTabsBox);
        buildFileTabs();

        // Add Expand button
        Button expandButton = new Button();
        expandButton.setName("ExpandButton");
        expandButton.setImage(SIDEBAR_EXPAND);
        expandButton.setShowArea(false);
        expandButton.setBounds(uin.getWidth() - 20, uin.getHeight() - 20, 16, 16);
        expandButton.setLeanX(HPos.RIGHT);
        expandButton.setLeanY(VPos.BOTTOM);
        uin.addChild(expandButton);

        // Set min height and return
        uin.setMinHeight(uin.getHeight());
        return uin;
    }

    /**
     * Override to set PickOnBounds.
     */
    protected void initUI()
    {
        // Get/configure SearchComboBox
        ComboBox<WebFile> searchComboBox = getView("SearchComboBox", ComboBox.class);
        searchComboBox.setItemTextFunction(itm -> itm.getName());
        searchComboBox.getListView().setItemTextFunction(itm -> itm.getName() + " - " + itm.getParent().getPath());
        searchComboBox.setPrefixFunction(s -> getFilesForPrefix(s));

        // Get/configure SearchComboBox.PopupList
        PopupList<?> searchPopup = searchComboBox.getPopupList();
        searchPopup.setRowHeight(22);
        searchPopup.setPrefWidth(300);
        searchPopup.setMaxRowCount(15);
        searchPopup.setAltPaint(Color.get("#F8F8F8"));

        // Get/configure SearchText: radius, prompt, image, animation
        TextField searchText = searchComboBox.getTextField();
        searchText.setBorderRadius(8);
        searchText.setPromptText("Search");
        searchText.getLabel().setImage(Image.get(TextPane.class, "Find.png"));
        TextField.setBackLabelAlignAnimatedOnFocused(searchText, true);

        // Enable events on buttons
        String[] bnames = {"HomeButton", "BackButton", "NextButton", "RefreshButton", "RunButton"};
        for (String name : bnames) enableEvents(name, MouseRelease, MouseEnter, MouseExit);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        Image img = getAppPane().isShowSideBar() ? SIDEBAR_EXPAND : SIDEBAR_COLLAPSE;
        getView("ExpandButton", Button.class).setImage(img);
    }

    /**
     * Respond to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Get AppPane and AppBrowser
        AppPane appPane = getAppPane();
        WebBrowser appBrowser = _projFilesPane.getBrowser();

        // Handle MouseEnter: Make buttons glow
        if (anEvent.isMouseEnter() && anEvent.getView() != _selTab) {
            View view = anEvent.getView();
            _tempFill = view.getFill();
            view.setFill(Color.WHITE);
            return;
        }

        // Handle MouseExit: Restore fill
        if (anEvent.isMouseExit() && anEvent.getView() != _selTab) {
            View view = anEvent.getView();
            view.setFill(_tempFill);
            return;
        }

        // Handle HomeButton
        if (anEvent.equals("HomeButton") && anEvent.isMouseRelease())
            appPane.showHomePage();

        // Handle LastButton, NextButton
        if (anEvent.equals("BackButton") && anEvent.isMouseRelease())
            appBrowser.trackBack();
        if (anEvent.equals("NextButton") && anEvent.isMouseRelease())
            appBrowser.trackForward();

        // Handle RefreshButton
        if (anEvent.equals("RefreshButton") && anEvent.isMouseRelease())
            appBrowser.reloadPage();

        // Handle RunButton
        if (anEvent.equals("RunButton") && anEvent.isMouseRelease()) appPane._filesPane.run();

        // Handle RunConfigMenuItems
        if (anEvent.getName().endsWith("RunConfigMenuItem")) {
            String name = anEvent.getName().replace("RunConfigMenuItem", "");
            RunConfigs runConfigs = RunConfigs.get(getRootSite());
            RunConfig runConfig = runConfigs.getRunConfig(name);
            if (runConfig != null) {
                runConfigs.getRunConfigs().remove(runConfig);
                runConfigs.getRunConfigs().add(0, runConfig);
                runConfigs.writeFile();
                setRunMenuButtonItems();
                appPane._filesPane.run();
            }
        }

        // Handle RunConfigsMenuItem
        if (anEvent.equals("RunConfigsMenuItem"))
            appBrowser.setURL(getRunConfigsPageURL());

        // Show history
        if (anEvent.equals("ShowHistoryMenuItem"))
            _projFilesPane.showHistory();

        // Handle FileTab
        if (anEvent.equals("FileTab") && anEvent.isMouseRelease())
            handleFileTabClicked(anEvent);

        // Handle SearchComboBox
        if (anEvent.equals("SearchComboBox"))
            handleSearchComboBox(anEvent);

        // Handle ExpandButton
        if (anEvent.equals("ExpandButton")) {
            boolean showSideBar = !appPane.isShowSideBar();
            appPane.setShowSideBar(showSideBar);
        }
    }

    /**
     * Returns the RunConfigsPage.
     */
    public RunConfigsPage getRunConfigsPage()
    {
        if (_runConfigsPage != null) return _runConfigsPage;
        _runConfigsPage = new RunConfigsPage();
        _projFilesPane.setPageForURL(_runConfigsPage.getURL(), _runConfigsPage);
        return _runConfigsPage;
    }

    /**
     * Returns the RunConfigsPageURL.
     */
    public WebURL getRunConfigsPageURL()
    {
        return getRunConfigsPage().getURL();
    }

    /**
     * Handle FileTab clicked.
     */
    protected void handleFileTabClicked(ViewEvent anEvent)
    {
        FileTab fileTab = anEvent.getView(FileTab.class);
        WebFile file = fileTab.getFile();

        // Handle single click
        if (anEvent.getClickCount() == 1) {
            _projFilesPane.getBrowser().setTransition(WebBrowser.Instant);
            _projFilesPane.setSelectedFile(file);
        }

        // Handle double click
        else if (anEvent.getClickCount() == 2) {
            WebBrowserPane browserPane = new WebBrowserPane();
            browserPane.getUI().setPrefSize(800, 800);
            browserPane.getBrowser().setURL(file.getURL());
            browserPane.getWindow().show(getUI().getRootView(), 600, 200);
        }
    }

    /**
     * Handle SearchComboBox changes.
     */
    public void handleSearchComboBox(ViewEvent anEvent)
    {
        // Get selected file and/or text
        WebFile file = (WebFile) anEvent.getSelItem();
        String text = anEvent.getStringValue();

        // If file available, open file
        if (file != null)
            _projFilesPane.setBrowserFile(file);

            // If text available, either open URL or search for string
        else if (text != null && text.length() > 0) {
            int colon = text.indexOf(':');
            if (colon > 0 && colon < 6) {
                WebURL url = WebURL.getURL(text);
                _projFilesPane.setBrowserURL(url);
            }
            else {
                _appPane.getSearchPane().search(text);
                _appPane.setSupportTrayIndex(SupportTray.SEARCH_PANE);
            }
        }

        // Clear SearchComboBox
        setViewText("SearchComboBox", null);
    }

    /**
     * Creates a pop-up menu for preview edit button (currently with look and feel options).
     */
    private MenuItem[] getRunMenuButtonItems()
    {
        ViewBuilder<MenuItem> mib = new ViewBuilder<>(MenuItem.class);

        // Add RunConfigs MenuItems
        List<RunConfig> runConfigs = RunConfigs.get(getRootSite()).getRunConfigs();
        for (RunConfig runConfig : runConfigs) {
            String name = runConfig.getName() + "RunConfigMenuItem";
            mib.name(name).text(name).save();
        }

        // Add separator
        if (runConfigs.size() > 0)
            mib.save();

        // Add RunConfigsMenuItem
        mib.name("RunConfigsMenuItem").text("Run Configurations...").save();
        mib.name("ShowHistoryMenuItem").text("Show History...").save();

        // Return MenuItems
        return mib.buildAll();
    }

    /**
     * Sets the RunMenuButton items.
     */
    public void setRunMenuButtonItems()
    {
        MenuButton rmb = getView("RunMenuButton", MenuButton.class);
        rmb.setItems(Arrays.asList(getRunMenuButtonItems()));
        for (MenuItem mi : rmb.getItems())
            mi.setOwner(this);
    }

    /**
     * Builds the file tabs.
     */
    public void buildFileTabs()
    {
        // If not on event thread, come back on that
        if (!isEventThread()) {
            runLater(() -> buildFileTabs());
            return;
        }

        // Clear selected view
        _selTab = null;

        // Create HBox for tabs
        RowView hbox = new RowView();
        hbox.setSpacing(2);

        // Iterate over OpenFiles, create FileTabs, init and add
        WebFile[] openFiles = _projFilesPane.getOpenFiles();
        for (WebFile file : openFiles) {
            Label bm = new FileTab(file);
            bm.setOwner(this);
            enableEvents(bm, MouseEvents);
            hbox.addChild(bm);
        }

        // Add box
        _fileTabsBox.setContent(hbox);
    }

    /**
     * Returns a list of files for given prefix.
     */
    private List<WebFile> getFilesForPrefix(String aPrefix)
    {
        if (aPrefix.length() == 0) return Collections.EMPTY_LIST;
        List<WebFile> files = new ArrayList<>();

        for (WebSite site : getAppPane().getSites())
            getFilesForPrefix(aPrefix, site.getRootDir(), files);
        files.sort(_fileComparator);
        return files;
    }

    /**
     * Gets files for given name prefix.
     */
    private void getFilesForPrefix(String aPrefix, WebFile aFile, List<WebFile> theFiles)
    {
        // If hidden file, just return
        SitePane sitePane = SitePane.get(aFile.getSite());
        if (sitePane.isHiddenFile(aFile))
            return;

        // If directory, recurse
        if (aFile.isDir()) for (WebFile file : aFile.getFiles())
            getFilesForPrefix(aPrefix, file, theFiles);

            // If file that starts with prefix, add to files
        else if (StringUtils.startsWithIC(aFile.getName(), aPrefix))
            theFiles.add(aFile);
    }

    /**
     * Comparator for files.
     */
    Comparator<WebFile> _fileComparator = (o1, o2) -> {
        int c = o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
        return c != 0 ? c : o1.getName().compareToIgnoreCase(o2.getName());
    };

    /**
     * A class to represent a rounded label.
     */
    protected class FileTab extends Label {

        // The File
        WebFile _file;

        /**
         * Creates a new FileTab for given file.
         */
        public FileTab(WebFile aFile)
        {
            // Create label for file and configure
            _file = aFile;
            setText(aFile.getName());
            setFont(TAB_FONT);
            setName("FileTab");
            setPrefHeight(19);
            setMaxHeight(19);
            setBorder(TAB_BORDER);
            setBorderRadius(6);
            setPadding(1, 2, 1, 4);
            setFill(TAB_COLOR);

            WebFile selFile = _projFilesPane.getSelectedFile();
            if (aFile == selFile) {
                setFill(Color.WHITE);
                _selTab = this;
            }

            // Add a close box graphic
            Polygon poly = new Polygon(0, 2, 2, 0, 5, 3, 8, 0, 10, 2, 7, 5, 10, 8, 8, 10, 5, 7, 2, 10, 0, 8, 3, 5);
            ShapeView sview = new ShapeView(poly);
            sview.setBorder(TAB_CLOSE_BORDER1);
            sview.setPrefSize(11, 11);
            sview.addEventFilter(e -> handleTabCloseBoxEvent(e), MouseEnter, MouseExit, MouseRelease);
            setGraphicAfter(sview);
        }

        /**
         * Returns the file.
         */
        public WebFile getFile()  { return _file; }

        /**
         * Called for events on tab close button.
         */
        private void handleTabCloseBoxEvent(ViewEvent anEvent)
        {
            View cbox = anEvent.getView();
            if (anEvent.isMouseEnter()) {
                cbox.setFill(Color.CRIMSON);
                cbox.setBorder(TAB_CLOSE_BORDER2);
            } else if (anEvent.isMouseExit()) {
                cbox.setFill(null);
                cbox.setBorder(TAB_CLOSE_BORDER1);
            } else if (anEvent.isMouseRelease()) {
                if (anEvent.isAltDown())
                    _projFilesPane.removeAllOpenFilesExcept(_file);
                else _projFilesPane.removeOpenFile(_file);
            }
            anEvent.consume();
        }
    }
}