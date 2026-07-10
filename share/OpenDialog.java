package com.vangemert.videoAssist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bitmapcache.BitmapLruCache;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenDialog {
    private static final String TAG = "OpenDialog";
    private static final boolean VERBOSE = false;

    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    private final String path;
    private final AlertDialog dialog;
    private final Context context;
    private final String[] filter;
    private List<MediaListAdapter.Item> items = null;
    private MediaListAdapter listAdapter = null;

    public OpenDialog(Context context, BitmapLruCache cache, String path, String[] filter, SelectedCallback selectedCallback) throws IOException {
        this.context = context;
        this.path = path;
        this.filter = filter;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.LargerDialog);
        dialog = dialogBuilder.create();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        dialog.setCanceledOnTouchOutside(true);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_open, null);
        dialog.setView(layout);

        ImageButton backButton = layout.findViewById(R.id.back_button);
        TextView titleView = layout.findViewById(R.id.text1);
        ListView listView = layout.findViewById(R.id.files_list);
        ImageButton menuButton = layout.findViewById(R.id.menu_button);

        backButton.setOnClickListener(v -> {
            listView.clearChoices(); // Purge choice index states safely before closing
            dialog.cancel();
        });

        menuButton.setOnClickListener(v -> {
            int qty = listView.getCheckedItemCount();
            if (qty == 0) {
                UiThread.snackbar(listView, "Select file(s) with long click first");
                return;
            }

            PopupMenu menu = new PopupMenu(context, v);
            if (qty == 1) menu.getMenu().add(Menu.NONE, 0, 1, "Rename...");
            menu.getMenu().add(Menu.NONE, 1, 1, "Delete...");
            menu.getMenu().add(Menu.NONE, 2, 1, "Copy to documents...");

            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 0: // Rename
                        String oldFn = getNameOfFirstItemChecked(listView);
                        Utils.inputDialog(context, "Rename", oldFn, newFn -> {
                            if (Utils.extensionsMatch(oldFn, newFn)) {
                                UiThread.snackbar(listView, "Renaming file");
                                if (Utils.rename(Utils.getAbsolutePath(context, oldFn), Utils.getAbsolutePath(context, newFn))) {
                                    updateFiles(listView);
                                    titleView.setText("Open");
                                }
                            }
                        });
                        break;

                    case 1:  // Delete
                        new AlertBox(context, "Delete", "Are you sure you want to delete <b> " + qty + " files</b>?", ok -> {
                            if (ok) {
                                UiThread.snackbar(listView, "Deleting file(s)");
                                for (int i = 0; i < listView.getCount(); ++i) {
                                    if (listView.isItemChecked(i)) {
                                        Utils.deleteFile(Utils.getAbsolutePath(context, items.get(i).name));
                                    }
                                }
                                updateFiles(listView);
                                titleView.setText("Open");
                            }
                        });
                        break;

                    case 2:  // Copy to documents
                        final List<MediaListAdapter.Item> targetsToCopy = new ArrayList<>();
                        for (int i = 0; i < listView.getCount(); ++i) {
                            if (listView.isItemChecked(i)) {
                                targetsToCopy.add(items.get(i));
                            }
                        }

                        executePublicDocumentsExport(targetsToCopy, titleView, listView);
                        break;
                }
                return true;
            });
            menu.show();
        });

        items = getFiles();
        listAdapter = new MediaListAdapter(context, cache, items);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sel = ((MediaListAdapter.Item) listAdapter.getItem(position)).name;
                String _sel = Utils.getFirstWord(sel);

                for (int i = 0; i < 20; ++i) {
                    try {
                        JSONObject object = new Preferences(context).getUri(i);
                        if (object != null) {
                            String code = object.getString("code");
                            if (code.equals(_sel)) {
                                selectedCallback.onSelect(_sel);
                                dialog.cancel();
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                selectedCallback.onSelect(path + "/" + sel);
                dialog.cancel();
            }
        });

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            if (listAdapter.isFile(i)) {
                listView.setItemChecked(i, !listView.isItemChecked(i));
                titleView.setText(listView.getCheckedItemCount() + " items selected");
            }
            return true;
        });

        dialog.show();
    }

    private void executePublicDocumentsExport(List<MediaListAdapter.Item> targets, TextView titleView, ListView listView) {
        final Snackbar snackbar = Snackbar.make(titleView, "", Snackbar.LENGTH_INDEFINITE);

        exportExecutor.execute(() -> {
            for (MediaListAdapter.Item item : targets) {
                final String internalFileAbsPath = Utils.getAbsolutePath(context, item.name);

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, Utils.extractFilename(internalFileAbsPath));

                String targetSubfolder;
                if (Utils.isVideoFile(internalFileAbsPath)) {
                    targetSubfolder = Environment.DIRECTORY_DOCUMENTS + "/videoAssist/video";
                } else if (Utils.isGraphicFile(internalFileAbsPath)) {
                    targetSubfolder = Environment.DIRECTORY_DOCUMENTS + "/videoAssist/graphic";
                } else {
                    continue;
                }

                values.put(MediaStore.MediaColumns.RELATIVE_PATH, targetSubfolder);

                Uri destinationUri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (destinationUri == null) continue;

                final String currentProgressLabel = "Copying " + item.name + " to public Documents folder...";
                ((Activity) context).runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        snackbar.setText(currentProgressLabel);
                        if (!snackbar.isShown())
                            snackbar.show();
                    }
                });

                try (FileInputStream is = new FileInputStream(internalFileAbsPath);
                     OutputStream os = context.getContentResolver().openOutputStream(destinationUri)) {
                    if (os != null) {
                        byte[] buffer = new byte[4096]; // FIX: Correctly initialized 4KB data chunk size
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                    Thread.sleep(800); // Controlled user feedback rest ticker
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ((Activity) context).runOnUiThread(() -> {
                snackbar.dismiss();
                if (dialog.isShowing()) {
                    updateFiles(listView);
                    titleView.setText("Open");
                }
            });
        });
    }

    private String getNameOfFirstItemChecked(ListView listView) {
        for (int i = 0; i < listView.getCount(); ++i) {
            if (listView.isItemChecked(i)) {
                return items.get(i).name;
            }
        }
        return null;
    }

    private List<MediaListAdapter.Item> getFiles() {
        List<MediaListAdapter.Item> files = new ArrayList<MediaListAdapter.Item>();

        try {
            File dirFile = new File(path);
            if (!dirFile.exists() || !dirFile.isDirectory())
                return files;

            for (File file : Objects.requireNonNull(dirFile.listFiles())) {
                if (!file.isDirectory()) {
                    long lastModified = file.lastModified();

                    if (filter == null)
                        files.add(new MediaListAdapter.Item(file.getName(), true, lastModified, ""));
                    else {
                        String fn = file.getAbsolutePath();
                        for (String s : filter) {
                            if (fn.contains(s)) {
                                files.add(new MediaListAdapter.Item(file.getName(), true, lastModified, ""));
                                break;
                            }
                        }
                    }
                }
            }

            // video filter triggers the display of juice url's
            if (filter != null && Utils.isVideo(filter)) {
                String self = "juice://" + Utils.id(context);

                JSONObject object;
                for (int i = 0; i < 20; ++i) {
                    object = new Preferences(context).getUri(i);
                    if (object != null) {
                        String uri = object.getString("uri");
                        String code = object.getString("code");

                        if (self.equals(uri))
                            files.add(new MediaListAdapter.Item(code + " (self)", false, 0, uri));
                        else
                            files.add(new MediaListAdapter.Item(code, false, 0, uri));
                    }
                }
            }

            files.sort(new Comparator<MediaListAdapter.Item>() {
                public int compare(MediaListAdapter.Item o1, MediaListAdapter.Item o2) {
                    return Long.compare(o2.lastModified, o1.lastModified);
                }
            });
        } catch (Exception ignored) {
        }

        return files;
    }

    private void updateFiles(ListView listView) {
        listView.clearChoices();
        //items.clear();
        //items.addAll(getFiles());

        items = getFiles();

        //listAdapter.notifyDataSetChanged();

        listAdapter.updateItems(items);
    }

    public interface SelectedCallback {
        void onSelect(String filename);
    }
}

