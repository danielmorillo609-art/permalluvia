package com.permalluvia;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

public final class Items {

    public static final List<String> NOMBRES = Arrays.asList(
            "manzana_vida", "pocion_res2", "totem_res1", "totem_res2",
            "totem_regen", "totem_vel", "libro_crafteos");

    private final NamespacedKey kVida;
    private final NamespacedKey kTotem;

    public Items(Plugin plugin) {
        this.kVida = new NamespacedKey(plugin, "pl_vida");
        this.kTotem = new NamespacedKey(plugin, "pl_totem");
    }

    private static String nombre(ChatColor c, String s) {
        return ChatColor.RESET + "" + c + ChatColor.BOLD + s;
    }

    private static String lore(String s) {
        return ChatColor.RESET + "" + ChatColor.GRAY + s;
    }

    private static void brillo(ItemMeta m) {
        m.addEnchant(Enchantment.DURABILITY, 1, true);
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    public ItemStack manzanaVida() {
        ItemStack it = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(nombre(ChatColor.RED, "Manzana de Vida"));
        m.setLore(Arrays.asList(lore("+1 corazón permanente")));
        m.setCustomModelData(1);
        brillo(m);
        m.getPersistentDataContainer().set(kVida, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    public ItemStack pocionRes2() {
        ItemStack it = new ItemStack(Material.POTION);
        PotionMeta m = (PotionMeta) it.getItemMeta();
        m.setDisplayName(nombre(ChatColor.DARK_AQUA, "Poción de Resistencia II"));
        m.setLore(Arrays.asList(lore("Resistencia II (3:00)")));
        m.setColor(Color.fromRGB(10044730));
        m.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 3600, 1), true);
        it.setItemMeta(m);
        return it;
    }

    public ItemStack totem(String tipo) {
        ChatColor color;
        String titulo;
        String efecto;
        int modelo;
        switch (tipo) {
            case "res1":
                color = ChatColor.GRAY; titulo = "Tótem de Resistencia I"; efecto = "Resistencia I"; modelo = 1; break;
            case "res2":
                color = ChatColor.DARK_PURPLE; titulo = "Tótem de Resistencia II"; efecto = "Resistencia II"; modelo = 2; break;
            case "regen":
                color = ChatColor.LIGHT_PURPLE; titulo = "Tótem de Regeneración"; efecto = "Regeneración I"; modelo = 3; break;
            case "vel":
                color = ChatColor.AQUA; titulo = "Tótem de Velocidad"; efecto = "Velocidad II"; modelo = 4; break;
            default:
                return null;
        }
        ItemStack it = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(nombre(color, titulo));
        m.setLore(Arrays.asList(lore("Sostenlo en una mano:"), lore(efecto)));
        m.setCustomModelData(modelo);
        brillo(m);
        m.getPersistentDataContainer().set(kTotem, PersistentDataType.STRING, tipo);
        it.setItemMeta(m);
        return it;
    }

    public ItemStack libro() {
        ItemStack it = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta m = (BookMeta) it.getItemMeta();
        m.setTitle("Libro de Crafteos");
        m.setAuthor("Permalluvia");
        m.setPages(Arrays.asList(
                titulo(ChatColor.BLACK, "Libro de Crafteos")
                        + "\n\nCrafteos custom del servidor.\n\nP2: Manzana de Vida\nP3: Manzana de Notch\nP4: Poción Resist. II\nP5-8: Tótems\nP9: Lluvia",
                titulo(ChatColor.RED, "Manzana de Vida")
                        + "\n+1 corazón permanente (máx. 20 extra)\n\nD S D\nN A N\nD S D\n\nD: Bloque de diamante\nS: Estrella del Nether\nN: Lingote de netherite\nA: Manzana de oro",
                titulo(ChatColor.GOLD, "Manzana de Notch")
                        + "\n\nG G G\nG M G\nG G G\n\nG: Bloque de oro\nM: Manzana normal\n\nDa una manzana de oro encantada.",
                titulo(ChatColor.DARK_AQUA, "Poción de Resistencia II")
                        + "\nDura 3 minutos.\n\nSin forma fija:\n- Botella de cristal\n- Lingote de netherite\n- 2 bloques de hierro",
                titulo(ChatColor.GRAY, "Tótem de Resistencia I")
                        + "\nSujétalo en una mano.\n\nI D I\nD T D\nI D I\n\nI: Bloque de hierro\nD: Diamante\nT: Tótem de la inmortalidad",
                titulo(ChatColor.DARK_PURPLE, "Tótem de Resistencia II")
                        + "\nSujétalo en una mano.\n\nI N I\nD T D\nI N I\n\nI: Bloque de hierro\nN: Lingote de netherite\nD: Bloque de diamante\nT: Tótem",
                titulo(ChatColor.LIGHT_PURPLE, "Tótem de Regeneración")
                        + "\nSujétalo en una mano.\n\nM L M\nL T L\nM L M\n\nM: Manzana de oro\nL: Lágrima de ghast\nT: Tótem",
                titulo(ChatColor.AQUA, "Tótem de Velocidad")
                        + "\nDa Velocidad II.\n\nE D E\nD T D\nE D E\n\nE: Perla de ender\nD: Diamante\nT: Tótem",
                titulo(ChatColor.BLUE, "Ciclo de lluvia")
                        + "\n\nDías 0-99:\n5 secos, 3 de lluvia.\n\nDesde el día 100:\n3 secos, 6 de tormenta con rayos."
        ));
        it.setItemMeta(m);
        return it;
    }

    private static String titulo(ChatColor c, String s) {
        return c + "" + ChatColor.BOLD + s + ChatColor.RESET + "\n";
    }

    public ItemStack porNombre(String n) {
        switch (n.toLowerCase()) {
            case "manzana_vida": return manzanaVida();
            case "pocion_res2": return pocionRes2();
            case "totem_res1": return totem("res1");
            case "totem_res2": return totem("res2");
            case "totem_regen": return totem("regen");
            case "totem_vel": return totem("vel");
            case "libro_crafteos": return libro();
            default: return null;
        }
    }

    public boolean esManzanaVida(ItemStack it) {
        if (it == null || it.getType() != Material.GOLDEN_APPLE || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(kVida, PersistentDataType.BYTE);
    }

    public String tipoTotem(ItemStack it) {
        if (it == null || it.getType() != Material.TOTEM_OF_UNDYING || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(kTotem, PersistentDataType.STRING);
    }
}
