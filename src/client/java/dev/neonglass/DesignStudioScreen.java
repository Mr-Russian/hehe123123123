package dev.neonglass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DesignStudioScreen extends Screen {
    private int selectedTab = 0;
    public DesignStudioScreen() { super(Component.literal("Neon Glass Design Studio")); }

    @Override protected void init() {
        int center = width/2;
        addRenderableWidget(Button.builder(Component.literal(NeonGlassClient.CONFIG.hudEnabled ? "HUD: ON" : "HUD: OFF"), b -> {
            NeonGlassClient.CONFIG.hudEnabled = !NeonGlassClient.CONFIG.hudEnabled; b.setMessage(Component.literal(NeonGlassClient.CONFIG.hudEnabled ? "HUD: ON" : "HUD: OFF")); NeonGlassClient.CONFIG.save();
        }).bounds(center-250, height-42, 110, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Connect Spotify"), b -> NeonGlassClient.SPOTIFY.connect()).bounds(center-132, height-42, 130, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Disconnect"), b -> NeonGlassClient.SPOTIFY.disconnect()).bounds(center-132, height-72, 130, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Save settings"), b -> { NeonGlassClient.CONFIG.save(); NeonGlassClient.status("Settings saved."); }).bounds(center+6, height-42, 110, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(center+124, height-42, 90, 24).build());
        addRenderableWidget(Button.builder(Component.literal("− Blur"), b -> { NeonGlassClient.CONFIG.blur=Math.max(0,NeonGlassClient.CONFIG.blur-1); }).bounds(width/2-420,height/2-72,75,22).build());
        addRenderableWidget(Button.builder(Component.literal("+ Blur"), b -> { NeonGlassClient.CONFIG.blur=Math.min(30,NeonGlassClient.CONFIG.blur+1); }).bounds(width/2-340,height/2-72,75,22).build());
        addRenderableWidget(Button.builder(Component.literal("Color: Cyan / Pink"), b -> { NeonGlassClient.CONFIG.accent = NeonGlassClient.CONFIG.accent==0xFF27C9FF ? 0xFFEC43F2 : 0xFF27C9FF; }).bounds(width/2-100,height/2+80,200,24).build());
        addRenderableWidget(Button.builder(Component.literal("− Volume"), b -> { NeonGlassClient.CONFIG.volume=Math.max(0,NeonGlassClient.CONFIG.volume-.05f); NeonGlassClient.SPOTIFY.setVolume(NeonGlassClient.CONFIG.volume); NeonGlassClient.CONFIG.save(); }).bounds(width/2-410,height/2+130,90,22).build());
        addRenderableWidget(Button.builder(Component.literal("+ Volume"), b -> { NeonGlassClient.CONFIG.volume=Math.min(1,NeonGlassClient.CONFIG.volume+.05f); NeonGlassClient.SPOTIFY.setVolume(NeonGlassClient.CONFIG.volume); NeonGlassClient.CONFIG.save(); }).bounds(width/2-315,height/2+130,90,22).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.fill(0,0,width,height,0xCC070916);
        int gap=12, panelW=Math.min(310,(width-80)/3), panelH=Math.min(440,height-120), top=(height-panelH)/2-10;
        int left=width/2-(panelW*3+gap*2)/2;
        panel(g,left,top,panelW,panelH,"⚙ Settings");
        panel(g,left+panelW+gap,top,panelW,panelH,"◉ Color Preview");
        panel(g,left+(panelW+gap)*2,top,panelW,panelH,"‹› Generated CSS");
        int y=top+48;
        label(g,"Blur value",left+20,y); value(g,NeonGlassClient.CONFIG.blur+"px",left+panelW-60,y); y+=42;
        label(g,"Refraction",left+20,y); value(g,"0.05",left+panelW-60,y); y+=42;
        label(g,"Depth",left+20,y); value(g,"5",left+panelW-60,y); y+=42;
        label(g,"Border Radius",left+20,y); value(g,NeonGlassClient.CONFIG.borderRadius+"px",left+panelW-60,y); y+=42;
        label(g,"Border Opacity",left+20,y); value(g,String.format("%.2f",NeonGlassClient.CONFIG.borderOpacity),left+panelW-60,y); y+=46;
        label(g,"Background",left+20,y); y+=25;
        label(g,"Background Image",left+20,y); y+=26;
        g.fill(left+20,y,left+panelW-20,y+25,0xFF263D67); g.text(font,"Tech background",left+28,y+8,0xFFE5E9FF,false); y+=48;
        label(g,"Background Opacity",left+20,y); value(g,"1.00",left+panelW-60,y); y+=40;
        label(g,"Background Blur",left+20,y); value(g,"0px",left+panelW-60,y);

        int mid=left+panelW+gap;
        label(g,"Base Color",mid+20,top+48); g.fill(mid+20,top+72,mid+panelW-20,top+108,0xFFEB8EFD); g.outline(mid+20,top+72,panelW-40,36,0xFFCF9BFF);
        label(g,"Glass Opacity",mid+20,top+128); value(g,String.format("%.2f",NeonGlassClient.CONFIG.glassOpacity),mid+panelW-70,top+128);
        label(g,"Saturation",mid+20,top+174); value(g,"1.00",mid+panelW-70,top+174);
        label(g,"Brightness",mid+20,top+220); value(g,"1.00",mid+panelW-70,top+220);
        label(g,"Glassmorphic Variants",mid+20,top+266);
        int sw=52; int[] colors={0xFFFBE9FF,0xFFF1C5F8,0xFFE9A5F6,0xFFD96CF2,0xFFC936EC,0xFFB81DDC,0xFF970AB9,0xFF76058E,0xFF510064,0xFF330044};
        for(int i=0;i<colors.length;i++){int xx=mid+20+(i%4)*(sw+8), yy=top+294+(i/4)*(sw+8);g.fill(xx,yy,xx+sw,yy+sw,colors[i]);g.outline(xx,yy,sw,sw,0xFF6D5B7A);}
        g.fill(mid+20,top+435,mid+panelW-20,top+panelH-20,0x665B4265); g.text(font,"Color Preview",mid+panelW/2-42,top+panelH-65,0xFFFFFFFF,true); g.text(font,"#EB8EFD",mid+panelW/2-25,top+panelH-45,0xFFDCD4E9,false);

        int right=left+(panelW+gap)*2;
        label(g,"Colored Glass Card",right+20,top+48); g.fill(right+20,top+74,right+panelW-20,top+202,0x9910111C);
        g.text(font,".glass-card-colored {",right+32,top+88,0xFFE8E5F0,false);
        g.text(font,"  backdrop-filter: blur(15px);",right+32,top+106,0xFFE8E5F0,false);
        g.text(font,"  border-radius: 30px;",right+32,top+124,0xFFE8E5F0,false);
        g.text(font,"  border: 1px solid rgba(...);",right+32,top+142,0xFFE8E5F0,false);
        label(g,"Base Glass Card",right+20,top+222); g.fill(right+20,top+248,right+panelW-20,top+376,0x9910111C);
        g.text(font,".glass-card {",right+32,top+262,0xFFE8E5F0,false);
        g.text(font,"  background: rgba(255,255,255,.05);",right+32,top+280,0xFFE8E5F0,false);
        g.text(font,"  backdrop-filter: blur(15px);",right+32,top+298,0xFFE8E5F0,false);
        g.text(font,"  border-radius: 30px;",right+32,top+316,0xFFE8E5F0,false);
        g.text(font,"Spotify: "+(NeonGlassClient.SPOTIFY.track().title()),right+20,top+panelH-40,0xFFB5B0C9,false);
        g.text(font,NeonGlassClient.SPOTIFY.isConnected() ? "Account: connected" : "Account: not connected / reconnect",right+20,top+panelH-22,0xFF78B8FF,false);
        g.text(font,"NEON GLASS MUSIC HUD  •  MINECRAFT 26.3",width/2-120,18,0xFFB9C7FF,true);
    }

    private void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,String title){g.fill(x-2,y-2,x+w+2,y+h+2,0x3326BDF2);g.fill(x,y,x+w,y+h,0xE8171A28);g.outline(x,y,w,h,0x886B7896);g.text(font,title,x+20,y+18,0xFF78B8FF,true);}
    private void label(GuiGraphicsExtractor g,String s,int x,int y){g.text(font,s,x,y,0xFFF1EDF8,false);}
    private void value(GuiGraphicsExtractor g,String s,int x,int y){g.text(font,s,x,y,0xFF55D78A,true);}
}
