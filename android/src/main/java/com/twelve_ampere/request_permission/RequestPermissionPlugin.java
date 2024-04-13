package com.twelve_ampere.request_permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Objects;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class RequestPermissionPlugin implements
        FlutterPlugin,
        MethodCallHandler,
        EventChannel.StreamHandler,
        ActivityAware {

    public static final String LOG_TAG = RequestPermissionPlugin.class.getSimpleName();

    private static final String NAMESPACE = "com.twelve_ampere.request_permission.RequestPermissionPlugin";
    private static final String METHODCHANNEL_ID = NAMESPACE + ".methods";
    private static final String EVENTCHANNEL_ID = NAMESPACE + ".events";

    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink eventSink;

    private final PluginRegistry.RequestPermissionsResultListener permissionsResultListener;
    private final PluginRegistry.ActivityResultListener activityResultListener;

    private ActivityPluginBinding activityBinding;

    /**
     * logLevel is initialized with 2, which stands for verbose
     * {@link android.util.Log#VERBOSE}
     */
    private int logLevel;

    public RequestPermissionPlugin() {
        logLevel = 2;
        Log.setLogLevel(logLevel);
        permissionsResultListener = new PluginRegistry.RequestPermissionsResultListener() {
            @Override
            public boolean onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
                Log.i(
                        LOG_TAG,
                        "RequestPermissionsResultListener"
                                + "\nrequestCode: " + requestCode
                                + "\npermissions: " + Arrays.toString(permissions)
                                + "\ngrantResults: " + Arrays.toString(grantResults)
                );

                if (null != eventSink) {
                    // Event has to be sent in JSON format, because
                    // sending an array is not supported by the MethodChannel API
                    // and triggers a runtime error.
                    eventSink.success(
                            "{\"requestCode\":" + requestCode
                                    + ", \"permissions\":" + Utils.toJSONArray(permissions)
                                    + ", \"grantResults\":" + Utils.toJSONArray(Utils.convert(grantResults))
                                    + "}"
                    );
                } else {
                    Log.e(LOG_TAG, "onRequestPermissionsResult, eventSink is null");
                }

                return true;
            }
        };

        activityResultListener = new PluginRegistry.ActivityResultListener() {
            @Override
            public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
                // resultCode is either
                // Activity.RESULT_OK = -1 or
                // Activity.RESULT_CANCELED = 0
                Log.i(
                        LOG_TAG,
                        "\n\nActivityResultListener"
                                + "\npermission: " + data.toString()
                                + "\nrequestCode: " + requestCode
                                + "\nresultCode: " + resultCode

                );
                try {
                    String permission = "";
                    switch (Objects.requireNonNull(data.getAction())) {
                        case Settings
                                .ACTION_MANAGE_OVERLAY_PERMISSION:
                            permission = Manifest.permission.SYSTEM_ALERT_WINDOW;
                            break;


                        case Settings
                                .ACTION_REQUEST_SET_AUTOFILL_SERVICE:
                            if (Utils.isOreoOrAbove())
                                permission = Manifest.permission.BIND_AUTOFILL_SERVICE;
                            break;

                        default:
                            break;
                    }

                    // We have to check whether the user granted the permission or not, because
                    // resultCode always equals Activity.RESULT_CANCELED if the user comes back into
                    // the app via the return arrows in the top left corner.
                    final int code = hasPermission(activityBinding.getActivity(), permission)
                            ? PackageManager.PERMISSION_GRANTED
                            : PackageManager.PERMISSION_DENIED;

                    Log.i(
                            LOG_TAG,
                            "\n\nActivityResultListener"
                                    + "\npermission: " + permission
                                    + "\nrequestCode: " + requestCode
                                    + "\nresultCode: " + resultCode
                                    + "\ncode: " + code

                    );

                    if (null != eventSink) {
                        // Sending this as JSON format as well for compatibility
                        // reasons with the API on the Dart side.
                        eventSink.success(
                                "{\"requestCode\":" + requestCode
                                        + ", \"permissions\":" + Utils.toJSONArray(permission)
                                        + ", \"grantResults\":" + Utils.toJSONArray(code)
                                        + "}"
                        );
                    } else {
                        Log.e(LOG_TAG, "onRequestPermissionsResult, eventSink is null");
                    }
                }catch (Exception ignored){

                }

                return true;
            }
        };
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHODCHANNEL_ID);
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENTCHANNEL_ID);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityBinding = binding;
        activityBinding.addRequestPermissionsResultListener(permissionsResultListener);
        activityBinding.addActivityResultListener(activityResultListener);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activityBinding.removeRequestPermissionsResultListener(permissionsResultListener);
        activityBinding.removeActivityResultListener(activityResultListener);
        activityBinding = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activityBinding = binding;
        activityBinding.addRequestPermissionsResultListener(permissionsResultListener);
        activityBinding.addActivityResultListener(activityResultListener);
    }

    @Override
    public void onDetachedFromActivity() {
        activityBinding.removeRequestPermissionsResultListener(permissionsResultListener);
        activityBinding.removeActivityResultListener(activityResultListener);
        activityBinding = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "setLogLevel":
                logLevel = call.argument("logLevel");
                Log.setLogLevel(logLevel);
                Log.i(LOG_TAG, "logLevel is now <" + logLevel + ">");
                result.success(null);
                break;

            case "requestPermissions":
                final String[] permissions = Utils.listToArray(call.argument("permissions"));
                final int requestCode = call.argument("requestCode");

                if (null == activityBinding) {
                    Log.e(LOG_TAG, "requestMultipleAndroidPermissions, activityBinding is null");
                    result.error(null, "requestMultipleAndroidPermissions, activityBinding is null", null);
                    break;
                }

                if (null == permissions) {
                    Log.e(LOG_TAG, "requestMultipleAndroidPermissions, permissions is null");
                    result.error(null, "requestMultipleAndroidPermissions, permissions is null", null);
                    break;
                }

                // handle all other not yet granted permissions
                if (permissions.length > 0) {
                    ActivityCompat.requestPermissions(
                            activityBinding.getActivity(),
                            permissions,
                            requestCode
                    );
                }

                result.success(null);
                break;

            case "requestPermissionBindAutofillService":
                final int requestCode2 = call.argument("requestCode");

                Log.i(LOG_TAG, "requestPermissionBindAutofillService, requestCode: " + requestCode2);

                if (null == activityBinding) {
                    Log.e(LOG_TAG, "requestPermissionBindAutofillService, activityBinding is null");
                    result.error(null, "requestPermissionBindAutofillService, activityBinding is null", null);
                    break;
                }

                requestPermissionBindAutofillService(
                        activityBinding.getActivity(),
                        requestCode2
                );

                result.success(null);
                break;

            case "requestPermissionSystemAlertWindow":
                final int requestCode3 = call.argument("requestCode");

                Log.i(LOG_TAG, "requestPermissionSystemAlertWindow, requestCode: " + requestCode3);

                if (null == activityBinding) {
                    Log.e(LOG_TAG, "requestPermissionSystemAlertWindow, activityBinding is null");
                    result.error(null, "requestPermissionSystemAlertWindow, activityBinding is null", null);
                    break;
                }

                requestPermissionSystemAlertWindow(
                        activityBinding.getActivity(),
                        requestCode3
                );

                result.success(null);
                break;

            case "hasPermission":
                final String permission = call.argument("permission");

                if (null == activityBinding) {
                    Log.e(LOG_TAG, "hasPermission, activityBinding is null");
                    result.error(null, "hasPermission, activityBinding is null", null);
                    break;
                }

                if (null == permission) {
                    Log.e(LOG_TAG, "hasPermission, permission is null");
                    result.error(null, "hasPermission, permission is null", null);
                    break;
                }

                final boolean has = hasPermission(
                        activityBinding.getActivity(),
                        permission
                );

                Log.i(LOG_TAG, "hasPermission"
                        + "\npermission: " + permission
                        + "\nhasPermission: " + has);

                result.success(has);
                break;

            case "shouldShowRequestPermissionRationale":
                final String permission2 = call.argument("permission");

                if (null == activityBinding) {
                    Log.e(LOG_TAG, "shouldShowRequestPermissionRationale, activityBinding is null");
                    result.error(null, "shouldShowRequestPermissionRationale, activityBinding is null", null);
                    break;
                }

                if (null == permission2) {
                    Log.e(LOG_TAG, "shouldShowRequestPermissionRationale, permission is null");
                    result.error(null, "shouldShowRequestPermissionRationale, permission is null", null);
                    break;
                }

                final boolean shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        activityBinding.getActivity(),
                        permission2
                );

                Log.i(LOG_TAG, "shouldShowRequestPermissionRationale"
                        + "\npermission: " + permission2
                        + "\nshouldShowRequestPermissionRationale: " + shouldShowRequestPermissionRationale);

                result.success(shouldShowRequestPermissionRationale);
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        eventSink = null;
    }


    private boolean hasPermission(Activity activity, String permission) {

        /**
         * The permission {@link android.Manifest.permission#SYSTEM_ALERT_WINDOW} gets
         * automatically granted <b>if {@link Utils#isMarshmallowOrAbove()} is false</b> and
         * the app was downloaded from the Google PlayStore.
         * <br>
         * <b>If {@link Utils#isMarshmallowOrAbove()} is true</b>, then
         * {@link Settings#canDrawOverlays(Context)} has to be evaluated
         * to see if the user granted this app the permission
         * {@link android.Manifest.permission#SYSTEM_ALERT_WINDOW}.
         * <br>
         * <br>
         * <a href="https://stackoverflow.com/a/36019034">
         * Additional explanation.
         * </a>
         */
        if (permission.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            return !Utils.isMarshmallowOrAbove() ||
                    Settings.canDrawOverlays(activity.getApplicationContext());
        }

        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission);
    }

    /**
     * Use this function to request the permission
     * {@link android.Manifest.permission#BIND_AUTOFILL_SERVICE}.
     */
    private void requestPermissionBindAutofillService(Activity activity, int requestCode) {
        if (Utils.isOreoOrAbove() && !hasPermission(activity, Manifest.permission.BIND_AUTOFILL_SERVICE)) {
            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                    Utils.getIntentDataUri(activity)
            );
            activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * Use this function to request the permission
     * {@link android.Manifest.permission#SYSTEM_ALERT_WINDOW}.
     */
    private void requestPermissionSystemAlertWindow(Activity activity, int requestCode) {
        if (Utils.isMarshmallowOrAbove() && !hasPermission(activity, Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Utils.getIntentDataUri(activity)
            );
            activity.startActivityForResult(intent, requestCode);
        }
    }

}

