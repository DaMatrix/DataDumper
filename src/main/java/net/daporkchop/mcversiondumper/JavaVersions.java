package net.daporkchop.mcversiondumper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.daporkchop.lib.binary.UTF8;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.http.SimpleHTTP;
import net.daporkchop.lib.logging.Logging;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.daporkchop.mcversiondumper.MCVersionDumper.*;

/**
 * @author DaPorkchop_
 */
public class JavaVersions implements Logging {
    protected static Set<String> versions = Collections.synchronizedSet(new HashSet<>());

    protected static final File ROOT = new File("java");
    protected static final File LOCAL_VERSIONS = new File(ROOT, "versions.json");

    public static void fetch() throws IOException   {
        if (LOCAL_VERSIONS.exists()) {
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(LOCAL_VERSIONS)))) {
                versions = Collections.synchronizedSet(StreamSupport.stream(parser.parse(reader).getAsJsonArray().spliterator(), false)
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toSet()));
            }
        }
        JsonArray arr;
        try (Reader reader = new BufferedReader(new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()))) {
            arr = parser.parse(reader).getAsJsonObject().getAsJsonArray("versions");
        }
        logger.trace("Fetched %d versions.", arr.size());
        StreamSupport.stream(arr.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(o -> !versions.contains(o.get("id").getAsString()))
                .forEach((IOConsumer<JsonObject>) o -> {
                    String id = o.get("id").getAsString();
                    File root = new File(ROOT, id);
                    if (!root.exists() && !root.mkdirs()) {
                        throw new IllegalStateException(root.getAbsolutePath());
                    }
                    logger.info("Downloading %s...", id);
                    JsonObject downloads;

                    File versionJson = new File(ROOT, String.format("%s/version.json", id));
                    try (OutputStream out = new FileOutputStream(versionJson, false)) {
                        byte[] b = SimpleHTTP.get(o.get("url").getAsString());
                        out.write(b);
                        downloads = parser.parse(new InputStreamReader(new ByteArrayInputStream(b))).getAsJsonObject().getAsJsonObject("downloads");
                    }

                    for (String s : new String[]{"client", "server"})   {
                        if (!downloads.has(s))  {
                            continue;
                        }
                        logger.info("Downloading %s for %s...", s, id);
                        File file = new File(ROOT, String.format("%s/%s.jar", id, s));
                        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, false)))    {
                            out.write(SimpleHTTP.get(downloads.getAsJsonObject(s).get("url").getAsString()));
                        }
                    }

                    versions.add(id);
                    logger.success("%s complete.", id);
                });

        try (OutputStream out = new FileOutputStream(LOCAL_VERSIONS, false)) {
            out.write(gson.toJson(versions.stream()
                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll)
            ).getBytes(UTF8.utf8));
        }
    }
}
