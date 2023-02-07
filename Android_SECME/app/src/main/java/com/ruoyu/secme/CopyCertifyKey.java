package com.ruoyu.secme;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.Result;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.ruoyu.secme.HelperFile.BitMapUtil;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.JsonHelper;
import com.ruoyu.secme.HelperFile.QRCodeHelper;
import com.ruoyu.secme.HelperFile.RSAHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.CertifyKeyCheck;
import com.ruoyu.secme.JsonType.JsonMessage;

import java.security.PublicKey;
import java.util.Base64;

public class CopyCertifyKey extends AppCompatActivity {

    private WebSocketService mWebSocketService = null;
    private String userMd5Str = "";
    private SQLiteDatabase db = null;

    private int CHOOSE_CODE = 3; // 只在相册挑选图片的请求码
    private int COMBINE_CODE = 4; // 既可拍照获得现场图片、也可在相册挑选已有图片的请求码

    private ImageView iv_photo; // 声明一个图像视图对象

    private static final int WRITE_PERMISSION = 0x01;

    private boolean gotPublicKey = false;
    private boolean gotPrivateKey = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy_certify_key);

        startWebSocketService();
        requestWritePermission();

        //轮询client
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (mWebSocketService != null) {
                        userMd5Str = mWebSocketService.client.userMd5Str;
                        DBHelper dbHelper = new DBHelper(CopyCertifyKey.this, userMd5Str);
                        db = dbHelper.getReadableDatabase();
                        Log.i("testDebug", "WebSocket服务已联系成功，数据库已建立");
                        break;
                    }
                }
            }
        }).start();


        ScanOptions scanOption = new ScanOptions();
        //ONE_D_CODE_TYPES 条形码
        //QR_CODE 二维码
        scanOption.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOption.setPrompt("QRコードにかざしてください!");

        scanOption.setCameraId(0);//0 后置摄像头  1 前置摄像头
        scanOption.setBeepEnabled(false);// 扫到码后播放提示音
        scanOption.setBarcodeImageEnabled(false);//是否保存图片，扫描成功会截取扫描框的图形保存到手机
        scanOption.setOrientationLocked(true);//旋转锁定
//        scanOptions.setTimeout(10000);//设置超时时间

        //数据的回调
        ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Log.i("debugTest", "result.getContents() == null");
            } else {
                //扫描到的数据
                Log.i("debugTest", "Scanned: " + result.getContents());
                //验证密钥对 并且保存
                checkAndSaveKeys(result.getContents());
            }
        });

        ImageButton imgBtnScanKeys = (ImageButton) findViewById(R.id.imgBtnScanKeys);
        imgBtnScanKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeLauncher.launch(scanOption);
            }
        });

        Button btnPhotoQR = (Button) findViewById(R.id.btnPhotoQR);
        btnPhotoQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开系统相册
                Log.i("testDebug", "打开系统相册");
                selectPhoto();
            }
        });
    }


    private void selectPhoto() {
        // 创建一个内容获取动作的意图（准备跳到系统相册）
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); // 是否允许多选
        albumIntent.setType("image/*"); // 类型为图像
