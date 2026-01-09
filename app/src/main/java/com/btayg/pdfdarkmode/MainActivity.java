package com.btayg.pdfdarkmode;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.getcapacitor.BridgeActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends BridgeActivity {

    private FrameLayout bannerContainer;
    private FrameLayout nativeContainer;
    private AdView bannerAdView;
    private NativeAd nativeAd;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean isRewardEarned = false;

    private static final String BANNER_ID = "ca-app-pub-1066146186486671/9550459406";
    private static final String NATIVE_ID = "ca-app-pub-1066146186486671/1790323229";
    private static final String REWARDED_ID = "ca-app-pub-1066146186486671/2597892453";
    private static final String INTERSTITIAL_ID = "ca-app-pub-1066146186486671/2881055374";
    private static final int APP_BG_COLOR_INT = Color.parseColor("#0f0f13");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().getDecorView().setBackgroundColor(APP_BG_COLOR_INT);
            this.findViewById(android.R.id.content).setBackgroundColor(APP_BG_COLOR_INT);
        } catch (Exception e) { e.printStackTrace(); }

        MobileAds.initialize(this, status -> {});
        pushWebViewDown(true);
        setupBanner();
        setupNative();
        loadInterstitial();
        loadRewardedAd();

        if (this.getBridge() != null && this.getBridge().getWebView() != null) {
            this.getBridge().getWebView().addJavascriptInterface(this, "Android");
        }
    }

    private void setupBanner() {
        bannerContainer = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        bannerContainer.setLayoutParams(params);
        bannerContainer.setBackgroundColor(APP_BG_COLOR_INT);
        addContentView(bannerContainer, params);
        bannerAdView = new AdView(this);
        bannerAdView.setAdUnitId(BANNER_ID);
        bannerContainer.addView(bannerAdView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (int) (metrics.widthPixels / metrics.density));
        bannerAdView.setAdSize(adSize);
        bannerAdView.loadAd(new AdRequest.Builder().build());
    }

    private void setupNative() {
        nativeContainer = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP;
        nativeContainer.setLayoutParams(params);
        nativeContainer.setBackgroundColor(APP_BG_COLOR_INT);
        addContentView(nativeContainer, params);
        AdLoader loader = new AdLoader.Builder(this, NATIVE_ID)
                .forNativeAd(ad -> {
                    if (nativeAd != null) nativeAd.destroy();
                    nativeAd = ad;
                    showNative(ad);
                }).build();
        loader.loadAd(new AdRequest.Builder().build());
    }

    private void showNative(NativeAd ad) {
        try {
            NativeAdView adView = (NativeAdView) LayoutInflater.from(this).inflate(R.layout.ad_native_layout, nativeContainer, false);
            TextView headline = adView.findViewById(R.id.ad_headline);
            TextView body = adView.findViewById(R.id.ad_body);
            Button cta = adView.findViewById(R.id.ad_call_to_action);
            ImageView icon = adView.findViewById(R.id.ad_app_icon);
            headline.setText(ad.getHeadline());
            body.setText(ad.getBody());
            cta.setText(ad.getCallToAction());
            adView.setHeadlineView(headline);
            adView.setBodyView(body);
            adView.setCallToActionView(cta);
            adView.setIconView(icon);
            if (ad.getIcon() != null) {
                icon.setImageDrawable(ad.getIcon().getDrawable());
                icon.setVisibility(View.VISIBLE);
            } else icon.setVisibility(View.GONE);
            adView.setNativeAd(ad);
            nativeContainer.removeAllViews();
            nativeContainer.addView(adView);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void pushWebViewDown(boolean push) {
        runOnUiThread(() -> {
            try {
                if (getBridge() == null || getBridge().getWebView() == null) return;
                View webView = getBridge().getWebView();
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) webView.getLayoutParams();
                float density = getResources().getDisplayMetrics().density;
                int topMargin = push ? (int)(60 * density) : 0;
                int bottomMargin = push ? (int)(50 * density) : 0;
                params.setMargins(0, topMargin, 0, bottomMargin);
                webView.setLayoutParams(params);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_ID, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) { interstitialAd = null; }
        });
    }

    private void loadRewardedAd() {
        RewardedAd.load(this, REWARDED_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { rewardedAd = null; }
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) { rewardedAd = ad; }
        });
    }

    @JavascriptInterface
    public void showBigAd() {
        runOnUiThread(() -> {
            if (interstitialAd != null) {
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        loadInterstitial();
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("javascript:window.onBigAdClosed()", null);
                        }
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        interstitialAd = null;
                        if (getBridge() != null && getBridge().getWebView() != null) {
                            getBridge().getWebView().evaluateJavascript("javascript:window.onBigAdClosed()", null);
                        }
                    }
                });
                interstitialAd.show(MainActivity.this);
            } else {
                loadInterstitial();
                if (getBridge() != null && getBridge().getWebView() != null) {
                    getBridge().getWebView().evaluateJavascript("javascript:window.onBigAdClosed()", null);
                }
            }
        });
    }

    @JavascriptInterface
    public void showRewardedAd() {
        runOnUiThread(() -> {
            if (rewardedAd != null) {
                isRewardEarned = false;
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        loadRewardedAd();
                        runOnUiThread(() -> {
                            if (getBridge() != null && getBridge().getWebView() != null) {
                                getBridge().getWebView().evaluateJavascript("javascript:onRewardedAdCompleted(" + isRewardEarned + ")", null);
                            }
                        });
                        if (isRewardEarned) hideBannerAds();
                    }
                });
                rewardedAd.show(MainActivity.this, rewardItem -> isRewardEarned = true);
            } else {
                loadRewardedAd();
                Toast.makeText(this, "Ad loading...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public void hideBannerAds() {
        runOnUiThread(() -> {
            if (bannerContainer != null) bannerContainer.setVisibility(View.GONE);
            if (nativeContainer != null) nativeContainer.setVisibility(View.GONE);
            pushWebViewDown(false);
        });
    }

    @JavascriptInterface
    public void showBannerAds() {
        runOnUiThread(() -> {
            if (bannerContainer != null) bannerContainer.setVisibility(View.VISIBLE);
            if (nativeContainer != null) nativeContainer.setVisibility(View.VISIBLE);
            pushWebViewDown(true);
        });
    }

    @JavascriptInterface
    public void requestAppReview() {
        ReviewManager manager = ReviewManagerFactory.create(this);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (task.isSuccessful()) manager.launchReviewFlow(this, task.getResult());
        });
    }

    // ============ YENİ METODLAR: PDF KAYDET VE AÇ ============

    /**
     * PDF'i sadece kaydeder (açmaz)
     */
    @JavascriptInterface
    public void savePdfOnly(String base64Data, String fileName) {
        new Thread(() -> {
            try {
                byte[] pdfBytes = Base64.decode(base64Data, Base64.DEFAULT);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File pdfFile = new File(downloadsDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                fos.write(pdfBytes);
                fos.close();

                runOnUiThread(() -> Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * PDF'i kaydeder ve otomatik olarak açar
     */
    @JavascriptInterface
    public void savePdfAndOpen(String base64Data, String fileName) {
        new Thread(() -> {
            try {
                byte[] pdfBytes = Base64.decode(base64Data, Base64.DEFAULT);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File pdfFile = new File(downloadsDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                fos.write(pdfBytes);
                fos.close();

                // PDF'i aç
                runOnUiThread(() -> openPdfFile(pdfFile));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * Var olan bir PDF dosyasını açar
     */
    private void openPdfFile(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            // PDF okuyucu var mı kontrol et
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Open PDF with"));
            } else {
                Toast.makeText(this, "No PDF reader found. Please install one.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Path ile PDF aç (JavaScript'ten direkt çağrılabilir)
     */
    @JavascriptInterface
    public void openPdfByPath(String filePath) {
        runOnUiThread(() -> {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    openPdfFile(file);
                } else {
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
            }
        });
    }
}