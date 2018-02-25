package code.name.monkey.retromusic.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import code.name.monkey.retromusic.R;

/**
 * @author Aidan Follestad (afollestad), modified by Karim Abou Zeid
 */
public class BlacklistFolderChooserDialog extends DialogFragment implements MaterialDialog.ListCallback {

    private File parentFolder;
    private File[] parentContents;

    private FolderCallback callback;

    File initialPath = Environment.getExternalStorageDirectory().getAbsoluteFile();

    private String[] getContentsArray() {
        if (parentContents == null) {
            return new String[]{};
        }
        String[] results = new String[parentContents.length];
        for (int i = 0; i < parentContents.length; i++) {
            results[i] = parentContents[i].getName();
        }
        return results;
    }

    private File[] listFiles() {
        /*
         * Memorize the initial path and inject the path folders
         * again, even if android is not listing them in .listFiles()
         */
        File memorizedFolder = null;
        if(initialPath.getAbsolutePath().startsWith(parentFolder.getAbsolutePath())){
            String path = parentFolder.toURI().relativize(initialPath.toURI()).getPath();
            String[] split = path.split(File.separator);
            if(split.length > 0 && split[0].length() > 0) {
                memorizedFolder = new File(parentFolder, split[0]);
            }
        }


        File[] contents = parentFolder.listFiles();
        List<File> results = new ArrayList<>();
        if (contents != null) {
            for (File fi : contents) {
                if(memorizedFolder != null && memorizedFolder.equals(fi)){
                    memorizedFolder = null;
                }
                if (fi.isDirectory()) {
                    results.add(fi);
                }
            }
        }
        if(memorizedFolder != null) {
            results.add(memorizedFolder);
        }
        Collections.sort(results, new FolderSorter());
        return results.toArray(new File[0]);
    }

    public static BlacklistFolderChooserDialog create() {
        return new BlacklistFolderChooserDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ActivityCompat.checkSelfPermission(
                getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return new MaterialDialog.Builder(getActivity())
                    .title(R.string.md_error_label)
                    .content(R.string.md_storage_perm_error)
                    .positiveText(android.R.string.ok)
                    .build();
        }
        if (savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        if (!savedInstanceState.containsKey("current_path")) {
            savedInstanceState.putString("current_path", initialPath.toString());
        }
        parentFolder = new File(savedInstanceState.getString("current_path", initialPath.getAbsolutePath()));
        parentContents = listFiles();
        MaterialDialog.Builder builder =
                new MaterialDialog.Builder(getActivity())
                        .title(parentFolder.getAbsolutePath())
                        .items((CharSequence[]) getContentsArray())
                        .itemsCallback(this)
                        .autoDismiss(false)
                        .onPositive((dialog, which) -> {
                            dismiss();
                            callback.onFolderSelection(BlacklistFolderChooserDialog.this, parentFolder);
                        })
                        .onNeutral((materialDialog, dialogAction) -> {
                            onUpwards();
                        })
                        .onNegative((materialDialog, dialogAction) -> dismiss())
                        .positiveText(R.string.add_action)
                        .neutralText("Up")
                        .negativeText(android.R.string.cancel);
        return builder.build();
    }

    public void onUpwards() {
        if(parentFolder.getParentFile() != null) {
            parentFolder = parentFolder.getParentFile();
        }
        reload();
    }

    @Override
    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
        parentFolder = parentContents[i];
        reload();
    }

    private void reload() {
        parentContents = listFiles();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        dialog.setTitle(parentFolder.getAbsolutePath());
        dialog.setItems((CharSequence[]) getContentsArray());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_path", parentFolder.getAbsolutePath());
    }

    public void setCallback(FolderCallback callback) {
        this.callback = callback;
    }

    public interface FolderCallback {
        void onFolderSelection(@NonNull BlacklistFolderChooserDialog dialog, @NonNull File folder);
    }

    private static class FolderSorter implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}
