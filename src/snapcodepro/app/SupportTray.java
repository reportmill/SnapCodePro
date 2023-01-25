package snapcodepro.app;

import snap.view.*;

/**
 * A class to hold TabView for ProblemsPane, RunConsole, DebugPane.
 */
public class SupportTray extends ViewOwner {

    // The AppPane
    AppPane _appPane;

    // The tabbed pane
    TabView _tpane;

    // The list of tab owners
    ViewOwner[] _tabOwners;

    // Whether tray was explicitly opened
    boolean _explicitlyOpened;

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
        return _tpane != null ? _tpane.getSelIndex() : -1;
    }

    /**
     * Sets the selected index.
     */
    public void setSelIndex(int anIndex)
    {
        _tpane.setSelIndex(anIndex);
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
        _tpane = new TabView();
        _tpane.setName("TabView");
        _tpane.setFont(_tpane.getFont().deriveFont(12));
        _tpane.getTabBar().setTabMinWidth(70);
        _tpane.addTab("Problems", _appPane.getProblemsPane().getUI());
        _tpane.addTab("Console", new Label("RunConsole"));
        _tpane.addTab("Variables", new Label("DebugVarsPane"));
        _tpane.addTab("Expressions", new Label("DebugExprsPane"));
        _tpane.addTab("Breakpoints", new Label("Breakpoints"));
        _tpane.addTab("Search", new Label("Search"));
        return _tpane;
    }

    /**
     * Override to reset selected tab.
     */
    protected void resetUI()
    {
        int index = _tpane.getSelIndex();
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
        if (_tpane.getTabContent(_tpane.getSelIndex()) instanceof Label) {
            int index = _tpane.getSelIndex();
            ViewOwner sowner = _tabOwners[index];
            _tpane.setTabContent(sowner.getUI(), index);
        }
    }

}