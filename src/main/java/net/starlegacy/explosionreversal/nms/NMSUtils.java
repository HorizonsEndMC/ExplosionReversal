package net.starlegacy.explosionreversal.nms;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;

import javax.annotation.Nullable;

public class NMSUtils {
	@SuppressWarnings("UnstableApiUsage")
	private static byte[] serialize(CompoundTag nbt) throws IOException {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		NbtIo.write(nbt, output);

		return output.toByteArray();
	}

	@SuppressWarnings("UnstableApiUsage")
	private static CompoundTag deserialize(byte[] bytes) throws IOException {
		ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
		NbtAccounter readLimiter = new NbtAccounter(bytes.length * 10L);
		return NbtIo.read(input, readLimiter);
	}

    @Nullable
	public static byte[] getTileEntity(Block block) throws IOException {
		ServerLevel worldServer = ((CraftWorld) block.getWorld()).getHandle();

		BlockPos blockPosition = new BlockPos(block.getX(), block.getY(), block.getZ());

		BlockEntity tileEntity = worldServer.getBlockEntity(blockPosition);
		if (tileEntity == null) {
			return null;
		}

		CompoundTag nbt = tileEntity.saveWithFullMetadata();

		return serialize(nbt);
	}

	public static void setTileEntity(Block block, byte[] bytes) throws IOException {
		ServerLevel worldServer = ((CraftWorld) block.getWorld()).getHandle();

		BlockPos blockPosition = new BlockPos(block.getX(), block.getY(), block.getZ());

		CompoundTag nbt = deserialize(bytes);

		BlockState blockData = worldServer.getBlockState(blockPosition);

		BlockEntity tileEntity = BlockEntity.loadStatic(blockPosition, blockData, nbt);

		worldServer.removeBlockEntity(blockPosition);
		worldServer.setBlockEntity(Objects.requireNonNull(tileEntity));
	}

    @Nullable
	public static byte[] getEntityData(org.bukkit.entity.Entity entity) throws IOException {
		net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();

		CompoundTag nbt = new CompoundTag();
		nmsEntity.save(nbt);
		return serialize(nbt);
	}

	public static void restoreEntityData(org.bukkit.entity.Entity entity, byte[] entityData) throws IOException {
		net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
		CompoundTag nbt = deserialize(entityData);
		nmsEntity.load(nbt);
	}
}
