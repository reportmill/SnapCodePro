package snapcodepro.app;
import snap.view.*;

/**
 * A class to hold TabView for ProblemsPane, RunConsole, DebugPane.
 */
public class SupportTray extends ViewOwner {

    // The AppPane
    private AppPane  _appPane;

    // The tab view
    private TabView  _tabView;

    // The list of tab owners
    private ViewOwner[]  _tabOwners;

    // Whether tray was explicitly opened
    private boolean  _explicitlyOpened;

    // Constants for tabs
    public static final int PROBLEMS_PANE = 0;
    public static final int RUN_PANE = 1;
    public static final int DEBUG_PANE_VARS = 2;
    public static final int DEBUG_PANE_EXPRS = 3;
    public static final int BREAKPOINTS_PANE = 4;
    public static final int SEARCH_PANE = 5;

    /**
     * Creates a new SupportTray for given AppPane.
     */
    public SupportTray(AppPane anAppPane)
    {
        _appPane = anAppPane;
    }

    /**
     * Returns the selected index.
     */
    public int getSelIndex()
    {
        return _tabView != null ? _tabView.getSelIndex() : -1;
    }

    /**
     * Sets the selected index.
     */
    public void setSelIndex(int anIndex)
    {
        _tabView.setSelIndex(anIndex);
    }

    /**
     * Sets selected index to debug.
     */
    public void setDebug()
    {
        int ind = getSelIndex();
        if (ind != DEBUG_PANE_VARS && ind != DEBUG_PANE_EXPRS)
            _appPane.setSupportTrayIndex(DEBUG_PANE_VARS);
    }

    /**
     * Returns whether SupportTray was explicitly opened ("Show Tray" button was pressed).
     */
    public boolean isExplicitlyOpened()
    {
        return _explicitlyOpened;
    }

    /**
     * Sets whether SupportTray was explicitly opened ("Show Tray" button was pressed).
     */
    public void setExplicitlyOpened(boolean aValue)
    {
        _explicitlyOpened = aValue;
    }

    /**
     * Creates UI for SupportTray.
     */
    protected View createUI()
    {
        // Set TabOwners
        _tabOwners = new ViewOwner[]{_appPane.getProblemsPane(), _appPane.getRunConsole(), _appPane.getDebugVarsPane(),
                _appPane.getDebugExprsPane(), _appPane.getBreakpointsPanel(), _appPane.getSearchPane()};

        // Create TabbedPane, configure and return
        _tabView = new TabView();
        _tabView.setName("TabView");
        _tabView.setFont(_tabView.getFont().deriveFont(12));
        _tabView.getTabBar().setTabMinWidth(70);
        _tabView.addTab("Problems", _appPane.getProblemsPane().getUI());
        _tabView.addTab("Console", new Label("RunConsole"));
        _tabView.addTab("Variables", new Label("DebugVarsPane"));
        _tabView.addTab("Expressions", new Label("DebugExprsPane"));
        _tabView.addTab("Breakpoints", new Label("Breakpoints"));
        _tabView.addTab("Search", new Label("Search"));
        return _tabView;
    }

    /**
     * Override to reset selected tab.
     */
    protected void resetUI()
    {
        int index = _tabView.getSelIndex();
        ViewOwner sowner = _tabOwners[index];
        if (sowner != null)
            sowner.resetLater();
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle TabView
        int selIndex = _tabView.getSelIndex();
        View selContent = _tabView.getTabContent(selIndex);
        if (selContent instanceof Label) {
            ViewOwner viewOwner = _tabOwners[selIndex];
            _tabView.setTabContent(viewOwner.getUI(), selIndex);
        }

        // Open or close panel
        _appPane.setSupportTrayVisible(selIndex >= 0);
    }
}