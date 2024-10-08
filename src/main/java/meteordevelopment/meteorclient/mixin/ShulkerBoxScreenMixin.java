/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.mirage.ShulkerDupe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;;

@Mixin(ShulkerBoxScreen.class)
public class ShulkerBoxScreenMixin extends Screen {
    public ShulkerBoxScreenMixin(Text title) {
        super(title);
    }

    @Override
    protected void init()
    {
        super.init();
        if(Modules.get().isActive(ShulkerDupe.class)) {
            addDrawableChild(new ButtonWidget.Builder(Text.literal("Dupe 1"), button -> dupe())
                    .position(240, height / 2 + 35 - 140)
                    .size( 50, 15)
                    .build()
            );
            addDrawableChild(new ButtonWidget.Builder(Text.literal("Dupe 27"), button -> dupeAll())
                    .position(295, height / 2 + 35 - 140)
                    .size( 50, 15)
                    .build()
            );

    }
    }

    private void dupe() {
        ShulkerDupe.shouldDupe=true;
    }
    private void dupeAll() {
        ShulkerDupe.shouldDupeAll=true;
    }
}
