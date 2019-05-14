package net.daporkchop.mcversiondumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.daporkchop.lib.binary.UTF8;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.http.SimpleHTTP;
import net.daporkchop.lib.logging.LogAmount;
import net.daporkchop.lib.logging.Logging;
import sun.misc.IOUtils;
import sun.nio.ch.IOUtil;

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

/**
 * @author DaPorkchop_
 */
public class MCVersionDumper implements Logging {
    protected static final JsonParser parser = new JsonParser();
    protected static final Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public static void main(String... args) throws IOException {
        logger.enableANSI()
                .addFile(new File("output.log"), LogAmount.DEBUG)
                .setLogAmount(LogAmount.DEBUG);

        if (true) {
            logger.info("Fetching MCP mappings...");
            MCPVersions.fetch();
            logger.success("Complete!");
        }

        if (true) {
            logger.info("Fetching Java Edition jars...");
            JavaVersions.fetch();
            logger.success("Complete!");
        }
    }
}
