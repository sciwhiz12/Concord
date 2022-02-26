/*
 * Concord - Copyright (c) 2020-2022 SciWhiz12
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tk.sciwhiz12.concord.util;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class LinkedUsers {

    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .setLenient()
        .create();

    @SuppressWarnings("serial")
    public static final Type TYPE_OF_MAP = new TypeToken<Map<Long, UUID>>() {}.getType();

    private final Path path;
    private final Random random = new Random();

    public LinkedUsers(Path path) {
        this.path = path;
        load();
    }

    private final Long2ObjectMap<UUID> users = new Long2ObjectOpenHashMap<>();
    private final Cache<Integer, UUID> codeCache = CacheBuilder.newBuilder()
         .expireAfterWrite(Duration.ofHours(12))
         .softValues()
         .build();
    
    public int generateLinkCode(UUID minecraftUUID) {
        final var id = random.nextInt(100000001);
        codeCache.put(id, minecraftUUID);
        return id;
    }
    
    public boolean codeExists(int code) {
        return codeCache.getIfPresent(code) != null;
    }
    
    /**
     * Links a Discord account with a Minecraft one, by link codes.
     * @param code the link code
     * @param discordId the ID of the Discord account
     */
    public void linkByCode(int code, long discordId) {
        if (codeExists(code)) {
            final var mcId = codeCache.getIfPresent(code);
            link(discordId, mcId);
            codeCache.invalidate(code);
        }
    }
    
    /**
     * Links a Discord account with a Minecraft one.
     * @param discordID the ID of the Discord account
     * @param minecraftUUID the UUID of the Minecraft account
     */
    public void link(final long discordID, final UUID minecraftUUID) {
        users.put(discordID, minecraftUUID);
        save();
    }
    
    public void unLink(final long discordID) {
        users.remove(discordID);
        save();
    }
    
    public void unLink(final UUID minecraftUUID) {
        getDiscordID(minecraftUUID).ifPresent(this::unLink);
    }

    public Optional<UUID> getMinecraftUUID(final long discordId) {
        return Optional.ofNullable(users.get(discordId));
    }

    public OptionalLong getDiscordID(final UUID minecraftUUID) {
        return users.long2ObjectEntrySet().stream().filter(e -> e.getValue().equals(minecraftUUID))
                .mapToLong(Long2ObjectMap.Entry::getLongKey).findFirst();
    }

    public void save() {
        createFile();
        try (final var writer = new FileWriter(path.toFile())) {
            GSON.toJson(serialize(), writer);
        } catch (Exception e) {
            LOGGER.error("Exception while trying to write Linked Users file at path {}", path, e);
        }
    }
    
    public void load() {
        createFile();
        try (final var is = new FileReader(path.toFile())) {
            deserialize(GSON.fromJson(is, JsonObject.class));
        } catch (Exception e) {
            LOGGER.error("Exception while trying to read Linked Users file at path {}", path, e);
        }
    }
    
    private final void createFile() {
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (Exception e) {
                LOGGER.error("Could not create Linked Users file at path {}!", path, e);
                return;
            }
        }
    }

    public JsonObject serialize() {
        return GSON.fromJson(GSON.toJson(users), JsonObject.class);
    }

    @SuppressWarnings("unchecked")
    public void deserialize(JsonObject object) {
        users.clear();
        final var newMap = GSON.fromJson(object, TYPE_OF_MAP);
        users.putAll((Map<? extends Long, ? extends UUID>) newMap);
    }
}
