package cc.nexusdev.trails.animation;

import cc.nexusdev.trails.api.animation.*;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

/** Procedural geometry and envelopes only. Appearance comes exclusively from AnimationStyle. */
public final class BuiltInAnimations {
    private BuiltInAnimations() {}
    public enum Pattern {
        BREATHING, CHASE, COMET, WAVE, RIPPLE, HEARTBEAT, TWINKLE, SPARKLE, SCAN,
        SEQUENTIAL_FILL, DRIPPING, THEATER_CHASE, STACKING, CROSSFADE_CHASE,
        TICK_TOCK, CLOCK_HANDS, TIME_SKIP, REVERSE_ECHO, CLOCKWORK_FOOTSTEPS,
        UNWINDING_SPRING, STEAM_BURSTS, FUSE_BURN, ORBITING_GUIDE, SHATTERED_SECONDS,
        PENDULUM, INK_REVEAL, MECHANICAL_RELAY, GHOST_OF_TOMORROW, ESCAPING_SECONDS,
        CLOCKWORK_MOTH, BROKEN_TIMELINE, POCKET_WATCH_SWING, BORROWED_FOOTSTEPS,
        MECHANICAL_FIREFLIES, STITCHING_TIME, FALLING_HOURGLASS, ECHO_DOORS,
        MAGNETIC_SHAVINGS, CLOCK_TEETH, REWINDING_RIBBON, FUTURE_GLIMPSE,
        PRESSURE_LEAK, ORBIT_COLLAPSE, UNFINISHED_BLUEPRINT, DELAYED_SHADOW, SECOND_HAND_SWEEP;
        public String id() { return "nexustrails:" + name().toLowerCase(Locale.ROOT).replace('_','-'); }
    }
    public static Map<String,TrailAnimation> all() {
        Map<String,TrailAnimation> result=new LinkedHashMap<>();
        for (Pattern pattern:Pattern.values()) result.put(pattern.id(), (frame,sink)->new Draw(frame,sink).render(pattern));
        return Collections.unmodifiableMap(result);
    }

