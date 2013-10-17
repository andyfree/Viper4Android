package com.vipercn.viper4android_v2.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

public class Utils {
    public static class CpuInfo {
        private boolean m_bCPUHasNEON = false;
        private boolean m_bCPUHasVFP = false;

        // Lets read /proc/cpuinfo in java
        private boolean readCPUInfo() {
            String mCPUInfoFile = "/proc/cpuinfo";
            FileReader frCPUInfoReader = null;
            BufferedReader brReader = null;

            m_bCPUHasNEON = false;
            m_bCPUHasVFP = false;

            // Find "Features" line, extract neon and vfp
            try {
                frCPUInfoReader = new FileReader(mCPUInfoFile);
                brReader = new BufferedReader(frCPUInfoReader);
                while (true) {
                    String mLine = brReader.readLine();
                    if (mLine == null) break;
                    mLine = mLine.trim();
                    if (mLine.startsWith("Features")) {
                        Log.i("ViPER4Android_Utils", "CpuInfo[java] = <" + mLine + ">");
                        StringTokenizer stBlock = new StringTokenizer(mLine);
                        while (stBlock.hasMoreElements()) {
                            String mFeature = stBlock.nextToken();
                            if (mFeature != null) {
                                if (mFeature.equalsIgnoreCase("neon")) m_bCPUHasNEON = true;
                                else if (mFeature.equalsIgnoreCase("vfp")) m_bCPUHasVFP = true;
                            }
                            continue;
                        }
                    }
                }
                brReader.close();
                frCPUInfoReader.close();

                Log.i("ViPER4Android_Utils", "CpuInfo[java] = NEON:" + m_bCPUHasNEON + ", VFP:" + m_bCPUHasVFP);
                if (!m_bCPUHasNEON && !m_bCPUHasVFP)
                    return false;
                return true;
            } catch (IOException e) {
                try {
                    if (brReader != null) brReader.close();
                    if (frCPUInfoReader != null) frCPUInfoReader.close();
                    return false;
                } catch (Exception ex) {
                    return false;
                }
            }
        }

        // Lets read /proc/cpuinfo in jni
        private void ReadCPUInfoJni() {
            m_bCPUHasNEON = V4AJniInterface.isCPUSupportNEON();
            m_bCPUHasVFP = V4AJniInterface.isCPUSupportVFP();
        }

        // Buffered result
        public CpuInfo() {
            m_bCPUHasNEON = false;
            m_bCPUHasVFP = false;
            if (!readCPUInfo())
                ReadCPUInfoJni();
        }

        public boolean HasNEON() {
            return m_bCPUHasNEON;
        }

        public boolean HasVFP() {
            return m_bCPUHasVFP;
        }
    }

    // Check if the specified file exists.
    public static boolean fileExists(String filename) {
        boolean mExist = new File(filename).exists();
        if (!mExist) {
            RootTools.useRoot = true;
            mExist = RootTools.exists(filename);
        }
        return mExist;
    }

    // Get a file length
    public static long getFileLength(String mFileName) {
        try {
            return new File(mFileName).length();
        } catch (Exception e) {
            return 0;
        }
    }

