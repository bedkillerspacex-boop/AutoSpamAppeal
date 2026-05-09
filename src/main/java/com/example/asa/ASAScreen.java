package com.example.asa;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ASAScreen extends Screen {
    private TextFieldWidget reasonField;

    public ASAScreen() {
        super(Text.literal("ASA 设置"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 开关按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("ASA 状态: " + (ASAConfig.enabled ? "§a开启" : "§c关闭")),
            button -> {
                ASAConfig.enabled = !ASAConfig.enabled;
                button.setMessage(Text.literal("ASA 状态: " + (ASAConfig.enabled ? "§a开启" : "§c关闭")));
                ASAConfig.save();
            }
        ).dimensions(centerX - 100, centerY - 50, 200, 20).build());

        // 调试信息开关按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("调试信息: " + (ASAConfig.showDebug ? "§a显示" : "§c隐藏")),
            button -> {
                ASAConfig.showDebug = !ASAConfig.showDebug;
                button.setMessage(Text.literal("调试信息: " + (ASAConfig.showDebug ? "§a显示" : "§c隐藏")));
                ASAConfig.save();
            }
        ).dimensions(centerX - 100, centerY - 25, 200, 20).build());

        // 理由输入框 (向下移动一点以避开新按钮)
        this.reasonField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY + 10, 200, 20, Text.literal("申诉理由"));
        this.reasonField.setMaxLength(128);
        this.reasonField.setText(ASAConfig.appealReason);
        this.addDrawableChild(this.reasonField);

        // 保存按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("保存并关闭"),
            button -> {
                ASAConfig.appealReason = this.reasonField.getText();
                ASAConfig.save();
                this.close();
            }
        ).dimensions(centerX - 100, centerY + 40, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "申诉理由:", this.width / 2 - 100, this.height / 2 + 0, 0xFFFFFF);
    }
}
