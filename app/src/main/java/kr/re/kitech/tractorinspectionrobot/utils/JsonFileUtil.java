package kr.re.kitech.tractorinspectionrobot.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonFileUtil {
    public static String readJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
            json = sb.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static void writeJSONToFile(Context context, String fileName, String jsonString) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(jsonString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
