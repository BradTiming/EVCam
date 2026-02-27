package com.kooo.evcam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class BlindSpotLabFragment extends Fragment {
    private SwitchMaterial mainFloatingSwitch;
    private Spinner mainFloatingCameraSpinner;
    private SwitchMaterial reuseMainFloatingSwitch;
    private SwitchMaterial teslaStyleSwitch;
    private SwitchMaterial lowLatencySwitch;
    private SwitchMaterial disableMainFloatingSignalSwitch;
    private SwitchMaterial raceModeSwitch;
    private SeekBar raceZoomSeek;
    private SeekBar raceFisheyeSeek;
    private SeekBar raceWidthSeek;
    private SeekBar raceHeightSeek;
    private TextView raceZoomValue;
    private TextView raceFisheyeValue;
    private TextView raceWidthValue;
    private TextView raceHeightValue;
    private Button setupBlindSpotPosButton;
    private Button saveButton;
    private Button backButton;
    private Button homeButton;

    private AppConfig appConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blind_spot_lab, container, false);
        appConfig = new AppConfig(requireContext());
        initViews(view);
        loadSettings();
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.btn_back);
        homeButton = view.findViewById(R.id.btn_home);

        mainFloatingSwitch = view.findViewById(R.id.switch_main_floating);
        mainFloatingCameraSpinner = view.findViewById(R.id.spinner_main_floating_camera);
        reuseMainFloatingSwitch = view.findViewById(R.id.switch_reuse_main_floating);
        teslaStyleSwitch = view.findViewById(R.id.switch_tesla_style);
        lowLatencySwitch = view.findViewById(R.id.switch_low_latency);
        disableMainFloatingSignalSwitch = view.findViewById(R.id.switch_disable_main_floating_signal);
        raceModeSwitch = view.findViewById(R.id.switch_race_mode);
        raceZoomSeek = view.findViewById(R.id.seek_race_zoom);
        raceFisheyeSeek = view.findViewById(R.id.seek_race_fisheye);
        raceWidthSeek = view.findViewById(R.id.seek_race_width);
        raceHeightSeek = view.findViewById(R.id.seek_race_height);
        raceZoomValue = view.findViewById(R.id.tv_race_zoom_value);
        raceFisheyeValue = view.findViewById(R.id.tv_race_fisheye_value);
        raceWidthValue = view.findViewById(R.id.tv_race_width_value);
        raceHeightValue = view.findViewById(R.id.tv_race_height_value);
        setupBlindSpotPosButton = view.findViewById(R.id.btn_setup_blind_spot_pos);
        saveButton = view.findViewById(R.id.btn_save_apply);

        String[] cameraNames = {"Front camera", "Rear camera", "Left camera", "Right camera"};
        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, cameraNames);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainFloatingCameraSpinner.setAdapter(cameraAdapter);
    }

    private void loadSettings() {
        mainFloatingSwitch.setChecked(appConfig.isMainFloatingEnabled());
        mainFloatingCameraSpinner.setSelection(getCameraIndex(appConfig.getMainFloatingCamera()));
        reuseMainFloatingSwitch.setChecked(appConfig.isTurnSignalReuseMainFloating());
        teslaStyleSwitch.setChecked(appConfig.isBlindSpotTeslaStyleEnabled());
        lowLatencySwitch.setChecked(appConfig.isBlindSpotLowLatencyEnabled());
        disableMainFloatingSignalSwitch.setChecked(appConfig.isBlindSpotDisableMainFloatingInSignal());
        raceModeSwitch.setChecked(appConfig.isLabRaceModeEnabled());
        raceZoomSeek.setProgress(Math.round((appConfig.getLabRaceModeZoom() - 1.0f) * 100f));
        raceFisheyeSeek.setProgress(Math.round(appConfig.getLabRaceModeFisheyeReduction() * 100f));
        raceWidthSeek.setProgress(appConfig.getLabRaceModeWidth());
        raceHeightSeek.setProgress(appConfig.getLabRaceModeHeight());
        updateRaceValueLabels();
        setupBlindSpotPosButton.setVisibility(appConfig.isTurnSignalReuseMainFloating() ? View.GONE : View.VISIBLE);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager().popBackStack();
        });

        homeButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).goToRecordingInterface();
            }
        });

        mainFloatingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !WakeUpHelper.hasOverlayPermission(requireContext())) {
                mainFloatingSwitch.setChecked(false);
                Toast.makeText(requireContext(), "Please grant overlay permission first", Toast.LENGTH_SHORT).show();
                WakeUpHelper.requestOverlayPermission(requireContext());
                return;
            }
            appConfig.setMainFloatingEnabled(isChecked);
            BlindSpotService.update(requireContext());
        });

        mainFloatingCameraSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            private boolean first = true;

            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (first) {
                    first = false;
                    return;
                }
                appConfig.setMainFloatingCamera(getCameraPos(position));
                BlindSpotService.update(requireContext());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        reuseMainFloatingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appConfig.setTurnSignalReuseMainFloating(isChecked);
            setupBlindSpotPosButton.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            BlindSpotService.update(requireContext());
        });

        teslaStyleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appConfig.setBlindSpotTeslaStyleEnabled(isChecked);
            BlindSpotService.update(requireContext());
            String message = isChecked
                    ? "Tesla-style turn signal preview enabled"
                    : "Tesla-style turn signal preview disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        lowLatencySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appConfig.setBlindSpotLowLatencyEnabled(isChecked);
            BlindSpotService.update(requireContext());
            String message = isChecked
                    ? "Low-latency camera feed enabled"
                    : "Low-latency camera feed disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        disableMainFloatingSignalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appConfig.setBlindSpotDisableMainFloatingInSignal(isChecked);
            BlindSpotService.update(requireContext());
        });

        raceModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appConfig.setLabRaceModeEnabled(isChecked);
            Intent intent = new Intent(requireContext(), BlindSpotService.class);
            intent.putExtra("action", isChecked ? "show_race_mode" : "hide_race_mode");
            requireContext().startService(intent);
        });

        raceZoomSeek.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float zoom = 1.0f + (progress / 100f);
                appConfig.setLabRaceModeZoom(zoom);
                updateRaceValueLabels();
                BlindSpotService.update(requireContext());
            }
        });

        raceFisheyeSeek.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                appConfig.setLabRaceModeFisheyeReduction(progress / 100f);
                updateRaceValueLabels();
                BlindSpotService.update(requireContext());
            }
        });

        raceWidthSeek.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int width = Math.max(220, progress);
                appConfig.setLabRaceModeWindowSize(width, appConfig.getLabRaceModeHeight());
                updateRaceValueLabels();
                BlindSpotService.update(requireContext());
            }
        });

        raceHeightSeek.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int height = Math.max(124, progress);
                appConfig.setLabRaceModeWindowSize(appConfig.getLabRaceModeWidth(), height);
                updateRaceValueLabels();
                BlindSpotService.update(requireContext());
            }
        });

        setupBlindSpotPosButton.setOnClickListener(v -> {
            if (!WakeUpHelper.hasOverlayPermission(requireContext())) {
                Toast.makeText(requireContext(), "Please grant overlay permission first", Toast.LENGTH_SHORT).show();
                WakeUpHelper.requestOverlayPermission(requireContext());
                return;
            }
            Intent intent = new Intent(requireContext(), BlindSpotService.class);
            intent.putExtra("action", "setup_blind_spot_window");
            requireContext().startService(intent);
        });

        saveButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Configuration saved and applied", Toast.LENGTH_SHORT).show();
            BlindSpotService.update(requireContext());
        });
    }

    private int getCameraIndex(String pos) {
        switch (pos) {
            case "front": return 0;
            case "back": return 1;
            case "left": return 2;
            case "right": return 3;
            default: return 0;
        }
    }

    private String getCameraPos(int index) {
        switch (index) {
            case 0: return "front";
            case 1: return "back";
            case 2: return "left";
            case 3: return "right";
            default: return "front";
        }
    }

    private void updateRaceValueLabels() {
        raceZoomValue.setText(String.format(java.util.Locale.US, "%.2fx", appConfig.getLabRaceModeZoom()));
        raceFisheyeValue.setText(Math.round(appConfig.getLabRaceModeFisheyeReduction() * 100f) + "%");
        raceWidthValue.setText(appConfig.getLabRaceModeWidth() + " px");
        raceHeightValue.setText(appConfig.getLabRaceModeHeight() + " px");
    }

    private static abstract class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
