package cc.nexusdev.trails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cc.nexusdev.trails.api.QuestTrailAPI;
import cc.nexusdev.trails.quest.QuestTrailService;
import cc.nexusdev.trails.quest.QuestTrailCommand;
import org.bukkit.plugin.ServicePriority;
import cc.nexusdev.trails.animation.AnimationEngine;
import cc.nexusdev.trails.animation.AnimationCommand;
import cc.nexusdev.trails.api.animation.TrailAnimationAPI;

public final class NexusTrailsPlugin extends JavaPlugin implements Listener {
    private static final Set<String> RESERVED = Set.of("help", "list", "stop", "go", "set", "record", "pause", "resume", "save", "cancel", "delete", "reload");
    private RouteStore store;
    private TrailService trails;
    private QuestTrailService questTrails;
    private AnimationEngine animations;
    private volatile TrailSettings settings;
    private final Map<UUID, Recording> recordings = new ConcurrentHashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        if(!new java.io.File(getDataFolder(),"animations.yml").exists()) saveResource("animations.yml",false);
        store = new RouteStore(getDataFolder().toPath().resolve("destinations.yml"));
        try { store.load(); settings = TrailSettings.read(getConfig()); animations=new AnimationEngine(this); }
        catch (Exception ex) {
            getLogger().severe("Could not load Nexus Trails configuration: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        trails = new TrailService(this);
        TrailParticles.initialize(animations);
        getServer().getPluginManager().registerEvents(animations,this);
        getServer().getServicesManager().register(TrailAnimationAPI.class,animations,this,ServicePriority.Normal);
        AnimationCommand animationCommand=new AnimationCommand(animations);
        Objects.requireNonNull(getCommand("trailanimation")).setExecutor(animationCommand);
        Objects.requireNonNull(getCommand("trailanimation")).setTabCompleter(animationCommand);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("trail")).setExecutor(this);
        Objects.requireNonNull(getCommand("trail")).setTabCompleter(this);
        if (!isFolia()) {
            try {
                questTrails = new QuestTrailService(this, store, () -> settings);
                getServer().getPluginManager().registerEvents(questTrails, this);
                getServer().getServicesManager().register(QuestTrailAPI.class, questTrails, this, ServicePriority.Normal);
                getServer().getOnlinePlayers().forEach(questTrails::restore);
            } catch (IllegalArgumentException ex) {
                getLogger().severe("Quest trails disabled: " + ex.getMessage());
            }
        } else getLogger().info("Quest integration requires Paper; standalone trails remain available on Folia.");
        QuestTrailCommand questCommand = new QuestTrailCommand(questTrails);
        Objects.requireNonNull(getCommand("questtrail")).setExecutor(questCommand);
        Objects.requireNonNull(getCommand("questtrail")).setTabCompleter(questCommand);
        getLogger().info("Nexus Trails loaded " + store.all().size() + " destinations.");
    }
    @Override public void onDisable() {
        if (questTrails != null) questTrails.shutdown();
        getServer().getServicesManager().unregisterAll(this);
        if (trails != null) trails.shutdown();
        if(animations!=null) animations.shutdown();
        TrailParticles.initialize(null);
        recordings.clear();
    }

