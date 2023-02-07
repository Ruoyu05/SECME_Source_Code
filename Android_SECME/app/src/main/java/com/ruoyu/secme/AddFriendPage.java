package com.ruoyu.secme;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.zxing.Result;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.ruoyu.secme.HelperFile.BitMapUtil;
import com.ruoyu.secme.HelperFile.DBHelper;
import com.ruoyu.secme.HelperFile.QRCodeHelper;
import com.ruoyu.secme.HelperFile.TypeChangeHelper;
import com.ruoyu.secme.HelperFile.WebSocketService;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.MyQRCodeInfo;

public class AddFriendPage extends AppCompatActivity {
    private WebSocketService mWebSocketService = null;

    private int CHOOSE_CODE = 3; // 只在相册挑选图片的请求码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend_page);
        startWebSocketService();


        ScanOptions scanOption = new ScanOptions();
        scanOption.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOption.setPrompt("QRコードにかざしてください!");
        scanOption.setCameraId(0);//0 后置摄像头  1 前置摄像头
        scanOption.setBeepEnabled(false);// 扫到码后播放提示音
        scanOption.setBarcodeImageEnabled(false);//是否保存图片，扫描成功会截取扫描框的图形保存到手机
        scanOption.setOrientationLocked(true);//旋转锁定
        //数据的回调
        ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Log.i("debugTest", "result.getContents() == null");
            } else {
                //扫描到的数据
                readQRCode(result.getContents());
            }
        });

        ImageButton imgBtnShowMyQRCode = (ImageButton) findViewById(R.id.imgBtnShowMyQRCode);
        imgBtnShowMyQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddFriendPage.this, MyQRCode.class);

                Gson gson = new Gson();
                String host = mWebSocketService.client.host;
                String port = mWebSocketService.client.portStr;
                String user = mWebSocketService.client.username;

                String userMd5Str = mWebSocketService.client.userMd5Str;
                DBHelper dbHelper = new DBHelper(AddFriendPage.this, userMd5Str);
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String publicKeyStr = dbHelper.getCertifyPublicKey(db);
                //如果是java公钥则转换为ios公钥

                MyQRCodeInfo myQRCodeInfo = new MyQRCodeInfo(host, port, user, publicKeyStr);
                String myQRCodeStr = gson.toJson(myQRCodeInfo);
                String verifyMd5Str = host + port + user + publicKeyStr;
                String verifyMd5 = TypeChangeHelper.getMd5(verifyMd5Str);

                intent.putExtra("user", user);
                intent.putExtra("host", host);
                intent.putExtra("port", port);
                intent.putExtra("verifyMd5", verifyMd5);
                intent.putExtra("myQRCodeStr", myQRCodeStr);
                startActivity(intent);
            }
        });


        ImageButton imgBtnFromCamera = (ImageButton) findViewById(R.id.imgBtnFromCamera);
        imgBtnFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeLauncher.launch(scanOption);
            }
        });
        ImageButton imgBtnFromImage = (ImageButton) findViewById(R.id.imgBtnFromImage);
        imgBtnFromImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });

    }

    private void readQRCode(String result) {
        try {
            String userMd5Str = mWebSocketService.client.userMd5Str;
            DBHelper dbHelper = new DBHelper(AddFriendPage.this, userMd5Str);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Gson gson = new Gson();
            MyQRCodeInfo friendQRCodeInfo = gson.fromJson(result, MyQRCodeInfo.class);


            String user = friendQRCodeInfo.user;
            String host = friendQRCodeInfo.host;
            String port = friendQRCodeInfo.port;
            String publicKeyStr = friendQRCodeInfo.certifyKey;

            String verifyMd5Str = host + port + user + publicKeyStr;
            String verifyMd5 = TypeChangeHelper.getMd5(verifyMd5Str);
            Intent intent = null;

            if(dbHelper.isFriendActived(db, verifyMd5)){
                Log.i("testDebug", "好友激活。");
                intent = new Intent(AddFriendPage.this, FriendQRResult.class);
            }else {
                Log.i("testDebug", "好友未激活。");
                intent = new Intent(AddFriendPage.this, FriendQRResult.class);
            }
            intent.putExtra("user", user);
            intent.putExtra("host", host);
            intent.putExtra("port", port);
            intent.putExtra("verifyMd5", verifyMd5);
            intent.putExtra("friend_certifyPublicKey", publicKeyStr);

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "QRコードが正しくありません!", Toast.LENGTH_SHORT).show();
            Log.i("testDebug", "解码失败,未识别到正确的二维码。");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_CODE) {
            // 从相册返回
            if (data.getData() != null) { // 从相册选择一张照片
                Uri uri = data.getData(); // 获得已选择照片的路径对象
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

                    //扫描到的结果
                    readQRCode(result.getText());

                } catch (Exception e) {
                    Toast.makeText(this, "QRコードが正しくありません!", Toast.LENGTH_SHORT).show();
                    Log.i("testDebug", "解码失败,未识别到正确的二维码。");
                }
            }
        } else {
            return;
        }
    }

    private void selectPhoto() {
        // 创建一个内容获取动作的意图（准备跳到系统相册）
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); // 是否允许多选
        albumIntent.setType("image/*"); // 类型为图像
//        private int CHOOSE_CODE = 3; // 只在相册挑选图片的请求码
        startActivityForResult(albumIntent, CHOOSE_CODE); // 打开系统相册
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

}