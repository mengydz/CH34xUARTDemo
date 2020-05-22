//Program by www.elecsail.com
//tip : 工程目录一定不要包含中文字符
//编译环境：Android Studio 3.5.2
//Build #AI-191.8026.42.35.5977832, built on October 31, 2019
//JRE: 1.8.0_202-release-1483-b03 amd64
//JVM: OpenJDK 64-Bit Server VM by JetBrains s.r.o
//Windows 7 6.1

package cn.wch.ch34xuartdriver;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import cn.wch.ch34xuartdemo.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Log;

public class MainActivity extends Activity {

    public static final String TAG = "cn.wch.wchusbdriver";
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    public readThread handlerThread;
    protected final Object ThreadLock = new Object();
    private EditText readText;
    private EditText writeText;
    private boolean isOpen;
    private LinearLayout linerMainPage;
    private LinearLayout linerAdvancedPage;
    private LinearLayout linerSettingPage;
    private LinearLayout linerUpdatePage;
    private TextView tvBaseData;
    private TextView tvProData;
    private TextView tvSetData;
    private TextView tvUpdate;
    private int retval;
    private MainActivity activity;

    private Button writeButton, openButton, clearButton;

    public byte[] writeBuffer;
    public byte[] readBuffer;
    public int actualNumBytes;

    public int numBytes;
    public byte count;
    public int status;
    public byte writeIndex = 0;
    public byte readIndex = 0;