    private static boolean isFolia() {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; }
        catch (ClassNotFoundException ignored) { return false; }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexustrails.use") && !sender.hasPermission("nexustrails.admin")) {
            sender.sendMessage("§cYou do not have permission to use trails."); return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        try {
            if (action.equals("help")) { help(sender); return true; }
            if (action.equals("list")) {
                sender.sendMessage("§bDestinations: §f" + (store.all().isEmpty() ? "None yet." : String.join(", ", new TreeSet<>(store.all().keySet()))));
                return true;
            }
            if (Set.of("set", "record", "pause", "resume", "save", "cancel", "delete", "reload").contains(action)
                    && !sender.hasPermission("nexustrails.admin")) {
                sender.sendMessage("§cOnly trail administrators can do that."); return true;
            }
            if (action.equals("reload")) {
                store.load(); reloadConfig(); settings = TrailSettings.read(getConfig());
                animations.reload();
                if (questTrails != null) questTrails.reload();
                trails.shutdown();
                sender.sendMessage("§aDestinations and configuration reloaded; ordinary trails stopped. Quest guidance uses the updated configuration."); return true;
            }
            if (action.equals("delete")) {
                if (args.length != 3 || !args[2].equals("DELETE")) {
                    sender.sendMessage("§eConfirm with /trail delete <id> DELETE"); return true;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (store.delete(id)) { trails.stopDestination(id); sender.sendMessage("§aDeleted " + id); }
                else sender.sendMessage("§cDestination not found.");
                return true;
            }
            if (!(sender instanceof Player player)) { sender.sendMessage("This command must be run in game."); return true; }
            switch (action) {
                case "stop" -> { trails.stop(player.getUniqueId()); player.sendMessage("§eTrail stopped."); }
                case "set" -> {
                    String id = newId(args);
                    if (id == null) { player.sendMessage("§cUse /trail set <new-id>; IDs use letters, numbers, - and _. Existing IDs cannot be overwritten."); break; }
                    saveNew(new Route(id, player.getWorld().getName(), List.of(TrailService.point(player.getLocation()))));
                    player.sendMessage("§aWaypoint saved: §f" + id + "§7. Players can use /trail " + id);
                }
                case "record" -> {
                    String id = newId(args);
                    if (id == null) { player.sendMessage("§cUse /trail record <new-id>. Choose an unused destination ID."); break; }
                    if (recordings.containsKey(player.getUniqueId())) { player.sendMessage("§cSave or cancel your current recording first."); break; }
                    if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) { player.sendMessage("§cStart recording on foot."); break; }
                    trails.stop(player.getUniqueId());
                    recordings.put(player.getUniqueId(), new Recording(id, player.getWorld().getName(), TrailService.point(player.getLocation())));
                    player.sendMessage("§aRecording §f" + id + "§a. Walk the route, then /trail save at the destination.");
                }
                case "pause" -> {
                    Recording rec = recording(player);
                    if (rec != null) { rec.paused = true; player.sendMessage("§eRecording paused."); }
                }
                case "resume" -> {
                    Recording rec = recording(player);
                    if (rec != null && canContinue(player, rec)) { rec.paused = false; player.sendMessage("§aRecording resumed."); }
                }
                case "save" -> {
                    Recording rec = recording(player);
                    if (rec == null || !canContinue(player, rec)) break;
                    Route.Point end = TrailService.point(player.getLocation());
                    List<Route.Point> points = new ArrayList<>(rec.points);
                    if (points.getLast().distanceSquared(end) > .01) points.add(end);
                    if (points.size() < 2) { player.sendMessage("§cWalk farther before saving, or use /trail set for a simple waypoint."); break; }
                    if (points.size() > settings.maxPoints()) { player.sendMessage("§cPoint limit reached. Return to the last recorded point to save."); break; }
                    saveNew(new Route(rec.id, rec.world, points, rec.teleports));
                    recordings.remove(player.getUniqueId());
                    player.sendMessage("§aRoute saved: §f" + rec.id + " §7(" + points.size() + " points)");
                }
                case "cancel" -> { recordings.remove(player.getUniqueId()); player.sendMessage("§eRecording cancelled. Saved destinations are unchanged."); }
                default -> {
                    String id = action.equals("go") ? (args.length == 2 ? args[1].toLowerCase(Locale.ROOT) : "") : action;
                    Route destination = store.get(id);
                    if (destination == null) player.sendMessage("§cUnknown destination. Use /trail list.");
                    else trails.start(player, destination, settings);
                }
            }
        } catch (Exception ex) {
            getLogger().warning("Trail command failed: " + ex.getMessage());
            sender.sendMessage("§cCould not complete that action. " + ex.getMessage());
        }
        return true;
    }

    private synchronized void saveNew(Route route) throws java.io.IOException {
        if (store.get(route.id()) != null) throw new IllegalArgumentException("That destination already exists; use a different ID.");
        store.save(route);
    }
    private String newId(String[] args) {
        if (args.length != 2) return null;
        String id = args[1].toLowerCase(Locale.ROOT);
        return id.matches("[a-z0-9][a-z0-9_-]{0,47}") && !RESERVED.contains(id) && store.get(id) == null ? id : null;
    }
    private Recording recording(Player p) {
        Recording rec = recordings.get(p.getUniqueId());
        if (rec == null) p.sendMessage("§cNo active recording. Use /trail record <id>.");
        return rec;
    }
    private boolean canContinue(Player p, Recording rec) {
        if (!p.getWorld().getName().equals(rec.world) || p.isFlying() || p.isGliding() || p.isInsideVehicle()
                || rec.points.getLast().distanceSquared(TrailService.point(p.getLocation())) > settings.maxStep() * settings.maxStep()) {
            p.sendMessage("§cReturn on foot near the last recorded point in " + rec.world + " before resuming or saving."); return false;
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true) public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) return;
        Recording rec = recordings.get(event.getPlayer().getUniqueId());
        if (rec == null || rec.paused || event.getTo() == null || !event.hasChangedPosition()) return;
        Player p = event.getPlayer();
        if (!canContinue(p, rec) || !event.getTo().getWorld().getName().equals(rec.world)
                || rec.points.getLast().distanceSquared(TrailService.point(event.getTo())) > settings.maxStep() * settings.maxStep()) {
            rec.paused = true;
            p.sendMessage("§eRecording paused. Return near the last recorded point and use /trail resume.");
            return;
        }
        if (rec.points.size() >= settings.maxPoints()) {
            rec.paused = true; p.sendMessage("§eRecording point limit reached. Use /trail save or /trail cancel."); return;
        }
        Route.Point point = TrailService.point(event.getTo());
        if (rec.points.getLast().distanceSquared(point) >= settings.recordSpacing() * settings.recordSpacing()) rec.points.add(point);
    }
    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        trails.rejoin(event.getPlayer().getUniqueId());
        Recording rec = recordings.get(event.getPlayer().getUniqueId());
        if (rec == null || event.getTo() == null) return;
        switch (rec.teleport(event.getTo().getWorld().getName(), TrailService.point(event.getFrom()),
                TrailService.point(event.getTo()), settings.maxPoints())) {
            case WORLD_CHANGED -> event.getPlayer().sendMessage("§eRecording paused: routes cannot span worlds. Return to " + rec.world + " before /trail resume.");
            case POINT_LIMIT -> event.getPlayer().sendMessage("§eRecording point limit reached. Return to the last recorded point to save, or /trail cancel.");
            default -> { }
        }
    }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { trails.rejoin(event.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { trails.stop(event.getPlayer().getUniqueId()); recordings.remove(event.getPlayer().getUniqueId()); }

    private void help(CommandSender sender) {
        sender.sendMessage("§bNexus Trails §7— /trail <destination>, /trail list, /trail stop");
        if (sender.hasPermission("nexustrails.admin")) {
            sender.sendMessage("§7/trail set <id> — save your position as a waypoint");
            sender.sendMessage("§7/trail record <id> — walk a route toward its destination");
            sender.sendMessage("§7/trail pause | resume | save | cancel");
            sender.sendMessage("§7/trail delete <id> DELETE | /trail reload");
        }
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexustrails.use") && !sender.hasPermission("nexustrails.admin")) return List.of();
        Set<String> candidates = new TreeSet<>();
        if (args.length == 1) {
            candidates.addAll(store.all().keySet()); candidates.addAll(List.of("help", "list", "stop", "go"));
            if (sender.hasPermission("nexustrails.admin")) candidates.addAll(RESERVED);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("go") || (args[0].equalsIgnoreCase("delete") && sender.hasPermission("nexustrails.admin")))) {
            candidates.addAll(store.all().keySet());
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(s -> s.startsWith(prefix)).toList();
    }
    static final class Recording {
        final String id, world;
        final List<Route.Point> points = new ArrayList<>();
        final List<Integer> teleports = new ArrayList<>();
        boolean paused;
        Recording(String id, String world, Route.Point start) { this.id = id; this.world = world; points.add(start); }

        enum TeleportResult { CONTINUED, ALREADY_PAUSED, WORLD_CHANGED, POINT_LIMIT }

        TeleportResult teleport(String targetWorld, Route.Point from, Route.Point to, int maxPoints) {
            if (paused) return TeleportResult.ALREADY_PAUSED;
            if (!world.equals(targetWorld)) { paused = true; return TeleportResult.WORLD_CHANGED; }
            List<Route.Point> additions = new ArrayList<>(2);
            Route.Point last = points.getLast();
            if (last.distanceSquared(from) > .01) { additions.add(from); last = from; }
            boolean moved = last.distanceSquared(to) > .01;
            if (moved) additions.add(to);
            if (points.size() + additions.size() > maxPoints) { paused = true; return TeleportResult.POINT_LIMIT; }
            points.addAll(additions);
            if (moved) teleports.add(points.size() - 1);
            return TeleportResult.CONTINUED;
        }
    }
}
