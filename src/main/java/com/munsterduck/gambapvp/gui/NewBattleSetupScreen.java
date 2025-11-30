package com.munsterduck.gambapvp.gui;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.util.Identifier;

public class NewBattleSetupScreen extends BaseUIModelScreen<FlowLayout> {

    public NewBattleSetupScreen() {
        super(FlowLayout.class, DataSource.asset(new Identifier("gambapvp", "battle_screen")));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void build(FlowLayout rootComponent) {
        rootComponent.childById(ButtonComponent.class, "inspector-button")
                .onPress(button -> this.uiAdapter.toggleInspector());

        rootComponent.childById(ButtonComponent.class, "next-button")
                .onPress(button -> this.uiAdapter.toggleInspector());

        final var boxContainer = rootComponent.childById(FlowLayout.class, "box-container");
        var horizontalExpandAnimation = boxContainer
                .horizontalSizing().animate(750, Easing.CUBIC, Sizing.fixed(150));
        var verticalExpandAnimation = boxContainer
                .verticalSizing().animate(750, Easing.CUBIC, Sizing.fixed(150));

        final var theBox = rootComponent.childById(BoxComponent.class, "the-box");
        var boxAnimation = theBox
                .positioning().animate(750, Easing.CUBIC, Positioning.relative(50, 50));
        var boxColorAnimation = theBox
                .endColor().animate(1500, Easing.QUADRATIC, Color.ofRgb(0x5800FF));

        var animations = Animation.compose(horizontalExpandAnimation, verticalExpandAnimation, boxAnimation, boxColorAnimation);

        rootComponent.childById(ButtonComponent.class, "show-playground-button")
                .onPress(button -> {
                    button.parent().removeChild(button);
                    animations.forwards();
                    boxContainer.padding(Insets.of(1));
                });
    }
}
