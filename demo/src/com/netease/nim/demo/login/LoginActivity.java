package com.netease.nim.demo.login;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.TextView;

import com.netease.nim.demo.DemoCache;
import com.netease.nim.demo.DemoPrivatizationConfig;
import com.netease.nim.demo.R;
import com.netease.nim.demo.config.preference.Preferences;
import com.netease.nim.demo.config.preference.UserPreferences;
import com.netease.nim.demo.contact.ContactHttpClient;
import com.netease.nim.demo.main.activity.MainActivity;
import com.netease.nim.demo.main.activity.PrivatizationConfigActivity;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.wrapper.NimToolBarOptions;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.activity.ToolBarOptions;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.ui.widget.ClearableEditTextWithIcon;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.uikit.common.util.string.MD5;
import com.netease.nim.uikit.common.util.sys.NetworkUtil;
import com.netease.nim.uikit.common.util.sys.ScreenUtil;
import com.netease.nim.uikit.support.permission.MPermission;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionDenied;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionGranted;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.ClientType;
import com.netease.nimlib.sdk.auth.LoginInfo;

/**
 * ??????/????????????
 * <p/>
 * Created by huangjun on 2015/2/1.
 */
public class LoginActivity extends UI implements OnKeyListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final String KICK_OUT = "KICK_OUT";

    private final int BASIC_PERMISSION_REQUEST_CODE = 110;

    private TextView rightTopBtn;  // ActionBar????????????

    private TextView leftTopBtn;

    private TextView switchModeBtn;  // ??????/??????????????????

    private ClearableEditTextWithIcon loginAccountEdit;

    private ClearableEditTextWithIcon loginPasswordEdit;

    private ClearableEditTextWithIcon loginSubtypeEdit;

    private ClearableEditTextWithIcon registerAccountEdit;

    private ClearableEditTextWithIcon registerNickNameEdit;

    private ClearableEditTextWithIcon registerPasswordEdit;

    private View loginLayout;

    private View registerLayout;

    private AbortableFuture<LoginInfo> loginRequest;

    private boolean registerMode = false; // ????????????

    private boolean registerPanelInited = false; // ???????????????????????????

    public static void start(Context context) {
        start(context, false);
    }

    public static void start(Context context, boolean kickOut) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KICK_OUT, kickOut);
        context.startActivity(intent);
    }

    @Override
    protected boolean displayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        ToolBarOptions options = new NimToolBarOptions();
        options.isNeedNavigate = false;
        options.logoId = R.drawable.actionbar_white_logo_space;
        setToolBar(R.id.toolbar, options);
        requestBasicPermission();
        onParseIntent();
        initRightTopBtn();
        initLeftTopBtn();
        setupLoginPanel();
        setupRegisterPanel();
    }

    /**
     * ??????????????????
     */
    private final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    private void requestBasicPermission() {
        MPermission.with(LoginActivity.this).setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                   .permissions(BASIC_PERMISSIONS).request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        ToastHelper.showToast(this, "????????????");
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        ToastHelper.showToast(this, "????????????");
    }

    private void onParseIntent() {
        if (!getIntent().getBooleanExtra(KICK_OUT, false)) {
            return;
        }
        int type = NIMClient.getService(AuthService.class).getKickedClientType();
        int customType = NIMClient.getService(AuthService.class).getKickedCustomClientType();
        String client;
        switch (type) {
            case ClientType.Web:
                client = "?????????";
                break;
            case ClientType.Windows:
            case ClientType.MAC:
                client = "?????????";
                break;
            case ClientType.REST:
                client = "?????????";
                break;
            default:
                client = "?????????";
                break;
        }
        EasyAlertDialogHelper.showOneButtonDiolag(LoginActivity.this,
                                                  getString(R.string.kickout_notify),
                                                  String.format(getString(R.string.kickout_content),
                                                                client + customType), getString(R.string.ok),
                                                  true, null);

    }

    /**
     * ActionBar ???????????????
     */
    private void initLeftTopBtn() {
        leftTopBtn = addRegisterLeftTopBtn(this, R.string.login_privatization_config_str);
        leftTopBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, PrivatizationConfigActivity.class));
        });
    }

    /**
     * ActionBar ???????????????
     */
    private void initRightTopBtn() {
        rightTopBtn = addRegisterRightTopBtn(this, R.string.login);
        rightTopBtn.setOnClickListener(v -> {
            if (registerMode) {
                register();
            } else {
                //fakeLoginTest(); // ?????????????????????
                login();
            }
        });
    }

    /**
     * ????????????
     */
    private void setupLoginPanel() {
        loginAccountEdit = findView(R.id.edit_login_account);
        loginPasswordEdit = findView(R.id.edit_login_password);
        loginSubtypeEdit = findViewById(R.id.edit_login_subtype);
        loginAccountEdit.setIconResource(R.drawable.user_account_icon);
        loginPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);
        loginAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginSubtypeEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginAccountEdit.addTextChangedListener(textWatcher);
        loginPasswordEdit.addTextChangedListener(textWatcher);
        loginPasswordEdit.setOnKeyListener(this);
        String account = Preferences.getUserAccount();
        loginAccountEdit.setText(account);
    }

    /**
     * ????????????
     */
    private void setupRegisterPanel() {
        loginLayout = findView(R.id.login_layout);
        registerLayout = findView(R.id.register_layout);
        switchModeBtn = findView(R.id.register_login_tip);
        switchModeBtn.setVisibility(
                DemoPrivatizationConfig.isPrivateDisable(this) ? View.VISIBLE : View.GONE);
        switchModeBtn.setOnClickListener(v -> switchMode());
    }

    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (registerMode) {
                return;
            }
            // ????????????  ??????????????????????????????
            boolean isEnable = loginAccountEdit.getText().length() > 0 &&
                               loginPasswordEdit.getText().length() > 0;
            updateRightTopBtn(rightTopBtn, isEnable);
        }
    };

    private void updateRightTopBtn(TextView rightTopBtn, boolean isEnable) {
        rightTopBtn.setText(R.string.done);
        rightTopBtn.setBackgroundResource(R.drawable.g_white_btn_selector);
        rightTopBtn.setEnabled(isEnable);
        rightTopBtn.setTextColor(getResources().getColor(R.color.color_blue_0888ff));
        rightTopBtn.setPadding(ScreenUtil.dip2px(10), 0, ScreenUtil.dip2px(10), 0);
    }

    /**
     * ***************************************** ?????? **************************************
     */
    private void login() {
        DialogMaker.showProgressDialog(this, null, getString(R.string.logining), true, dialog -> {
            if (loginRequest != null) {
                loginRequest.abort();
                onLoginDone();
            }
        }).setCanceledOnTouchOutside(false);
        // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????token???????????????????????????
        // ?????????????????????????????????????????????????????????token?????????
        // ???????????????????????????demo???????????????????????????md5??????token???
        // ?????????????????????????????????demo????????????appkey???????????????????????????????????????????????????????????????????????????????????????token???????????????????????????
        final String account = loginAccountEdit.getEditableText().toString().toLowerCase();
        final String token = tokenFromPassword(loginPasswordEdit.getEditableText().toString());
        int subtype = 0;
        try {
            Editable editable = loginSubtypeEdit.getEditableText();
            if (editable != null && editable.length() > 0) {
                subtype = Integer.parseInt(editable.toString());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // ??????
        loginRequest = NimUIKit.login(new LoginInfo(account, token, null, subtype),
                                      new RequestCallback<LoginInfo>() {

                                          @Override
                                          public void onSuccess(LoginInfo param) {
                                              LogUtil.i(TAG, "login success");
                                              onLoginDone();
                                              DemoCache.setAccount(account);
                                              saveLoginInfo(account, token);
                                              // ???????????????????????????
                                              initNotificationConfig();
                                              // ???????????????
                                              MainActivity.start(LoginActivity.this, null);
                                              finish();
                                          }

                                          @Override
                                          public void onFailed(int code) {
                                              onLoginDone();
                                              if (code == 302 || code == 404) {
                                                  ToastHelper.showToast(LoginActivity.this,
                                                                        R.string.login_failed);
                                              } else {
                                                  ToastHelper.showToast(LoginActivity.this,
                                                                        "????????????: " + code);
                                              }
                                          }

                                          @Override
                                          public void onException(Throwable exception) {
                                              ToastHelper.showToast(LoginActivity.this,
                                                                    R.string.login_exception);
                                              onLoginDone();
                                          }
                                      });
    }

    private void initNotificationConfig() {
        // ?????????????????????
        NIMClient.toggleNotification(UserPreferences.getNotificationToggle());
        // ?????????????????????
        StatusBarNotificationConfig statusBarNotificationConfig = UserPreferences.getStatusConfig();
        if (statusBarNotificationConfig == null) {
            statusBarNotificationConfig = DemoCache.getNotificationConfig();
            UserPreferences.setStatusConfig(statusBarNotificationConfig);
        }
        // ????????????
        NIMClient.updateStatusBarNotificationConfig(statusBarNotificationConfig);
    }

    private void onLoginDone() {
        loginRequest = null;
        DialogMaker.dismissProgressDialog();
    }

    private void saveLoginInfo(final String account, final String token) {
        Preferences.saveUserAccount(account);
        Preferences.saveUserToken(token);
    }

    //DEMO????????? username ?????? NIM ???account ???md5(password) ?????? token
    //????????????????????????????????????????????????????????????????????? NIM ?????????????????????
    private String tokenFromPassword(String password) {
        String appKey = readAppKey(this);
        boolean isDemo = "45c6af3c98409b18a84451215d0bdd6e".equals(appKey) ||
                         "fe416640c8e8a72734219e1847ad2547".equals(appKey) ||
                         "a24e6c8a956a128bd50bdffe69b405ff".equals(appKey);
        return isDemo ? MD5.getStringMD5(password) : password;
    }

    private static String readAppKey(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo != null) {
                return appInfo.metaData.getString("com.netease.nim.appKey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ***************************************** ?????? **************************************
     */
    private void register() {
        if (!registerMode || !registerPanelInited) {
            return;
        }
        if (!checkRegisterContentValid()) {
            return;
        }
        if (!NetworkUtil.isNetAvailable(LoginActivity.this)) {
            ToastHelper.showToast(LoginActivity.this, R.string.network_is_not_available);
            return;
        }
        DialogMaker.showProgressDialog(this, getString(R.string.registering), false);
        // ????????????
        final String account = registerAccountEdit.getText().toString();
        final String nickName = registerNickNameEdit.getText().toString();
        final String password = registerPasswordEdit.getText().toString();
        ContactHttpClient.getInstance().register(account, nickName, password,
                                                 new ContactHttpClient.ContactHttpCallback<Void>() {

                                                     @Override
                                                     public void onSuccess(Void aVoid) {
                                                         ToastHelper.showToast(LoginActivity.this,
                                                                               R.string.register_success);
                                                         switchMode();  // ???????????????
                                                         loginAccountEdit.setText(account);
                                                         loginPasswordEdit.setText(password);
                                                         registerAccountEdit.setText("");
                                                         registerNickNameEdit.setText("");
                                                         registerPasswordEdit.setText("");
                                                         DialogMaker.dismissProgressDialog();
                                                     }

                                                     @Override
                                                     public void onFailed(int code,
                                                                          String errorMsg) {
                                                         ToastHelper.showToast(LoginActivity.this,
                                                                               getString(
                                                                                       R.string.register_failed,
                                                                                       String.valueOf(
                                                                                               code),
                                                                                       errorMsg));
                                                         DialogMaker.dismissProgressDialog();
                                                     }
                                                 });
    }

    private boolean checkRegisterContentValid() {
        if (!registerMode || !registerPanelInited) {
            return false;
        }
        // ????????????
        String account = registerAccountEdit.getText().toString().trim();
        if (account.length() <= 0 || account.length() > 20) {
            ToastHelper.showToast(this, R.string.register_account_tip);
            return false;
        }
        // ????????????
        String nick = registerNickNameEdit.getText().toString().trim();
        if (nick.length() <= 0 || nick.length() > 10) {
            ToastHelper.showToast(this, R.string.register_nick_name_tip);
            return false;
        }
        // ????????????
        String password = registerPasswordEdit.getText().toString().trim();
        if (password.length() < 6 || password.length() > 20) {
            ToastHelper.showToast(this, R.string.register_password_tip);
            return false;
        }
        return true;
    }

    /**
     * ***************************************** ??????/???????????? **************************************
     */
    private void switchMode() {
        registerMode = !registerMode;
        if (registerMode && !registerPanelInited) {
            registerAccountEdit = findView(R.id.edit_register_account);
            registerNickNameEdit = findView(R.id.edit_register_nickname);
            registerPasswordEdit = findView(R.id.edit_register_password);
            registerAccountEdit.setIconResource(R.drawable.user_account_icon);
            registerNickNameEdit.setIconResource(R.drawable.nick_name_icon);
            registerPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);
            registerAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            registerNickNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            registerPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            registerAccountEdit.addTextChangedListener(textWatcher);
            registerNickNameEdit.addTextChangedListener(textWatcher);
            registerPasswordEdit.addTextChangedListener(textWatcher);
            registerPanelInited = true;
        }
        setTitle(registerMode ? R.string.register : R.string.login);
        loginLayout.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        registerLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        switchModeBtn.setText(registerMode ? R.string.login_has_account : R.string.register);
        if (registerMode) {
            rightTopBtn.setEnabled(true);
        } else {
            boolean isEnable = loginAccountEdit.getText().length() > 0 &&
                               loginPasswordEdit.getText().length() > 0;
            rightTopBtn.setEnabled(isEnable);
        }
    }

    public TextView addRegisterRightTopBtn(UI activity, int strResId) {
        String text = activity.getResources().getString(strResId);
        TextView textView = findView(R.id.action_bar_right_clickable_textview);
        textView.setText(text);
        textView.setBackgroundResource(R.drawable.register_right_top_btn_selector);
        textView.setPadding(ScreenUtil.dip2px(10), 0, ScreenUtil.dip2px(10), 0);
        return textView;
    }

    public TextView addRegisterLeftTopBtn(UI activity, int strResId) {
        String text = activity.getResources().getString(strResId);
        TextView textView = findView(R.id.action_bar_left_clickable_textview);
        textView.setText(text);
        textView.setBackgroundResource(R.drawable.register_right_top_btn_selector);
        textView.setPadding(ScreenUtil.dip2px(10), 0, ScreenUtil.dip2px(10), 0);
        return textView;
    }

    /**
     * *********** ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? **************
     */
    private void fakeLoginTest() {
        // ??????????????????????????????????????????????????????????????????????????????
        final String account = loginAccountEdit.getEditableText().toString().toLowerCase();
        final String token = tokenFromPassword(loginPasswordEdit.getEditableText().toString());
        // ???????????????
        boolean res = NIMClient.getService(AuthService.class).openLocalCache(
                account); // SDK??????DB????????????????????????
        Log.i("test", "fake login " + (res ? "success" : "failed"));
        if (!res) {
            return;
        }
        // Demo??????????????????????????????
        DemoCache.setAccount(account);
        // ???????????????????????????
        initNotificationConfig();
        // ??????uikit
        NimUIKit.loginSuccess(account);
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????408?????????
        MainActivity.start(LoginActivity.this, null);
        // ??????15s????????????????????????????????????????????????????????????
        getHandler().postDelayed(() -> {
            loginRequest = NIMClient.getService(AuthService.class).login(
                    new LoginInfo(account, token));
            loginRequest.setCallback(new RequestCallbackWrapper() {

                @Override
                public void onResult(int code, Object result, Throwable exception) {
                    Log.i("test", "real login, code=" + code);
                    if (code == ResponseCode.RES_SUCCESS) {
                        saveLoginInfo(account, token);
                        finish();
                    }
                }
            });
        }, 15 * 1000);
    }
}
