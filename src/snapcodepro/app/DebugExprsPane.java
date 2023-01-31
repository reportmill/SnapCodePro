package snapcodepro.app;
import snapcodepro.debug.DebugApp;
import snapcodepro.debug.ExprEval;
import snap.geom.HPos;
import snap.gfx.Font;
import snap.gfx.Image;
import snap.view.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A debug pane.
 */
public class DebugExprsPane extends ProjectTool {

    // The ProcPane
    private ProcPane  _procPane;

    // The variable table
    private TreeView<ExprTreeItem>  _varTree;

    // The variable text
    private TextView  _varText;

    // The list of expression tree items
    List<ExprTreeItem>  _exprItems = new ArrayList<>();

    /**
     * Creates a new DebugExprsPane.
     */
    public DebugExprsPane(AppPane projPane)
    {
        super(projPane);
        _procPane = projPane.getProcPane();
    }

    /**
     * Returns the debug app.
     */
    public DebugApp getDebugApp()
    {
        return _procPane.getSelDebugApp();
    }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        // Create VarTree and configure
        _varTree = new TreeView();
        _varTree.setGrowHeight(true);
        _varTree.setFont(Font.Arial11);
        TreeCol c0 = _varTree.getCol(0);
        c0.setHeaderText("Name");
        c0.setPrefWidth(450);
        TreeCol c1 = new TreeCol();
        c1.setHeaderText("Value");
        c1.setPrefWidth(150);
        c1.setGrowWidth(true);
        _varTree.addCol(c1);
        _varTree.setResolver(new DebugVarsPane.VarTreeResolver()); //_varTree.setEditable(true);

        // Set default item
        _exprItems.add(new ExprTreeItem("this"));

        // Set Cell Factory
    /*TreeTableColumn col0 = (TreeTableColumn)_varTable.getColumns().get(0);
    col0.setEditable(true); col0.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
    col0.setOnEditCommit(new EventHandler<CellEditEvent>() {
        public void handle(CellEditEvent e) {
            if(!(e.getRowValue() instanceof ExprTableItem)) return;
            ExprTableItem titem = (ExprTableItem)e.getRowValue();
            titem._expr = titem._name = (String)e.getNewValue(); resetVarTable(); } });*/

        // Create VarText TextView and configure in ScrollView
        _varText = new TextView();
        _varText.setWrapLines(true);
        _varText.setPrefHeight(40);

        // Create SplitView with VarTree and VarText
        SplitView split = new SplitView();
        split.setItems(_varTree, _varText);
        split.setVertical(true);
        split.setGrowHeight(true);

        // Create Buttons
        Label label = new Label("Expr: ");
        label.setPadding(0, 0, 0, 5);
        TextField tfield = new TextField();
        tfield.setName("ExprText");
        tfield.setPrefWidth(300);
        tfield.setFont(Font.Arial11);
        Image addImage = getImage("ExprAdd.png"), remImage = getImage("ExprRemove.png");
        Button addBtn = new Button();
        addBtn.setImage(addImage);
        addBtn.setName("AddButton");
        Button remBtn = new Button();
        remBtn.setImage(remImage);
        remBtn.setName("RemoveButton");
        addBtn.setPrefSize(32, 24);
        remBtn.setPrefSize(32, 24);
        addBtn.setLeanX(HPos.RIGHT);
        RowView hbox = new RowView();
        hbox.setChildren(label, tfield, addBtn, remBtn);

        // Add to VBox with padding
        ColView vbox = new ColView();
        vbox.setFillWidth(true);
        vbox.setChildren(hbox, split);
        vbox.setPadding(2, 2, 2, 2);
        return vbox;
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        // Set items
        _varTree.setItems(_exprItems);
        if (_varTree.getSelItem() == null && _exprItems.size() > 0) _varTree.setSelIndex(0);

        // Iterate over ExprTableItems and reset values
        if (_resetVarTable) {
            _resetVarTable = false;
            for (ExprTreeItem item : _exprItems) item.eval();
            _varTree.updateItems();
        }

        // Update ExprText
        DebugVarsPane.VarTreeItem vitem = _varTree.getSelItem();
        setViewText("ExprText", vitem instanceof ExprTreeItem ? vitem.getName() : null);
        setViewEnabled("ExprText", vitem instanceof ExprTreeItem);

        // Update VarText
        if (vitem != null && getDebugApp() != null && getDebugApp().isPaused()) {
            String pvalue = vitem.getValueToString();
            _varText.setText(pvalue != null ? pvalue : "(null)");
        } else _varText.setText("");
    }

    /**
     * Tells table to update model from DebugApp.
     */
    void resetVarTable()
    {
        if (isUISet()) {
            _resetVarTable = true;
            resetLater();
        }
    }

    boolean _resetVarTable = true;

    /**
     * Respond UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle ExprText
        if (anEvent.equals("ExprText")) {
            DebugVarsPane.VarTreeItem item = _varTree.getSelItem();
            ExprTreeItem exitem = item instanceof ExprTreeItem ? (ExprTreeItem) item : null;
            if (exitem == null) return;
            ExprTreeItem nitem = new ExprTreeItem(anEvent.getStringValue());
            nitem.eval();
            _exprItems.set(_exprItems.indexOf(exitem), nitem);
            _varTree.setItems(_exprItems);
            _varTree.setSelItem(nitem);
            resetVarTable();
        }

        // Handle AddButton
        if (anEvent.equals("AddButton")) {
            ExprTreeItem nitem = new ExprTreeItem(anEvent.getStringValue());
            nitem.eval();
            _exprItems.add(nitem);
            _varTree.setItems(_exprItems);
            _varTree.setSelItem(nitem);
            //runLater(() -> _varTree.edit(_exprItems.size()-1, _varTree.getCol(0)));
        }

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton")) {
            int index = _varTree.getSelIndex();
            if (index >= 0 && index < _exprItems.size()) _exprItems.remove(index);
        }

        // Everything makes text focus
        requestFocus("ExprText");
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Expressions"; }

    /**
     * A class to hold a ExprTableItem Variable.
     */
    public class ExprTreeItem extends DebugVarsPane.VarTreeItem {

        // Ivars
        String _expr;

        /**
         * Create ExprTableItem.
         */
        public ExprTreeItem(String aName)
        {
            super(null, aName, null);
            _expr = aName;
        }

        /**
         * Override to use current app.
         */
        public DebugApp getApp()
        {
            return getDebugApp();
        }

        /**
         * Makes TreeItem re-eval expression.
         */
        void eval()
        {
            DebugApp dapp = getDebugApp();
            _children = null; //if(isChildrenSet()) { resetChildren(); setExpanded(false); }
            try {
                _value = dapp != null ? ExprEval.eval(dapp, _expr) : null;
            } catch (Exception e) {
                _value = e;
            }
        }
    }

}