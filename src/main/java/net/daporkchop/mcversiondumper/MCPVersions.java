package net.daporkchop.mcversiondumper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.daporkchop.lib.binary.UTF8;
import net.daporkchop.lib.binary.stream.StreamUtil;
import net.daporkchop.lib.common.function.PFunctions;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.http.SimpleHTTP;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Native;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.daporkchop.lib.logging.Logging.logger;
import static net.daporkchop.mcversiondumper.MCVersionDumper.gson;
import static net.daporkchop.mcversiondumper.MCVersionDumper.parser;

/**
 * @author DaPorkchop_
 */
public class MCPVersions {
    protected static Set<Version> versions = Collections.synchronizedSet(new HashSet<>());
    protected static Set<Version> allVersions = Collections.synchronizedSet(new HashSet<>());

    protected static final File ROOT = new File("mcp");
    protected static final File LOCAL_VERSIONS = new File(ROOT, "versions.json");

    public static void fetch() throws IOException {
        if (LOCAL_VERSIONS.exists()) {
            versions = readVersions(new FileInputStream(LOCAL_VERSIONS));
        }

        allVersions = readVersions(new URL("http://export.mcpbot.bspk.rs/versions.json").openStream());

        allVersions.parallelStream()
                .filter(PFunctions.invert(versions::contains))
                .forEach((IOConsumer<Version>) v -> {
                    logger.info("Downloading %s...", v);

                    File root = new File(ROOT, String.format("%s/%s_%s", v.mc, v.type, v.version));
                    if (!root.exists() && !root.mkdirs())   {
                        throw new IllegalStateException();
                    }

                    try (OutputStream out = new FileOutputStream(new File(root, "mappings.zip")))   {
                        out.write(SimpleHTTP.get(v.getURL()));
                    }

                    try (ZipFile file = new ZipFile(new File(root, "mappings.zip")))    {
                        for (Enumeration<ZipArchiveEntry> it = file.getEntries(); it.hasMoreElements();) {
                            ZipArchiveEntry entry = it.nextElement();
                            byte[] b = new byte[(int) entry.getSize()];
                            try (InputStream in = file.getInputStream(entry))   {
                                StreamUtil.read(in, b, 0, b.length);
                            }
                            try (OutputStream out = new FileOutputStream(new File(root, entry.getName())))  {
                                out.write(b);
                            }
                        }
                    }

                    versions.add(v);
                });

        JsonObject obj = new JsonObject();
        for (Version version : versions)    {
            if (!obj.has(version.mc))   {
                obj.add(version.mc, new JsonObject());
            }
            JsonObject mc = obj.getAsJsonObject(version.mc);
            if (!mc.has(version.type))  {
                mc.add(version.type, new JsonArray());
            }
            mc.getAsJsonArray(version.type).add(version.version);
        }
        try (OutputStream out = new FileOutputStream(LOCAL_VERSIONS))   {
            out.write(gson.toJson(obj).getBytes(UTF8.utf8));
        }
    }

    protected static Set<Version> readVersions(InputStream in) throws IOException  {
        try (Reader reader = new BufferedReader(new InputStreamReader(in))) {
            return Collections.synchronizedSet(parser.parse(reader).getAsJsonObject().entrySet().stream()
                    .flatMap(entry -> {
                        String mc = entry.getKey();
                        return entry.getValue().getAsJsonObject().entrySet().stream()
                                .flatMap(entry1 -> {
                                    String type = entry1.getKey();
                                    return StreamSupport.stream(entry1.getValue().getAsJsonArray().spliterator(), false)
                                            .map(JsonElement::getAsString)
                                            .map(version -> new Version(mc, type, version));
                                });
                    })
                    .collect(Collectors.toSet()));
        }
    }

    protected static class Version implements Comparable<Version> {
        protected final String mc;
        protected final String type;
        protected final String version;

        public Version(String mc, String type, String version) {
            this.mc = mc;
            this.type = type;
            this.version = version;
        }

        @Override
        public int hashCode() {
            return (this.mc.hashCode() * 31 + this.type.hashCode()) * 31 + this.version.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Version) {
                Version v = (Version) obj;
                return this.mc.equals(v.mc) && this.type.equals(v.type) && this.version.equals(v.version);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return String.format("%s_%s-%s", this.type, this.version, this.mc);
        }

        @Override
        public int compareTo(Version o) {
            return this.toString().compareTo(o.toString());
        }

        public String getURL()  {
            return String.format(
                    "http://export.mcpbot.bspk.rs/mcp_%s/%s-%s/mcp_%1$s-%2$s-%3$s.zip",
                    this.type,
                    this.version,
                    this.mc
            );
        }
    }
}
