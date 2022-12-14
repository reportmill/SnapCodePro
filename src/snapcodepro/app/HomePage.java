package snapcodepro.app;

import snap.gfx.Color;
import snap.gfx.GFXEnv;
import snap.view.ScrollView;
import snap.view.View;
import snap.view.ViewArchiver;
import snap.view.ViewEvent;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * A WebPage subclass that is the default homepage for SnapCode projects.
 */
public class HomePage extends WebPage {

    // Whether to do stupid animation (rotate buttons on mouse enter)
    boolean _stupidAnim;

    /**
     * Returns the browser as AppBrowser.
     */
    public AppBrowser getBrowser()
    {
        return (AppBrowser) super.getBrowser();
    }

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()
    {
        return getBrowser().getAppPane();
    }

    /**
     * Returns the AppPane RootSite.
     */
    public WebSite getRootSite()
    {
        return getAppPane().getRootSite();
    }

    /**
     * Override to put in Page pane.
     */
    protected View createUI()
    {
        return new ScrollView(super.createUI());
    }

    /**
     * Initialize UI.
     */
    public void initUI()
    {
        enableEvents("Header", MouseRelease);
        enableEvents("JavaPlayground", MouseEvents);
        enableEvents("NewSnapScene", MouseEvents);
        enableEvents("AddSnapKit", MouseEvents);
        enableEvents("AddSnapTea", MouseEvents);
        enableEvents("NewJavaFile", MouseEvents);
        enableEvents("NewFile", MouseEvents);
        enableEvents("SnapDocs", MouseEvents);
        enableEvents("RMDocs", MouseEvents);
    }

    /**
     * RespondUI.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Trigger animations on main buttons for MouseEntered/MouseExited
        if (anEvent.isMouseEnter()) {
            if (_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1.12).getRoot(1000).setRotate(180).play();
            else anEvent.getView().getAnimCleared(200).setScale(1.12).play();
        }
        if (anEvent.isMouseExit()) {
            if (_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1).getRoot(1000).setRotate(0).play();
            else anEvent.getView().getAnimCleared(200).setScale(1).play();
        }

        // Handle Header: Play click anim and toggle StupidAnim
        if (anEvent.equals("Header")) {
            View hdr = anEvent.getView();
            _stupidAnim = !_stupidAnim;
            hdr.setBorder(_stupidAnim ? Color.MAGENTA : Color.BLACK, 1);
            hdr.setScale(1.05);
            hdr.getAnimCleared(200).setScale(1).play();
        }

        // Handle NewJavaFile
        if (anEvent.equals("JavaPlayground") && anEvent.isMouseRelease()) {
            WebFile file = getRootSite().createFile("/Playground.pgd", false);
            WebPage page = getBrowser().createPage(file);
            //file = page.showNewFilePanel(getBrowser());
            if (file != null) try {
                //file.save();
                getBrowser().setFile(file);
            } catch (Exception e) {
                getBrowser().showException(file.getURL(), e);
            }
        }

        // Handle NewSnapScene
        if (anEvent.equals("NewSnapScene") && anEvent.isMouseRelease()) {
            ProjectPane ppane = ProjectPane.get(getRootSite());
            ppane.addProject("SnapKit", "https://github.com/reportmill/SnapKit.git");
            addSceneFiles(getRootSite(), "Scene1");
        }

        // Handle NewJavaFile
        if (anEvent.equals("NewJavaFile") && anEvent.isMouseRelease())
            getAppPane().showNewFilePanel();

        // Handle NewFile
        if (anEvent.equals("NewFile") && anEvent.isMouseRelease())
            getAppPane().showNewFilePanel();

        // Handle AddSnapKit
        if (anEvent.equals("AddSnapKit") && anEvent.isMouseRelease()) {
            ProjectPane ppane = ProjectPane.get(getRootSite());
            ppane.addProject("SnapKit", "https://github.com/reportmill/SnapKit.git");
        }

        // Handle AddSnapTea
        if (anEvent.equals("AddSnapTea") && anEvent.isMouseRelease()) {
            ProjectPane ppane = ProjectPane.get(getRootSite());
            if (ppane.getProject().getProjectSet().getProject("SnapKit") == null)
                ppane.addProject("SnapKit", "https://github.com/reportmill/SnapKit.git");
            if (ppane.getProject().getProjectSet().getProject("CJDom") == null)
                ppane.addProject("SnapTea", "https://github.com/reportmill/SnapTea.git");
            //if(ppane.getProject().getProjectSet().getProject("CJDom")==null)
            //    ppane.addProject("CJDom", "https://github.com/reportmill/CJDom.git");
            //if(ppane.getProject().getProjectSet().getProject("SnapCJ")==null)
            //    ppane.addProject("SnapCJ", "https://github.com/reportmill/SnapCJ.git");
        }

        // Handle SnapDocs
        if (anEvent.equals("SnapDocs") && anEvent.isMouseRelease())
            GFXEnv.getEnv().openURL("http://www.reportmill.com/snap1/javadoc");

        // Handle RMDocs
        if (anEvent.equals("RMDocs") && anEvent.isMouseRelease())
            GFXEnv.getEnv().openURL("http://www.reportmill.com/support");
        //getBrowser().setURLString("http://www.reportmill.com/support/UserGuide.pdf");
    }

    /**
     * Makes given site a Studio project.
     */
    public void addSceneFiles(WebSite aSite, String aName)
    {
        // Create/add Scene Java and UI files
        addSceneJavaFile(aSite, aName);
        WebFile snpFile = addSceneUIFile(aSite, aName);
        if (snpFile == null)
            return;

        // Add run config
        SitePane spane = SitePane.get(aSite);
        spane.setUseSnapEditor(true);
        RunConfigs rc = RunConfigs.get(aSite);
        if (rc.getRunConfig() == null) {
            rc.getRunConfigs().add(new RunConfig().setName("StudioApp").setMainClassName("Scene1"));
            rc.writeFile();
        }

        // Select and show snp file
        getBrowser().setFile(snpFile);
        getAppPane().getFilesPane().showInTree(snpFile);
    }

