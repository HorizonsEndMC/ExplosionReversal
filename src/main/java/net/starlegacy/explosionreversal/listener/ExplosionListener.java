package net.starlegacy.explosionreversal.listener;

import java.io.IOException;
import net.starlegacy.explosionreversal.ExplosionReversalPlugin;
import net.starlegacy.explosionreversal.Settings;
import net.starlegacy.explosionreversal.data.ExplodedBlockData;
import net.starlegacy.explosionreversal.nms.NMSUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nullable;
import java.util.*;

public class ExplosionListener implements Listener {
    private final ExplosionReversalPlugin plugin;

    public ExplosionListener(ExplosionReversalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onEntityExplode(EntityExplodeEvent event) throws IOException {
        if (plugin.getSettings().getIgnoredEntityExplosions().contains(event.getEntity().getType())) {
            return;
        }

        World world = event.getEntity().getWorld();
        Location location = event.getLocation();
        List<Block> blockList = event.blockList();
        addRedstone(blockList);
        processExplosion(world, location, blockList);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onBlockExplode(BlockExplodeEvent event) throws IOException {
        World world = event.getBlock().getWorld();
        Location location = event.getBlock().getLocation();
        List<Block> blockList = event.blockList();
        addRedstone(blockList);
        processExplosion(world, location, blockList);
    }

    private void processExplosion(World world, Location explosionLocation, List<Block> list) throws IOException {
        if (plugin.getSettings().getIgnoredWorlds().contains(world.getName())) {
            return;
        }

        if (list.isEmpty()) {
            return;
        }

        List<ExplodedBlockData> explodedBlockDataList = new LinkedList<>();

        double eX = explosionLocation.getX(), eY = explosionLocation.getY(), eZ = explosionLocation.getZ();

        for (Iterator<Block> iterator = list.iterator(); iterator.hasNext(); ) {
            processBlock(explodedBlockDataList, eX, eY, eZ, iterator);
        }

        // if no blocks were handled by the plugin at all (for example, every block's type is ignored)
        if (explodedBlockDataList.isEmpty()) {
            return;
        }

        plugin.getWorldData().addAll(world, explodedBlockDataList);
    }

    private void processBlock(List<ExplodedBlockData> explodedBlockDataList, double eX, double eY, double eZ,
                              Iterator<Block> iterator) throws IOException {
        Block block = iterator.next();
        BlockData blockData = block.getBlockData();

        if (ignoreMaterial(blockData.getMaterial())) {
            return;
        }

        int x = block.getX(), y = block.getY(), z = block.getZ();
        long explodedTime = plugin.getExplodedTime(eX, eY, eZ, x, y, z);

        @Nullable byte[] tileEntity = NMSUtils.getTileEntity(block);

        if (tileEntity != null) {
            processTileEntity(explodedBlockDataList, block, explodedTime);
        }

        ExplodedBlockData explodedBlockData = new ExplodedBlockData(x, y, z, explodedTime, blockData, tileEntity);
        explodedBlockDataList.add(explodedBlockData);

        // break the block manually
        iterator.remove();
        block.setType(Material.AIR, false);
    }

    private boolean ignoreMaterial(Material material) {
        Settings settings = plugin.getSettings();
        Set<Material> includedMaterials = settings.getIncludedMaterials();

        return material == Material.AIR ||
                settings.getIgnoredMaterials().contains(material) ||
                !includedMaterials.isEmpty() && !includedMaterials.contains(material);
    }

    private void processTileEntity(List<ExplodedBlockData> explodedBlockDataList, Block block, long explodedTime) throws IOException {
        BlockState state = block.getState();

        if (state instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) state).getInventory();
            // Double chests are weird so you have to get the state (as a holder)'s inventory's holder to cast to DoubleChest
            InventoryHolder inventoryHolder = inventory.getHolder();

            if (inventoryHolder instanceof DoubleChest) {
                boolean isRight = ((Chest) block.getBlockData()).getType() == Chest.Type.RIGHT;
                DoubleChest doubleChest = (DoubleChest) inventoryHolder;
                processDoubleChest(explodedBlockDataList, isRight, doubleChest, explodedTime);
            }

            inventory.clear();
        }
    }

