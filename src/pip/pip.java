/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pip;

/**
 *
 * @author Marius
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static melektro.Email.EMailSender;
import melektro.LogsFormatter;
import static melektro.LogsFormatter.Log;
import static melektro.PublicIPAddress.GetPublicIp;

public class pip {

    static String EmailAddressTo;
    static String EmailAddressFrom;
    static String IpFile;
    static String EmailPasswordFrom;
    static String ProxyToUse;
    static String ProxyPortToUse;
    static String TestMode;
    static boolean bTestMode;
    static String RemotePublicIpNameForEmail;
    static String EmailAddressCC;

    public static void sendMail(String Ip) {
        GetProperties();
        String host = "smtp.gmail.com";
        String port = "587";
        String from = EmailAddressFrom;
        String password = EmailPasswordFrom;
        String to = EmailAddressTo;
        String cc = EmailAddressCC;
        String subject = RemotePublicIpNameForEmail + " Public IP Change";
        String message = Ip;

        for (int i = 1; i < 11; i++) {

            try {
                if (EMailSender(host, port, from, password, to, cc, subject, message)) {
                    Log("Email sent");
                    break;
                } else {
                    Log("Email attempt " + i + ". Sleeping for 5s");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Log("Exception: "+e.getMessage());
            }
        }

    }

    private static String GetDummyPublicIp() throws Exception {
        return "147.135.68.68";
    }

    private static String GetFileIp(String Ip) throws Exception {
        String data = "";
        File file = new File(IpFile);
        FileInputStream fis = null;

        try {
            if (file.exists()) {
                fis = new FileInputStream(file);
                int content;
                while ((content = fis.read()) != -1) {
                    data += (char) content;
                }
            } else {
                SetFileIp(Ip);
                data = Ip;
                Log("  File created");
                sendMail(Ip);
            }
        } catch (Exception e) {
            Log("Exception: "+e.getMessage());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log("Exception: "+e.getMessage());
            }
        }
        return data;
    }

    private static void SetFileIp(String Ip) throws Exception {
        FileOutputStream fos = null;
        File file;

        try {
            file = new File(IpFile);
            fos = new FileOutputStream(file);

            if (!file.exists()) {
                file.createNewFile();
            }

            byte[] contentInBytes = Ip.getBytes();

            fos.write(contentInBytes);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log("Exception: "+e.getMessage());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log("Exception: "+e.getMessage());
            }
        }
    }

    private static void SetProperties() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("configpip.properties");
            prop.setProperty("EmailAddressTo", "anemailaddressTo@email.com");
            prop.setProperty("EmailAddressFrom", "anemailaddressFrom@email.com");
            prop.setProperty("EmailAddressCC", "anemailaddressCC@email.com");
            prop.setProperty("EmailPasswordFrom", "emailPassword");
            prop.setProperty("IpFile", "fileIP.txt");
            prop.setProperty("ProxyToUse", "");
            prop.setProperty("ProxyPortToUse", "8080");
            prop.setProperty("TestMode", "false");
            prop.setProperty("RemotePublicIpNameForEmail", "RemoteIP");
            prop.store(output, null);

        } catch (IOException e) {
            Log("Exception: "+e.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log("Exception: "+e.getMessage());
                }
            }
        }
    }

    private static String LoadProperty(Properties prop, String property, String defaultvalue) {
        String PropertyValue = prop.getProperty(property);
        if (PropertyValue != null) {
            return PropertyValue;
        } else {
            return defaultvalue;
        }
    }

    private static void GetProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        File file = new File("configpip.properties");

        if (!file.exists()) {
            SetProperties();
            Log("Please configure the properties in the 'configpip.properties' file.");
            System.exit(0);
        } else {
            try {
                input = new FileInputStream(file);
                prop.load(input);

                EmailAddressTo = LoadProperty(prop, "EmailAddressTo", "");
                EmailAddressCC = LoadProperty(prop, "EmailAddressCC", "");
                EmailAddressFrom = LoadProperty(prop, "EmailAddressFrom", "");
                EmailPasswordFrom = LoadProperty(prop, "EmailPasswordFrom", "");
                IpFile = LoadProperty(prop, "IpFile", "fileIP.txt");
                ProxyToUse = LoadProperty(prop, "ProxyToUse", "");
                ProxyPortToUse = LoadProperty(prop, "ProxyPortToUse", "");
                TestMode = LoadProperty(prop, "TestMode", "false").toLowerCase();
                RemotePublicIpNameForEmail = LoadProperty(prop, "RemotePublicIpNameForEmail", "domain");
                bTestMode = !TestMode.equals("false");

            } catch (IOException e) {
                Log("Exception: "+e.getMessage());
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        Log("Exception: "+e.getMessage());
                    }
                }
            }
        }
    }

    private static void CompareIp(Boolean bSendAnyway) {
        try {
            String publicIpAddress;
            if (bTestMode) {
                publicIpAddress = GetDummyPublicIp();
            } else {
                publicIpAddress = GetPublicIp(ProxyToUse, ProxyPortToUse);
            }
            String fileIpAddress = GetFileIp(publicIpAddress);
            if (!fileIpAddress.equals(publicIpAddress)) {
                Log("Not Same - "+"Public IP address changes from "+fileIpAddress+" to " + publicIpAddress);
                sendMail(publicIpAddress);
                SetFileIp(publicIpAddress);
            }
        } catch (Exception e) {
            Log("Exception: "+e.getMessage());
        }
    }

    private static void FileCompare() {
        File file = new File(IpFile);
        if (file.exists()) {
            CompareIp(false);
            //file.delete();
        }
    }
    
    public static void main(String[] args) throws Exception {
        new LogsFormatter().setLogging(Level.ALL);
        //Log("Initialising ...");
        GetProperties();
        CompareIp(false);
        FileCompare();
    }

}
