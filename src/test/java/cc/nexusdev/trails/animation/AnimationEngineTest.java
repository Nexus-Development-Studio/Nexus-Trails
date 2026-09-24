package cc.nexusdev.trails.animation;

import cc.nexusdev.trails.api.animation.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnimationEngineTest {
    @TempDir Path directory;
    AnimationEngine engine;JavaPlugin plugin;Plugin extension;Player viewer;PluginManager manager;
    MockedStatic<Bukkit> bukkit;
    final UUID uuid=new UUID(1,2);
    @BeforeEach void setup() throws Exception {
        Files.copy(Path.of("src/main/resources/animations.yml"),directory.resolve("animations.yml"));
        plugin=mock(JavaPlugin.class);extension=mock(Plugin.class);viewer=mock(Player.class);manager=mock(PluginManager.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());when(plugin.isEnabled()).thenReturn(true);
        when(extension.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("animations-test"));
        when(plugin.getResource("animations.yml")).thenAnswer(call->Files.newInputStream(Path.of("src/main/resources/animations.yml")));
        when(viewer.getUniqueId()).thenReturn(uuid);when(viewer.getLocation()).thenReturn(new Location(mock(World.class),0,64,0));
        bukkit=mockStatic(Bukkit.class);bukkit.when(Bukkit::getPluginManager).thenReturn(manager);bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        engine=new AnimationEngine(plugin);
    }
    @AfterEach void close() {engine.shutdown();bukkit.close();}
    void render(double seconds) {engine.render(viewer,TrailKind.QUEST,"npc:11",BuiltInAnimationsTest.path(),seconds,.8,12);}

    @Test void customCallbacksAreSelectableAndListenerCanOverrideSelection() {
        AtomicInteger a=new AtomicInteger(),b=new AtomicInteger();
        engine.register(extension,"example:one",(frame,sink)->a.incrementAndGet());
        engine.register(extension,"example:two",(frame,sink)->b.incrementAndGet());
        engine.select(uuid,"example:one");render(0);assertEquals(1,a.get());
        doAnswer(call->{((TrailAnimationSelectEvent)call.getArgument(0)).setAnimationId("example:two");return null;}).when(manager).callEvent(any());
        render(1);assertEquals(1,b.get());assertEquals(1,a.get());
        assertEquals("example:one",engine.selected(uuid)); // Event selection is frame-local.
        doAnswer(call->{((TrailAnimationSelectEvent)call.getArgument(0)).setCancelled(true);return null;}).when(manager).callEvent(any());
        render(2);assertEquals(1,b.get());
    }

    @Test void customEmissionsArePrivateBudgetedAndSinkCannotBeRetained() {
        ParticleSink[] retained=new ParticleSink[1];
        engine.register(extension,"example:burst",(frame,sink)->{
            retained[0]=sink;assertEquals(12,sink.remaining());
            assertTrue(sink.emit(Particle.FLAME,frame.point(.5,0,0),100,null));
            assertEquals(0,sink.remaining());assertFalse(sink.emit(Particle.FLAME,frame.point(.5,0,0),1,null));
        });
        engine.select(uuid,"example:burst");render(0);
        verify(viewer).spawnParticle(eq(Particle.FLAME),anyDouble(),anyDouble(),anyDouble(),eq(12),eq(0.0),eq(0.0),eq(0.0),eq(0.0),isNull());
        assertThrows(IllegalStateException.class,()->retained[0].remaining());
    }

    @Test void ownershipAndPluginDisableCleanupProtectRegistrations() {
        engine.register(extension,"example:custom",(frame,sink)->{});
        assertThrows(IllegalArgumentException.class,()->engine.register(extension,"comet",(f,s)->{}));
        assertThrows(IllegalArgumentException.class,()->engine.register(extension,"example:custom",(f,s)->{}));
        assertThrows(IllegalArgumentException.class,()->engine.unregister(plugin,"example:custom"));
        engine.select(uuid,"example:custom");
        engine.disabled(new PluginDisableEvent(extension));
        assertFalse(engine.animations().contains("example:custom"));assertEquals("nexustrails:breathing",engine.selected(uuid));
    }

    @Test void failingCustomAnimationFallsBackWithoutBeingCalledEveryFrame() {
        AtomicInteger calls=new AtomicInteger();
        engine.register(extension,"example:broken",(frame,sink)->{calls.incrementAndGet();throw new IllegalStateException("test error");});
        engine.select(uuid,"example:broken");render(0);render(.3);render(.6);assertEquals(1,calls.get());
        verify(viewer,atLeastOnce()).spawnParticle(eq(Particle.DUST),anyDouble(),anyDouble(),anyDouble(),eq(1),eq(0.0),eq(0.0),eq(0.0),eq(0.0),any(Particle.DustOptions.class));
    }

    @Test void styleOverridesReachCustomCallbacksAndStateIsIsolatedAndReset() throws Exception {
        List<AnimationFrame> frames=new ArrayList<>();
        engine.register(extension,"example:state",(frame,sink)->{frames.add(frame);frame.state().put("seen",true);});
        var blue=BuiltInAnimationsTest.config().common().withPalette(List.of(Color.BLUE));
        engine.select(uuid,"example:state");engine.setStyle(uuid,blue);render(0);render(1);
        assertSame(frames.get(0).state(),frames.get(1).state());assertEquals(blue,frames.get(1).style());
        engine.forget(uuid,TrailKind.QUEST);render(2);assertNotSame(frames.get(0).state(),frames.get(2).state());
        engine.resetStyle(uuid);render(3);assertNotEquals(blue,frames.get(3).style());
    }
}
