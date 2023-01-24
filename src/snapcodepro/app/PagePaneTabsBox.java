/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcodepro.app;
import snap.geom.HPos;
import snap.geom.Polygon;
import snap.gfx.Border;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.view.*;
import snap.viewx.WebBrowser;
import snap.web.WebFile;

/**
 * This class manages the file tabs for PagePane open files.
 */
public class PagePaneTabsBox extends ViewOwner {

    // The PagePane
    private PagePane  _pagePane;

    // The file tabs box
    private BoxView  _fileTabsBox;

    // The view for the currently selected view
    private FileTab  _selTab;

    // Constant for file tab attributes
    private static Color BOX_BORDER_COLOR = Color.GRAY7;
    private static Font TAB_FONT = new Font("Arial", 12);
    private static Color TAB_TEXT_COLOR = Color.GRAY2;
    private static Color TAB_COLOR = null; //new Color(.5, .65, .8, .8);
    private static Color TAB_COLOR_SEL = Color.WHITE;
    private static int TAB_HEIGHT = 19;
    private static Color TAB_BORDER_COLOR = Color.GRAY8;
    private static Border TAB_BORDER = Border.createLineBorder(TAB_BORDER_COLOR, 1);
    private static Border TAB_CLOSE_BORDER1 = Border.createLineBorder(Color.BLACK, .5);
    private static Border TAB_CLOSE_BORDER2 = Border.createLineBorder(Color.BLACK, 1);

    /**
     * Constructor.
     */
    public PagePaneTabsBox(PagePane aPagePane)
    {
        super();
        _pagePane = aPagePane;
    }

    /**
     * Create UI.
     */
    @Override
    protected View createUI()
    {
        // Add FileTabsPane pane
        _fileTabsBox = new ScaleBox();
        _fileTabsBox.setBorder(BOX_BORDER_COLOR, 1);
        _fileTabsBox.setPadding(3, 3, 3, 5);
        _fileTabsBox.setAlignX(HPos.LEFT);
        _fileTabsBox.setGrowWidth(true);

        // Set PrefHeight so it will show when empty
        _fileTabsBox.setPrefHeight(_fileTabsBox.getPadding().getHeight() + TAB_HEIGHT + 2);

        // Register to build tabs whenever PagePage changes
        _pagePane.addPropChangeListener(pc -> buildFileTabs(), PagePane.OpenFiles_Prop);

        // Return
        return _fileTabsBox;
    }

    /**
     * Respond to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle MouseEnter: Make buttons glow
        if (anEvent.isMouseEnter() && anEvent.getView() != _selTab) {
            View view = anEvent.getView();
            view.setFill(TAB_COLOR_SEL);
            return;
        }

        // Handle MouseExit: Restore fill
        if (anEvent.isMouseExit() && anEvent.getView() != _selTab) {
            View view = anEvent.getView();
            view.setFill(TAB_COLOR);
            return;
        }

        // Handle FileTab
        if (anEvent.equals("FileTab") && anEvent.isMouseRelease())
            handleFileTabMouseClick(anEvent);
    }

    /**
     * Handle FileTab clicked.
     */
    protected void handleFileTabMouseClick(ViewEvent anEvent)
    {
        FileTab fileTab = anEvent.getView(FileTab.class);
        WebFile file = fileTab._file;

        // Handle single click
        if (anEvent.getClickCount() == 1) {
            _pagePane.getBrowser().setTransition(WebBrowser.Instant);
            _pagePane.setSelectedFile(file);

            if (_selTab != null)
                _selTab.setFill(TAB_COLOR);
            _selTab = fileTab;
            _selTab.setFill(TAB_COLOR_SEL);
        }

        // Handle double click
        /* else if (anEvent.getClickCount() == 2) {
            WebBrowserPane browserPane = new WebBrowserPane();
            browserPane.getUI().setPrefSize(800, 800);
            browserPane.getBrowser().setURL(file.getURL());
            browserPane.getWindow().show(getUI().getRootView(), 600, 200);
        }*/
    }

    /**
     * Builds the file tabs.
     */
    private void buildFileTabs()
    {
        // If not on event thread, come back on that
        if (!isEventThread()) {
            runLater(() -> buildFileTabs());
            return;
        }

        // Clear selected view
        _selTab = null;

        // Create HBox for tabs
        RowView rowView = new RowView();
        rowView.setSpacing(4);

        // Iterate over OpenFiles, create FileTabs, init and add
        WebFile[] openFiles = _pagePane.getOpenFiles();
        for (WebFile file : openFiles) {
            Label fileTab = new FileTab(file);
            fileTab.setOwner(this);
            enableEvents(fileTab, MouseEvents);
            rowView.addChild(fileTab);
        }

        // Add box
        _fileTabsBox.setContent(rowView);
    }

    /**
     * A class to represent a rounded label.
     */
    protected class FileTab extends Label {

        // The File
        private WebFile _file;

        /**
         * Creates a new FileTab for given file.
         */
        public FileTab(WebFile aFile)
        {
            // Create label for file and configure
            _file = aFile;
            setText(aFile.getName());
            setFont(TAB_FONT);
            setTextFill(TAB_TEXT_COLOR);
            setName("FileTab");
            setPrefHeight(TAB_HEIGHT);
            setBorder(TAB_BORDER);
            setBorderRadius(4);
            setPadding(1, 2, 1, 4);
            setFill(TAB_COLOR);

            // If selected, configure selected
            WebFile selFile = _pagePane.getSelectedFile();
            if (aFile == selFile) {
                setFill(TAB_COLOR_SEL);
                _selTab = this;
            }

            // Create close box polygon
            Polygon poly = new Polygon(0, 2, 2, 0, 5, 3, 8, 0, 10, 2, 7, 5, 10, 8, 8, 10, 5, 7, 2, 10, 0, 8, 3, 5);

            // Create close box ShapeView
            ShapeView closeBox = new ShapeView(poly);
            closeBox.setBorder(TAB_CLOSE_BORDER1);
            closeBox.setPrefSize(11, 11);
            closeBox.addEventFilter(e -> handleTabCloseBoxEvent(e), MouseEnter, MouseExit, MouseRelease);

            // Add to FileTab
            setGraphicAfter(closeBox);
        }

        /**
         * Called for events on tab close button.
         */
        private void handleTabCloseBoxEvent(ViewEvent anEvent)
        {
            View closeBox = anEvent.getView();

            // Handle MouseEnter
            if (anEvent.isMouseEnter()) {
                closeBox.setFill(Color.CRIMSON);
                closeBox.setBorder(TAB_CLOSE_BORDER2);
            }

            // Handle MouseExit
            else if (anEvent.isMouseExit()) {
                closeBox.setFill(null);
                closeBox.setBorder(TAB_CLOSE_BORDER1);
            }

            // Handle MouseRemove
            else if (anEvent.isMouseRelease()) {
                if (anEvent.isAltDown())
                    _pagePane.removeAllOpenFilesExcept(_file);
                else _pagePane.removeOpenFile(_file);
            }

            anEvent.consume();
        }
    }
}