    private static final class Draw {
        private static final double TAU=Math.PI*2;
        final AnimationFrame f;
        final ParticleSink sink;
        final double p,t,width,tail,frequency;
        final int samples,markers;
        Draw(AnimationFrame frame,ParticleSink sink) {
            f=frame; this.sink=sink; p=f.phase(); t=f.elapsedSeconds()/f.style().periodSeconds();
            width=param("pulse-width",.12,.01,1); tail=param("tail-length",.3,.01,1);
            frequency=param("frequency",3,.1,20);
            markers=(int)param("markers",5,1,16);
            samples=Math.clamp((int)Math.ceil(f.path().length()/f.spacing())+1,2,Math.min(180,Math.max(2,f.budget())));
        }
        double param(String key,double value,double min,double max) { return Math.clamp(f.style().parameter(key,value),min,max); }
        double u(int i) { return (double)i/(samples-1); }
        void dot(double u,double side,double up,double light) {
            if (light>.003 && sink.remaining()>0) sink.emit(f.point(u,side,up),clamp(light),clamp(u));
        }
        void line(DoubleUnaryOperator brightness) {
            for(int i=0;i<samples && sink.remaining()>0;i++) dot(u(i),0,0,Math.max(f.style().baseBrightness(),brightness.applyAsDouble(u(i))));
        }
        void base() { for(int i=0;i<samples;i+=3) dot(u(i),0,0,f.style().baseBrightness()); }
        double noise(int i) { return random(f.seed()+i*0x9E3779B97F4A7C15L); }
        double pulse(double x,double center) { double d=(x-center)/width; return Math.exp(-d*d*3); }
        double cyclicPulse(double x,double center) { double d=Math.abs(frac(x-center+.5)-.5)/width; return Math.exp(-d*d*3); }
        double fade(double age) { return clamp(1-age); }
        double meters(double value) { return value/Math.max(1,f.path().length()); }
        void arrow(double center,double light,double side,double up) {
            for(int i=0;i<5;i++) {
                double v=i/4.0;
                dot(center-meters(.9)*(1-v),side,up,light);
                if(i<4) { dot(center-meters(.5)*v,side+.45*v,up,light); dot(center-meters(.5)*v,side-.45*v,up,light); }
            }
        }
        void ring(double center,double radius,double up,double light,boolean upright) {
            int n=(int)param("ring-points",12,6,32);
            for(int i=0;i<n;i++) {
                double a=TAU*i/n;
                if(upright) dot(center,Math.cos(a)*radius,up+Math.sin(a)*radius,light);
                else dot(center+meters(Math.cos(a)*radius),Math.sin(a)*radius,up,light);
            }
        }
        void footprint(double center,int side,double light) {
            for(int i=0;i<4;i++) dot(center+meters(i*.11),side*.3,0,light*(.7+i*.1));
        }
        void silhouette(double center,double light) {
            ring(center,.18,1.5,light,true);
            for(int i=0;i<6;i++) dot(center,0,.5+i*.13,light);
            for(int i=0;i<4;i++) { double v=i/3.0; dot(center,-.3*v,.8-.5*v,light); dot(center,.3*v,.8-.5*v,light); }
        }
        void render(Pattern pattern) {
            switch(pattern) {
                case BREATHING -> line(x->.5-.5*Math.cos(TAU*p));
                case CHASE -> line(x->cyclicPulse(x,p));
                case COMET -> line(x->{ double age=frac(p-x); return age<tail ? Math.pow(1-age/tail,2) : 0; });
                case WAVE -> line(x->.5+.5*Math.sin(TAU*(x*frequency-p)));
                case RIPPLE -> { double origin=param("origin",0,0,1); line(x->pulse(Math.abs(x-origin),p)); }
                case HEARTBEAT -> line(x->Math.max(Math.max(pulse(p,.12),.8*pulse(p,.3)),p>=.3?.45*Math.exp(-(p-.3)*5):0));
                case TWINKLE -> line(x->{ int i=(int)(x*(samples-1)); return Math.pow(.5+.5*Math.sin(TAU*(t*(.5+noise(i))+noise(i+91))),2); });
                case SPARKLE -> line(x->{ int i=(int)(x*(samples-1)); return Math.pow(.5+.5*Math.cos(TAU*(t*frequency+noise(i))),28); });
                case SCAN -> line(x->pulse(x,1-Math.abs(2*p-1)));
                case SEQUENTIAL_FILL -> line(x->p<.72 ? (x<p/.72?1:0) : (1-p)/.28);
                case DRIPPING -> line(x->{ double cluster=frac(x*frequency-p); return cluster<.23 ? Math.sin(Math.PI*cluster/.23) : 0; });
                case THEATER_CHASE -> line(x->Math.floorMod((int)Math.round(x*(samples-1))-(int)Math.floor(t*frequency),3)==0 ? 1 : 0);
                case STACKING -> line(x->{ double stage=p*markers; int filled=(int)stage;
                    if(p>.85) return x>1-(markers-1)*width ? (1-p)/.15 : 0;
                    double moving=frac(stage)*(1-filled*width);
                    return Math.max(pulse(x,moving),x>1-filled*width ? .8 : 0); });
                case CROSSFADE_CHASE -> line(x->{ double w=.5-.5*Math.cos(TAU*p); return (1-w)*cyclicPulse(x,p)+w*cyclicPulse(x,frac(p+.5)); });
                default -> { base(); geometry(pattern); }
            }
        }

