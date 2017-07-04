package fan.fwd.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onlab.packet.IPv4;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.apache.commons.lang3.time.DateUtils;

import org.json.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

public class Authentication {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String portal_mac = "ea:e9:78:fb:fd:2d";
    private String gateway_mac = "00:50:56:fc:6e:36";

    private String src_mac;
    private String dst_mac;
    private String src_ip;
    private String dst_ip;
    private String src_port = "";
    private String dst_port = "";
    private byte protocol;

    private String src_access_sw;
    private String src_access_port;
    private String dst_access_sw;
    private String dst_access_port;

    private String in_sw;
    private String in_port;

    private String time;
    private Long bytes;

    private String src_user = "";
    private String dst_user = "";

    private boolean mac_enable = false;

    private String result = "Drop";

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private String accessDbUrl = "http://127.0.0.1:5000";

    public Authentication(String src_mac, String dst_mac, String src_ip, String dst_ip, 
            String src_port, String dst_port, byte protocol, String src_access_sw, String src_access_port, 
            String dst_access_sw, String dst_access_port, String in_sw, String in_port, String time) {

        this.src_mac = src_mac;
        this.dst_mac = dst_mac;
        this.src_ip = src_ip;
        this.dst_ip = dst_ip;
        this.protocol = protocol;

        if(protocol == IPv4.PROTOCOL_TCP || protocol == IPv4.PROTOCOL_UDP) {
            this.src_port = src_port;
            this.dst_port = dst_port;
        }

        this.src_access_sw = src_access_sw;
        this.src_access_port = src_access_port;
        this.dst_access_sw = dst_access_sw;
        this.dst_access_port = dst_access_port;

        this.in_sw = in_sw;
        this.in_port = in_port;

        this.time = time;
    }

    public String accessCheck() {
        if(protocol == IPv4.PROTOCOL_ICMP) {
            return "Pass";
        }
        else if(src_mac.equalsIgnoreCase(portal_mac)) {
            if(src_port.equals("80") || src_port.equals("443")) {
                return "PktFromPortal";
            }
            else {
                return "Pass";
            }
        }
        else if(dst_mac.equalsIgnoreCase(portal_mac)) {
            return "Pass";
        }

        boolean src_enable = isMacPass(src_mac);

        src_user = macToUser(src_mac);
        dst_user = macToUser(dst_mac);
        
        if(src_mac.equalsIgnoreCase(gateway_mac)) {
            result = "Pass";
        }
        else if(!src_mac.equalsIgnoreCase(gateway_mac) && src_enable) {
            result = "Pass";
        }
        else if(!src_mac.equalsIgnoreCase(portal_mac) && !dst_mac.equalsIgnoreCase(portal_mac)) {
            if(dst_port.equals("80") || dst_port.equals("443")) {               
                updateIp();
                return "RedirectToPortal";
            }
        }

        insertAssociation();
        return result;
    }

    private void insertAssociation() {
        try {
            String date = dateFormat.format(new Date(Long.valueOf(this.time)));
            String time = timeFormat.format(new Date(Long.valueOf(this.time)));

            String s_url = accessDbUrl + "/insert_asso?src_mac=" + src_mac + "&dst_mac=" + dst_mac + 
                "&src_ip=" + src_ip + "&dst_ip=" + dst_ip + "&src_port=" + src_port + "&dst_port=" + dst_port + 
                "&protocol=" + protocol + "&src_user=" + src_user + "&dst_user=" + dst_user + "&in_sw=" + in_sw + 
                "&in_port=" + in_port + "&date=" + date + "&time=" + time + "&src_access_sw=" + src_access_sw + 
                "&src_access_port=" + src_access_port + "&dst_access_sw=" + dst_access_sw + 
                "&dst_access_port=" + dst_access_port;

            s_url = s_url.replace(" ","%20");
            URL url = new URL(s_url);
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();
        }
        catch (Exception e) {
            log.info("insertAssociation exception: ", e);
            return;			
        }
    }
    
    public void updateBytes(String in_sw, String in_port, String src_mac, String dst_mac, String src_ip, String dst_ip, 
            String src_port, String dst_port, short protocol, Long bytes) {
        try {
            String s_url = accessDbUrl + "/update_bytes?src_mac=" + src_mac + "&dst_mac=" + dst_mac + 
                "&src_ip=" + src_ip + "&dst_ip=" + dst_ip + "&src_port=" + src_port + "&dst_port=" + dst_port + 
                "&protocol=" + protocol + "&in_sw=" + in_sw + "&in_port=" + in_port + "&bytes=" + bytes;

            s_url = s_url.replace(" ","%20");
            URL url = new URL(s_url);
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();
        }
        catch (Exception e) {
            log.info("updateBytes exception: ", e);
            return;			
        }
    }

    private String macToUser(String mac) {
        try {
            URL url = new URL(accessDbUrl + "/macToUser?mac=" + mac);	
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();

            if(!inputLine.equals("empty")) {
                JSONObject j = new JSONObject(inputLine);
                return j.getString("User_ID");
            }
            else {
                return "";
            }
        }
        catch (Exception e) {
            log.info("macToUser exception: ", e);
            return "";			
        }  
    }

    private void updateIp() {
        try {
            URL url = new URL(accessDbUrl + "/query_ip?ip=" + src_ip);
			URLConnection yc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

			String inputLine = in.readLine();
			in.close();

            String nowTime = dateFormat.format(new Date(Long.valueOf(this.time)));

            String s_url;

            if(!inputLine.equals("empty"))
                s_url = accessDbUrl + "/update_ip?ip=" + src_ip + "&mac=" + src_mac;           
            else
                s_url = accessDbUrl + "/insert_ip?ip=" + src_ip + "&mac=" + src_mac;
            
            s_url = s_url.replace(" ","%20");
            url = new URL(s_url);
            yc = url.openConnection();
            in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            
            in.close();
        }
        catch (Exception e) {
            log.info("updateIp exception: ", e);
            return;			
        }
    }

    private void insertMac(String mac) {
        try {
            if(!mac.equalsIgnoreCase(portal_mac) && !mac.equalsIgnoreCase(gateway_mac)) {
                URL url = new URL(accessDbUrl + "/insert_mac?mac=" + mac + "&enable=0");
                URLConnection yc = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

                String inputLine = in.readLine();
                in.close();
            }
        }
        catch (Exception e) {
            log.info("insertMac exception: ", e);
            return;			
        }
    }

    private boolean macExist(String mac) {
        try {
            URL url = new URL(accessDbUrl + "/query_mac?mac=" + mac);
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();

            if(!inputLine.equals("empty")) {	
                JSONObject j = new JSONObject(inputLine);
                mac_enable = (j.getInt("Enable") != 0);
                
                return true; 
            }
            else
                insertMac(mac); 

            return false;
        }
        catch (Exception e) {
            log.info("macExist exception: ", e);
            return false;			
        }
    }

    private boolean isMacPass(String mac) {
        if(macExist(mac)) {
            if(mac_enable)
                return true;
        }
        
        return false;
    }
}
