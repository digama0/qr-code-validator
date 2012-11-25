package edu.osu.cse.security.qrcodevalidator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity {
    public TextView text;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView)findViewById(R.id.text);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        final Button button = (Button)findViewById(R.id.go);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final IntentIntegrator integrator = new IntentIntegrator(
                    MainActivity.this);
                integrator.initiateScan();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
        final Intent intent)
    {
        final IntentResult scanResult = IntentIntegrator.parseActivityResult(
            requestCode, resultCode, intent);
        if (scanResult != null) {
            text.setText(Html.fromHtml("<a href=\"" + scanResult.getContents()
                + "\">" + scanResult.getContents() + "</a>"));
        }
    }
}
