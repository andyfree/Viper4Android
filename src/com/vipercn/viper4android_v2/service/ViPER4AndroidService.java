package com.vipercn.viper4android_v2.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.vipercn.viper4android_v2.activity.IrsUtils;
import com.vipercn.viper4android_v2.activity.StaticEnvironment;
import com.vipercn.viper4android_v2.activity.V4AJniInterface;
import com.vipercn.viper4android_v2.activity.ViPER4Android;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class ViPER4AndroidService extends Service {

    private class ResourceMutex {
        private Semaphore mSignal = new Semaphore(1);

        public boolean acquire() {
            try {
                mSignal.acquire();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        public void release() {
            mSignal.release();
        }
    }

    private class V4ADSPModule {
        private final UUID EFFECT_TYPE_NULL =
                UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
        public AudioEffect mInstance;

        public V4ADSPModule(int mAudioSession) {
            try {
                mInstance = AudioEffect.class.getConstructor(
                        UUID.class, UUID.class, Integer.TYPE, Integer.TYPE).newInstance(
                        EFFECT_TYPE_NULL, ViPER4AndroidService.ID_V4A_GENERAL_FX, 0, mAudioSession);
                Log.i("ViPER4Android", "Creating viper4android module, " + ViPER4AndroidService.ID_V4A_GENERAL_FX.toString());
            } catch (Exception e) {
                Log.i("ViPER4Android", e.getMessage());
                mInstance = null;
            }
        }

        public void release() {
            Log.i("ViPER4Android", "Free viper4android module.");
            if (mInstance != null)
                mInstance.release();
            mInstance = null;
        }

        private byte[] intToByteArray(int value) {
            ByteBuffer converter = ByteBuffer.allocate(4);
            converter.order(ByteOrder.nativeOrder());
            converter.putInt(value);
            return converter.array();
        }

        private int byteArrayToInt(byte[] valueBuf) {
            ByteBuffer converter = ByteBuffer.wrap(valueBuf);
            converter.order(ByteOrder.nativeOrder());
            return converter.getInt(0);
        }

        private byte[] concatArrays(byte[]... arrays) {
            int len = 0;
            for (byte[] a : arrays) {
                len += a.length;
            }
            byte[] b = new byte[len];
            int offs = 0;
            for (byte[] a : arrays) {
                System.arraycopy(a, 0, b, offs, a.length);
                offs += a.length;
            }
            return b;
        }

        public void setParameter_px4_vx4x1(int param, int valueL) {
            try {
                byte[] p = intToByteArray(param);
                byte[] v = intToByteArray(valueL);
                setParameter_Native(p, v);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_px4_vx4x1: " + e.getMessage());
            }
        }

        public void setParameter_px4_vx4x2(int param, int valueL, int valueH) {
            try {
                byte[] p = intToByteArray(param);
                byte[] vL = intToByteArray(valueL);
                byte[] vH = intToByteArray(valueH);
                byte[] v = concatArrays(vL, vH);
                setParameter_Native(p, v);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_px4_vx4x2: " + e.getMessage());
            }
        }

        public void setParameter_px4_vx4x3(int param, int valueL, int valueH, int valueE) {
            try {
                byte[] p = intToByteArray(param);
                byte[] vL = intToByteArray(valueL);
                byte[] vH = intToByteArray(valueH);
                byte[] vE = intToByteArray(valueE);
                byte[] v = concatArrays(vL, vH, vE);
                setParameter_Native(p, v);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_px4_vx4x3: " + e.getMessage());
            }
        }

        @SuppressWarnings("unused")  /* For future use */
        public void setParameter_px4_vx4x4(int param, int valueL, int valueH, int valueE, int valueR) {
            try {
                byte[] p = intToByteArray(param);
                byte[] vL = intToByteArray(valueL);
                byte[] vH = intToByteArray(valueH);
                byte[] vE = intToByteArray(valueE);
                byte[] vR = intToByteArray(valueR);
                byte[] v = concatArrays(vL, vH, vE, vR);
                setParameter_Native(p, v);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_px4_vx4x4: " + e.getMessage());
            }
        }

        public void setParameter_px4_vx1x256(int param, int dataLength, byte[] byteData) {
            try {
                byte[] p = intToByteArray(param);
                byte[] vL = intToByteArray(dataLength);
                byte[] v = concatArrays(vL, byteData);
                if (v.length < 256) {
                    int zeroPad = 256 - v.length;
                    byte[] zeroArray = new byte[zeroPad];
                    v = concatArrays(v, zeroArray);
                }
                setParameter_Native(p, v);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_px4_vx1x256: " + e.getMessage());
            }
        }

        public void setParameter_px4_vx2x8192(int param, int valueL, int dataLength, byte[] byteData) {
            try {
                byte[] p = intToByteArray(param);
                byte[] vL = intToByteArray(valueL);
                byte[] vH = intToByteArray(dataLength);
                byte[] v = concatArrays(vL, vH, byteData);
                if (v.length < 8192) {
                    int zeroPad = 8192 - v.length;
                    byte[] zeroArray = new byte[zeroPad];
                    v = concatArrays(v, zeroArray);
                }
                setParameter_Native(p, v);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_px4_vx2x8192: " + e.getMessage());
            }
        }

        @SuppressWarnings("unused")
        public void setParameter_px4_vxString(int param, String szData) {
            int stringLen = szData.length();
            byte[] stringBytes = szData.getBytes(Charset.forName("US-ASCII"));
            setParameter_px4_vx1x256(param, stringLen, stringBytes);
        }

        public void setParameter_Native(byte[] parameter, byte[] value) {
            if (mInstance == null) return;
            try {
                Method setParameter = AudioEffect.class.getMethod("setParameter", byte[].class, byte[].class);
                setParameter.invoke(mInstance, parameter, value);
            } catch (Exception e) {
                Log.i("ViPER4Android", "setParameter_Native: " + e.getMessage());
            }
        }

        public int getParameter_px4_vx4x1(int param) {
            try {
                byte[] p = intToByteArray(param);
                byte[] v = new byte[4];
                getParameter_Native(p, v);
                int val = byteArrayToInt(v, 0);
                return val;
            } catch (Exception e) {
                Log.i("ViPER4Android", "getParameter_px4_vx4x1: " + e.getMessage());
                return -1;
            }
        }

        public void getParameter_Native(byte[] parameter, byte[] value) {
            if (mInstance == null) return;
            try {
                Method getParameter = AudioEffect.class.getMethod("getParameter", byte[].class, byte[].class);
                getParameter.invoke(mInstance, parameter, value);
            } catch (Exception e) {
                Log.i("ViPER4Android", "getParameter_Native: " + e.getMessage());
            }
        }

        private void ProceedIRBuffer_Speaker(String mConvIRFile, int mChannels, int mFrames, int mBytes) {
            // 1. Tell driver to prepare kernel buffer
            Random rndMachine = new Random(System.currentTimeMillis());
            int mKernelBufferID = rndMachine.nextInt();
            setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, mKernelBufferID, mChannels, 0);

            // 2. Read entire ir data and get hash code
            byte[] mKernelData = V4AJniInterface.readImpulseResponseToArray(mConvIRFile);
            if (mKernelData == null) {
                // Read failed
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            if (mKernelData.length <= 0) {
                // Empty ir file
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            int[] mHashCode = V4AJniInterface.getHashImpulseResponseArray(mKernelData);
            if (mHashCode == null) {
                // Wrong with hash
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            if (mHashCode.length != 2) {
                // Wrong with hash
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            if (mHashCode[0] == 0) {
                // Wrong with hash
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            int nHashCode = mHashCode[1];

            Log.i("ViPER4Android", "[Kernel] Channels = " + mChannels + ", Frames = " + mFrames + ", Bytes = " + mKernelData.length + ", Hash = " + nHashCode);

            // 3. Split kernel data and send to driver
            int mBlockSize = 8184;  /* 8192(packet size) - sizeof(int) - sizeof(int), 8184 bytes = 2046 float samples = 1023 stereo frames */
            int mRestBytes = mKernelData.length, mSendOffset = 0;
            while (mRestBytes > 0) {
                int mMinBlockSize = Math.min(mBlockSize, mRestBytes);
                byte[] mSendData = new byte[mMinBlockSize];
                System.arraycopy(mKernelData, mSendOffset, mSendData, 0, mMinBlockSize);
                mSendOffset += mMinBlockSize;
                mRestBytes -= mMinBlockSize;
                // Send to driver
                int mFramesCount = mMinBlockSize / 4;  /* sizeof(float) = 4 */
                setParameter_px4_vx2x8192(ViPER4AndroidService.PARAM_SPKFX_CONV_SETBUFFER, mKernelBufferID, mFramesCount, mSendData);
            }

            // 4. Tell driver to commit kernel buffer
            setParameter_px4_vx4x2(ViPER4AndroidService.PARAM_SPKFX_CONV_COMMITBUFFER, mKernelBufferID, nHashCode);
        }

        private void ProceedIRBuffer_Headphone(String mConvIRFile, int mChannels, int mFrames, int mBytes) {
            // 1. Tell driver to prepare kernel buffer
            Random rndMachine = new Random(System.currentTimeMillis());
            int mKernelBufferID = rndMachine.nextInt();
            setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, mKernelBufferID, mChannels, 0);

            // 2. Read entire ir data and get hash code
            byte[] mKernelData = V4AJniInterface.readImpulseResponseToArray(mConvIRFile);
            if (mKernelData == null) {
                // Read failed
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            if (mKernelData.length <= 0) {
                // Empty ir file
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            int[] mHashCode = V4AJniInterface.getHashImpulseResponseArray(mKernelData);
            if (mHashCode == null) {
                // Wrong with hash
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            if (mHashCode.length != 2) {
                // Wrong with hash
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            if (mHashCode[0] == 0) {
                // Wrong with hash
                setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            int hashCode = mHashCode[1];

            Log.i("ViPER4Android", "[Kernel] Channels = " + mChannels + ", Frames = " + mFrames + ", Bytes = " + mKernelData.length + ", Hash = " + hashCode);

            // 3. Split kernel data and send to driver
            int nBlockSize = 8184;  /* 8192(packet size) - sizeof(int) - sizeof(int), 8184 bytes = 2046 float samples = 1023 stereo frames */
            int mRestBytes = mKernelData.length, mSendOffset = 0, mPacketIndex = 0;
            while (mRestBytes > 0) {
                int mMinBlockSize = Math.min(nBlockSize, mRestBytes);
                byte[] mSendData = new byte[mMinBlockSize];
                System.arraycopy(mKernelData, mSendOffset, mSendData, 0, mMinBlockSize);
                mSendOffset += mMinBlockSize;
                mRestBytes -= mMinBlockSize;
                Log.i("ViPER4Android", "Setting kernel buffer, index = " + mPacketIndex + ", length = " + mMinBlockSize);
                mPacketIndex++;
                // Send to driver
                int mFramesCount = mMinBlockSize / 4;  /* sizeof(float) = 4 */
                setParameter_px4_vx2x8192(ViPER4AndroidService.PARAM_HPFX_CONV_SETBUFFER, mKernelBufferID, mFramesCount, mSendData);
            }

            // 4. Tell driver to commit kernel buffer
            setParameter_px4_vx4x2(ViPER4AndroidService.PARAM_HPFX_CONV_COMMITBUFFER, mKernelBufferID, hashCode);
        }

        public void SetConvIRFile(String mConvIRFile, boolean mSpeakerParam) {
            /* Commit irs when called here
             * driver holds a current irs hash
             * so we just ignore the same irs commited more times
             */

            if (mConvIRFile == null) {
                Log.i("ViPER4Android", "Clear convolver kernel");
                // Clear convolver ir file
                if (mSpeakerParam)
                    setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                else
                    setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
            } else {
                Log.i("ViPER4Android", "Convolver kernel = " + mConvIRFile);

                // Set convolver ir file
                if (mConvIRFile.equals("")) {
                    Log.i("ViPER4Android", "Clear convolver kernel");
                    // Clear convolver ir file
                    if (mSpeakerParam)
                        setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                    else
                        setParameter_px4_vx4x3(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                } else {
                    int nCommand = ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER;
                    if (mSpeakerParam)
                        nCommand = ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER;

                    // Get ir file info
                    int[] iaIRInfo = V4AJniInterface.getImpulseResponseInfoArray(mConvIRFile);
                    if (iaIRInfo == null) setParameter_px4_vx4x3(nCommand, 0, 0, 1);
                    else {
                        if (iaIRInfo.length != 4) setParameter_px4_vx4x3(nCommand, 0, 0, 1);
                        else {
                            if (iaIRInfo[0] == 0) setParameter_px4_vx4x3(nCommand, 0, 0, 1);
                            else {
                                /* Proceed buffer */
                                if (mSpeakerParam)
                                    ProceedIRBuffer_Speaker(mConvIRFile, iaIRInfo[1], iaIRInfo[2], iaIRInfo[3]);
                                else
                                    ProceedIRBuffer_Headphone(mConvIRFile, iaIRInfo[1], iaIRInfo[2], iaIRInfo[3]);
                            }
                        }
                    }
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public ViPER4AndroidService getService() {
            return ViPER4AndroidService.this;
        }
    }

    public static final UUID ID_V4A_GENERAL_FX =
            UUID.fromString("41d3c987-e6cf-11e3-a88a-11aba5d5c51b");

    public static final int DEVICE_GLOBAL_OUTPUT_MIXER = 0;

    /* ViPER4Android Driver Status */
    public static final int PARAM_GET_DRIVER_VERSION = 32769;
    public static final int PARAM_GET_NEONENABLED = 32770;
    public static final int PARAM_GET_ENABLED = 32771;
    public static final int PARAM_GET_CONFIGURE = 32772;
    public static final int PARAM_GET_STREAMING = 32773;
    public static final int PARAM_GET_EFFECT_TYPE = 32774;
    public static final int PARAM_GET_SAMPLINGRATE = 32775;
    public static final int PARAM_GET_CHANNELS = 32776;
    public static final int PARAM_GET_CONVUSABLE = 32777;
    /**
     * ***************************
     */

    /* ViPER4Android Driver Status Control */
    public static final int PARAM_SET_COMM_STATUS = 36865;
    public static final int PARAM_SET_UPDATE_STATUS = 36866;
    public static final int PARAM_SET_RESET_STATUS = 36867;
    /**
     * ***********************************
     */

    /* ViPER4Android FX Types */
    public static final int V4A_FX_TYPE_NONE = 0;
    public static final int V4A_FX_TYPE_HEADPHONE = 1;
    public static final int V4A_FX_TYPE_SPEAKER = 2;
    /**
     * **********************
     */

    /* ViPER4Android General FX Parameters */
    public static final int PARAM_FX_TYPE_SWITCH = 65537;
    public static final int PARAM_HPFX_CONV_PROCESS_ENABLED = 65538;
    public static final int PARAM_HPFX_CONV_PREPAREBUFFER = 65539;
    public static final int PARAM_HPFX_CONV_SETBUFFER = 65540;
    public static final int PARAM_HPFX_CONV_COMMITBUFFER = 65541;
    public static final int PARAM_HPFX_VHE_PROCESS_ENABLED = 65542;
    public static final int PARAM_HPFX_VHE_EFFECT_LEVEL = 65543;
    public static final int PARAM_HPFX_FIREQ_PROCESS_ENABLED = 65544;
    public static final int PARAM_HPFX_FIREQ_BANDLEVEL = 65545;
    public static final int PARAM_HPFX_COLM_PROCESS_ENABLED = 65546;
    public static final int PARAM_HPFX_COLM_WIDENING = 65547;
    public static final int PARAM_HPFX_COLM_MIDIMAGE = 65548;
    public static final int PARAM_HPFX_COLM_DEPTH = 65549;
    public static final int PARAM_HPFX_DIFFSURR_PROCESS_ENABLED = 65550;
    public static final int PARAM_HPFX_DIFFSURR_DELAYTIME = 65551;
    public static final int PARAM_HPFX_REVB_PROCESS_ENABLED = 65552;
    public static final int PARAM_HPFX_REVB_ROOMSIZE = 65553;
    public static final int PARAM_HPFX_REVB_WIDTH = 65554;
    public static final int PARAM_HPFX_REVB_DAMP = 65555;
    public static final int PARAM_HPFX_REVB_WET = 65556;
    public static final int PARAM_HPFX_REVB_DRY = 65557;
    public static final int PARAM_HPFX_AGC_PROCESS_ENABLED = 65558;
    public static final int PARAM_HPFX_AGC_RATIO = 65559;
    public static final int PARAM_HPFX_AGC_VOLUME = 65560;
    public static final int PARAM_HPFX_AGC_MAXSCALER = 65561;
    public static final int PARAM_HPFX_DYNSYS_PROCESS_ENABLED = 65562;
    public static final int PARAM_HPFX_DYNSYS_ENABLETUBE = 65563;
    public static final int PARAM_HPFX_DYNSYS_XCOEFFS = 65564;
    public static final int PARAM_HPFX_DYNSYS_YCOEFFS = 65565;
    public static final int PARAM_HPFX_DYNSYS_SIDEGAIN = 65566;
    public static final int PARAM_HPFX_DYNSYS_BASSGAIN = 65567;
    public static final int PARAM_HPFX_VIPERBASS_PROCESS_ENABLED = 65568;
    public static final int PARAM_HPFX_VIPERBASS_MODE = 65569;
    public static final int PARAM_HPFX_VIPERBASS_SPEAKER = 65570;
    public static final int PARAM_HPFX_VIPERBASS_BASSGAIN = 65571;
    public static final int PARAM_HPFX_VIPERCLARITY_PROCESS_ENABLED = 65572;
    public static final int PARAM_HPFX_VIPERCLARITY_MODE = 65573;
    public static final int PARAM_HPFX_VIPERCLARITY_CLARITY = 65574;
    public static final int PARAM_HPFX_CURE_PROCESS_ENABLED = 65575;
    public static final int PARAM_HPFX_CURE_CROSSFEED = 65576;
    public static final int PARAM_HPFX_OUTPUT_VOLUME = 65577;
    public static final int PARAM_HPFX_OUTPUT_PAN = 65578;
    public static final int PARAM_HPFX_LIMITER_THRESHOLD = 65579;
    public static final int PARAM_SPKFX_CONV_PROCESS_ENABLED = 65580;
    public static final int PARAM_SPKFX_CONV_PREPAREBUFFER = 65581;
    public static final int PARAM_SPKFX_CONV_SETBUFFER = 65582;
    public static final int PARAM_SPKFX_CONV_COMMITBUFFER = 65583;
    public static final int PARAM_SPKFX_FIREQ_PROCESS_ENABLED = 65584;
    public static final int PARAM_SPKFX_FIREQ_BANDLEVEL = 65585;
    public static final int PARAM_SPKFX_REVB_PROCESS_ENABLED = 65586;
    public static final int PARAM_SPKFX_REVB_ROOMSIZE = 65587;
    public static final int PARAM_SPKFX_REVB_WIDTH = 65588;
    public static final int PARAM_SPKFX_REVB_DAMP = 65589;
    public static final int PARAM_SPKFX_REVB_WET = 65590;
    public static final int PARAM_SPKFX_REVB_DRY = 65591;
    public static final int PARAM_SPKFX_AGC_PROCESS_ENABLED = 65592;
    public static final int PARAM_SPKFX_AGC_RATIO = 65593;
    public static final int PARAM_SPKFX_AGC_VOLUME = 65594;
    public static final int PARAM_SPKFX_AGC_MAXSCALER = 65595;
    public static final int PARAM_SPKFX_OUTPUT_VOLUME = 65596;
    public static final int PARAM_SPKFX_LIMITER_THRESHOLD = 65597;
    /**
     * ***********************************
     */

    private final LocalBinder mBinder = new LocalBinder();
    protected boolean mUseHeadset;
    protected boolean mUseBluetooth;
    protected boolean mUseUSB;
    protected String mPreviousMode = "none";
    private float[] mOverriddenEqualizerLevels;

    private boolean mServicePrepared;
    private boolean mDriverIsReady;
    private V4ADSPModule mGeneralFX;
    private SparseArray<V4ADSPModule> mGeneralFXList = new SparseArray<V4ADSPModule>();
    private ResourceMutex mV4AMutex = new ResourceMutex();

    private final Timer tmDrvStatusCommTimer = new Timer();
    private static Handler hDrvStatusCommTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg == null) {
                super.handleMessage(msg);
                return;
            }

            if (msg.what == 1) {
                try {
                    if (msg.obj == null) {
                        super.handleMessage(msg);
                        return;
                    }
                    V4ADSPModule v4a = (V4ADSPModule) (msg.obj);
                    if (v4a != null) {
                        if (v4a.mInstance != null)
                            v4a.setParameter_px4_vx4x1(PARAM_SET_COMM_STATUS, 1);
                    }
                    super.handleMessage(msg);
                } catch (Exception e) {
                    super.handleMessage(msg);
                }
            }
        }
    };
    private TimerTask ttDrvStatusCommTimer = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            message.obj = (V4ADSPModule) mGeneralFX;
            hDrvStatusCommTimerHandler.sendMessage(message);
        }
    };

    private boolean mediaMounted;
    private final Timer mediaStatusTimer = new Timer();
    private TimerTask mediaTimerTask = new TimerTask() {
        @Override
        public void run() {
            /* This is the *best* way to solve the fragmentation of android system */
            /* Use a media mounted broadcast is not safe */

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                mediaMounted = false;
            else {
                if (!mediaMounted) {
                    Log.i("ViPER4Android", "Media mounted, now updating parameters");
                    mediaMounted = true;
                    updateSystem(false);
                }
            }
        }
    };

    private final BroadcastReceiver mAudioSessionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mAudioSessionReceiver::onReceive()");

            SharedPreferences prefSettings = getSharedPreferences(
                    ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
            String mCompatibleMode = prefSettings.getString("viper4android.settings.compatiblemode", "global");
            boolean mFXInLocalMode = false;
            if (mCompatibleMode.equals("global")) mFXInLocalMode = false;
            else mFXInLocalMode = true;

            String action = intent.getAction();
            int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
            if (sessionId == 0) {
                Log.i("ViPER4Android", "Global output mixer session control received! ");
                return;
            }

            if (action.equals(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)) {
                Log.i("ViPER4Android", String.format("New audio session: %d", sessionId));
                if (!mFXInLocalMode) {
                    Log.i("ViPER4Android", "Only global effect allowed.");
                    return;
                }
                if (mV4AMutex.acquire()) {
                    if (mGeneralFXList.indexOfKey(sessionId) < 0) {
                        Log.i("ViPER4Android", "Creating local V4ADSPModule ...");
                        mGeneralFXList.put(sessionId, new V4ADSPModule(sessionId));
                    }
                    mV4AMutex.release();
                } else Log.i("ViPER4Android", "Semaphore accquire failed.");
            }

            if (action.equals(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)) {
                Log.i("ViPER4Android", String.format("Audio session removed: %d", sessionId));
                if (mV4AMutex.acquire()) {
                    if (mGeneralFXList.indexOfKey(sessionId) >= 0) {
                        V4ADSPModule v4aRemove = mGeneralFXList.get(sessionId);
                        mGeneralFXList.remove(sessionId);
                        if (v4aRemove != null)
                            v4aRemove.release();
                    }
                    mV4AMutex.release();
                } else Log.i("ViPER4Android", "Semaphore accquire failed.");
            }

            updateSystem(false);
        }
    };

    private final BroadcastReceiver mPreferenceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mPreferenceUpdateReceiver::onReceive()");
            updateSystem(false);
        }
    };

    private final BroadcastReceiver mShowNotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mShowNotifyReceiver::onReceive()");

            String mode = getAudioOutputRouting();
            if (mode.equalsIgnoreCase("headset"))
                showNotification(getString(getResources().getIdentifier("text_headset", "string", getApplicationInfo().packageName)));
            else if (mode.equalsIgnoreCase("bluetooth"))
                showNotification(getString(getResources().getIdentifier("text_bluetooth", "string", getApplicationInfo().packageName)));
            else if (mode.equalsIgnoreCase("usb"))
                showNotification(getString(getResources().getIdentifier("text_usb", "string", getApplicationInfo().packageName)));
            else
                showNotification(getString(getResources().getIdentifier("text_speaker", "string", getApplicationInfo().packageName)));
        }
    };

    private final BroadcastReceiver mCancelNotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mCancelNotifyReceiver::onReceive()");
            cancelNotification();
        }
    };

    private final BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.i("ViPER4Android", "mScreenOnReceiver::onReceive()");
            /* Nothing to do here, for now */
        }
    };

    private final BroadcastReceiver mRoutingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.i("ViPER4Android", "mRoutingReceiver::onReceive()");

            final String action = intent.getAction();
            final boolean prevUseHeadset = mUseHeadset;
            final boolean prevUseBluetooth = mUseBluetooth;
            final boolean prevUseUSB = mUseUSB;

            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                mUseHeadset = intent.getIntExtra("state", 0) == 1;
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE,
                        BluetoothA2dp.STATE_DISCONNECTED);
                mUseBluetooth = state == BluetoothA2dp.STATE_CONNECTED;
            } else if (action.equals("android.intent.action.ANALOG_AUDIO_DOCK_PLUG")) {
                mUseUSB = intent.getIntExtra("state", 0) == 1;
            }

            Log.i("ViPER4Android", "Headset=" + mUseHeadset + ", Bluetooth=" + mUseBluetooth +
                    ", USB=" + mUseUSB);
            if (prevUseHeadset != mUseHeadset
                    || prevUseBluetooth != mUseBluetooth
                    || prevUseUSB != mUseUSB) {
            /* Audio output method changed, so we flush buffer */
                updateSystem(true);
            }
        }
    };

    private void showNotification(String nFXType) {
        SharedPreferences preferences = getSharedPreferences(ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
        boolean enableNotify = preferences.getBoolean("viper4android.settings.show_notify_icon", false);
        if (!enableNotify) {
            Log.i("ViPER4Android", "showNotification(): show_notify = false");
            return;
        }

        int nIconID = getResources().getIdentifier("icon", "drawable", getApplicationInfo().packageName);
        String mNotifyText = "ViPER4Android FX " + nFXType;
        CharSequence contentTitle = "ViPER4Android FX", contentText = nFXType;
        Intent notificationIntent = new Intent(ViPER4AndroidService.this, ViPER4Android.class);
        PendingIntent contentItent = PendingIntent.getActivity(ViPER4AndroidService.this, 0, notificationIntent, 0);

        Notification notify = new Notification.Builder(ViPER4AndroidService.this)
                .setAutoCancel(false)
                .setOngoing(true)
                .setDefaults(0)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(nIconID)
                .setTicker(mNotifyText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentItent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0x1234, notify);
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0x1234);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mServicePrepared = false;

        try {
            cancelNotification();

            try {
                Log.i("ViPER4Android", "Creating global V4ADSPModule ...");
                if (mGeneralFX == null)
                    mGeneralFX = new V4ADSPModule(DEVICE_GLOBAL_OUTPUT_MIXER);
            } catch (Exception e) {
                Log.i("ViPER4Android", "Creating V4ADSPModule failed.");
                mGeneralFX = null;
            }

            if (mGeneralFX == null)
                mDriverIsReady = false;
            else {
                mDriverIsReady = true;
                String mDriverVer = getDriverVersion();
                if (mDriverVer.equals("0.0.0.0")) mDriverIsReady = false;
                else mDriverIsReady = true;
            }

            if (mDriverIsReady) {
                SharedPreferences prefSettings = getSharedPreferences(ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", 0);
                boolean mDriverConfigured = prefSettings.getBoolean("viper4android.settings.driverconfigured", false);
                if (!mDriverConfigured) {
                    Editor editPrefs = prefSettings.edit();
                    if (editPrefs != null) {
                        editPrefs.putBoolean("viper4android.settings.driverconfigured", true);
                        editPrefs.commit();
                    }
                }
            }

            if (Build.VERSION.SDK_INT < 18)
                startForeground(ViPER4Android.NOTIFY_FOREGROUND_ID, new Notification());

            IntentFilter audioSessionFilter = new IntentFilter();
            audioSessionFilter.addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            audioSessionFilter.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            registerReceiver(mAudioSessionReceiver, audioSessionFilter);

            final IntentFilter screenFilter = new IntentFilter();
            screenFilter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(mScreenOnReceiver, screenFilter);

            final IntentFilter audioFilter = new IntentFilter();
            audioFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            audioFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            audioFilter.addAction("android.intent.action.ANALOG_AUDIO_DOCK_PLUG");
            audioFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(mRoutingReceiver, audioFilter);

            registerReceiver(mPreferenceUpdateReceiver,
                    new IntentFilter(ViPER4Android.ACTION_UPDATE_PREFERENCES));
            registerReceiver(mShowNotifyReceiver,
                    new IntentFilter(ViPER4Android.ACTION_SHOW_NOTIFY));
            registerReceiver(mCancelNotifyReceiver,
                    new IntentFilter(ViPER4Android.ACTION_CANCEL_NOTIFY));

            Log.i("ViPER4Android", "Service launched.");

            updateSystem(true);
            mServicePrepared = true;

            tmDrvStatusCommTimer.schedule(ttDrvStatusCommTimer, 60000, 60000);
            mediaStatusTimer.schedule(mediaTimerTask, 15000, 60000);  /* First is 15 secs, then 60 secs */
        } catch (Exception e) {
            mServicePrepared = false;
            cancelNotification();
            System.exit(0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mServicePrepared = false;

        try {
            tmDrvStatusCommTimer.cancel();
            mediaStatusTimer.cancel();

            if (Build.VERSION.SDK_INT < 18)
                stopForeground(true);

            unregisterReceiver(mAudioSessionReceiver);
            unregisterReceiver(mScreenOnReceiver);
            unregisterReceiver(mRoutingReceiver);
            unregisterReceiver(mPreferenceUpdateReceiver);
            unregisterReceiver(mShowNotifyReceiver);
            unregisterReceiver(mCancelNotifyReceiver);

            cancelNotification();

            if (mGeneralFX != null)
                mGeneralFX.release();
            mGeneralFX = null;

            Log.i("ViPER4Android", "Service destroyed.");
        } catch (Exception e) {
            cancelNotification();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setEqualizerLevels(float[] levels) {
        mOverriddenEqualizerLevels = levels;
        updateSystem(false);
    }

    public String getAudioOutputRouting() {
        SharedPreferences prefSettings = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
        String mLockedEffect = prefSettings.getString("viper4android.settings.lock_effect", "none");
        if (mLockedEffect.equalsIgnoreCase("none")) {
            if (mUseHeadset) return "headset";
            if (mUseBluetooth) return "bluetooth";
            if (mUseUSB) return "usb";
            return "speaker";
        }
        return mLockedEffect;
    }

    public boolean getServicePrepared() {
        return mServicePrepared;
    }

    public boolean getDriverIsReady() {
        return mDriverIsReady;
    }

    public void startStatusUpdating() {
        if (mGeneralFX != null && mDriverIsReady)
            mGeneralFX.setParameter_px4_vx4x1(PARAM_SET_UPDATE_STATUS, 1);
    }

    public void stopStatusUpdating() {
        if (mGeneralFX != null && mDriverIsReady)
            mGeneralFX.setParameter_px4_vx4x1(PARAM_SET_UPDATE_STATUS, 0);
    }

    public String getDriverVersion() {
        int nVerDWord = 0;
        if (mGeneralFX != null && mDriverIsReady)
            nVerDWord = mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_DRIVER_VERSION);
        int VMain, VSub, VExt, VBuild;
        VMain = (nVerDWord & 0xFF000000) >> 24;
        VSub = (nVerDWord & 0x00FF0000) >> 16;
        VExt = (nVerDWord & 0x0000FF00) >> 8;
        VBuild = (nVerDWord & 0x000000FF);
        return VMain + "." + VSub + "." + VExt + "." + VBuild;
    }

    public boolean getDriverNEON() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_NEONENABLED) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverEnabled() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_ENABLED) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverUsable() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_CONFIGURE) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverProcess() {
        boolean bResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_STREAMING) == 1)
                bResult = true;
        }
        return bResult;
    }

    public int getDriverEffectType() {
        int nResult = V4A_FX_TYPE_NONE;
        if (mGeneralFX != null && mDriverIsReady)
            nResult = mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_EFFECT_TYPE);
        return nResult;
    }

    public int getDriverSamplingRate() {
        int nResult = 0;
        if (mGeneralFX != null && mDriverIsReady)
            nResult = mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_SAMPLINGRATE);
        return nResult;
    }

    public int getDriverChannels() {
        int nResult = 0;
        if (mGeneralFX != null && mDriverIsReady)
            nResult = mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_CHANNELS);
        return nResult;
    }

    public boolean getConvolverUsable() {
        boolean bResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_CONVUSABLE) == 1)
                bResult = true;
        }
        return bResult;
    }

    protected void setV4AEqualizerBandLevel(int idx, int level, boolean hpfx, V4ADSPModule dsp) {
        if (dsp == null || !mDriverIsReady) return;
        if (hpfx) dsp.setParameter_px4_vx4x2(PARAM_HPFX_FIREQ_BANDLEVEL, idx, level);
        else dsp.setParameter_px4_vx4x2(PARAM_SPKFX_FIREQ_BANDLEVEL, idx, level);
    }

    protected void updateSystem(boolean mRequireReset) {
        String mode = getAudioOutputRouting();
        SharedPreferences preferences = getSharedPreferences(ViPER4Android.SHARED_PREFERENCES_BASENAME + "." + mode, 0);
        Log.i("ViPER4Android", "Begin system update(" + mode + ")");

        int nFXType = V4A_FX_TYPE_NONE;
        if (mode.equalsIgnoreCase("headset") || mode.equalsIgnoreCase("bluetooth") || mode.equalsIgnoreCase("usb"))
            nFXType = V4A_FX_TYPE_HEADPHONE;
        else if (mode.equalsIgnoreCase("speaker"))
            nFXType = V4A_FX_TYPE_SPEAKER;

        if (!mode.equalsIgnoreCase(mPreviousMode)) {
            mPreviousMode = mode;
            if (mode.equalsIgnoreCase("headset"))
                showNotification(getString(getResources().getIdentifier(
                        "text_headset", "string", getApplicationInfo().packageName)));
            else if (mode.equalsIgnoreCase("bluetooth"))
                showNotification(getString(getResources().getIdentifier(
                        "text_bluetooth", "string", getApplicationInfo().packageName)));
            else if (mode.equalsIgnoreCase("usb"))
                showNotification(getString(getResources().getIdentifier(
                        "text_usb", "string", getApplicationInfo().packageName)));
            else showNotification(getString(getResources().getIdentifier(
                        "text_speaker", "string", getApplicationInfo().packageName)));
        }

        SharedPreferences prefSettings = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);

        String mCompatibleMode = prefSettings.getString("viper4android.settings.compatiblemode", "global");
        boolean mFXInLocalMode = false;
        if (mCompatibleMode.equals("global")) mFXInLocalMode = false;
        else mFXInLocalMode = true;

        Log.i("ViPER4Android", "<+++++++++++++++ Update global effect +++++++++++++++>");
        updateSystem_Global(preferences, nFXType, mRequireReset, mFXInLocalMode);
        Log.i("ViPER4Android", "<++++++++++++++++++++++++++++++++++++++++++++++++++++>");

        Log.i("ViPER4Android", "<+++++++++++++++ Update local effect +++++++++++++++>");
        updateSystem_Local(preferences, nFXType, mRequireReset, mFXInLocalMode);
        Log.i("ViPER4Android", "<+++++++++++++++++++++++++++++++++++++++++++++++++++>");
    }

    protected void updateSystem_Global(SharedPreferences preferences, int mFXType, boolean mRequireReset, boolean mLocalFX) {
        if ((mGeneralFX == null) || (mGeneralFX.mInstance == null) || (!mDriverIsReady)) {
            Log.i("ViPER4Android", "updateSystem(): Effects is invalid!");
            return;
        }

        AudioEffect.Descriptor mFXVerify = mGeneralFX.mInstance.getDescriptor();
        if (mFXVerify == null) {
            Log.i("ViPER4Android", "updateSystem(): Effects token lost!");
            return;
        }
        if (!mFXVerify.uuid.equals(ID_V4A_GENERAL_FX)) {
            Toast.makeText(ViPER4AndroidService.this,
                    getString(getResources().getIdentifier("text_token_lost", "string", getApplicationInfo().packageName)),
                    Toast.LENGTH_LONG).show();

            Log.i("ViPER4Android", "updateSystem(): Effects token lost!");
            Log.i("ViPER4Android", "updateSystem(): The effect has been replaced by system!");
            Log.i("ViPER4Android", "updateSystem(): Reloading driver");
            try {
                mGeneralFX.release();
                mGeneralFX = new V4ADSPModule(0);
                if ((mGeneralFX == null) || (mGeneralFX.mInstance == null)) return;
            } catch (Exception e) {
                return;
            }
        }
        if (!mGeneralFX.mInstance.hasControl()) {
            Toast.makeText(ViPER4AndroidService.this,
                    getString(getResources().getIdentifier(
                            "text_token_lost", "string", getApplicationInfo().packageName)),
                    Toast.LENGTH_LONG).show();

            Log.i("ViPER4Android", "updateSystem(): Effects token lost!");
            Log.i("ViPER4Android", "updateSystem(): The effect has been taken over by system!");
            Log.i("ViPER4Android", "updateSystem(): Reloading driver");
            try {
                mGeneralFX.release();
                mGeneralFX = new V4ADSPModule(0);
                if ((mGeneralFX == null) || (mGeneralFX.mInstance == null)) return;
            } catch (Exception e) {
                return;
            }
        }

        if (mLocalFX) updateSystem_Module(preferences, mFXType, mGeneralFX, mRequireReset, true);
        else updateSystem_Module(preferences, mFXType, mGeneralFX, mRequireReset, false);
    }

    protected void updateSystem_Local(SharedPreferences preferences, int nFXType, boolean mRequireReset, boolean mLocalFX) {
        if (mV4AMutex.acquire()) {
            List<Integer> v4aUnderControl = new ArrayList<Integer>();
            for (int idx = 0; idx < mGeneralFXList.size(); idx++) {
                Integer sessionId = mGeneralFXList.keyAt(idx);
                V4ADSPModule v4aModule = mGeneralFXList.valueAt(idx);
                if ((sessionId < 0) || (v4aModule == null)) continue;
                try {
                    if (!mLocalFX)
                        updateSystem_Module(preferences, nFXType, v4aModule, mRequireReset, true);
                    else updateSystem_Module(preferences, nFXType, v4aModule, mRequireReset, false);
                } catch (Exception e) {
                    Log.i("ViPER4Android", String.format("Trouble trying to manage session %d, removing...", sessionId), e);
                    v4aUnderControl.add(sessionId);
                    continue;
                }
            }
            for (Integer aV4aUnderControl : v4aUnderControl)
                mGeneralFXList.remove(aV4aUnderControl);

            mV4AMutex.release();
        } else Log.i("ViPER4Android", "Semaphore accquire failed.");
    }

    protected void updateSystem_Module(SharedPreferences preferences, int mFXType, V4ADSPModule v4aModule, boolean mRequireReset, boolean mMasterSwitchOff) {
        Log.i("ViPER4Android", "updateSystem(): Commiting effects type");
        v4aModule.setParameter_px4_vx4x1(PARAM_FX_TYPE_SWITCH, mFXType);

        /******************************************** Headphone FX ********************************************/
        if (mFXType == V4A_FX_TYPE_HEADPHONE) {
            Log.i("ViPER4Android", "updateSystem(): Commiting headphone-fx parameters");

            /* FIR Equalizer */
            Log.i("ViPER4Android", "updateSystem(): Updating FIR Equalizer.");
            if (mOverriddenEqualizerLevels != null) {
                for (int i = 0; i < mOverriddenEqualizerLevels.length; i++)
                    setV4AEqualizerBandLevel(i, (int) Math.round(mOverriddenEqualizerLevels[i] * 100), true, v4aModule);
            } else {
                String[] levels = preferences.getString(
                        "viper4android.headphonefx.fireq.custom", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
                for (short i = 0; i < levels.length; i++)
                    setV4AEqualizerBandLevel(i, (int) Math.round(Float.valueOf(levels[i]) * 100), true, v4aModule);
            }
            if (preferences.getBoolean("viper4android.headphonefx.fireq.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_FIREQ_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_FIREQ_PROCESS_ENABLED, 0);

            /* Convolver */
            Log.i("ViPER4Android", "updateSystem(): Updating Convolver.");
            String szConvIRFileName = preferences.getString("viper4android.headphonefx.convolver.kernel", "");
            v4aModule.SetConvIRFile(szConvIRFileName, false);
            if (preferences.getBoolean("viper4android.headphonefx.convolver.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_CONV_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_CONV_PROCESS_ENABLED, 0);

            /* Colorful Music (ViPER's Headphone 360) */
            Log.i("ViPER4Android", "updateSystem(): Updating Field Surround (Colorful Music).");
            String[] cmParameter = preferences.getString(
                    "viper4android.headphonefx.colorfulmusic.coeffs", "120;200").split(";");
            if (cmParameter.length == 2) {
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_COLM_WIDENING, Integer.valueOf(cmParameter[0]));
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_COLM_DEPTH, Integer.valueOf(cmParameter[1]));
            }
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_COLM_MIDIMAGE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.colorfulmusic.midimage", "150")));
            if (preferences.getBoolean("viper4android.headphonefx.colorfulmusic.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_COLM_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_COLM_PROCESS_ENABLED, 0);

            /* Diff Surround */
            Log.i("ViPER4Android", "updateSystem(): Updating Diff Surround.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DIFFSURR_DELAYTIME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.diffsurr.delay", "500")));
            if (preferences.getBoolean("viper4android.headphonefx.diffsurr.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DIFFSURR_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DIFFSURR_PROCESS_ENABLED, 0);

            /* ViPER's Headphone Surround Engine + */
            Log.i("ViPER4Android", "updateSystem(): Updating ViPER's Headphone Surround Engine +.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VHE_EFFECT_LEVEL, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.vhs.qual", "0")));
            if (preferences.getBoolean("viper4android.headphonefx.vhs.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VHE_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VHE_PROCESS_ENABLED, 0);

            /* ViPER's Reverberation */
            Log.i("ViPER4Android", "updateSystem(): Updating Reverberation.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_ROOMSIZE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomsize", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_WIDTH, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomwidth", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_DAMP, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.damp", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_WET, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.wet", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_DRY, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.dry", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.reverb.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_REVB_PROCESS_ENABLED, 0);

            /* Playback Auto Gain Control */
            Log.i("ViPER4Android", "updateSystem(): Updating Playback AGC.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_AGC_RATIO, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.ratio", "50")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_AGC_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.volume", "80")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_AGC_MAXSCALER, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.maxscaler", "400")));
            if (preferences.getBoolean("viper4android.headphonefx.playbackgain.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_AGC_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_AGC_PROCESS_ENABLED, 0);

            /* Dynamic System */
            Log.i("ViPER4Android", "updateSystem(): Updating Dynamic System.");
            String[] dsParameter = preferences.getString(
                    "viper4android.headphonefx.dynamicsystem.coeffs", "100;5600;40;40;50;50").split(";");
            if (dsParameter.length == 6) {
                v4aModule.setParameter_px4_vx4x2(PARAM_HPFX_DYNSYS_XCOEFFS, Integer.valueOf(dsParameter[0]),
                        Integer.valueOf(dsParameter[1]));
                v4aModule.setParameter_px4_vx4x2(PARAM_HPFX_DYNSYS_YCOEFFS, Integer.valueOf(dsParameter[2]),
                        Integer.valueOf(dsParameter[3]));
                v4aModule.setParameter_px4_vx4x2(PARAM_HPFX_DYNSYS_SIDEGAIN, Integer.valueOf(dsParameter[4]),
                        Integer.valueOf(dsParameter[5]));
            }
            int dsBass = Integer.valueOf(preferences.getString("viper4android.headphonefx.dynamicsystem.bass", "0"));
            dsBass = (dsBass * 20) + 100;
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DYNSYS_BASSGAIN, dsBass);
            if (preferences.getBoolean("viper4android.headphonefx.dynamicsystem.tube", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DYNSYS_ENABLETUBE, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DYNSYS_ENABLETUBE, 0);
            if (preferences.getBoolean("viper4android.headphonefx.dynamicsystem.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DYNSYS_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_DYNSYS_PROCESS_ENABLED, 0);

            /* Fidelity Control */
            Log.i("ViPER4Android", "updateSystem(): Updating Fidelity Control.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERBASS_MODE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.bass.mode", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERBASS_SPEAKER, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.bass.freq", "40")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERBASS_BASSGAIN, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.bass.gain", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.fidelity.bass.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERBASS_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERBASS_PROCESS_ENABLED, 0);
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERCLARITY_MODE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.clarity.mode", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERCLARITY_CLARITY, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.clarity.gain", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.fidelity.clarity.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERCLARITY_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_VIPERCLARITY_PROCESS_ENABLED, 0);

            /* Cure System */
            Log.i("ViPER4Android", "updateSystem(): Updating Cure System.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_CURE_CROSSFEED, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.cure.crossfeed", "0")));
            if (preferences.getBoolean("viper4android.headphonefx.cure.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_CURE_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_CURE_PROCESS_ENABLED, 0);

            /* Limiter */
            Log.i("ViPER4Android", "updateSystem(): Updating Limiter.");
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_OUTPUT_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.outvol", "100")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_OUTPUT_PAN, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.channelpan", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_HPFX_LIMITER_THRESHOLD, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.limiter", "100")));

            /* Master Switch */
            boolean bMasterControl = preferences.getBoolean("viper4android.headphonefx.enable", false);
            if (mMasterSwitchOff) bMasterControl = false;
            v4aModule.mInstance.setEnabled(bMasterControl);
        }
        /******************************************************************************************************/
        /********************************************* Speaker FX *********************************************/
        else if (mFXType == V4A_FX_TYPE_SPEAKER) {
            Log.i("ViPER4Android", "updateSystem(): Commiting speaker-fx parameters");

            /* FIR Equalizer */
            Log.i("ViPER4Android", "updateSystem(): Updating FIR Equalizer.");
            if (mOverriddenEqualizerLevels != null) {
                for (int i = 0; i < mOverriddenEqualizerLevels.length; i++)
                    setV4AEqualizerBandLevel(i, (int) Math.round(mOverriddenEqualizerLevels[i] * 100), false, v4aModule);
            } else {
                String[] levels = preferences.getString(
                        "viper4android.headphonefx.fireq.custom", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
                for (short i = 0; i < levels.length; i++)
                    setV4AEqualizerBandLevel(i, (int) Math.round(Float.valueOf(levels[i]) * 100), false, v4aModule);
            }
            if (preferences.getBoolean("viper4android.headphonefx.fireq.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_FIREQ_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_FIREQ_PROCESS_ENABLED, 0);

            /* ViPER's Reverberation */
            Log.i("ViPER4Android", "updateSystem(): Updating Reverberation.");
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_ROOMSIZE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomsize", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_WIDTH, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomwidth", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_DAMP, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.damp", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_WET, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.wet", "0")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_DRY, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.dry", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.reverb.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_REVB_PROCESS_ENABLED, 0);

            /* Convolver */
            Log.i("ViPER4Android", "updateSystem(): Updating Convolver.");
            String szConvIRFileName = preferences.getString("viper4android.headphonefx.convolver.kernel", "");
            v4aModule.SetConvIRFile(szConvIRFileName, true);
            if (preferences.getBoolean("viper4android.headphonefx.convolver.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_CONV_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_CONV_PROCESS_ENABLED, 0);

            /* eXtraLoud */
            Log.i("ViPER4Android", "updateSystem(): Updating eXtraLoud.");
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_AGC_RATIO, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.ratio", "50")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_AGC_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.volume", "80")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_AGC_MAXSCALER, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.maxscaler", "400")));
            if (preferences.getBoolean("viper4android.headphonefx.playbackgain.enable", false))
                v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_AGC_PROCESS_ENABLED, 1);
            else v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_AGC_PROCESS_ENABLED, 0);

            /* Limiter */
            Log.i("ViPER4Android", "updateSystem(): Updating Limiter.");
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_OUTPUT_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.outvol", "100")));
            v4aModule.setParameter_px4_vx4x1(PARAM_SPKFX_LIMITER_THRESHOLD, Integer.valueOf(
                    preferences.getString("viper4android.speakerfx.limiter", "100")));

            /* Master Switch */
            boolean bMasterControl = preferences.getBoolean("viper4android.speakerfx.enable", false);
            if (mMasterSwitchOff) bMasterControl = false;
            v4aModule.mInstance.setEnabled(bMasterControl);
        }
        /******************************************************************************************************/

        /* Reset */
        if (mRequireReset)
            v4aModule.setParameter_px4_vx4x1(PARAM_SET_RESET_STATUS, 1);
        /*********/

        Log.i("ViPER4Android", "System updated.");
    }
}
