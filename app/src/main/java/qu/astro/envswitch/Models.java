package qu.astro.envswitch;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Models {
    public enum Type { ENVIRONMENT, VISTA, FOOTPRINT }

    public static final Set<String> PROTECTED_PKGS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "com.meta.shell.env.vista.central",
            "com.meta.shell.env.footprint.haven2025",
            "com.meta.environment.prod.nuxd"
        ))
    );

    public static class EnvApk {
        public final Type type;
        public final String label;
        public final String packageName;
        public String sceneUri;
        public final List<String> apkPaths;
        public final List<String> zipEntries;

        public EnvApk(Type type, String label, String packageName, String sceneUri,
                      List<String> apkPaths, List<String> zipEntries) {
            this.type = type;
            this.label = label;
            this.packageName = packageName;
            this.sceneUri = sceneUri;
            this.apkPaths = apkPaths;
            this.zipEntries = zipEntries;
        }

        public boolean isProtected() {
            return PROTECTED_PKGS.contains(packageName);
        }
    }

    public static class SectionHeader {
        public final String title;
        public SectionHeader(String title) { this.title = title; }
    }
}
