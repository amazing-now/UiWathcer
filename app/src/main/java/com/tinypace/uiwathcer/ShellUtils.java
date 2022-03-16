package com.tinypace.uiwathcer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShellUtils {

    public static String execCmd(String cmd) {
        return execCmd(cmd, new String[]{});
    }

    public static String execCmd(String cmd, String contain) {
        return execCmd(cmd, new String[]{contain});
    }

    public static String execCmd(String cmd, String[] contains) {
        String result = null;
        Process p = null;
        BufferedReader reader = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf8"));
            String line;
            boolean isContain = false;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().equals("")) {
                    if (contains != null) {
                        for (String s : contains) {
                            if (line.contains(s)) {
                                result = line;
                                isContain = true;
                                break;
                            }
                        }
                    } else {
                        result += line;
                    }
                }
                if (isContain) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (p != null) {
                p.destroy();
            }
        }
        return result;
    }


    public static String execCmdAnd(String cmd, String[] and) {
        String result = null;
        Process p = null;
        BufferedReader reader = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf8"));
            String line;
            while ((line = reader.readLine()) != null) {
                int count = 0;
                if (!line.trim().equals("")) {
                    if (and != null) {
                        for (String s : and) {
                            if (line.contains(s)) {
                                result = line;
                                count++;
                            }
                        }
                    } else {
                        result += line;
                    }
                }
                if (and != null && count == and.length) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (p != null) {
                p.destroy();
            }
        }
        return result;
    }


    public static boolean fastExecCmd(String cmd) {
        boolean isSuccess = false;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return isSuccess;
    }

}
