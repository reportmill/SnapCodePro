package snapcodepro.app;
import snap.util.ArrayUtils;
import snap.viewx.TextPage;
import snap.viewx.WebBrowser;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebResponse;

/**
 * A browser for the Snap app.
 */
public class AppBrowser extends WebBrowser {

    // The AppPane that owns this browser
    AppPane _appPane;

    // The ProjectFilesPane
    private ProjectFilesPane  _projectFilesPane;

    /**
     * Constructor.
     */
    public AppBrowser(AppPane appPane)
    {
        super();
        _appPane = appPane;
        _projectFilesPane = appPane.getProjFilesPane();
    }

    /**
     * Override to make sure that AppPane is in sync.
     */
    public void setPage(WebPage aPage)
    {
        // Do normal version
        if (aPage == getPage()) return;
        super.setPage(aPage);

        // Forward to AppPane and AppPaneToolBar
        WebFile file = aPage != null ? aPage.getFile() : null;
        _projectFilesPane.setSelectedFile(file);
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
        if (file != null && file.isRoot() && ArrayUtils.containsId(_appPane.getSites(), file.getSite()))
            return SitePane.SitePage.class;

        // Handle Java
        if (type.equals("java")) {
            if (file != null && SnapEditorPage.isSnapEditSet(file))
                return SnapEditorPage.class;
            return JavaPage.class;
        }

        //if(type.equals("snp") && file.getName().equals("Scene1.snp")) return studio.app.EditorPage.class;

        if (type.equals("rpt")) return getPageClass("com.reportmill.app.ReportPageEditor", TextPage.class);
        //if(type.equals("snp")) return snapbuild.app.EditorPage.class;
        if (type.equals("class") && ArrayUtils.containsId(_appPane.getSites(), file.getSite())) return ClassInfoPage.class;
        if (type.equals("pgd")) return JavaShellPage.class;

        // Do normal version
        return super.getPageClass(aResp);
    }
}