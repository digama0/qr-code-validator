package edu.osu.cse.security.qrcodevalidator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import edu.osu.cse.security.qrcodevalidator.SiteReputation.ErrorCode;

public class MainActivity extends Activity {
    public TextView text;
    public ImageView wot;
    public Button go;
    public int wotRating = -2;
    public String goUrl = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView)findViewById(R.id.text);
        wot = (ImageView)findViewById(R.id.wot);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        final Button scan = (Button)findViewById(R.id.scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final IntentIntegrator integrator = new IntentIntegrator(
                    MainActivity.this);
                integrator.initiateScan();
            }
        });
        go = (Button)findViewById(R.id.go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(goUrl)));
            }
        });

        if (savedInstanceState != null) {
            text.setText(savedInstanceState.getString("scanResults"));
            wot.setImageResource(wotResource(wotRating = savedInstanceState
                .getInt("wotRating")));
            goUrl = savedInstanceState.getString("goUrl");
            if (goUrl != null)
                go.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("scanResults", text.getText().toString());
        outState.putInt("wotRating", wotRating);
        outState.putString("goUrl", goUrl);
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
            wot.setImageDrawable(null);
            go.setVisibility(View.GONE);
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
            goUrl = sr.redirectURL;
            String redirectInfo = "Redirects to: <a href=\"" + goUrl
                + "\">" + goUrl + "</a><br/>";
            String trustInfo = "WOT Rating: " + sr.getWot() + "<br/>"
                + sr.getBlacklisted();

            if (sr.basicInfo == ErrorCode.NOT_SITE) {
                text.setText(sr.originalURL);
                wot.setImageDrawable(null);
                wotRating = -2;
                goUrl = null;
                go.setVisibility(View.GONE);
            }
            else {
                switch (sr.basicInfo) {
                    case SUCCESS:
                        text.setText(Html.fromHtml(origLink + trustInfo));
                        break;
                    case REDIRECT:
                    case BROKEN_REDIRECT:
                        text.setText(Html.fromHtml(origLink + resultInfo
                            + redirectInfo + trustInfo));
                        break;
                    default:
                        text.setText(Html.fromHtml(origLink + resultInfo
                            + trustInfo));
                }
                wot.setImageResource(wotResource(wotRating = sr.getWot()));
                go.setVisibility(View.VISIBLE);
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
