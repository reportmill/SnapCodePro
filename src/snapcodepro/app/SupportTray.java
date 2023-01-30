package snapcodepro.app;
import snap.geom.Side;
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
    public int getSelIndex()  { return _tabView != null ? _tabView.getSelIndex() : -1; }

    /**
     * Sets the selected index.
     */
    public void setSelIndex(int anIndex)
    {
        _tabView.setSelIndex(anIndex);
    }

    /**
     * Shows the problems tool.
     */
    public void showProblemsTool()  { setSelIndex(PROBLEMS_PANE); }

    /**
     * Shows the search tool.
     */
    public void showSearchTool()  { setSelIndex(SEARCH_PANE); }

    /**
     * Shows the run tool.
     */
    public void showRunTool()  { setSelIndex(RUN_PANE); }

    /**
     * Sets selected index to debug.
     */
    public void showDebugTool()  { setSelIndex(DEBUG_PANE_VARS); }

    /**
     * Hides selected tool.
     */
    public void hideTools()  { setSelIndex(-1); }

    /**
     * Creates UI for SupportTray.
     */
    protected View createUI()
    {
        // Set TabOwners
        _tabOwners = new ViewOwner[] {
                _appPane.getProblemsPane(), _appPane.getRunConsole(),
                _appPane.getDebugVarsPane(), _appPane.getDebugExprsPane(),
                _appPane.getBreakpointsPanel(), _appPane.getSearchPane()
        };

        // Create/config TabView
        _tabView = new TabView();
        _tabView.setName("TabView");
        _tabView.setFont(_tabView.getFont().deriveFont(12));
        _tabView.setTabSide(Side.BOTTOM);
        _tabView.getTabBar().setTabMinWidth(70);
        _tabView.getTabBar().setAllowEmptySelection(true);

        // Bogus: Manually set TabView PrefHeight
        _tabView.addPropChangeListener(pc -> {
            double prefH = _tabView.getContent() != null ? 240 : 30;
            _tabView.setPrefHeight(prefH);
        }, TabView.SelIndex_Prop);

        // Add Tabs
        _tabView.addTab("Problems", _appPane.getProblemsPane().getUI());
        _tabView.addTab("Console", new Label("RunConsole"));
        _tabView.addTab("Variables", new Label("DebugVarsPane"));
        _tabView.addTab("Expressions", new Label("DebugExprsPane"));
        _tabView.addTab("Breakpoints", new Label("Breakpoints"));
        _tabView.addTab("Search", new Label("Search"));

        // Return
        return _tabView;
    }

    /**
     * Override to reset selected tab.
     */
    protected void resetUI()
    {
        int selIndex = _tabView.getSelIndex();
        ViewOwner viewOwner = selIndex >= 0 ? _tabOwners[selIndex] : null;
        if (viewOwner != null)
            viewOwner.resetLater();
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle TabView
        int selIndex = _tabView.getSelIndex();
        View selContent = selIndex >= 0 ? _tabView.getTabContent(selIndex) : null;
        if (selContent instanceof Label) {
            ViewOwner viewOwner = _tabOwners[selIndex];
            _tabView.setTabContent(viewOwner.getUI(), selIndex);
        }
    }
}