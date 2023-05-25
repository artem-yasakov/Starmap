package com.experimental.star_map;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.experimental.star_map.views.StarMapView;

public class MainActivity extends AppCompatActivity {
    private SeekBar seekBar;
    private StarMapView starMapView;
    private SwitchCompat enableMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar = findViewById(R.id.setRotate);
        starMapView = findViewById(R.id.Image);
        enableMap = findViewById(R.id.enableMap);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                starMapView.setAngle(-i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        enableMap.setOnCheckedChangeListener((buttonView, isChecked) -> starMapView.switchMap());
    }
}
