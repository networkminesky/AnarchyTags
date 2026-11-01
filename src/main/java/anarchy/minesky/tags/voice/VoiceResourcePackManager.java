package anarchy.minesky.tags.voice;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.util.FoliaTasks;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Extrai o voice-pack do jar, gera zip e envia aos jogadores (Folia-safe).
 */
public final class VoiceResourcePackManager implements Listener {

    private final AnarchyTags plugin;
    private volatile URI packUri;
    private volatile byte[] packHash;

    public VoiceResourcePackManager(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void prepare() {
        try {
            Path folder = plugin.getDataFolder().toPath().resolve("voice-resourcepack");
            Path source = extractBundledPack(folder);
            Path zip = plugin.getDataFolder().toPath().resolve("voice-icons.zip");
            buildZip(source, zip);
            packUri = zip.toUri();
            packHash = sha1(zip);
            plugin.getLogger().info("Resource pack de ícones de voz pronto: " + zip.getFileName());
        } catch (IOException e) {
            plugin.getLogger().warning("Não foi possível preparar voice-icons.zip: " + e.getMessage());
        }
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.config().getNametagConfig().isVoiceChatSendResourcePack()) {
            return;
        }
        if (packUri == null) {
            return;
        }
        Player player = event.getPlayer();
        FoliaTasks.runOnEntity(plugin, player, () -> sendPack(player));
    }

    public void sendPack(Player player) {
        if (packUri == null || !player.isOnline()) {
            return;
        }
        try {
            player.setResourcePack(
                    packUri.toString(),
                    packHash,
                    Component.text("AnarchyTags — ícones de voz"),
                    true
            );
        } catch (Exception ex) {
            plugin.getLogger().fine("Falha ao enviar resource pack para " + player.getName() + ": " + ex.getMessage());
        }
    }

    private Path extractBundledPack(Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        copyResourceTree("voice-pack", targetDir);
        return targetDir;
    }

    private void copyResourceTree(String resourceRoot, Path targetDir) throws IOException {
        try (InputStream listing = plugin.getResource(resourceRoot + "/pack.mcmeta")) {
            if (listing == null) {
                throw new IOException("voice-pack ausente no jar");
            }
        }
        copyResourcePath(resourceRoot, targetDir);
    }

    private void copyResourcePath(String path, Path target) throws IOException {
        String entryPath = path.endsWith("/") ? path : path + "/";
        try (InputStream root = plugin.getResource(entryPath)) {
            if (root != null) {
                Files.createDirectories(target);
                try (InputStream file = plugin.getResource(path)) {
                    if (file != null) {
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                return;
            }
        }
        // walk known structure
        String[] known = {
                "pack.mcmeta",
                "ICONS.txt",
                "assets/anarchytags/font/voice_icons.json",
                "assets/anarchytags/textures/voice/talking.png",
                "assets/anarchytags/textures/voice/whispering.png",
                "assets/anarchytags/textures/voice/quiet.png",
                "assets/anarchytags/textures/voice/disabled.png",
                "assets/anarchytags/textures/voice/not_installed.png"
        };
        for (String relative : known) {
            String resource = path + "/" + relative;
            try (InputStream in = plugin.getResource(resource)) {
                if (in == null) {
                    continue;
                }
                Path out = target.resolve(relative);
                Files.createDirectories(out.getParent());
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void buildZip(Path sourceDir, Path zipFile) throws IOException {
        Files.createDirectories(zipFile.getParent());
        try (OutputStream out = Files.newOutputStream(zipFile);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                walk.filter(Files::isRegularFile)
                        .forEach(path -> {
                            String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                            try (InputStream in = Files.newInputStream(path)) {
                                zip.putNextEntry(new ZipEntry(entryName));
                                in.transferTo(zip);
                                zip.closeEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
    }

    private static byte[] sha1(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return digest.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
