package qu.astro.envswitch;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends ComponentActivity {
    private static final String STOCK_VISTA_URI = "apk://com.meta.shell.env.vista.central/assets/scene.zip";
    private static final String STOCK_FOOTPRINT_URI = "apk://com.meta.shell.env.footprint.haven2025/assets/scene.zip";
    private static final String STOCK_ENV_URI = "apk://com.meta.shell.env.footprint.haven2025/assets/scene.zip";
    private static final String FALLBACK_ENV_URI = "apk://com.meta.environment.prod.nuxd/assets/scene.zip";

    private RecyclerView recycler;
    private EnvAdapter adapter;
    private TextView txtStatus, txtScene, txtVista, txtFootprint;
    private Button btnRefresh, btnRoot, btnDefaults, btnRestart;
    private Button tabEnvironments, tabVistas, tabFootprints;

    private String ocPrefsBin = null;
    private String currentScene = null;
    private String currentVista = null;
    private String currentFootprint = null;

    private List<Models.EnvApk> allEnvs = new ArrayList<>();
    private List<Models.EnvApk> allVistas = new ArrayList<>();
    private List<Models.EnvApk> allFootprints = new ArrayList<>();
    private Models.Type activeTab = Models.Type.ENVIRONMENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recycler = findViewById(R.id.recycler);
        txtStatus = findViewById(R.id.txtStatus);
        txtScene = findViewById(R.id.txtScene);
        txtVista = findViewById(R.id.txtVista);
        txtFootprint = findViewById(R.id.txtFootprint);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnRoot = findViewById(R.id.btnRoot);
        btnDefaults = findViewById(R.id.btnDefaults);
        btnRestart = findViewById(R.id.btnRestart);
        tabEnvironments = findViewById(R.id.tabEnvironments);
        tabVistas = findViewById(R.id.tabVistas);
        tabFootprints = findViewById(R.id.tabFootprints);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EnvAdapter(new ArrayList<>(), getPackageManager(), new EnvAdapter.Callbacks() {
            @Override
            public void onChooseZip(Models.EnvApk item, TextView sceneView) {
                chooseZip(item, sceneView);
            }

            @Override
            public void onApply(Models.EnvApk item, TextView stateView, TextView sceneView) {
                String uri = sceneView.getText().toString();
                applyByType(item, uri, stateView);
            }

            @Override
            public void onUninstall(Models.EnvApk item, TextView stateView) {
                uninstall(item, stateView);
            }

            @Override
            public void onApplyAsEnv(Models.EnvApk item, TextView stateView, TextView sceneView) {
                String uri = sceneView.getText().toString();
                applyAsEnv(item, uri, stateView);
            }

            @Override
            public void onSetDefault(Models.EnvApk item, TextView stateView, TextView sceneView) {
                String uri = sceneView.getText().toString();
                setDefaultOnly(item, uri, stateView);
            }
        });
        recycler.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> scan());
        btnRoot.setOnClickListener(v -> checkRoot());
        btnDefaults.setOnClickListener(v -> setDefaults());
        btnRestart.setOnClickListener(v -> restartVrs());
        tabEnvironments.setOnClickListener(v -> selectTab(Models.Type.ENVIRONMENT));
        tabVistas.setOnClickListener(v -> selectTab(Models.Type.VISTA));
        tabFootprints.setOnClickListener(v -> selectTab(Models.Type.FOOTPRINT));

        checkRoot();
    }

    private void checkRoot() {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean root = RootShell.isRootAvailable();
            String resolved = root ? RootShell.resolveOculusPrefs() : null;
            ocPrefsBin = resolved;
            runOnUiThread(() -> {
                if (!root) {
                    Toast.makeText(this, "Root (su) not available", Toast.LENGTH_LONG).show();
                    txtStatus.setText("Root missing — Magisk required");
                } else if (resolved == null) {
                    Toast.makeText(this, "oculuspreferences not found", Toast.LENGTH_LONG).show();
                    txtStatus.setText("oculuspreferences not found");
                } else {
                    txtStatus.setText("Found: " + resolved);
                    readAllCurrent();
                    scan();
                }
            });
        });
    }

    private void readAllCurrent() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (ocPrefsBin == null) return;
            String sc = RootShell.getCurrentSelected(ocPrefsBin);
            String vs = RootShell.getCurrentVista(ocPrefsBin);
            String fp = RootShell.getCurrentFootprint(ocPrefsBin);
            runOnUiThread(() -> {
                currentScene = sc;
                currentVista = vs;
                currentFootprint = fp;
                updateStatus();
            });
        });
    }

    private void selectTab(Models.Type type) {
        tabEnvironments.setSelected(type == Models.Type.ENVIRONMENT);
        tabVistas.setSelected(type == Models.Type.VISTA);
        tabFootprints.setSelected(type == Models.Type.FOOTPRINT);
        switch (type) {
            case VISTA:     adapter.update(allVistas);    break;
            case FOOTPRINT: adapter.update(allFootprints); break;
            default:        adapter.update(allEnvs);       break;
        }
    }

    private void updateStatus() {
        txtScene.setText(currentScene != null ? currentScene : "(unknown)");
        txtVista.setText(currentVista != null ? currentVista : "(unknown)");
        txtFootprint.setText(currentFootprint != null ? currentFootprint : "(unknown)");
    }

    private void scan() {
        txtStatus.setText("Scanning…");
        Executors.newSingleThreadExecutor().execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            List<ApplicationInfo> candidates = new ArrayList<>();

            for (ApplicationInfo ai : apps) {
                String pn = ai.packageName;
                if (pn.contains(".environment.") || pn.contains(".env.vista.")
                        || pn.contains(".env.footprint.") || pn.startsWith("com.env.")) {
                    candidates.add(ai);
                }
            }
            candidates.sort((a, b) -> pm.getApplicationLabel(a).toString()
                    .compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

            List<Models.EnvApk> envs = new ArrayList<>();
            List<Models.EnvApk> vistas = new ArrayList<>();
            List<Models.EnvApk> footprints = new ArrayList<>();

            for (ApplicationInfo ai : candidates) {
                List<String> paths = new ArrayList<>();
                if (ai.sourceDir != null) paths.add(ai.sourceDir);
                if (ai.publicSourceDir != null && !ai.publicSourceDir.equals(ai.sourceDir))
                    paths.add(ai.publicSourceDir);
                if (ai.splitPublicSourceDirs != null)
                    Collections.addAll(paths, ai.splitPublicSourceDirs);

                List<String> zips = new ArrayList<>();
                for (String p : paths) {
                    zips.addAll(listZips(p));
                }
                if (zips.isEmpty()) continue;

                String label = pm.getApplicationLabel(ai).toString();
                String pkg = ai.packageName;
                String defaultUri = "apk://" + pkg + "/assets/scene.zip";

                Models.EnvApk item = new Models.EnvApk(detectType(pkg), label, pkg, defaultUri, paths, zips);

                switch (item.type) {
                    case VISTA:
                        vistas.add(item);
                        break;
                    case FOOTPRINT:
                        footprints.add(item);
                        break;
                    default:
                        envs.add(item);
                        break;
                }
            }

            runOnUiThread(() -> {
                allEnvs = envs;
                allVistas = vistas;
                allFootprints = footprints;
                int total = envs.size() + vistas.size() + footprints.size();
                txtStatus.setText(envs.size() + " env, " + vistas.size() + " vista"
                        + (vistas.size() != 1 ? "s" : "")
                        + ", " + footprints.size() + " fprint");

                // refresh current tab
                switch (activeTab) {
                    case VISTA:     adapter.update(allVistas);    break;
                    case FOOTPRINT: adapter.update(allFootprints); break;
                    default:        adapter.update(allEnvs);       break;
                }
                Toast.makeText(this, "Found " + total + " APKs", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private Models.Type detectType(String pkg) {
        if (pkg.contains(".env.vista.")) return Models.Type.VISTA;
        if (pkg.contains(".env.footprint.")) return Models.Type.FOOTPRINT;
        return Models.Type.ENVIRONMENT;
    }

    private List<String> listZips(String apkPath) {
        List<String> out = new ArrayList<>();
        try (ZipFile zip = new ZipFile(apkPath)) {
            Enumeration<? extends ZipEntry> e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                String name = ze.getName();
                if (name != null && name.toLowerCase().endsWith(".zip")) {
                    out.add(name);
                }
            }
        } catch (Throwable ignore) {}
        return out;
    }

    private void chooseZip(Models.EnvApk item, TextView sceneView) {
        if (item.zipEntries == null || item.zipEntries.isEmpty()) return;
        String[] entries = item.zipEntries.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Choose ZIP in " + item.packageName)
                .setItems(entries, (d, which) -> {
                    String selected = entries[which];
                    String uri = "apk://" + item.packageName + "/" + selected;
                    item.sceneUri = uri;
                    sceneView.setText(uri);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyByType(Models.EnvApk item, String uri, TextView stateView) {
        if (ocPrefsBin == null) return;
        stateView.setText("Applying…");
        Executors.newSingleThreadExecutor().execute(() -> {
            StringBuilder diag = new StringBuilder();
            boolean ok;

            switch (item.type) {
                case ENVIRONMENT:
                    ok = applyEnvironmentKeys(uri, diag);
                    break;
                case VISTA:
                    ok = applyVistaKeys(uri, diag);
                    break;
                case FOOTPRINT:
                    ok = applyFootprintKey(uri, diag);
                    break;
                default:
                    ok = false;
                    diag.append("Unknown type");
                    break;
            }

            boolean finalOk = ok;
            runOnUiThread(() -> {
                if (finalOk) {
                    stateView.setText("Applied");
                    readAllCurrent();
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                } else {
                    stateView.setText("Failed");
                    String msg = diag.length() == 0 ? "Unknown error" : diag.toString();
                    Toast.makeText(this, msg.length() > 400 ? msg.substring(0, 400) : msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void applyAsEnv(Models.EnvApk item, String uri, TextView stateView) {
        if (ocPrefsBin == null) return;
        stateView.setText("Applying as Env…");
        Executors.newSingleThreadExecutor().execute(() -> {
            StringBuilder diag = new StringBuilder();
            boolean ok = applyEnvironmentKeys(uri, diag);
            if (item.type == Models.Type.FOOTPRINT) {
                if (!applyFootprintKey(uri, diag)) ok = false;
            }

            boolean finalOk = ok;
            runOnUiThread(() -> {
                if (finalOk) {
                    stateView.setText("Applied as Env");
                    readAllCurrent();
                    Toast.makeText(this, "Set as environment", Toast.LENGTH_SHORT).show();
                } else {
                    stateView.setText("Failed");
                    String msg = diag.length() == 0 ? "Unknown error" : diag.toString();
                    Toast.makeText(this, msg.length() > 400 ? msg.substring(0, 400) : msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void setDefaultOnly(Models.EnvApk item, String uri, TextView stateView) {
        if (ocPrefsBin == null) return;
        stateView.setText("Setting default…");
        Executors.newSingleThreadExecutor().execute(() -> {
            String[] rEnv = RootShell.setDefault(ocPrefsBin, uri);
            String[] rFoot = RootShell.setDefaultFootprint(ocPrefsBin, uri);
            boolean ok = "0".equals(rEnv[0]) && "0".equals(rFoot[0]);
            runOnUiThread(() -> {
                if (ok) {
                    stateView.setText("Default set");
                    readAllCurrent();
                    Toast.makeText(this, "environment_default + default_footprint updated", Toast.LENGTH_SHORT).show();
                } else {
                    stateView.setText("Failed");
                }
            });
        });
    }

    private boolean applyEnvironmentKeys(String uri, StringBuilder diag) {
        boolean ok = true;
        String[] r;

        r = RootShell.setSelected(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("environment_selected failed\n"); ok = false; }

        r = RootShell.setDefault(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("environment_default failed\n"); ok = false; }

        r = RootShell.setResolvedEnvironment(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("resolved_environment failed\n"); ok = false; }

        return ok;
    }

    private boolean applyVistaKeys(String uri, StringBuilder diag) {
        boolean ok = true;
        String[] r;

        r = RootShell.setDefaultVista(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("default_vista failed\n"); ok = false; }

        r = RootShell.setResolvedVista(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("resolved_vista failed\n"); ok = false; }

        r = RootShell.setEnvironmentVistaSelected(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("environment_vista_selected failed\n"); ok = false; }

        return ok;
    }

    private boolean applyFootprintKey(String uri, StringBuilder diag) {
        String[] r = RootShell.setDefaultFootprint(ocPrefsBin, uri);
        if (!"0".equals(r[0])) { diag.append("default_footprint failed\n"); return false; }
        return true;
    }

    private void setDefaults() {
        if (ocPrefsBin == null) return;
        txtStatus.setText("Resetting defaults…");
        Executors.newSingleThreadExecutor().execute(() -> {
            String envUri = isPackageInstalled("com.meta.shell.env.footprint.haven2025")
                    ? STOCK_ENV_URI : FALLBACK_ENV_URI;

            StringBuilder diag = new StringBuilder();
            boolean ok = applyEnvironmentKeys(envUri, diag)
                      & applyVistaKeys(STOCK_VISTA_URI, diag)
                      & applyFootprintKey(STOCK_FOOTPRINT_URI, diag);

            boolean finalOk = ok;
            runOnUiThread(() -> {
                if (finalOk) {
                    txtStatus.setText("Defaults restored");
                    Toast.makeText(this, "Env + Vista + Footprint reset to stock", Toast.LENGTH_SHORT).show();
                } else {
                    txtStatus.setText("Reset failed");
                    String msg = diag.length() == 0 ? "Unknown error" : diag.toString();
                    Toast.makeText(this, msg.length() > 400 ? msg.substring(0, 400) : msg, Toast.LENGTH_LONG).show();
                }
                readAllCurrent();
            });
        });
    }

    private void uninstall(Models.EnvApk item, TextView stateView) {
        if (Models.PROTECTED_PKGS.contains(item.packageName)) {
            Toast.makeText(this, "Protected: cannot uninstall " + item.label, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Uninstall " + item.label + "?")
                .setMessage("This will uninstall " + item.packageName + " via root.\nProceed?")
                .setPositiveButton("Uninstall", (d, which) -> {
                    stateView.setText("Uninstalling…");
                    Executors.newSingleThreadExecutor().execute(() -> {
                        String[] r = RootShell.uninstallPackage(item.packageName);
                        runOnUiThread(() -> {
                            if ("0".equals(r[0]) || r[1].contains("Success")) {
                                stateView.setText("Uninstalled");
                                Toast.makeText(this, "Uninstalled " + item.packageName, Toast.LENGTH_SHORT).show();
                                scan();
                            } else {
                                stateView.setText("Failed");
                                String msg = r[1].length() > 400 ? r[1].substring(0, 400) : r[1];
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void restartVrs() {
        if (ocPrefsBin == null) return;
        txtStatus.setText("Restarting VRS…");
        Executors.newSingleThreadExecutor().execute(() -> {
            String[] r = RootShell.restartVrShell();
            runOnUiThread(() -> {
                if ("0".equals(r[0])) {
                    txtStatus.setText("VRS restarted");
                    Toast.makeText(this, "VRS restarted", Toast.LENGTH_SHORT).show();
                } else {
                    txtStatus.setText("Restart failed");
                    Toast.makeText(this, r[1].length() > 400 ? r[1].substring(0, 400) : r[1], Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void copyCmd(String uri) {
        if (ocPrefsBin == null) return;
        String cmd = ocPrefsBin + " --setc environment_selected \"" + uri + "\"";
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("cmd", cmd));
        Toast.makeText(this, "Copied: " + cmd, Toast.LENGTH_SHORT).show();
    }
}
