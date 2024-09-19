package com.gmail.theminiluca.inconvenient.survival;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.commands.DebugCommand;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

import static com.gmail.theminiluca.inconvenient.survival.WorldData.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Inconvenient extends JavaPlugin implements Listener {

    public static final Set<Material> materials = new HashSet<>();
    public static final Map<Material, Integer> correct = new HashMap<>();
    private static Inventory inventory;

    private static Inconvenient instance;

    public static Inconvenient getInstance() {
        return instance;
    }

    @Override
    public @NotNull Path getDataPath() {
        return super.getDataPath();
    }

    public static boolean inventoryEqual(Inventory player, Inventory player1) {
        for (int i = 0; i < 41; i++) {
            if (i == 36) {
                i = 40;
            }
            if (!Objects.equals(player.getItem(i), player1.getItem(i))) {
                return false;
            }
        }
        return true;
    }


    public static World getDataWorld() {
        World world = Bukkit.getWorld("world");
        if (world == null) throw new NullPointerException("world cannot be null");
        return world;
    }

    @Override
    public void onDisable() {
        World world = getDataWorld();
        saveMaterialsToWorld(world);
        saveMapToWorld(world);
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        World world = getDataWorld();
        saveMaterialsToWorld(world);
        saveMapToWorld(world);
    }

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
        inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
        World world = getDataWorld();
        loadMaterialsFromWorld(world);
        loadMapFromWorld(world);
        if (correct.isEmpty()) {
            List<Material> materialList = new ArrayList<>(Arrays.stream(Material.values()).filter(material -> material.isBlock()
                    && !(material.getHardness() < 0) && !(material.equals(Material.LAVA) || material.equals(Material.WATER))).toList());
            Map<Material, Integer> correctMaterials = new HashMap<>();
            for (int i = 1; i < inventory.getContents().length; i++) {
                Material material = materialList.remove(new Random().nextInt(materialList.size()));
                correctMaterials.put(material, i);
            }
            correct.clear();
            correct.putAll(correctMaterials);
        }

        getLogger().info(materials.toString() + "<-- materials");
        getLogger().info(correct.toString() + "<-- correct");
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);
                    if (inventoryEqual(player.getInventory(), inventory)) continue;
                    inventory.setContents(player.getInventory().getContents());
                    for (int j = 0; j < players.size(); j++) {
                        Player target = players.get(j);
                        if (i == j) continue;
                        target.getInventory().setContents(inventory.getContents());
                    }

                }
            }
        }.runTaskTimer(this, 0, 1);

        getCommand("debug").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
                if (!(commandSender instanceof Player player)) return false;
                Inventory inventory = Bukkit.createInventory(null, 9 * 6, Component.text("DEBUG", NamedTextColor.RED));
                inventory.setContents(Inconvenient.inventory.getContents());
                player.openInventory(inventory);
                return false;
            }
        });

    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        if (materials.contains(material)) {
            return;
        }
        materials.add(material);
        if (correct.containsKey(material)) {
            inventory.setItem(correct.get(material), new ItemStack(Material.GOLDEN_APPLE));
            player.getInventory().setContents(inventory.getContents());
            spawnRandomFirework(player.getLocation());
            Bukkit.broadcast(Component.text(player.getName(), NamedTextColor.RED).append(Component.text("님이 ", NamedTextColor.WHITE))
                    .append(Component.translatable(material.translationKey(), NamedTextColor.GOLD)).append(Component.text(" 블럭을 파괴하여 ", NamedTextColor.WHITE))
                    .append(Component.text(correct.get(material), NamedTextColor.YELLOW)).append(Component.text("번 이 잠금해제되었습니다.", NamedTextColor.WHITE)));
        }
    }

    private void spawnRandomFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // 랜덤 효과 설정
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(getRandomColor())         // 랜덤 색상
                .withFade(getRandomColor())          // 랜덤 페이드 색상
                .with(FireworkEffect.Type.values()[new Random().nextInt(FireworkEffect.Type.values().length)]) // 랜덤 모양
                .flicker(new Random().nextBoolean()) // 깜빡임 여부
                .trail(new Random().nextBoolean())   // 꼬리 여부
                .build();

        meta.addEffect(effect);
        meta.setPower(0);  // 폭죽의 높이 (1~2)
        firework.setFireworkMeta(meta);
    }

    // 랜덤한 색상을 반환하는 함수
    private Color getRandomColor() {
        Random random = new Random();
        return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getLogger().info(Arrays.toString(inventory.getContents()) + "<-- inventory");
        getLogger().info(inventory.isEmpty() + "<-- empty");
        inventory.setContents(player.getInventory().getContents());
        if (inventory.isEmpty()) {
            for (Map.Entry<Material, Integer> entry : correct.entrySet()) {
                if (!materials.contains(entry.getKey())) {
                    inventory.setItem(entry.getValue(), new ItemStack(Material.BARRIER));
                }
            }
        }
        getLogger().info(Arrays.toString(inventory.getContents()) + "<-- inventory");
        player.getInventory().setContents(inventory.getContents());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.getDrops().clear();
        event.setKeepInventory(true);
        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack stack = inventory.getContents()[i];
            if (stack == null || stack.getType().equals(Material.BARRIER)) continue;
            stack = stack.clone();
            inventory.setItem(i, new ItemStack(Material.AIR));
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.getInventory().setContents(inventory.getContents());
    }

    @EventHandler
    public void onInventoryClick(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (event.getMainHandItem().getType() == Material.BARRIER
                || event.getOffHandItem().getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) return;
        if (itemStack.getType().equals(Material.BARRIER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = (Player) event.getPlayer();
        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (itemStack.getType().equals(Material.BARRIER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        if (itemStack.getType().equals(Material.BARRIER)) {
            event.setCancelled(true);
        }
    }
}