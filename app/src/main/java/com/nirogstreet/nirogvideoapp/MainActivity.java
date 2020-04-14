package com.nirogstreet.nirogvideoapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.agora.rtc.Constants;

public class MainActivity extends AppCompatActivity {

    private TextView tv_video_call, tv_audio_call;
    private TextView tv_video_broad_cost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_video_call = findViewById(R.id.tv_video_call);
        tv_audio_call = findViewById(R.id.tv_audio_call);
        tv_video_broad_cost = findViewById(R.id.tv_video_broad_cost);

        tv_video_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                startActivity(intent);
                ;
            }
        });

        tv_audio_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AudioActivity.class);
                startActivity(intent);
            }
        });


        tv_video_broad_cost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickJoin();
            }
        });

    }

    public void onClickJoin() {
        // Show a dialog box to choose a user role.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.choose_role);

        builder.setNegativeButton(R.string.label_audience, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.forwardToLiveRoom(Constants.CLIENT_ROLE_AUDIENCE);
            }
        });

        builder.setPositiveButton(R.string.label_broadcaster, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.forwardToLiveRoom(Constants.CLIENT_ROLE_BROADCASTER);
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    public void forwardToLiveRoom(int cRole) {
        Intent i = new Intent(MainActivity.this, VideoBroadcastActivity.class);
        i.putExtra("CRole", cRole);
        i.putExtra("CName", "NirogVideoApp");
        startActivity(i);
    }

}
