package think.rpgitems.utils.nms;

import org.bukkit.Bukkit;
import think.rpgitems.utils.nms.legacy.LegacyEntityTools;
import think.rpgitems.utils.nms.legacy.LegacyNBTTagTools;
import think.rpgitems.utils.nms.legacy.LegacyStackTools;
import think.rpgitems.utils.nms.v1_17_R1.EntityTools_v1_17_R1;
import think.rpgitems.utils.nms.v1_17_R1.NBTTagTools_v1_17_R1;
import think.rpgitems.utils.nms.v1_17_R1.StackTools_v1_17_R1;
import think.rpgitems.utils.nms.v1_18_R1.EntityTools_v1_18_R1;
import think.rpgitems.utils.nms.v1_18_R1.NBTTagTools_v1_18_R1;
import think.rpgitems.utils.nms.v1_18_R1.StackTools_v1_18_R1;
import think.rpgitems.utils.nms.v1_18_R2.EntityTools_v1_18_R2;
import think.rpgitems.utils.nms.v1_18_R2.NBTTagTools_v1_18_R2;
import think.rpgitems.utils.nms.v1_18_R2.StackTools_v1_18_R2;
import think.rpgitems.utils.nms.v1_19_R3.EntityTools_v1_19_R3;
import think.rpgitems.utils.nms.v1_19_R3.NBTTagTools_v1_19_R3;
import think.rpgitems.utils.nms.v1_19_R3.StackTools_v1_19_R3;
import think.rpgitems.utils.nms.v1_20_R1.EntityTools_v1_20_R1;
import think.rpgitems.utils.nms.v1_20_R1.NBTTagTools_v1_20_R1;
import think.rpgitems.utils.nms.v1_20_R1.StackTools_v1_20_R1;
import think.rpgitems.utils.nms.v1_20_R2.EntityTools_v1_20_R2;
import think.rpgitems.utils.nms.v1_20_R2.NBTTagTools_v1_20_R2;
import think.rpgitems.utils.nms.v1_20_R2.StackTools_v1_20_R2;
import think.rpgitems.utils.nms.v1_20_R3.EntityTools_v1_20_R3;
import think.rpgitems.utils.nms.v1_20_R3.NBTTagTools_v1_20_R3;
import think.rpgitems.utils.nms.v1_20_R3.StackTools_v1_20_R3;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class NMS {
    Logger logger;
    IStackTools stackTools;
    IEntityTools entityTools;
    INBTTagTools nbtTools;
    private static String versionString;
    private static NMS inst = null;
    private final Map<String, Runnable> versions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER) {{
        put("v1_17_R1", () -> {
            stackTools = new StackTools_v1_17_R1();
            entityTools = new EntityTools_v1_17_R1();
            nbtTools = new NBTTagTools_v1_17_R1();
        });
        put("v1_18_R1", () -> {
            stackTools = new StackTools_v1_18_R1();
            entityTools = new EntityTools_v1_18_R1();
            nbtTools = new NBTTagTools_v1_18_R1();
        });
        put("v1_18_R2", () -> {
            stackTools = new StackTools_v1_18_R2();
            entityTools = new EntityTools_v1_18_R2();
            nbtTools = new NBTTagTools_v1_18_R2();
        });
        put("v1_19_R3", () -> {
            stackTools = new StackTools_v1_19_R3();
            entityTools = new EntityTools_v1_19_R3();
            nbtTools = new NBTTagTools_v1_19_R3();
        });
        put("v1_20_R1", () -> {
            stackTools = new StackTools_v1_20_R1();
            entityTools = new EntityTools_v1_20_R1();
            nbtTools = new NBTTagTools_v1_20_R1();
        });
        put("v1_20_R2", () -> {
            stackTools = new StackTools_v1_20_R2();
            entityTools = new EntityTools_v1_20_R2();
            nbtTools = new NBTTagTools_v1_20_R2();
        });
        put("v1_20_R3", () -> {
            stackTools = new StackTools_v1_20_R3();
            entityTools = new EntityTools_v1_20_R3();
            nbtTools = new NBTTagTools_v1_20_R3();
        });
    }};
    private NMS(Logger logger) {
        this.logger = logger;
    }

    public static boolean init(Logger logger) {
        if (inst != null) return true;
        inst = new NMS(logger);
        return inst.init();
    }
    public boolean init() {
        try {
            String v = getVersion();
            Runnable init = versions.get(v);
            if (init != null) {
                init.run();
                logger.info("Your server version " + v + " is supported! Enjoy!");
            } else {
                logger.warning("Your server version " + v + " is not supported!");
                logger.warning("Now you are running in Legacy Mode. Some functions may not work.");
                stackTools = new LegacyStackTools();
                entityTools = new LegacyEntityTools();
                nbtTools = new LegacyNBTTagTools(); // TODO: 找个库来处理 nbt
            }
            return init != null;
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                t.printStackTrace(pw);
            }
            logger.warning(sw.toString());
        }
        return false;
    }

    public static IStackTools stackTools() {
        return inst.stackTools;
    }

    public static IEntityTools entityTools() {
        return inst.entityTools;
    }

    public static INBTTagTools nbtTools() {
        return inst.nbtTools;
    }

    public static boolean starts(String v, String... versions) {
        for (String s : versions) {
            if (v.startsWith(s)) return true;
        }
        return false;
    }
    public static boolean equals(String v, String... versions) {
        for (String s : versions) {
            if (v.equals(s)) return true;
        }
        return false;
    }

    public static String getVersion() {
        if (versionString == null) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            versionString = name.substring(name.lastIndexOf('.') + 1);
        }

        return versionString;
    }
}