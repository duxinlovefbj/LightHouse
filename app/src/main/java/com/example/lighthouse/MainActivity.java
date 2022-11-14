package com.example.lighthouse;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.qweather.sdk.view.HeConfig;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String[] ACTIVITY_RECOGNITION_PERMISSION = {Manifest.permission.ACTIVITY_RECOGNITION};
    private TextView mStepText;
    private SensorManager mSensorManager;
    private MySensorEventListener mListener;
    private int mStepDetector = 0;  // 自应用运行以来STEP_DETECTOR检测到的步数
    private int mStepCounter = 0;   // 自系统开机以来STEP_COUNTER检测到的步数
    private CardView healthy;

    public static Intent buildIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HeConfig.init("PublicId", "PrivateKey");
        HeConfig.switchToDevService();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide(); //隐藏标题栏
        }

        mStepText = (TextView) findViewById(R.id.steps);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mListener = new MySensorEventListener();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 检查该权限是否已经获取
            int get = ContextCompat.checkSelfPermission(this, ACTIVITY_RECOGNITION_PERMISSION[0]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (get != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求自动开启权限
                ActivityCompat.requestPermissions(this, ACTIVITY_RECOGNITION_PERMISSION, 321);
            }
        }

    }


    /*打开app*/
    @TargetApi(Build.VERSION_CODES.DONUT)
    private void doStartApplicationWithPackageName(String packagename) {

        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);

        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = getPackageManager()
                .queryIntentActivities(resolveIntent, 0);

        ResolveInfo resolveinfo = resolveinfoList.iterator().next();
        if (resolveinfo != null) {
            // packagename = 参数packname
            String packageName = resolveinfo.activityInfo.packageName;
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
            String className = resolveinfo.activityInfo.name;
            // LAUNCHER Intent
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            // 设置ComponentName参数1:packagename参数2:MainActivity路径
            ComponentName cn = new ComponentName(packageName, className);

            intent.setComponent(cn);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                    SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //提示用户手动开启权限
                    new AlertDialog.Builder(this)
                            .setTitle("健康运动权限")
                            .setMessage("健康运动权限不可用")
                            .setPositiveButton("立即开启", (dialog12, which) -> {
                                // 跳转到应用设置界面
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, 123);
                            })
                            .setNegativeButton("取消", (dialog1, which) -> {
                                Toast.makeText(getApplicationContext(), "没有获得权限，应用无法运行！", Toast.LENGTH_SHORT).show();
                                finish();
                            }).setCancelable(false).show();
                }
            }
        }
    }

    private class MySensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                System.out.println("@@@:" + event.sensor.getType() + "--" + Sensor.TYPE_STEP_DETECTOR + "--" + Sensor.TYPE_STEP_COUNTER);

                if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                    if (event.values[0] == 1.0f) {
                        mStepDetector++;
                    }
                } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                    mStepCounter = (int) event.values[0];
                }

                String desc = String.format(Locale.CHINESE, "%d步", mStepDetector);
                mStepText.setText(desc);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }


}