    // Download a file from internet
    public static boolean downloadFile(String mURL, String mFileName, String mStorePath) {
        try {
            URL url = new URL(mURL);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            if (conn.getContentLength() <= 0) return false;
            if (is == null) return false;
            FileOutputStream fos = new FileOutputStream(mStorePath + mFileName);

            byte[] buf = new byte[1024];
            do {
                int numread = is.read(buf);
                if (numread == -1) break;
                fos.write(buf, 0, numread);
            } while (true);
            is.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check a file with checksum
    public static boolean fileChecksum(String mFilePathName, String mCheckSum) {
        long checkSum = 0;

        try {
            FileInputStream fis = new FileInputStream(mFilePathName);
            byte[] buf = new byte[1024];
            do {
                int numread = fis.read(buf);
                if (numread == -1) break;
                for (int idx = 0; idx < numread; idx++)
                    checkSum = checkSum + (long) buf[idx];
            } while (true);
            fis.close();
            String mNewChecksum = Long.toString(checkSum);
            if (mCheckSum.equals(mNewChecksum)) return true;
            else return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Read file list from path
    public static void getFileNameList(File path, String fileExt, ArrayList<String> fileList) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (null == files) return;
            for (File file : files) getFileNameList(file, fileExt, fileList);
        } else {
            String filePath = path.getAbsolutePath();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (fileName.toLowerCase(Locale.US).endsWith(fileExt))
                fileList.add(fileName);
        }
    }

    // Get profile name from a file
    public static String getProfileName(String mProfileFileName) {
        try {
            FileInputStream fisInput = new FileInputStream(mProfileFileName);
            InputStreamReader isrInput = new InputStreamReader(fisInput, "UTF-8");
            BufferedReader brInput = new BufferedReader(isrInput);
            String mProfileName = "";
            while (true) {
                String mLine = brInput.readLine();
                if (mLine == null) break;
                if (mLine.startsWith("#")) continue;

                String[] mChunks = mLine.split("=");
                if (mChunks.length != 2) continue;
                if (mChunks[0].trim().equalsIgnoreCase("profile_name")) {
                    mProfileName = mChunks[1];
                    break;
                }
            }
            brInput.close();
            isrInput.close();
            fisInput.close();

            return mProfileName;
        } catch (Exception e) {
            return "";
        }
    }

    // Get profile name list
    public static ArrayList<String> getProfileList(String mProfileDir) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> mProfileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", mProfileList);

            ArrayList<String> mProfileNameList = new ArrayList<String>();
            for (String profileList : mProfileList) {
                String mFileName = mProfileDir + profileList;
                String mName = getProfileName(mFileName);
                mProfileNameList.add(mName.trim());
            }

            return mProfileNameList;
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    // Check whether profile has been exists
    public static boolean checkProfileExists(String mProfileName, String mProfileDir) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> mProfileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", mProfileList);

            boolean mFoundProfile = false;
            for (String profileList : mProfileList) {
                String mFileName = mProfileDir + profileList;
                String mName = getProfileName(mFileName);
                if (mProfileName.trim().equalsIgnoreCase(mName.trim())) {
                    mFoundProfile = true;
                    break;
                }
            }

            return mFoundProfile;
        } catch (Exception e) {
            return false;
        }
    }

    // Load profile from file
    public static boolean loadProfile(String mProfileName, String mProfileDir, String mPreferenceName, Context ctx) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> mProfileFileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", mProfileFileList);
            String mProfileFileName = "";
            for (String aMProfileFileList : mProfileFileList) {
                String mFileName = mProfileDir + aMProfileFileList;
                String mName = getProfileName(mFileName);
                if (mProfileName.trim().equalsIgnoreCase(mName.trim())) {
                    mProfileFileName = mFileName;
                    break;
                }
            }
            if (mProfileFileName.equals("")) return false;

