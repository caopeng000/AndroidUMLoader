package com.androidumloader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

import com.androidumloader.utils.L;

public class MainActivity extends Activity {
    private DisplayImageOptions options;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .build();
        iv = (ImageView) findViewById(R.id.iv);
        L.i("MainActivity create");
        ImageLoader.getInstance().displayImage("http://imgtu.5011.net/uploads/mnthumb/aq050mzqabp.jpg", iv);
    }
}
