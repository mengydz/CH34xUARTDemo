//Program by www.elecsail.com
//tip : ����Ŀ¼һ����Ҫ���������ַ�
//���뻷����Android Studio 3.5.2
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
    private EditText serialNumText;
    private EditText workStatusText;
    private EditText voltageText;
    private EditText generateCurrentText;
    private EditText loadCurrentText;
    private EditText generatePowerText;
    private EditText loadPowerText;
    private EditText generateEnergyText;
    private EditText singleWorkTimeText;
    private EditText totalWorkTimeText;
    private EditText upkeppTimeText;//ʣ�ౣ��ʱ��
    private EditText cylinderTemperatureText;//��ͷ�¶�
    private EditText motorTemperatureText;
    private EditText speedText;

    private boolean isOpen;
    private LinearLayout linerMainPage;
    private LinearLayout linerAdvancedPage;
    private LinearLayout linerSettingPage;
    private LinearLayout linerUpdatePage;
    private TextView tvBaseData;
    private TextView tvProData;
    private TextView tvSetData;
    private TextView tvUpdate;

    private TextView tvSaveWrite;
    private int retval;
    private MainActivity activity;

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
        if (!MyApp.driver.UsbFeatureSupported())// �ж�ϵͳ�Ƿ�֧��USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("��ʾ")
                    .setMessage("�����ֻ���֧��USB HOST������������ֻ����ԣ�")
                    .setPositiveButton("ȷ��",
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// ���ֳ�������Ļ��״̬
        readBuffer = new byte[512];
        isOpen = false;
        activity = this;

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

        tvSaveWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] to_send = {0x31,0x32,0x33};//toByteArray(writeText.getText().toString());
                int retval = MyApp.driver.WriteData(to_send, to_send.length);//д���ݣ���һ������Ϊ��Ҫ���͵��ֽ����飬�ڶ�������Ϊ��Ҫ���͵��ֽڳ��ȣ�����ʵ�ʷ��͵��ֽڳ���
                if (retval < 0)
                    Toast.makeText(MainActivity.this, "дʧ��!",
                            Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void onResume() {
        super.onResume();
        if (!MyApp.driver.isConnected()) {
            int retval = MyApp.driver.ResumeUsbPermission();
            if (retval == 0) {
                int _baudrate = 115200;
                byte _datebit = 8;
                byte _stopbit = 1;
                byte _parity = 0;
                byte _flowcontrol = 0;

                if (!isOpen) {
                    retval = MyApp.driver.ResumeUsbList();
                    if (retval == -1)// ResumeUsbList��������ö��CH34X�豸�Լ�������豸
                    {
                        Toast.makeText(MainActivity.this, "���豸ʧ��!",
                                Toast.LENGTH_SHORT).show();
                        MyApp.driver.CloseDevice();
                    } else if (retval == 0) {
                        if (!MyApp.driver.UartInit()) {//�Դ����豸���г�ʼ������
                            Toast.makeText(MainActivity.this, "�豸��ʼ��ʧ��!",
                                    Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "��" +
                                            "�豸ʧ��!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(MainActivity.this, "���豸�ɹ�!",
                                Toast.LENGTH_SHORT).show();
                        /******���ô���*********************************************************/
                        if (MyApp.driver.SetConfig(_baudrate, _datebit, _stopbit, _parity, _flowcontrol)) {//���ô��ڲ����ʣ�����˵���ɲ��ձ���ֲ�
                            Toast.makeText(MainActivity.this, "�������óɹ�!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "��������ʧ��!",
                                    Toast.LENGTH_SHORT).show();
                        }
                        isOpen = true;
                        new readThread().start();//�������̶߳�ȡ���ڽ��յ�����
                    } else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setIcon(R.drawable.icon);
                        builder.setTitle("δ��Ȩ��");
                        builder.setMessage("ȷ���˳���");
                        builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
//								MainFragmentActivity.this.finish();
                                System.exit(0);
                            }
                        });
                        builder.setNegativeButton("����", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub

                            }
                        });
                        builder.show();

                    }
                } else {
                    isOpen = false;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    MyApp.driver.CloseDevice();
                }
            } else if (retval == -2) {
                Toast.makeText(MainActivity.this, "��ȡȨ��ʧ��!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //�������
    private void initUI() {
        serialNumText = (EditText) findViewById(R.id.serial_num);
        workStatusText = (EditText) findViewById(R.id.����״̬);
        voltageText = (EditText) findViewById(R.id.��ѹ);
        generateCurrentText = (EditText) findViewById(R.id.�������);
        loadCurrentText = (EditText) findViewById(R.id.���繦��);
        loadPowerText = (EditText) findViewById(R.id.�õ繦��);
        generateEnergyText = (EditText) findViewById(R.id.��������);
        singleWorkTimeText = (EditText) findViewById(R.id.����ʱ��);
        totalWorkTimeText = (EditText) findViewById(R.id.�ܹ���ʱ��);
        upkeppTimeText = (EditText) findViewById(R.id.ʣ�ౣ��ʱ��);
        cylinderTemperatureText = (EditText) findViewById(R.id.��ͷ�¶�);
        motorTemperatureText = (EditText) findViewById(R.id.������¶�);
        speedText = (EditText) findViewById(R.id.ת��);

        linerMainPage = (LinearLayout) findViewById(R.id.main_page);
        linerAdvancedPage = (LinearLayout) findViewById(R.id.advanced_page);
        linerSettingPage = (LinearLayout) findViewById(R.id.setting_page);
        linerUpdatePage = (LinearLayout) findViewById(R.id.update_page);

        tvBaseData = (TextView)findViewById(R.id.tv_base_data);
        tvProData = (TextView)findViewById(R.id.tv_pro_data);
        tvSetData = (TextView)findViewById(R.id.tv_set_data);
        tvUpdate = (TextView)findViewById(R.id.tv_update_data);

        tvSaveWrite = (TextView)findViewById(R.id.д��);
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
                    //workStatusText.setText(toHexString(buffer, 5));
//                    if(buffer[0] == 0x88 && buffer[1] == 0xA1) {
                            switch (buffer[3]) {
                                case 0x01: {
                                    short _signed_value;
                                    byte[] _buffer = new byte[2];
                                    serialNumText.setText("P3500-006A");

                                    _buffer[0] = buffer[4];
                                    _buffer[1] = buffer[5];  //��ѹ
                                    _signed_value = byte2short(_buffer);
                                    voltageText.setText(String.valueOf(_signed_value));
                                    _buffer[0] = buffer[6];
                                    _buffer[1] = buffer[7];  //�������
                                    _signed_value = byte2short(_buffer);
                                    generateCurrentText.setText(String.valueOf(_signed_value));
                                    _buffer[0] = buffer[8];
                                    _buffer[1] = buffer[9];  //���
                                    _signed_value = byte2short(_buffer);
                                    loadCurrentText.setText(String.valueOf(_signed_value));
                                    _buffer[0] = buffer[10];
                                    _buffer[1] = buffer[11];    //ת��
                                    _signed_value = byte2short(_buffer);
                                    speedText.setText(String.valueOf(_signed_value));
                                    _buffer[0] = buffer[12];
                                    _buffer[1] = buffer[13];    //��ͷ�¶�
                                    _signed_value = byte2short(_buffer);
                                    cylinderTemperatureText.setText(String.valueOf(_signed_value));
                                    _buffer[0] = buffer[14];
                                    _buffer[1] = buffer[15];    //������¶�
                                    _signed_value = byte2short(_buffer);
                                    motorTemperatureText.setText(String.valueOf(_signed_value));
                                    _buffer[0] = buffer[14];
                                    _buffer[1] = buffer[15];    //�õ����
                                    _signed_value = byte2short(_buffer);
                                    loadPowerText.setText(String.valueOf(_signed_value));

                                }
                                break;
                                case 0x02: {

                                }
                                break;
                                default:
                                    break;

                            }
//                    }
                }
            }
        }
    }

    public static byte[] short2byte(short s){
        byte[] b = new byte[2];
        for(int i = 0; i < 2; i++){
            int offset = 16 - (i+1)*8; //��Ϊbyteռ4���ֽڣ�����Ҫ����ƫ����
            b[i] = (byte)((s >> offset)&0xff); //��16λ��Ϊ2��8λ���зֱ�洢
        }
        return b;
    }

    public static short byte2short(byte[] b){
        short l = 0;
        for (int i = 0; i < 2; i++) {
            l<<=8; //<<=�����ǵ� +=��һ���ģ���˼���� l = l << 8
            l |= (b[i] & 0xff); //������Ҳ��һ����  l = l | (b[i]&0xff)
        }
        return l;
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
     * ��byte[]����ת��ΪString����
     *
     * @param arg    ��Ҫת����byte[]����
     * @param length ��Ҫת�������鳤��
     * @return ת�����String����
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
     * ��Stringת��Ϊbyte[]����
     *
     * @param arg ��Ҫת����String����
     * @return ת�����byte[]����
     */
    private byte[] toByteArray(String arg) {
        if (arg != null) {
            /* 1.��ȥ��String�е�' '��Ȼ��Stringת��Ϊchar���� */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            /* ��char�����е�ֵת��һ��ʵ�ʵ�ʮ�������� */
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
                /* �� ÿ��char��ֵÿ�������һ��16�������� */
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
     * ��Stringת��Ϊbyte[]����
     *
     * @param arg ��Ҫת����String����
     * @return ת�����byte[]����
     */
    private byte[] toByteArray2(String arg) {
        if (arg != null) {
            /* 1.��ȥ��String�е�' '��Ȼ��Stringת��Ϊchar���� */
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
