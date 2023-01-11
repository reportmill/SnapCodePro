package snapcodepro.app;
import snap.view.ViewOwner;
import snapcodepro.debug.RunApp;
import snap.gfx.Color;
import snap.gfx.Font;
import snapcodepro.project.ProjectX;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snap.view.ViewEvent;
import snap.viewx.ConsoleView;
import snap.web.WebFile;

/**
 * A panel to run a process.
 */
public class RunConsole extends ViewOwner {

    // The AppPane
    AppPane _appPane;

    // The output text
    RCTextView  _textView;

    // The error color
    static Color ERROR_COLOR = new Color("CC0000");

    /**
     * Creates a new DebugPane.
     */
    public RunConsole(AppPane anAppPane)
    {
        _appPane = anAppPane;
    }

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()
    {
        return _appPane;
    }

    /**
     * Returns the ProcPane.
     */
    public ProcPane getProcPane()
    {
        return _appPane.getProcPane();
    }

    /**
     * Clears the RunConsole text.
     */
    public void clear()
    {
        if (_textView != null) _textView.clear();
    }

    /**
     * Appends to out.
     */
    public void appendOut(String aStr)
    {
        if (!isEventThread()) {
            runLater(() -> appendOut(aStr));
            return;
        }  // Make sure we're in app event thread
        appendString(aStr, Color.BLACK);                               // Append string in black
    }

    /**
     * Appends to err.
     */
    public void appendErr(String aStr)
    {
        if (!isEventThread()) {
            runLater(() -> appendErr(aStr));
            return;
        }  // Make sure we're in app event thread
        appendString(aStr, ERROR_COLOR);                                   // Append string in red
    }

    /**
     * Appends text with given color.
     */
    void appendString(String aStr, Color aColor)
    {
        // Get default style modified for color
        TextStyle style = _textView.getStyleForCharIndex(_textView.length());
        if (_textView.length() > 100000) return;
        style = style.copyFor(aColor);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {

            // Get start/end of Java file name inside parens (if parens not found, just add chars and continue)
            int s = aStr.lastIndexOf("(", i), e = aStr.indexOf(")", i);
            if (s < start || e < 0) {
                _textView.addChars(aStr.substring(start, start = i + 6), style);
                continue;
            }

            // Get chars before parens and add
            String prefix = aStr.substring(start, s + 1);
            _textView.addChars(prefix, style);

            // Get link text, link address, TextLink
            String linkText = aStr.substring(s + 1, e);
            String linkAddr = getLink(prefix, linkText);
            TextLink textLink = new TextLink(linkAddr);

            // Get TextStyle for link and add link chars
            TextStyle lstyle = style.copyFor(textLink);
            _textView.addChars(linkText, lstyle);

            // Update start to end of link text and continue
            start = e;
        }

        // Add remainder normally
        _textView.addChars(aStr.substring(start), style);
    }

    /**
     * Returns a link for a StackString.
     */
    String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0) return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start) end = aPrefix.lastIndexOf('.');
        if (end < start) end = aPrefix.length() - 1;

        // Create link from path and return
        String path = aPrefix.substring(start, end);
        path = '/' + path.replace('.', '/') + ".java";
        path = getSourceURL(path);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);
        int line = SnapUtils.intValue(lineStr);
        if (line > 0) path += "#LineNumber=" + line;
        return path;
    }

    /**
     * Returns a source URL for path.
     */
    String getSourceURL(String aPath)
    {
        if (aPath.startsWith("/java/") || aPath.startsWith("/javax/"))
            return "http://reportmill.com/jars/8u05/src.zip!" + aPath;
        if (aPath.startsWith("/javafx/"))
            return "http://reportmill.com/jars/8u05/javafx-src.zip!" + aPath;
        ProjectX proj = ProjectX.getProjectForSite(_appPane.getRootSite());
        if (proj == null) return aPath;
        WebFile file = proj.getProjectSet().getSourceFile(aPath);
        return file != null ? file.getURL().getString() : aPath;
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get font
        String[] names = {"Monoco", "Consolas", "Courier"};
        Font defaultFont = null;
        for (int i = 0; i < names.length; i++) {
            defaultFont = new Font(names[i], 12);
            if (defaultFont.getFamily().startsWith(names[i]))
                break;
        }

        // Get output text
        _textView = getView("OutputText", RCTextView.class);
        _textView._runConsole = this;
        _textView.setFont(defaultFont);
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        RunApp selApp = getProcPane().getSelApp();
        String labelText = selApp.getName() + " Console";
        setViewText("NameLabel", labelText);
        setViewEnabled("TerminateButton", !selApp.isTerminated());
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle ClearButton
        if (anEvent.equals("ClearButton")) {
            clear();
            getProcPane().getSelApp().clearOutput();
        }

        // Handle TerminateButton
        if (anEvent.equals("TerminateButton"))
            getProcPane().getSelApp().terminate();
    }

    /**
     * A TextView subclass to open links.
     */
    public static class RCTextView extends ConsoleView {

        RunConsole  _runConsole;

        /**
         * Override to open in browser.
         */
        protected void openLink(String aLink)
        {
            _runConsole._appPane.getBrowser().setURLString(aLink);
        }

        /**
         * Override to send to process.
         */
        protected void processEnterAction()
        {
            RunApp proc = _runConsole.getProcPane().getSelApp();
            if (proc == null) return;
            String str = getInput();
            proc.sendInput(str);
        }
    }

}