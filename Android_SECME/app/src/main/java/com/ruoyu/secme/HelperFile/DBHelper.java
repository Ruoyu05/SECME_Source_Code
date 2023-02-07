package com.ruoyu.secme.HelperFile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ruoyu.secme.JsonType.FriendInfo;
import com.ruoyu.secme.JsonType.LastMessageItem;
import com.ruoyu.secme.ui.dashboard.DashboardFragment;
import com.ruoyu.secme.ui.home.HomeFragment;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(@Nullable Context context, String db_name) {
        super(context, db_name, null, 1);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE NAMES_TABLE(_id INTEGER,name TEXT)");
        //密钥存储表
        db.execSQL("CREATE TABLE IF NOT EXISTS CertifyStorage (id INTEGER PRIMARY KEY AUTOINCREMENT ,certify_publickey TEXT ,certify_privatekey TEXT)");
        Log.i("testDebug", "CREATE CertifyStorage");
        //好友列表
        db.execSQL("CREATE TABLE IF NOT EXISTS FriendList (id INTEGER PRIMARY KEY AUTOINCREMENT,friend_certifyPublicKey TEXT,friend_chat_publickey TEXT,my_chat_publickey TEXT,my_chat_privatekey TEXT,host TEXT,port TEXT,friendName TEXT,nickName TEXT,md5Str TEXT,my_uuid TEXT,friend_uuid TEXT,active TINYINT(1))");
        Log.i("testDebug", "CREATE FriendList");
    }

    public static void createChatList(SQLiteDatabase db, String friendUUID) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `" + friendUUID + "` (id INTEGER PRIMARY KEY AUTOINCREMENT, message TEXT, is_in TINYINT(1),in_time DATETIME,alreadyRead TINYINT(1))");
        Log.i("testDebug", "为好友聊天创建聊天数据库");
    }

    public void gotNewMessage(SQLiteDatabase db, String frienduuid, String message, Double in_time) {
        db.execSQL("INSERT INTO '" + frienduuid + "' ('message', 'is_in', 'in_time', 'alreadyRead') VALUES ('" + message + "', true, '" + in_time + "', false)");
    }

    public static boolean isFriendExist(SQLiteDatabase db, String friendMd5) {
        Cursor cursor = db.query("FriendList", null, "md5Str = '" + friendMd5 + "'", null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            return true;
        }
        return false;
    }

    @SuppressLint("Range")
    public static boolean isFriendActived(SQLiteDatabase db, String friendMd5) {
        if (isFriendExist(db, friendMd5)) {
            Cursor cursor = db.query("FriendList", null, "md5Str = '" + friendMd5 + "'", null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                int active_int = cursor.getInt(cursor.getColumnIndex("active"));
                Log.i("testDebug", "active_int：" + active_int);
                if (active_int == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    //新插入好友
    @SuppressLint("Range")
    public FriendInfo addNewFriend(SQLiteDatabase db, FriendInfo newfriendInfo) {
        FriendInfo friendInfo = new FriendInfo();

        friendInfo.id = newfriendInfo.id;
        friendInfo.friend_certifyPublicKey = newfriendInfo.friend_certifyPublicKey;
        friendInfo.friend_chat_publickey = newfriendInfo.friend_chat_publickey;
        friendInfo.my_chat_publickey = newfriendInfo.my_chat_publickey;
        friendInfo.my_chat_privatekey = newfriendInfo.my_chat_privatekey;

        friendInfo.host = newfriendInfo.host;
        friendInfo.port = newfriendInfo.port;
        friendInfo.friendName = newfriendInfo.friendName;
        friendInfo.nickName = newfriendInfo.nickName;
        friendInfo.md5Str = newfriendInfo.md5Str;
        friendInfo.my_uuid = newfriendInfo.my_uuid;
        friendInfo.friend_uuid = newfriendInfo.friend_uuid;
        friendInfo.active = newfriendInfo.active;
        //附加
        friendInfo.my_certifyPublicKey = newfriendInfo.my_certifyPublicKey;

        //检查好友状态
        if (isFriendExist(db, newfriendInfo.md5Str)) {
            //存在 获取数据
            Log.i("testDebug", "已存在好友");
            Cursor cursor = db.query("FriendList", null, "md5Str = '" + newfriendInfo.md5Str + "'", null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();

                friendInfo.id = cursor.getInt(cursor.getColumnIndex("id"));
                friendInfo.friend_certifyPublicKey = cursor.getString(cursor.getColumnIndex("friend_certifyPublicKey"));
                friendInfo.friend_chat_publickey = cursor.getString(cursor.getColumnIndex("friend_chat_publickey"));
                friendInfo.my_chat_publickey = cursor.getString(cursor.getColumnIndex("my_chat_publickey"));
                friendInfo.my_chat_privatekey = cursor.getString(cursor.getColumnIndex("my_chat_privatekey"));

                friendInfo.host = cursor.getString(cursor.getColumnIndex("host"));
                friendInfo.port = cursor.getString(cursor.getColumnIndex("port"));
                friendInfo.friendName = cursor.getString(cursor.getColumnIndex("friendName"));
                friendInfo.nickName = cursor.getString(cursor.getColumnIndex("nickName"));
                friendInfo.md5Str = cursor.getString(cursor.getColumnIndex("md5Str"));
                friendInfo.my_uuid = cursor.getString(cursor.getColumnIndex("my_uuid"));
                friendInfo.friend_uuid = cursor.getString(cursor.getColumnIndex("friend_uuid"));
                int active_int = cursor.getInt(cursor.getColumnIndex("active"));

                if (active_int == 1) {
                    friendInfo.active = true;
                } else {
                    friendInfo.active = false;
                }
                //附加
                friendInfo.my_certifyPublicKey = newfriendInfo.my_certifyPublicKey;
            }
        } else {
            //不存在
            friendInfo.friend_uuid = UUID.randomUUID().toString();
            //新建一个好友的数据库  名字 friend_uuid
            //FIXME 为好友建立一个聊天数据库
            createChatList(db, friendInfo.friend_uuid);
            friendInfo.active = false;
            try {
                KeyPair keyPair = RSAHelper.createKeyPair(2048);
                friendInfo.my_chat_privatekey = RSAHelper.getStringFromKey(keyPair.getPrivate());
                friendInfo.my_chat_publickey = RSAHelper.getStringFromKey(keyPair.getPublic());
            } catch (NoSuchAlgorithmException e) {
                Log.i("testDebug", "为好友聊天创建密钥失败");
            }
            //插入数据库
            insertNewFriend(db, friendInfo);
            Log.i("testDebug", "已插入好友至数据库");
        }
        return friendInfo;
    }

    public void insertNewFriend(SQLiteDatabase db, FriendInfo friendInfo) {
        db.execSQL("INSERT INTO `FriendList` (`friend_certifyPublicKey`, `friend_chat_publickey`, `my_chat_publickey`, `my_chat_privatekey`, `host`, `port`, `friendName`, `nickName`, `md5Str`, `my_uuid`, `friend_uuid`, `active`) VALUES ('" + friendInfo.friend_certifyPublicKey + "', '" + friendInfo.friend_chat_publickey + "', '" + friendInfo.my_chat_publickey + "', '" + friendInfo.my_chat_privatekey + "', '" + friendInfo.host + "', '" + friendInfo.port + "', '" + friendInfo.friendName + "', '" + friendInfo.nickName + "', '" + friendInfo.md5Str + "', '" + friendInfo.my_uuid + "', '" + friendInfo.friend_uuid + "', " + friendInfo.active + ")");
    }

    public static void updateActiveFriendInfo(SQLiteDatabase db, String friend_uuid, String friend_chat_publickey, String my_uuid) {
        db.execSQL("UPDATE `FriendList` SET `friend_chat_publickey` = '" + friend_chat_publickey + "', `my_uuid` = '" + my_uuid + "',`active` = 1 WHERE `FriendList`.`friend_uuid` = '" + friend_uuid + "'");
        Log.i("testDebug", "激活好友成功");
    }

    //输出好友存在的信息
    @SuppressLint("Range")
    public static FriendInfo getFriendInfo(SQLiteDatabase db, String md5Str) {
        FriendInfo friendInfo = new FriendInfo();
        Cursor cursor = db.query("FriendList", null, "md5Str = '" + md5Str + "'", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            Log.i("testDebug", "cursor count:" + cursor.getCount());
            cursor.moveToFirst();
            friendInfo.id = cursor.getInt(cursor.getColumnIndex("id"));
            friendInfo.host = cursor.getString(cursor.getColumnIndex("host"));
            friendInfo.port = cursor.getString(cursor.getColumnIndex("port"));
            friendInfo.friendName = cursor.getString(cursor.getColumnIndex("friendName"));
            friendInfo.nickName = cursor.getString(cursor.getColumnIndex("nickName"));
            friendInfo.md5Str = cursor.getString(cursor.getColumnIndex("md5Str"));
            friendInfo.friend_uuid = cursor.getString(cursor.getColumnIndex("friend_uuid"));
            int active_int = cursor.getInt(cursor.getColumnIndex("active"));
            if (active_int == 1) {
                friendInfo.active = true;
            } else {
                friendInfo.active = false;
            }
            friendInfo.friend_certifyPublicKey = cursor.getString(cursor.getColumnIndex("friend_certifyPublicKey"));
            friendInfo.friend_chat_publickey = cursor.getString(cursor.getColumnIndex("friend_chat_publickey"));
            friendInfo.my_chat_publickey = cursor.getString(cursor.getColumnIndex("my_chat_publickey"));
            friendInfo.my_chat_privatekey = cursor.getString(cursor.getColumnIndex("my_chat_privatekey"));
            friendInfo.my_uuid = cursor.getString(cursor.getColumnIndex("my_uuid"));
        }
        return friendInfo;
    }

    public static void updateFriend(SQLiteDatabase db, FriendInfo friendInfo) {
        String md5Str = friendInfo.md5Str;
        db.execSQL("UPDATE 'FriendList' SET 'friend_uuid' = '" + friendInfo.friend_uuid + "' WHERE `md5Str` = '" + md5Str + "'");
        db.execSQL("UPDATE 'FriendList' SET 'my_chat_privatekey' = '" + friendInfo.my_chat_privatekey + "' WHERE `md5Str` = '" + md5Str + "'");
        db.execSQL("UPDATE 'FriendList' SET 'my_chat_publickey' = '" + friendInfo.my_chat_publickey + "' WHERE `md5Str` = '" + md5Str + "'");
        db.execSQL("UPDATE 'FriendList' SET 'active' = true WHERE `md5Str` = '" + md5Str + "'");
        db.execSQL("UPDATE 'FriendList' SET 'friend_certifyPublicKey' = '" + friendInfo.friend_certifyPublicKey + "' WHERE `md5Str` = '" + md5Str + "'");
        db.execSQL("UPDATE 'FriendList' SET 'friend_chat_publickey' = '" + friendInfo.friend_chat_publickey + "' WHERE `md5Str` = '" + md5Str + "'");
    }

    public String getMyChatPrivateKey(SQLiteDatabase db, String friend_uuid) {
        Cursor cursor = db.query("FriendList", null, "friend_uuid = '" + friend_uuid + "'", null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                int index = cursor.getColumnIndex("my_chat_privatekey");
                String my_chat_privatekey = cursor.getString(index);
                return my_chat_privatekey;
            } else {
                Log.i("testDebug", "getFriendChatPrivateKey未查找到好友数据");
            }
        }
        return "none";
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void installValue(SQLiteDatabase db) {
        db.execSQL("INSERT INTO `CertifyStorage` (`certify_privatekey`, `certify_publickey`) VALUES ('MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEA6y6wxCP6Y+4wFJWcBoYyza5AhTBOt5OsR/oJ3KQ4DFQzdOo+eLk/s6d7LazCXqW9MuUaFm/bLJRRysU/Hhm4MQIDAQABAkBnPZhgihC1hI67wo97N7cennN5ZGLx6JQ1BpkEsFxgYFCmsTTXqgAa0eyXS1u8FNxJp8iIBw3lCjyWdltX1HM3AiEA/dn2qYIiadTP6nem3XBecQvCQd4v7aV3x0/cj6FB4ncCIQDtLEaKYT3HWr8Qh2V1Gg2LTLMf5/2Zu0WwpvAyiTD8lwIhALAxiTM+UASE4YssYXVxeRudvcdaIIoiP3DnzX8jvkchAiEAoQNJ8HALzOdihwokateBEmzDvol0tYVZzo/GycgxpYkCIBYMncunrmtrSNvje1AcX41MMXaGncTp61HnWmL5rkxc', 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAOsusMQj+mPuMBSVnAaGMs2uQIUwTreTrEf6CdykOAxUM3TqPni5P7Oney2swl6lvTLlGhZv2yyUUcrFPx4ZuDECAwEAAQ==')");
    }

    public void createCertifyKey(SQLiteDatabase db, String newPublicKey, String newPrivateKey) {
        //查询第一条信息
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话覆写
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                //不存在
                Log.i("testDebug", "不存在");
                db.execSQL("INSERT INTO `CertifyStorage` (`certify_publickey`, `certify_privatekey`) VALUES ('" + newPublicKey + "','" + newPrivateKey + "')");
            } else {
                //存在
                Log.i("testDebug", "存在");
                db.execSQL("UPDATE `CertifyStorage` SET `certify_publickey` = '" + newPublicKey + "', `certify_privatekey` = '" + newPrivateKey + "' WHERE `CertifyStorage`.`id` = 1");
            }
        } else {
            //cursor为null
            Log.i("testDebug", "error cursor is null");
        }
    }


    public static String getCertifyPublicKey(SQLiteDatabase db) {
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            if (cursor.getCount() != 0) {
//                Log.i("testDebug", "cursor.getCount():" + cursor.getCount());
                cursor.moveToFirst();
                int index = cursor.getColumnIndex("certify_publickey");
                String publicKeyStr = cursor.getString(index);
//                Log.i("testDebug", "查询到公钥:" + publicKeyStr);
                return publicKeyStr;
            }
        }
        return "None CertifyPublicKey";
    }

    public static String getCertifyPrivateKey(SQLiteDatabase db) {
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                int index = cursor.getColumnIndex("certify_privatekey");
                String privateKeyStr = cursor.getString(index);
                Log.i("testDebug", "查询到私钥:" + privateKeyStr);
                return privateKeyStr;
            }
        }
        return "None CertifyPrivateKey";
    }

    public String getCertify_PrivateKey(SQLiteDatabase db) {
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            if (cursor.getCount() != 0) {
                int index = cursor.getColumnIndex("certify_privatekey");
                cursor.moveToFirst();
                String privateKeyStr = cursor.getString(index);
                Log.i("testDebug", "查询到私钥:" + privateKeyStr);
                return privateKeyStr;
            }
        }
        return "None CertifyPrivateKey";
    }

    public String getCertify_PublicKey(SQLiteDatabase db) {
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            if (cursor.getCount() != 0) {
//                Log.i("testDebug", "cursor.getCount():" + cursor.getCount());
                cursor.moveToFirst();
                int index = cursor.getColumnIndex("certify_publickey");
                String publicKeyStr = cursor.getString(index);
//                Log.i("testDebug", "查询到公钥:" + publicKeyStr);
                return publicKeyStr;
            }
        }
        return "None CertifyPublicKey";
    }

    public static void saveCertifyPrivateKey(SQLiteDatabase db, String privateKeyStr) {
        //查询是否存在数据
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                //无数据 //执行插入
                String sql_create = "INSERT INTO `certifystorage` (`id`, `certify_publickey`, `certify_privatekey`) VALUES (1, \'none\', \'none\')";
//                Log.i("testDebug", "PrivateKey_sql_create:" + sql_create);
                db.execSQL(sql_create);
            }
            //更新数据
            String sql_update = "UPDATE `CertifyStorage` SET `certify_privatekey` = '" + privateKeyStr + "' WHERE `id` = 1";
