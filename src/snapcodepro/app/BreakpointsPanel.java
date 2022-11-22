package snapcodepro.app;
import javakit.ide.Breakpoint;
import javakit.ide.Breakpoints;
import snap.viewx.BindingViewOwner;
import snapcodepro.project.Project;
import snap.view.ListView;
import snap.view.ViewEvent;

/**
 * A custom class.
 */
public class BreakpointsPanel extends BindingViewOwner {

    // The AppPane
    AppPane _appPane;

    // The Project
    Project _proj;

    // The breakpoints list
    ListView<Breakpoint> _bpList;

    /**
     * Creates a new BreakpointsPanel.
     */
    public BreakpointsPanel(AppPane anAP)
    {
        _appPane = anAP;
    }

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()
    {
        return _appPane;
    }

    /**
     * Returns the project.
     */
    public Project getProject()
    {
        return _proj != null ? _proj : (_proj = Project.getProjectForSite(_appPane.getRootSite()));
    }

    /**
     * Returns the list of Breakpoints.
     */
    public Breakpoints getBreakpoints()
    {
        return getProject().getBreakpoints();
    }

    /**
     * Returns the selected Breakpoint.
     */
    public Breakpoint getSelectedBreakpoint()
    {
        return _bpList.getSelItem();
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        _bpList = getView("BreakpointList", ListView.class);
        enableEvents(_bpList, MouseRelease);
        _bpList.setRowHeight(24);
        getBreakpoints().addPropChangeListener(pce -> resetLater());
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle DeleteButton
        if (anEvent.equals("DeleteButton"))
            getProject().getBreakpoints().remove(getSelectedBreakpoint());

        // Handle DeleteAllButton
        if (anEvent.equals("DeleteAllButton"))
            getProject().getBreakpoints().clear();

        // Handle BreakpointList
        if (anEvent.equals("BreakpointList") && anEvent.getClickCount() == 2) {
            Breakpoint bp = getSelectedBreakpoint();
            String urls = bp.getFile().getURL().getString() + "#LineNumber=" + (bp.getLine() + 1);
            getAppPane().getBrowser().setURLString(urls);
        }
    }

}