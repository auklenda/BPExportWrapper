/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aea.auklend;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 *
 * @author Alf E. Auklend
 */
public class AALogger {
    static int DEBUG = 1;
    static int INFO = 2;
    static int ERROR = 3;
    static int WARNING = 4;
    static boolean [] levels = {true,true,true,true,true}; 
    static PrintStream pw = System.out;
    public AALogger() {
        
    }

    public static void setPw(PrintStream pw) {
        AALogger.pw = pw;
    }
    public static void setDebug(boolean b) {
        levels[DEBUG] = b;
    }
    public static String getmyDate() {
      return(getDateAndTime("yyyy-MM-dd"));
    }
   public static String getDateAndTime() {
     return(getDateAndTime("yyyy-MM-dd HH:mm:ss"));
   }
   
   public static String getDateAndTime(String frm) {
     Date now = new Date(System.currentTimeMillis());
     String formatStr = frm;
     SimpleDateFormat formatter = new SimpleDateFormat(formatStr);
     return (formatter.format(now));
   }
   public static void logInfo(String log) {
       logInfo(null,log);
   }
   public static void logInfo(String log,String info) {
       if (pw == null)
           pw = System.out;
       if (levels[INFO]) {
           StringBuilder sb = new StringBuilder();
           sb.append(getDateAndTime()).append(" INFO ");
           if (log != null) {
               sb.append(log).append("  ");
           }
           sb.append(info);
           pw.println(sb.toString());
       }
   }
   public static void logDebug(String log) {
       logDebug(null,log);
   }
   public static void logDebug(String log, String info) {
       if (levels[DEBUG]) {
           StringBuilder sb = new StringBuilder();
           sb.append(getDateAndTime()).append("  ");
           if (log != null) {
               sb.append(log).append(" DEBUG ");
           }
           sb.append(info);
           pw.println(sb.toString());
       }
   }
   public static void logError(String log, String info) {
       if (levels[ERROR]) {
           StringBuilder sb = new StringBuilder();
           sb.append(getDateAndTime()).append(" ERROR ");
           if (log != null) {
               sb.append(log).append("  ");
           }
           sb.append(info);
           pw.println(sb.toString());
       }
   }   
   public static void logEnter(String log, String info) {
       logDebug(log,"  entered " + info);
   }
   public static void logLeaving(String log, String info) {
       logDebug(log,"  leaving " + info);
   }
   
}
