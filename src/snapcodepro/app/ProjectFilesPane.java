package snapcodepro.app;
import snap.props.PropChange;
import snap.view.View;
import snap.view.ViewOwner;
import snap.web.WebFile;

/**
 * This ViewOwner displays files for editing.
 */
public class ProjectFilesPane extends ViewOwner {

    // The ProjectPane
    private AppPane  _projPane;

    // The currently selected file
    private WebFile  _selFile;

    // The WebBrowser for displaying editors
    private AppBrowser  _browser;

    /**
     * Constructor.
     */
    public ProjectFilesPane(AppPane aProjPane)
    {
        super();
        _projPane = aProjPane;
    }

    /**
     * Returns the browser.
     */
    public AppBrowser getBrowser()
    {
        if (_browser != null) return _browser;
        getUI();
        return _browser;
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

        // Reset FilesPane
        _projPane._filesPane.resetLater();
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        _browser = new AppBrowser();
        _browser.setGrowHeight(true);
        _browser.setAppPane(_projPane);

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
}
