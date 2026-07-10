package com.vangemert.videoAssist;

import static com.vangemert.videoAssist.Content.FILL_CAM;
import static com.vangemert.videoAssist.Content.FILL_MP1;
import static com.vangemert.videoAssist.Content.FILL_MP2;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bitmapcache.BitmapLruCache;
import com.google.common.io.Files;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandDialog extends DialogFragment {
    public interface Callback {
        void callback(String markup);
    }

    private Callback callback = null;
    private EditText textView;
    private EditText fromView;
    private EditText atView;
    private TextView filenameTitle;

    private LinearLayout filenameLayout,
            audioLayout,
            videoLayout,
            mediaPlayerLayout,
            cameraLayout,
            cgLayout,
            textLayout,
            pageLayout,
            recordLayout,
            atLayout;

    private final String title;
    private final BitmapLruCache cache;
    private final int device;

    private Preferences preferences;

    private String mediaPath;
    private String[] mediaExtent;

    private Spinner cameraOptions;
    private SeekBar cameraZoom;
    private CheckBox cameraAwbl;
    private CheckBox cameraAel;

    private TextView filename;
    private CheckBox loop;
    private Spinner videoOptions;
    private Spinner audioOptions;
    private Spinner recordOptions;
    private SeekBar audioLevel;

    public CommandDialog(String title, BitmapLruCache cache, int device) {
        this.title = title;
        this.cache = cache;
        this.device = device;
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        preferences = new Preferences(getContext());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.dialog_command, null);

        initCamera(rootView);
        initMediaPlayer(rootView);
        initCg(rootView);
        initFilename(rootView);
        initVideo(rootView);
        initAudio(rootView);
        initText(rootView);
        initRecord(rootView);
        initAt(rootView);
        setVisibility(device);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(rootView);

        builder.setTitle(title);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (callback != null) {
                    String fn = Files.getNameWithoutExtension(filename.getText().toString());

                    StringBuilder sb = new StringBuilder();
                    switch (device) {
                        case Msg.CAM:
                            sb.append("*cam");
                            switch (cameraOptions.getSelectedItemPosition()) {
                                case 0:
                                    // current
                                    break;

                                case 1:
                                    sb.append(" front");
                                    break;

                                case 2:
                                    sb.append(" back");
                                    break;
                            }

                            if(cameraZoom.getProgress() != 0)
                                sb.append(" zoom ").append(cameraZoom.getProgress());

                            if(cameraAwbl.isChecked())
                                sb.append(" awbl");

                            if(cameraAel.isChecked())
                                sb.append(" ael");

                            processAudio(sb);
                            processVideo(sb);
                            processAt(sb);
                            break;

                        case Msg.MP1:
                            sb.append("*mp1");

                            if(!fn.isEmpty())
                                sb.append(" ").append(fn);

                            if(!String.valueOf(fromView.getText()).isEmpty()) {
                                sb.append(" from ");
                                sb.append(fromView.getText());
                            }

                            if (!fn.isEmpty() && loop.isChecked())
                                sb.append(" loop");

                            processAudio(sb);
                            processVideo(sb);
                            processAt(sb);
                            break;

                        case Msg.MP2:
                            sb.append("*mp2");
                            if(!fn.isEmpty())
                                sb.append(" ").append(fn);

                            if (!fn.isEmpty() && loop.isChecked())
                                sb.append(" loop");

                            processAudio(sb);
                            processVideo(sb);
                            processAt(sb);
                            break;

                        case Msg.CG:
                            sb.append("*cg");
                            sb.append(" ").append(fn);

                            if (filename.getText().toString().contains(".pdf")) {
                                sb.append(" page");
                                EditText number = rootView.findViewById(R.id.number);
                                sb.append(" ").append(number.getText());
                            }

                            processVideo(sb);

                            if (filename.getText().toString().contains(".xml")) {
                                sb.append("\n");
                                sb.append(textView.getText());
                                sb.append("\n");
                            }
                            break;

                        case Msg.TTS:
                            sb.append("*tts");

                            processAudio(sb);
                            processAt(sb);

                            sb.append("\n");
                            sb.append(textView.getText());
                            sb.append("\n");
                            break;

                        case Msg.RECORD:
                            sb.append("*rec");

                            processRecord(sb);
                            processAt(sb);

                            break;
                    }

                    sb.append(System.lineSeparator());

                    callback.callback(sb.toString());
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        Dialog dialog = builder.create();
        // Clear the dim flag
        Objects.requireNonNull(dialog.getWindow()).clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return dialog;
    }

    // processed first
    private void processAudio(StringBuilder sb) {
        sb.append(" audio");
        switch (audioOptions.getSelectedItemPosition()) {
            case 0:
                // follow
                sb.append(" follow");
                sb.append(" level ");
                sb.append(audioLevel.getProgress());
                break;

            case 1:
                // over
                sb.append(" over");
                sb.append(" level ");
                sb.append(audioLevel.getProgress());
                break;

            case 2:
                // none
                sb.append(" none");
                break;
        }
    }

    // processed second
    private void processVideo(StringBuilder sb) {
        sb.append(" video");
        switch (videoOptions.getSelectedItemPosition()) {
            case 0:
                // cut
                sb.append(" cut");
                break;

            case 1:
                // short mix
                sb.append(" mix 6");
                break;

            case 2:
                // long mix
                sb.append(" mix 12");
                break;

            case 3:
                if (device == Msg.CG) {
                    // key on
                    sb.append(" key on");
                } else {
                    // none
                    sb.append(" none");
                }
                break;

            case 4:
                // key off
                if (device == Msg.CG) {
                    sb.append(" key off");
                }
                break;

            case 5:
                if (device == Msg.CG) {
                    // none
                    sb.append(" none");
                    break;
                }
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void initText(View rootView) {
        cgLayout = rootView.findViewById(R.id.cgLayout);
        textView = rootView.findViewById(R.id.text);
    }
    private void initRecord(View rootView) {
        recordLayout = rootView.findViewById(R.id.rec_layout);
        recordOptions = rootView.findViewById(R.id.rec_options);

        List<String> selections = new ArrayList<String>();
        selections.add("off");
        selections.add("on");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, selections);
        recordOptions.setAdapter(adapter);
    }

    private void processRecord(StringBuilder sb) {
        switch (recordOptions.getSelectedItemPosition()) {
            case 0:
                sb.append(" off");
                break;

            case 1:
                sb.append(" on");
                break;
        }
    }

    private void initAt(View rootView) {
        atLayout = rootView.findViewById(R.id.at_layout);
        atView = rootView.findViewById(R.id.at);
    }

    private void processAt(StringBuilder sb) {
        if(!String.valueOf(atView.getText()).isEmpty()) {
            sb.append(" at ");
            sb.append(atView.getText());
        }
    }

    private void initFilename(final View rootView) {
        filenameLayout = rootView.findViewById(R.id.filename_layout);
        filenameTitle = rootView.findViewById(R.id.filename_title);
        filename = rootView.findViewById(R.id.filename);
        ImageButton filenameButton = rootView.findViewById(R.id.filename_button);

        filenameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    OpenDialog dialog = new OpenDialog(getContext(), cache, mediaPath, mediaExtent, new OpenDialog.SelectedCallback() {
                        @Override
                        public void onSelect(final String fn) {
                           filename.setText(new File(fn).getName());

                            if (fn.endsWith(".png")) {
                                cgLayout.setVisibility(View.VISIBLE);
                                textLayout.setVisibility(View.GONE);
                                pageLayout.setVisibility(View.GONE);

                            } else if (fn.endsWith(".pdf")) {
                                cgLayout.setVisibility(View.VISIBLE);
                                textLayout.setVisibility(View.GONE);
                                pageLayout.setVisibility(View.VISIBLE);

                                EditText number = rootView.findViewById(R.id.number);
                                number.setText("1");

                            } else if (fn.endsWith(".xml")) {
                                cgLayout.setVisibility(View.VISIBLE);
                                textLayout.setVisibility(View.VISIBLE);
                                pageLayout.setVisibility(View.GONE);

                                textView.setText("");

                                StringBuilder sb = new StringBuilder();
                                Contents contents = new Contents();
                                boolean hasScript = false;

                                if (contents.open(fn)) {
                                    for (int i = 0; i < contents.size(); i++) {
                                        Content content = contents.get(i);

                                        if (content.getSkipScriptField())
                                            continue;
                                        if(content.getFillMode() == FILL_CAM)
                                            continue;

                                        if(content.getFillMode() == FILL_MP1) {
                                            if(Utils.isNullOrEmpty(content.getText()))
                                                content.setText("mp1_filename")  ;
                                        }

                                        if(content.getFillMode() == FILL_MP2) {
                                            if(Utils.isNullOrEmpty(content.getText()))
                                                content.setText("mp2_filename")  ;
                                        }

                                        hasScript = true;
                                        sb.append(content.getText());
                                        if (i < contents.size() - 1)
                                            sb.append("\n");
                                    }

                                    if(hasScript)
                                        textView.setText(sb);
                                    else
                                        textLayout.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void initVideo(View rootView) {
        videoLayout = rootView.findViewById(R.id.video_layout);
        videoOptions = rootView.findViewById(R.id.video_options);

        List<String> selections = new ArrayList<String>();
        selections.add("cut");
        selections.add("short mix");
        selections.add("long mix");

        if (device == Msg.CG) {
            selections.add("key on");
            selections.add("key off");
        }

        selections.add("none");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, selections);
        videoOptions.setAdapter(adapter);
    }

    private void initAudio(View rootView) {
        audioLayout = rootView.findViewById(R.id.audio_layout);

        final TextView audioLevelText = rootView.findViewById(R.id.audio_level_text);
        audioLevel = rootView.findViewById(R.id.audio_level);

        audioLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioLevelText.setText(Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        audioLevel.setProgress(100);

        audioOptions = rootView.findViewById(R.id.audio_options);

        List<String> selections = new ArrayList<String>();
        selections.add("follow");
        selections.add("over");
        selections.add("none");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, selections);
        audioOptions.setAdapter(adapter);
    }

    private void initMediaPlayer(View rootView) {
        mediaPlayerLayout = rootView.findViewById(R.id.media_player_layout);
        fromView = rootView.findViewById(R.id.from);
        loop = rootView.findViewById(R.id.loop);
    }

    private void initCamera(View rootView) {
        cameraLayout = rootView.findViewById(R.id.camera_layout);
        cameraOptions = rootView.findViewById(R.id.camera_options);
        cameraZoom = rootView.findViewById(R.id.camera_zoom);
        cameraAel = rootView.findViewById(R.id.camera_ael);
        cameraAwbl = rootView.findViewById(R.id.camera_awbl);

        List<String> selections = new ArrayList<String>();
        selections.add("current");
        selections.add("front");
        selections.add("back");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, selections);
        cameraOptions.setAdapter(adapter);

        final TextView zoomLevelText = rootView.findViewById(R.id.camera_zoom_text);
        cameraZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                zoomLevelText.setText(Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        cameraZoom.setProgress(0);
    }

    private void initCg(View rootView) {
        textView = rootView.findViewById(R.id.text);
        textLayout = rootView.findViewById(R.id.textLayout);
        pageLayout = rootView.findViewById(R.id.pageLayout);
    }

    private void setVisibility(int device) {
        switch (device) {
            case Msg.CAM:
                cameraLayout.setVisibility(View.VISIBLE);
                mediaPlayerLayout.setVisibility(View.GONE);
                cgLayout.setVisibility(View.GONE);
                recordLayout.setVisibility(View.GONE);
                atLayout.setVisibility(View.VISIBLE);
                videoLayout.setVisibility(View.VISIBLE);
                audioLayout.setVisibility(View.VISIBLE);
                filenameLayout.setVisibility(View.GONE);
                break;

            case Msg.MP1:
                cameraLayout.setVisibility(View.GONE);
                mediaPlayerLayout.setVisibility(View.VISIBLE);
                cgLayout.setVisibility(View.GONE);
                recordLayout.setVisibility(View.GONE);
                atLayout.setVisibility(View.VISIBLE);
                videoLayout.setVisibility(View.VISIBLE);
                audioLayout.setVisibility(View.VISIBLE);
                filenameLayout.setVisibility(View.VISIBLE);
                filenameTitle.setText("Mp1 filename:");
                mediaPath = preferences.videoPath();
                mediaExtent = new String[]{".mp4", ".m4a", ".ts"};
                break;

            case Msg.MP2:
                cameraLayout.setVisibility(View.GONE);
                mediaPlayerLayout.setVisibility(View.VISIBLE);
                cgLayout.setVisibility(View.GONE);
                recordLayout.setVisibility(View.GONE);
                atLayout.setVisibility(View.VISIBLE);
                videoLayout.setVisibility(View.VISIBLE);
                audioLayout.setVisibility(View.VISIBLE);
                filenameLayout.setVisibility(View.VISIBLE);
                filenameTitle.setText("Mp2 filename:");
                mediaPath = preferences.videoPath();
                mediaExtent = new String[]{".mp4", ".m4a", ".ts"};
                break;

            case Msg.CG:
                cameraLayout.setVisibility(View.GONE);
                mediaPlayerLayout.setVisibility(View.GONE);
                recordLayout.setVisibility(View.GONE);
                atLayout.setVisibility(View.VISIBLE);
                // todo, separate fields and page
                cgLayout.setVisibility(View.GONE);
                textLayout.setVisibility(View.GONE); // visible when user opens xml
                pageLayout.setVisibility(View.GONE);

                videoLayout.setVisibility(View.VISIBLE);
                audioLayout.setVisibility(View.GONE);
                filenameLayout.setVisibility(View.VISIBLE);
                filenameTitle.setText("Cg filename:");
                mediaPath = preferences.graphicsPath();
                mediaExtent = new String[]{".pdf", ".xml", ".png", ".jpg", ".webp"};
                break;

            case Msg.TTS:
                cameraLayout.setVisibility(View.GONE);
                mediaPlayerLayout.setVisibility(View.GONE);
                recordLayout.setVisibility(View.GONE);
                atLayout.setVisibility(View.VISIBLE);

                cgLayout.setVisibility(View.VISIBLE);
                textLayout.setVisibility(View.VISIBLE);
                pageLayout.setVisibility(View.GONE);

                videoLayout.setVisibility(View.GONE);
                audioLayout.setVisibility(View.VISIBLE);
                filenameLayout.setVisibility(View.GONE);
                break;

            case Msg.RECORD:
                cameraLayout.setVisibility(View.GONE);
                mediaPlayerLayout.setVisibility(View.GONE);
                recordLayout.setVisibility(View.VISIBLE);
                atLayout.setVisibility(View.VISIBLE);

                // todo, separate fields and page
                cgLayout.setVisibility(View.GONE);
                textLayout.setVisibility(View.GONE);
                pageLayout.setVisibility(View.GONE);

                videoLayout.setVisibility(View.GONE);
                audioLayout.setVisibility(View.GONE);
                filenameLayout.setVisibility(View.GONE);
                break;
        }
    }
}
