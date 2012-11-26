package edu.osu.cse.security.qrcodevalidator;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity {
    public TextView text;
    public ImageView wot;
    public int wotRating = -2;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView)findViewById(R.id.text);
        wot = (ImageView)findViewById(R.id.wot);
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

        if (savedInstanceState != null) {
            text.setText(savedInstanceState.getString("scanResults"));
            wot.setImageResource(wotResource(wotRating = savedInstanceState
                .getInt("wotRating")));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("scanResults", text.getText().toString());
        outState.putInt("wotRating", wotRating);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
        final Intent intent)
    {
        final IntentResult scanResult = IntentIntegrator.parseActivityResult(
            requestCode, resultCode, intent);
        if (scanResult != null) {
            text.setText(Html.fromHtml(scanResult.getContents()
                + "<br/>Checking security..."));
            new RedirectExplorerTask().execute(scanResult.getContents());
        }
    }

    private class RedirectExplorerTask extends
        AsyncTask<String, Void, SiteReputation>
    {
        protected SiteReputation doInBackground(String... url) {
            SiteReputation sr = new SiteReputation(url[0]);
            sr.getWot();
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
            String trustInfo = "WOT Rating: " + sr.getWot() + "<br/>"
                + sr.getBlacklisted();

            switch (sr.basicInfo) {
                case NOT_SITE:
                    text.setText(sr.originalURL);
                    wot.setImageDrawable(null);
                    wotRating = -2;
                    break;
                case SUCCESS:
                    text.setText(Html.fromHtml(origLink + trustInfo));
                    wot.setImageResource(wotResource(wotRating = sr.getWot()));
                    break;
                case REDIRECT:
                case BROKEN_REDIRECT:
                    text.setText(Html.fromHtml(origLink + resultInfo
                        + redirectInfo + trustInfo));
                    wot.setImageResource(wotResource(wotRating = sr.getWot()));
                    break;
                default:
                    text.setText(Html.fromHtml(origLink + resultInfo
                        + trustInfo));
                    wot.setImageResource(wotResource(wotRating = sr.getWot()));
            }
        }
    }

    public static int wotResource(int wot) {
        switch ((wot + 20) / 20) {
            case 1:
                return R.drawable.verypoor;
            case 2:
                return R.drawable.poor;
            case 3:
                return R.drawable.unsatisfactory;
            case 4:
                return R.drawable.good;
            case 5:
                return R.drawable.excellent;
        }
        return R.drawable.unrated;
    }
}