        void geometry(Pattern pattern) {
            switch(pattern) {
                case TICK_TOCK -> {
                    int tick=(int)Math.floor(t*markers);
                    double head=frac((double)tick/markers);
                    for(int i=0;i<8;i++) dot(head-meters(i*.15),(tick%2==0?-1:1)*.45,0,1-i/9.0);
                }
                case CLOCK_HANDS -> {
                    for(int i=0;i<markers;i++) {
                        double c=(i+.5)/markers, a=TAU*p+i*.4;
                        ring(c,.4,.25,.35,false);
                        for(int j=1;j<=6;j++) dot(c+meters(Math.cos(a)*j*.07),Math.sin(a)*j*.07,.25,1);
                    }
                }
                case TIME_SKIP -> {
                    double head=p<.3?.25:p<.55?.25:.65+(p-.55)*.7;
                    double light=p<.3?1:p<.45?1-(p-.3)/.15:p<.55?0:1;
                    line(x->light*pulse(x,head));
                    for(int i=0;i<5;i++) dot(.25-meters(i*.1),0,0,.18*fade((p-.3)/.7));
                }
                case REVERSE_ECHO -> {
                    line(x->Math.max(cyclicPulse(x,p),.25*cyclicPulse(x,frac(-p+.35))));
                    for(int i=0;i<samples;i+=2) dot(u(i),.25,0,.15*cyclicPulse(u(i),frac(-p+.7)));
                }
                case CLOCKWORK_FOOTSTEPS, BORROWED_FOOTSTEPS -> footsteps(pattern==Pattern.BORROWED_FOOTSTEPS);
                case UNWINDING_SPRING -> {
                    double stretch=.08+.55*Math.sin(Math.PI*p), head=.12+p*.65;
                    for(int i=0;i<samples;i++) { double q=u(i), a=TAU*(q*6-p);
                        dot(head+q*stretch,Math.cos(a)*(.45-.3*p),.5+Math.sin(a)*(.45-.3*p),Math.sin(Math.PI*p)); }
                }
                case STEAM_BURSTS -> {
                    for(int i=0;i<markers;i++) { double age=frac(p*2-i/(double)markers);
                        for(int j=0;j<8;j++) { double q=j/8.0;
                            dot((i+.3)/markers+meters(age*.8),Math.sin(age*TAU+q*3)*age*.35,age*1.4+q*.3,fade(age)*fade(q)); }
                    }
                }
                case FUSE_BURN -> {
                    arrow(p,1,0,0);
                    for(int i=0;i<samples;i++) { double age=frac(p-u(i));
                        if(age<tail) dot(u(i),(noise(i)-.5)*age*3,noise(i+50)*age*2,Math.pow(fade(age/tail),3)); }
                }
                case ORBITING_GUIDE -> {
                    for(int i=0;i<samples;i++) { double q=u(i), a=TAU*(q*frequency-p);
                        for(int arm=0;arm<2;arm++) dot(q,Math.cos(a+arm*Math.PI)*.5,.65+Math.sin(a+arm*Math.PI)*.5,.65+.35*Math.sin(a)); }
                }
                case SHATTERED_SECONDS -> {
                    double gather=Math.sin(Math.PI*p), center=.25+.5*p;
                    for(int i=0;i<24;i++) { double q=(i%8)/7.0, side=i<8?0:(i<16?-1:1)*q*.5;
                        dot(center-meters(q*.8)+(noise(i)-.5)*.18*(1-gather),side*gather+(noise(i+30)-.5)*2*(1-gather),
                                .4+(noise(i+60)-.5)*(1-gather),.25+.75*gather); }
                }
                case PENDULUM -> {
                    double swing=Math.sin(TAU*p)*.85;
                    for(int i=0;i<12;i++) dot(p,swing+(noise(i)-.5)*.18,.6+(noise(i+20)-.5)*.2,1-i/16.0);
                }
                case INK_REVEAL -> {
                    for(int i=0;i<samples;i++) { double q=u(i), age=p*1.4-q;
                        if(age>0) { double light=fade(age), spread=Math.min(.6,age*2);
                            dot(q,0,0,light); dot(q,spread*(noise(i)>.5?1:-1),0,light*.6); }
                    }
                }
                case MECHANICAL_RELAY -> {
                    for(int i=0;i<markers;i++) { double age=frac(p-i/(double)markers);
                        ring((i+.5)/markers,age*.6,.2,Math.pow(fade(age),5),false); }
                }
                case GHOST_OF_TOMORROW -> silhouette(.3+Math.floor(p*3)*.2,Math.pow(Math.sin(Math.PI*frac(p*3)),2)*.65);
                case ESCAPING_SECONDS -> {
                    for(int i=0;i<24;i++) { double age=frac(p+i/24.0), launch=Math.min(1,age/.2);
                        AnimationPoint rest=f.playerPosition().add((noise(i)-.5)*1.5*f.style().width()*f.style().scale(),
                                (.6+noise(i+40))*f.style().height()*f.style().scale(),(noise(i+80)-.5)*1.5*f.style().width()*f.style().scale());
                        AnimationPoint position=age<.4?f.playerPosition().interpolate(rest,launch):rest.interpolate(f.point(.8,0,0),Math.pow((age-.4)/.6,2));
                        sink.emit(position,fade(age*.6),age); }
                }
                case CLOCKWORK_MOTH -> moth();
                case BROKEN_TIMELINE -> {
                    double align=Math.pow(Math.sin(Math.PI*p),6);
                    for(int i=0;i<samples;i++) { int group=i/4; double side=(noise(group)-.5)*2*(1-align);
                        dot(u(i),side,(noise(group+50)-.5)*(1-align),.2+.8*align); }
                }
                case POCKET_WATCH_SWING -> {
                    double side=Math.sin(TAU*p)*.8, c=.2+p*.6;
                    for(int i=0;i<8;i++) dot(c,side*i/8.0,1.8-i*.14,.25);
                    for(int i=0;i<18;i++) { double a=TAU*i/18; dot(c,side+Math.cos(a)*.3,.65+Math.sin(a)*.3,1); }
                }
                case MECHANICAL_FIREFLIES -> {
                    double spread=.15+Math.pow(Math.cos(Math.PI*p),2), c=.2+p*.65;
                    for(int i=0;i<32;i++) dot(c+meters(Math.sin(t*2+i)*spread),Math.cos(t*3+i*2)*spread,
                            .7+Math.sin(t+i*3)*spread*.5,.4+.6*noise(i));
                }
                case STITCHING_TIME -> {
                    for(int i=0;i<samples;i++) { double q=u(i), age=frac(p-q);
                        if(age<tail) dot(q,Math.sin(q*TAU*markers)*.4,0,fade(age/tail)); }
                    dot(p,Math.sin(p*TAU*markers)*.4,.1,1);
                }
                case FALLING_HOURGLASS -> {
                    for(int i=0;i<markers;i++) for(int j=0;j<10;j++) {
                        double age=frac(p+i*.2+j*.1), c=(i+.5)/markers;
                        if(age<.65) dot(c,(noise(j)-.5)*(1-age/.65),1.4*(1-age/.65),.8);
                        else dot(c+meters((age-.65)*2),.1*Math.sin(j),.1,fade((age-.65)/.35));
                    }
                }
                case ECHO_DOORS -> {
                    for(int i=0;i<Math.min(markers,4);i++) { double c=(i+1.0)/(Math.min(markers,4)+1), open=frac(p+i*.2);
                        for(int j=0;j<7;j++) { double q=j/6.0;
                            for(int side:new int[]{-1,1}) {
                                dot(c-meters(open*.7),side*(.65+open*.7),q*1.8,fade(open));
                                dot(c-meters(open*.7*q),side*q*(.65+open*.7),1.8,fade(open));
                            }
                        }
                    }
                }
                case MAGNETIC_SHAVINGS -> {
                    double align=Math.pow(Math.sin(Math.PI*p),4);
                    for(int i=0;i<samples;i++) { double q=u(i);
                        dot(q+(noise(i)-.5)*.04*(1-align),(noise(i+30)-.5)*(1-align)+Math.sin(t*35+i)*.04,0,.3+.7*align); }
                    if(align>.5) arrow(.75,align,0,0);
                }
                case CLOCK_TEETH -> {
                    for(int i=0;i<markers*2;i++) { double c=(i+.5)/(markers*2), lift=Math.max(0,Math.sin(TAU*(p-i/(double)markers)));
                        for(int j=0;j<5;j++) dot(c,(j-2)*.12,lift*.65,.25+.75*lift); }
                }
                case REWINDING_RIBBON -> {
                    for(int i=0;i<samples;i++) { double q=u(i), unwind=p<.5?0:(p-.5)*2;
                        double c=p<.5?.55-q*.25*p:.3+q*unwind*.65;
                        dot(c,Math.sin(q*TAU*3-p*TAU)*.5*(1-unwind),.5+Math.cos(q*TAU*3)*.3*(1-unwind),.9); }
                }
                case FUTURE_GLIMPSE -> {
                    arrow(.95,fade(p/.5),0,.15);
                    for(int i=0;i<markers;i++) { double c=1-(i+1.0)/(markers+1), birth=(i+1.0)/(markers+2);
                        if(p>birth) arrow(c,fade((p-birth)*1.5),0,0); }
                }
                case PRESSURE_LEAK -> {
                    for(int i=0;i<samples;i++) { double q=u(i), swell=pulse(q,p); dot(q,0,swell*.65,.3+.7*swell);
                        if(f.path().bend(q*f.path().length())>.1 || i==samples/2) {
                            for(int j=0;j<5;j++) dot(q+meters(j*.1),Math.sin(t*TAU+j)*swell*.3,j*.2*swell,swell*fade(j/6.0)); }
                    }
                }
                case ORBIT_COLLAPSE -> {
                    int marker=(int)Math.floor(p*markers); double age=frac(p*markers), c=(marker+.5)/markers;
                    if(age<.7) ring(c,.75*(1-age/.7),.5,.8,false);
                    else arrow(c+(age-.7)/.3/markers,1,0,.5);
                }
                case UNFINISHED_BLUEPRINT -> {
                    for(int i=0;i<samples;i++) { double q=u(i);
                        if(q<p*1.7 && i%2==0) { dot(q,-.45,0,fade(Math.max(0,p-.7)/.3)); dot(q,.45,0,fade(Math.max(0,p-.7)/.3)); }
                        if(i%6==0 && q<p*1.7) for(int j=0;j<4;j++) dot(q,-.45+j*.3,0,.35);
                    }
                    if(p>.55) arrow(.8,Math.sin(Math.PI*(p-.55)/.45),0,0);
                }
                case DELAYED_SHADOW -> {
                    arrow(p,1,0,.3);
                    for(int i=1;i<=3;i++) { double delayed=t-i*.14, q=frac(Math.floor(delayed*markers)/markers);
                        double catchup=Math.pow(frac(delayed*markers),5)/markers;
                        arrow(frac(q+catchup),.5/(i+1),0,.3); }
                }
                case SECOND_HAND_SWEEP -> {
                    double bend=.5, strength=0;
                    for(int i=1;i<samples-1;i++) { double b=f.path().bend(u(i)*f.path().length()); if(b>strength) {strength=b;bend=u(i);} }
                    double angle=TAU*p;
                    for(int i=0;i<24;i++) { double r=i/12.0; dot(bend+meters(Math.cos(angle)*r),Math.sin(angle)*r,.2,1-i/28.0); }
                    for(int i=0;i<samples;i++) dot(u(i),0,0,pulse(u(i),bend)*Math.pow(Math.max(0,Math.cos(angle)),4));
                }
                default -> throw new IllegalArgumentException("Not a geometry pattern: " + pattern);
            }
        }

