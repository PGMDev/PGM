package tc.oc.util.bukkit.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.Convertable;
import net.minecraft.server.v1_8_R3.EmptyChunk;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.IPlayerFileData;
import net.minecraft.server.v1_8_R3.IProgressUpdate;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.WorldData;
import net.minecraft.server.v1_8_R3.WorldProvider;

/** A {@link Convertable} world loader that only loads empty {@link Chunk}s in memory. */
public class EmptyConvertable implements Convertable, IDataManager, IPlayerFileData, IChunkLoader {

  private final UUID id;
  private final File file;
  private final Table<Integer, Integer, Chunk> chunks;

  public EmptyConvertable() {
    this.id = UUID.randomUUID();
    this.file = Files.createTempDir();
    this.chunks = HashBasedTable.create();
  }

  @Override
  public IDataManager a(String s, boolean b) {
    return this;
  }

  @Override
  public IPlayerFileData getPlayerFileData() {
    return this;
  }

  @Override
  public IChunkLoader createChunkLoader(WorldProvider worldProvider) {
    return this;
  }

  @Override
  public UUID getUUID() {
    return id;
  }

  @Override
  public String g() { // getName
    return id.toString();
  }

  @Override
  public File getDirectory() {
    return file;
  }

  @Override
  public File getDataFile(String s) {
    return file;
  }

  @Override
  public Chunk a(World world, int i, int i1) throws IOException {
    Chunk chunk = chunks.get(i, i1);
    if (chunk == null) {
      chunk = new EmptyChunk(world, i, i1);
      chunks.put(i, i1, chunk);
    }
    return chunk;
  }

  @Override
  public boolean e(String s) {
    chunks.clear();
    return true;
  }

  @Override
  public boolean isConvertable(String s) {
    return false;
  }

  @Override
  public boolean convert(String s, IProgressUpdate iProgressUpdate) {
    return true;
  }

  @Override
  public WorldData getWorldData() {
    return null;
  }

  @Override
  public NBTTagCompound load(EntityHuman entityHuman) {
    return null;
  }

  @Override
  public String[] getSeenPlayers() {
    return new String[0];
  }

  @Override
  public void d() {
    // No-op
  }

  @Override
  public void checkSession() {
    // No-op
  }

  @Override
  public void saveWorldData(WorldData worldData, NBTTagCompound nbtTagCompound) {
    // No-op
  }

  @Override
  public void saveWorldData(WorldData worldData) {
    // No-op
  }

  @Override
  public void a(World world, Chunk chunk) {
    // No-op
  }

  @Override
  public void b(World world, Chunk chunk) {
    // No-op
  }

  @Override
  public void a() {
    // No-op
  }

  @Override
  public void b() {
    // No-op
  }

  @Override
  public void save(EntityHuman entityHuman) {
    // No-op
  }
}