    /**
     * Creates the Scene1.snp file.
     */
    protected WebFile addSceneUIFile(WebSite aSite, String aName)
    {
        // Get snap file (return if already exists)
        String path = "/src/" + aName + ".snp";
        WebFile sfile = aSite.getFileForPath(path);
        if (sfile != null) return null;

        // Create content
        snap.viewx.SnapScene scene = new snap.viewx.SnapScene();
        scene.setSize(800, 500);
        String str = new ViewArchiver().writeToXML(scene).getString();

        // Create file, set content, save and return
        sfile = aSite.createFile(path, false);
        sfile.setText(str);
        try {
            sfile.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sfile.setProp("OpenInEditor", true);
        return sfile;
    }

    /**
     * Creates the Scene file.
     */
    protected WebFile addSceneJavaFile(WebSite aSite, String aName)
    {
        // Get snap file (return if already exists)
        String path = "/src/" + aName + ".java";
        WebFile jfile = aSite.getFileForPath(path);
        if (jfile != null) return null;

        // Create content
        StringBuffer sb = new StringBuffer();
        sb.append("import snap.view.*;\n\n");
        sb.append("import snap.viewx.*;\n\n");
        sb.append("/**\n").append(" * A SnapStudio SceneOwner subclass. SnapEdit=true.\n").append(" */\n");
        sb.append("public class Scene1 extends SnapSceneOwner {\n\n");
        sb.append("/**\n").append(" * Initialize UI.\n").append(" */\n");
        sb.append("protected void initUI()\n").append("{\n}\n\n");
        sb.append("/**\n").append(" * Reset UI.\n").append(" */\n");
        sb.append("protected void resetUI()\n").append("{\n}\n\n");
        sb.append("/**\n").append(" * Respond to UI changes.\n").append(" */\n");
        sb.append("protected void respondUI(ViewEvent anEvent)\n").append("{\n}\n\n");
        sb.append("/**\n").append(" * Called on every frame.\n").append(" */\n");
        sb.append("public void act()\n").append("{\n}\n\n");
        sb.append("/**\n").append(" * Standard main method.\n").append(" */\n");
        sb.append("public static void main(String args[])\n{\n");
        sb.append("    new Scene1().setWindowVisible(true);\n}\n\n");
        sb.append("}");

        // Create file, set content, save and return
        jfile = aSite.createFile(path, false);
        jfile.setText(sb.toString());
        try {
            jfile.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return jfile;
    }

    /**
     * Override to suppress.
     */
    public void reload()
    {
    }

    /**
     * Return better title.
     */
    public String getTitle()
    {
        return getRootSite().getName() + " Home Page";
    }

}