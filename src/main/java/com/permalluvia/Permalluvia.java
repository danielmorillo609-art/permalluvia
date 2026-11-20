package com.permalluvia;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Permalluvia extends JavaPlugin implements Listener, TabExecutor {

    private Items items;
    private NamespacedKey kVidaExtra;
    private NamespacedKey kInicio;
    private final List<NamespacedKey> recetas = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new Items(this);
        kVidaExtra = new NamespacedKey(this, "vida_extra");
        kInicio = new NamespacedKey(this, "inicio");

        registrarRecetas();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("permalluvia").setExecutor(this);
        getCommand("permalluvia").setTabCompleter(this);

        new BukkitRunnable() {
            @Override
            public void run() {
                clima();
                efectosTotem();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    @Override
    public void onDisable() {
        for (NamespacedKey k : recetas) {
            Bukkit.removeRecipe(k);
        }
        recetas.clear();
    }

    // ---------------------------------------------------------------- Clima

    private void clima() {
        World w = Bukkit.getWorld(getConfig().getString("mundo", "world"));
        if (w == null) return;

        if (!Boolean.FALSE.equals(w.getGameRuleValue(GameRule.DO_WEATHER_CYCLE))) {
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }

        long dia = w.getFullTime() / 24000L;
        boolean lluvia;
        boolean trueno;
        if (dia < 100) {
            lluvia = (dia % 8) >= 5;
            trueno = false;
        } else {
            lluvia = ((dia - 100) % 9) >= 3;
            trueno = lluvia;
        }

        if (w.hasStorm() != lluvia || w.isThundering() != trueno) {
            w.setStorm(lluvia);
            w.setThundering(trueno);
        }
        if (lluvia) {
            w.setWeatherDuration(1000000);
            if (trueno) w.setThunderDuration(1000000);
        } else {
            w.setClearWeatherDuration(1000000);
        }
    }

    // --------------------------------------------------------------- Tótems

    private void efectosTotem() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            aplicarTotem(p, p.getInventory().getItemInMainHand());
            aplicarTotem(p, p.getInventory().getItemInOffHand());
        }
    }

    private void aplicarTotem(Player p, ItemStack item) {
        String tipo = items.tipoTotem(item);
        if (tipo == null) return;
        switch (tipo) {
            case "res1":
                dar(p, PotionEffectType.DAMAGE_RESISTANCE, 0);
                break;
            case "res2":
                dar(p, PotionEffectType.DAMAGE_RESISTANCE, 1);
                break;
            case "regen":
                if (!p.hasPotionEffect(PotionEffectType.REGENERATION)) {
                    dar(p, PotionEffectType.REGENERATION, 0);
                }
                break;
            case "vel":
                dar(p, PotionEffectType.SPEED, 1);
                break;
            default:
                break;
        }
    }

    private void dar(Player p, PotionEffectType tipo, int nivel) {
        p.addPotionEffect(new PotionEffect(tipo, 80, nivel, true, false));
    }

    // ----------------------------------------------------------------- Vida

    private void aplicarVida(Player p) {
        int n = p.getPersistentDataContainer().getOrDefault(kVidaExtra, PersistentDataType.INTEGER, 0);
        if (n <= 0) return;
        if (n > 20) n = 20;
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) attr.setBaseValue(20.0 + 2.0 * n);
    }

    @EventHandler
    public void alComer(PlayerItemConsumeEvent e) {
        if (!items.esManzanaVida(e.getItem())) return;
        Player p = e.getPlayer();
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        int n = pdc.getOrDefault(kVidaExtra, PersistentDataType.INTEGER, 0) + 1;
        if (n > 20) n = 20;
        pdc.set(kVidaExtra, PersistentDataType.INTEGER, n);
        aplicarVida(p);
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.RED + "+1 corazón permanente"));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.discoverRecipes(recetas);
        aplicarVida(p);
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (!pdc.has(kInicio, PersistentDataType.BYTE)) {
            pdc.set(kInicio, PersistentDataType.BYTE, (byte) 1);
            entregar(p, items.libro());
            p.sendMessage(ChatColor.GOLD + "Recibiste el Libro de Crafteos y se desbloquearon las recetas custom.");
        }
    }

    @EventHandler
    public void alReaparecer(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> aplicarVida(p));
    }

    private void entregar(Player p, ItemStack it) {
        Map<Integer, ItemStack> sobra = p.getInventory().addItem(it);
        for (ItemStack s : sobra.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), s);
        }
    }

    // -------------------------------------------------------------- Recetas

    private <T extends Recipe & Keyed> void registrar(T r) {
        NamespacedKey k = r.getKey();
        Bukkit.removeRecipe(k);
        Bukkit.addRecipe(r);
        recetas.add(k);
    }

    private void forma(String id, ItemStack resultado, String[] patron, Object... ingredientes) {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, id), resultado);
        r.shape(patron);
        for (int i = 0; i < ingredientes.length; i += 2) {
            r.setIngredient((Character) ingredientes[i], (Material) ingredientes[i + 1]);
        }
        registrar(r);
    }

    private void registrarRecetas() {
        forma("manzana_vida", items.manzanaVida(), new String[]{"DSD", "NAN", "DSD"},
                'D', Material.DIAMOND_BLOCK, 'S', Material.NETHER_STAR,
                'N', Material.NETHERITE_INGOT, 'A', Material.GOLDEN_APPLE);

        forma("manzana_notch", new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), new String[]{"GGG", "GMG", "GGG"},
                'G', Material.GOLD_BLOCK, 'M', Material.APPLE);

        forma("totem_res1", items.totem("res1"), new String[]{"IDI", "DTD", "IDI"},
                'I', Material.IRON_BLOCK, 'D', Material.DIAMOND, 'T', Material.TOTEM_OF_UNDYING);

        forma("totem_res2", items.totem("res2"), new String[]{"INI", "DTD", "INI"},
                'I', Material.IRON_BLOCK, 'N', Material.NETHERITE_INGOT,
                'D', Material.DIAMOND_BLOCK, 'T', Material.TOTEM_OF_UNDYING);

        forma("totem_regen", items.totem("regen"), new String[]{"MLM", "LTL", "MLM"},
                'M', Material.GOLDEN_APPLE, 'L', Material.GHAST_TEAR, 'T', Material.TOTEM_OF_UNDYING);

        forma("totem_vel", items.totem("vel"), new String[]{"EDE", "DTD", "EDE"},
                'E', Material.ENDER_PEARL, 'D', Material.DIAMOND, 'T', Material.TOTEM_OF_UNDYING);

        ShapelessRecipe pocion = new ShapelessRecipe(new NamespacedKey(this, "pocion_res2"), items.pocionRes2());
        pocion.addIngredient(Material.GLASS_BOTTLE);
        pocion.addIngredient(Material.NETHERITE_INGOT);
        pocion.addIngredient(2, Material.IRON_BLOCK);
        registrar(pocion);

        ShapelessRecipe libro = new ShapelessRecipe(new NamespacedKey(this, "libro_crafteos"), items.libro());
        libro.addIngredient(Material.BOOK);
        libro.addIngredient(Material.APPLE);
        registrar(libro);
    }

    // ------------------------------------------------------------- Comandos

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("permalluvia.admin")) {
            s.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }
        if (a.length < 2 || !a[0].equalsIgnoreCase("dar")) {
            s.sendMessage(ChatColor.YELLOW + "Uso: /permalluvia dar <item> [cantidad] [jugador]");
            s.sendMessage(ChatColor.GRAY + "Items: " + String.join(", ", Items.NOMBRES));
            return true;
        }
        ItemStack it = items.porNombre(a[1]);
        if (it == null) {
            s.sendMessage(ChatColor.RED + "Item desconocido. Items: " + String.join(", ", Items.NOMBRES));
            return true;
        }
        int cantidad = 1;
        if (a.length > 2) {
            try {
                cantidad = Math.max(1, Math.min(64, Integer.parseInt(a[2])));
            } catch (NumberFormatException ex) {
                s.sendMessage(ChatColor.RED + "La cantidad debe ser un número.");
                return true;
            }
        }
        Player objetivo = null;
        if (a.length > 3) {
            objetivo = Bukkit.getPlayerExact(a[3]);
        } else if (s instanceof Player) {
            objetivo = (Player) s;
        }
        if (objetivo == null) {
            s.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return true;
        }
        it.setAmount(cantidad);
        entregar(objetivo, it);
        s.sendMessage(ChatColor.GREEN + "Entregado " + cantidad + "x " + a[1] + " a " + objetivo.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            if ("dar".startsWith(a[0].toLowerCase())) out.add("dar");
        } else if (a.length == 2 && a[0].equalsIgnoreCase("dar")) {
            for (String n : Items.NOMBRES) {
                if (n.startsWith(a[1].toLowerCase())) out.add(n);
            }
        } else if (a.length == 4 && a[0].equalsIgnoreCase("dar")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(a[3].toLowerCase())) out.add(p.getName());
            }
        }
        return out;
    }
}
