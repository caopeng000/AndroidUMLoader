package com.androidumloader;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    private DisplayImageOptions options;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .build();

    }
}