//        private int CHOOSE_CODE = 3; // 只在相册挑选图片的请求码
        startActivityForResult(albumIntent, CHOOSE_CODE); // 打开系统相册
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            //获取返回的图片bitmap
            if (resultCode == RESULT_OK && requestCode == CHOOSE_CODE) {
                // 从相册返回
                if (data.getData() != null) { // 从相册选择一张照片
                    Log.i("testDebug", "处理照片");
                    Uri uri = data.getData(); // 获得已选择照片的路径对象
                    Log.i("testDebug", "照片uri:" + uri);

                    //从系统表中查询指定Uri对应照片的数据
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(uri,
                            filePathColumn, null, null, null);
                    int index_data = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    try {
                        cursor.moveToFirst();
                        String imgPath = cursor.getString(index_data);
                        Log.i("testDebug", "imgPath:" + imgPath);

                        Bitmap bitmap = BitMapUtil.compressPicture(imgPath);

                        Result result = new QRCodeHelper().getZxingResult(bitmap);
                        checkAndSaveKeys(formatKeys(result.getText()));
                    } catch (Exception e) {
                        Toast.makeText(this, "QRコードはありません!", Toast.LENGTH_SHORT).show();
                        Log.i("testDebug", "解码失败,图片中未识别到二维码。");
                    }
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_PERMISSION) {
            if (requestCode == WRITE_PERMISSION) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d(LOG_TAG, "Write Permission Failed");
                    Toast.makeText(this, "请求授权相机权限", Toast.LENGTH_SHORT).show();
//                    finish();
                }
            }
        }
    }

    private void requestWritePermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, WRITE_PERMISSION);
        }
    }
    public String formatKeys(String input) {
        return input.replace("\\", "");
    }

    private void checkAndSaveKeys(String inputStr) {

        ImageView imgPublicKeyState = (ImageView) findViewById(R.id.imgPublicKeyState);
        ImageView imgPraviteKeyState = (ImageView) findViewById(R.id.imgPraviteKeyState);
        int errorImgInt = R.drawable.authkey_error2;
        int successaImgInt = R.drawable.authkey_success2;

        //识别为公钥或者私钥
        if (RSAHelper.isPrivateKey(inputStr)) {
            Log.i("debugTest", "检测到私钥");

            DBHelper.saveCertifyPrivateKey(db, inputStr);
            mWebSocketService.client.setCertifyPrivateKey(inputStr);
            //变更状态表示
            gotPrivateKey = true;
            imgPraviteKeyState.setImageResource(successaImgInt);

        } else if (RSAHelper.is_IOS_PublicKey(inputStr)) {
            Log.i("debugTest", "检测到IOS公钥");
            try {
                //转换为Java公钥
                PublicKey publicKey = RSAHelper.getPublicKey_from_ios(inputStr);
                String publicKeyStr_java = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                //存储至数据库
                DBHelper.saveCertifyPublicKey(db, publicKeyStr_java);
                //变更状态表示
                gotPublicKey = true;
                imgPublicKeyState.setImageResource(successaImgInt);

            } catch (Exception e) {
                Log.i("debugTest", "IOS公钥转换Java公钥失败");
                imgPublicKeyState.setImageResource(errorImgInt);
            }
            //变更状态表示
            imgPublicKeyState.setImageResource(successaImgInt);

        } else if (RSAHelper.is_Java_PublicKey(inputStr)) {
            Log.i("debugTest", "检测到Java公钥");
            DBHelper.saveCertifyPublicKey(db, inputStr);
            //变更状态表示
            gotPublicKey = true;
            imgPublicKeyState.setImageResource(successaImgInt);
        } else {
            Log.i("debugTest", "未检测到密钥");
        }

        if (gotPublicKey && gotPrivateKey) {

            String testPublicKey = DBHelper.getCertifyPublicKey(db);
            Log.i("debugTest", "testPublicKey:" + testPublicKey);

            //MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDlF6faNHG0kLJo62GZufEPaKoyc1qRdevgHIUYb7uotcneiey44SSA5ErHRgtvREQ7drsiTSmHrbVZDXN2LBiiBjcYCRFJ/spYzK8O62ID+PxeeQ8FRLY+I00ipMqATrQf68x5o+o7R7vD0pkTyppFxW/iEHtn7MGvi//koZzMRwIDAQAB
            //MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDlF6faNHG0kLJo62GZufEPaKoyc1qRdevgHIUYb7uotcneiey44SSA5ErHRgtvREQ7drsiTSmHrbVZDXN2LBiiBjcYCRFJ/spYzK8O62ID+PxeeQ8FRLY+I00ipMqATrQf68x5o+o7R7vD0pkTyppFxW/iEHtn7MGvi//koZzMRwIDAQAB
            String testPrivateKey = DBHelper.getCertifyPrivateKey(db);

            if (RSAHelper.isKeyPair(testPublicKey, testPrivateKey)) {
                //是公钥对
                mWebSocketService.client.certifyKeyChecked = false;
                AlertDialog mDialog = new AlertDialog.Builder(CopyCertifyKey.this, R.style.myWaittingDialog)
                        .setTitle("")
                        .setMessage("鍵を検証中...")
                        .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //取消连接按钮
                                if (mWebSocketService.client != null) {
                                    mWebSocketService.client.close();
                                }
                            }
                        })
                        .create();
                sendCertifyKeyCheck(testPublicKey);
                mDialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            //检查登陆成功
                            try {
                                Thread.sleep(200);
                                if (mWebSocketService.client.certifyKeyChecked) {
                                    mDialog.dismiss();
                                    //跳转HomePage
                                    Intent intent = new Intent(CopyCertifyKey.this, HomePage.class);
                                    startActivity(intent);
                                    break;
                                }else if(mWebSocketService.client.isClosed()){
                                    //掉线
                                    Intent intent = new Intent(CopyCertifyKey.this, MainActivity.class);
                                    startActivity(intent);
                                    break;
                                }
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }
                }).start();

            } else {
                //不是公钥对 状态变更
                imgPraviteKeyState.setImageResource(errorImgInt);
                imgPublicKeyState.setImageResource(errorImgInt);
                gotPrivateKey = false;
                gotPublicKey = false;
                Toast.makeText(this, "鍵の組み合わせが違います!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void startWebSocketService() {
        //联系后台服务
        Intent bindIntent = new Intent(this, WebSocketService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //服务与活动成功绑定
            mWebSocketService = ((WebSocketService.JWebSocketClientBinder) iBinder).getService();
            Log.i("testDebug", "WebSocket服务与Application成功绑定");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //服务与活动断开
            mWebSocketService = null;
            Log.i("testDebug", "WebSocket服务与Application成功断开: ");
        }
    };

    private void sendCertifyKeyCheck(String certify_publickey) {
        JsonMessage jsonMessage = new JsonMessage();
        jsonMessage.type = "CertifyKeyCheck";
        CertifyKeyCheck certifyKeyCheck = new CertifyKeyCheck();
        certifyKeyCheck.username = mWebSocketService.client.username;
        certifyKeyCheck.certify_publickey = certify_publickey;
        jsonMessage.contents = certifyKeyCheck;
        String certifyKeyCheckStr = JsonHelper.buildJsonMessage(jsonMessage);
        try {
            String encryptJsonMessageStr = RSAHelper.encryptJsonMessage(certifyKeyCheckStr, mWebSocketService.client.server_publicKeyStr);
            mWebSocketService.client.send(encryptJsonMessageStr.getBytes());
            Log.i("testDebug", "sendCertifyKeyCheck发送");
        } catch (Exception e) {
            Log.i("testDebug", "sendCertifyKeyCheck加密失败");
        }
    }
}