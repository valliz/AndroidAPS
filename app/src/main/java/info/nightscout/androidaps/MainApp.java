package info.nightscout.androidaps;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.SystemClock;

import androidx.annotation.PluralsRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import net.danlew.android.joda.JodaTimeAndroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import info.nightscout.androidaps.data.ConstraintChecker;
import info.nightscout.androidaps.database.AppRepository;
import info.nightscout.androidaps.database.entities.Bolus;
import info.nightscout.androidaps.database.entities.BolusCalculatorResult;
import info.nightscout.androidaps.database.entities.Carbs;
import info.nightscout.androidaps.database.entities.ExtendedBolus;
import info.nightscout.androidaps.database.entities.GlucoseValue;
import info.nightscout.androidaps.database.entities.ProfileSwitch;
import info.nightscout.androidaps.database.entities.TemporaryBasal;
import info.nightscout.androidaps.database.entities.TemporaryTarget;
import info.nightscout.androidaps.database.entities.TherapyEvent;
import info.nightscout.androidaps.database.entities.links.MealLink;
import info.nightscout.androidaps.database.interfaces.DBEntry;
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime;
import info.nightscout.androidaps.database.transactions.SaveVersionChangeIfNeededTransaction;
import info.nightscout.androidaps.db.DatabaseHelper;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin;
import info.nightscout.androidaps.plugins.aps.openAPSAMA.OpenAPSAMAPlugin;
import info.nightscout.androidaps.plugins.aps.openAPSMA.OpenAPSMAPlugin;
import info.nightscout.androidaps.plugins.aps.openAPSSMB.OpenAPSSMBPlugin;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.constraints.dstHelper.DstHelperPlugin;
import info.nightscout.androidaps.plugins.constraints.objectives.ObjectivesPlugin;
import info.nightscout.androidaps.plugins.constraints.safety.SafetyPlugin;
import info.nightscout.androidaps.plugins.constraints.signatureVerifier.SignatureVerifierPlugin;
import info.nightscout.androidaps.plugins.constraints.storage.StorageConstraintPlugin;
import info.nightscout.androidaps.plugins.constraints.versionChecker.VersionCheckerPlugin;
import info.nightscout.androidaps.plugins.general.actions.ActionsPlugin;
import info.nightscout.androidaps.plugins.general.automation.AutomationPlugin;
import info.nightscout.androidaps.plugins.general.careportal.CareportalPlugin;
import info.nightscout.androidaps.plugins.general.food.FoodPlugin;
import info.nightscout.androidaps.plugins.general.maintenance.LoggerUtils;
import info.nightscout.androidaps.plugins.general.maintenance.MaintenancePlugin;
import info.nightscout.androidaps.plugins.general.nsclient.NSClientPlugin;
import info.nightscout.androidaps.plugins.general.nsclient.receivers.DBAccessReceiver;
import info.nightscout.androidaps.plugins.general.open_humans.OpenHumansUploader;
import info.nightscout.androidaps.plugins.general.overview.OverviewPlugin;
import info.nightscout.androidaps.plugins.general.persistentNotification.PersistentNotificationPlugin;
import info.nightscout.androidaps.plugins.general.smsCommunicator.SmsCommunicatorPlugin;
import info.nightscout.androidaps.plugins.general.wear.WearPlugin;
import info.nightscout.androidaps.plugins.general.xdripStatusline.StatuslinePlugin;
import info.nightscout.androidaps.plugins.insulin.InsulinOrefFreePeakPlugin;
import info.nightscout.androidaps.plugins.insulin.InsulinOrefRapidActingPlugin;
import info.nightscout.androidaps.plugins.insulin.InsulinOrefUltraRapidActingPlugin;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin;
import info.nightscout.androidaps.plugins.profile.local.LocalProfilePlugin;
import info.nightscout.androidaps.plugins.profile.ns.NSProfilePlugin;
import info.nightscout.androidaps.plugins.profile.simple.SimpleProfilePlugin;
import info.nightscout.androidaps.plugins.pump.combo.ComboPlugin;
import info.nightscout.androidaps.plugins.pump.danaR.DanaRPlugin;
import info.nightscout.androidaps.plugins.pump.danaRKorean.DanaRKoreanPlugin;
import info.nightscout.androidaps.plugins.pump.danaRS.DanaRSPlugin;
import info.nightscout.androidaps.plugins.pump.danaRv2.DanaRv2Plugin;
import info.nightscout.androidaps.plugins.pump.insight.LocalInsightPlugin;
import info.nightscout.androidaps.plugins.pump.mdi.MDIPlugin;
import info.nightscout.androidaps.plugins.pump.medtronic.MedtronicPumpPlugin;
import info.nightscout.androidaps.plugins.pump.virtual.VirtualPumpPlugin;
import info.nightscout.androidaps.plugins.sensitivity.SensitivityAAPSPlugin;
import info.nightscout.androidaps.plugins.sensitivity.SensitivityOref0Plugin;
import info.nightscout.androidaps.plugins.sensitivity.SensitivityOref1Plugin;
import info.nightscout.androidaps.plugins.sensitivity.SensitivityWeightedAveragePlugin;
import info.nightscout.androidaps.plugins.source.SourceDexcomPlugin;
import info.nightscout.androidaps.plugins.source.SourceEversensePlugin;
import info.nightscout.androidaps.plugins.source.SourceGlimpPlugin;
import info.nightscout.androidaps.plugins.source.SourceMM640gPlugin;
import info.nightscout.androidaps.plugins.source.SourceNSClientPlugin;
import info.nightscout.androidaps.plugins.source.SourcePoctechPlugin;
import info.nightscout.androidaps.plugins.source.SourceTomatoPlugin;
import info.nightscout.androidaps.plugins.source.SourceXdripPlugin;
import info.nightscout.androidaps.plugins.treatments.TreatmentService;
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin;
import info.nightscout.androidaps.receivers.DataReceiver;
import info.nightscout.androidaps.receivers.KeepAliveReceiver;
import info.nightscout.androidaps.receivers.NSAlarmReceiver;
import info.nightscout.androidaps.receivers.TimeDateOrTZChangeReceiver;
import info.nightscout.androidaps.services.Intents;
import info.nightscout.androidaps.utils.FabricPrivacy;
import info.nightscout.androidaps.utils.LocaleHelper;
import info.nightscout.androidaps.utils.SP;
import io.fabric.sdk.android.Fabric;
import io.reactivex.schedulers.Schedulers;