    private void processDoubleChest(List<ExplodedBlockData> explodedBlockDataList, boolean isRight,
                                    DoubleChest doubleChest, long explodedTime) throws IOException {
        DoubleChestInventory inventory = (DoubleChestInventory) doubleChest.getInventory();
        Inventory otherInventory = isRight ? inventory.getRightSide() : inventory.getLeftSide();
        Location otherInventoryLocation = Objects.requireNonNull(otherInventory.getLocation());
        Block other = otherInventoryLocation.getBlock();

        int otherX = other.getX(), otherY = other.getY(), otherZ = other.getZ();
        BlockData otherBlockData = other.getBlockData();
        byte[] otherTile = NMSUtils.getTileEntity(other);

        explodedBlockDataList.add(new ExplodedBlockData(otherX, otherY, otherZ, explodedTime, otherBlockData, otherTile));

        otherInventory.clear();
        other.setType(Material.AIR, false);
    }

    private static void addRedstone(List<Block> explodedBlocks) {
        List<Block> adjacentBlocks = getAdjacentBlocks(explodedBlocks);
        addSupportedRedstoneBlocks(explodedBlocks, adjacentBlocks);
    }

    private static List<Block> getAdjacentBlocks(List<Block> explodedBlocks) {
        List<Block> adjacentBlocks = new ArrayList<>();
        BlockFace[] cardinalDirections = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

        for (Block block : explodedBlocks) {
            for (BlockFace face : cardinalDirections) {
                Block adjacentBlock = block.getRelative(face);
                if (!explodedBlocks.contains(adjacentBlock)) {
                    adjacentBlocks.add(adjacentBlock);
                }
            }
        }

        return adjacentBlocks;
    }

    private static void addSupportedRedstoneBlocks(List<Block> explodedBlocks, List<Block> adjacentBlocks) {
        for (Block adjacentBlock : adjacentBlocks) {
            // This block is a potential block that we want to determine if it is supported
            BlockData blockData = adjacentBlock.getBlockData();
			Material material = blockData.getMaterial();
			if (material != Material.REDSTONE_WIRE
				&& material != Material.REDSTONE_TORCH
				&& material != Material.REDSTONE_WALL_TORCH
				&& material != Material.REPEATER
				&& material != Material.COMPARATOR
			) continue;

			if (!isSupportedByExplodedBlock(explodedBlocks, adjacentBlock)) continue;

			// adjacentBlock is a block we care about, it is supported by supportBlock, and supportBlock is about to be exploded
			// add adjacentBlock to the list of exploded blocks
			explodedBlocks.add(adjacentBlock);
        }
    }

	// Can't use BlockData.isSupported(), as the underlying NMS gets the current block state and sees air
	// Use the list of exploded blocks as that will have the correct block state, and basically re-implement BlockData.isSupported()
    private static Boolean isSupportedByExplodedBlock(List<Block> explodedBlocks, Block adjacentBlock) {
		BlockData adjacentBlockData = adjacentBlock.getBlockData();
        if (adjacentBlockData instanceof RedstoneWire || adjacentBlockData instanceof Repeater || adjacentBlockData instanceof Comparator) {
			Block relativeBlock = adjacentBlock.getRelative(BlockFace.DOWN);
			if (!explodedBlocks.contains(relativeBlock)) return false;

			return adjacentBlock.getRelative(BlockFace.DOWN).getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.FULL);
        } else if (adjacentBlockData instanceof RedstoneWallTorch) {
			BlockFace face = ((RedstoneWallTorch) adjacentBlockData).getFacing();
            BlockFace oppositeFace = face.getOppositeFace();
			Block relativeBlock = adjacentBlock.getRelative(oppositeFace);
			if (!explodedBlocks.contains(relativeBlock)) return false;

			return adjacentBlock.getRelative(oppositeFace).getBlockData().isFaceSturdy(face, BlockSupport.FULL);
        } else if (adjacentBlock.getType() == Material.REDSTONE_TORCH) {
            // This redstone torch is a ground torch
			Block relativeBlock = adjacentBlock.getRelative(BlockFace.DOWN);
			if (!explodedBlocks.contains(relativeBlock)) return false;

            return adjacentBlock.getRelative(BlockFace.DOWN).getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.CENTER);
        }
        // Don't care if this block is not supported
        return false;
    }
}