        void moth() {
            AnimationPoint guide=(AnimationPoint)f.state().get("moth-guide");
            double wait=param("wait-distance",3,1,12), ahead=param("look-ahead",5,1,20);
            if(guide==null || guide.distanceSquared(f.playerPosition())<wait*wait || guide.distanceSquared(f.playerPosition())>24*24) {
                guide=f.path().at(Math.min(ahead,f.path().length())); f.state().put("moth-guide",guide);
            }
            double flap=Math.sin(t*TAU*8), orbit=t*TAU;
            for(int i=0;i<16;i++) { double a=TAU*i/16;
                double wing=(i<8?-1:1)*(.15+.25*Math.abs(flap));
                sink.emit(guide.add((Math.cos(orbit)*.2+wing*Math.sin(a))*f.style().width()*f.style().scale(),
                        (.8+Math.sin(a)*.18)*f.style().height()*f.style().scale(),
                        (Math.sin(orbit)*.2+Math.cos(a)*.15)*f.style().width()*f.style().scale()),.9,i/16.0);
            }
        }

        private record Foot(List<AnimationPoint> points,double born,int number) {}
        @SuppressWarnings("unchecked")
        void footsteps(boolean borrowed) {
            List<Foot> feet=(List<Foot>)f.state().computeIfAbsent("footprints",key->new ArrayList<Foot>());
            int number=(int)Math.floor(t*markers);
            Integer previous=(Integer)f.state().get("footstep-number");
            if(previous==null||previous!=number) {
                f.state().put("footstep-number",number);
                double center=.12+frac(number/(double)markers)*.55;
                List<AnimationPoint> points=new ArrayList<>();
                for(int i=0;i<4;i++) points.add(f.point(center+meters(i*.11),(number%2==0?-1:1)*.3,0));
                feet.add(new Foot(List.copyOf(points),f.elapsedSeconds(),number));
            }
            double lifetime=f.style().periodSeconds()*.8;
            feet.removeIf(foot->f.elapsedSeconds()-foot.born>lifetime);
            while(feet.size()>markers*2)feet.removeFirst();
            for(Foot foot:feet) {
                double age=(f.elapsedSeconds()-foot.born)/lifetime;
                if(borrowed&&noise(foot.number)>.45&&age>.2&&age<.45)continue;
                for(AnimationPoint point:foot.points) sink.emit(point,fade(age),frac(foot.number/(double)markers));
            }
        }
    }
    private static double clamp(double x) { return Math.clamp(x,0,1); }
    private static double frac(double x) { return x-Math.floor(x); }
    private static double random(long x) { x=(x^(x>>>30))*0xbf58476d1ce4e5b9L; x=(x^(x>>>27))*0x94d049bb133111ebL; return ((x^(x>>>31))>>>11)*0x1.0p-53; }
}
