package com.webhtml.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private DownloadHelper downloadHelper;

    // Kodovi i promenljive za sistemske dozvole u hodu
    private final static int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private final static int MEDIA_PERMISSION_REQUEST_CODE = 101;
    
    private String pendingGeolocationOrigin;
    private GeolocationPermissions.Callback pendingGeolocationCallback;
    private PermissionRequest pendingPermissionRequest;

    @SuppressLint({"SetJavaScriptEnabled", "QueryPermissionsNeeded"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        );
        super.onCreate(savedInstanceState);
        
        // 1. Kreiramo čisti WebView preko celog ekrana (bez SwipeRefreshLayout-a)
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        webView.setBackgroundColor(Color.parseColor("#070707"));

        // Postavljamo WebView direktno kao glavni sadržaj
        setContentView(webView);

        // Inicijalizujemo DownloadHelper
        downloadHelper = new DownloadHelper(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Omogućavanje kolačića i kolačića treće strane
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Registrujemo DownloadHelper kao JavaScript Bridge
        webView.addJavascriptInterface(downloadHelper, "AndroidBridge");

        // Moderno upravljanje dugmetom nazad (OnBackPressedDispatcher)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String rawSuggestedName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
            String extension = "txt";
            if (rawSuggestedName != null && rawSuggestedName.contains(".")) {
                extension = rawSuggestedName.substring(rawSuggestedName.lastIndexOf(".") + 1);
            }
            final String suggestedFileName = "download." + extension;

            if (url.startsWith("blob:") || url.startsWith("data:")) {
                String js = "(function() {" +
                        "  fetch('" + url + "')" +
                        "    .then(res => res.blob())" +
                        "    .then(blob => {" +
                        "      var reader = new FileReader();" +
                        "      reader.onload = function() {" +
                        "        window.AndroidBridge.cacheData(reader.result);" +
                        "      };" +
                        "      reader.readAsDataURL(blob);" +
                        "    }).catch(err => window.AndroidBridge.cacheData('ERROR'));" +
                        "})();";
                webView.evaluateJavascript(js, null);

                webView.postDelayed(() -> DialogHelper.showNativeDownloadDialog(
                        MainActivity.this, suggestedFileName, url, mimetype, true,
                        finalName -> downloadHelper.executeDownloadTask(finalName, url, mimetype, true)
                ), 300);
            } else {
                DialogHelper.showNativeDownloadDialog(
                        MainActivity.this, suggestedFileName, url, mimetype, false,
                        finalName -> downloadHelper.executeDownloadTask(finalName, url, mimetype, false)
                );
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlLoading(view, url);
            }

            private boolean handleUrlLoading(WebView view, String url) {
                if (url.startsWith("file://") || url.startsWith("http://") || url.startsWith("https://")) {
                    return false; 
                }
                
                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            try {
                                startActivity(intent);
                                return true;
                            } catch (Exception e) {
                                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                                if (fallbackUrl != null) {
                                    view.loadUrl(fallbackUrl);
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {}
                    return true;
                }

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url != null && url.startsWith("file://")) {
                    view.evaluateJavascript("window.webhtml = true;", null);
                }

                // Univerzalna lokalna Pull-to-Refresh skripta koja radi na svakom sajtu i lokalnom fajlu
                String rawPtrInjection = 
                    "(function() {" +
                    "    if (window._ptrInitialized) return;" +
                    "    window._ptrInitialized = true;" +
                    "    " +
                    "    let startY = 0;" +
                    "    let pulling = false;" +
                    "    let refreshing = false;" +
                    "    " +
                    "    let indicator = document.createElement('div');" +
                    "    indicator.style.cssText = 'position:fixed;top:-50px;left:50%;transform:translateX(-50%);width:35px;height:35px;background:#222;border:2px solid #555;border-radius:50%;z-index:999999;display:flex;align-items:center;justify-content:center;transition:top 0.2s;box-shadow:0 2px 5px rgba(0,0,0,0.3);';" +
                    "    indicator.innerHTML = '<div style=\"width:18px;height:18px;border:2px solid #fff;border-top-color:transparent;border-radius:50%;animation:ptr-spin 0.8s linear infinite;\"></div>';" +
                    "    " +
                    "    let styleSheet = document.createElement('style');" +
                    "    styleSheet.innerHTML = '@keyframes ptr-spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }';" +
                    "    document.head.appendChild(styleSheet);" +
                    "    document.body.appendChild(indicator);" +
                    "    " +
                    "    window.addEventListener('touchstart', function(e) {" +
                    "        if (window.pageYOffset <= 0) {" +
                    "            let el = e.target;" +
                    "            let inScrollable = false;" +
                    "            while (el && el !== document.body) {" +
                    "                let style = window.getComputedStyle(el);" +
                    "                let oy = style.getPropertyValue('overflow-y');" +
                    "                if ((oy === 'auto' || oy === 'scroll') && el.scrollTop > 0) {" +
                    "                    inScrollable = true;" +
                    "                    break;" +
                    "                }" +
                    "                el = el.parentElement;" +
                    "            }" +
                    "            if (!inScrollable) {" +
                    "                startY = e.touches[0].clientY;" +
                    "                pulling = true;" +
                    "            }" +
                    "        }" +
                    "    }, {passive: true});" +
                    "    " +
                    "    window.addEventListener('touchmove', function(e) {" +
                    "        if (!pulling || refreshing) return;" +
                    "        let y = e.touches[0].clientY;" +
                    "        let diff = y - startY;" +
                    "        if (diff > 0 && window.pageYOffset <= 0) {" +
                    "            let pullDistance = Math.min(diff * 0.4, 80);" +
                    "            indicator.style.top = (pullDistance - 40) + 'px';" +
                    "            if (pullDistance > 60) {" +
                    "                refreshing = true;" +
                    "                pulling = false;" +
                    "                indicator.style.top = '20px';" +
                    "                setTimeout(function() { window.location.reload(); }, 400);" +
                    "            }" +
                    "        }" +
                    "    }, {passive: true});" +
                    "    " +
                    "    window.addEventListener('touchend', function() {" +
                    "        if (!refreshing) {" +
                    "            pulling = false;" +
                    "            indicator.style.top = '-50px';" +
                    "        }" +
                    "    }, {passive: true});" +
                    "})();";

                view.evaluateJavascript(rawPtrInjection, null);
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String urlStr = request.getUrl().toString();
                if (!urlStr.contains("#")) {
                    return super.shouldInterceptRequest(view, request);
                }

                try {
                    String[] mainParts = urlStr.split("#", 2);
                    String cleanUrlStr = mainParts[0];
                    String fragment = mainParts[1];

                    boolean isHtmlMode = fragment.contains("html");
                    boolean isApiMode = fragment.contains("api");

                    String defaultUa;
                    if (isHtmlMode) {
                        defaultUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
                    } else if (isApiMode) {
                        defaultUa = "LFM_MusicApp/1.0 (contact: moj@gmail.com)";
                    } else {
                        defaultUa = webView.getSettings().getUserAgentString();
                    }

                    String finalUa = defaultUa;
                    if (fragment.contains("ua=")) {
                        try {
                            String[] uaParts = fragment.split("ua=");
                            if (uaParts.length > 1) {
                                String customUa = URLDecoder.decode(uaParts[1].split("&")[0], "UTF-8");
                                if (!customUa.isEmpty() && customUa.length() < 300) {
                                    finalUa = customUa;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    URL url = new URL(cleanUrlStr);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", finalUa);
                    
                    if (isHtmlMode) {
                        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                    }
                    
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    InputStream inputStream = connection.getInputStream();
                    String mimeType = connection.getContentType();
                    if (mimeType == null) {
                        mimeType = isHtmlMode ? "text/html; charset=UTF-8" : "application/json; charset=UTF-8";
                    }
                    
                    String encoding = "UTF-8";
                    if (mimeType.contains("charset=")) {
                        try {
                            encoding = mimeType.split("charset=")[1].split(";")[0].trim();
                        } catch (Exception ignored) {}
                    }

                    return new WebResourceResponse(mimeType.split(";")[0].trim(), encoding, inputStream);

                } catch (Exception e) {}
                
                return super.shouldInterceptRequest(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                DialogHelper.showCustomAlert(MainActivity.this, message, result);
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(MainActivity.this, 
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    androidx.core.app.ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                    pendingGeolocationOrigin = origin;
                    pendingGeolocationCallback = callback;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] requestedResources = request.getResources();
                boolean needsCamera = false;
                boolean needsAudio = false;

                for (String resource : requestedResources) {
                    if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) needsCamera = true;
                    if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) needsAudio = true;
                }

                ArrayList<String> permissionsToRequest = new ArrayList<>();
                if (needsCamera && androidx.core.content.ContextCompat.checkSelfPermission(MainActivity.this, 
                        android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(android.Manifest.permission.CAMERA);
                }
                if (needsAudio && androidx.core.content.ContextCompat.checkSelfPermission(MainActivity.this, 
                        android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO);
                }

                if (!permissionsToRequest.isEmpty()) {
                    pendingPermissionRequest = request;
                    androidx.core.app.ActivityCompat.requestPermissions(MainActivity.this,
                            permissionsToRequest.toArray(new String[0]),
                            MEDIA_PERMISSION_REQUEST_CODE);
                } else {
                    request.grant(requestedResources);
                }
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams
            ) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;
                Intent intent = fileChooserParams.createIntent();

                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    uploadMessage = null;
                    return false;
                }

                return true;
            }
        });

        // Provera intenta pri pokretanju
        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;

        if (data != null) {
            String targetUrl = data.getQueryParameter("url");
            if (targetUrl != null && !targetUrl.isEmpty()) {
                webView.loadUrl(targetUrl);
            } else {
                webView.loadUrl(data.toString());
            }
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (pendingGeolocationCallback != null && pendingGeolocationOrigin != null) {
                boolean granted = grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
                pendingGeolocationCallback.invoke(pendingGeolocationOrigin, granted, false);
                pendingGeolocationCallback = null;
                pendingGeolocationOrigin = null;
            }
        } else if (requestCode == MEDIA_PERMISSION_REQUEST_CODE) {
            if (pendingPermissionRequest != null) {
                boolean allGranted = true;
                for (int res : grantResults) {
                    if (res != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                } else {
                    pendingPermissionRequest.deny();
                }
                pendingPermissionRequest = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && intent != null) {
                String dataString = intent.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            if (webView.getParent() instanceof ViewGroup) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
