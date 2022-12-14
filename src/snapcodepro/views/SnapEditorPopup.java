package snapcodepro.views;

import javakit.parse.JExprId;
import javakit.parse.JNode;
import javakit.parse.JavaParser;
import javakit.resolver.JavaDecl;
import javakit.ide.NodeCompleter;
import snap.gfx.Font;
import snap.parse.Parser;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * This class provides UI for editing an expression.
 */
public class SnapEditorPopup extends ViewOwner {

    // The expression part
    JNodeView _spart;

    // The popup
    PopupWindow _popup = getPopup();

    // The ListView
    ListView<JavaDecl> _listView;

    // The fixed expression
    String _startText, _endText, _idText;

    // Expression parser
    Parser _exprParser = JavaParser.getShared().getExprParser();

    /**
     * Returns a shared instance.
     */
    public static SnapEditorPopup getShared()
    {
        return _s != null ? _s : (_s = new SnapEditorPopup());
    }

    static SnapEditorPopup _s;

    /**
     * Activates the popup list (shows popup if multiple suggestions, does replace for one, does nothing for none).
     */
    public void activatePopupList(JNodeView aPart, String aString, int anIndex)
    {
        // Set current SnapPart
        _spart = aPart;

        // Create expression from string
        JNode node = null;
        try {
            node = _exprParser.parseCustom(aString, JNode.class);
        } catch (Exception e) {
        }
        if (node == null) {
            hide();
            return;
        }
        node.setParent(aPart.getJNode().getParent());

        // Get id at editing (just return if not id)
        JNode id = node.getNodeAtCharIndex(anIndex);
        if (!(id instanceof JExprId)) {
            hide();
            return;
        }

        // If same Id, just return
        if (SnapUtils.equals(id.getName(), _idText)) return;
        _idText = id.getName();

        // Get suggestions
        JavaDecl suggestions[] = new NodeCompleter().getCompletionsForNode(id);
        if (suggestions.length == 0) {
            hide();
            return;
        }

        // If multiple suggestions
        _listView.setItems(suggestions);
        _listView.setSelIndex(0);
        showPopup(aPart);

        // Get fixed text
        _startText = aString.substring(0, id.getStartCharIndex());
        _endText = aString.substring(anIndex);
    }

    /**
     * Show Dialog.
     */
    public void showPopup(JNodeView aView)
    {
        if (getPopup().isShowing()) return;
        getPopup().show(aView, 0, aView.getHeight());
    }

    /**
     * Hides the popup.
     */
    public void hide()
    {
        getPopup().hide();
        _idText = null;
    }

    /**
     * Returns whether popup is visible.
     */
    public boolean isShowing()
    {
        return getPopup().isShowing();
    }

    /**
     * Returns the popup.
     */
    public PopupWindow getPopup()
    {
        return _popup != null ? _popup : (_popup = createPopup());
    }

    /**
     * Creates the popup.
     */
    protected PopupWindow createPopup()
    {
        PopupWindow popup = new PopupWindow();
        popup.setContent(getUI());
        return popup;
    }

    /**
     * Creates the UI.
     */
    protected View createUI()
    {
        _listView = new ListView();
        _listView.setFont(Font.Arial11);
        _listView.setPrefSize(320, 260);
        enableEvents(_listView, KeyEvents);
        return _listView;
    }

    /**
     * Respond to UI events.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle ListView KeyEvents to consume Up/Down arrow, Escape and EnterKey
        if (anEvent.isKeyEvent()) {
            if (anEvent.isEscapeKey()) {
                if (anEvent.isKeyRelease()) getPopup().hide();
                anEvent.consume();
            }
            if (anEvent.isEnterKey()) {
                if (anEvent.isKeyRelease()) ((JExprEditor) _spart).fireTextFieldAction();
                anEvent.consume();
            }
        }
    }

    /**
     * Returns the fixed text.
     */
    public String getFixedText()
    {
        JavaDecl item = _listView.getSelItem();
        return _startText + item.getReplaceString() + _endText;
    }
}