    public boolean isConfiged = false;
    public boolean READ_ENABLE = false;
    public SharedPreferences sharePrefSettings;
    public String act_string;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_vertical);
        MyApp.driver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION);
        initUI();
        if (!MyApp.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态
        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        isOpen = false;
        writeButton.setEnabled(false);
        activity = this;

        //打开流程主要步骤为ResumeUsbList，UartInit
        openButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                int _baudrate = 115200;
                byte _datebit = 8;
                byte _stopbit = 1;
                byte _parity = 0;
                byte _flowcontrol = 0;

                if (!isOpen) {
                    retval = MyApp.driver.ResumeUsbList();
                    if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    {
                        Toast.makeText(MainActivity.this, "打开设备失败!",
                                Toast.LENGTH_SHORT).show();
                        MyApp.driver.CloseDevice();
                    } else if (retval == 0) {
                        if (!MyApp.driver.UartInit()) {//对串口设备进行初始化操作
                            Toast.makeText(MainActivity.this, "设备初始化失败!",
                                    Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "打开" +
                                            "设备失败!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(MainActivity.this, "打开设备成功!",
                                Toast.LENGTH_SHORT).show();
                        /******配置串口*********************************************************/
                        if (MyApp.driver.SetConfig(_baudrate, _datebit, _stopbit, _parity, _flowcontrol)) {//配置串口波特率，函数说明可参照编程手册
                            Toast.makeText(MainActivity.this, "串口设置成功!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "串口设置失败!",
                                    Toast.LENGTH_SHORT).show();
                        }
                        isOpen = true;
                        openButton.setText("Close");
                        writeButton.setEnabled(true);
                        new readThread().start();//开启读线程读取串口接收的数据
                    } else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setIcon(R.drawable.icon);
                        builder.setTitle("未授权限");
                        builder.setMessage("确认退出吗？");
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
//								MainFragmentActivity.this.finish();
                                System.exit(0);
                            }
                        });
                        builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub

                            }
                        });
                        builder.show();

                    }
                } else {
                    openButton.setText("Open");
                    writeButton.setEnabled(false);
                    isOpen = false;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    MyApp.driver.CloseDevice();
                }
            }
        });

        writeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                    byte[] to_send = {0x31,0x32,0x33};//toByteArray(writeText.getText().toString());
                    int retval = MyApp.driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
                    if (retval < 0)
                        Toast.makeText(MainActivity.this, "写失败!",
                                Toast.LENGTH_SHORT).show();
            }
        });

        tvBaseData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linerAdvancedPage.setVisibility(View.GONE);
                linerSettingPage.setVisibility(View.GONE);
                linerUpdatePage.setVisibility(View.GONE);
                linerMainPage.setVisibility(View.VISIBLE);
            }
        });

        tvProData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linerMainPage.setVisibility(View.GONE);
                linerSettingPage.setVisibility(View.GONE);
                linerUpdatePage.setVisibility(View.GONE);
                linerAdvancedPage.setVisibility(View.VISIBLE);

            }
        });

        tvSetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linerMainPage.setVisibility(View.GONE);
                linerAdvancedPage.setVisibility(View.GONE);
                linerUpdatePage.setVisibility(View.GONE);
                linerSettingPage.setVisibility(View.VISIBLE);
            }
        });

        tvUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linerMainPage.setVisibility(View.GONE);
                linerAdvancedPage.setVisibility(View.GONE);
                linerSettingPage.setVisibility(View.GONE);
                linerUpdatePage.setVisibility(View.VISIBLE);
            }
        });

    }

    public void onResume() {
        super.onResume();
        if (!MyApp.driver.isConnected()) {
            int retval = MyApp.driver.ResumeUsbPermission();
            if (retval == 0) {

            } else if (retval == -2) {
                Toast.makeText(MainActivity.this, "获取权限失败!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //处理界面
    private void initUI() {
        openButton = (Button) findViewById(R.id.open_device);
        readText = (EditText) findViewById(R.id.serial_num);
        writeText = (EditText) findViewById(R.id.WriteValues);
        writeButton = (Button) findViewById(R.id.WriteButton);

        linerMainPage = (LinearLayout) findViewById(R.id.main_page);
        linerAdvancedPage = (LinearLayout) findViewById(R.id.advanced_page);
        linerSettingPage = (LinearLayout) findViewById(R.id.setting_page);
        linerUpdatePage = (LinearLayout) findViewById(R.id.update_page);

        tvBaseData = (TextView)findViewById(R.id.tv_base_data);
        tvProData = (TextView)findViewById(R.id.tv_pro_data);
        tvSetData = (TextView)findViewById(R.id.tv_set_data);
        tvUpdate = (TextView)findViewById(R.id.tv_update_data);
        return;
    }

    private class readThread extends Thread {

        public void run() {

            byte[] buffer = new byte[64];

            while (true) {

                Message msg = Message.obtain();
                if (!isOpen) {
                    break;
                }
                int length = MyApp.driver.ReadData(buffer, 64);
                if (length > 0) {
                    switch(buffer[0])
                    {
                        case 0:{
                            readText.setText("hello world");
                        }break;
                        case 1:{
                            int _value = buffer[1]*256 + buffer[2];
                            readText.setText(String.valueOf(_value));
                        }break;
                        default:break;

                    }

                }
            }
        }
    }

    private String toIntString(byte[] arg, int length) {//BytetoString
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }
    /**
     * 将byte[]数组转化为String类型
     *
     * @param arg    需要转换的byte[]数组
     * @param length 需要转换的数组长度
     * @return 转换后的String队形
     */
    private String toHexString(byte[] arg, int length) {//BytetoString
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }

    /**
     * 将String转化为byte[]数组
     *
     * @param arg 需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private byte[] toByteArray(String arg) {
        if (arg != null) {
            /* 1.先去除String中的' '，然后将String转换为char数组 */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            /* 将char数组中的值转成一个实际的十进制数组 */
            int EvenLength = (length % 2 == 0) ? length : length + 1;
            if (EvenLength != 0) {
                int[] data = new int[EvenLength];
                data[EvenLength - 1] = 0;
                for (int i = 0; i < length; i++) {
                    if (NewArray[i] >= '0' && NewArray[i] <= '9') {
                        data[i] = NewArray[i] - '0';
                    } else if (NewArray[i] >= 'a' && NewArray[i] <= 'f') {
                        data[i] = NewArray[i] - 'a' + 10;
                    } else if (NewArray[i] >= 'A' && NewArray[i] <= 'F') {
                        data[i] = NewArray[i] - 'A' + 10;
                    }
                }
                /* 将 每个char的值每两个组成一个16进制数据 */
                byte[] byteArray = new byte[EvenLength / 2];
                for (int i = 0; i < EvenLength / 2; i++) {
                    byteArray[i] = (byte) (data[i * 2] * 16 + data[i * 2 + 1]);
                }
                return byteArray;
            }
        }
        return new byte[]{};
    }

    /**
     * 将String转化为byte[]数组
     *
     * @param arg 需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private byte[] toByteArray2(String arg) {
        if (arg != null) {
            /* 1.先去除String中的' '，然后将String转换为char数组 */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            NewArray[length] = 0x0D;
            NewArray[length + 1] = 0x0A;
            length += 2;

            byte[] byteArray = new byte[length];
            for (int i = 0; i < length; i++) {
                byteArray[i] = (byte) NewArray[i];
            }
            return byteArray;

        }
        return new byte[]{};
    }
}