            SharedPreferences preferences = ctx.getSharedPreferences(mPreferenceName, Context.MODE_PRIVATE);
            if (preferences != null) {
                FileInputStream fisInput = new FileInputStream(mProfileFileName);
                InputStreamReader isrInput = new InputStreamReader(fisInput, "UTF-8");
                BufferedReader brInput = new BufferedReader(isrInput);
                Editor e = preferences.edit();
                while (true) {
                    String mLine = brInput.readLine();
                    if (mLine == null) break;
                    if (mLine.startsWith("#")) continue;

                    String[] mChunks = mLine.split("=");
                    if (mChunks.length != 3) continue;
                    if (mChunks[1].trim().equalsIgnoreCase("boolean")) {
                        String mParameter = mChunks[0];
                        boolean mValue = Boolean.valueOf(mChunks[2]);
                        e.putBoolean(mParameter, mValue);
                    } else if (mChunks[1].trim().equalsIgnoreCase("string")) {
                        String mParameter = mChunks[0];
                        String mValue = mChunks[2];
                        e.putString(mParameter, mValue);
                    } else {
                    }
                }
                e.commit();
                brInput.close();
                isrInput.close();
                fisInput.close();

                return true;
            } else return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Save profile to file
    public static void saveProfile(String mProfileName, String mProfileDir, String mPreferenceName, Context ctx) {
        try {
            SharedPreferences preferences = ctx.getSharedPreferences(mPreferenceName, Context.MODE_PRIVATE);
            if (preferences != null) {
                String mOutFileName = mProfileDir + mProfileName + ".prf";
                if (fileExists(mOutFileName)) new File(mOutFileName).delete();

                FileOutputStream fosOutput = new FileOutputStream(mOutFileName);
                OutputStreamWriter oswOutput = new OutputStreamWriter(fosOutput, "UTF-8");
                BufferedWriter mOutput = new BufferedWriter(oswOutput);

                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd   hh:mm:ss", Locale.US);
                String mDate = sDateFormat.format(new java.util.Date());

                mOutput.write("# ViPER4Android audio effect profile !\n");
                mOutput.write("# Created " + mDate + "\n\n");
                mOutput.write("profile_name=" + mProfileName + "\n\n");

                String mValue = "";

                // boolean values
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.enable", false));
                mOutput.write("viper4android.headphonefx.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.speakerfx.enable", false));
                mOutput.write("viper4android.speakerfx.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.playbackgain.enable", false));
                mOutput.write("viper4android.headphonefx.playbackgain.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.fireq.enable", false));
                mOutput.write("viper4android.headphonefx.fireq.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.convolver.enable", false));
                mOutput.write("viper4android.headphonefx.convolver.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.colorfulmusic.enable", false));
                mOutput.write("viper4android.headphonefx.colorfulmusic.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.diffsurr.enable", false));
                mOutput.write("viper4android.headphonefx.diffsurr.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.vhs.enable", false));
                mOutput.write("viper4android.headphonefx.vhs.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.reverb.enable", false));
                mOutput.write("viper4android.headphonefx.reverb.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.dynamicsystem.enable", false));
                mOutput.write("viper4android.headphonefx.dynamicsystem.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.dynamicsystem.tube", false));
                mOutput.write("viper4android.headphonefx.dynamicsystem.tube=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.fidelity.bass.enable", false));
                mOutput.write("viper4android.headphonefx.fidelity.bass.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.fidelity.clarity.enable", false));
                mOutput.write("viper4android.headphonefx.fidelity.clarity.enable=boolean=" + mValue + "\n");
                mValue = String.valueOf(preferences.getBoolean("viper4android.headphonefx.cure.enable", false));
                mOutput.write("viper4android.headphonefx.cure.enable=boolean=" + mValue + "\n");

                // string values
                mValue = preferences.getString("viper4android.headphonefx.playbackgain.ratio", "50");
                mOutput.write("viper4android.headphonefx.playbackgain.ratio=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.playbackgain.maxscaler", "400");
                mOutput.write("viper4android.headphonefx.playbackgain.maxscaler=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.playbackgain.volume", "80");
                mOutput.write("viper4android.headphonefx.playbackgain.volume=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fireq", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;");
                mOutput.write("viper4android.headphonefx.fireq=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fireq.custom", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;");
                mOutput.write("viper4android.headphonefx.fireq.custom=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.convolver.kernel", "");
                mOutput.write("viper4android.headphonefx.convolver.kernel=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.colorfulmusic.coeffs", "120;200");
                mOutput.write("viper4android.headphonefx.colorfulmusic.coeffs=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.colorfulmusic.midimage", "150");
                mOutput.write("viper4android.headphonefx.colorfulmusic.midimage=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.diffsurr.delay", "500");
                mOutput.write("viper4android.headphonefx.diffsurr.delay=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.vhs.qual", "0");
                mOutput.write("viper4android.headphonefx.vhs.qual=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.reverb.roomsize", "0");
                mOutput.write("viper4android.headphonefx.reverb.roomsize=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.reverb.roomwidth", "0");
                mOutput.write("viper4android.headphonefx.reverb.roomwidth=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.reverb.damp", "0");
                mOutput.write("viper4android.headphonefx.reverb.damp=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.reverb.wet", "0");
                mOutput.write("viper4android.headphonefx.reverb.wet=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.reverb.dry", "50");
                mOutput.write("viper4android.headphonefx.reverb.dry=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.dynamicsystem.coeffs", "100;5600;40;80;50;50");
                mOutput.write("viper4android.headphonefx.dynamicsystem.coeffs=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.dynamicsystem.bass", "0");
                mOutput.write("viper4android.headphonefx.dynamicsystem.bass=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fidelity.bass.mode", "0");
                mOutput.write("viper4android.headphonefx.fidelity.bass.mode=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fidelity.bass.freq", "40");
                mOutput.write("viper4android.headphonefx.fidelity.bass.freq=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fidelity.bass.gain", "50");
                mOutput.write("viper4android.headphonefx.fidelity.bass.gain=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fidelity.clarity.mode", "0");
                mOutput.write("viper4android.headphonefx.fidelity.clarity.mode=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.fidelity.clarity.gain", "50");
                mOutput.write("viper4android.headphonefx.fidelity.clarity.gain=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.cure.crossfeed", "0");
                mOutput.write("viper4android.headphonefx.cure.crossfeed=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.outvol", "100");
                mOutput.write("viper4android.headphonefx.outvol=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.channelpan", "0");
                mOutput.write("viper4android.headphonefx.channelpan=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.headphonefx.limiter", "100");
                mOutput.write("viper4android.headphonefx.limiter=string=" + mValue + "\n");
                mValue = preferences.getString("viper4android.speakerfx.limiter", "100");
                mOutput.write("viper4android.speakerfx.limiter=string=" + mValue + "\n");

                mOutput.flush();
                mOutput.close();
                oswOutput.close();
                fosOutput.close();
            }
        } catch (Exception e) {
        }
    }

    // Modify audio_effects.conf
    public static boolean modifyFXConfig(String mInputFile, String mOutputFile) {
        Log.i("ViPER4Android_Utils", "Editing audio configuration, input = " + mInputFile + ", output = " + mOutputFile);
        try {
            long lInputFileLength = getFileLength(mInputFile);

            // Create reading and writing stuff
            FileInputStream fisInput = new FileInputStream(mInputFile);
            FileOutputStream fosOutput = new FileOutputStream(mOutputFile);
            InputStreamReader isrInput = new InputStreamReader(fisInput, "ASCII");
            OutputStreamWriter oswOutput = new OutputStreamWriter(fosOutput, "ASCII");
            BufferedReader brInput = new BufferedReader(isrInput);
            BufferedWriter bwOutput = new BufferedWriter(oswOutput);

            // Check whether the file has already modified
            boolean bConfigModified = false;
            brInput.mark((int) lInputFileLength);
            do {
                String mLine = brInput.readLine();
                if (mLine == null) break;
                if (mLine.startsWith("#")) continue;
                /* This is v4a effect uuid */
                if (mLine.toLowerCase(Locale.US).contains("41d3c987-e6cf-11e3-a88a-11aba5d5c51b")) {
                    Log.i("ViPER4Android_Utils", "Source file has been modified, line = " + mLine);
                    bConfigModified = true;
                    break;
                }
            } while (true);

            boolean bLibraryAppend = false;
            boolean bEffectAppend = false;
            if (bConfigModified) {
                // Already modified, just copy
                brInput.reset();
                do {
                    String mLine = brInput.readLine();
                    if (mLine == null) break;
                    bwOutput.write(mLine + "\n");
                } while (true);
                bwOutput.flush();

                brInput.close();
                isrInput.close();
                fisInput.close();
                bwOutput.close();
                oswOutput.close();
                fosOutput.close();

                return true;
            } else {
                // Lets append v4a library and effect to configuration
                brInput.reset();
                do {
                    String mLine = brInput.readLine();
                    if (mLine == null) break;
                    if (mLine.trim().equalsIgnoreCase("libraries {") && !bLibraryAppend) {
                        // Append library
                        bwOutput.write(mLine + "\n");
                        bwOutput.write("  v4a_fx {\n");
                        bwOutput.write("    path /system/lib/soundfx/libv4a_fx_ics.so\n");
                        bwOutput.write("  }\n");
                        bLibraryAppend = true;
                    } else if (mLine.trim().equalsIgnoreCase("effects {") && !bEffectAppend) {
                        // Append effect
                        bwOutput.write(mLine + "\n");
                        bwOutput.write("  v4a_standard_fx {\n");
                        bwOutput.write("    library v4a_fx\n");
                        bwOutput.write("    uuid 41d3c987-e6cf-11e3-a88a-11aba5d5c51b\n");
                        bwOutput.write("  }\n");
                        bEffectAppend = true;
                    } else bwOutput.write(mLine + "\n");
                } while (true);
                bwOutput.flush();

                brInput.close();
                isrInput.close();
                fisInput.close();
                bwOutput.close();
                oswOutput.close();
                fosOutput.close();

                // Just in case, different config file format in future
                return bLibraryAppend & bEffectAppend;
            }
        } catch (Exception e) {
            Log.i("ViPER4Android_Utils", "Error: " + e.getMessage());
            return false;
        }
    }

    // Get application data path
    public static String getBasePath(Context ctx) {
        Context cont = ctx.getApplicationContext();
        String mBasePath = cont.getFilesDir().toString();
        if (!cont.getFilesDir().exists())
            if (!cont.getFilesDir().mkdir()) return "";
        return mBasePath;
    }

    // Copy assets to local
    public static boolean copyAssetsToLocal(Context ctx, String mSourceName, String mDstName) {
        String mBasePath = getBasePath(ctx);
        if (mBasePath.equals("")) return false;
        mDstName = mBasePath + "/" + mDstName;

        InputStream myInput = null;
        OutputStream myOutput = null;
        String outFileName = mDstName;
        try {
            File hfOutput = new File(mDstName);
            if (hfOutput.exists()) hfOutput.delete();

            myOutput = new FileOutputStream(outFileName);
            myInput = ctx.getAssets().open(mSourceName);
            byte[] tBuffer = new byte[4096];  /* 4K page size */
            int mLength = 0;
            while ((mLength = myInput.read(tBuffer)) > 0)
                myOutput.write(tBuffer, 0, mLength);
            myOutput.flush();
            myInput.close();
            myOutput.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    // Uninstall ViPER4Android FX driver
    public static void uninstallDrv_FX() {
        /* When uninstalling the v4a driver, we just delete the driver file (or just uninstall the apk).
         * Android will check all effect drivers before load, so keep v4a in audio_effects.conf is safe.
         */

        // Lets acquire root first :)
        RootTools.useRoot = true;
        if (!RootTools.isRootAvailable()) return;
        if (!RootTools.isAccessGiven()) return;
        // When done, a root shell was opened

        // Then delete the driver
        String mDriverPathName = "/system/lib/soundfx/libv4a_fx_ics.so";
        try {
            RootTools.useRoot = true;
            if (RootTools.exists(mDriverPathName)) {
                RootTools rtTools = new RootTools();
                rtTools.deleteFileOrDirectory(mDriverPathName, true);
                // Close all shells
                RootTools.closeAllShells();
            }
        } catch (Exception e) {
            return;
        }
    }

    // Install ViPER4Android FX driver
    public static boolean installDrv_FX(Context ctx, String mDriverName) {
        // Make sure we can use external storage for temp directory
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return false;

        // Copy driver assets to local
        if (!copyAssetsToLocal(ctx, mDriverName, "libv4a_fx_ics.so"))
            return false;

        // Lets acquire root first :)
        RootTools.useRoot = true;
        if (!RootTools.isRootAvailable()) return false;
        if (!RootTools.isAccessGiven()) return false;
        // When done, a root shell was opened

        // Check chmod utils
        String mChmod = "";
        if (RootTools.checkUtil("chmod"))
            mChmod = "chmod";
        else {
            if (RootTools.checkUtil("busybox") && RootTools.hasUtil("chmod", "busybox"))
                mChmod = "busybox chmod";
            else {
                if (RootTools.checkUtil("toolbox") && RootTools.hasUtil("chmod", "toolbox"))
                    mChmod = "toolbox chmod";
            }
        }
        if ((mChmod == null) || mChmod.equals(""))
            return false;

        // Generate temp config file path, thanks to 'ste71m'
        String mExternalStoragePathName = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (Build.VERSION.SDK_INT >= 18) {
            if (mExternalStoragePathName.endsWith("/emulated/0"))
                mExternalStoragePathName = mExternalStoragePathName.replace("/emulated/0", "/emulated/legacy");
        }
        String mSystemConf = mExternalStoragePathName + "/v4a_audio_system.conf";
        String mVendorConf = mExternalStoragePathName + "/v4a_audio_vendor.conf";

        // Check vendor directory
        boolean vendorExists = false;
        if (fileExists("/system/vendor/etc/audio_effects.conf"))
            vendorExists = true;

        // Copy configuration to temp directory
        if (vendorExists) {
            /* Copy to external storage, we dont need remount */
            RootTools.copyFile("/system/etc/audio_effects.conf", mSystemConf, false, false);
            RootTools.copyFile("/system/vendor/etc/audio_effects.conf", mVendorConf, false, false);
        } else {
            /* Copy to external storage, we dont need remount */
            RootTools.copyFile("/system/etc/audio_effects.conf", mSystemConf, false, false);
        }

        // Modifing configuration
        boolean modifyResult = true;
        modifyResult &= modifyFXConfig(mSystemConf, mSystemConf + ".out");
        if (vendorExists) modifyResult &= modifyFXConfig(mVendorConf, mVendorConf + ".out");
        if (!modifyResult) {
            /* Modify the configuration failed, lets cleanup temp file(s) */
            try {
                RootTools rtTools = new RootTools();
                if (vendorExists) {
                    if (!rtTools.deleteFileOrDirectory(mSystemConf, false))
                        new File(mSystemConf).delete();
                    if (!rtTools.deleteFileOrDirectory(mVendorConf, false))
                        new File(mVendorConf).delete();
                    if (!rtTools.deleteFileOrDirectory(mSystemConf + ".out", false))
                        new File(mSystemConf + ".out").delete();
                    if (!rtTools.deleteFileOrDirectory(mVendorConf + ".out", false))
                        new File(mVendorConf + ".out").delete();
                } else {
                    if (!rtTools.deleteFileOrDirectory(mSystemConf, false))
                        new File(mSystemConf).delete();
                    if (!rtTools.deleteFileOrDirectory(mSystemConf + ".out", false))
                        new File(mSystemConf + ".out").delete();
                }
                // Close all shells
                RootTools.closeAllShells();
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        // Copy back to system
        boolean operationSuccess = true;
        try {
            if (vendorExists) {
                String mBaseDrvPathName = getBasePath(ctx) + "/" + "libv4a_fx_ics.so";
                // Copy files
                operationSuccess &= RootTools.remount("/system", "RW");
                if (operationSuccess)
                    operationSuccess &= RootTools.copyFile(mBaseDrvPathName, "/system/lib/soundfx/libv4a_fx_ics.so", false, false);
                if (operationSuccess)
                    operationSuccess &= RootTools.copyFile(mSystemConf + ".out", "/system/etc/audio_effects.conf", false, false);
                if (operationSuccess)
                    operationSuccess &= RootTools.copyFile(mVendorConf + ".out", "/system/vendor/etc/audio_effects.conf", false, false);
                // Modify permission
                CommandCapture ccSetPermission = new CommandCapture(0,
                        mChmod + " 644 /system/etc/audio_effects.conf",
                        mChmod + " 644 /system/vendor/etc/audio_effects.conf",
                        mChmod + " 644 /system/lib/soundfx/libv4a_fx_ics.so");
                RootTools.getShell(true).add(ccSetPermission).waitForFinish();
                RootTools.remount("/system", "RO");
            } else {
                String mBaseDrvPathName = getBasePath(ctx) + "/" + "libv4a_fx_ics.so";
                // Copy files
                operationSuccess &= RootTools.remount("/system", "RW");
                if (operationSuccess)
                    operationSuccess &= RootTools.copyFile(mBaseDrvPathName, "/system/lib/soundfx/libv4a_fx_ics.so", false, false);
                if (operationSuccess)
                    operationSuccess &= RootTools.copyFile(mSystemConf + ".out", "/system/etc/audio_effects.conf", false, false);
                // Modify permission
                CommandCapture ccSetPermission = new CommandCapture(0,
                        mChmod + " 644 /system/etc/audio_effects.conf",
                        mChmod + " 644 /system/lib/soundfx/libv4a_fx_ics.so");
                RootTools.getShell(true).add(ccSetPermission).waitForFinish();
                RootTools.remount("/system", "RO");
            }
        } catch (Exception e) {
            operationSuccess = false;
        }

        /* Cleanup temp file(s) and close root shell */
        try {
            RootTools rtTools = new RootTools();
            if (vendorExists) {
                if (!rtTools.deleteFileOrDirectory(mSystemConf, false))
                    new File(mSystemConf).delete();
                if (!rtTools.deleteFileOrDirectory(mVendorConf, false))
                    new File(mVendorConf).delete();
                if (!rtTools.deleteFileOrDirectory(mSystemConf + ".out", false))
                    new File(mSystemConf + ".out").delete();
                if (!rtTools.deleteFileOrDirectory(mVendorConf + ".out", false))
                    new File(mVendorConf + ".out").delete();
            } else {
                if (!rtTools.deleteFileOrDirectory(mSystemConf, false))
                    new File(mSystemConf).delete();
                if (!rtTools.deleteFileOrDirectory(mSystemConf + ".out", false))
                    new File(mSystemConf + ".out").delete();
            }
            // Close all shells
            RootTools.closeAllShells();
        } catch (Exception e) {
            return false;
        }

        return operationSuccess;
    }
}
