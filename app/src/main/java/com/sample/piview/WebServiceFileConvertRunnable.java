package com.sample.piview;

import android.os.Environment;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

public class WebServiceFileConvertRunnable implements Runnable
{
    private static final Logger logger = Logger.getLogger("com.sample.piview");
    private final String m_address;
    private final int m_port;
    private final MainActivity m_activity;
    private final Uri m_input_file_uri;

    public WebServiceFileConvertRunnable(MainActivity activity, Uri input_file_uri, String address, int port) {
        m_activity = activity;
        m_input_file_uri = input_file_uri;
        m_address = address;
        m_port = port;
    }
    // https://stackoverflow.com/a/59049496
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                    outputStream.write(buf, 0, readLen);
                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }

    // Copy an InputStream to a File.
    // https://stackoverflow.com/a/28131358
    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[10240];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }
                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        Thread thread = new Thread(() -> {
            m_activity.onDebugError("WebService Runnable Thread started");
            try  {
                // Prepare Data
                m_activity.onDebugError("Preparing Data...");
                InputStream fileReader = m_activity.getContentResolver().openInputStream(m_input_file_uri);
                String input_file_basename;
                try {
                    input_file_basename = UriUtils.getUriRealBasename(m_activity, m_input_file_uri);
                    m_activity.onDebugError("Input File Basename : `"+input_file_basename+"`");
                } catch (RuntimeException e) {
                    // Couldn't extract
                    m_activity.onDebugError("Error: couldn't extract basename from Uri : "+m_input_file_uri.toString());
                    m_activity.onConversionFailure("Filename to convert is invalid");
                    return;
                }
                String str_boundary = UUID.randomUUID().toString();
                byte[] boundary = str_boundary.getBytes();
                byte[] heading =
                        ("\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+input_file_basename+"\""
                                + "\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes();
                byte[] file = readAllBytes(fileReader); // 50 MB max
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                outputStream.write("--".getBytes());
                outputStream.write(boundary);
                outputStream.write(heading);
                outputStream.write(file);
                outputStream.write("\r\n--".getBytes());
                outputStream.write(boundary);
                outputStream.write("--\r\n".getBytes());
                byte[] data = outputStream.toByteArray( );

                m_activity.onDebugError("Sending Data...");
                // Send Data
                URL url;
                try {
                    URL tmpurl = new URL(m_address);
                    url = new URL(tmpurl.getProtocol(), tmpurl.getHost(), m_port, tmpurl.getFile());
                    logger.severe("[log] final url port : "+url.getPort());
                } catch (MalformedURLException e){
                    logger.severe("Malformed url is : "+m_address);

                    logger.severe("MalformedURLException when connecting to web service : "+e.toString());

                    m_activity.onDebugError("MalformedURLException when connecting to web service : "+e.toString());
                    m_activity.onConversionFailure("Bad server Url");
                    return;
                }
                HttpURLConnection client = null;
                try {
                    logger.severe("Openning Connection...");
                    m_activity.onDebugError("Openning Connection...");
                    client = (HttpURLConnection) url.openConnection();
                    client.setUseCaches(false);
                    client.setDoOutput(true);
                    client.setDoInput(true);
                    logger.severe("Opened Connection");
                    m_activity.onDebugError("Opened Connection");
                    client.setRequestMethod("POST");
                    client.setRequestProperty("Host", url.getHost());
                    client.setRequestProperty("User-Agent","curl/7.73.0");
                    client.setRequestProperty("Accept","*/*");
                    client.setRequestProperty("Content-Length",String.valueOf(data.length));
                    client.setRequestProperty("Content-Type","multipart/form-data; boundary="+str_boundary);
                    client.setRequestProperty("Accept-Encoding", "identity");
                    client.setDoOutput(true);

                    logger.severe("Writing Data POST...");
                    m_activity.onDebugError("Writing Data to POST request...");
                    // Write data to POST
                    client.setFixedLengthStreamingMode(data.length);
                    OutputStream wr = client.getOutputStream();
                    wr.write(data);
                    wr.flush();
                    wr.close();
                    logger.severe("Wrote Data");
                    m_activity.onDebugError("Data written to request");
                    int status = client.getResponseCode();
                    logger.severe("Answer : "+status);
                    m_activity.onDebugError("Answer : "+status);
                    if (status == 415) {
                        logger.severe("Unsupported Data Type, can't convert");
                        m_activity.onDebugError("Unsupported Data Type, can't convert");
                        return;
                    }

                    // Read answer
                    m_activity.onDebugError("Reading Answer...");
                    logger.severe("Reading Answer...");
                    File download_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    final int lastPeriodPos = input_file_basename.lastIndexOf('.');
                    String output_file_path;
                    if (lastPeriodPos <= 0) {
                        output_file_path = download_dir.getPath()+"/"+input_file_basename;
                    } else {
                        output_file_path = download_dir.getPath()+"/"+input_file_basename.substring(0, lastPeriodPos)+".pdf";
                    }
                    m_activity.onDebugError("Writing to "+output_file_path);
                    logger.severe("Writing to file : `"+output_file_path+"`");
                    File new_file = new File(output_file_path);
                    new_file.createNewFile();
                    copyInputStreamToFile(client.getInputStream(), new_file);
                    m_activity.onDebugError("File Converted");
                    logger.severe("File Converted");
                    m_activity.onFileConverted(output_file_path);
                } catch ( IOException e ){
                    logger.severe("IOException when connecting to web service : "+e.toString());
                    m_activity.onDebugError("IOException when connecting to web service : "+e.toString());
                    m_activity.onConversionFailure("Can't connect to webservice, verify your internet ?");
                } catch ( SecurityException e){
                    logger.severe("SecurityException when connecting to web service : "+e.toString());
                    m_activity.onDebugError("SecurityException when connecting to web service : "+e.toString());
                    m_activity.onConversionFailure("Security error when connecting to web service");
                } catch ( IllegalArgumentException e){
                    logger.severe("IllegalArgumentException when connecting to web service : "+e.toString());
                    m_activity.onDebugError("IllegalArgumentException when connecting to web service : "+e.toString());
                    m_activity.onConversionFailure("Failed to connect to web service");
                } catch ( UnsupportedOperationException e){
                    logger.severe("UnsupportedOperationException when connecting to web service : "+e.toString());
                    m_activity.onDebugError("UnsupportedOperationException when connecting to web service : "+e.toString());
                    m_activity.onConversionFailure("Failed to request web service");
                } finally {
                    if(client != null) // Make sure the connection is not null.
                        client.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                m_activity.onConversionFailure("Failed to convert");
            }
        });
        thread.start();
    }
}
