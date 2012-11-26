package edu.osu.cse.security.qrcodevalidator;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
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
            text.setText(Html.fromHtml(scanResult.getContents() + "<br/>Checking security..."));
            new RedirectExplorerTask().execute(scanResult.getContents());
        }
    }

    private class RedirectExplorerTask extends
        AsyncTask<String, Void, SiteReputation>
    {
        protected SiteReputation doInBackground(String... url) {
            SiteReputation sr = new SiteReputation(url[0]);
            sr.getWOT();
            sr.getBlacklisted();
            return sr;
        }

        protected void onProgressUpdate(Void... nothing) {}

        protected void onPostExecute(SiteReputation sr) {
            String origLink = "<a href=\"" + sr.originalURL + "\">"
                + sr.originalURL + "</a><br/>";
            String resultInfo = sr.basicInfo.verbose + "<br/>";
            String redirectInfo = "Redirects to: <a href=\"" + sr.redirectURL
                + "\">" + sr.redirectURL + "</a><br/>";
            String trustInfo = "WOT Rating: " + sr.getWOT() + "<br/>"
                + sr.getBlacklisted();
            switch (sr.basicInfo) {
                case NOT_SITE:
                    text.setText(sr.originalURL);
                    break;
                case SUCCESS:
                    text.setText(Html.fromHtml(origLink + trustInfo));
                    break;
                case REDIRECT:
                case BROKEN_REDIRECT:
                    text.setText(Html.fromHtml(origLink + resultInfo + redirectInfo
                        + trustInfo));
                    break;
                default:
                    text.setText(Html.fromHtml(origLink + resultInfo + trustInfo));
            }
        }
    }
}