//            Log.i("testDebug", "PrivateKey_sql_update:" + sql_update);
            db.execSQL(sql_update);
        } else {
            Log.i("testDebug", "saveCertifyPrivateKey() 查询故障");
        }
    }

    public static void saveCertifyPublicKey(SQLiteDatabase db, String publicKeyStr) {
        //查询是否存在数据
        Cursor cursor = db.query("CertifyStorage", null, "id = 1", null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                //无数据 //执行插入
                String sql_create = "INSERT INTO `certifystorage` (`id`, `certify_publickey`, `certify_privatekey`) VALUES (1, \'none\', \'none\')";
//                Log.i("testDebug", "PublicKey_sql_create:" + sql_create);
                db.execSQL(sql_create);
            }
            //更新数据
            String sql_update = "UPDATE `CertifyStorage` SET `certify_publickey` = '" + publicKeyStr + "' WHERE `id` = 1";
//            Log.i("testDebug", "PrivateKey_sql_update:" + sql_update);
            db.execSQL(sql_update);
        } else {
            Log.i("testDebug", "saveCertifyPublicKey() 查询故障");
        }
    }

    @SuppressLint("Range")
    public static ArrayList<FriendItem> getFriendItem(SQLiteDatabase db) {
        ArrayList<FriendItem> friendList = new ArrayList<FriendItem>();
        //从数据库查询数据并输出
        Cursor cursor = db.query("FriendList", null, null, null, null, null, null);
        //存在的话输出
        if (cursor != null) {
            Log.i("testDebug", "cursor count:" + cursor.getCount());
            while (cursor.moveToNext()) {
                FriendItem item = new FriendItem();
                item.host = cursor.getString(cursor.getColumnIndex("host"));
                item.port = cursor.getString(cursor.getColumnIndex("port"));
                item.name = cursor.getString(cursor.getColumnIndex("friendName"));
                item.nickname = cursor.getString(cursor.getColumnIndex("nickName"));
                item.md5Str = cursor.getString(cursor.getColumnIndex("md5Str"));
                item.friend_uuid = cursor.getString(cursor.getColumnIndex("friend_uuid"));
                int active_int = cursor.getInt(cursor.getColumnIndex("active"));
                if (active_int == 1) {
                    item.active = true;
                } else {
                    item.active = false;
                }
                friendList.add(item);
            }
        } else {
            Log.i("testDebug", "数据库未查询到FriendList数据");
        }
        return friendList;
    }

    @SuppressLint("Range")
    public static ArrayList<ChatMessageTable> getChatMessages(SQLiteDatabase db, String friend_uuid) {
        ArrayList<ChatMessageTable> chatMessageTables = new ArrayList<ChatMessageTable>();

        String sql = "SELECT * FROM `" + friend_uuid + "` WHERE 1";
        Cursor cursor_all_message = db.rawQuery(sql, null);

        if (cursor_all_message != null) {
            while (cursor_all_message.moveToNext()) {
                ChatMessageTable chatMessageTable = new ChatMessageTable();
                chatMessageTable.message = cursor_all_message.getString(cursor_all_message.getColumnIndex("message"));
                int isin_int = cursor_all_message.getInt(cursor_all_message.getColumnIndex("is_in"));
                if (isin_int == 1) {
                    chatMessageTable.isIn = true;
                } else {
                    chatMessageTable.isIn = false;
                }
                chatMessageTables.add(chatMessageTable);
            }
        }
        return chatMessageTables;
    }

    @SuppressLint("Range")
    public static ArrayList<LastMessageItem> getLastMessageItem(SQLiteDatabase db) {
        ArrayList<LastMessageItem> lastMessageItems = new ArrayList<LastMessageItem>();

        LastMessageItem lastMessageItem = new LastMessageItem();
        String[] columns = {"md5Str", "friend_uuid", "friendName", "nickName"};
        //查找所有的好友
        Cursor cursor_all_friend = db.query("FriendList", columns, null, null, null, null, null);


        if (cursor_all_friend != null) {
            while (cursor_all_friend.moveToNext()) {

                lastMessageItem.md5Str = cursor_all_friend.getString(cursor_all_friend.getColumnIndex("md5Str"));
                lastMessageItem.friendName = cursor_all_friend.getString(cursor_all_friend.getColumnIndex("friendName"));
                lastMessageItem.nickName = cursor_all_friend.getString(cursor_all_friend.getColumnIndex("nickName"));
                lastMessageItem.friend_uuid = cursor_all_friend.getString(cursor_all_friend.getColumnIndex("friend_uuid"));

                if (lastMessageItem.friend_uuid.equals("null")) {
                    Log.i("testDebug", "好友聊天数据库尚未建立" + lastMessageItem.friend_uuid);
                    continue;
                } else {
                    createChatList(db, lastMessageItem.friend_uuid);
                }

                //FIXME 如果新加的好友 还没有建立数据库 则会报错
                String sql = "SELECT * FROM `" + lastMessageItem.friend_uuid + "` WHERE 1 ORDER by id DESC LIMIT 1";
                Log.i("testDebug", "sql:" + sql);

                //修复 在生成uuid的同时建立数据库


                Cursor cursor_lastMessage = db.rawQuery(sql, null);
                if (cursor_lastMessage != null) {
                    Log.i("testDebug", "count:" + cursor_lastMessage.getCount());
                    if (cursor_lastMessage.getCount() != 0) {
                        Log.i("testDebug", "有数据");
                        cursor_lastMessage.moveToFirst();
                        lastMessageItem.lastMessage = cursor_lastMessage.getString(cursor_lastMessage.getColumnIndex("message"));
                        lastMessageItems.add(lastMessageItem);
                    }
                }
            }
        }
        return lastMessageItems;
    }

    public static void saveChatMessage(SQLiteDatabase db, String friend_uuid, String message) {
        db.execSQL("INSERT INTO `" + friend_uuid + "` (`message`, `is_in`, `in_time`, `alreadyRead`) VALUES ('" + message + "',false,null,true)");
    }
}
