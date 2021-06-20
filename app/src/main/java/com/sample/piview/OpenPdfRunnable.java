package com.sample.piview;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.logging.Logger;

public class OpenPdfRunnable implements Runnable
{
    private static final Logger logger = Logger.getLogger("com.sample.piview");
    private final MainActivity m_activity;
    private final String m_pdf_file;
    public OpenPdfRunnable(MainActivity activity, String pdf_file) {
        m_activity = activity;
        m_pdf_file = pdf_file;
    }
    @Override
    public void run() {
        File file = new File(m_pdf_file);
        if (!file.exists()) {
            logger.severe("Converted pdf file doesn't exist, can't open it");
            return;
        }
        logger.severe("Pdf File "+m_pdf_file+" exists");
        m_activity.onDebugError("Pdf File "+m_pdf_file+" exists");
        Intent target = new Intent();
        target.setAction(android.content.Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(m_activity, "com.sample.piview.fileprovider", file);
        target.setDataAndType(uri, "application/pdf");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = Intent.createChooser(target, m_activity.getString(R.string.open_pdf_file_text));
        try {
            m_activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            logger.severe("No Pdf reader available, please install one");
            m_activity.onDebugError("No Pdf reader available, please install one");
            m_activity.onConversionFailure("No pdf reader available");
        }
    }
}

