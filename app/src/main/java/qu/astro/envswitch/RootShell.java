package qu.astro.envswitch;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootShell {
    private static final String[] CANDIDATES = new String[] { "oculuspreferences" };

    public static boolean isRootAvailable() {
        try {
            Process p = new ProcessBuilder("su", "-c", "id").start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String[] runAsRoot(String cmd) {
        try {
            Process p = new ProcessBuilder("su", "-c", cmd).start();
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = out.readLine()) != null) {
                sb.append(line).append("\n");
            }
            while ((line = err.readLine()) != null) {
                sb.append("ERR: ").append(line).append("\n");
            }
            int code = p.waitFor();
            return new String[] { String.valueOf(code), sb.toString() };
        } catch (Exception e) {
            return new String[] { "-1", "Exception: " + e.getMessage() };
        }
    }

    public static String resolveOculusPrefs() {
        for (String bin : CANDIDATES) {
            String[] r = runAsRoot(bin + " --help");
            if ("0".equals(r[0]))
                return bin;
        }
        return null;
    }

    public static String getPref(String bin, String key) {
        String[] r = runAsRoot(bin + " --getc " + key);
        String out = r[1];
        int s = out.indexOf("apk://");
        if (s >= 0) {
            int e = out.indexOf('"', s);
            if (e > s)
                return out.substring(s, e);
        }
        return null;
    }

    public static String getCurrentSelected(String bin) {
        return getPref(bin, "environment_selected");
    }

    public static String getCurrentVista(String bin) {
        return getPref(bin, "environment_vista_selected");
    }

    public static String getCurrentFootprint(String bin) {
        return getPref(bin, "default_footprint");
    }

    public static String getResolvedEnvironment(String bin) {
        return getPref(bin, "resolved_environment");
    }

    public static String getResolvedVista(String bin) {
        return getPref(bin, "resolved_vista");
    }

    public static String getDefaultVista(String bin) {
        return getPref(bin, "default_vista");
    }

    public static String getDefaultFootprint(String bin) {
        return getPref(bin, "default_footprint");
    }

    public static String getEnvironmentDefault(String bin) {
        return getPref(bin, "environment_default");
    }

    // --- setters ---

    public static String[] setSelected(String bin, String uri) {
        return runAsRoot(bin + " --setc environment_selected \"" + uri + "\"");
    }

    public static String[] setResolvedEnvironment(String bin, String uri) {
        return runAsRoot(bin + " --setc resolved_environment \"" + uri + "\"");
    }

    public static String[] setDefault(String bin, String uri) {
        return runAsRoot(bin + " --setc environment_default \"" + uri + "\"");
    }

    public static String[] setDefaultVista(String bin, String uri) {
        return runAsRoot(bin + " --setc default_vista \"" + uri + "\"");
    }

    public static String[] setResolvedVista(String bin, String uri) {
        return runAsRoot(bin + " --setc resolved_vista \"" + uri + "\"");
    }

    public static String[] setEnvironmentVistaSelected(String bin, String uri) {
        return runAsRoot(bin + " --setc environment_vista_selected \"" + uri + "\"");
    }

    public static String[] setDefaultFootprint(String bin, String uri) {
        return runAsRoot(bin + " --setc default_footprint \"" + uri + "\"");
    }

    public static String[] uninstallPackage(String pkg) {
        return runAsRoot("pm uninstall -k --user 0 " + pkg);
    }

    public static String[] restartVrShell() {
        return runAsRoot("am force-stop com.oculus.vrshell");
    }
}