import static info.nightscout.androidaps.plugins.constraints.versionChecker.VersionCheckerUtilsKt.triggerCheckVersion;


public class MainApp extends Application {
    private static Logger log = LoggerFactory.getLogger(L.CORE);
    private static KeepAliveReceiver keepAliveReceiver;

    private static MainApp sInstance;
    public static Resources sResources;
    private static UUID applicationId;

    private static FirebaseAnalytics mFirebaseAnalytics;

    private static DatabaseHelper sDatabaseHelper = null;
    private static ConstraintChecker sConstraintsChecker = null;

    private static ArrayList<PluginBase> pluginsList = null;

    private static DataReceiver dataReceiver = new DataReceiver();
    private static NSAlarmReceiver alarmReciever = new NSAlarmReceiver();
    //private static AckAlarmReceiver ackAlarmReciever = new AckAlarmReceiver();
    private static DBAccessReceiver dbAccessReciever = new DBAccessReceiver();
    private LocalBroadcastManager lbm;
    BroadcastReceiver btReceiver;
    TimeDateOrTZChangeReceiver timeDateOrTZChangeReceiver;

    public static boolean devBranch;
    public static boolean engineeringMode;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        sInstance = this;
        AppRepository.INSTANCE.initialize(this);
        String gitRemote = BuildConfig.REMOTE;
        String commitHash = BuildConfig.HEAD;
        if (gitRemote.contains("NoGitSystemAvailable")) {
            gitRemote = null;
            commitHash = null;
        }
        AppRepository.INSTANCE.runTransaction(new SaveVersionChangeIfNeededTransaction(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, gitRemote, commitHash)).subscribe();
        SP.copyMissingValuesToDatabase();
        AppRepository.INSTANCE.getChangeObservable()
                .observeOn(Schedulers.io())
                .subscribe(changes -> {
                    Long earliestDataChange = null;
                    boolean glucoseValuesChanged = false;
                    boolean temporaryBasalsChanged = false;
                    boolean extendedBolusesChanged = false;
                    boolean temporaryTargetChanged = false;
                    boolean treatmentsChanged = false;
                    boolean therapyEventsChanged = false;
                    boolean profileSwitchesChanged = false;
                    for (DBEntry entry : changes) {
                        if (entry instanceof DBEntryWithTime) {
                            if (earliestDataChange == null || earliestDataChange < ((DBEntryWithTime) entry).getTimestamp()) {
                                earliestDataChange = ((DBEntryWithTime) entry).getTimestamp();
                            }
                        }
                        if (entry instanceof GlucoseValue) glucoseValuesChanged = true;
                        else if (entry instanceof TemporaryBasal) temporaryBasalsChanged = true;
                        else if (entry instanceof ExtendedBolus) extendedBolusesChanged = true;
                        else if (entry instanceof TemporaryTarget) temporaryTargetChanged = true;
                        else if (entry instanceof Bolus
                                || entry instanceof Carbs
                                || entry instanceof BolusCalculatorResult
                                || entry instanceof MealLink) treatmentsChanged = true;
                        else if (entry instanceof TherapyEvent) therapyEventsChanged = true;
                        else if (entry instanceof ProfileSwitch) profileSwitchesChanged = true;
                    }
                    if (earliestDataChange != null)
                        DatabaseHelper.updateEarliestDataChange(earliestDataChange);
                    if (glucoseValuesChanged) DatabaseHelper.scheduleBgChange();
                    if (temporaryBasalsChanged) DatabaseHelper.scheduleTemporaryBasalChange();
                    if (extendedBolusesChanged) DatabaseHelper.scheduleExtendedBolusChange();
                    if (temporaryTargetChanged) DatabaseHelper.scheduleTemporaryTargetChange();
                    if (treatmentsChanged) TreatmentService.scheduleTreatmentChange(null);
                    if (therapyEventsChanged) DatabaseHelper.scheduleCareportalEventChange();
                    if (profileSwitchesChanged) DatabaseHelper.scheduleProfileSwitchChange();
                });
        log.debug("onCreate");
        sResources = getResources();
        LocaleHelper.INSTANCE.update(this);
        sConstraintsChecker = new ConstraintChecker();
        sDatabaseHelper = OpenHelperManager.getHelper(sInstance, DatabaseHelper.class);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> log.error("Uncaught exception crashing app", ex));

        try {
            if (FabricPrivacy.fabricEnabled()) {
                Fabric.with(this, new Crashlytics());
            }
        } catch (Exception e) {
            log.error("Error with Fabric init! " + e);
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(!Boolean.getBoolean("disableFirebase"));

        JodaTimeAndroid.init(this);

        log.info("Version: " + BuildConfig.VERSION_NAME);
        log.info("BuildVersion: " + BuildConfig.BUILDVERSION);
        log.info("Remote: " + BuildConfig.REMOTE);

        String extFilesDir = LoggerUtils.getLogDirectory();
        File engineeringModeSemaphore = new File(extFilesDir, "engineering_mode");

        engineeringMode = engineeringModeSemaphore.exists() && engineeringModeSemaphore.isFile();
        devBranch = BuildConfig.VERSION.contains("-") || BuildConfig.VERSION.matches(".*[a-zA-Z]+.*");

        registerLocalBroadcastReceiver();

        //trigger here to see the new version on app start after an update
        triggerCheckVersion();
        //setBTReceiver();

        if (pluginsList == null) {
            pluginsList = new ArrayList<>();
            // Register all tabs in app here
            pluginsList.add(OverviewPlugin.INSTANCE);
            pluginsList.add(IobCobCalculatorPlugin.getPlugin());
            if (!Config.NSCLIENT) pluginsList.add(ActionsPlugin.INSTANCE);
            pluginsList.add(InsulinOrefRapidActingPlugin.getPlugin());
            pluginsList.add(InsulinOrefUltraRapidActingPlugin.getPlugin());
            pluginsList.add(InsulinOrefFreePeakPlugin.getPlugin());
            pluginsList.add(SensitivityOref0Plugin.getPlugin());
            pluginsList.add(SensitivityAAPSPlugin.getPlugin());
            pluginsList.add(SensitivityWeightedAveragePlugin.getPlugin());
            pluginsList.add(SensitivityOref1Plugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(DanaRPlugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(DanaRKoreanPlugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(DanaRv2Plugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(DanaRSPlugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(LocalInsightPlugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(ComboPlugin.getPlugin());
            if (Config.PUMPDRIVERS) pluginsList.add(MedtronicPumpPlugin.getPlugin());
            if (!Config.NSCLIENT) pluginsList.add(MDIPlugin.getPlugin());
            pluginsList.add(VirtualPumpPlugin.getPlugin());
            pluginsList.add(CareportalPlugin.getPlugin());
            if (Config.APS) pluginsList.add(LoopPlugin.getPlugin());
            if (Config.APS) pluginsList.add(OpenAPSMAPlugin.getPlugin());
            if (Config.APS) pluginsList.add(OpenAPSAMAPlugin.getPlugin());
            if (Config.APS) pluginsList.add(OpenAPSSMBPlugin.getPlugin());
            pluginsList.add(NSProfilePlugin.getPlugin());
            if (!Config.NSCLIENT) pluginsList.add(SimpleProfilePlugin.getPlugin());
            if (!Config.NSCLIENT) pluginsList.add(LocalProfilePlugin.getPlugin());
            pluginsList.add(TreatmentsPlugin.getPlugin());
            if (!Config.NSCLIENT) pluginsList.add(SafetyPlugin.getPlugin());
            if (!Config.NSCLIENT) pluginsList.add(VersionCheckerPlugin.INSTANCE);
            if (Config.APS) pluginsList.add(StorageConstraintPlugin.getPlugin());
            if (Config.APS) pluginsList.add(SignatureVerifierPlugin.getPlugin());
            if (Config.APS) pluginsList.add(ObjectivesPlugin.INSTANCE);
            pluginsList.add(SourceXdripPlugin.INSTANCE);
            pluginsList.add(SourceNSClientPlugin.INSTANCE);
            pluginsList.add(SourceMM640gPlugin.INSTANCE);
            pluginsList.add(SourceGlimpPlugin.INSTANCE);
            pluginsList.add(SourceDexcomPlugin.INSTANCE);
            pluginsList.add(SourcePoctechPlugin.INSTANCE);
            pluginsList.add(SourceTomatoPlugin.INSTANCE);
            pluginsList.add(SourceEversensePlugin.INSTANCE);
            if (!Config.NSCLIENT) pluginsList.add(SmsCommunicatorPlugin.getPlugin());
            pluginsList.add(FoodPlugin.getPlugin());

            pluginsList.add(WearPlugin.initPlugin(this));
            pluginsList.add(StatuslinePlugin.initPlugin(this));
            pluginsList.add(PersistentNotificationPlugin.getPlugin());
            pluginsList.add(NSClientPlugin.getPlugin());
            //if (engineeringMode) pluginsList.add(TidepoolPlugin.INSTANCE);
            pluginsList.add(MaintenancePlugin.initPlugin(this));
            pluginsList.add(AutomationPlugin.INSTANCE);

            pluginsList.add(ConfigBuilderPlugin.getPlugin());

            pluginsList.add(DstHelperPlugin.getPlugin());

            if (Config.APS) pluginsList.add(OpenHumansUploader.INSTANCE);


            ConfigBuilderPlugin.getPlugin().initialize();
        }

        //NSUpload.uploadAppStart();

        final PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();
        if (pump != null) {
            new Thread(() -> {
                SystemClock.sleep(5000);
                ConfigBuilderPlugin.getPlugin().getCommandQueue().readStatus("Initialization", null);
                startKeepAliveService();
            }).start();
        }
    }

    public static UUID getApplicationId() {
        if (applicationId == null) {
            String string = SP.getString("applicationId", null);
            if (string == null) {
                applicationId = UUID.randomUUID();
                SP.putString("applicationId", applicationId.toString());
            } else {
                applicationId = UUID.fromString(string);
            }
        }
        return applicationId;
    }

    private void registerLocalBroadcastReceiver() {
        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_TREATMENT));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_CHANGED_TREATMENT));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_REMOVED_TREATMENT));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_FOOD));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_CHANGED_FOOD));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_REMOVED_FOOD));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_SGV));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_PROFILE));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_STATUS));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_MBG));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_DEVICESTATUS));
        lbm.registerReceiver(dataReceiver, new IntentFilter(Intents.ACTION_NEW_CAL));

        //register alarms
        lbm.registerReceiver(alarmReciever, new IntentFilter(Intents.ACTION_ALARM));
        lbm.registerReceiver(alarmReciever, new IntentFilter(Intents.ACTION_ANNOUNCEMENT));
        lbm.registerReceiver(alarmReciever, new IntentFilter(Intents.ACTION_CLEAR_ALARM));
        lbm.registerReceiver(alarmReciever, new IntentFilter(Intents.ACTION_URGENT_ALARM));

        //register ack alarm
        //lbm.registerReceiver(ackAlarmReciever, new IntentFilter(Intents.ACTION_ACK_ALARM));

        //register dbaccess
        //lbm.registerReceiver(dbAccessReciever, new IntentFilter(Intents.ACTION_DATABASE));
        lbm.registerReceiver(dbAccessReciever, new IntentFilter(Intents.ACTION_DATABASE));

        this.timeDateOrTZChangeReceiver = new TimeDateOrTZChangeReceiver();
        this.timeDateOrTZChangeReceiver.registerBroadcasts(this);

    }

    private void startKeepAliveService() {
        if (keepAliveReceiver == null) {
            keepAliveReceiver = new KeepAliveReceiver();
            keepAliveReceiver.setAlarm(this);
        }
    }

    public void stopKeepAliveService() {
        if (keepAliveReceiver != null)
            KeepAliveReceiver.cancelAlarm(this);
    }

    public static String gs(int id) {
        return sResources.getString(id);
    }

    public static String gs(int id, Object... args) {
        return sResources.getString(id, args);
    }

    public static String gq(@PluralsRes int id, int quantity, Object... args) {
        return sResources.getQuantityString(id, quantity, args);
    }

    public static int gc(int id) {
        return sResources.getColor(id);
    }

    public static MainApp instance() {
        return sInstance;
    }

    public static DatabaseHelper getDbHelper() {
        return sDatabaseHelper;
    }

    public static void closeDbHelper() {
        if (sDatabaseHelper != null) {
            sDatabaseHelper.close();
            sDatabaseHelper = null;
        }
    }

    public static FirebaseAnalytics getFirebaseAnalytics() {
        return mFirebaseAnalytics;
    }

    public static ConstraintChecker getConstraintChecker() {
        return sConstraintsChecker;
    }

    public static ArrayList<PluginBase> getPluginsList() {
        return pluginsList;
    }

    public static ArrayList<PluginBase> getSpecificPluginsList(PluginType type) {
        ArrayList<PluginBase> newList = new ArrayList<>();

        if (pluginsList != null) {
            for (PluginBase p : pluginsList) {
                if (p.getType() == type)
                    newList.add(p);
            }
        } else {
            log.error("pluginsList=null");
        }
        return newList;
    }

    public static ArrayList<PluginBase> getSpecificPluginsVisibleInList(PluginType type) {
        ArrayList<PluginBase> newList = new ArrayList<>();

        if (pluginsList != null) {
            for (PluginBase p : pluginsList) {
                if (p.getType() == type)
                    if (p.showInList(type))
                        newList.add(p);
            }
        } else {
            log.error("pluginsList=null");
        }
        return newList;
    }

    public static ArrayList<PluginBase> getSpecificPluginsListByInterface(Class interfaceClass) {
        ArrayList<PluginBase> newList = new ArrayList<>();

        if (pluginsList != null) {
            for (PluginBase p : pluginsList) {
                if (p.getClass() != ConfigBuilderPlugin.class && interfaceClass.isAssignableFrom(p.getClass()))
                    newList.add(p);
            }
        } else {
            log.error("pluginsList=null");
        }
        return newList;
    }

    public static ArrayList<PluginBase> getSpecificPluginsVisibleInListByInterface(Class interfaceClass, PluginType type) {
        ArrayList<PluginBase> newList = new ArrayList<>();

        if (pluginsList != null) {
            for (PluginBase p : pluginsList) {
                if (p.getClass() != ConfigBuilderPlugin.class && interfaceClass.isAssignableFrom(p.getClass()))
                    if (p.showInList(type))
                        newList.add(p);
            }
        } else {
            log.error("pluginsList=null");
        }
        return newList;
    }

    public static boolean isEngineeringModeOrRelease() {
        if (!Config.APS)
            return true;
        return engineeringMode || !devBranch;
    }

    public static boolean isDev() {
        return devBranch;
    }

    public static int getIcon() {
        if (Config.NSCLIENT)
            return R.mipmap.ic_yellowowl;
        else if (Config.PUMPCONTROL)
            return R.mipmap.ic_pumpcontrol;
        else
            return R.mipmap.ic_launcher;
    }

    public static int getNotificationIcon() {
        if (Config.NSCLIENT)
            return R.drawable.ic_notif_nsclient;
        else if (Config.PUMPCONTROL)
            return R.drawable.ic_notif_pumpcontrol;
        else
            return R.drawable.ic_notif_aaps;
    }

    @Override
    public void onTerminate() {
        if (L.isEnabled(L.CORE))
            log.debug("onTerminate");
        super.onTerminate();
        if (sDatabaseHelper != null) {
            sDatabaseHelper.close();
            sDatabaseHelper = null;
        }

        if (btReceiver != null) {
            unregisterReceiver(btReceiver);
        }

        if (timeDateOrTZChangeReceiver != null) {
            unregisterReceiver(timeDateOrTZChangeReceiver);
        }

    }

    public static int dpToPx(int dp) {
        float scale = sResources.getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
