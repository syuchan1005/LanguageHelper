import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class LanguageHelper {
	private Map<String, FileConfiguration> Configs = new HashMap<>();
	private String defaultLang = null;

	public LanguageHelper(Plugin plugin) throws IOException, InvalidConfigurationException {
		FileConfiguration f = getConfig(plugin, "config.yml");
		if (f.isString("DefaultLanguage")) defaultLang = f.getString("DefaultLanguage");
		JarFile jar = null;
		try {
			jar = new JarFile(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
				String Name = e.nextElement().getName();
				if (!Name.endsWith(".lang")) continue;
				String[] name = Name.split("/");
				Configs.put(name[name.length - 1].replaceAll(".lang", ""), getConfig(plugin, Name));
			}
		} finally {
			if (jar != null) jar.close();
		}
	}

	public String getString(CommandSender sender, String path) {
		if (sender instanceof Player) return this.getString((Player) sender, path);
		return (defaultLang == null) ? path : this.getString(defaultLang, path);
	}

	public String getString(Player player, String path) {
		try {
			return this.getString(getLocale(player), path);
		} catch (ReflectiveOperationException e) {
			return (defaultLang == null) ? path : this.getString(defaultLang, path);
		}
	}

	public String getString(String LangName, String path) {
		if (Configs.containsKey(LangName)) return toColor(Configs.get(LangName).getString(path));
		return (defaultLang == null) ? path : this.getString(defaultLang, path);
	}

	public List<String> getStringList(CommandSender sender, String path) {
		if (sender instanceof Player) return this.getStringList((Player) sender, path);
		return (defaultLang == null) ? Arrays.asList(path) : this.getStringList(defaultLang, path);
	}

	public List<String> getStringList(Player player, String path) {
		try {
			return this.getStringList(getLocale(player), path);
		} catch (ReflectiveOperationException e) {
			return (defaultLang == null) ? Arrays.asList(path) : this.getStringList(defaultLang, path);
		}
	}

	public List<String> getStringList(String LangName, String path) {
		if (Configs.containsKey(LangName))
			return Configs.get(LangName).getStringList(path).stream().map(s -> toColor(s)).collect(Collectors.toList());
		return (defaultLang == null) ? Arrays.asList(path) : this.getStringList(defaultLang, path);
	}

	public FileConfiguration getConfig(Plugin plugin, String filename) throws IOException, InvalidConfigurationException {
		File file = new File(plugin.getDataFolder(), filename);
		if (!file.exists()) plugin.saveResource(filename, false);
		Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
		FileConfiguration conf = new YamlConfiguration();
		conf.load(reader);
		return conf;
	}

	private static Method handleMethod = null;
	private static Field localeField = null;

	public static String getLocale(Player player) throws ReflectiveOperationException {
		if (handleMethod == null) handleMethod = player.getClass().getDeclaredMethod("getHandle");
		Object o = handleMethod.invoke(player);
		if (localeField == null) localeField = o.getClass().getField("locale");
		return (String) localeField.get(o);
	}

	private static String toColor(String msg) {
		if (msg == null) return "null";
		return ChatColor.translateAlternateColorCodes('&', msg);
	}
}
