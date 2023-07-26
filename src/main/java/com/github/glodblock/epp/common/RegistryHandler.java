package com.github.glodblock.epp.common;

import appeng.api.upgrades.Upgrades;
import appeng.block.AEBaseBlockItem;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import com.github.glodblock.epp.EPP;
import com.github.glodblock.epp.container.ContainerExInterface;
import com.github.glodblock.epp.container.ContainerExPatternProvider;
import com.github.glodblock.epp.util.FCUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class RegistryHandler {

    public static final RegistryHandler INSTANCE = new RegistryHandler();

    protected final List<Pair<String, Block>> blocks = new ArrayList<>();
    protected final List<Pair<String, Item>> items = new ArrayList<>();
    protected final List<Pair<String, BlockEntityType<?>>> tiles = new ArrayList<>();

    public void block(String name, Block block) {
        blocks.add(Pair.of(name, block));
        if (block instanceof AEBaseEntityBlock<?> tileBlock) {
            tile(name, tileBlock.getBlockEntityType());
        }
    }

    public <T extends AEBaseBlockEntity> void block(String name, AEBaseEntityBlock<T> block, Class<T> clazz, BlockEntityType.BlockEntitySupplier<? extends T> supplier) {
        bindTileEntity(clazz, block, supplier);
        block(name, block);
    }

    public void item(String name, Item item) {
        items.add(Pair.of(name, item));
    }

    public void tile(String name, BlockEntityType<?> type) {
        tiles.add(Pair.of(name, type));
    }

    @SubscribeEvent
    public void runRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            this.onRegisterBlocks();
            this.onRegisterItems();
            this.onRegisterTileEntities();
            this.onRegisterContainer();
        }
    }

    private void onRegisterBlocks() {
        for (Pair<String, Block> entry : blocks) {
            String key = entry.getLeft();
            Block block = entry.getRight();
            ForgeRegistries.BLOCKS.register(EPP.id(key), block);
        }
    }

    private void onRegisterItems() {
        for (Pair<String, Block> entry : blocks) {
            ForgeRegistries.ITEMS.register(EPP.id(entry.getLeft()), new AEBaseBlockItem(entry.getRight(), new Item.Properties()));
        }
        for (Pair<String, Item> entry : items) {
            ForgeRegistries.ITEMS.register(EPP.id(entry.getLeft()), entry.getRight());
        }
    }

    private void onRegisterTileEntities() {
        for (Pair<String, BlockEntityType<?>> entry : tiles) {
            String key = entry.getLeft();
            BlockEntityType<?> tile = entry.getRight();
            ForgeRegistries.BLOCK_ENTITY_TYPES.register(EPP.id(key), tile);
        }
    }

    private void onRegisterContainer() {
        ForgeRegistries.MENU_TYPES.register(AppEng.makeId("ex_pattern_provider"), ContainerExPatternProvider.TYPE);
        ForgeRegistries.MENU_TYPES.register(AppEng.makeId("ex_interface"), ContainerExInterface.TYPE);
    }

    private <T extends AEBaseBlockEntity> void bindTileEntity(Class<T> clazz, AEBaseEntityBlock<T> block, BlockEntityType.BlockEntitySupplier<? extends T> supplier) {
        BlockEntityTicker<T> serverTicker = null;
        if (ServerTickingBlockEntity.class.isAssignableFrom(clazz)) {
            serverTicker = (level, pos, state, entity) -> ((ServerTickingBlockEntity) entity).serverTick();
        }
        BlockEntityTicker<T> clientTicker = null;
        if (ClientTickingBlockEntity.class.isAssignableFrom(clazz)) {
            clientTicker = (level, pos, state, entity) -> ((ClientTickingBlockEntity) entity).clientTick();
        }
        block.setBlockEntity(clazz, FCUtil.getTileType(clazz, supplier, block), clientTicker, serverTicker);
    }

    public void onInit() {
        for (Pair<String, Block> entry : blocks) {
            Block block = ForgeRegistries.BLOCKS.getValue(EPP.id(entry.getKey()));
            if (block instanceof AEBaseEntityBlock<?>) {
                AEBaseBlockEntity.registerBlockEntityItem(
                        ((AEBaseEntityBlock<?>) block).getBlockEntityType(),
                        block.asItem()
                );
            }
        }
        this.registerAEUpgrade();
    }

    private void registerAEUpgrade() {
        Upgrades.add(AEItems.FUZZY_CARD, EPPItemAndBlock.EX_INTERFACE.asItem(), 1, "gui.expatternprovider.ex_interface");
        Upgrades.add(AEItems.CRAFTING_CARD, EPPItemAndBlock.EX_INTERFACE.asItem(), 1, "gui.expatternprovider.ex_interface");
    }

    public void registerTab(Registry<CreativeModeTab> registry) {
        var tab = CreativeModeTab.builder()
                .icon(() -> new ItemStack(EPPItemAndBlock.EX_PATTERN_PROVIDER))
                .title(Component.translatable("itemGroup.epp"))
                .displayItems((__, o) -> {
                    for (Pair<String, Item> entry : items) {
                        o.accept(entry.getRight());
                    }
                    for (Pair<String, Block> entry : blocks) {
                        o.accept(entry.getRight());
                    }
                })
                .build();
        Registry.register(registry, EPP.id("tab_main"), tab);
    }

}