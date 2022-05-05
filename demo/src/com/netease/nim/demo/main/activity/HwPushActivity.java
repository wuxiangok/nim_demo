package com.netease.nim.demo.main.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.netease.nim.demo.mixpush.DemoMixPushMessageHandler;

import java.util.HashMap;
import java.util.Set;

public class HwPushActivity extends Activity {
    private static final String TAG = "HwPushActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseIntent();
        finish();
    }

    void parseIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "getData null");
            return;
        }
        Set<String> parameterSet = uri.getQueryParameterNames();
        HashMap<String, String> map = new HashMap<>(parameterSet.size() << 1);
        String value;
        for (String p : parameterSet) {
            value = uri.getQueryParameter(p);
            if (value == null) {
                continue;
            }
            map.put(p, value);
        }
        new DemoMixPushMessageHandler().onNotificationClicked(this.getApplicationContext(), map);
    }
}
