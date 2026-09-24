package cc.nexusdev.trails.animation;

import cc.nexusdev.trails.api.animation.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BuiltInAnimationsTest {
    static AnimationConfig config() throws Exception {
        var cfg=new YamlConfiguration();cfg.load("src/main/resources/animations.yml");return AnimationConfig.read(cfg);
    }
    static AnimationPath path() {
        return new AnimationPath(List.of(new AnimationPoint(0,64,0),new AnimationPoint(10,64,0),new AnimationPoint(10,64,10)));
    }
    static AnimationFrame frame(AnimationStyle style,double seconds,int budget,Map<String,Object> state) {
        return new AnimationFrame(new UUID(1,2),TrailKind.QUEST,"npc:11",path(),new AnimationPoint(-2,64,0),seconds,73,.8,budget,style,state);
    }
    static final class Capture implements ParticleSink {
        final List<double[]> points=new ArrayList<>();
        final int budget;
        Capture(int budget) {this.budget=budget;}
        @Override public boolean emit(AnimationPoint point,double light,double color) {
            assertTrue(Double.isFinite(light)&&light>=0&&light<=1);
            assertTrue(Double.isFinite(color)&&color>=0&&color<=1);
            if(remaining()==0)return false;
            points.add(new double[]{point.x(),point.y(),point.z(),light,color});return true;
        }
        @Override public boolean emit(Particle type,AnimationPoint point,int count,Object data) {throw new AssertionError("Built-ins must use configurable particles");}
        @Override public int remaining() {return budget-points.size();}
    }

    @TestFactory Stream<DynamicTest> everyRequestedPatternAnimatesAcrossItsCycleWithinBudget() throws Exception {
        AnimationConfig config=config();
        assertEquals(46,BuiltInAnimations.all().size());
        assertEquals(BuiltInAnimations.all().keySet(),config.profiles().keySet());
        return BuiltInAnimations.all().entrySet().stream().map(entry->DynamicTest.dynamicTest(entry.getKey(),()->{
            AnimationStyle style=config.style(entry.getKey());Map<String,Object> state=new HashMap<>();
            Set<String> frames=new HashSet<>();int emitted=0;
            for(int i=0;i<40;i++) {
                Capture sink=new Capture(120);
                entry.getValue().render(frame(style,style.periodSeconds()*i/40,120,state),sink);
                assertTrue(sink.points.size()<=120);
                frames.add(sink.points.stream().map(Arrays::toString).toList().toString());emitted+=sink.points.size();
            }
            assertTrue(emitted>0,"Must produce a visible animation");
            assertTrue(frames.size()>=3,"Must change throughout its cycle (theater chase has three steps)");
            Capture tiny=new Capture(6);entry.getValue().render(frame(style,.4,6,new HashMap<>()),tiny);assertTrue(tiny.points.size()<=6);
        }));
    }

    @Test void allPatternsHaveDistinctTracesAndExportPreviewFrames() throws Exception {
        AnimationConfig config=config();Set<String> traces=new HashSet<>();StringBuilder json=new StringBuilder("[");
        for(var entry:BuiltInAnimations.all().entrySet()) {
            if(json.length()>1)json.append(',');json.append("{\"id\":\"").append(entry.getKey()).append("\",\"seconds\":")
                    .append(config.style(entry.getKey()).periodSeconds()).append(",\"frames\":[");
            StringBuilder signature=new StringBuilder();Map<String,Object> state=new HashMap<>();AnimationStyle style=config.style(entry.getKey());
            for(int i=0;i<48;i++) {
                Capture capture=new Capture(120);entry.getValue().render(frame(style,style.periodSeconds()*i/48,120,state),capture);
                if(i>0)json.append(',');json.append('[');
                for(int j=0;j<capture.points.size();j++) {
                    double[] p=capture.points.get(j);if(j>0)json.append(',');
                    Color color=style.color(p[4],p[3]);
                    json.append(String.format(Locale.ROOT,"[%.3f,%.3f,%.3f,%d]",p[0],p[1]-64,p[2],color.asRGB()));
                    signature.append(Arrays.toString(p));
                }
                json.append(']');
            }
            assertTrue(traces.add(signature.toString()),"Duplicate pattern: "+entry.getKey());json.append("]}");
        }
        json.append(']');Files.createDirectories(Path.of("target"));Files.writeString(Path.of("target/animation-preview.json"),json);
        Files.writeString(Path.of("target/animation-preview.html"),Files.readString(Path.of("src/test/resources/animation-preview.html")).replace("__FRAMES__",json));
        atlas(config);
    }

    private static void atlas(AnimationConfig config) throws Exception {
        int columns=4,cellWidth=300,cellHeight=180;
        var image=new java.awt.image.BufferedImage(columns*cellWidth,12*cellHeight,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(16,24,33));g.fillRect(0,0,image.getWidth(),image.getHeight());
        int index=0;
        for(var entry:BuiltInAnimations.all().entrySet()) {
            int x=(index%columns)*cellWidth,y=(index/columns)*cellHeight;index++;
            g.setColor(new java.awt.Color(220,230,240));g.setFont(new java.awt.Font("SansSerif",java.awt.Font.PLAIN,13));g.drawString(entry.getKey().substring(12),x+12,y+20);
            Capture sink=new Capture(120);var style=config.style(entry.getKey());Map<String,Object> state=new HashMap<>();
            for(int i=0;i<=15;i++){sink=new Capture(120);entry.getValue().render(frame(style,style.periodSeconds()*i/40,120,state),sink);}
            for(double[] p:sink.points) {
                int px=x+150+(int)((p[0]-p[2])*8),py=y+60+(int)((p[0]+p[2])*3.5-(p[1]-64)*20);
                Color color=style.color(p[4],p[3]);g.setColor(new java.awt.Color(color.asRGB()));g.fillOval(px-2,py-2,4,4);
            }
        }
        g.dispose();javax.imageio.ImageIO.write(image,"png",Path.of("target/animation-atlas.png").toFile());
    }

    @Test void palettesAndParametersAreImmutableAndInvalidAppearanceIsRejected() throws Exception {
        AnimationStyle original=config().common();
        assertThrows(UnsupportedOperationException.class,()->original.palette().clear());
        assertThrows(UnsupportedOperationException.class,()->original.parameters().clear());
        AnimationStyle red=original.withPalette(List.of(Color.RED));
        assertEquals(Color.RED,red.color(.7,1));
        assertEquals(Color.fromRGB(128,0,0),red.color(.7,.5));
        assertNotEquals(red.palette(),original.palette());
        assertThrows(IllegalArgumentException.class,()->original.withPeriod(0));
        assertThrows(IllegalArgumentException.class,()->original.withPalette(List.of()));
    }
    @Test void mothWaitsInWorldSpaceUntilTheViewerApproaches() throws Exception {
        var moth=BuiltInAnimations.all().get("nexustrails:clockwork-moth");var style=config().common();Map<String,Object> state=new HashMap<>();
        moth.render(frame(style,0,120,state),new Capture(120));Object first=state.get("moth-guide");
        moth.render(frame(style,1,120,state),new Capture(120));assertEquals(first,state.get("moth-guide"));
        AnimationPoint near=(AnimationPoint)first;
        AnimationPath advanced=new AnimationPath(List.of(near,new AnimationPoint(10,64,10)));
        moth.render(new AnimationFrame(new UUID(1,2),TrailKind.QUEST,"npc:11",advanced,near,2,73,.8,120,style,state),new Capture(120));
        assertNotEquals(first,state.get("moth-guide"));
    }
}
