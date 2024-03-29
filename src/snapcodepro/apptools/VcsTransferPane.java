package snapcodepro.apptools;
import snap.geom.Pos;
import snap.gfx.Image;
import snapcodepro.project.VersionControl.Op;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs panel and transfers files for a Project WebSite.
 */
public class VcsTransferPane extends ViewOwner {

    // The VersionControlPane
    private VcsPane _vcp;

    // The list of transfer files
    private List<WebFile>  _files = new ArrayList<>();

    // The VersionControl operation
    private Op  _op;

    // The commit message (if commit)
    private String  _commitMsg;

    // Images
    static Image AddedLocalBadge = Image.get(VcsTransferPane.class, "AddedLocalBadge.png");
    static Image RemovedLocalBadge = Image.get(VcsTransferPane.class, "RemovedLocalBadge.png");
    static Image UpdatedLocalBadge = Image.get(VcsTransferPane.class, "UpdatedLocalBadge.png");
    static Image AddedRemoteBadge = Image.get(VcsTransferPane.class, "AddedRemoteBadge.png");
    static Image RemovedRemoteBadge = Image.get(VcsTransferPane.class, "RemovedRemoteBadge.png");
    static Image UpdatedRemoteBadge = Image.get(VcsTransferPane.class, "UpdatedRemoteBadge.png");

    /**
     * Show panel.
     */
    public boolean showPanel(VcsPane aVC, List<WebFile> theFiles, Op anOp)
    {
        // Set component and data source
        _vcp = aVC;

        // Set transfer files and transfer op
        _files = theFiles;
        _op = anOp;

        // If no transfer files, just tell user and return
        if (_files.size() == 0) {
            String msg = "No " + getOp() + " files to transfer.", title = "Synchronize Files";
            DialogBox dbox = new DialogBox(title);
            dbox.setWarningMessage(msg);
            dbox.showMessageDialog(aVC._appPane.getUI());
            return false;
        }

        // Show confirmation dialog with files to transfer
        String mode = getOp().toString();
        String[] options = new String[]{mode, "Cancel"};
        DialogBox dbox = new DialogBox(mode + " Files Panel");
        dbox.setContent(getUI());
        dbox.setOptions(options);
        if (dbox.showOptionDialog(aVC._appPane.getUI(), mode) != 0) return false;

        // If commit, get message
        if (anOp == Op.Commit) {
            _commitMsg = getViewText("CommentText");
            if (_commitMsg != null) _commitMsg = _commitMsg.trim();
            if (_commitMsg == null || _commitMsg.length() == 0) {
                DialogBox db = new DialogBox("Commit Files Message Panel");
                db.setMessage("Enter Commit Message");
                db.showMessageDialog(aVC._appPane.getUI());
                return showPanel(aVC, theFiles, anOp);
            }
        }

        // Return true
        return true;
    }

    /**
     * Returns the transfer files.
     */
    public List<WebFile> getFiles()  { return _files; }

    /**
     * Returns the TransferMode.
     */
    public Op getOp()  { return _op; }

    /**
     * Returns the commit message.
     */
    public String getCommitMessage()  { return _commitMsg; }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Set FilesList CellConfigure
        ListView<WebFile> filesList = getView("FilesList", ListView.class);
        filesList.setRowHeight(22);
        filesList.setCellConfigure(this::configureFilesListCell);
        if (getOp() != Op.Commit) {
            TextView commentText = getView("CommentText", TextView.class);
            getView("SplitView", SplitView.class).removeItem(commentText);
        }
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update FilesList
        List<WebFile> files = getFiles();
        setViewItems("FilesList", files);
    }

    /**
     * Called to configure a FilesList cell.
     */
    private void configureFilesListCell(ListCell<WebFile> aCell)
    {
        WebFile file = aCell.getItem();
        if (file == null) return;
        aCell.setText(file.getPath());
        aCell.setGraphic(getFileGraphic(file));
    }

    /**
     * Returns the icon to use list item.
     */
    protected View getFileGraphic(WebFile aFile)
    {
        WebFile remoteFile = _vcp.getVC().getRepoFile(aFile.getPath(), false, false);
        Image badge = null;

        // Handle missing LocalFile, missing RemoteFile or Update
        boolean isCommit = getOp() == Op.Commit;
        if (!aFile.getExists())
            badge = isCommit ? RemovedLocalBadge : AddedRemoteBadge;
        else if (remoteFile == null)
            badge = isCommit ? AddedLocalBadge : RemovedRemoteBadge;
        else badge = isCommit ? UpdatedLocalBadge : UpdatedRemoteBadge;

        // Composite file and return
        Image image = ViewUtils.getFileIconImage(aFile);
        ImageView imageView = new ImageView(image), bview = new ImageView(badge);
        StackView stackView = new StackView();
        stackView.setAlign(Pos.TOP_LEFT);
        stackView.setChildren(imageView, bview);
        stackView.setPrefSize(16 + 6, 16);
        bview.setLean(Pos.CENTER_RIGHT);
        return stackView;
    }
}